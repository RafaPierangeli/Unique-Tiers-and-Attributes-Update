package draylar.tiered.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class ScrollLootFunction extends ConditionalLootFunction {

    // O Codec obrigatório da 1.21.11 para registrar a função
    public static final MapCodec<ScrollLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            addConditionsField(instance).apply(instance, ScrollLootFunction::new)
    );

    private ScrollLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        Random random = context.getRandom();

        // 🌟 1. ROLA O TIER (As suas porcentagens exatas!)
        String tier = rollTier(random);

        // 🌟 2. ROLA O ATRIBUTO
        String attribute = rollAttribute(random);

        // 🌟 3. CALCULA O VALOR (Baseado no Tier)
        float value = calculateValue(tier, attribute);

        // 🌟 4. APLICA OS DADOS NO PERGAMINHO
        stack.set(TieredDataComponents.SCROLL_DATA, new ScrollData(attribute, tier, value));

        return stack;
    }

    private String rollTier(Random random) {
        int roll = random.nextInt(100); // 0 a 99
        if (roll < 40) return "common";       // 40%
        if (roll < 65) return "uncommon";     // 25%
        if (roll < 80) return "rare";         // 15%
        if (roll < 90) return "epic";         // 10%
        if (roll < 96) return "legendary";    // 6%
        if (roll < 99) return "unique";       // 3%
        return "mythic";                      // 1%
    }

    private String rollAttribute(Random random) {
        // Adicione ou remova atributos da sua lista aqui!
        String[] attributes = {
                "max_health",
                "attack_damage",
                "armor",
                "movement_speed",
                "attack_speed",
                "tiered:critical_chance",
                "tiered:range_attack_damage"
        };
        return attributes[random.nextInt(attributes.length)];
    }

    private float calculateValue(String tier, String attribute) {
        int tierIndex = List.of("common", "uncommon", "rare", "epic", "legendary", "unique", "mythic").indexOf(tier);

        // 🌟 CORREÇÃO: Como o nosso ScrollHelper e o TooltipCallback já são inteligentes
        // e dividem os valores de porcentagem por 100 automaticamente, nós só precisamos
        // gerar números inteiros (1.0 a 7.0) para TODOS os atributos aqui no Loot!
        return 1.0f + tierIndex;
    }

    @Override
    public LootFunctionType<ScrollLootFunction> getType() {
        return ScrollLootInjector.SCROLL_LOOT_TYPE;
    }

    public static ConditionalLootFunction.Builder<?> builder() {
        return builder(ScrollLootFunction::new);
    }
}