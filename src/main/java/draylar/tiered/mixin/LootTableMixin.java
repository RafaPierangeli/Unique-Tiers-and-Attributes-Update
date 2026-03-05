package draylar.tiered.mixin;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(LootTable.class)
public class LootTableMixin {

    @ModifyVariable(
            method = "generateLoot(Lnet/minecraft/loot/context/LootContext;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Consumer<ItemStack> wrapLootConsumer(Consumer<ItemStack> originalConsumer) {

        // Retornamos um NOVO entregador que faz o nosso trabalho antes de chamar o original
        return (itemStack) -> {

            // 1. Verifica se o mod está configurado para modificar loots
            if (ConfigInit.CONFIG.lootContainerModifier) {
                // 2. Aplica o Tier (Raridade) no item recém-criado
                ModifierUtils.setItemStackAttribute(null, itemStack, false);
            }

            // 3. Devolve o item (agora com Tier) para o fluxo normal do Minecraft
            originalConsumer.accept(itemStack);
        };
    }
}