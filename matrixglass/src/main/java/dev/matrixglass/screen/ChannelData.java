package dev.matrixglass.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Which terminal currently owns each of the 8 channels. Saved with the world. */
public class ChannelData extends SavedData {
    private final Map<Integer, GlobalPos> map = new HashMap<>();
    public static final SavedData.Factory<ChannelData> FACTORY = new SavedData.Factory<>(ChannelData::new, ChannelData::load, null);

    public static ChannelData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "matrixglass_channels");
    }

    public GlobalPos get(int channel) { return map.get(channel); }

    public void set(int channel, GlobalPos pos) { map.put(channel, pos); setDirty(); }

    public void remove(GlobalPos pos) {
        if (map.values().removeIf(p -> p.equals(pos))) setDirty();
    }

    static ChannelData load(CompoundTag tag, HolderLookup.Provider lookup) {
        ChannelData d = new ChannelData();
        for (Tag t : tag.getList("channels", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
            d.map.put(c.getInt("ch"), GlobalPos.of(dim, new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"))));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        map.forEach((ch, gp) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("ch", ch); c.putString("dim", gp.dimension().location().toString());
            c.putInt("x", gp.pos().getX()); c.putInt("y", gp.pos().getY()); c.putInt("z", gp.pos().getZ());
            list.add(c);
        });
        tag.put("channels", list);
        return tag;
    }
}
