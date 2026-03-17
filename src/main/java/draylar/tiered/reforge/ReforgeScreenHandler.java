package draylar.tiered.reforge;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.TieredItemTags;
import draylar.tiered.config.ConfigInit;

import java.util.List;

public class ReforgeScreenHandler extends ScreenHandler {

    private final Inventory inventory = new SimpleInventory(3) {
        @Override
        public void markDirty() {
            super.markDirty();
            ReforgeScreenHandler.this.onContentChanged(this);
        }
    };

    private final ScreenHandlerContext context;
    private final PlayerEntity player;
    private BlockPos pos;

    private final PropertyDelegate propertyDelegate;

    public ReforgeScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public ReforgeScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Tiered.REFORGE_SCREEN_HANDLER_TYPE, syncId);

        this.context = context;
        this.player = playerInventory.player;

        this.propertyDelegate = new PropertyDelegate() {
            private int value = 0;
            @Override
            public int get(int index) { return value; }
            @Override
            public void set(int index, int value) { this.value = value; }
            @Override
            public int size() { return 1; }
        };
        this.addProperties(this.propertyDelegate);

        this.addSlot(new Slot(this.inventory, 0, 45, 47));
        this.addSlot(new Slot(this.inventory, 1, 80, 35));
        this.addSlot(new Slot(this.inventory, 2, 115, 47) {
            @Override
            public boolean canInsert(ItemStack stack) {
                // 🌟 PERMITE INSERIR ECHO SHARD PARA O PRESTÍGIO
                return stack.isIn(TieredItemTags.REFORGE_ADDITION) || stack.isOf(net.minecraft.item.Items.ECHO_SHARD);
            }
        });

        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.context.run((world, pos) -> {
            ReforgeScreenHandler.this.setPos(pos);
        });
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        if (inventory == this.inventory) {
            this.updateResult();
        }
    }

    // 🌟 Verifica se o item no slot central está pronto para o Prestígio
    public boolean isPrestigeMode() {
        ItemStack stack = this.getSlot(1).getStack();
        if (stack.contains(draylar.tiered.data.TieredDataComponents.ARPG_DATA)) {
            draylar.tiered.api.ARPGEquipmentData data = stack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            return data != null && data.level() >= 100 && data.prestige() < 3;
        }
        return false;
    }

    // 🌟 Calcula o custo de XP baseado no Prestígio atual (Para UPAR o Prestígio)
    public int getPrestigeXpCost(int currentPrestige) {
        return switch (currentPrestige) {
            case 0 -> 500;
            case 1 -> 750;
            case 2 -> 1000;
            default -> 500;
        };
    }

    // 🌟 Calcula a chance de sucesso baseada no Prestígio e na Sorte do jogador
    public int getPrestigeSuccessChance(int currentPrestige, PlayerEntity player) {
        int baseChance = switch (currentPrestige) {
            case 0 -> 75;
            case 1 -> 50;
            case 2 -> 25;
            default -> 0;
        };

        if (player != null) {
            int luck = (int) player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.LUCK);
            return Math.max(0, Math.min(100, baseChance + luck));
        }

        return baseChance;
    }

    // 🌟 Verifica se a arma já possui uma afinidade despertada
    public boolean isAwakened(ItemStack stack) {
        if (stack.contains(draylar.tiered.data.TieredDataComponents.ARPG_DATA)) {
            draylar.tiered.api.ARPGEquipmentData data = stack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            return data != null && !"unawakened".equals(data.affinity());
        }
        return false;
    }

    // 🌟 NOVO: Calcula o custo de XP para REFORJAR O TIER (Raridade)
    public int getReforgeXpCost(ItemStack stack) {
        int baseCost = ConfigInit.CONFIG.reforgeXpCost;

        // Se a arma for desperta, aplica a tabela de preços punitiva!
        if (isAwakened(stack)) {
            draylar.tiered.api.ARPGEquipmentData data = stack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            if (data != null) {
                int prestige = data.prestige();
                int level = data.level();
                int maxLevel = 100; // Ajuste se o seu level máximo for diferente

                if (prestige == 0) return 200;
                if (prestige == 1) return 250;
                if (prestige == 2) return 300;
                if (prestige >= 3) {
                    if (level >= maxLevel) return 500; // Arma perfeita = Custo máximo
                    return 350;
                }
            }
        }
        return baseCost; // Retorna o custo normal se não for desperta
    }

    private void updateResult() {
        boolean isReady = false;
        ItemStack stack = this.getSlot(1).getStack();

        if (isPrestigeMode()) {
            ItemStack baseItem = this.getSlot(0).getStack();
            ItemStack additionItem = this.getSlot(2).getStack();

            if (baseItem.isOf(net.minecraft.item.Items.NETHER_STAR) && additionItem.isOf(net.minecraft.item.Items.ECHO_SHARD)) {
                isReady = true;
            }

            draylar.tiered.api.ARPGEquipmentData data = stack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            int currentPrestige = data != null ? data.prestige() : 0;

            if (isReady && this.player.totalExperience < getPrestigeXpCost(currentPrestige) && !this.player.isCreative()) {
                isReady = false;
            }
        } else {
            // 🌟 LÓGICA NORMAL DE REFORJA (Trocando a Raridade)
            if (this.getSlot(0).hasStack() && this.getSlot(1).hasStack() && this.getSlot(2).hasStack()) {
                Item item = stack.getItem();
                if (!stack.isIn(TieredItemTags.MODIFIER_RESTRICTED) && ModifierUtils.getRandomAttributeIDFor(null, item, false) != null && !stack.isDamaged()) {
                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
                    ItemStack baseItem = this.getSlot(0).getStack();

                    if (!items.isEmpty()) {
                        isReady = items.stream().anyMatch(it -> it == baseItem.getItem());
                    } else {
                        var repairable = stack.get(DataComponentTypes.REPAIRABLE);
                        if (repairable != null && repairable.items() != null) {
                            isReady = repairable.items().contains(baseItem.getRegistryEntry());
                        } else {
                            isReady = baseItem.isIn(TieredItemTags.REFORGE_BASE_ITEM);
                        }
                    }
                }
            }

            if (isReady && !ConfigInit.CONFIG.uniqueReforge && ModifierUtils.getAttributeId(stack) != null && ModifierUtils.getAttributeId(stack).getPath().contains("unique")) {
                isReady = false;
            }

            if (isReady && ModifierUtils.getAttributeId(stack) != null && ModifierUtils.getAttributeId(stack).getPath().contains("mythic")) {
                isReady = false;
            }

            // 🌟 REMOVIDA A TRAVA DE AFINIDADE AQUI! Agora armas despertas podem ser reforjadas.

            // 🌟 APLICA O CUSTO DINÂMICO
            int xpCost = getReforgeXpCost(stack);
            if (isReady && this.player.totalExperience < xpCost && !this.player.isCreative()) {
                isReady = false;
            }
        }

        this.propertyDelegate.set(0, isReady ? 1 : 0);
    }

    public boolean isReforgeReady() {
        return this.propertyDelegate.get(0) == 1;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> this.dropInventory(player, this.inventory));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.context.get((world, pos) -> {
            return player.squaredDistanceTo((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();

            if (index == 1) {
                if (!this.insertItem(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(itemStack2, itemStack);
            } else if (index == 0 || index == 2) {
                if (!this.insertItem(itemStack2, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 3 && index < 39) {
                if ((itemStack.isIn(TieredItemTags.REFORGE_ADDITION) || itemStack.isOf(net.minecraft.item.Items.ECHO_SHARD)) && !this.insertItem(itemStack2, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }

                if (this.getSlot(1).hasStack()) {
                    ItemStack targetStack = this.getSlot(1).getStack();
                    Item targetItem = targetStack.getItem();

                    var repairable = targetStack.get(DataComponentTypes.REPAIRABLE);
                    if (repairable != null && repairable.items() != null && repairable.items().contains(itemStack.getRegistryEntry())) {
                        if (!this.insertItem(itemStack2, 0, 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if ((itemStack.isIn(TieredItemTags.REFORGE_BASE_ITEM) || itemStack.isOf(net.minecraft.item.Items.NETHER_STAR)) && !this.insertItem(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }

                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(targetItem);
                    if (items.stream().anyMatch(it -> it == itemStack2.copy().getItem()) && !this.insertItem(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false) != null && !this.insertItem(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, itemStack2);
        }
        return itemStack;
    }

    public void reforge() {
        ItemStack itemStack = this.getSlot(1).getStack();

        if (isPrestigeMode()) {
            draylar.tiered.api.ARPGEquipmentData data = itemStack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            if (data == null) return;

            int currentPrestige = data.prestige();
            int xpCost = getPrestigeXpCost(currentPrestige);
            int chance = getPrestigeSuccessChance(currentPrestige,this.player);

            if (!this.player.isCreative()) {
                this.player.addExperience(-xpCost);
            }

            this.decrementStack(0);
            this.decrementStack(2);

            boolean success = this.player.getRandom().nextInt(100) < chance;

            if (success) {
                int newPrestige = currentPrestige + 1;
                int newMaxSlots = data.maxSlots();

                if (newPrestige == 3) {
                    newMaxSlots += 1;
                }

                draylar.tiered.api.ARPGEquipmentData newData = new draylar.tiered.api.ARPGEquipmentData(
                        1, 0, newPrestige, data.affinity(),
                        java.util.Map.of(), newMaxSlots, data.slots(), data.isBroken()
                );
                itemStack.set(draylar.tiered.data.TieredDataComponents.ARPG_DATA, newData);
                draylar.tiered.util.ARPGAttributeHelper.updateModifiers(itemStack);

                this.inventory.setStack(1, itemStack);

                this.context.run((world, pos) -> {
                    world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.5f);
                    world.syncWorldEvent(net.minecraft.world.WorldEvents.ANVIL_USED, pos, 0);
                });
            } else {
                this.context.run((world, pos) -> {
                    world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_ANVIL_DESTROY, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.5f);
                });
            }
            return;
        }

        // 🌟 REFORJA NORMAL (Trocando a Raridade)

        // 🌟 REMOVIDA A TRAVA DE SEGURANÇA AQUI TAMBÉM!

        net.minecraft.util.Identifier attrId = ModifierUtils.getAttributeId(itemStack);

        if (attrId != null) {
            String tierName = attrId.getPath();
            boolean isUniqueLocked = !ConfigInit.CONFIG.uniqueReforge && tierName.contains("unique");
            boolean isMythicLocked = tierName.contains("mythic");

            if (isUniqueLocked || isMythicLocked) {
                return;
            }
        }

        // 🌟 APLICA O CUSTO DINÂMICO NA HORA DE COBRAR
        int xpCost = getReforgeXpCost(itemStack);
        if (!this.player.isCreative()) {
            this.player.addExperience(-xpCost);
        }

        ModifierUtils.removeItemStackAttribute(itemStack);
        ModifierUtils.setItemStackAttribute(player, itemStack, true);

        this.decrementStack(0);
        this.decrementStack(2);

        this.context.run((world, pos) -> world.syncWorldEvent(WorldEvents.ANVIL_USED, (BlockPos) pos, 0));
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    private void decrementStack(int slot) {
        ItemStack itemStack = this.inventory.getStack(slot);
        itemStack.decrement(1);
        this.inventory.setStack(slot, itemStack);
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.inventory && super.canInsertIntoSlot(stack, slot);
    }
}