package draylar.tiered.mixin;

import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.api.ARPGEquipmentData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract ItemStack getStack();
    @Shadow private int itemAge;

    // 🌟 1. ETERNAL: Impede que o item desapareça com o tempo (Despawn)
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!this.getEntityWorld().isClient()) {
            if (isEternal(this.getStack())) {
                // O tempo de despawn padrão é 6000 ticks (5 minutos).
                // Se chegar perto de sumir, nós resetamos o relógio para zero!
                if (this.itemAge >= 5995) {
                    this.itemAge = 0;
                }
            }
        }
    }

    // 🌟 2. ETERNAL: Impede que o item seja destruído (Fogo, Lava, Cacto, Explosão)
    // Nota: Na 1.21.11, o método damage exige o ServerWorld como primeiro parâmetro!
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (isEternal(this.getStack())) {
            // Só permite a destruição se for o Void (cair fora do mundo) ou comando /kill
            if (!source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                cir.setReturnValue(false);
            }
        }
    }

    // 🌟 3. ETERNAL: Torna o item imune a pegar fogo visualmente
    @Inject(method = "isFireImmune", at = @At("HEAD"), cancellable = true)
    private void onIsFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (isEternal(this.getStack())) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean isEternal(ItemStack stack) {
        if (stack.contains(TieredDataComponents.ARPG_DATA)) {
            ARPGEquipmentData data = stack.get(TieredDataComponents.ARPG_DATA);
            // 🌟 A REGRA: Prestígio 2 ou superior ganha Eternal!
            return data != null && data.prestige() >= 2;
        }
        return false;
    }
}