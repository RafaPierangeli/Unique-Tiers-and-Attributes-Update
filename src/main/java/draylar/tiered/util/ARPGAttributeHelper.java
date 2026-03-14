package draylar.tiered.util;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ARPGAttributeHelper {

    public static void updateModifiers(ItemStack stack) {
        ARPGEquipmentData arpgData = stack.get(TieredDataComponents.ARPG_DATA);
        if (arpgData == null) return;

        // 🌟 1. A PROTEÇÃO VANILLA
        AttributeModifiersComponent currentModifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (currentModifiers == null) {
            currentModifiers = stack.getItem().getComponents().getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        }

        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        // 🌟 2. LIMPEZA: Mantém tudo que NÃO for do ARPG
        for (AttributeModifiersComponent.Entry entry : currentModifiers.modifiers()) {
            if (!entry.modifier().id().getPath().startsWith("arpg_")) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // 🌟 3. INJEÇÃO: Adiciona os novos bônus do ARPG
        if (arpgData.level() > 0) {
            String affinity = arpgData.affinity();
            double bonusValue = ARPGAffinityLogic.getBonusValue(affinity, arpgData.level(), arpgData.prestige());

            if (bonusValue > 0) {
                RegistryEntry<EntityAttribute> attribute = null;
                EntityAttributeModifier.Operation operation = EntityAttributeModifier.Operation.ADD_VALUE;

                // =====================================================================
                // 🌟 A MÁGICA DO SLOT INTELIGENTE (O seu código perfeito!)
                // =====================================================================
                EquipmentSlot naturalSlot = EquipmentSlot.MAINHAND; // Padrão para itens que não se vestem

                if (stack.contains(DataComponentTypes.EQUIPPABLE)) {
                    naturalSlot = stack.get(DataComponentTypes.EQUIPPABLE).slot();
                }

                // Converte o EquipmentSlot para o AttributeModifierSlot que o builder exige!
                AttributeModifierSlot modifierSlot = AttributeModifierSlot.forEquipmentSlot(naturalSlot);

                // =====================================================================
                // 🌟 ATRIBUTOS E BALANCEAMENTO (Evitando ficar OP)
                // =====================================================================
                switch (affinity) {
                    // ⚔️ --- ARMAS CORPO-A-CORPO ---
                    case "brute_force", "retaliation", "focused_mind", "deadly_dive":
                        attribute = EntityAttributes.ATTACK_DAMAGE;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE; // Dano é melhor somar fixo
                        break;
                    case "dancing_blade":
                        attribute = EntityAttributes.ATTACK_SPEED;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE; // Lembra? Tem que ser ADD_VALUE por causa do -2.4 da espada!
                        break;
                    case "true_strike", "eagle_eye":
                        attribute = Registries.ATTRIBUTE.getEntry(Identifier.of("tiered", "critical_chance")).orElse(null);
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        bonusValue = bonusValue / 100.0;
                        break;

                    case "heavy_shot":
                        attribute = Registries.ATTRIBUTE.getEntry(Identifier.of("tiered", "range_attack_damage")).orElse(null);
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        break;

                    // 🛡️ --- ARMADURAS ---
                    case "titan_heart", "winds_of_life":
                        attribute = EntityAttributes.MAX_HEALTH;
                        // 🌟 BALANCEAMENTO: Usa MULTIPLIED_TOTAL para dar % de vida extra em vez de corações fixos!
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        break;
                    case "bulwark", "wall":
                        attribute = EntityAttributes.ARMOR;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE; // Armadura base é fixa
                        break;
                    case "solid_foundation":
                        attribute = EntityAttributes.ARMOR_TOUGHNESS;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        break;
                    case "immovable":
                        attribute = EntityAttributes.KNOCKBACK_RESISTANCE;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        bonusValue = bonusValue / 100.0; // Knockback vai de 0.0 a 1.0
                        break;
                    case "guiding_winds", "light_steps":
                        attribute = EntityAttributes.MOVEMENT_SPEED;
                        // 🌟 BALANCEAMENTO: Velocidade de movimento DEVE ser multiplicador, senão o jogador voa!
                        operation = EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                        bonusValue = bonusValue / 100.0;
                        break;
                    case "aerial_boost" :
                        attribute = EntityAttributes.JUMP_STRENGTH;
                        operation = EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                        bonusValue = bonusValue / 100.0;
                        break;
                    case "mountain_walker", "long_strides" :
                        attribute = EntityAttributes.STEP_HEIGHT;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        break;
                    case "wayfarer" :
                        attribute = EntityAttributes.MOVEMENT_EFFICIENCY;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        bonusValue = bonusValue / 100.0;
                        break;
                    case "swift_shadows" :
                        attribute = EntityAttributes.SNEAKING_SPEED;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        bonusValue = bonusValue / 100.0;
                        break;
                    case "acrobat" :
                        attribute = EntityAttributes.SAFE_FALL_DISTANCE;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE;
                        break;

                    // ⛏️ --- FERRAMENTAS ---
                    case "hard_labor", "voracious_digger":
                        attribute = EntityAttributes.MINING_EFFICIENCY;
                        // 🌟 BALANCEAMENTO: Eficiência em % é muito mais seguro que valor fixo
                        operation = EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                        bonusValue = bonusValue / 100.0;
                        break;
                    case "long_reach", "earth_reach":
                        attribute = EntityAttributes.BLOCK_INTERACTION_RANGE;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE; // Alcance é em blocos (fixo)
                        break;
                    case "far_sight" :
                        attribute = EntityAttributes.ENTITY_INTERACTION_RANGE;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE; // Alcance é em blocos (fixo)
                        break;
                    case "oxygen" :
                        attribute = EntityAttributes.OXYGEN_BONUS;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE; // Alcance é em blocos (fixo)
                        break;
                    case "aquatic" :
                        attribute = EntityAttributes.WATER_MOVEMENT_EFFICIENCY;
                        operation = EntityAttributeModifier.Operation.ADD_VALUE; // Alcance é em blocos (fixo)
                        break;
                }

                if (attribute != null) {
                    Identifier modifierId = Identifier.of("tiered", "arpg_" + affinity);
                    EntityAttributeModifier modifier = new EntityAttributeModifier(modifierId, bonusValue, operation);
                    builder.add(attribute, modifier, modifierSlot);

                    // Debug atualizado para mostrar a operação e o slot!
                    System.out.println("[ARPG DEBUG] Injetado +" + bonusValue + " (" + operation.name() + ") de " + affinity + " no slot " + modifierSlot.asString() + "!");
                }
            }
        }

        // 🌟 4. SALVA na arma
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
}