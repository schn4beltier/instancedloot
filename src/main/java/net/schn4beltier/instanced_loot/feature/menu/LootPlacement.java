package net.schn4beltier.instanced_loot.feature.menu;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LootPlacement {
    private LootPlacement() {}

    public static ItemStack[] distributeRandomly(List<ItemStack> items, int size, long seed) {
        ItemStack[] grid = new ItemStack[size];
        for (int i = 0; i < size; i++) grid[i] = ItemStack.EMPTY;
        
        List<Integer> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) slots.add(i);
        Collections.shuffle(slots, new java.util.Random(seed)); 

        int si = 0;
        for (ItemStack s : items) {
            if (s == null || s.isEmpty()) continue;
            if (si >= slots.size()) break; 
            int slot = slots.get(si++);
            grid[slot] = s.copy();
        }
        return grid;
    }
}
