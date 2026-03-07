package draylar.tiered.util;

import draylar.tiered.api.ARPGEquipmentData;
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
    public static void addXp(ItemStack stack, String xpType, int amount, ServerPlayerEntity player) {
        ARPGEquipmentData data = stack.get(TieredDataComponents.ARPG_DATA);

        // Se não tem alma ou está quebrada, não ganha XP
        if (data == null || data.isBroken()) return;

        int currentLevel = data.level();
        int prestige = data.prestige();

        // Se já está no nível máximo, não faz nada
        if (currentLevel >= ARPGLevelingLogic.getMaxLevel()) return;

        int requiredXp = ARPGLevelingLogic.getRequiredXpForNextLevel(currentLevel, prestige);

        if (currentLevel == 0) {
            // 💤 --- FASE DE TREINAMENTO (NÍVEL 0) ---

            // Cria um mapa mutável a partir do mapa imutável do Record
            Map<String, Integer> trainingXp = new HashMap<>(data.trainingXp());
            trainingXp.put(xpType, trainingXp.getOrDefault(xpType, 0) + amount);

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

                // Efeitos Épicos para o Jogador
                player.sendMessage(Text.translatable("tiered.arpg.message.awaken").formatted(Formatting.GOLD, Formatting.BOLD), true);
                player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
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

            int newCurrentXp = data.currentXp() + amount;

            if (newCurrentXp >= requiredXp) {
                // 🌟 LEVEL UP!
                int newLevel = currentLevel + 1;
                int leftoverXp = newCurrentXp - requiredXp; // Guarda o XP que sobrou

                ARPGEquipmentData newData = new ARPGEquipmentData(
                        newLevel, leftoverXp, prestige, data.affinity(),
                        data.trainingXp(), data.maxSlots(), data.slots(), false
                );
                stack.set(TieredDataComponents.ARPG_DATA, newData);

                // Efeitos de Level Up
                player.sendMessage(Text.translatable("tiered.arpg.message.levelup", newLevel).formatted(Formatting.GREEN), true);
                player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
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
}