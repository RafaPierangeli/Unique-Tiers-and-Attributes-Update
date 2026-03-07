package draylar.tiered.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import draylar.tiered.util.AttributeHelper;

@Mixin(PersistentProjectileEntity.class)
public abstract class RangedWeaponItemMixin {

    // 🌟 1. A NOSSA MATEMÁTICA (Range Damage + Custom Crit)
    @ModifyVariable(
            method = "onEntityHit",
            at = @At(value = "STORE", ordinal = 0),
            ordinal = 0
    )
    private int modifyBaseDamage(int originalDamage) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        Entity owner = projectile.getOwner();

        if (owner instanceof PlayerEntity player) {
            float newDamage = originalDamage;

            newDamage = AttributeHelper.getExtraRangeDamage(player, newDamage);

            boolean isVanillaCrit = projectile.isCritical();
            boolean isArpgCrit = AttributeHelper.shouldMeeleCrit(player);

            if (isVanillaCrit || isArpgCrit) {
                // Aplica o SEU multiplicador (ex: 4x)
                newDamage = AttributeHelper.getExtraCritDamage(player, newDamage);

                if (!isVanillaCrit) {
                    projectile.setCritical(true); // Garante as partículas
                }
            }

            return Math.round(newDamage);
        }

        return originalDamage;
    }

    // 🌟 2. O HACK: DESATIVANDO O CRÍTICO VANILLA PARA JOGADORES
    @Redirect(
            method = "onEntityHit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;isCritical()Z")
    )
    private boolean disableVanillaCritDamage(PersistentProjectileEntity projectile) {
        // Quando o Minecraft for calcular o dano extra de 50%, nós interceptamos a pergunta.
        if (projectile.getOwner() instanceof PlayerEntity) {
            // Dizemos "false". O Vanilla não vai adicionar dano nenhum!
            // Como isso só afeta o cálculo de dano, a flecha CONTINUA soltando partículas no ar!
            return false;
        }
        // Se for um Esqueleto atirando, deixamos o Vanilla agir normalmente.
        return projectile.isCritical();
    }
}