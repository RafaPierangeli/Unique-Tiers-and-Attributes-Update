package draylar.tiered.util;

import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.api.ARPGEquipmentData;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Predicate;

public class AoEMiningHelper {

    public static final ThreadLocal<Boolean> IS_AOE_MINING = ThreadLocal.withInitial(() -> false);

    public static void processAoEMining(ServerWorld world, ServerPlayerEntity player, BlockPos centerPos, BlockState centerState) {
        if (IS_AOE_MINING.get()) return;

        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty()) return;

        boolean isPickaxe = tool.isIn(ItemTags.PICKAXES);
        boolean isShovel = tool.isIn(ItemTags.SHOVELS);
        boolean isHoe = tool.isIn(ItemTags.HOES);

        if (!isPickaxe && !isShovel && !isHoe) return;

        ARPGEquipmentData data = tool.get(TieredDataComponents.ARPG_DATA);
        if (data == null || data.isBroken() || data.level() <= 0) return;

        String affinity = data.affinity();

        // 🌟 O FILTRO INTELIGENTE: Define exatamente o que cada ferramenta pode quebrar em área
        Predicate<BlockState> isValidTarget = (state) -> {
            if (isPickaxe && "hard_labor".equals(affinity)) {
                return state.isIn(BlockTags.PICKAXE_MINEABLE);
            }
            if (isShovel && "voracious_digger".equals(affinity)) {
                return state.isIn(BlockTags.SHOVEL_MINEABLE);
            }
            if (isHoe && ("bountiful_harvest".equals(affinity) || "soil_master".equals(affinity))) {
                // Para a Enxada: Aceita Plantações, Fungo do Nether OU blocos normais de enxada (Folhas, etc)
                return state.isIn(BlockTags.CROPS) ||
                        state.isOf(net.minecraft.block.Blocks.NETHER_WART) ||
                        state.isIn(BlockTags.HOE_MINEABLE);
            }
            return false;
        };

        // Se o bloco central não for um alvo válido, aborta
        if (!isValidTarget.test(centerState)) return;

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

        if (currentRadius <= 0) return;

        HitResult hit = player.raycast(20.0D, 0.0F, false);
        Direction face = Direction.UP;
        if (hit.getType() == HitResult.Type.BLOCK) {
            face = ((BlockHitResult) hit).getSide();
        }

        IS_AOE_MINING.set(true);
        try {
            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int y = -currentRadius; y <= currentRadius; y++) {
                    if (x == 0 && y == 0) continue;

                    BlockPos targetPos = switch (face.getAxis()) {
                        case Y -> centerPos.add(x, 0, y);
                        case Z -> centerPos.add(x, y, 0);
                        case X -> centerPos.add(0, x, y);
                    };

                    BlockState targetState = world.getBlockState(targetPos);

                    // Usa o nosso Filtro Inteligente para checar os blocos ao redor
                    if (isValidTarget.test(targetState) && targetState.getHardness(world, targetPos) >= 0) {
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
                                (isHoe && "soil_master".equals(affinity)) ||
                                (isHoe && "bountiful_harvest".equals(affinity));

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
                                player.sendMessage(net.minecraft.text.Text.translatable("tiered.arpg.message.aoe_mode", sizeStr).formatted(Formatting.AQUA), true);

                                return net.minecraft.util.ActionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
            return net.minecraft.util.ActionResult.PASS;
        });
    }

    public static void registerTillingEvent() {
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Só executa no servidor e na mão principal
            if (world.isClient() || hand != net.minecraft.util.Hand.MAIN_HAND) return net.minecraft.util.ActionResult.PASS;

            ItemStack tool = player.getStackInHand(hand);
            if (!tool.isIn(ItemTags.HOES)) return net.minecraft.util.ActionResult.PASS;

            ARPGEquipmentData data = tool.get(TieredDataComponents.ARPG_DATA);
            if (data == null || data.isBroken() || data.level() <= 0) return net.minecraft.util.ActionResult.PASS;

            String affinity = data.affinity();
            // 🌟 CORREÇÃO: Agora aceita tanto soil_master quanto bountiful_harvest!
            if (!"soil_master".equals(affinity) && !"bountiful_harvest".equals(affinity)) {
                return net.minecraft.util.ActionResult.PASS;
            }

            int maxRadius = 0;
            if (data.prestige() >= 1) maxRadius = 1;
            if (data.prestige() >= 2) maxRadius = 2;
            if (data.prestige() >= 3) maxRadius = 3;
            if (data.prestige() >= 3 && data.level() >= 100) maxRadius = 4;

            int currentRadius = maxRadius;
            if (maxRadius > 0) {
                Integer selectedMode = tool.get(TieredDataComponents.AOE_MODE);
                if (selectedMode != null) {
                    currentRadius = Math.min(selectedMode, maxRadius);
                }
            }

            if (currentRadius <= 0) return net.minecraft.util.ActionResult.PASS;

            BlockPos centerPos = hitResult.getBlockPos();

            // Trava de segurança para evitar loop infinito
            if (IS_AOE_MINING.get()) return net.minecraft.util.ActionResult.PASS;

            IS_AOE_MINING.set(true);
            try {
                // Arar a terra é um processo 2D (X e Z), não precisamos mexer no Y (altura)
                for (int x = -currentRadius; x <= currentRadius; x++) {
                    for (int z = -currentRadius; z <= currentRadius; z++) {
                        if (x == 0 && z == 0) continue; // O bloco central o Minecraft já vai arar naturalmente

                        BlockPos targetPos = centerPos.add(x, 0, z);

                        // Cria um "Contexto Falso" para enganar a enxada e fazê-la achar que o jogador clicou no bloco vizinho
                        net.minecraft.item.ItemUsageContext context = new net.minecraft.item.ItemUsageContext(
                                player, hand, new BlockHitResult(
                                new net.minecraft.util.math.Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5),
                                Direction.UP,
                                targetPos,
                                false
                        )
                        );

                        // Manda a enxada agir naquele bloco!
                        tool.getItem().useOnBlock(context);
                    }
                }
            } finally {
                IS_AOE_MINING.set(false);
            }

            // Retorna PASS para que o Minecraft processe o bloco central normalmente
            return net.minecraft.util.ActionResult.PASS;
        });
    }
}