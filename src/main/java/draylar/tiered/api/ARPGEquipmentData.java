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

        // 🌟 A MÁGICA ACONTECE AQUI: Um mapa dinâmico que aceita qualquer tipo de XP!
        // Ex: {"damage": 40, "agile": 10} ou {"mining": 50}
        Map<String, Integer> trainingXp,

        int maxSlots,
        List<String> slots,
        boolean isBroken
) {
    // O estado padrão agora nasce com um Mapa vazio (Map.of())
    public static final ARPGEquipmentData DEFAULT = new ARPGEquipmentData(
            0, 0, 0, "unawakened", Map.of(), 0, List.of(), false
    );

    // O Codec agora usa unboundedMap para salvar o dicionário no JSON/NBT
    public static final Codec<ARPGEquipmentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(ARPGEquipmentData::level),
            Codec.INT.fieldOf("current_xp").forGetter(ARPGEquipmentData::currentXp),
            Codec.INT.fieldOf("prestige").forGetter(ARPGEquipmentData::prestige),
            Codec.STRING.fieldOf("affinity").forGetter(ARPGEquipmentData::affinity),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("training_xp").forGetter(ARPGEquipmentData::trainingXp),
            Codec.INT.fieldOf("max_slots").forGetter(ARPGEquipmentData::maxSlots),
            Codec.STRING.listOf().fieldOf("slots").forGetter(ARPGEquipmentData::slots),
            Codec.BOOL.fieldOf("is_broken").forGetter(ARPGEquipmentData::isBroken)
    ).apply(instance, ARPGEquipmentData::new));

    // O PacketCodec empacota o mapa para enviar ao Cliente
    public static final PacketCodec<RegistryByteBuf, ARPGEquipmentData> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, ARPGEquipmentData::level,
            PacketCodecs.INTEGER, ARPGEquipmentData::currentXp,
            PacketCodecs.INTEGER, ARPGEquipmentData::prestige,
            PacketCodecs.STRING, ARPGEquipmentData::affinity,
            PacketCodecs.codec(Codec.unboundedMap(Codec.STRING, Codec.INT)), ARPGEquipmentData::trainingXp,
            PacketCodecs.INTEGER, ARPGEquipmentData::maxSlots,
            PacketCodecs.codec(Codec.STRING.listOf()), ARPGEquipmentData::slots,
            PacketCodecs.BOOLEAN, ARPGEquipmentData::isBroken,
            ARPGEquipmentData::new
    );
}