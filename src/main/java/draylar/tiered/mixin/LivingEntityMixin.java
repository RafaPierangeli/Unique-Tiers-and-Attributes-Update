package draylar.tiered.mixin;

import draylar.tiered.mixin.access.ServerPlayerEntityAccessor;
import draylar.tiered.network.TieredServerPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound; // 🌟 NOVO IMPORT
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; // 🌟 NOVO IMPORT
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Mutable
    @Shadow
    @Final
    private static TrackedData<Float> HEALTH;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract float getMaxHealth();

    @Shadow
    public abstract void setHealth(float health);


    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void readCustomDataFromNbtMixin(ReadView view, CallbackInfo ci) {
        if (view.contains("Health")) {
            this.dataTracker.set(HEALTH, view.getFloat("Health",getHealth()));
        }
    }

    @Inject(method = "getEquipmentChanges", at = @At(value = "TAIL"))
    private void getEquipmentChangesMixin(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (cir.getReturnValue() != null && (Object) this instanceof ServerPlayerEntity serverPlayerEntity) {
            this.setHealth(this.getHealth() > this.getMaxHealth() ? this.getMaxHealth() : this.getHealth());
            TieredServerPacket.writeS2CHealthPacket(serverPlayerEntity);
            ((ServerPlayerEntityAccessor) serverPlayerEntity).setSyncedHealth(serverPlayerEntity.getHealth());
        }
    }
}