package draylar.tiered.mixin;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {

    @Inject(method = "use", at = @At("RETURN"))
    private void onUseGiveFishingXp(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        int result = cir.getReturnValue();
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;

        if (bobber.getPlayerOwner() instanceof ServerPlayerEntity player) {

            // 🐟 RESULTADO 1: Pescou um peixe ou tesouro com sucesso!
            if (result == 1) {
                int xpAmount = ConfigInit.CONFIG.xpBaseFishing;
                // Treina a Sorte no Mar (Bênção dos Mares)
                ARPGXpHelper.addXp(usedItem, "luck_of_sea", xpAmount, player);
            }
            // 🌊 RESULTADO 0: Puxou a isca de volta sem pescar nada
            else if (result == 0) {
                // 🛡️ SISTEMA ANTI-EXPLOIT:
                // A boia precisa estar tocando a água E ter ficado lá por pelo menos 2 segundos (40 ticks)
                if (bobber.isTouchingWater() && bobber.age > 40) {
                    int xpAmount = ConfigInit.CONFIG.xpBaseCastRod;
                    // Treina a Velocidade de Pesca (Isca Irresistível)
                    ARPGXpHelper.addXp(usedItem, "lure", xpAmount, player);
                }
            }
        }
    }
}