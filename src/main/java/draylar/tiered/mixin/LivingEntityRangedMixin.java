package draylar.tiered.mixin;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.util.ARPGAffinityLogic;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRangedMixin {

    @Shadow protected int itemUseTimeLeft;
    @Shadow public abstract ItemStack getActiveItem();
    @Shadow public abstract boolean isUsingItem();

    // ⚡ EFEITO: QUICK DRAW (Acelera o tempo de puxar a corda)
    @Inject(method = "tickActiveItemStack", at = @At("RETURN"))
    private void onTickActiveItemStack(CallbackInfo ci) {
        if (this.isUsingItem() && this.itemUseTimeLeft > 0) {
            if ((Object) this instanceof PlayerEntity player) {

                ItemStack weapon = this.getActiveItem();
                if (weapon.getItem() instanceof net.minecraft.item.RangedWeaponItem) {

                    ARPGEquipmentData data = weapon.get(TieredDataComponents.ARPG_DATA);
                    if (data != null && data.level() > 0 && "quick_draw".equals(data.affinity())) {

                        double bonus = ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());
                        if (bonus > 0) {
                            // Se o bônus for 100%, extraTicks = 1 (diminui 2 por tick, puxa no dobro da velocidade)
                            int extraTicks = (int) (bonus / 100.0);
                            if (player.getRandom().nextDouble() * 100.0 < (bonus % 100.0)) {
                                extraTicks++;
                            }

                            if (extraTicks > 0) {
                                // Acelera o tempo de uso do item (fazendo o arco atirar mais rápido)
                                this.itemUseTimeLeft = Math.max(0, this.itemUseTimeLeft - extraTicks);
                            }
                        }
                    }
                }
            }
        }
    }
}