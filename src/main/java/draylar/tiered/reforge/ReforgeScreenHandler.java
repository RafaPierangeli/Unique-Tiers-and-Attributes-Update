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

    // 🌟 NOVO: Sistema nativo para sincronizar o botão (substitui o pacote antigo)
    private final PropertyDelegate propertyDelegate;

    // 🌟 NOVO: Construtor do Cliente (Necessário para a 1.21.11)
    public ReforgeScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    // Construtor do Servidor
    public ReforgeScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Tiered.REFORGE_SCREEN_HANDLER_TYPE, syncId);

        this.context = context;
        this.player = playerInventory.player;

        // Inicializa o sincronizador do botão
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

        // 🌟 MANTIVE SUAS COORDENADAS EM "V" AQUI!
        // Slot 0: Ingrediente Base (Esquerda)
        this.addSlot(new Slot(this.inventory, 0, 45, 47));
        // Slot 1: Item a ser reforjado (Centro/Topo)
        this.addSlot(new Slot(this.inventory, 1, 80, 35));
        // Slot 2: Adição (Direita)
        this.addSlot(new Slot(this.inventory, 2, 115, 47) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isIn(TieredItemTags.REFORGE_ADDITION);
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

    private void updateResult() {
        boolean isReady = false;
        ItemStack stack = this.getSlot(1).getStack();

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

        // 🌟 TRAVA DE XP: O botão não acende se tiver menos de 30 pontos de XP
        // 🌟 TRAVA DE XP DINÂMICA
        int xpCost = ConfigInit.CONFIG.reforgeXpCost;
        if (isReady && this.player.totalExperience < xpCost && !this.player.isCreative()) {
            isReady = false;
        }

        // Atualiza o estado do botão para a tela
        this.propertyDelegate.set(0, isReady ? 1 : 0);
    }

    // 🌟 NOVO: Metodo que a Screen chama para saber se acende o botão
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
                if (itemStack.isIn(TieredItemTags.REFORGE_ADDITION) && !this.insertItem(itemStack2, 2, 3, false)) {
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
                    } else if (itemStack.isIn(TieredItemTags.REFORGE_BASE_ITEM) && !this.insertItem(itemStack2, 0, 1, false)) {
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
        // 1. Pega o item que está no slot do meio
        ItemStack itemStack = this.getSlot(1).getStack();

        // 2. Descobre qual é o Tier atual dele
        net.minecraft.util.Identifier attrId = ModifierUtils.getAttributeId(itemStack);

        // 🌟 TRAVA DUPLA: Bloqueia se for Único (e a config proibir) OU se for Mítico!
        if (attrId != null) {
            String tierName = attrId.getPath();
            boolean isUniqueLocked = !ConfigInit.CONFIG.uniqueReforge && tierName.contains("unique");
            boolean isMythicLocked = tierName.contains("mythic"); // Mítico sempre bloqueado

            if (isUniqueLocked || isMythicLocked) {
                return; // Aborta a reforja!
            }
        }

        // 🌟 4. COBRANÇA DE XP DINÂMICA (Só cobra se passou pela trava acima)
        int xpCost = ConfigInit.CONFIG.reforgeXpCost;
        if (!this.player.isCreative()) {
            this.player.addExperience(-xpCost); // Subtrai o valor da config
        }

        // 5. Remove o Tier antigo e rola os dados para um Tier novo
        ModifierUtils.removeItemStackAttribute(itemStack);
        ModifierUtils.setItemStackAttribute(player, itemStack, true);

        // 6. Gasta os ingredientes (Diamante e Ametista)
        this.decrementStack(0);
        this.decrementStack(2);

        // 7. Toca o som da bigorna
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