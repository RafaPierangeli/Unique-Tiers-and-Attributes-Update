package draylar.tiered.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class PotentialAttribute {

    public static final Codec<PotentialAttribute> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("id", "").forGetter(PotentialAttribute::getID),
            ItemVerifier.CODEC.listOf().fieldOf("verifiers").forGetter(PotentialAttribute::getVerifiers),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(PotentialAttribute::getWeight),
            Style.Codecs.CODEC.optionalFieldOf("style", Style.EMPTY).forGetter(PotentialAttribute::getStyle),

            AttributeTemplate.CODEC.listOf().optionalFieldOf("attributes", List.of()).forGetter(PotentialAttribute::getAttributes),

            RollTemplate.CODEC.listOf().optionalFieldOf("roll_templates", List.of()).forGetter(PotentialAttribute::getRollTemplates),
            AttributeRoll.CODEC.listOf().optionalFieldOf("positive_pool", List.of()).forGetter(PotentialAttribute::getPositivePool),
            AttributeRoll.CODEC.listOf().optionalFieldOf("negative_pool", List.of()).forGetter(PotentialAttribute::getNegativePool)

    ).apply(instance, PotentialAttribute::new));

    private String id;
    private final List<ItemVerifier> verifiers;
    private final int weight;
    private final Style style;

    private final List<AttributeTemplate> attributes;
    private final List<RollTemplate> rollTemplates;
    private final List<AttributeRoll> positivePool;
    private final List<AttributeRoll> negativePool;

    public PotentialAttribute(String id, List<ItemVerifier> verifiers, int weight, Style style,
                              List<AttributeTemplate> attributes, List<RollTemplate> rollTemplates,
                              List<AttributeRoll> positivePool, List<AttributeRoll> negativePool) {
        this.id = id;
        this.verifiers = verifiers;
        this.weight = weight;
        this.style = style;
        this.attributes = attributes;
        this.rollTemplates = rollTemplates;
        this.positivePool = positivePool;
        this.negativePool = negativePool;
    }

    public String getID() { return id; }
    public void setID(String id) { this.id = id; }
    public List<ItemVerifier> getVerifiers() { return verifiers; }
    public int getWeight() { return weight; }
    public Style getStyle() { return style; }
    public List<AttributeTemplate> getAttributes() { return attributes; }
    public List<RollTemplate> getRollTemplates() { return rollTemplates; }
    public List<AttributeRoll> getPositivePool() { return positivePool; }
    public List<AttributeRoll> getNegativePool() { return negativePool; }

    public boolean isValid(Identifier id) {
        for (ItemVerifier verifier : verifiers) {
            if (verifier.isValid(id)) return true;
        }
        return false;
    }

    public record RollTemplate(int positive, int negative, int weight) {
        public static final Codec<RollTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("positive", 0).forGetter(RollTemplate::positive),
                Codec.INT.optionalFieldOf("negative", 0).forGetter(RollTemplate::negative),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(RollTemplate::weight)
        ).apply(instance, RollTemplate::new));
    }

    // 🌟 A MÁGICA DO AMOUNT: Adicionamos o campo 'amount' como Optional<Double>
    public record AttributeRoll(Identifier type, Optional<Double> amount, double min, double max, EntityAttributeModifier.Operation operation,
                                List<EquipmentSlot> requiredEquipmentSlots, List<EquipmentSlot> optionalEquipmentSlots) {
        public static final Codec<AttributeRoll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("type").forGetter(AttributeRoll::type),
                Codec.DOUBLE.optionalFieldOf("amount").forGetter(AttributeRoll::amount), // Novo campo!
                Codec.DOUBLE.optionalFieldOf("min", 0.0).forGetter(AttributeRoll::min),
                Codec.DOUBLE.optionalFieldOf("max", 0.0).forGetter(AttributeRoll::max),
                EntityAttributeModifier.Operation.CODEC.optionalFieldOf("operation", EntityAttributeModifier.Operation.ADD_VALUE).forGetter(AttributeRoll::operation),
                EquipmentSlot.CODEC.listOf().optionalFieldOf("required_equipment_slots", List.of()).forGetter(AttributeRoll::requiredEquipmentSlots),
                EquipmentSlot.CODEC.listOf().optionalFieldOf("optional_equipment_slots", List.of()).forGetter(AttributeRoll::optionalEquipmentSlots)
        ).apply(instance, AttributeRoll::new));
    }
}