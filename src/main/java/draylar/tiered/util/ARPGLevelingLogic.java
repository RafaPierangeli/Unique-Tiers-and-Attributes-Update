package draylar.tiered.util;

import draylar.tiered.config.ConfigInit;

public class ARPGLevelingLogic {

    int maxlvl = ConfigInit.CONFIG.maxLevelEquipment;

    public static int getBaseXp() { return 100; }
    public static double getXpMultiplier() { return 0.05; }
    public static int getMaxLevel() { return ConfigInit.CONFIG.maxLevelEquipment; } // 🌟 Atualizado para chave dinamica. Padrao 100!
    public static int getMaxPrestige() { return 3; } // 🌟 Limite de Prestígio

    /**
     * Calcula o XP necessário para o PRÓXIMO nível, considerando o Prestígio.
     */
    public static int getRequiredXpForNextLevel(int currentLevel, int prestige) {
        if (currentLevel == 0) {
            return getBaseXp(); // Nível 0 para 1 sempre é 100 XP
        }

        if (currentLevel >= getMaxLevel()) {
            return Integer.MAX_VALUE; // Nível máximo
        }

        // Fórmula base: Base * (1 + Multiplicador)^Nível
        int baseForLevel = (int) Math.round(getBaseXp() * Math.pow(1.0 + getXpMultiplier(), currentLevel));

        // 🌟 O Fator Prestígio: Dobra o XP necessário a cada reset!
        // Prestige 0 = x1 | Prestige 1 = x2 | Prestige 2 = x4 | Prestige 3 = x8
        int prestigeMultiplier = (int) Math.pow(2, prestige);

        return baseForLevel * prestigeMultiplier;
    }
}