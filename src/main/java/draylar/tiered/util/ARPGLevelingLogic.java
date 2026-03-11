package draylar.tiered.util;

import draylar.tiered.config.ConfigInit; // 🌟 Não esqueça de importar a sua config!

public class ARPGLevelingLogic {

    public static int getBaseXp() { return 100; }
    public static double getXpMultiplier() { return 0.025; }

    // 🌟 AGORA É DINÂMICO! Puxa direto da configuração do jogador
    public static int getMaxLevel() {
        // Prevenção de segurança: garante que o nível máximo nunca seja menor que 1
        return Math.max(1, ConfigInit.CONFIG.maxLevelEquipment);
    }

    public static int getMaxPrestige() { return 3; }

    /**
     * Calcula o XP necessário para o PRÓXIMO nível, considerando o Prestígio.
     */
    public static int getRequiredXpForNextLevel(int currentLevel, int prestige) {
        if (currentLevel == 0) {
            return getBaseXp();
        }

        if (currentLevel >= getMaxLevel()) {
            return Integer.MAX_VALUE;
        }

        int baseForLevel = (int) Math.round(getBaseXp() * Math.pow(1.0 + getXpMultiplier(), currentLevel));
        int prestigeMultiplier = (int) Math.pow(2, prestige);

        return baseForLevel * prestigeMultiplier;
    }
}