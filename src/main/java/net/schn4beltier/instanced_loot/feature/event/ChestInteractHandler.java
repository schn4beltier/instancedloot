package net.schn4beltier.instanced_loot.feature.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.schn4beltier.instanced_loot.*;
import net.schn4beltier.instanced_loot.feature.data.PlayerChestData;
import net.schn4beltier.instanced_loot.feature.logic.LogicalContainer;
import net.schn4beltier.instanced_loot.feature.logic.LootRoller;
import net.schn4beltier.instanced_loot.feature.menu.PersistingChestMenu;

import java.util.UUID;

@EventBusSubscriber(modid = Instanced_loot.MODID)
public class ChestInteractHandler {

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        BlockEntity be = level.getBlockEntity(pos);
        HolderLookup.Provider regs = level.registryAccess();

        if (!(be instanceof RandomizableContainerBlockEntity rcbe)) return;

        var lootTableKey = rcbe.getLootTable();
        if (lootTableKey == null) return;

        var logicalContainer = LogicalContainer.of(level, pos, level.getBlockState(pos), rcbe);
        if (logicalContainer == null) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        logicalContainer.notifyStartOpen(player);

        var store = PlayerChestData.get(level);
        UUID uuid = player.getUUID();
        String globalId = logicalContainer.globalId();

        ItemStack[] items = store.getOrCreatePlayerLoot(globalId, uuid.toString(), () ->
                LootRoller.rollForPlayer(level, (ServerPlayer) player, lootTableKey, rcbe.getLootTableSeed(), logicalContainer.size(), pos, be), regs
        );

        SimpleContainer container = new SimpleContainer(logicalContainer.size()) {
            @Override public void setChanged() {
                super.setChanged();
                store.put(globalId, uuid.toString(), arrayFrom(this), regs);
            }
        };
        for (int i = 0; i < logicalContainer.size(); i++) container.setItem(i, items[i] == null ? ItemStack.EMPTY : items[i]);

        MenuProvider provider = new SimpleMenuProvider((id, inv, ply) ->
                PersistingChestMenu.forSize(id, inv, container, logicalContainer.rows(), () -> {
                    store.put(globalId, uuid.toString(), arrayFrom(container), regs);
                    logicalContainer.notifyStopOpen(ply);
                }), logicalContainer.displayName()
        );

        player.openMenu(provider);
    }

    private static ItemStack[] arrayFrom(SimpleContainer container) {
        ItemStack[] arr = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < arr.length; i++) arr[i] = container.getItem(i).copy();
        return arr;
    }
}
