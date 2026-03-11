package draylar.tiered.data;


import draylar.tiered.api.ARPGEquipmentData;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class TieredDataComponents {

    // Registra o nosso Super Componente!
    public static final ComponentType<ARPGEquipmentData> ARPG_DATA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("tiered", "arpg_data"),
            ComponentType.<ARPGEquipmentData>builder()
                    .codec(ARPGEquipmentData.CODEC)
                    .packetCodec(ARPGEquipmentData.PACKET_CODEC)
                    .build()
    );

    // 🌟 Salva o raio atual escolhido pelo jogador (0 = 1x1, 1 = 3x3, 2 = 5x5, etc)
    public static final net.minecraft.component.ComponentType<Integer> AOE_MODE = net.minecraft.registry.Registry.register(
            net.minecraft.registry.Registries.DATA_COMPONENT_TYPE,
            net.minecraft.util.Identifier.of("tiered", "aoe_mode"), // Troque "tiered" pelo seu MOD_ID se for diferente
            net.minecraft.component.ComponentType.<Integer>builder().codec(com.mojang.serialization.Codec.INT).build()
    );

    public static void init() {
        // Apenas para carregar a classe no onInitialize()
    }
}