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
public abstract class LivingEntityHealMixin {

    @Inject(method = "heal", at = @At("HEAD"))
    private void onHealGrantChestplateXp(float amount, CallbackInfo ci) {
        if (amount > 0 && (Object) this instanceof ServerPlayerEntity player) {
            ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);

            if (!chestplate.isEmpty()) {
                // 👕 PEITORAL: Coração de Titã (Curar-se)

                // 🌟 SISTEMA ANTI-EXPLOIT DE REGENERAÇÃO
                // A regeneração natural cura 1.0f (meio coração) por vez, muito rápido.
                // Vamos dar 1 de XP, mas a CHANCE de ganhar depende do tamanho da cura!
                // Ex: Cura de 1.0f = 20% de chance de ganhar 1 XP.
                // Ex: Poção de Cura (4.0f) = 80% de chance de ganhar 1 XP.
                float chance = amount * 0.20f;

                if (player.getRandom().nextFloat() < chance) {
                    // Passamos 1 de XP específico. O multiplicador global vai agir em cima disso!
                    ARPGXpHelper.grantXp(chestplate, "titan_heart", 3, false, 0, player);
                }
            }
        }
    }
}