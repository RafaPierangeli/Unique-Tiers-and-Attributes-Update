package draylar.tiered.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ARPGAffinityLogic {

    public static final DecimalFormat FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    // 🎨 1. O Sistema de Cores Psicológicas
    public static Formatting getAffinityColor(String affinity) {
        return switch (affinity) {
            case "damage", "ranged_damage", "lethal", "critical" -> Formatting.RED;
            case "agile", "light", "speed" -> Formatting.AQUA;
            case "mining", "fortune", "interaction" -> Formatting.GOLD;
            case "vital", "resistance" -> Formatting.WHITE;
            case "aquatic", "oxygen", "luck_of_sea", "lure" -> Formatting.BLUE;
            default -> Formatting.GRAY;
        };
    }

    // 🧮 2. A Matemática do Prestígio
    public static double getBonusValue(String affinity, int level, int prestige) {
        // O multiplicador base é 1.0 (Sem bônus extra nos prestígios 0, 1 e 2)
        double prestigeMultiplier = 1.0;
        double absoluteBonus = 0.0;

        // 🌟 A Recompensa Máxima: Só ganha multiplicador de status no Prestígio 3!
        if (prestige >= 3) {
            prestigeMultiplier = 1.5; // 50% a mais de status base!

            // Se o maluco chegou no Nível 100 com Prestígio 3 (O Deus do Grind)
            if (level >= ARPGLevelingLogic.getMaxLevel()) {
                absoluteBonus = switch (affinity) {
                    case "damage", "ranged_damage" -> 5.0; // +5 de Dano Fixo Final
                    case "critical" -> 10.0; // +10% de Chance de Crítico Final
                    case "mining" -> 5.0; // +5 de Velocidade de Mineração Final
                    case "vital" -> (5.0); // +5 de vida por Final
                    case "resistance" -> (6.0); // +6 de armadura Final
                    default -> 0.0;
                };
            }
        }

        double baseValue = switch (affinity) {
            case "damage", "ranged_damage" -> (level * 0.25);
            case "mining" -> (level * 0.5);
            case "agile" -> (level * 0.02);
            case "light", "speed" -> (level * 0.01);
            case "lethal" -> (level * 0.05);
            case "critical" -> (level * 1.0);
            case "vital" -> (level * 0.5); // +1 de vida por nível
            case "resistance" -> (level * 0.1); // +0.2 de armadura por nível
            case "aquatic", "lure" -> (level * 0.05);
            case "oxygen", "luck_of_sea", "fortune" -> (level * 1.0);
            case "interaction" -> (level * 0.1); // +0.1 blocos de alcance por nível
            default -> 0.0;
        };

        return (baseValue * prestigeMultiplier) + absoluteBonus;
    }

    // 📝 3. O Texto da Tooltip
    public static MutableText getAffinityBonusText(String affinity, int level, int prestige) {
        double bonusValue = getBonusValue(affinity, level, prestige);

        if (bonusValue <= 0) {
            return Text.literal("");
        }

        // Pega a cor correta para pintar o bônus também!
        Formatting color = getAffinityColor(affinity);

        return Text.literal(" (+")
                .append(Text.literal(FORMAT.format(bonusValue)))
                .append(Text.literal(" "))
                .append(Text.translatable("tiered.arpg.bonus." + affinity))
                .append(Text.literal(")"))
                .formatted(color); // Pinta o bônus com a cor da afinidade
    }
}