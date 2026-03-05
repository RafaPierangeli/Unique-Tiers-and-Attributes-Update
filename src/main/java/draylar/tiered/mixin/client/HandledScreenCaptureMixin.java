package draylar.tiered.mixin.client;

import draylar.tiered.client.TooltipContextHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenCaptureMixin {

    @Shadow @Nullable protected Slot focusedSlot;

    // 🌟 PASSO 1: Limpa o porta-malas no início de cada frame (Garante que não vaze memória)
    @Inject(method = "render", at = @At("HEAD"))
    private void clearOnRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TooltipContextHolder.currentStack = net.minecraft.item.ItemStack.EMPTY;
    }

    // 🌟 PASSO 2: Guarda o item EXATO que o Minecraft focou (Sem matemática manual!)
    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
    private void captureTooltipItem(DrawContext context, int x, int y, CallbackInfo ci) {
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            TooltipContextHolder.currentStack = this.focusedSlot.getStack();
        }
    }
}