package draylar.tiered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;

@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {


    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    public void quickMoveMixin(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> info) {


        if (slot == 0 && player.getInventory().getEmptySlot() == -1) {
            info.setReturnValue(ItemStack.EMPTY);
        }
    }
}