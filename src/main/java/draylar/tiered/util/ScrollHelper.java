package draylar.tiered.util;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ScrollHelper {

    /**
     * Lê os pergaminhos equipados na arma e injeta os atributos reais no Minecraft.
     * Deve ser chamado sempre que um pergaminho for inserido ou removido.
     */
    public static void updateWeaponScrollAttributes(ItemStack weapon) {
        ARPGEquipmentData arpgData = weapon.get(TieredDataComponents.ARPG_DATA);

        // Se não tem dados de ARPG, não faz nada
        if (arpgData == null) return;

        // Pega os modificadores atuais da arma (Dano base, Tiers, etc)
        AttributeModifiersComponent currentModifiers = weapon.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        // 1. LIMPEZA: Removemos os atributos antigos de pergaminhos para não acumular infinitamente
        for (AttributeModifiersComponent.Entry entry : currentModifiers.modifiers()) {
            if (!entry.modifier().id().getPath().startsWith("scroll_")) {
                // Mantém tudo que NÃO for pergaminho (Vanilla e Tiers)
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        // 2. INJEÇÃO: Adicionamos os atributos dos pergaminhos atualmente equipados
        for (int i = 0; i < arpgData.slots().size(); i++) {
            ScrollData scroll = arpgData.slots().get(i);

            // Tenta converter a String do atributo (ex: "attack_damage") para um Identifier
            Identifier attrId = Identifier.tryParse(scroll.attributeId());
            if (attrId == null) continue;

            // Busca o atributo real no registro do jogo
            RegistryEntry<EntityAttribute> attribute = Registries.ATTRIBUTE.getEntry(attrId).orElse(null);
            if (attribute == null) continue;

            // 🌟 O SEGREDO DA TOOLTIP: O ID começa com "arpg_scroll_" para ficar invisível na lista padrão!
            // Usamos o índice 'i' no nome para garantir que dois pergaminhos de Vida não se sobrescrevam.
            Identifier modifierId = Identifier.of("tiered", "scroll_" + i + "_" + attrId.getPath());

            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    modifierId,
                    scroll.value(),
                    EntityAttributeModifier.Operation.ADD_VALUE // Pergaminhos dão status FLAT (ex: +2, +5)
            );

            // Adiciona o bônus para quando o item estiver na Mão Principal
            // Se você quiser que armaduras recebam pergaminhos no futuro, precisaremos de uma lógica para checar o tipo do item aqui.
            builder.add(attribute, modifier, AttributeModifierSlot.MAINHAND);
        }

        // 3. SALVAMENTO: Aplica os novos modificadores de volta na arma
        weapon.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
}