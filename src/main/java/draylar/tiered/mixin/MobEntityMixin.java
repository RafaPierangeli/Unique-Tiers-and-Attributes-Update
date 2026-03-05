package draylar.tiered.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity; // 🌟 NOVO IMPORT
import net.minecraft.item.ItemStack;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;

@Mixin(MobEntity.class)
public class MobEntityMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private void initializeMixin(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, CallbackInfoReturnable<EntityData> info) {
        if (ConfigInit.CONFIG.entityItemModifier) {
            // 🌟 CORREÇÃO: Convertendo 'this' para LivingEntity para acessar o método nativo
            LivingEntity livingEntity = (LivingEntity) (Object) this;

            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                // 🌟 CORREÇÃO: Chamando o metodo diretamente da LivingEntity
                ItemStack itemStack = livingEntity.getEquippedStack(equipmentSlot);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ModifierUtils.setItemStackAttribute(null, itemStack, false);
            }
        }
    }

    // 🌟 A NOVA ESTRATÉGIA: Injetamos no exato momento da morte, antes do loot cair.
        @Inject(method = "dropEquipment", at = @At("HEAD"))
        private void onDropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer, CallbackInfo ci) {

            // Verifica se a configuração do mod permite modificar itens de entidades
            if (ConfigInit.CONFIG.entityDropModifier) {
                MobEntity mob = (MobEntity) (Object) this;

                // Passamos por todos os slots (Mão, Capacete, Peitoral, etc)
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = mob.getEquippedStack(slot);

                    // Se o mob estiver segurando/vestindo algo, aplicamos o Tier na hora!
                    if (!stack.isEmpty()) {
                        ModifierUtils.setItemStackAttribute(null, stack, false);
                    }
                }
            }
        }

    // 🌟 CORREÇÃO: Removemos o @Shadow problemático! Não precisamos mais dele.
}
