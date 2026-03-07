package draylar.tiered.api;

import draylar.tiered.Tiered;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ModifierUtils {

    @Nullable
    public static Identifier getRandomAttributeIDFor(@Nullable PlayerEntity playerEntity, Item item, boolean reforge) {
        return getRandomAttributeIDFor(playerEntity, item, reforge, 0);
    }

    @Nullable
    public static Identifier getRandomAttributeIDFor(@Nullable PlayerEntity playerEntity, Item item, boolean reforge, int merchantLevel) {
        List<Identifier> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();
        List<Identifier> fallbackAttributes = new ArrayList<>();
        List<Integer> fallbackWeights = new ArrayList<>();

        Identifier itemId = Registries.ITEM.getId(item);

        Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (attribute.isValid(itemId) && (attribute.getWeight() > 0 || reforge)) {
                int weight = attribute.getWeight();
                boolean allowed = true;

                if (merchantLevel > 0) {
                    if (merchantLevel == 1 && weight < 45) allowed = false;
                    else if (merchantLevel == 2 && weight < 20) allowed = false;
                    else if (merchantLevel == 3 && weight < 14) allowed = false;
                    else if (merchantLevel == 4 && weight < 8) allowed = false;
                    else if (merchantLevel == 5 && weight > 50) allowed = false;
                }

                if (allowed) {
                    potentialAttributes.add(id);
                    attributeWeights.add(reforge ? weight + 1 : weight);
                }

                fallbackAttributes.add(id);
                fallbackWeights.add(reforge ? weight + 1 : weight);
            }
        });

        if (potentialAttributes.isEmpty() && !fallbackAttributes.isEmpty() && merchantLevel == 0) {
            potentialAttributes.addAll(fallbackAttributes);
            attributeWeights.addAll(fallbackWeights);
        }

        if (potentialAttributes.isEmpty()) {
            return null;
        }

        if (reforge && attributeWeights.size() > 2) {
            sortWeightsAndAttributes(attributeWeights, potentialAttributes);
            int maxWeight = attributeWeights.get(attributeWeights.size() - 1);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > maxWeight / 2) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * ConfigInit.CONFIG.reforgeModifier));
                }
            }
        }

        if (playerEntity != null) {
            double luck = playerEntity.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.LUCK);

            if (luck > 0) {
                double luckMaxWeight = Collections.max(attributeWeights);

                for (int i = 0; i < attributeWeights.size(); i++) {
                    double currentWeight = attributeWeights.get(i);

                    // Se for um Tier "Ruim/Comum" (Peso alto, maior que 1/3 do máximo)
                    if (currentWeight > luckMaxWeight / 3) {
                        // Diminui o peso (Culling the trash)
                        int newWeight = (int) (currentWeight * (1.0f - (0.02 * luck)));
                        attributeWeights.set(i, Math.max(2, newWeight));
                    }
                    // Se for um Tier "Bom/Raro" (Peso baixo)
                    else {
                        // Aumenta o peso! (Buffing the rare)
                        // Ex: Se o peso era 5, e o jogador tem 2 de sorte, vira 5 + (5 * 0.5 * 2) = 10
                        int newWeight = (int) (currentWeight + (1 + (ConfigInit.CONFIG.luckReforgeModifier * luck)));
                        attributeWeights.set(i, newWeight);
                    }

                }
            }
        }

        int totalWeight = attributeWeights.stream().mapToInt(Integer::intValue).sum();
        if (reforge) {
            System.out.println("=== [DEBUG FORJA] RAIO-X DOS PESOS (INT) ===");
            double currentLuck = playerEntity != null ? playerEntity.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.LUCK) : 0.0;

            System.out.println("Sorte do Jogador: " + currentLuck);
            System.out.println("Reforge Modifier (Corte da Forja): " + ConfigInit.CONFIG.reforgeModifier);
            System.out.println("Luck Modifier (Bônus da Sorte): " + ConfigInit.CONFIG.luckReforgeModifier);
            System.out.println("--- CICLO DE VIDA DOS PESOS ---");

            for (int i = 0; i < attributeWeights.size(); i++) {
                Identifier attrId = potentialAttributes.get(i);

                // Busca o atributo original na memória para ver o peso do JSON
                PotentialAttribute originalAttr = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attrId);
                int baseWeight = originalAttr != null ? originalAttr.getWeight() : -1;
                int weightWithPlusOne = baseWeight + 1;
                int finalWeight = attributeWeights.get(i);

                System.out.println(attrId.getPath() +
                        " -> JSON: " + baseWeight +
                        " | Forja(+1): " + weightWithPlusOne +
                        " | FINAL: " + finalWeight);
            }

            System.out.println("Peso Total da Roleta: " + totalWeight);
            System.out.println("====================================================");
        }
        int randomChoice = new Random().nextInt(totalWeight);

        sortWeightsAndAttributes(attributeWeights, potentialAttributes);

        for (int i = 0; i < attributeWeights.size(); i++) {
            if (randomChoice < attributeWeights.get(i)) {
                return potentialAttributes.get(i);
            }
            randomChoice -= attributeWeights.get(i);
        }

        return potentialAttributes.get(new Random().nextInt(potentialAttributes.size()));
    }

    private static void sortWeightsAndAttributes(List<Integer> weights, List<Identifier> attributes) {
        List<WeightAttributePair> pairs = new ArrayList<>();
        for (int i = 0; i < weights.size(); i++) {
            pairs.add(new WeightAttributePair(weights.get(i), attributes.get(i)));
        }
        pairs.sort((p1, p2) -> Integer.compare(p1.weight, p2.weight));
        weights.clear();
        attributes.clear();
        for (WeightAttributePair pair : pairs) {
            weights.add(pair.weight);
            attributes.add(pair.attribute);
        }
    }

    private static class WeightAttributePair {
        int weight;
        Identifier attribute;
        WeightAttributePair(int weight, Identifier attribute) {
            this.weight = weight;
            this.attribute = attribute;
        }
    }

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge) {
        setItemStackAttribute(playerEntity, stack, reforge, 0);
    }

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge, int merchantLevel) {
        if (!stack.isIn(TieredItemTags.MODIFIER_RESTRICTED)) {
            Identifier potentialAttributeID = getRandomAttributeIDFor(playerEntity, stack.getItem(), reforge, merchantLevel);

            if (potentialAttributeID != null) {
                PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(potentialAttributeID);

                if (potentialAttribute != null) {
                    // 🎰 RODA O CASSINO ARPG PRIMEIRO!
                    // Passamos o ID do tier para ele salvar no RG da arma depois
                    applyARPGModifiers(stack, potentialAttribute);

                    initializeARPGData(stack);
                }
            }
        }
    }

    public static void initializeARPGData(ItemStack stack) {
        if (stack.get(TieredDataComponents.ARPG_DATA) == null) {

            double roll = Math.random();
            int maxSlots = 0;
            if (roll >= 0.85 && roll < 0.95) {
                maxSlots = 1;
            } else if (roll >= 0.95) {
                maxSlots = 2;
            }

            ARPGEquipmentData newData = new ARPGEquipmentData(
                    0, 0, 0, "unawakened",
                    new java.util.HashMap<>(), // 🌟 Mapa de XP vazio, pronto para aprender!
                    maxSlots,
                    new java.util.ArrayList<>(),
                    false
            );

            stack.set(TieredDataComponents.ARPG_DATA, newData);
        }
    }

    // ========================================================================
    // 🎰 O MOTOR ARPG: GERAÇÃO DE STATUS ÚNICOS
    // ========================================================================

    public static void applyARPGModifiers(ItemStack stack, PotentialAttribute tier) {
        AttributeModifiersComponent currentModifiers = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        // Copia os originais (Vanilla), mas DESTRÓI os antigos do nosso mod!
        for (AttributeModifiersComponent.Entry entry : currentModifiers.modifiers()) {
            if (!entry.modifier().id().getNamespace().equals("tiered")) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        Random random = new Random();

        // --- FASE 1: ATRIBUTOS FIXOS (Retrocompatibilidade) ---
        if (tier.getAttributes() != null) {
            for (AttributeTemplate template : tier.getAttributes()) {
                List<EquipmentSlot> requiredSlots = template.getRequiredEquipmentSlots();
                if (requiredSlots == null || requiredSlots.isEmpty()) {
                    EquipmentSlot naturalSlot;
                    if (stack.contains(DataComponentTypes.EQUIPPABLE)) {
                        naturalSlot = stack.get(DataComponentTypes.EQUIPPABLE).slot();
                    } else {
                        naturalSlot = EquipmentSlot.MAINHAND;
                    }
                    template.applyModifiers(naturalSlot, (attribute, modifier) -> {
                        builder.add(attribute, modifier, AttributeModifierSlot.forEquipmentSlot(naturalSlot));
                    });
                } else {
                    for (EquipmentSlot slot : requiredSlots) {
                        template.applyModifiers(slot, (attribute, modifier) -> {
                            builder.add(attribute, modifier, AttributeModifierSlot.forEquipmentSlot(slot));
                        });
                    }
                }
            }
        }

        // --- FASE 2: SORTEIO DO TEMPLATE ---
        List<PotentialAttribute.RollTemplate> rollTemplates = tier.getRollTemplates();
        if (rollTemplates != null && !rollTemplates.isEmpty()) {
            int totalWeight = rollTemplates.stream().mapToInt(PotentialAttribute.RollTemplate::weight).sum();

            if (totalWeight > 0) {
                int choice = random.nextInt(totalWeight);
                PotentialAttribute.RollTemplate selectedRoll = null;

                for (PotentialAttribute.RollTemplate rt : rollTemplates) {
                    if (choice < rt.weight()) {
                        selectedRoll = rt;
                        break;
                    }
                    choice -= rt.weight();
                }

                if (selectedRoll != null) {
                    // --- FASE 3: ROLAGEM DOS ATRIBUTOS POSITIVOS E NEGATIVOS ---
                    List<Identifier> usedAttributes = new ArrayList<>();

                    // 🌟 Voltamos a usar tier.getID() como no seu código original!
                    rollAndApplyPool(stack, builder, tier.getPositivePool(), selectedRoll.positive(), random, tier.getID(), usedAttributes);
                    rollAndApplyPool(stack, builder, tier.getNegativePool(), selectedRoll.negative(), random, tier.getID(), usedAttributes);
                }
            }
        }

        // --- FASE 4: CRAVAR NA ESPADA ---
        AttributeModifiersComponent finalModifiers = builder.build();
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, finalModifiers);

        // =================================================================
        // 🌟 FASE 5: CAÇAR A DURABILIDADE E FAZER O RESET AUTOMÁTICO
        // =================================================================
        float durableFactor = -1f;
        int operation = 0;

        for (AttributeModifiersComponent.Entry entry : finalModifiers.modifiers()) {
            if (entry.attribute().getKey().isPresent() &&
                    entry.attribute().getKey().get().getValue().equals(Identifier.of("tiered", "durable"))) {

                durableFactor = (float) Math.round(entry.modifier().value() * 100.0f) / 100.0f;
                operation = entry.modifier().operation().ordinal();
                break;
            }
        }

        // 🌟 NOVA LÓGICA DE RESET E APLICAÇÃO
        if (stack.contains(DataComponentTypes.MAX_DAMAGE)) {
            // 1. Pega o valor original de fábrica (ex: 407)
            int baseMaxDamage = stack.getItem().getComponents().getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
            // 2. Pega o valor que está na espada agora antes de limpar (ex: 285)
            int oldMaxDamage = stack.getOrDefault(DataComponentTypes.MAX_DAMAGE, baseMaxDamage);

            if (baseMaxDamage > 0) {
                // Por padrão, o novo limite será o original de fábrica (RESET)
                int newMaxDamage = baseMaxDamage;

                // Mas se a roleta sorteou durabilidade, nós calculamos o bônus/punição
                if (durableFactor != -1f) {
                    double calculatedMaxDamage = baseMaxDamage;

                    if (operation == EntityAttributeModifier.Operation.ADD_VALUE.ordinal()) {
                        calculatedMaxDamage += durableFactor;
                    } else if (operation == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE.ordinal() ||
                            operation == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL.ordinal()) {
                        calculatedMaxDamage += (baseMaxDamage * durableFactor);
                    }
                    newMaxDamage = (int) Math.round(calculatedMaxDamage);
                }

                // 3. Injeta o valor final (seja ele o modificado ou o resetado para fábrica)
                stack.set(DataComponentTypes.MAX_DAMAGE, newMaxDamage);

                // 4. Ajuste proporcional da barrinha de dano (para a espada não quebrar do nada)
                if (stack.contains(DataComponentTypes.DAMAGE)) {
                    int currentDamage = stack.getOrDefault(DataComponentTypes.DAMAGE, 0);
                    if (currentDamage > 0 && oldMaxDamage > 0) {
                        // Descobre a % de dano que a espada tinha (ex: estava 50% quebrada)
                        double damagePercent = (double) currentDamage / oldMaxDamage;
                        // Aplica os mesmos 50% de dano no novo limite
                        int newCurrentDamage = (int) Math.round(newMaxDamage * damagePercent);
                        stack.set(DataComponentTypes.DAMAGE, newCurrentDamage);
                    }
                }
            }
        }

        // --- FASE 6: SALVAR O TIER COMPONENT (O RG DA ARMA) ---
        // 🌟 Usamos tier.getID() aqui também para salvar a String correta!
        TierComponent component = new TierComponent(tier.getID(), durableFactor, operation);
        stack.set(Tiered.TIER, component);
    }


    // 🌟 A MUDANÇA: Adicionamos o 'ItemStack stack' aqui no começo!
    private static void rollAndApplyPool(ItemStack stack, AttributeModifiersComponent.Builder builder, List<PotentialAttribute.AttributeRoll> pool, int count, Random random, String tierId, List<Identifier> alreadyUsedAttributes) {
        if (pool == null || pool.isEmpty() || count <= 0) return;

        List<PotentialAttribute.AttributeRoll> availablePool = new ArrayList<>(pool);
        int rollsLeft = count;

        while (rollsLeft > 0 && !availablePool.isEmpty()) {
            int index = random.nextInt(availablePool.size());
            PotentialAttribute.AttributeRoll roll = availablePool.get(index);

            if (alreadyUsedAttributes.contains(roll.type())) {
                availablePool.remove(index);
                continue;
            }

            alreadyUsedAttributes.add(roll.type());
            availablePool.remove(index);
            rollsLeft--;

            double finalValue;

            if (roll.amount().isPresent()) {
                finalValue = roll.amount().get();
            } else {
                double rawValue = roll.min() + (random.nextDouble() * (roll.max() - roll.min()));

                String attrName = roll.type().getPath();
                if (attrName.equals("attack_damage") || attrName.equals("max_health") || attrName.equals("luck") || attrName.equals("safe_fall_distance") || attrName.equals("oxygen_bonus") || attrName.equals("armor_toughness") || attrName.equals("armor") || attrName.equals("entity_interaction_range") || attrName.equals("dig_speed") || attrName.equals("block_break_speed") || attrName.equals("block_interaction_range") || attrName.equals("mining_efficiency") || attrName.equals("range_attack_damage")) {
                    finalValue = Math.round(rawValue);
                } else {
                    finalValue = Math.round(rawValue * 100.0) / 100.0;
                }

                if (finalValue == 0.0 && roll.min() > 0) {
                    finalValue = roll.min();
                }
            }

            var attributeEntry = Registries.ATTRIBUTE.getEntry(roll.type());
            if (attributeEntry.isPresent()) {

                // =====================================================================
                // 🌟 A MÁGICA DO SLOT INTELIGENTE (Movido para CIMA!)
                // =====================================================================
                EquipmentSlot naturalSlot = EquipmentSlot.MAINHAND; // Padrão para itens que não se vestem

                // Na nova engine, nós lemos o slot diretamente do Data Component do item!
                if (stack.contains(DataComponentTypes.EQUIPPABLE)) {
                    naturalSlot = stack.get(DataComponentTypes.EQUIPPABLE).slot();
                }

                String attrName = roll.type().getPath();

                // 🌟 A CORREÇÃO DO CRASH DOS DATAPACKS:
                // Substituímos TODOS os ":" por "_" para garantir que o Identifier seja 100% válido!
                // Ex: "tosted:meu_mod" vira "tosted_meu_mod"
                String safeTierId = tierId.replace(":", "_");
                Identifier modId = Identifier.of("tiered", safeTierId + "_" + attrName + "_" + naturalSlot.getName());


                EntityAttributeModifier modifier = new EntityAttributeModifier(modId, finalValue, roll.operation());

                List<EquipmentSlot> requiredSlots = roll.requiredEquipmentSlots();

                if (requiredSlots == null || requiredSlots.isEmpty()) {
                    // Se o JSON não exigir nenhum slot, aplica no slot natural do item!
                    builder.add(attributeEntry.get(), modifier, AttributeModifierSlot.forEquipmentSlot(naturalSlot));
                } else {
                    // Se o JSON tiver uma lista (ex: ["head", "chest", "legs", "feet"])
                    // Nós verificamos se o slot natural do item está nessa lista.
                    if (requiredSlots.contains(naturalSlot)) {
                        // Aplica APENAS no slot correto do item! (Fim da duplicação!)
                        builder.add(attributeEntry.get(), modifier, AttributeModifierSlot.forEquipmentSlot(naturalSlot));
                    } else {
                        // Fallback: se por algum motivo o item não bater com a lista, usa o primeiro da lista
                        builder.add(attributeEntry.get(), modifier, AttributeModifierSlot.forEquipmentSlot(requiredSlots.get(0)));
                    }
                }
            }
        }
    }

    public static void removeItemStackAttribute(ItemStack itemStack) {
        itemStack.remove(Tiered.TIER);
        // Opcional: Se quiser que remover o Tier também remova os atributos ARPG,
        // você precisaria restaurar os atributos originais do item aqui.
    }

    @Nullable
    public static Identifier getAttributeId(ItemStack itemStack) {
        var tierComponent = itemStack.get(Tiered.TIER);
        if (tierComponent != null) {
            return Identifier.of(tierComponent.tier());
        }
        return null;
    }

    public static void updateItemStackComponent(PlayerInventory playerInventory) {
        for (int u = 0; u < playerInventory.size(); u++) {
            ItemStack itemStack = playerInventory.getStack(u);

            // 🌟 MODERNIZAÇÃO: Usamos contains() para verificar Data Components na 1.21.11
            if (!itemStack.isEmpty() && itemStack.contains(Tiered.TIER)) {
                Identifier currentTierId = Identifier.of(itemStack.get(Tiered.TIER).tier());
                boolean isValid = false;

                // Verifica se o atributo atual ainda existe na memória e se é válido para este item
                PotentialAttribute currentAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(currentTierId);
                if (currentAttribute != null && currentAttribute.isValid(Registries.ITEM.getId(itemStack.getItem()))) {
                    isValid = true;
                }

                // Se o atributo não for mais válido (ex: o dono do servidor deletou o JSON ou mudou as regras)
                if (!isValid) {
                    // 1. Remove o atributo ilegal
                    removeItemStackAttribute(itemStack);

                    // 2. Sorteia um novo atributo válido
                    Identifier newAttributeID = getRandomAttributeIDFor(playerInventory.player, itemStack.getItem(), false);

                    if (newAttributeID != null) {
                        PotentialAttribute newAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(newAttributeID);

                        if (newAttribute != null) {
                            float durableFactor = -1f;
                            int operation = 0;

                            // Recalcula a durabilidade
                            for (AttributeTemplate template : newAttribute.getAttributes()) {
                                if (template.getAttributeTypeID().equals(Identifier.of("tiered", "durable"))) {
                                    durableFactor = (float) Math.round(template.getEntityAttributeModifier().value() * 100.0f) / 100.0f;
                                    operation = template.getEntityAttributeModifier().operation().ordinal();
                                    break;
                                }
                            }

                            // 3. Salva o novo nome e cor no item
                            itemStack.set(Tiered.TIER, new TierComponent(newAttributeID.toString(), durableFactor, operation));

                            // 🌟 CORREÇÃO CRÍTICA: Aplica os status reais (Dano, Crítico, etc) da nova raridade!
                            applyARPGModifiers(itemStack, newAttribute);

                            // 4. Devolve o item consertado para o inventário do jogador
                            playerInventory.setStack(u, itemStack);
                        }
                    }
                }
            }
        }
    }
}