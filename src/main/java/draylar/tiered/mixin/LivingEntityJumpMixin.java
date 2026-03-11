package draylar.tiered.mixin;

import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jump", at = @At("TAIL"))
    private void onJumpGrantBootsXp(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

            if (!boots.isEmpty()) {
                // 🥾 BOTAS: Impulso Aéreo (Pular) + Ação Base (1 XP)
                ARPGXpHelper.grantXp(boots, "aerial_boost", 2, false, 0, player);
            }
        }
    }
}