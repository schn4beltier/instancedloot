package net.schn4beltier.instanced_loot.feature.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.schn4beltier.instanced_loot.Instanced_loot;
import net.schn4beltier.instanced_loot.config.BreakBehaviour;
import net.schn4beltier.instanced_loot.config.Config;
import net.schn4beltier.instanced_loot.feature.logic.LogicalContainer;
import net.schn4beltier.instanced_loot.feature.data.PlayerChestData;

import java.util.*;

@EventBusSubscriber(modid = Instanced_loot.MODID)
public class ChestCleanup {

    private static Map<BlockPos, Integer> chestStates = new HashMap<>();

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        Player player = event.getPlayer();
        if (be instanceof RandomizableContainerBlockEntity rcbe) {

            var data = PlayerChestData.get(level);
            boolean hasData = data.hasDataFor(level, pos);

            if (!hasData)return;

            BreakBehaviour setting = Config.chestBreakBehaviour;

            if (setting == BreakBehaviour.BREAKABLE) {
                LogicalContainer logicalContainer = LogicalContainer.of(level, pos, state, rcbe);
                if (logicalContainer != null) PlayerChestData.get(level).remove(logicalContainer.globalId());
            }
            if (setting == BreakBehaviour.WHILE_SNEAKING) {
                if (Config.breakMessage) {
                    if (chestStates.containsKey(pos)) {
                        if (player.isShiftKeyDown()) {
                            //Breaking allowed
                            event.setCanceled(true);
                            dropPlayerLoot(player, pos);
                            return;
                        } else {
                            event.setCanceled(true);
                            return;
                        }
                    } else {
                        displayMessage(player);
                        chestStates.put(pos, 100);
                        event.setCanceled(true);
                        return;
                    }
                }
                if (player.isShiftKeyDown()) {
                    //Breaking allowed
                    event.setCanceled(true);
                    dropPlayerLoot(player, pos);
                    return;
                }
            }
            if (setting == BreakBehaviour.DISABLED) {
                event.setCanceled(true);
                return;
            }
            if (setting == BreakBehaviour.CREATIVE) {
                if (player.gameMode() == GameType.CREATIVE) {
                    //Breking allowed
                    event.setCanceled(true);
                    dropPlayerLoot(player, pos);
                    return;
                } else {
                    event.setCanceled(true);
                    return;
                }
            }
            if (setting == BreakBehaviour.OP_ONLY) {
                if (player.getPermissionLevel() < 4) {
                    event.setCanceled(true);
                    return;
                }
                event.setCanceled(true);
                dropPlayerLoot(player, pos);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if(!chestStates.isEmpty()){
            Iterator<Map.Entry<BlockPos, Integer>> it = chestStates.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = it.next();
                if (entry.getValue() == 0) {
                    chestStates.remove(entry.getKey());
                    continue;
                }
                chestStates.put(entry.getKey(), entry.getValue() - 1);
            }
        }
    }

    private static void dropPlayerLoot(Player player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof RandomizableContainerBlockEntity)) return;

        var data = PlayerChestData.get(level);
        ItemStack[] items =  data.getItemsFor(level, pos, (ServerPlayer) player);
        for (ItemStack item : items) {
            Instanced_loot.log("Item: " + item);
            dropItem(level, pos, item);
        }
        level.removeBlockEntity(pos);
        level.removeBlock(pos, false);
    }

    private static void dropItem(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity it = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                stack.copy());
        double f = level.random.nextFloat() * 0.5 + 0.25;
        double a = level.random.nextDouble() * Math.PI * 2;
        it.setDeltaMovement(-Math.sin(a) * f, 0.2, Math.cos(a) * f);
        level.addFreshEntity(it);
    }


    private static void displayMessage (Player player) {
        String message = Config.message;
        player.displayClientMessage(Component.literal(message), true);
    }
}

