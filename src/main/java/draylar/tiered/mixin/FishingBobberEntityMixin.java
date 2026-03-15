package draylar.tiered.mixin;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.util.ARPGAffinityLogic;
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {

    // 🌟 CORREÇÃO 1.21.11: Nomes corretos do Yarn para os cronômetros da boia
    @Shadow private int waitCountdown;
    @Shadow private int fishTravelCountdown;
    @Shadow public abstract PlayerEntity getPlayerOwner();

    // =================================================================
    // 📈 1. TREINAMENTO DE XP (Atualizado para grantXp)
    // =================================================================
    @Inject(method = "use", at = @At("RETURN"))
    private void onUseGiveFishingXp(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        int result = cir.getReturnValue();
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;

        if (bobber.getPlayerOwner() instanceof ServerPlayerEntity player) {

            // 🐟 RESULTADO 1: Pescou um peixe ou tesouro com sucesso!
            if (result == 1) {
                int xpAmount = ConfigInit.CONFIG.xpBaseFishing;
                // 🌟 CORREÇÃO: Usando grantXp para treinar/upar luck_of_sea
                ARPGXpHelper.grantXp(usedItem, "luck_of_sea", xpAmount, true, xpAmount, player);
            }
            // 🌊 RESULTADO 0: Puxou a isca de volta sem pescar nada
            else if (result == 0) {
                // 🛡️ SISTEMA ANTI-EXPLOIT: Boia na água por pelo menos 2 segundos
                if (bobber.isTouchingWater() && bobber.age > 20) {
                    int xpAmount = ConfigInit.CONFIG.xpBaseCastRod;
                    // 🌟 CORREÇÃO: Usando grantXp para treinar/upar lure
                    ARPGXpHelper.grantXp(usedItem, "lure", xpAmount, true, xpAmount, player);
                }
            }
        }
    }

    // =================================================================
    // ⚡ 2. EFEITO: LURE (Acelera o tempo para o peixe morder)
    // =================================================================
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickLureEffect(CallbackInfo ci) {
        PlayerEntity player = this.getPlayerOwner();

        if (player != null && !player.getEntityWorld().isClient()) {
            ItemStack rod = player.getMainHandStack();
            if (!(rod.getItem() instanceof net.minecraft.item.FishingRodItem)) {
                rod = player.getOffHandStack();
            }

            if (rod.getItem() instanceof net.minecraft.item.FishingRodItem) {
                ARPGEquipmentData data = rod.get(TieredDataComponents.ARPG_DATA);

                if (data != null && data.level() > 0 && "lure".equals(data.affinity())) {
                    double bonus = ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());

                    if (bonus > 0) {
                        int extraTicks = (int) (bonus / 100.0);
                        if (player.getRandom().nextDouble() * 100.0 < (bonus % 100.0)) {
                            extraTicks++;
                        }

                        if (extraTicks > 0) {
                            // Reduz o tempo de espera inicial
                            if (this.waitCountdown > 0) {
                                this.waitCountdown = Math.max(0, this.waitCountdown - extraTicks);
                            }
                            // 🌟 CORREÇÃO: Reduz o tempo de viagem do peixe usando o nome correto
                            if (this.fishTravelCountdown > 0) {
                                this.fishTravelCountdown = Math.max(0, this.fishTravelCountdown - extraTicks);
                            }
                        }
                    }
                }
            }
        }
    }

    // =================================================================
    // 💎 3. EFEITO: LUCK OF THE SEA (Aumenta a chance de tesouros)
    // =================================================================
    @Redirect(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getLuck()F")
    )
    private float onGetLuckForFishing(PlayerEntity player) {
        float originalLuck = player.getLuck();

        ItemStack rod = player.getMainHandStack();
        if (!(rod.getItem() instanceof net.minecraft.item.FishingRodItem)) {
            rod = player.getOffHandStack();
        }

        if (rod.getItem() instanceof net.minecraft.item.FishingRodItem) {
            ARPGEquipmentData data = rod.get(TieredDataComponents.ARPG_DATA);

            if (data != null && data.level() > 0 && "luck_of_sea".equals(data.affinity())) {
                double bonus = ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());

                if (bonus > 0) {
                    // 100% de bônus = +1.0 de Sorte (Soma com o encantamento Sorte do Mar)
                    return originalLuck + (float) ((bonus / 100.0) * 4.0);
                }
            }
        }

        return originalLuck;
    }
}