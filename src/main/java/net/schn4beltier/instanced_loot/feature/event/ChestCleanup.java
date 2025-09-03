package net.schn4beltier.instanced_loot.feature.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.schn4beltier.instanced_loot.Instanced_loot;
import net.schn4beltier.instanced_loot.feature.logic.LogicalContainer;
import net.schn4beltier.instanced_loot.feature.data.PlayerChestData;

@EventBusSubscriber(modid = Instanced_loot.MODID)
public class ChestCleanup {
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RandomizableContainerBlockEntity rcbe))return;
        LogicalContainer lc = LogicalContainer.of(level, pos, state, rcbe);
        if (lc != null) PlayerChestData.get(level).remove(lc.globalId());
    }
}

