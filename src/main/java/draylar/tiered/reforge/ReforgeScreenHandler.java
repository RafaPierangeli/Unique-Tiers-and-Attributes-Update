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

    // 🌟 NOVO: Verifica se o item no slot central está pronto para o Prestígio
    public boolean isPrestigeMode() {
        ItemStack stack = this.getSlot(1).getStack();
        if (stack.contains(draylar.tiered.data.TieredDataComponents.ARPG_DATA)) {
            draylar.tiered.api.ARPGEquipmentData data = stack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            return data != null && data.level() >= 100 && data.prestige() < 3;
        }
        return false;
    }

    // 🌟 NOVO: Verifica se a arma já possui uma afinidade despertada
    public boolean isAwakened(ItemStack stack) {
        if (stack.contains(draylar.tiered.data.TieredDataComponents.ARPG_DATA)) {
            draylar.tiered.api.ARPGEquipmentData data = stack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            return data != null && !"unawakened".equals(data.affinity());
        }
        return false;
    }

    private void updateResult() {
        boolean isReady = false;
        ItemStack stack = this.getSlot(1).getStack();

        if (isPrestigeMode()) {
            // 🌟 LÓGICA DE VALIDAÇÃO DO PRESTÍGIO
            ItemStack baseItem = this.getSlot(0).getStack();
            ItemStack additionItem = this.getSlot(2).getStack();

            // Exige Nether Star na esquerda e Echo Shard na direita
            if (baseItem.isOf(net.minecraft.item.Items.NETHER_STAR) && additionItem.isOf(net.minecraft.item.Items.ECHO_SHARD)) {
                isReady = true;
            }

            // Custo fixo de 500 pontos de XP para o Prestígio
            if (isReady && this.player.totalExperience < 500 && !this.player.isCreative()) {
                isReady = false;
            }
        } else {
            // 🌟 LÓGICA NORMAL DE REFORJA
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

            // 🌟 TRAVA DE AFINIDADE: Se já despertou, não pode reforjar a raridade!
            if (isReady && isAwakened(stack)) {
                isReady = false;
            }

            int xpCost = ConfigInit.CONFIG.reforgeXpCost;
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
                // 🌟 PERMITE SHIFT-CLICK DO ECHO SHARD
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
                        // 🌟 PERMITE SHIFT-CLICK DA NETHER STAR
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
            // 🌟 O RITUAL DE PRESTÍGIO
            if (!this.player.isCreative()) {
                this.player.addExperience(-500); // Cobra 500 pontos de XP
            }

            draylar.tiered.api.ARPGEquipmentData data = itemStack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
            if (data != null) {
                // 🌟 CORREÇÃO: Usando os 8 argumentos exatos do seu Record atualizado!
                draylar.tiered.api.ARPGEquipmentData newData = new draylar.tiered.api.ARPGEquipmentData(
                        1, // level: Reseta para Nível 1
                        0, // currentXp: Zera o XP
                        data.prestige() + 1, // prestige: Sobe o Prestígio
                        data.affinity(), // affinity: Mantém a afinidade intacta
                        java.util.Map.of(), // trainingXp: Zera o mapa de treinamento
                        data.maxSlots(), // maxSlots: Mantém o limite de slots
                        data.slots(), // slots: Mantém as runas/gemas já equipadas
                        data.isBroken() // isBroken: Mantém o estado de durabilidade
                );
                itemStack.set(draylar.tiered.data.TieredDataComponents.ARPG_DATA, newData);
            }

            this.decrementStack(0);
            this.decrementStack(2);

            this.context.run((world, pos) -> {
                // Toca um som de Level Up épico em vez do som normal da bigorna
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.5f);
                world.syncWorldEvent(WorldEvents.ANVIL_USED, (BlockPos) pos, 0);
            });
            return; // Aborta a reforja normal para não rolar os status!
        }

        // 🌟 REFORJA NORMAL
        // 🌟 TRAVA DE SEGURANÇA: Impede a execução se estiver despertada
        if (isAwakened(itemStack)) {
            return;
        }
        net.minecraft.util.Identifier attrId = ModifierUtils.getAttributeId(itemStack);

        if (attrId != null) {
            String tierName = attrId.getPath();
            boolean isUniqueLocked = !ConfigInit.CONFIG.uniqueReforge && tierName.contains("unique");
            boolean isMythicLocked = tierName.contains("mythic");

            if (isUniqueLocked || isMythicLocked) {
                return;
            }
        }

        int xpCost = ConfigInit.CONFIG.reforgeXpCost;
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