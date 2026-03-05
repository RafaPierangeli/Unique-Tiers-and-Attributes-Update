package draylar.tiered.api;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class CustomEntityAttributes {

    public static final RegistryEntry<EntityAttribute> DIG_SPEED = register("tiered:dig_speed",
            new ClampedEntityAttribute("attribute.name.tiered.dig_speed", 0.0D, 0.0D, 2048.0D).setTracked(true));
    public static final RegistryEntry<EntityAttribute> CRIT_CHANCE = register("tiered:critical_chance",
            new ClampedEntityAttribute("attribute.name.tiered.critical_chance", 0.0, 0.0, 1.0).setTracked(true));
    public static final RegistryEntry<EntityAttribute> DURABLE = register("tiered:durable",
            new ClampedEntityAttribute("attribute.name.tiered.durable", 0.0D, 0.0D, 1D).setTracked(true));
    public static final RegistryEntry<EntityAttribute> RANGE_ATTACK_DAMAGE = register("tiered:range_attack_damage",
            new ClampedEntityAttribute("attribute.name.tiered.range_attack_damage", 0.0D, 0.0D, 2048.0D).setTracked(true));
    public static final RegistryEntry<EntityAttribute> CRITICAL_DAMAGE = register("tiered:critical_damage",
            new ClampedEntityAttribute("attribute.name.tiered.critical_damage", 1.5, 0.0, 5.0).setTracked(true));

    public static void init() {
    }

    private static RegistryEntry<EntityAttribute> register(String id, EntityAttribute attribute) {
        return Registry.registerReference(Registries.ATTRIBUTE, Identifier.of(id), attribute);
    }
}