package draylar.tiered.api;

import draylar.tiered.api.EquipmentCategory;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.util.ARPGXpHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;

public class ARPGEventHandlers {

    public static void register() {

        // ⛏️ 1. EVENTO DE MINERAÇÃO
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack mainHand = serverPlayer.getMainHandStack();
                if (EquipmentCategory.getCategory(mainHand) == EquipmentCategory.TOOL) {
                    int xpAmount = ConfigInit.CONFIG.xpBaseMineBlock;
                    int multiplier = 1;

                    if (state.isOf(Blocks.ANCIENT_DEBRIS)) multiplier = ConfigInit.CONFIG.xpMultiplierNetherite;
                    else if (state.isIn(BlockTags.EMERALD_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierEmerald;
                    else if (state.isIn(BlockTags.DIAMOND_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierDiamond;
                    else if (state.isIn(BlockTags.GOLD_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierGold;
                    else if (state.isIn(BlockTags.LAPIS_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierLapis;
                    else if (state.isIn(BlockTags.IRON_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierIron;
                    else if (state.isIn(BlockTags.REDSTONE_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierRedstone;
                    else if (state.isIn(BlockTags.COPPER_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierCopper;
                    else if (state.isIn(BlockTags.COAL_ORES)) multiplier = ConfigInit.CONFIG.xpMultiplierCoal;
                    else if (state.isOf(Blocks.NETHER_QUARTZ_ORE)) multiplier = ConfigInit.CONFIG.xpMultiplierQuartz;

                    xpAmount *= multiplier;
                    ARPGXpHelper.addXp(mainHand, "mining", xpAmount, serverPlayer);
                }
            }
        });

        // 🌊 2. EVENTO DE TICK DO SERVIDOR (Aquatic e Oxygen)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {

                if (serverPlayer.age % 20 == 0) {
                    ItemStack mainHand = serverPlayer.getMainHandStack();

                    if (mainHand.isOf(Items.FISHING_ROD)) {

                        // 🌟 Lógica de Estrategista: Evitando o ganho duplo de XP!
                        // 1º Checa se está nadando ativamente (Sprint na água)
                        if (serverPlayer.isSwimming()) {
                            ARPGXpHelper.addXp(mainHand, "aquatic", ConfigInit.CONFIG.xpBaseSwim, serverPlayer);
                        }
                        // 2º Se NÃO estiver nadando, mas a cabeça estiver debaixo d'água (Parado/Andando no fundo)
                        else if (serverPlayer.isSubmergedInWater()) {
                            ARPGXpHelper.addXp(mainHand, "oxygen", ConfigInit.CONFIG.xpBaseSubmerge, serverPlayer);
                        }
                    }
                }
            }
        });
    }
}