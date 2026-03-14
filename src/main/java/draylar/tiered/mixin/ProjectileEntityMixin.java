package draylar.tiered.mixin;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.util.ARPGAffinityLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    // 🎯 EFEITO: ELITE SHOOTER (Multiplica a velocidade e o dano do projétil)
    @Inject(method = "setVelocity(Lnet/minecraft/entity/Entity;FFFFF)V", at = @At("RETURN"))
    private void onSetVelocity(Entity shooter, float pitch, float yaw, float roll, float speed, float divergence, CallbackInfo ci) {
        if (shooter instanceof ServerPlayerEntity player) {

            // Tenta achar a arma que atirou (pode estar na mão principal, secundária ou ativa)
            ItemStack weapon = player.getActiveItem();
            if (weapon.isEmpty() || !(weapon.getItem() instanceof net.minecraft.item.RangedWeaponItem)) {
                weapon = player.getMainHandStack();
            }
            if (weapon.isEmpty() || !(weapon.getItem() instanceof net.minecraft.item.RangedWeaponItem)) {
                weapon = player.getOffHandStack();
            }

            if (!weapon.isEmpty() && weapon.getItem() instanceof net.minecraft.item.RangedWeaponItem) {
                ARPGEquipmentData data = weapon.get(TieredDataComponents.ARPG_DATA);

                if (data != null && data.level() > 0 && "elite_shooter".equals(data.affinity())) {
                    double bonus = ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());

                    if (bonus > 0) {
                        // Se o bônus for 50%, o multiplicador é 1.5x
                        float multiplier = (float) (1.0 + (bonus / 100.0));

                        ProjectileEntity projectile = (ProjectileEntity) (Object) this;

                        // A MÁGICA: Multiplica o vetor de velocidade!
                        projectile.setVelocity(projectile.getVelocity().multiply(multiplier));
                    }
                }
            }
        }
    }
}