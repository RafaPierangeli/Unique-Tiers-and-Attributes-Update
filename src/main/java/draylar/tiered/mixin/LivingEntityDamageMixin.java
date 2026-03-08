package draylar.tiered.mixin;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    // 🌟 Mudamos para o método "damage" e injetamos no RETURN (final do método)
    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamageGiveArmorXp(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {

        if (!cir.getReturnValue() || amount <= 0) return;

        if ((Object) this instanceof ServerPlayerEntity player) {

            int xpAmount = ConfigInit.CONFIG.xpBaseTakeDamage;

            if (amount >= 10.0f) {
                xpAmount *= 3;
            }

            EquipmentSlot[] armorSlots = {
                    EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.FEET
            };

            for (EquipmentSlot slot : armorSlots) {
                ItemStack armorStack = player.getEquippedStack(slot);

                if (!armorStack.isEmpty()) {
                    ARPGXpHelper.addXp(armorStack, "resistance", xpAmount, player);
                }
            }
        }
    }
}