package draylar.tiered.mixin;

import draylar.tiered.api.EquipmentCategory;
import draylar.tiered.config.ConfigInit; // 🌟 Importe sua config
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeathGiveARPGXp(DamageSource damageSource, CallbackInfo ci) {
        if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {

            ItemStack mainHandStack = player.getMainHandStack();
            EquipmentCategory category = EquipmentCategory.getCategory(mainHandStack);

            int xpAmount = ConfigInit.CONFIG.xpBaseKillEntity;

            if (category == EquipmentCategory.MELEE_WEAPON) {
                ARPGXpHelper.addXp(mainHandStack, "damage", xpAmount, player);
            }
            else if (category == EquipmentCategory.RANGED_WEAPON) {
                ARPGXpHelper.addXp(mainHandStack, "ranged_damage", xpAmount, player);
            }
        }
    }
}