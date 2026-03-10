package draylar.tiered.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ARPGAffinityLogic {

    public static final DecimalFormat FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    // 🎨 1. O Sistema de Cores Psicológicas (Limpo: Apenas Nomes Oficiais)
    public static Formatting getAffinityColor(String affinity) {
        return switch (affinity) {

            // ⚔️ 1. OFENSIVO (Vermelho)
            case "brute_force", "true_strike", "bloodthirst", "retaliation", "focused_mind", "spiked_vengeance"
                    -> Formatting.RED;

            // 🛡️ 2. DEFENSIVO (Aqua)
            case "bulwark", "solid_foundation", "vital_guard", "wall", "immovable", "titan_heart", "unyielding_vigor", "life_blessing", "winds_of_life"
                    -> Formatting.AQUA;

            // 🥾 3. MOBILIDADE (Verde Escuro)
            case "dancing_blade", "guiding_winds", "wayfarer", "swift_shadows", "long_strides", "light_steps", "aerial_boost", "mountain_walker", "gale", "acrobat"
                    -> Formatting.DARK_GREEN;

            // ⛏️ 4. MINERAÇÃO / ESCAVAÇÃO / INTERAÇÃO (Amarelo)
            case "hard_labor", "voracious_digger", "long_reach", "far_sight", "earth_reach", "soil_master", "bountiful_harvest"
                    -> Formatting.YELLOW;

            // 🌊 5. RELACIONADO A ÁGUA (Azul)
            case "aquatic", "oxygen"
                    -> Formatting.BLUE;

            // ✨ 6. EXPERIÊNCIA E SORTE (Verde Claro)
            case "ancient_wisdom", "midas_touch", "hidden_treasures", "demeter_blessing", "luck_of_sea", "lure"
                    -> Formatting.GREEN;

            default -> Formatting.GRAY;
        };
    }

    // 🧮 2. A Matemática do Prestígio (Limpa: Apenas Nomes Oficiais)
    public static double getBonusValue(String affinity, int level, int prestige) {
        double prestigeMultiplier = 1.0;
        double absoluteBonus = 0.0;

        if (prestige >= 3) {
            prestigeMultiplier = 1.5;

            if (level >= ARPGLevelingLogic.getMaxLevel()) {
                absoluteBonus = switch (affinity) {
                    case "brute_force", "retaliation", "focused_mind" -> 5.0;
                    case "true_strike", "ancient_wisdom", "life_blessing", "bloodthirst" -> 10.0;
                    case "hard_labor", "voracious_digger" -> 5.0;
                    case "titan_heart", "unyielding_vigor", "vital_guard", "winds_of_life" -> 5.0;
                    case "bulwark", "solid_foundation", "wall" -> 6.0;
                    case "midas_touch", "hidden_treasures", "demeter_blessing", "oxygen", "luck_of_sea" -> 10.0;
                    case "long_reach", "far_sight", "earth_reach" -> 2.0;
                    case "spiked_vengeance" -> 5.0;
                    default -> 0.0;
                };
            }
        }

        double baseValue = switch (affinity) {
            case "brute_force", "retaliation", "focused_mind", "spiked_vengeance", "heavy_shot", "elite_shooter" -> (level * 0.25);
            case "hard_labor", "voracious_digger" -> (level * 0.5);
            case "dancing_blade", "aerial_boost", "acrobat", "mountain_walker", "long_strides", "wayfarer", "swift_shadows", "immovable", "gale", "quick_draw", "eagle_eye" -> (level * 0.02);
            case "guiding_winds", "light_steps" -> (level * 0.001); // Velocidade de movimento é muito sensível
            case "true_strike", "ancient_wisdom", "life_blessing", "oxygen", "luck_of_sea", "midas_touch", "hidden_treasures", "demeter_blessing" -> (level * 1.0);
            // 🌟 1. Deixe o Vigor Inabalável (Fome) sozinho no 0.5
            case "unyielding_vigor" -> (level * 0.5);
            // 🌟 2. Crie uma nova linha para a Sede de Sangue com 0.1 (Máximo de 10% no Nível 100 base)
            case "bloodthirst" -> (level * 0.2);
            case "titan_heart", "vital_guard", "winds_of_life" -> (level * 0.5);
            case "bulwark", "solid_foundation", "wall" -> (level * 0.1);
            case "aquatic", "lure", "soil_master", "bountiful_harvest" -> (level * 0.05);
            case "long_reach", "far_sight", "earth_reach" -> (level * 0.1);
            default -> 0.0;
        };

        return (baseValue * prestigeMultiplier) + absoluteBonus;
    }

    // 📝 3. O Texto da Tooltip (Agora com a cor dinâmica!)
    public static MutableText getAffinityBonusText(String affinity, int level, int prestige) {
        double bonusValue = getBonusValue(affinity, level, prestige);

        if (bonusValue <= 0) {
            return Text.literal("");
        }

        boolean isPercentage = switch (affinity) {
            case "true_strike", "ancient_wisdom", "life_blessing", "bloodthirst", "unyielding_vigor" -> true;
            default -> false;
        };

        String formattedValue = isPercentage ?
                FORMAT.format(bonusValue) + "%" :
                FORMAT.format(bonusValue);

        String translationKey = switch (affinity) {
            case "brute_force", "focused_mind", "retaliation" -> "tiered.arpg.bonus.damage";
            case "dancing_blade" -> "tiered.arpg.bonus.attack_speed";
            case "true_strike" -> "tiered.arpg.bonus.crit_chance";
            case "bloodthirst" -> "tiered.arpg.bonus.lifesteal";
            case "ancient_wisdom" -> "tiered.arpg.bonus.xp_increase";
            case "guiding_winds", "light_steps" -> "tiered.arpg.bonus.speed";
            case "far_sight", "long_reach", "earth_reach" -> "tiered.arpg.bonus.interaction";
            case "titan_heart", "vital_guard", "winds_of_life" -> "tiered.arpg.bonus.vital";
            case "unyielding_vigor" -> "tiered.arpg.bonus.stamina";
            case "bulwark", "solid_foundation", "wall" -> "tiered.arpg.bonus.resistance";
            case "life_blessing" -> "tiered.arpg.bonus.healing_increase";
            case "aerial_boost", "acrobat" -> "tiered.arpg.bonus.jump_height";
            case "mountain_walker", "long_strides" -> "tiered.arpg.bonus.step_height";
            case "wayfarer" -> "tiered.arpg.bonus.path_speed";
            case "swift_shadows" -> "tiered.arpg.bonus.swift_sneak";
            case "immovable" -> "tiered.arpg.bonus.knockback_resistance";
            case "spiked_vengeance" -> "tiered.arpg.bonus.thorns";
            case "hard_labor" -> "tiered.arpg.bonus.mining";
            case "midas_touch", "hidden_treasures", "demeter_blessing" -> "tiered.arpg.bonus.fortune";
            case "voracious_digger" -> "tiered.arpg.bonus.excavation";
            case "soil_master" -> "tiered.arpg.bonus.area_tilling";
            case "bountiful_harvest" -> "tiered.arpg.bonus.area_harvesting";
            case "aquatic" -> "tiered.arpg.bonus.aquatic";
            case "oxygen" -> "tiered.arpg.bonus.oxygen";
            case "luck_of_sea" -> "tiered.arpg.bonus.luck_of_sea";
            case "lure" -> "tiered.arpg.bonus.lure";
            case "gale" -> "tiered.arpg.bonus.flight_speed";
            case "quick_draw" -> "tiered.arpg.bonus.quick_draw";
            case "eagle_eye" -> "tiered.arpg.bonus.eagle_eye";
            case "heavy_shot" -> "tiered.arpg.bonus.heavy_shot";
            case "elite_shooter" -> "tiered.arpg.bonus.elite_shooter";
            default -> "tiered.arpg.bonus.unknown";
        };

        // 🌟 Pega a cor da afinidade e pinta o texto do bônus com ela!
        Formatting color = getAffinityColor(affinity);

        return Text.literal("+" + formattedValue + " ")
                .append(Text.translatable(translationKey))
                .formatted(color);
    }
}