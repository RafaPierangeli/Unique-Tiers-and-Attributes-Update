package draylar.tiered.util;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

public class ARPGXpHelper {

    /**
     * Método central para dar XP a qualquer equipamento.
     * @param stack O item que vai receber o XP
     * @param xpType O tipo de XP (ex: "damage", "mining", "resistance")
     * @param amount A quantidade de XP
     * @param player O jogador (para tocarmos sons e mandarmos mensagens)
     */

    // 🌟 Rastreador de Combate: Guarda o último tick em que o jogador causou dano
    public static final java.util.Map<java.util.UUID, Long> lastDamageTick = new java.util.HashMap<>();

    public static void addXp(ItemStack stack, String xpType, int amount, ServerPlayerEntity player) {
        ARPGEquipmentData data = stack.get(TieredDataComponents.ARPG_DATA);

        // Se não tem alma ou está quebrada, não ganha XP
        if (data == null || data.isBroken()) return;

        // 🌟 1. APLICA O MULTIPLICADOR GLOBAL DA CONFIGURAÇÃO
        float multipliedAmount = amount * ConfigInit.CONFIG.globalXpMultiplier;
        int finalAmount = (int) multipliedAmount;

        // 🌟 2. O SEGREDO DO XP FRACIONADO
        // Se o multiplicador for 0.5 e o amount for 1, finalAmount seria 0.
        // Para não perder esse XP, nós usamos a sobra decimal como "chance" de ganhar 1 XP!
        float remainder = multipliedAmount - finalAmount;
        if (remainder > 0 && player.getRandom().nextFloat() < remainder) {
            finalAmount += 1;
        }

        // Se após os cálculos o XP for 0, aborta para economizar processamento
        if (finalAmount <= 0) return;

        int currentLevel = data.level();
        int prestige = data.prestige();

        // Se já está no nível máximo, não faz nada
        if (currentLevel >= ARPGLevelingLogic.getMaxLevel()) return;

        int requiredXp = ARPGLevelingLogic.getRequiredXpForNextLevel(currentLevel, prestige);

        if (currentLevel == 0) {
            // 💤 --- FASE DE TREINAMENTO (NÍVEL 0) ---

            // Cria um mapa mutável a partir do mapa imutável do Record
            Map<String, Integer> trainingXp = new HashMap<>(data.trainingXp());
            trainingXp.put(xpType, trainingXp.getOrDefault(xpType, 0) + finalAmount);

            // Calcula o XP total somando todos os valores do mapa
            int totalXp = trainingXp.values().stream().mapToInt(Integer::intValue).sum();

            if (totalXp >= requiredXp) {
                // 🔥 DESPERTAR! (Nível 0 -> 1)
                String dominantAffinity = xpType; // Fallback
                int max = 0;

                // Descobre qual foi o XP mais treinado para definir a Afinidade
                for (Map.Entry<String, Integer> entry : trainingXp.entrySet()) {
                    if (entry.getValue() > max) {
                        max = entry.getValue();
                        dominantAffinity = entry.getKey();
                    }
                }

                // Cria a nova alma Nível 1, limpa o mapa de treino e define a afinidade
                ARPGEquipmentData newData = new ARPGEquipmentData(
                        1, 0, prestige, dominantAffinity,
                        Map.of(), // Mapa de treino volta a ficar vazio
                        data.maxSlots(), data.slots(), false
                );
                stack.set(TieredDataComponents.ARPG_DATA, newData);

                // Efeitos Épicos para o Jogador (Agora com o nome do item!)
                player.sendMessage(Text.translatable("tiered.arpg.message.awaken", stack.getName()).formatted(Formatting.GOLD), true);
                player.getEntityWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
            } else {
                // Apenas salva o novo XP no Nível 0
                ARPGEquipmentData newData = new ARPGEquipmentData(
                        0, 0, prestige, "unawakened",
                        trainingXp,
                        data.maxSlots(), data.slots(), false
                );
                stack.set(TieredDataComponents.ARPG_DATA, newData);
            }
        } else {
            // ⚔️ --- FASE DE EVOLUÇÃO (NÍVEL 1+) ---

            int newCurrentXp = data.currentXp() + finalAmount;

            if (newCurrentXp >= requiredXp) {
                // 🌟 LEVEL UP!
                int newLevel = currentLevel + 1;
                int leftoverXp = newCurrentXp - requiredXp; // Guarda o XP que sobrou

                ARPGEquipmentData newData = new ARPGEquipmentData(
                        newLevel, leftoverXp, prestige, data.affinity(),
                        data.trainingXp(), data.maxSlots(), data.slots(), false
                );
                stack.set(TieredDataComponents.ARPG_DATA, newData);

                // Efeitos de Level Up (Agora com o nome do item e o nível!)
                player.sendMessage(Text.translatable("tiered.arpg.message.levelup", stack.getName(), newLevel).formatted(Formatting.GREEN), true);
                player.getEntityWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
            } else {
                // Apenas salva o novo XP
                ARPGEquipmentData newData = new ARPGEquipmentData(
                        currentLevel, newCurrentXp, prestige, data.affinity(),
                        data.trainingXp(), data.maxSlots(), data.slots(), false
                );
                stack.set(TieredDataComponents.ARPG_DATA, newData);
            }
        }
    }

    /**
     * Novo método inteligente para lidar com Ações Específicas vs Ações Base.
     * @param actionAffinity A afinidade que esta ação treina (ex: "focused_mind")
     * @param actionAmount O XP ganho se a arma for dessa afinidade (ex: 3)
     * @param isBaseAction Se essa ação também serve como XP base para qualquer afinidade
     * @param baseAmount O XP ganho se for ação base e a arma for de outra afinidade (ex: 1)
     */
    public static void grantXp(ItemStack stack, String actionAffinity, int actionAmount, boolean isBaseAction, int baseAmount, ServerPlayerEntity player) {
        ARPGEquipmentData data = stack.get(TieredDataComponents.ARPG_DATA);
        if (data == null || data.isBroken()) return;

        if (data.level() == 0) {
            // Fase de Treinamento: Só ganha XP se a ação tiver uma afinidade específica
            if (actionAffinity != null && actionAmount > 0) {
                addXp(stack, actionAffinity, actionAmount, player);
            }
        } else {
            // Fase de Evolução (Nível 1+)
            int xpToGive = 0;

            // Se a ação que o jogador fez é exatamente a afinidade da arma, ganha o bônus!
            if (actionAffinity != null && actionAffinity.equals(data.affinity())) {
                xpToGive = actionAmount;
            }
            // Se não for a afinidade dela, mas for uma Ação Base, ganha o XP base
            else if (isBaseAction) {
                xpToGive = baseAmount;
            }

            if (xpToGive > 0) {
                // Injeta o XP usando a afinidade atual da arma para não sobrescrever
                addXp(stack, data.affinity(), xpToGive, player);
            }
        }
    }
}