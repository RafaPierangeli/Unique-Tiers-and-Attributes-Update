package draylar.tiered.client;

import com.mojang.serialization.MapCodec;
import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.item.ScrollItem;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ScrollAttributeTintSource() implements TintSource {
    public static final MapCodec<ScrollAttributeTintSource> CODEC = MapCodec.unit(new ScrollAttributeTintSource());

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        ScrollData data = stack.get(TieredDataComponents.SCROLL_DATA);
        if (data == null) return 0xFFFFFF;
        return ScrollItem.getAttributeHexColor(data.attributeId());
    }

    @Override
    public MapCodec<? extends TintSource> getCodec() {
        return CODEC;
    }
}