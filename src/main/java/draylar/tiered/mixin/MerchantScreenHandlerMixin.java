package draylar.tiered.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

@Mixin(MerchantScreenHandler.class)
public abstract class MerchantScreenHandlerMixin extends ScreenHandler {

    @Shadow @Final private Merchant merchant;

    // 🌟 UNIQUE: Variável temporária para guardar quem é o jogador fazendo a troca
    @Unique
    private PlayerEntity tiered$currentPlayer;

    public MerchantScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    // 🌟 INJECT: Captura o jogador no exato momento em que ele aperta Shift-Click
    @Inject(method = "quickMove", at = @At("HEAD"))
    private void capturePlayerMixin(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        this.tiered$currentPlayer = player;
    }

    @ModifyVariable(method = "quickMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/MerchantScreenHandler;insertItem(Lnet/minecraft/item/ItemStack;IIZ)Z", ordinal = 0), ordinal = 1)
    private ItemStack quickMoveMixin(ItemStack original) {

        if (this.tiered$currentPlayer instanceof ServerPlayerEntity serverPlayer) {

            // 🌟 PASSO 1: O LIMPA-TRILHOS (Sempre executa)
            ModifierUtils.removeItemStackAttribute(original);

            // 🌟 PASSO 2: APLICAÇÃO (Só executa se a config principal estiver ligada)
            if (ConfigInit.CONFIG.merchantModifier) {
                int merchantLevel = 0;

                if (ConfigInit.CONFIG.merchantLevelScaling && this.merchant instanceof VillagerEntity villager) {
                    merchantLevel = villager.getVillagerData().level();
                }

                ModifierUtils.setItemStackAttribute(serverPlayer, original, false, merchantLevel);
            }
        }
        return original;
    }
}