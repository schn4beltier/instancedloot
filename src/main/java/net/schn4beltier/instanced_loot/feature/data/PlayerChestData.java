package net.schn4beltier.instanced_loot.feature.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.schn4beltier.instanced_loot.Instanced_loot;

import java.util.*;
import java.util.function.Supplier;

public class PlayerChestData extends SavedData {

    private static final String KEY_CONTAINERS = "containers";
    private static final String KEY_ID = "id";
    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_SLOT = "Slot";

    private final Map<String, Map<String, ListTag>> data = new HashMap<>();

    public static final Codec<PlayerChestData> CODEC =
            CompoundTag.CODEC.xmap(PlayerChestData::fromNbt, PlayerChestData::toNbt);

    public static final SavedDataType<PlayerChestData> TYPE =
            new SavedDataType<>(Instanced_loot.MODID + "/player_chest_data", PlayerChestData::new, CODEC);

    public PlayerChestData() {}

    public static PlayerChestData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public ItemStack[] getOrCreatePlayerLoot(String containerId, String playerUUID, Supplier<ItemStack[]> generator, HolderLookup.Provider regs) {
        var byPlayer = data.computeIfAbsent(containerId, k -> new HashMap<>());
        var inv = byPlayer.get(playerUUID);
        if (inv == null) {
            ItemStack[] fresh = sanitize(generator.get());
            byPlayer.put(playerUUID, writeInv(fresh, regs));
            setDirty();
            return fresh;
        }
        return readInv(inv, regs);
    }

    public void put(String containerId, String player, ItemStack[] stacks, HolderLookup.Provider regs) {
        data.computeIfAbsent(containerId, k -> new HashMap<>())
                .put(player, writeInv(sanitize(stacks), regs));
        setDirty();
    }

    public void remove(String containerId) {
        if (data.remove(containerId) != null) setDirty();
    }

    private static PlayerChestData fromNbt(CompoundTag cTag) {
        PlayerChestData data = new PlayerChestData();
        ListTag containers = cTag.getList(KEY_CONTAINERS).isPresent() ? cTag.getList(KEY_CONTAINERS).get() : null;
        if (containers == null)return null;
        for (Tag tag : containers) {
            CompoundTag compoundTag = (CompoundTag) tag;
            String id = compoundTag.getString(KEY_ID).isPresent() ? compoundTag.getString(KEY_ID).get() : null;
            ListTag players = compoundTag.getList(KEY_PLAYERS).isPresent() ? compoundTag.getList(KEY_PLAYERS).get() : null;
            if (players == null) continue;
            Map<String, ListTag> map = new HashMap<>();
            for (Tag playerTag : players) {
                CompoundTag playerCompoundTag = (CompoundTag) playerTag;
                String uuid = playerCompoundTag.getStringOr(KEY_UUID, "null");
                ListTag inv = playerCompoundTag.getList(KEY_ITEMS).isPresent() ? playerCompoundTag.getList(KEY_ITEMS).get() : null;
                map.put(uuid, inv);
            }
            data.data.put(id, map);
        }
        return data;
    }

    private CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag containers = new ListTag();
        data.forEach((id, byPlayer) -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString(KEY_ID, id);
            ListTag players = new ListTag();
            byPlayer.forEach((uuid, inv) -> {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putString(KEY_UUID, uuid);
                playerTag.put(KEY_ITEMS, inv.copy());
                players.add(playerTag);
            });
            cTag.put(KEY_PLAYERS, players);
            containers.add(cTag);
        });
        tag.put(KEY_CONTAINERS, containers);
        return tag;
    }

    private static ListTag writeInv(ItemStack[] stacks, HolderLookup.Provider regs) {
        ListTag list = new ListTag();
        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, regs);

        for (ItemStack in : stacks) {
            ItemStack stack = (in == null) ? ItemStack.EMPTY : in;
            DataResult<Tag> res = ItemStack.CODEC.encodeStart(ops, stack);
            Tag tag = res.result().orElseGet(CompoundTag::new);
            if (!(tag instanceof CompoundTag ct)) {
                CompoundTag wrap = new CompoundTag();
                wrap.put("stack", tag);
                tag = wrap;
            }
            list.add(tag);
        }
        return list;
    }

    private static ItemStack[] readInv(ListTag list, HolderLookup.Provider regs) {
        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, regs);
        ItemStack[] out = new ItemStack[list.size()];

        for (int i = 0; i < list.size(); i++) {
            Tag tag = list.get(i);
            if (tag instanceof CompoundTag cTag && cTag.contains("stack")) tag = cTag.get("stack");

            out[i] = ItemStack.CODEC.parse(ops, tag)
                    .result()
                    .orElse(ItemStack.EMPTY);
        }
        return out;
    }

    private static ItemStack[] sanitize(ItemStack[] in) {
        ItemStack[] out = new ItemStack[in.length];
        for (int i = 0; i < in.length; i++) out[i] = (in[i] == null ? ItemStack.EMPTY : in[i]).copy();
        return out;
    }
}
