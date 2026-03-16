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

public record ScrollTierTintSource() implements TintSource {
    // O Codec que permite o JSON ler essa classe
    public static final MapCodec<ScrollTierTintSource> CODEC = MapCodec.unit(new ScrollTierTintSource());

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user) {
        ScrollData data = stack.get(TieredDataComponents.SCROLL_DATA);
        if (data == null) return 0xFFFFFF;
        return ScrollItem.getTierHexColor(data.tier());
    }

    @Override
    public MapCodec<? extends TintSource> getCodec() {
        return CODEC;
    }
}