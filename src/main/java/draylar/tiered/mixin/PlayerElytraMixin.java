package draylar.tiered.mixin;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.util.ARPGAffinityLogic;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public abstract class PlayerElytraMixin {

    // 🚀 Intercepta o momento exato em que o foguete aplica a velocidade no jogador
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V")
    )
    private void redirectFireworkVelocity(LivingEntity entity, Vec3d vanillaVelocity) {

        if (entity instanceof PlayerEntity player) {
            ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);

            if (!chest.isEmpty()) {
                ARPGEquipmentData data = chest.get(TieredDataComponents.ARPG_DATA);

                // Verifica se tem a afinidade 'gale'
                if (data != null && data.level() > 0 && "gale".equals(data.affinity())) {
                    double bonusPercentage = ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());

                    if (bonusPercentage > 0) {
                        // Transforma o bônus em um multiplicador. Ex: 50% de bônus = 1.5x de força
                        double a = 1.0 + (bonusPercentage / 100.0);

                        Vec3d look = player.getRotationVector();
                        Vec3d currentVel = player.getVelocity();

                        // 🌟 A FÓRMULA VANILLA ANABOLIZADA
                        // Multiplicamos a força base (0.1) e a velocidade máxima alvo (1.5) pelo nosso bônus 'a'
                        Vec3d newVelocity = currentVel.add(
                                look.x * 0.1 * a + (look.x * 1.5 * a - currentVel.x) * 0.5,
                                look.y * 0.1 * a + (look.y * 1.5 * a - currentVel.y) * 0.5,
                                look.z * 0.1 * a + (look.z * 1.5 * a - currentVel.z) * 0.5
                        );

                        // Aplica a nossa velocidade super sônica e ignora a do Vanilla
                        player.setVelocity(newVelocity);
                        return;
                    }
                }
            }
        }

        // Se o jogador não tiver 'gale' ou for um mob, aplica a velocidade normal do Vanilla
        entity.setVelocity(vanillaVelocity);
    }
}