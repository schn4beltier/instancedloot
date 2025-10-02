package net.schn4beltier.instanced_loot.feature.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.schn4beltier.instanced_loot.feature.data.PlayerChestData;

@EventBusSubscriber
public class MinecartCleanupHandler {

    @SubscribeEvent
    public static void onLeave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (!(e.getEntity() instanceof net.minecraft.world.entity.vehicle.MinecartChest cart)) return;

        ServerLevel level = (ServerLevel) e.getLevel();
        String gid = MinecartInstancingHandler.entityContainerId(level, cart.getUUID());
        PlayerChestData.get(level).remove(gid);
    }
}

