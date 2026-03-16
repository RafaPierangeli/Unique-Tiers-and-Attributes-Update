package draylar.tiered;

import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.client.ScrollAttributeTintSource;
import draylar.tiered.client.ScrollTierTintSource;
import draylar.tiered.client.ScrollTooltipCallback;
import draylar.tiered.network.TieredClientPacket;
import draylar.tiered.reforge.ReforgeScreen;
import draylar.tiered.reforge.ReforgeScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.item.tint.TintSourceTypes;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class TieredClient implements ClientModInitializer {

    // map for storing attributes before logging into a server
    public static final Map<Identifier, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();



    @Override
    public void onInitializeClient() {
        HandledScreens.<ReforgeScreenHandler, ReforgeScreen>register(Tiered.REFORGE_SCREEN_HANDLER_TYPE, ReforgeScreen::new);
        TieredClientPacket.init();
        draylar.tiered.client.TieredTooltipCallback.register();
        // 🌟 Registra o Motor de Partículas
        draylar.tiered.client.ARPGParticleEngine.register();

        // 🌟 REGISTRA O EVENTO DA TOOLTIP DO PERGAMINHO
        ScrollTooltipCallback.register();


        // 🌟 INJEÇÃO DIRETA NO DICIONÁRIO DO MINECRAFT
        // Liga a String do seu JSON ao seu código Java
        TintSourceTypes.ID_MAPPER.put(Identifier.of("tiered", "scroll_tier"), ScrollTierTintSource.CODEC);
        TintSourceTypes.ID_MAPPER.put(Identifier.of("tiered", "scroll_attribute"), ScrollAttributeTintSource.CODEC);

    }

}
