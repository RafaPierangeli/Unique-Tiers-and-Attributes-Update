package draylar.tiered.api;

import draylar.tiered.api.EquipmentCategory;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.util.ARPGXpHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;

public class ARPGEventHandlers {

    public static void register() {

        // ⛏️ 1. EVENTO DE MINERAÇÃO E ESCAVAÇÃO
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack mainHand = serverPlayer.getMainHandStack();

                // ⛏️ PICARETA (Usando a Tag oficial da 1.21.11 em vez da classe deletada)
                if (mainHand.isIn(net.minecraft.registry.tag.ItemTags.PICKAXES)) {

                    // Só ganha XP se a picareta for a ferramenta correta para o bloco
                    if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
                        String specificAffinity = null;
                        int specificAmount = 0;

                        // 🌟 1. A SUA LÓGICA DE MULTIPLICADORES (XP Base)
                        int baseAmount = ConfigInit.CONFIG.xpBaseMineBlock;
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

                        baseAmount *= multiplier;

                        // 🌟 2. AS AFINIDADES ESPECÍFICAS (3 XP)
                        // Toque de Midas (Minérios Raros)
                        if (state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES) ||
                                state.isOf(Blocks.ANCIENT_DEBRIS) || state.isIn(BlockTags.GOLD_ORES) ||
                                state.isIn(BlockTags.LAPIS_ORES) || state.isIn(BlockTags.REDSTONE_ORES)) {

                            specificAffinity = "midas_touch";
                            specificAmount = 3;
                        }
                        // Trabalho Árduo (Pedras e blocos duros)
                        else if (state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.BASE_STONE_NETHER) ||
                                state.isOf(Blocks.END_STONE) || state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN)) {

                            specificAffinity = "hard_labor";
                            specificAmount = 2;

                            // Obsidiana é muito dura, merece um bônus extra no XP base!
                            if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN)) {
                                baseAmount *= 5;
                            }
                        }

                        // Aplica o XP Base (com seus multiplicadores) + XP Específico (se houver)
                        ARPGXpHelper.grantXp(mainHand, specificAffinity, specificAmount, true, baseAmount, serverPlayer);

                        // 🌟 3. Braço Longo (Distância máxima)
                        // Calculamos a distância do olho do jogador até o centro do bloco
                        double distance = serverPlayer.getEyePos().distanceTo(pos.toCenterPos());
                        if (distance >= 4.0) {
                            // Passamos false para isBaseAction para não dar o XP base em dobro
                            ARPGXpHelper.grantXp(mainHand, "long_reach", 4, false, 0, serverPlayer);
                        }
                    }
                }
                // 🪓 PÁ
                else if (mainHand.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) {

                    // Só ganha XP se a pá for a ferramenta correta para o bloco
                    if (state.isIn(BlockTags.SHOVEL_MINEABLE)) {
                        String specificAffinity = null;
                        int specificAmount = 0;
                        int baseAmount = ConfigInit.CONFIG.xpBaseMineBlock;

                        // 1. Tesouros Ocultos (Blocos valiosos/raros de pá)
                        if (state.isOf(Blocks.CLAY) || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.SNOW) ||
                                state.isOf(Blocks.SOUL_SAND) || state.isOf(Blocks.SOUL_SOIL) ||
                                state.isOf(Blocks.MYCELIUM) || state.isOf(Blocks.PODZOL)) {

                            specificAffinity = "hidden_treasures";
                            specificAmount = 3;
                            baseAmount *= 2; // Bônus de XP base por achar blocos raros
                        }
                        // 2. Escavador Voraz (Terra, Areia, Cascalho)
                        else if (state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.SAND) || state.isOf(Blocks.GRAVEL)) {
                            specificAffinity = "voracious_digger";
                            specificAmount = 2;
                        }

                        // Aplica o XP Base + XP Específico
                        ARPGXpHelper.grantXp(mainHand, specificAffinity, specificAmount, true, baseAmount, serverPlayer);

                        // 3. Alcance Terrestre (Distância máxima)
                        double distance = serverPlayer.getEyePos().distanceTo(pos.toCenterPos());
                        if (distance >= 4.0) {
                            // 🌟 4 XP para garantir que o alcance vença a disputa!
                            ARPGXpHelper.grantXp(mainHand, "earth_reach", 4, false, 0, serverPlayer);
                        }
                    }
                }
                // 🌾 ENXADA
                else if (mainHand.isIn(net.minecraft.registry.tag.ItemTags.HOES)) {

                    boolean isHoeBlock = state.isIn(BlockTags.HOE_MINEABLE);
                    boolean isCrop = state.isIn(BlockTags.CROPS) || state.isOf(Blocks.MELON) ||
                            state.isOf(Blocks.PUMPKIN) || state.isOf(Blocks.SUGAR_CANE) ||
                            state.isOf(Blocks.NETHER_WART);

                    if (isHoeBlock || isCrop) {
                        String specificAffinity = null;
                        int specificAmount = 0;
                        int baseAmount = ConfigInit.CONFIG.xpBaseMineBlock;

                        // 1. Bênção de Deméter (Blocos Raros, Mágicos e Botânicos)
                        // Juntamos os blocos antigos do soil_master aqui!
                        if (state.isOf(Blocks.SPONGE) || state.isOf(Blocks.WET_SPONGE) ||
                                state.isOf(Blocks.SCULK_CATALYST) || state.isOf(Blocks.SCULK_SHRIEKER) ||
                                state.isOf(Blocks.SCULK_SENSOR) || state.isIn(BlockTags.LEAVES) ||
                                state.isOf(Blocks.MOSS_BLOCK) || state.isOf(Blocks.NETHER_WART_BLOCK) ||
                                state.isOf(Blocks.WARPED_WART_BLOCK) || state.isOf(Blocks.HAY_BLOCK) ||
                                state.isOf(Blocks.SHROOMLIGHT) || state.isOf(Blocks.SCULK) ||
                                state.isOf(Blocks.TARGET)) {

                            specificAffinity = "demeter_blessing";
                            specificAmount = 3;

                            // Bônus de XP base apenas para os blocos realmente raros/perigosos
                            if (state.isOf(Blocks.SPONGE) || state.isOf(Blocks.WET_SPONGE) ||
                                    state.isOf(Blocks.SCULK_CATALYST) || state.isOf(Blocks.SCULK_SHRIEKER) ||
                                    state.isOf(Blocks.SCULK_SENSOR)) {
                                baseAmount *= 3;
                            }
                        }
                        // 2. Colheita Farta (Plantações e Frutos)
                        else if (isCrop) {
                            specificAffinity = "bountiful_harvest";
                            specificAmount = 3;
                        }

                        // 🌟 MANTEMOS TRUE! Assim, quebrar folhas ou plantações sempre dá XP Base para a enxada,
                        // não importa qual seja a afinidade dela no Nível 1+.
                        ARPGXpHelper.grantXp(mainHand, specificAffinity, specificAmount, true, baseAmount, serverPlayer);

                        // 3. Braço Longo (Distância máxima)
                        double distance = serverPlayer.getEyePos().distanceTo(pos.toCenterPos());
                        if (distance >= 4.0) {
                            ARPGXpHelper.grantXp(mainHand, "long_reach", 4, false, 0, serverPlayer);
                        }
                    }
                }

                // (Futuramente adicionaremos a Pá e a Enxada aqui embaixo)
            }
        });


        // 🌊 2. EVENTO DE TICK DO SERVIDOR
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {

                // ⏱️ A cada 3 segundo (60 ticks)
                if (serverPlayer.age % 60 == 0) {
                    ItemStack mainHand = serverPlayer.getMainHandStack();

                    if (mainHand.isOf(Items.FISHING_ROD)) {
                        if (serverPlayer.isSwimming()) {
                            // 🎣 VARA: Nado Rápido (3 XP) + Ação Base (1 XP)
                            // Se a vara já estiver despertada com outra afinidade, ela ganha 1 XP por nadar!
                            ARPGXpHelper.grantXp(mainHand, "aquatic", 3, true, 1, serverPlayer);
                        } else if (serverPlayer.isSubmergedInWater()) {
                            // 🎣 VARA: Pulmões de Aço (3 XP)
                            ARPGXpHelper.grantXp(mainHand, "oxygen", 3, false, 0, serverPlayer);
                        }
                    }
                }

                // ⏱️ A cada 2 segundos (40 ticks)
                if (serverPlayer.age % 40 == 0) {

                    // 🪖 CAPACETE: Ventos Guias (Correr)
                    ItemStack helmet = serverPlayer.getEquippedStack(EquipmentSlot.HEAD);
                    if (!helmet.isEmpty() && serverPlayer.isSprinting()) {
                        ARPGXpHelper.grantXp(helmet, "guiding_winds", 3, false, 0, serverPlayer);
                    }

                    // 👖 CALÇAS: Mobilidade e Furtividade
                    ItemStack leggings = serverPlayer.getEquippedStack(EquipmentSlot.LEGS);
                    if (!leggings.isEmpty()) {
                        // Consideramos que está se movendo se estiver com velocidade horizontal, correndo ou agachando
                        boolean isMoving = serverPlayer.getVelocity().horizontalLengthSquared() > 0.0001 || serverPlayer.isSprinting() || serverPlayer.isSneaking();

                        if (isMoving) {
                            String specificAffinity = null;
                            int specificAmount = 0;

                            // 1. Sombras Furtivas (Agachar perto de monstros)
                            if (serverPlayer.isSneaking()) {
                                boolean hasMonsters = !serverPlayer.getEntityWorld().getEntitiesByClass(
                                        net.minecraft.entity.mob.HostileEntity.class,
                                        serverPlayer.getBoundingBox().expand(10.0),
                                        e -> true
                                ).isEmpty();

                                if (hasMonsters) {
                                    specificAffinity = "swift_shadows";
                                    specificAmount = 3;
                                }
                            }
                            // 🌟 2. Andarilho (Caminhos de Terra - CORRIGIDO)
                            else {
                                // Pegamos a posição exata do jogador e a posição abaixo dele
                                net.minecraft.util.math.BlockPos pos = serverPlayer.getBlockPos();
                                net.minecraft.block.BlockState stateAtFeet = serverPlayer.getEntityWorld().getBlockState(pos);
                                net.minecraft.block.BlockState stateBelow = serverPlayer.getEntityWorld().getBlockState(pos.down());

                                // Checamos se o caminho de terra está no pé dele (bloco incompleto) OU embaixo dele (caso ele esteja pulando baixinho)
                                if (stateAtFeet.isOf(Blocks.DIRT_PATH) || stateBelow.isOf(Blocks.DIRT_PATH) || stateAtFeet.isOf(Blocks.SOUL_SAND)) {
                                    specificAffinity = "wayfarer";
                                    specificAmount = 3;
                                }
                                // 3. Passos Largos (Correr em terreno irregular - pulando/caindo)
                                else if (serverPlayer.isSprinting() && !serverPlayer.isOnGround()) {
                                    specificAffinity = "long_strides";
                                    specificAmount = 3;
                                }
                                // 4. Vigor Inabalável (Viajar longas distâncias - Correndo normal)
                                else if (serverPlayer.isSprinting()) {
                                    specificAffinity = "unyielding_vigor";
                                    specificAmount = 3;
                                }
                            }

                            // Ação Base: 1 XP por estar se movendo
                            ARPGXpHelper.grantXp(leggings, specificAffinity, specificAmount, false, 0, serverPlayer);
                        }
                    }

                    // 🥾 BOTAS: Passos e Montanhas
                    ItemStack boots = serverPlayer.getEquippedStack(EquipmentSlot.FEET);
                    if (!boots.isEmpty()) {
                        // 1. Passos Leves (Correr)
                        if (serverPlayer.isSprinting()) {
                            ARPGXpHelper.grantXp(boots, "light_steps", 3, false, 0, serverPlayer);
                        }
                        // 2. Caminhante das Montanhas (Escalar escadas/vinhas)
                        else if (serverPlayer.isClimbing()) {
                            ARPGXpHelper.grantXp(boots, "mountain_walker", 3, false, 0, serverPlayer);
                        }
                    }
                }
            }
        });
    }
}