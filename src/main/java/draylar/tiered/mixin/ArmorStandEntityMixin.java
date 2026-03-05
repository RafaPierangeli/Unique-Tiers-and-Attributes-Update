package draylar.tiered.mixin;

import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin {

    @Unique
    private boolean isGenerated = true;
    @Unique
    private boolean isClient = true;

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void writeCustomDataToNbtMixin(WriteView view, CallbackInfo ci) {
        view.putBoolean("IsGenerated", this.isGenerated);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void readCustomDataMixin(ReadView view, CallbackInfo ci) {
        // 🌟 CORREÇÃO: getBoolean retorna um boolean primitivo, não um Optional.
        // Para ter um valor padrão (true), verificamos se a chave existe primeiro.
        if (view.contains("IsGenerated")) {
            this.isGenerated = view.getBoolean("IsGenerated",true);
        } else {
            this.isGenerated = true;
        }
    }

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void interactAt(PlayerEntity player, Vec3d hitPos, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        this.isGenerated = false;
        this.isClient = player.getEntityWorld().isClient();
    }

    @Inject(method = "equip", at = @At("HEAD"))
    private void equipStackMixin(PlayerEntity player, EquipmentSlot slot, ItemStack stack, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        if (!this.isClient && this.isGenerated && ConfigInit.CONFIG.lootContainerModifier) {
            ModifierUtils.setItemStackAttribute(null, stack, false);
        }
    }
}