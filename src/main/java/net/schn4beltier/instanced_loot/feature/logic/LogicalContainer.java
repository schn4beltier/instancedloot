package net.schn4beltier.instanced_loot.feature.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.List;

public record LogicalContainer(ResourceKey<Level> dim, BlockPos mainPos, int size, Component displayName,
                               List<RandomizableContainerBlockEntity> parts) {

    public static LogicalContainer of(Level level, BlockPos pos, BlockState state, RandomizableContainerBlockEntity be) {
        if (be instanceof BarrelBlockEntity barrel) {
            return new LogicalContainer(level.dimension(), pos, 27, Component.translatable("container.barrel"), List.of(barrel));
        }
        if (be instanceof ChestBlockEntity chest) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type == ChestType.SINGLE) {
                return new LogicalContainer(level.dimension(), pos, 27, state.getBlock().getName(), List.of(chest));
            } else {
                Direction dir = ChestBlock.getConnectedDirection(state);
                BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
                BlockEntity otherBe = level.getBlockEntity(otherPos);
                if (!(otherBe instanceof ChestBlockEntity otherChest)) return null;

                BlockPos main = (pos.asLong() < otherPos.asLong()) ? pos : otherPos;
                return new LogicalContainer(level.dimension(), main, 54, state.getBlock().getName(), List.of(chest, otherChest));
            }
        }
        return null;
    }

    public String globalId() {
        return this.dim.location().toString() + "|" + this.mainPos.toString();
    }

    public void notifyStartOpen(Player p) {
        for (RandomizableContainerBlockEntity be : parts) be.startOpen(p);
    }
    public void notifyStopOpen(Player p) {
        for (RandomizableContainerBlockEntity be : parts) be.stopOpen(p);
    }

    public int rows() {
        return Mth.clamp((size + 8) / 9, 1, 6);
    }
}
