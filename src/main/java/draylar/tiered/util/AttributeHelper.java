package draylar.tiered.util;

import draylar.tiered.api.CustomEntityAttributes; // Ajuste se o seu se chamar TieredAttributes
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;

public class AttributeHelper {

    // 🌟 1. CHANCE DE CRÍTICO (Agora de 0 a 100%)
    public static boolean shouldMeeleCrit(PlayerEntity playerEntity) {
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.CRIT_CHANCE);
        if (instance != null) {
            double critChance = instance.getValue();
            return playerEntity.getRandom().nextDouble() < critChance;
        }
        return false;
    }

    // 🌟 2. DANO CRÍTICO (Adeus gambiarra, olá atributo novo!)
    public static float getExtraCritDamage(PlayerEntity playerEntity, float oldDamage) {
        // Agora usamos o SEU novo atributo de Dano Crítico!
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.CRITICAL_DAMAGE);
        if (instance != null) {

            float bonusMultiplier = (float) instance.getValue();

            return oldDamage + (oldDamage * bonusMultiplier);
        }
        return oldDamage;
    }

    // 🌟 3. VELOCIDADE DE MINERAÇÃO (Mantido o cálculo original para não quebrar compatibilidade)
    public static float getExtraDigSpeed(PlayerEntity playerEntity, float oldDigSpeed) {
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.DIG_SPEED);
        if (instance != null) {
            float extraDigSpeed = oldDigSpeed;
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.value();

                if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                    extraDigSpeed += amount;
                } else {
                    extraDigSpeed *= (amount + 1);
                }
            }
            return extraDigSpeed;
        }
        return oldDigSpeed;
    }

    // 🌟 4. DANO À DISTÂNCIA (Arcos/Bestas)
    public static float getExtraRangeDamage(PlayerEntity playerEntity, float oldDamage) {
        EntityAttributeInstance instance = playerEntity.getAttributeInstance(CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
        if (instance != null) {
            float rangeDamage = oldDamage;
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.value();

                if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                    rangeDamage += amount;
                } else {
                    rangeDamage *= (amount + 1.0f);
                }
            }
            return Math.min(rangeDamage, Integer.MAX_VALUE);
        }
        return oldDamage;
    }
}