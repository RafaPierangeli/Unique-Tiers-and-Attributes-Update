package draylar.tiered.mixin;

import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.util.AttributeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {


    @Unique
    private boolean isCustomCrit = false;

    private PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void createPlayerAttributesMixin(CallbackInfoReturnable<DefaultAttributeContainer.Builder> info) {
        info.getReturnValue().add(CustomEntityAttributes.CRIT_CHANCE);
        info.getReturnValue().add(CustomEntityAttributes.DIG_SPEED);
        info.getReturnValue().add(CustomEntityAttributes.DURABLE);
        info.getReturnValue().add(CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
        info.getReturnValue().add(CustomEntityAttributes.CRITICAL_DAMAGE);

    }

    @ModifyVariable(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffectUtil;hasHaste(Lnet/minecraft/entity/LivingEntity;)Z"), index = 2)
    private float getBlockBreakingSpeedMixin(float f) {
        return AttributeHelper.getExtraDigSpeed((PlayerEntity) (Object) this, f);
    }

    @ModifyVariable(
            method = "attack",
            at = @At("STORE"),
            ordinal = 2
    )

    private boolean forceVanillaCrit(boolean originalBl3) {

        if (originalBl3) {
            return true;
        }

        return AttributeHelper.shouldMeeleCrit((PlayerEntity) (Object) this);
    }

    @ModifyConstant(
            method = "attack",
            constant = @Constant(floatValue = 1.5f)
    )
    private float modifyCritDamageMultiplier(float originalMultiplier) {

        double bonusCritDamage = this.getAttributeValue(CustomEntityAttributes.CRITICAL_DAMAGE);

        return (float) (bonusCritDamage);

    }
}





