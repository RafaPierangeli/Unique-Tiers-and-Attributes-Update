package draylar.tiered.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ARPGAffinityLogic {

    public static final DecimalFormat FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    /**
     * Calcula o valor numérico do bônus com base no nível e prestígio.
     * Mais tarde, usaremos esse mesmo método para injetar os atributos reais na arma!
     */
    public static double getBonusValue(String affinity, int level, int prestige) {
        // O Prestígio aumenta o bônus final! (Ex: Prestige 1 = +10% de status)
        double prestigeMultiplier = 1.0 + (prestige * 0.1);

        return switch (affinity) {
            case "damage", "ranged_damage", "mining" -> (level * 0.5) * prestigeMultiplier; // +0.5 por nível
            case "agile" -> (level * 0.02) * prestigeMultiplier; // +0.02 de Vel. Ataque por nível
            case "light" -> (level * 0.01) * prestigeMultiplier; // +0.01 de Vel. Movimento por nível
            case "lethal" -> (level * 0.05) * prestigeMultiplier; // +0.05x de Multiplicador Crítico por nível
            case "critical" -> (level * 1.0) * prestigeMultiplier; // +1% de Chance de Crítico por nível
            default -> 0.0;
        };
    }

    /**
     * Gera o texto formatado para a Tooltip: ex: "(+2.5 Dano)"
     */
    public static MutableText getAffinityBonusText(String affinity, int level, int prestige) {
        double bonusValue = getBonusValue(affinity, level, prestige);

        if (bonusValue <= 0) {
            return Text.literal(""); // Se não tem bônus, não mostra nada
        }

        // Monta o texto: " (+2.5 Dano)"
        return Text.literal(" (+")
                .append(Text.literal(FORMAT.format(bonusValue)))
                .append(Text.literal(" "))
                .append(Text.translatable("tiered.arpg.bonus." + affinity))
                .append(Text.literal(")"))
                .formatted(Formatting.DARK_GREEN); // Verde escuro para destacar que é um bônus positivo
    }
}