package draylar.tiered.client;

import draylar.tiered.Tiered;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.config.AttributeColorMode;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TooltipDisplayMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@Environment(EnvType.CLIENT)
public class TieredTooltipCallback {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private static String[] extractIconAndName(String translationKey) {
        String rawTranslated = Language.getInstance().get(translationKey);
        if (rawTranslated == null) rawTranslated = translationKey;

        String cleanTranslated = rawTranslated.replaceAll("§[0-9a-fk-or]", "");

        for (int i = 0; i < cleanTranslated.length(); ) {
            int cp = cleanTranslated.codePointAt(i);
            if ((cp >= 0xE000 && cp <= 0xF8FF) || (cp >= 0xF900 && cp <= 0xFAFF) ||
                    (cp >= 0x1CD00 && cp <= 0x1CDFF) || (cp >= 0x1FB00 && cp <= 0x1FBFF) ||
                    (cp >= 0xF0000 && cp <= 0xFFFFD) || (cp >= 0x100000 && cp <= 0x10FFFD)) {

                int len = Character.charCount(cp);
                String icon = new String(Character.toChars(cp));
                String nameWithoutIcon = cleanTranslated.substring(i + len).trim();
                return new String[]{icon, nameWithoutIcon};
            }
            i += Character.charCount(cp);
        }
        return new String[]{"", cleanTranslated};
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {

            if (stack.get(Tiered.TIER) != null) {
                String rawTierId = stack.get(Tiered.TIER).tier();
                Identifier tierId = Identifier.of(rawTierId);

                PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);

                if (potentialAttribute != null) {

                    TooltipDisplayMode displayMode = ConfigInit.CONFIG.uniqueTooltipMode;

                    if (displayMode == TooltipDisplayMode.OFF) {
                        return;
                    }

                    // Verifica os mods carregados no início
                    boolean hasDynamicTooltip = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("dynamictooltips");
                    boolean isBetterCombatLoaded = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bettercombat");

                    if (displayMode == TooltipDisplayMode.ON_SHIFT) {
                        net.minecraft.client.util.Window window = MinecraftClient.getInstance().getWindow();

                        boolean isShiftDown = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                                InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
                        boolean isCtrlDown = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) ||
                                InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

                        // Lógica de compatibilidade: Se Dynamic Tooltips estiver presente, exige CTRL. Senão, exige SHIFT.
                        boolean shouldShow = hasDynamicTooltip ? isCtrlDown : isShiftDown;

                        if (!shouldShow) {
                            lines.add(Text.empty());
                            if (hasDynamicTooltip) {
                                // Lembre-se de adicionar "tiered.tooltip.press_ctrl" no seu pt_br.json / en_us.json
                                lines.add(Text.translatable("tiered.tooltip.press_ctrl").formatted(Formatting.DARK_GRAY));
                            } else {
                                lines.add(Text.translatable("tiered.tooltip.press_shift").formatted(Formatting.DARK_GRAY));
                            }
                            return;
                        }
                    }

                    // 1. Preservar Nome, Lore e Encantamentos (Agora roda sempre, independente do Dynamic Tooltips)
                    List<Text> preservedLines = new ArrayList<>();
                    for (Text line : lines) {
                        if (line.getContent() instanceof TranslatableTextContent translatable) {
                            if (translatable.getKey().startsWith("item.modifiers.")) {
                                break;
                            }
                        }
                        preservedLines.add(line);
                    }
                    lines.clear();
                    lines.addAll(preservedLines);

                    String attrMargin = "";
                    AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);

                    // 🌟 PASSO 1: CÁLCULO DOS TOTAIS (Matemática exata do Minecraft)
                    double baseDamage = 1.0; // Dano base do jogador com a mão vazia
                    double baseSpeed = 4.0;  // Velocidade base do jogador com a mão vazia

                    double damageAdd = 0.0;
                    double speedAdd = 0.0;

                    double damageMultBase = 0.0;
                    double speedMultBase = 0.0;

                    double damageMultTotal = 1.0;
                    double speedMultTotal = 1.0;

                    if (modifiers != null) {
                        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                            Identifier attrId = Registries.ATTRIBUTE.getId(entry.attribute().value());
                            if (attrId != null) {
                                String path = attrId.getPath();
                                double val = entry.modifier().value();
                                EntityAttributeModifier.Operation op = entry.modifier().operation();

                                if (path.equals("attack_damage")) {
                                    if (op == EntityAttributeModifier.Operation.ADD_VALUE) {
                                        damageAdd += val;
                                    } else if (op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                                        damageMultBase += val;
                                    } else if (op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                        damageMultTotal *= (1.0 + val);
                                    }
                                } else if (path.equals("attack_speed")) {
                                    if (op == EntityAttributeModifier.Operation.ADD_VALUE) {
                                        speedAdd += val;
                                    } else if (op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                                        speedMultBase += val;
                                    } else if (op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                        speedMultTotal *= (1.0 + val);
                                    }
                                }
                            }
                        }
                    }

// Fórmula oficial do Minecraft: (Base + Add) * (1 + MultBase) * MultTotal
                    double totalDamage = (baseDamage + damageAdd) * (1.0 + damageMultBase) * damageMultTotal;
                    double totalSpeed = (baseSpeed + speedAdd) * (1.0 + speedMultBase) * speedMultTotal;

                    // 🌟 PASSO 2: O DISFARCE VANILLA
                    if (modifiers != null) {
                        Map<AttributeModifierSlot, List<AttributeModifiersComponent.Entry>> vanillaModifiers = new HashMap<>();

                        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                            if (!entry.modifier().id().getNamespace().equals("tiered")) {
                                vanillaModifiers.computeIfAbsent(entry.slot(), k -> new ArrayList<>()).add(entry);
                            }
                        }

                        boolean isFirstVanillaGroup = true;

                        for (Map.Entry<AttributeModifierSlot, List<AttributeModifiersComponent.Entry>> group : vanillaModifiers.entrySet()) {

                            if (!isFirstVanillaGroup) {
                                lines.add(Text.empty());
                            }
                            isFirstVanillaGroup = false;

                            lines.add(Text.translatable("item.modifiers." + group.getKey().asString()).formatted(Formatting.GRAY));

                            for (AttributeModifiersComponent.Entry entry : group.getValue()) {
                                EntityAttributeModifier modifier = entry.modifier();
                                RegistryEntry<EntityAttribute> attributeEntry = entry.attribute();

                                boolean isDamage = modifier.id().equals(Identifier.ofVanilla("base_attack_damage"));
                                boolean isSpeed = modifier.id().equals(Identifier.ofVanilla("base_attack_speed"));

                                String translationKey = attributeEntry.value().getTranslationKey();

                                String[] iconAndName = extractIconAndName(translationKey);
                                String icon = iconAndName[0];
                                String cleanName = iconAndName[1];

                                // Cria a linha base
                                MutableText finalLine = Text.literal(attrMargin);

                                // Adiciona o ícone com Formatting.WHITE para não herdar cores e manter a cor da imagem
                                if (!icon.isEmpty()) {
                                    finalLine.append(Text.literal(icon + " ").formatted(Formatting.WHITE));
                                }

                                // Cria o corpo do texto (Valor + Nome) separadamente
                                MutableText body;
                                if (isDamage) {
                                    body = Text.translatable("attribute.modifier.equals.0", DECIMAL_FORMAT.format(totalDamage), Text.literal(cleanName));
                                    body.formatted(Formatting.DARK_GREEN);
                                } else if (isSpeed) {
                                    body = Text.translatable("attribute.modifier.equals.0", DECIMAL_FORMAT.format(totalSpeed), Text.literal(cleanName));
                                    body.formatted(Formatting.DARK_GREEN);
                                } else {
                                    double value = modifier.value();
                                    if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE ||
                                            modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                        value = value * 100.0;
                                    }
                                    boolean isPositive = value > 0;
                                    String opId = String.valueOf(modifier.operation().getId());
                                    String plusOrTake = isPositive ? "plus" : "take";

                                    body = Text.translatable("attribute.modifier." + plusOrTake + "." + opId, DECIMAL_FORMAT.format(Math.abs(value)), Text.literal(cleanName));
                                    body.formatted(isPositive ? Formatting.BLUE : Formatting.RED);
                                }

                                // Junta o corpo colorido ao ícone neutro
                                finalLine.append(body);
                                lines.add(finalLine);
                            }
                        }

                        // 🌟 PASSO 3: ATRIBUTOS ARPG
                        boolean addedHeader = false;
                        Set<Identifier> drawnModifiers = new HashSet<>();

                        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                            EntityAttributeModifier modifier = entry.modifier();
                            Identifier modId = modifier.id();
                            RegistryEntry<EntityAttribute> attributeEntry = entry.attribute();

                            if (modId.getNamespace().equals("tiered") && !drawnModifiers.contains(modId)) {

                                if (!addedHeader) {
                                    lines.add(Text.empty());
                                    lines.add(Text.literal(" ").append(Text.translatable("tiered.tooltip.tier_attributes")).formatted(Formatting.GRAY));
                                    addedHeader = true;
                                }

                                Identifier attrId = Registries.ATTRIBUTE.getId(attributeEntry.value());
                                boolean isPercentageAttribute = attrId != null && (attrId.equals(Identifier.of("tiered", "critical_chance")));

                                double value = modifier.value();

                                if (isPercentageAttribute && modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                                    value = value * 100.0;
                                }

                                if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE ||
                                        modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                    value = value * 100.0;
                                }

                                boolean isPositive = value > 0;
                                String sign = isPositive ? "+" : "";
                                String percent = (isPercentageAttribute || modifier.operation() != EntityAttributeModifier.Operation.ADD_VALUE) ? "%" : "";

                                String translationKey = attributeEntry.value().getTranslationKey();
                                if (isBetterCombatLoaded && attrId != null && attrId.getPath().equals("entity_interaction_range")) {
                                    translationKey = "attribute.name.generic.attack_range";
                                }

                                String[] iconAndName = extractIconAndName(translationKey);
                                String icon = iconAndName[0];
                                String cleanName = iconAndName[1];

                                // Cria a linha base
                                MutableText finalLine = Text.literal(attrMargin);

                                // Adiciona o ícone com Formatting.WHITE
                                if (!icon.isEmpty()) {
                                    finalLine.append(Text.literal(icon + " ").formatted(Formatting.WHITE));
                                }

                                // Cria o texto do atributo separadamente para receber a cor do Tier
                                MutableText attributeText = Text.literal(sign + DECIMAL_FORMAT.format(value) + percent + " ")
                                        .append(Text.literal(cleanName));

                                AttributeColorMode mode = ConfigInit.CONFIG.attributeColorMode;
                                if (mode == AttributeColorMode.TIER_COLOR) {
                                    if (isPositive) {
                                        attributeText.setStyle(potentialAttribute.getStyle());
                                    } else {
                                        attributeText.formatted(Formatting.RED);
                                    }
                                } else {
                                    Formatting color;
                                    if (mode == AttributeColorMode.GREEN_RED) {
                                        color = isPositive ? Formatting.GREEN : Formatting.RED;
                                    } else {
                                        color = isPositive ? Formatting.BLUE : Formatting.RED;
                                    }
                                    attributeText.formatted(color);
                                }

                                // Junta o texto colorido ao ícone neutro
                                finalLine.append(attributeText);
                                lines.add(finalLine);
                            }
                        }
                    }

                    // 🌟 PASSO 4: DURABILIDADE DA ARMA
                    if (stack.isDamageable()) {
                        int maxDamage = stack.getMaxDamage();
                        int currentDamage = stack.getDamage();
                        int remainingDamage = maxDamage - currentDamage;

                        double durabilityPercent = (double) remainingDamage / maxDamage;
                        Formatting durabilityColor;

                        if (durabilityPercent <= 0.05) {
                            durabilityColor = Formatting.DARK_RED;
                        } else if (durabilityPercent <= 0.10) {
                            durabilityColor = Formatting.RED;
                        } else if (durabilityPercent <= 0.25) {
                            durabilityColor = Formatting.GOLD;
                        } else if (durabilityPercent <= 0.50) {
                            durabilityColor = Formatting.YELLOW;
                        } else if (durabilityPercent <= 0.75) {
                            durabilityColor = Formatting.GREEN;
                        } else {
                            durabilityColor = Formatting.DARK_GREEN;
                        }

                        lines.add(Text.empty());
                        MutableText durabilityText = Text.literal(attrMargin)
                                .append(Text.translatable("tiered.tooltip.durability").append(": ").formatted(Formatting.GRAY))
                                .append(Text.literal(remainingDamage + " / " + maxDamage).formatted(durabilityColor));

                        lines.add(durabilityText);
                    }
                }
            }
        });
    }
}