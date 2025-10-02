package net.schn4beltier.instanced_loot.feature.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.schn4beltier.instanced_loot.feature.data.PlayerChestData;
import net.schn4beltier.instanced_loot.feature.logic.LootRoller;
import net.schn4beltier.instanced_loot.feature.menu.PersistingChestMenu;

import java.util.UUID;

@EventBusSubscriber
public class MinecartInstancingHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide()) return;
        if (!(e.getTarget() instanceof MinecartChest cart)) return;

        ServerLevel level = (ServerLevel) e.getLevel();
        ServerPlayer player = (ServerPlayer) e.getEntity();

        ResourceKey<LootTable> lootTableKey = cart.getContainerLootTable();
        if (lootTableKey == null) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.CONSUME);

        String gid = entityContainerId(level, cart.getUUID());
        int size = 27;

        var store = PlayerChestData.get(level);
        UUID pu = player.getUUID();

        ItemStack[] items = store.getOrCreatePlayerLoot(
                gid, pu.toString(),
                () -> LootRoller.rollForPlayer(
                        level, player, lootTableKey, level.getRandom().nextLong(),
                        size, cart.blockPosition(), null
                ), level.registryAccess()
        );

        SimpleContainer container = new SimpleContainer(size) {
            @Override public void setChanged() {
                super.setChanged();
                store.put(gid, pu.toString(), snapshot(this), level.registryAccess());
            }
        };
        for (int i = 0; i < size; i++) container.setItem(i, (i < items.length && items[i] != null) ? items[i] : ItemStack.EMPTY);

        int rows = 3;
        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, ply) -> PersistingChestMenu.forSize(id, inv, container, rows, () -> {
                    store.put(gid, pu.toString(), snapshot(container), level.registryAccess());
                }),
                Component.translatable("entity.minecraft.chest_minecart")
        );

        level.playSound(null, cart.blockPosition(), net.minecraft.sounds.SoundEvents.CHEST_OPEN,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);

        player.openMenu(provider);
    }

    private static ItemStack[] snapshot(Container c) {
        ItemStack[] arr = new ItemStack[c.getContainerSize()];
        for (int i = 0; i < arr.length; i++) arr[i] = c.getItem(i).copy();
        return arr;
    }

    public static String entityContainerId(ServerLevel level, UUID uuid) {
        return level.dimension().location() + "|entity|" + uuid;
    }
}
