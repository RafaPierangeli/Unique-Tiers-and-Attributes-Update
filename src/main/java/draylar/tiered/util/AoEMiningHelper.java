package draylar.tiered.util;

import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.api.ARPGEquipmentData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AoEMiningHelper {

    public static final ThreadLocal<Boolean> IS_AOE_MINING = ThreadLocal.withInitial(() -> false);

    public static void processAoEMining(ServerWorld world, ServerPlayerEntity player, BlockPos centerPos, BlockState centerState) {
        if (IS_AOE_MINING.get()) return;

        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty()) return;

        TagKey<Block> mineableTag = null;
        boolean isPickaxe = tool.isIn(ItemTags.PICKAXES);
        boolean isShovel = tool.isIn(ItemTags.SHOVELS);
        boolean isHoe = tool.isIn(ItemTags.HOES);

        if (isPickaxe) mineableTag = BlockTags.PICKAXE_MINEABLE;
        else if (isShovel) mineableTag = BlockTags.SHOVEL_MINEABLE;
        else if (isHoe) mineableTag = BlockTags.HOE_MINEABLE;

        if (mineableTag == null) return;

        ARPGEquipmentData data = tool.get(TieredDataComponents.ARPG_DATA);

        // 🌟 CORREÇÃO: Sinais matemáticos arrumados (<=)
        if (data == null || data.isBroken() || data.level() <= 0) return;

        String affinity = data.affinity();
        boolean hasValidAffinity = (isPickaxe && "hard_labor".equals(affinity)) ||
                (isShovel && "voracious_digger".equals(affinity)) ||
                (isHoe && "soil_master".equals(affinity));

        if (!hasValidAffinity) return;

        int maxRadius = 0;
        if (data.prestige() >= 1) maxRadius = 1; // Prestígio 1 = 3x3
        if (data.prestige() >= 2) maxRadius = 2; // Prestígio 2 = 5x5
        if (data.prestige() >= 3) maxRadius = 3; // Prestígio 3 = 7x7
        if (data.prestige() >= 3 && data.level() >= 100) maxRadius = 4; // Prestígio 3 + Nível 100 = 9x9

        int currentRadius = maxRadius;

        if (maxRadius > 0) {
            Integer selectedMode = tool.get(TieredDataComponents.AOE_MODE);
            if (selectedMode != null) {
                currentRadius = Math.min(selectedMode, maxRadius);
            }
        }

        // 🌟 CORREÇÃO: Sinais matemáticos arrumados (<=)
        if (currentRadius <= 0) return;

        if (!centerState.isIn(mineableTag)) return;

        HitResult hit = player.raycast(20.0D, 0.0F, false);
        Direction face = Direction.UP;
        if (hit.getType() == HitResult.Type.BLOCK) {
            face = ((BlockHitResult) hit).getSide();
        }

        IS_AOE_MINING.set(true);
        try {
            // 🌟 CORREÇÃO: Sinais matemáticos arrumados (<=)
            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int y = -currentRadius; y <= currentRadius; y++) {
                    if (x == 0 && y == 0) continue;

                    BlockPos targetPos = switch (face.getAxis()) {
                        case Y -> centerPos.add(x, 0, y);
                        case Z -> centerPos.add(x, y, 0);
                        case X -> centerPos.add(0, x, y);
                    };

                    BlockState targetState = world.getBlockState(targetPos);

                    if (targetState.isIn(mineableTag) && targetState.getHardness(world, targetPos) >= 0) {
                        player.interactionManager.tryBreakBlock(targetPos);
                        if (player.getMainHandStack().isEmpty()) break;
                    }
                }
            }
        } finally {
            IS_AOE_MINING.set(false);
        }
    }

    public static void registerToggleEvent() {
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || hand != net.minecraft.util.Hand.MAIN_HAND) return net.minecraft.util.ActionResult.PASS;

            if (player.isSneaking()) {
                ItemStack tool = player.getStackInHand(hand);

                boolean isPickaxe = tool.isIn(ItemTags.PICKAXES);
                boolean isShovel = tool.isIn(ItemTags.SHOVELS);
                boolean isHoe = tool.isIn(ItemTags.HOES);

                if (isPickaxe || isShovel || isHoe) {

                    ARPGEquipmentData data = tool.get(TieredDataComponents.ARPG_DATA);

                    if (data != null && !data.isBroken()) {

                        String affinity = data.affinity();
                        boolean hasValidAffinity = (isPickaxe && "hard_labor".equals(affinity)) ||
                                (isShovel && "voracious_digger".equals(affinity)) ||
                                (isHoe && "soil_master".equals(affinity));

                        if (hasValidAffinity) {
                            int maxRadius = 0;
                            if (data.prestige() >= 1) maxRadius = 1;
                            if (data.prestige() >= 2) maxRadius = 2;
                            if (data.prestige() >= 3) maxRadius = 3;
                            if (data.prestige() >= 3 && data.level() >= 100) maxRadius = 4;

                            if (maxRadius > 0) {
                                Integer currentMode = tool.getOrDefault(TieredDataComponents.AOE_MODE, maxRadius);

                                int nextMode = currentMode + 1;
                                if (nextMode > maxRadius) nextMode = 0;

                                tool.set(TieredDataComponents.AOE_MODE, nextMode);

                                String sizeStr = nextMode == 0 ? "1x1" : (nextMode * 2 + 1) + "x" + (nextMode * 2 + 1);
                                player.sendMessage(net.minecraft.text.Text.translatable("tiered.arpg.message.aoe_mode", sizeStr), true);

                                return net.minecraft.util.ActionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });
    }
}