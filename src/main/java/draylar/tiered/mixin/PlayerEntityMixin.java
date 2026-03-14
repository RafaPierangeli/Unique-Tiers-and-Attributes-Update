package draylar.tiered.mixin;

import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.api.SoulboundAccessor;
import draylar.tiered.util.AttributeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.util.ARPGSurvivalHelper;
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.injection.*;


import java.util.HashMap;
import java.util.Map;

// 🌟 ADICIONADO: implements SoulboundAccessor
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements SoulboundAccessor {

    @Unique
    private boolean isCustomCrit = false;

    // 🌟 ADICIONADO: O Mapa que guarda os itens da alma quando o jogador morre
    @Unique
    private final Map<Integer, ItemStack> tiered$soulboundItems = new HashMap<>();

    private PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    // 🌟 ADICIONADO: O método da interface para acessar os itens
    @Override
    public Map<Integer, ItemStack> tiered$getSoulboundItems() {
        return this.tiered$soulboundItems;
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
    // =================================================================
    // 🧠 1. EFEITO: MULTIPLICAR O XP RECEBIDO (ancient_wisdom)
    // =================================================================
    @ModifyVariable(method = "addExperience", at = @At("HEAD"), argsOnly = true)
    private int modifyExperienceGain(int experience) {
        if (experience > 0 && !this.getEntityWorld().isClient() && (Object) this instanceof ServerPlayerEntity player) {

            double bonusPercentage = ARPGSurvivalHelper.getTotalAffinityBonus(player, "ancient_wisdom");

            if (bonusPercentage > 0) {
                int extraXp = (int) (experience * (bonusPercentage / 100.0));
                if (extraXp == 0 && bonusPercentage > 0) extraXp = 1;
                return experience + extraXp;
            }
        }
        return experience;
    }

    // =================================================================
    // 📈 2. TREINAMENTO: DAR XP PARA O CAPACETE AO GANHAR XP VANILLA
    // =================================================================
    @Inject(method = "addExperience", at = @At("HEAD"))
    private void onAddExperienceGrantXp(int experience, CallbackInfo ci) {
        if (experience > 0 && !this.getEntityWorld().isClient() && (Object) this instanceof ServerPlayerEntity player) {

            ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);

            if (!helmet.isEmpty()) {
                ARPGEquipmentData data = helmet.get(TieredDataComponents.ARPG_DATA);

                if (data != null) {
                    String affinity = data.affinity();

                    if (data.level() == 0 || "ancient_wisdom".equals(affinity)) {
                        // 🌟 UPGRADE DE ARQUITETO: Passamos a variável 'experience' em vez de '1'.
                        // Agora, se o jogador pegar uma orbe gigante, o capacete ganha muito XP de uma vez!
                        ARPGXpHelper.grantXp(helmet, "ancient_wisdom", experience, false, 0, player);
                    }
                }
            }
        }
    }
}
