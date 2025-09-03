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

    private static PlayerChestData fromNbt(CompoundTag tag) {
        PlayerChestData d = new PlayerChestData();
        ListTag containers = tag.getList(KEY_CONTAINERS).isPresent() ? tag.getList(KEY_CONTAINERS).get() : null;
        if (containers == null)return null;
        for (Tag t : containers) {
            CompoundTag c = (CompoundTag) t;
            String id = c.getString(KEY_ID).isPresent() ? c.getString(KEY_ID).get() : null;
            ListTag players = c.getList(KEY_PLAYERS).isPresent() ? c.getList(KEY_PLAYERS).get() : null;
            if (players == null) continue;
            Map<String, ListTag> map = new HashMap<>();
            for (Tag pt : players) {
                CompoundTag pc = (CompoundTag) pt;
                String u = pc.getStringOr(KEY_UUID, "null");
                ListTag inv = pc.getList(KEY_ITEMS).isPresent() ? pc.getList(KEY_ITEMS).get() : null;
                map.put(u, inv);
            }
            d.data.put(id, map);
        }
        return d;
    }

    private CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag containers = new ListTag();
        data.forEach((id, byPlayer) -> {
            CompoundTag c = new CompoundTag();
            c.putString(KEY_ID, id);
            ListTag players = new ListTag();
            byPlayer.forEach((uuid, inv) -> {
                CompoundTag pc = new CompoundTag();
                pc.putString(KEY_UUID, uuid);
                pc.put(KEY_ITEMS, inv.copy());
                players.add(pc);
            });
            c.put(KEY_PLAYERS, players);
            containers.add(c);
        });
        tag.put(KEY_CONTAINERS, containers);
        return tag;
    }

    // ---- ItemStack <-> ListTag ----
    private static ListTag writeInv(ItemStack[] stacks, HolderLookup.Provider regs) {
        ListTag list = new ListTag();
        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, regs);

        for (ItemStack in : stacks) {
            ItemStack s = (in == null) ? ItemStack.EMPTY : in;
            DataResult<Tag> res = ItemStack.CODEC.encodeStart(ops, s);
            Tag tag = res.result().orElseGet(CompoundTag::new); // fallback leeres Compound
            // Optional: immer ein Compound wollen? Dann casten oder konvertieren:
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
            Tag t = list.get(i);
            // Falls du oben „wrap“ benutzt hast:
            if (t instanceof CompoundTag ct && ct.contains("stack")) t = ct.get("stack");

            out[i] = ItemStack.CODEC.parse(ops, t)
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
