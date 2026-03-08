package draylar.tiered.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "tiered")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class TieredConfig implements ConfigData {

    // Adicione junto com as outras variáveis
    @ConfigEntry.Gui.Tooltip
    public boolean enableDefaultModifiers = true;
    @Comment("Items in for example mineshaft chests get modifiers")
    public boolean lootContainerModifier = true;
    @ConfigEntry.Gui.Tooltip
    public boolean entityItemModifier = true;
    @ConfigEntry.Gui.Tooltip
    public boolean entityDropModifier = true;
    @Comment("Crafted items get modifiers")
    public boolean craftingModifier = true;
    @Comment("Merchant items get modifiers")
    public boolean merchantModifier = true;
    @Comment("Merchant items Scaling for merchant level")
    public boolean merchantLevelScaling = true;
    @Comment("Decreases the biggest weights by this modifier")
    public float reforgeModifier = 0.9F;
    @Comment("Modify the biggest weights by this modifier per smithing level")
    public float levelzReforgeModifier = 0.01F;
    @Comment("Modify the biggest weights by this modifier per luck")
    public float luckReforgeModifier = 0.02F;
    @ConfigEntry.Gui.Tooltip
    public int reforgeXpCost = 50;
    @ConfigEntry.Gui.Tooltip
    public boolean uniqueReforge = false;

    @ConfigEntry.Category("client_settings")
    public boolean showReforgingTab = true;
    @ConfigEntry.Category("client_settings")
    public int xIconPosition = 0;
    @ConfigEntry.Category("client_settings")
    public int yIconPosition = 0;
    @ConfigEntry.Category("client_settings")
    public boolean tieredTooltip = true;
    @ConfigEntry.Category("client_settings")
    public boolean centerName = true;

    @ConfigEntry.Category("client_settings")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @Comment("Color mode for attributes in tooltip")
    public AttributeColorMode attributeColorMode = AttributeColorMode.DEFAULT;
    @ConfigEntry.Category("client_settings")
    @Comment("Draws a solid colored line around the tooltip based on the tier's rarity color.")
    public boolean enableTierColorBorder = true; // Desenha a linha colorida em volta
    @ConfigEntry.Category("client_settings")
    @Comment("Draws custom textures (corners and top icon) around the tooltip")
    public boolean enableTierOrnaments = true;   // Desenha as texturas (cantos e ícone)
    @ConfigEntry.Category("client_settings")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip(count = 4)
    public TooltipDisplayMode uniqueTooltipMode = TooltipDisplayMode.ALWAYS;


    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int maxLevelEquipment = 100;
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpBaseKillEntity = 2;
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpBaseMineBlock = 1;// XP base (pedra, terra, etc)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierCoal = 2;      // Carvão (x2)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierCopper = 2;    // Cobre (x2)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierQuartz = 2;    // Quartzo (x2)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierIron = 3;      // Ferro (x3)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierRedstone = 3;  // Redstone (x3)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierGold = 4;      // Ouro (x4)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierLapis = 4;     // Lápis-lazúli (x4)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierDiamond = 10;  // Diamante (x10) - Jackpot!
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierEmerald = 15;  // Esmeralda (x15)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpMultiplierNetherite = 20;// Detritos Ancestrais (x20) - O ápice!
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpBaseTakeDamage = 1; // XP que a armadura ganha ao receber um hit

    // 🌟 XP Dinâmico: Ações da Vara de Pescar
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpBaseFishing = 5; // Pescar demora, então o XP base deve ser maior!
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpBaseCastRod = 1;   // XP ao arremessar a isca (Lure)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpBaseSwim = 1;      // XP por segundo nadando (Aquatic)
    @ConfigEntry.Category("equipments_settings")
    @ConfigEntry.Gui.Tooltip
    public int xpBaseSubmerge = 1;  // XP por segundo submerso (Oxygen)
}
