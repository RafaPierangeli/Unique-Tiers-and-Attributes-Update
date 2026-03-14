package draylar.tiered.api;

import net.minecraft.item.ItemStack;
import java.util.Map;

public interface SoulboundAccessor {
    // Guarda o Slot (Integer) e o Item (ItemStack)
    Map<Integer, ItemStack> tiered$getSoulboundItems();
}