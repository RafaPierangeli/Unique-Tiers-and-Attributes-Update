package draylar.tiered.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Armazena informações sobre um template de AttributeModifier aplicado a um ItemStack.
 */
public class AttributeTemplate {

    // 🌟 O CODEC: Substitui o GSON e ensina o Minecraft a ler este objeto do JSON
    public static final Codec<AttributeTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            // Lê o tipo do atributo como um Identifier (ex: "minecraft:generic.attack_damage" ou "estaminamod:max_stamina")
            Identifier.CODEC.fieldOf("type").forGetter(AttributeTemplate::getAttributeTypeID),

            // Lê o modificador usando o Codec NATIVO do Minecraft 1.21.11
            EntityAttributeModifier.CODEC.fieldOf("modifier").forGetter(AttributeTemplate::getEntityAttributeModifier),

            // Lê os slots como Listas. Se não existirem no JSON, retorna uma lista vazia.
            EquipmentSlot.CODEC.listOf().optionalFieldOf("required_equipment_slots", List.of()).forGetter(AttributeTemplate::getRequiredEquipmentSlots),
            EquipmentSlot.CODEC.listOf().optionalFieldOf("optional_equipment_slots", List.of()).forGetter(AttributeTemplate::getOptionalEquipmentSlots)
    ).apply(instance, AttributeTemplate::new));

    private final Identifier attributeTypeID;
    private final EntityAttributeModifier entityAttributeModifier;
    private final List<EquipmentSlot> requiredEquipmentSlots;
    private final List<EquipmentSlot> optionalEquipmentSlots;

    public AttributeTemplate(Identifier attributeTypeID, EntityAttributeModifier entityAttributeModifier, List<EquipmentSlot> requiredEquipmentSlots, List<EquipmentSlot> optionalEquipmentSlots) {
        this.attributeTypeID = attributeTypeID;
        this.entityAttributeModifier = entityAttributeModifier;
        this.requiredEquipmentSlots = requiredEquipmentSlots;
        this.optionalEquipmentSlots = optionalEquipmentSlots;
    }

    public List<EquipmentSlot> getRequiredEquipmentSlots() {
        return requiredEquipmentSlots;
    }

    public List<EquipmentSlot> getOptionalEquipmentSlots() {
        return optionalEquipmentSlots;
    }

    public EntityAttributeModifier getEntityAttributeModifier() {
        return entityAttributeModifier;
    }

    public Identifier getAttributeTypeID() {
        return attributeTypeID;
    }


    public void applyModifiers(EquipmentSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeConsumer) {
        // 🔍 A MÁGICA DA COMPATIBILIDADE: Procura o atributo no Registro Global.
        // Se o mod de Estamina estiver instalado, ele acha. Se não, ele pula.
        Optional<RegistryEntry.Reference<EntityAttribute>> optional = Registries.ATTRIBUTE.getEntry(this.attributeTypeID);

        if (optional.isPresent()) {
            // Na 1.21.11, o ID do modificador é um Identifier.
            // Pegamos o ID original e anexamos o nome do slot no final do "path".
            Identifier originalId = entityAttributeModifier.id();
            Identifier newId = Identifier.of("tiered", originalId.getNamespace() + "_" + originalId.getPath() + "_" + slot.getName());

            // Cria o clone com o novo ID, mantendo o valor e a operação originais
            EntityAttributeModifier cloneModifier = new EntityAttributeModifier(newId, entityAttributeModifier.value(), entityAttributeModifier.operation());

            // Verifica se o slot é válido para este modificador na 1.21.11
            AttributeModifierSlot modSlot = AttributeModifierSlot.forEquipmentSlot(slot);
            if (modSlot.matches(slot)) {
                attributeConsumer.accept(optional.get(), cloneModifier);
            }
        }
    }
}