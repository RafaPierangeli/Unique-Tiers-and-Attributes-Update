package draylar.tiered.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import draylar.tiered.util.AttributeHelper;

@Mixin(PersistentProjectileEntity.class)
public abstract class RangedWeaponItemMixin {


    @ModifyVariable(
            method = "onEntityHit",
            at = @At(value = "STORE", ordinal = 0),
            ordinal = 0 // Captura o primeiro float (que é o dano)
    )
    private float modifyProjectileDamage(float originalDamage) {
        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        Entity owner = projectile.getOwner();

        // Se o dono da flecha for um jogador, aplicamos o dano extra do seu mod
        if (owner instanceof PlayerEntity playerEntity) {
            // O seu AttributeHelper.getExtraCritDamage provavelmente espera o dano original
            // para calcular o bônus. Passamos o 'originalDamage' para ele.
            return AttributeHelper.getExtraCritDamage(playerEntity, originalDamage);
        }

        // Se não for um jogador (ex: esqueleto), retorna o dano normal
        return originalDamage;
    }
}