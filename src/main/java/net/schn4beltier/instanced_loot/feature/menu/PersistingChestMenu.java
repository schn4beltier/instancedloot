package net.schn4beltier.instanced_loot.feature.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nullable;

public class PersistingChestMenu extends ChestMenu {
    private final Runnable onClose;

    private PersistingChestMenu(@Nullable MenuType<?> type, int id, Inventory playerInv, Container container, int rows, Runnable onClose) {
        super(type, id, playerInv, container, rows);
        this.onClose = onClose;
    }

    public static PersistingChestMenu forSize(int id, Inventory playerInv, Container container, int rows, Runnable onClose) {
        MenuType<?> mt = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        return new PersistingChestMenu(mt, id, playerInv, container, rows, onClose);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (onClose != null) onClose.run();
    }
}
