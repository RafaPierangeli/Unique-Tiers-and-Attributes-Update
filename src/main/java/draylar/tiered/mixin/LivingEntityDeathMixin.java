package draylar.tiered.mixin;

import draylar.tiered.api.EquipmentCategory;
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
        // Verifica se quem causou o dano fatal foi um jogador (no lado do servidor)
        if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {

            // Pega o item que o jogador está segurando na mão principal
            ItemStack mainHandStack = player.getMainHandStack();
            EquipmentCategory category = EquipmentCategory.getCategory(mainHandStack);

            // Se for arma Melee, ganha XP de "damage" (Força Bruta)
            if (category == EquipmentCategory.MELEE_WEAPON) {
                // Dá 10 de XP por abate (você pode mudar esse valor depois)
                ARPGXpHelper.addXp(mainHandStack, "damage", 10, player);
            }
            // Se for arma à distância, ganha XP de "ranged_damage" (Tiro Pesado)
            else if (category == EquipmentCategory.RANGED_WEAPON) {
                ARPGXpHelper.addXp(mainHandStack, "ranged_damage", 10, player);
            }
        }
    }
}