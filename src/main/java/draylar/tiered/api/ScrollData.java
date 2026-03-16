package draylar.tiered.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record ScrollData(String attributeId, String tier, float value) {

    public static final Codec<ScrollData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("attribute_id").forGetter(ScrollData::attributeId),
            Codec.STRING.fieldOf("tier").forGetter(ScrollData::tier),
            Codec.FLOAT.fieldOf("value").forGetter(ScrollData::value)
    ).apply(instance, ScrollData::new));

    public static final PacketCodec<RegistryByteBuf, ScrollData> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ScrollData::attributeId,
            PacketCodecs.STRING, ScrollData::tier,
            PacketCodecs.FLOAT, ScrollData::value,
            ScrollData::new
    );
}