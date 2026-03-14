package draylar.tiered.mixin;

import draylar.tiered.api.SoulboundAccessor;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.api.ARPGEquipmentData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.rule.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow @Final public PlayerEntity player;
    @Shadow public abstract int size();
    @Shadow public abstract ItemStack getStack(int slot);
    @Shadow public abstract void setStack(int slot, ItemStack stack);

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void onDropAll(CallbackInfo ci) {
        // 🌟 CORREÇÃO 1.21.11: GameRules agora vivem exclusivamente no ServerWorld!
        if (this.player.getEntityWorld() instanceof ServerWorld serverWorld) {
            if (serverWorld.getGameRules().getValue(GameRules.KEEP_INVENTORY)) {
                return; // Se o keepInventory estiver ativo, aborta o sequestro (o jogo já vai salvar tudo)
            }
        }

        // Varre todos os slots do jogador
        for (int i = 0; i < this.size(); ++i) {
            ItemStack stack = this.getStack(i);

            if (!stack.isEmpty() && isSoulbound(stack)) {
                // 1. Salva uma cópia exata do item e o slot onde ele estava na Alma do jogador
                ((SoulboundAccessor) this.player).tiered$getSoulboundItems().put(i, stack.copy());

                // 2. Apaga o item do inventário ANTES do Minecraft dropar no chão
                this.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    @Unique
    private boolean isSoulbound(ItemStack stack) {
        if (stack.contains(TieredDataComponents.ARPG_DATA)) {
            ARPGEquipmentData data = stack.get(TieredDataComponents.ARPG_DATA);
            // 🌟 A REGRA: Prestígio 1 ou superior ganha Soulbound!
            return data != null && data.prestige() >= 1;
        }
        return false;
    }
}