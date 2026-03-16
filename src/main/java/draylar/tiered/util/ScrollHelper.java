package draylar.tiered.util;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ScrollHelper {

    public static void updateWeaponScrollAttributes(ItemStack stack) {
        ARPGEquipmentData arpgData = stack.get(TieredDataComponents.ARPG_DATA);
        if (arpgData == null || arpgData.slots() == null) return;

        // 🌟 1. A PROTEÇÃO VANILLA E DA AFINIDADE
        AttributeModifiersComponent currentModifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (currentModifiers == null) {
            currentModifiers = stack.getItem().getComponents().getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        }

        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        // 🌟 2. LIMPEZA: Mantém tudo que NÃO for do Scroll (Preserva Vanilla e ARPG Affinity)
        for (AttributeModifiersComponent.Entry entry : currentModifiers.modifiers()) {
            if (!entry.modifier().id().getPath().startsWith("scroll_")) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // 🌟 3. A MÁGICA DO SLOT INTELIGENTE
        EquipmentSlot naturalSlot = EquipmentSlot.MAINHAND; // Padrão para espadas/ferramentas

        // Se o item for "vestível" (Armadura, Elytra, Escudo), pega o slot correto!
        if (stack.contains(DataComponentTypes.EQUIPPABLE)) {
            naturalSlot = stack.get(DataComponentTypes.EQUIPPABLE).slot();
        }

        // Converte para o formato que o Builder exige na 1.21.11
        AttributeModifierSlot modifierSlot = AttributeModifierSlot.forEquipmentSlot(naturalSlot);

        // 🌟 4. INJEÇÃO DOS PERGAMINHOS
        for (int i = 0; i < arpgData.slots().size(); i++) {
            ScrollData scroll = arpgData.slots().get(i);

            // Ignora buracos vazios
            if (scroll == null || scroll.attributeId().equals("empty")) continue;

            String attrIdStr = scroll.attributeId();
            double bonusValue = scroll.value();

            Identifier attrId = Identifier.tryParse(attrIdStr);
            if (attrId == null) continue;

            RegistryEntry<EntityAttribute> attribute = Registries.ATTRIBUTE.getEntry(attrId).orElse(null);
            if (attribute != null) {
                EntityAttributeModifier.Operation operation = EntityAttributeModifier.Operation.ADD_VALUE;

                // =====================================================================
                // 🌟 BALANCEAMENTO DOS PERGAMINHOS (Porcentagem vs Fixo)
                // =====================================================================
                String path = attrId.getPath();

                switch (path) {
                    // 🏃 Multiplicadores de Porcentagem (Dividimos por 100)
                    case "movement_speed":
                    case "jump_strength":
                    case "mining_efficiency":
                        operation = EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                        bonusValue = bonusValue / 100.0; // Ex: +6 vira +0.06 (6% a mais)
                        break;

                    // 🎯 Valores Fixos que representam Porcentagem na UI (Dividimos por 100)
                    case "critical_chance":
                    case "knockback_resistance":
                    case "movement_efficiency":
                    case "sneaking_speed":
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        bonusValue = bonusValue / 100.0;
                        break;

                    // ⚔️ Valores Fixos Normais (Vida, Dano, Armadura, etc)
                    default:
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        break;
                }

                // 🌟 CRÍTICO: Cria um ID único usando o índice do slot (i).
                // Isso permite que o jogador coloque 2 pergaminhos de Vida na mesma arma e os dois funcionem!
                Identifier modifierId = Identifier.of("tiered", "scroll_slot_" + i + "_" + path);
                EntityAttributeModifier modifier = new EntityAttributeModifier(modifierId, bonusValue, operation);

                builder.add(attribute, modifier, modifierSlot);

                // Debug no console para você ter certeza de que aplicou certo
                System.out.println("[SCROLL DEBUG] Injetado +" + bonusValue + " (" + operation.name() + ") de " + path + " no slot " + modifierSlot.asString() + "!");
            }
        }

        // 🌟 5. SALVA na arma
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
}