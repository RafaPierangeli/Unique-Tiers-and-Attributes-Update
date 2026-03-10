package draylar.tiered.mixin;

import draylar.tiered.api.EquipmentCategory;
import draylar.tiered.config.ConfigInit; // 🌟 Importe sua config
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

}