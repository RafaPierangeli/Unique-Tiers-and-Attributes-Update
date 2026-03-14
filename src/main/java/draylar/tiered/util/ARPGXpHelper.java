package draylar.tiered.util;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

public class ARPGXpHelper {

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
        float remainder = multipliedAmount - finalAmount;
        if (remainder > 0 && player.getRandom().nextFloat() < remainder) {
            finalAmount += 1;
        }

        if (finalAmount <= 0) return;

        int currentLevel = data.level();
        int prestige = data.prestige();
        int maxLevel = ARPGLevelingLogic.getMaxLevel();

        // Se já está no nível máximo, não faz nada
        if (currentLevel >= maxLevel) return;

        int requiredXp = ARPGLevelingLogic.getRequiredXpForNextLevel(currentLevel, prestige);

        boolean awakenedNow = false;
        boolean leveledUp = false;

        int currentXp = data.currentXp();
        Map<String, Integer> trainingXp = new HashMap<>(data.trainingXp());
        String affinity = data.affinity();

        // 💤 --- FASE DE TREINAMENTO (NÍVEL 0) ---
        if (currentLevel == 0) {
            trainingXp.put(xpType, trainingXp.getOrDefault(xpType, 0) + finalAmount);
            int totalXp = trainingXp.values().stream().mapToInt(Integer::intValue).sum();

            if (totalXp >= requiredXp) {
                // 🔥 DESPERTAR!
                awakenedNow = true;
                currentLevel = 1;
                currentXp = totalXp - requiredXp; // 🌟 Salva o XP que sobrou para a Fase de Evolução!

                String dominantAffinity = xpType;
                int max = 0;

                for (Map.Entry<String, Integer> entry : trainingXp.entrySet()) {
                    if (entry.getValue() > max) {
                        max = entry.getValue();
                        dominantAffinity = entry.getKey();
                    }
                }

                affinity = dominantAffinity;
                trainingXp = Map.of(); // Limpa o mapa de treino
                requiredXp = ARPGLevelingLogic.getRequiredXpForNextLevel(currentLevel, prestige); // Recalcula para o Nível 1
            } else {
                // Não despertou ainda, apenas salva o treino e sai
                ARPGEquipmentData newData = new ARPGEquipmentData(
                        0, 0, prestige, "unawakened",
                        trainingXp, data.maxSlots(), data.slots(), false
                );
                stack.set(TieredDataComponents.ARPG_DATA, newData);
                return;
            }
        } else {
            // ⚔️ --- FASE DE EVOLUÇÃO (NÍVEL 1+) ---
            currentXp += finalAmount;
        }

        // 🌟 A MÁGICA DA CASCATA: O 'while' consome o XP até acabar ou bater no nível máximo!
        while (currentXp >= requiredXp && currentLevel < maxLevel) {
            currentXp -= requiredXp; // Paga o custo do nível
            currentLevel++;          // Sobe de nível
            leveledUp = true;

            // Recalcula o custo para o NOVO nível
            requiredXp = ARPGLevelingLogic.getRequiredXpForNextLevel(currentLevel, prestige);
        }

        // Trava de segurança: Se bateu no nível máximo, zera o XP excedente
        if (currentLevel >= maxLevel) {
            currentXp = 0;
        }

        // 🌟 SALVA OS DADOS FINAIS
        ARPGEquipmentData newData = new ARPGEquipmentData(
                currentLevel, currentXp, prestige, affinity,
                trainingXp, data.maxSlots(), data.slots(), false
        );
        stack.set(TieredDataComponents.ARPG_DATA, newData);
        // 🌟 ATUALIZA OS ATRIBUTOS FÍSICOS DA ARMA
        // O Minecraft vai detectar que o DataComponent mudou e vai atualizar a força do jogador no próximo tick!
        ARPGAttributeHelper.updateModifiers(stack);

        // 🌟 FEEDBACK VISUAL E SONORO
        if (awakenedNow) {
            player.sendMessage(Text.translatable("tiered.arpg.message.awaken", stack.getName()).formatted(Formatting.GOLD), true);
            player.getEntityWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else if (leveledUp) {
            // Mostra o nível final que a arma alcançou após a cascata
            player.sendMessage(Text.translatable("tiered.arpg.message.levelup", stack.getName(), currentLevel).formatted(Formatting.GREEN), true);
            player.getEntityWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
        }
    }

    public static void grantXp(ItemStack stack, String actionAffinity, int actionAmount, boolean isBaseAction, int baseAmount, ServerPlayerEntity player) {
        ARPGEquipmentData data = stack.get(TieredDataComponents.ARPG_DATA);
        if (data == null || data.isBroken()) return;

        if (data.level() == 0) {
            if (actionAffinity != null && actionAmount > 0) {
                addXp(stack, actionAffinity, actionAmount, player);
            }
        } else {
            int xpToGive = 0;
            if (actionAffinity != null && actionAffinity.equals(data.affinity())) {
                xpToGive = actionAmount;
            } else if (isBaseAction) {
                xpToGive = baseAmount;
            }

            if (xpToGive > 0) {
                addXp(stack, data.affinity(), xpToGive, player);
            }
        }
    }
}