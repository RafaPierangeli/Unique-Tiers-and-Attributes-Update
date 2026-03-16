package draylar.tiered.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.List;
import java.util.Map;

public record ARPGEquipmentData(
        int level,
        int currentXp,
        int prestige,
        String affinity,
        Map<String, Integer> trainingXp,
        int maxSlots,

        // 🌟 UPGRADE: Agora a arma guarda objetos estruturados em vez de Strings!
        List<ScrollData> slots,

        boolean isBroken
) {
    public static final ARPGEquipmentData DEFAULT = new ARPGEquipmentData(
            0, 0, 0, "unawakened", Map.of(), 0, List.of(), false
    );

    public static final Codec<ARPGEquipmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(ARPGEquipmentData::level),
            Codec.INT.fieldOf("current_xp").forGetter(ARPGEquipmentData::currentXp),
            Codec.INT.fieldOf("prestige").forGetter(ARPGEquipmentData::prestige),
            Codec.STRING.fieldOf("affinity").forGetter(ARPGEquipmentData::affinity),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("training_xp").forGetter(ARPGEquipmentData::trainingXp),
            Codec.INT.fieldOf("max_slots").forGetter(ARPGEquipmentData::maxSlots),

            // 🌟 UPGRADE: Codec do ScrollData
            ScrollData.CODEC.listOf().fieldOf("slots").forGetter(ARPGEquipmentData::slots),

            Codec.BOOL.fieldOf("is_broken").forGetter(ARPGEquipmentData::isBroken)
    ).apply(instance, ARPGEquipmentData::new));

    public static final PacketCodec<RegistryByteBuf, ARPGEquipmentData> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, ARPGEquipmentData::level,
            PacketCodecs.INTEGER, ARPGEquipmentData::currentXp,
            PacketCodecs.INTEGER, ARPGEquipmentData::prestige,
            PacketCodecs.STRING, ARPGEquipmentData::affinity,
            PacketCodecs.codec(Codec.unboundedMap(Codec.STRING, Codec.INT)), ARPGEquipmentData::trainingXp,
            PacketCodecs.INTEGER, ARPGEquipmentData::maxSlots,

            // 🌟 UPGRADE: PacketCodec do ScrollData
            PacketCodecs.codec(ScrollData.CODEC.listOf()), ARPGEquipmentData::slots,

            PacketCodecs.BOOLEAN, ARPGEquipmentData::isBroken,
            ARPGEquipmentData::new
    );
}