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
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class TieredTooltipCallback {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

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

                    if (displayMode == TooltipDisplayMode.ON_SHIFT) {
                        net.minecraft.client.util.Window window = MinecraftClient.getInstance().getWindow();
                        boolean isShiftDown = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                                InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

                        if (!isShiftDown) {
                            lines.add(Text.empty());
                            lines.add(Text.translatable("tiered.tooltip.press_shift").formatted(Formatting.DARK_GRAY));
                            return;
                        }
                    }

                    boolean hasDynamicTooltip = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("dynamictooltips");
                    boolean isBetterCombatLoaded = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bettercombat");

                    if (!hasDynamicTooltip) {
                        // 1. Preservar Nome, Lore e Encantamentos
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

                        //
                        // 🌟 PASSO 1: CÁLCULO DOS TOTAIS
                        //
                        double totalDamage = 1.0;
                        double totalSpeed = 4.0;

                        if (modifiers != null) {
                            for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                                Identifier attrId = Registries.ATTRIBUTE.getId(entry.attribute().value());
                                if (attrId != null) {
                                    String path = attrId.getPath();
                                    double val = entry.modifier().value();

                                    if (path.equals("attack_damage")) {
                                        totalDamage += val;
                                    } else if (path.equals("attack_speed")) {
                                        totalSpeed += val;
                                    }
                                }
                            }
                        }

                        //
                        // 🌟 PASSO 2: O DISFARCE VANILLA (Para o Better Combat ler perfeitamente)
                        //
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

                                // IMPORTANTE: Sem margem no cabeçalho para o Better Combat conseguir injetar o "Two Handed"
                                lines.add(Text.translatable("item.modifiers." + group.getKey().asString()).formatted(Formatting.GRAY));

                                for (AttributeModifiersComponent.Entry entry : group.getValue()) {
                                    EntityAttributeModifier modifier = entry.modifier();
                                    RegistryEntry<EntityAttribute> attributeEntry = entry.attribute();

                                    boolean isDamage = modifier.id().equals(Identifier.ofVanilla("base_attack_damage"));
                                    boolean isSpeed = modifier.id().equals(Identifier.ofVanilla("base_attack_speed"));

                                    String translationKey = attributeEntry.value().getTranslationKey();

                                    if (isDamage) {
                                        // ⚔️ DANO TOTAL (Disfarçado com a chave Vanilla)
                                        MutableText text = Text.literal(attrMargin)
                                                .append(Text.translatable("attribute.modifier.equals.0", DECIMAL_FORMAT.format(totalDamage), Text.translatable(translationKey)))
                                                .formatted(Formatting.DARK_GREEN);
                                        lines.add(text);

                                    } else if (isSpeed) {
                                        // ⚡ VELOCIDADE TOTAL (Disfarçado com a chave Vanilla)
                                        MutableText text = Text.literal(attrMargin)
                                                .append(Text.translatable("attribute.modifier.equals.0", DECIMAL_FORMAT.format(totalSpeed), Text.translatable(translationKey)))
                                                .formatted(Formatting.DARK_GREEN);
                                        lines.add(text);

                                    } else {
                                        // Outros atributos Vanilla (Disfarçados com as chaves Vanilla)
                                        double value = modifier.value();
                                        if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE ||
                                                modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                            value = value * 100.0;
                                        }
                                        boolean isPositive = value > 0;
                                        String opId = String.valueOf(modifier.operation().getId());
                                        String plusOrTake = isPositive ? "plus" : "take";

                                        MutableText text = Text.literal(attrMargin)
                                                .append(Text.translatable("attribute.modifier." + plusOrTake + "." + opId, DECIMAL_FORMAT.format(Math.abs(value)), Text.translatable(translationKey)))
                                                .formatted(isPositive ? Formatting.BLUE : Formatting.RED);
                                        lines.add(text);
                                    }
                                }
                            }

                            //
                            // 🌟 PASSO 3: ATRIBUTOS ARPG (Detalhamento dos Buffs)
                            //
                            boolean addedHeader = false;
                            Set<Identifier> drawnModifiers = new HashSet<>();

                            for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                                EntityAttributeModifier modifier = entry.modifier();
                                Identifier modId = modifier.id();
                                RegistryEntry<EntityAttribute> attributeEntry = entry.attribute();

                                if (modId.getNamespace().equals("tiered") && !drawnModifiers.contains(modId)) {

                                    if (!addedHeader) {
                                        lines.add(Text.empty());
                                        // Aqui podemos usar margem, pois o BC não lê essa parte
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

                                    // 🌟 CAMALEÃO NO DETALHAMENTO TAMBÉM!
                                    String translationKey = attributeEntry.value().getTranslationKey();
                                    if (isBetterCombatLoaded && attrId != null && attrId.getPath().equals("entity_interaction_range")) {
                                        translationKey = "attribute.name.generic.attack_range";
                                    }

                                    MutableText attributeText = Text.literal(attrMargin)
                                            .append(Text.literal(sign + DECIMAL_FORMAT.format(value) + percent + " "))
                                            .append(Text.translatable(translationKey));

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

                                    lines.add(attributeText);
                                }
                            }
                        }

                        //
                        // 🌟 PASSO 4: DURABILIDADE DA ARMA (Atual / Total com Cores Dinâmicas)
                        //
                        if (stack.isDamageable()) {
                            int maxDamage = stack.getMaxDamage();
                            int currentDamage = stack.getDamage();
                            int remainingDamage = maxDamage - currentDamage;

                            // Calcula a porcentagem restante (0.0 a 1.0)
                            double durabilityPercent = (double) remainingDamage / maxDamage;
                            Formatting durabilityColor;

                            // Lógica de cores baseada na porcentagem
                            if (durabilityPercent <= 0.05) {
                                durabilityColor = Formatting.DARK_RED;      // <= 5%: Vermelho Escuro (Crítico)
                            } else if (durabilityPercent <= 0.10) {
                                durabilityColor = Formatting.RED;           // <= 10%: Vermelho
                            } else if (durabilityPercent <= 0.25) {
                                durabilityColor = Formatting.GOLD;          // <= 25%: Laranja (Gold no Minecraft)
                            } else if (durabilityPercent <= 0.50) {
                                durabilityColor = Formatting.YELLOW;        // <= 50%: Amarelo
                            } else if (durabilityPercent <= 0.75) {
                                durabilityColor = Formatting.GREEN;         // <= 75%: Verde Claro
                            } else {
                                durabilityColor = Formatting.DARK_GREEN;    // > 75%: Verde Normal/Escuro (Cheia)
                            }

                            lines.add(Text.empty());

                            // Monta o texto separando a cor do título (Cinza) da cor dos números (Dinâmica)
                            MutableText durabilityText = Text.literal(attrMargin)
                                    .append(Text.translatable("tiered.tooltip.durability").append(": ").formatted(Formatting.GRAY))
                                    .append(Text.literal(remainingDamage + " / " + maxDamage).formatted(durabilityColor));

                            lines.add(durabilityText);
                        }
                    }
                }
            }
        });
    }
}