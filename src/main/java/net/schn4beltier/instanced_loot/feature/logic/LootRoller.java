package net.schn4beltier.instanced_loot.feature.logic;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

public final class LootRoller {
    private LootRoller() {}

    public static ItemStack[] rollForPlayer(ServerLevel level,
                                            ServerPlayer player,
                                            ResourceKey<LootTable> tableKey,
                                            long tableSeed,
                                            int size,
                                            BlockPos containerPos,
                                            BlockEntity beNullable) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(tableKey);

        long seed = (tableSeed != 0L ? tableSeed : level.random.nextLong())
                ^ player.getUUID().getMostSignificantBits()
                ^ player.getUUID().getLeastSignificantBits();

        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(containerPos))
                .withParameter(LootContextParams.THIS_ENTITY, player);
        if (beNullable != null) {
            builder.withParameter(LootContextParams.BLOCK_ENTITY, beNullable);
        }
        LootParams params = builder.create(LootContextParamSets.CHEST);

        List<ItemStack> generated = table.getRandomItems(params, RandomSource.create(seed));

        ItemStack[] out = new ItemStack[size];
        Arrays.fill(out, ItemStack.EMPTY);
        for (int i = 0; i < size && i < generated.size(); i++) {
            out[i] = generated.get(i).copy();
        }
        return out;
    }
}


