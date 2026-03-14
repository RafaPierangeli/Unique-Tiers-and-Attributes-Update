package draylar.tiered.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Block.class)
public abstract class BlockDropMixin {

    // Injetamos no momento exato em que o bloco decide o que vai dropar
    @Inject(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void onGetDroppedStacks(BlockState state, ServerWorld world, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        // Se não tem ferramenta, ignora
        if (tool == null || tool.isEmpty()) return;

        draylar.tiered.api.ARPGEquipmentData data = tool.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
        if (data == null || data.isBroken() || data.level() <= 0) return;

        String affinity = data.affinity();
        boolean isMatch = false;

        // 🌟 1. IDENTIFICAÇÃO DO ALVO (Qual bloco foi quebrado?)
        if ("midas_touch".equals(affinity) && (
                state.isIn(net.minecraft.registry.tag.BlockTags.COAL_ORES) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.IRON_ORES) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.GOLD_ORES) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.REDSTONE_ORES) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.LAPIS_ORES) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.DIAMOND_ORES) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.EMERALD_ORES) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.COPPER_ORES)
        )) {
            isMatch = true; // Minérios Vanilla
        } else if ("hidden_treasures".equals(affinity) && state.isIn(net.minecraft.registry.tag.BlockTags.SHOVEL_MINEABLE)) {
            isMatch = true; // Terra, Areia, Cascalho, Argila
        } else if ("bountiful_harvest".equals(affinity) && state.isIn(net.minecraft.registry.tag.BlockTags.CROPS)) {
            isMatch = true; // Plantações (Trigo, Cenoura, etc)
        } else if ("soil_master".equals(affinity) && state.isIn(net.minecraft.registry.tag.BlockTags.CROPS)) {
            isMatch = true; // Plantações (Trigo, Cenoura, etc)
        } else if ("demeter_blessing".equals(affinity) && (state.isOf(net.minecraft.block.Blocks.NETHER_WART) || state.isOf(net.minecraft.block.Blocks.CHORUS_PLANT) || state.isOf(net.minecraft.block.Blocks.CHORUS_FLOWER))) {
            isMatch = true; // Plantações do Nether/End
        }

        if (isMatch) {
            // Puxa o valor do bônus (Ex: 50.0 significa 50% de chance de dropar o dobro)
            double bonus = draylar.tiered.util.ARPGAffinityLogic.getBonusValue(affinity, data.level(), data.prestige());
            if (bonus <= 0) return;

            List<ItemStack> originalDrops = cir.getReturnValue();
            if (originalDrops == null || originalDrops.isEmpty()) return;

            // Cria uma nova lista para podermos adicionar os itens extras
            List<ItemStack> newDrops = new ArrayList<>(originalDrops);

            for (ItemStack drop : originalDrops) {
                // 🌟 2. ANTI-EXPLOIT DE SILK TOUCH
                // Impede o jogador de duplicar o próprio bloco de minério e farmar infinitamente
                if ("midas_touch".equals(affinity) && drop.isOf(state.getBlock().asItem())) {
                    continue;
                }

                // 🌟 3. MATEMÁTICA DO LOOT
                // Se o bônus for 150: extraMultiplier = 1 garantido + 50% de chance de virar 2
                int extraMultiplier = (int) (bonus / 100.0);
                if (world.random.nextDouble() * 100.0 < (bonus % 100.0)) {
                    extraMultiplier++;
                }

                if (extraMultiplier > 0) {
                    ItemStack extraDrop = drop.copy();
                    extraDrop.setCount(drop.getCount() * extraMultiplier);
                    newDrops.add(extraDrop);
                }
            }

            // Substitui os drops originais pela nossa nova lista recheada!
            cir.setReturnValue(newDrops);
        }
    }
}