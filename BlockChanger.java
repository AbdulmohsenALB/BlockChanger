package me.blockchanger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author TheGaming999
 * @version 1.9.0
 * @apiNote 1.7 - 1.21.9 easy to use class to take advantage of different. 1.7 is not fully supported due to significant nms differences between it and the other versions
 * methods
 * that allow you to change blocks at rocket speeds
 * <p>
 * Made with the help of <a href=
 * "https://github.com/CryptoMorin/XSeries/blob/master/src/main/java/com/cryptomorin/xseries/ReflectionUtils.java">ReflectionUtils</a>
 * by <a href="https://github.com/CryptoMorin">CryptoMorin</a>
 * </p>
 * <p>
 * Uses the methods found
 * <a href="https://www.spigotmc.org/threads/395868/">here</a> by
 * <a href="https://www.spigotmc.org/members/220001/">NascentNova</a>
 * </p>
 * <p>
 * Async methods were made using
 * <a href="https://www.spigotmc.org/threads/409003/">How to handle
 * heavy splittable tasks</a> by
 * <a href="https://www.spigotmc.org/members/43809/">7smile7</a>
 * </p>
 */
public class BlockChanger {

    private static final Map<Material, Object> NMS_BLOCK_MATERIALS = new HashMap<>();
    private static final Map<String, Object> NMS_BLOCK_NAMES = new HashMap<>();
    private static final Map<World, Object> NMS_WORLDS = new HashMap<>();
    private static final Map<String, Object> NMS_WORLD_NAMES = new HashMap<>();
    private static final MethodHandle WORLD_GET_HANDLE;
    /**
     * <p>
     * Invoked parameters ->
     * <i>CraftItemStack.asNMSCopy({@literal<org.bukkit.inventory.ItemStack>})</i>
     */
    private static final MethodHandle NMS_ITEM_STACK_COPY;
    /**
     * <p>
     * Invoked parameters ->
     * <i>Block.asBlock({@literal<net.minecraft.world.item.Item>})</i>
     */
    private static final MethodHandle NMS_BLOCK_FROM_ITEM;
    /**
     * <p>
     * Invoked parameters ->
     * <i>Block.getByName({@literal<net.minecraft.world.item.Item>})</i>
     */
    private static final MethodHandle NMS_BLOCK_FROM_NAME;
    /**
     * <p>
     * Invoked parameters ->
     * <i>{@literal<net.minecraft.world.block.Block>}.getName()</i>
     */
    private static final MethodHandle NMS_BLOCK_NAME;
    /**
     * <p>
     * Invoked parameters ->
     * <i>{@literal<net.minecraft.world.item.ItemStack>}.getItem()</i>
     */
    private static final MethodHandle NMS_ITEM_STACK_TO_ITEM;
    /**
     * <p>
     * Changes block data / durability
     * </p>
     * <p>
     * Invoked parameters ->
     * <i>{@literal<net.minecraft.world.block.Block>}.fromLegacyData({@literal<int>});</i>
     * </p>
     */
    private static final MethodHandle BLOCK_DATA_FROM_LEGACY_DATA;
    /**
     * <p>
     * Invoked parameters ->
     * <i>{@literal<net.minecraft.world.level.block.Block>}.getBlockData()</i>
     */
    private static final MethodHandle ITEM_TO_BLOCK_DATA;
    private static final MethodHandle SET_TYPE_AND_DATA;
    private static final MethodHandle WORLD_GET_CHUNK;
    private static final MethodHandle CHUNK_GET_SECTIONS;
    private static final MethodHandle CHUNK_SECTION_SET_TYPE;
    /**
     * <p>
     * Behavior -> <i>{@literal<Chunk>}.getLevelHeightAccessor()</i>
     */
    private static final MethodHandle GET_LEVEL_HEIGHT_ACCESSOR;
    /**
     * <p>
     * Behavior -> <i>{@literal<Chunk>}.getSectionIndex()</i> or
     * <i>{@literal<LevelHeightAccessor>}.getSectionIndex()</i>
     */
    private static final MethodHandle GET_SECTION_INDEX;
    /**
     * <p>
     * Behavior -> <i>Chunk.getSections[{@literal<index>}] =
     * {@literal<ChunkSection>}</i>
     * </p>
     */
    private static final MethodHandle SET_SECTION_ELEMENT;
    private static final MethodHandle CHUNK_SECTION;
    private static final MethodHandle CHUNK_SET_TYPE;
    private static final MethodHandle BLOCK_NOTIFY;
    private static final MethodHandle CRAFT_BLOCK_GET_NMS_BLOCK;
    private static final MethodHandle NMS_BLOCK_GET_BLOCK_DATA;
    /**
     * A map containing placed tile entities, world.capturedTileEntities;
     */
    private static final MethodHandle WORLD_CAPTURED_TILE_ENTITIES;
    /**
     * Check if tile entity is in a map, world.capturedTileEntities.containsKey(x);
     */
    private static final MethodHandle IS_TILE_ENTITY;
    /**
     * Remove a title entity from a map, world.capturedTileEntities.remove(x);
     */
    private static final MethodHandle WORLD_REMOVE_TILE_ENTITY;
    private static final MethodHandle GET_NMS_TILE_ENTITY;
    private static final MethodHandle GET_SNAPSHOT_NBT;
    private static final MethodHandle GET_SNAPSHOT;
    private static final BlockUpdater BLOCK_UPDATER;
    private static final BlockPositionConstructor BLOCK_POSITION_CONSTRUCTOR;
    private static final BlockDataRetriever BLOCK_DATA_GETTER;
    private static final TileEntityManager TILE_ENTITY_MANAGER;
    private static final String AVAILABLE_BLOCKS;
    private static final UncheckedSetters UNCHECKED_SETTERS;
    private static final WorkloadRunnable WORKLOAD_RUNNABLE;
    private static final JavaPlugin PLUGIN;
    private static final Object AIR_BLOCK_DATA;

    static {

        Class<?> worldServer = ReflectionUtils.getNMSClass("server.level",
                ReflectionUtils.supports(21) ? "ServerLevel" : "WorldServer");
        Class<?> world = ReflectionUtils.getNMSClass("world.level",
                ReflectionUtils.supports(21) ? "Level" : "World");
        Class<?> craftWorld = ReflectionUtils.getCraftClass("CraftWorld");
        Class<?> craftBlock = ReflectionUtils.getCraftClass("block.CraftBlock");
        Class<?> blockPosition = ReflectionUtils.supports(8)
                ? ReflectionUtils.getNMSClass("core", ReflectionUtils.supports(21) ? "BlockPos" : "BlockPosition")
                : null;
        Class<?> blocks = ReflectionUtils.getNMSClass("world.level.block", "Blocks");
        Class<?> mutableBlockPosition = ReflectionUtils.supports(8)
                ? ReflectionUtils.getNMSClass("core", ReflectionUtils.supports(21) ? "BlockPos$MutableBlockPos" : "BlockPosition$MutableBlockPosition")
                : null;
        Class<?> blockData = ReflectionUtils.supports(21)
                ? ReflectionUtils.getNMSClass("world.level.block.state", "BlockState")
                : (ReflectionUtils.supports(8)
                ? ReflectionUtils.getNMSClass("world.level.block.state", "IBlockData")
                : null);
        Class<?> craftItemStack = ReflectionUtils.getCraftClass("inventory.CraftItemStack");
        Class<?> worldItemStack = ReflectionUtils.getNMSClass("world.item", "ItemStack");
        Class<?> item = ReflectionUtils.getNMSClass("world.item", "Item");
        Class<?> block = ReflectionUtils.getNMSClass("world.level.block", "Block");
        Class<?> chunk = ReflectionUtils.getNMSClass("world.level.chunk",
                ReflectionUtils.supports(21) ? "LevelChunk" : "Chunk");
        Class<?> chunkSection = ReflectionUtils.getNMSClass("world.level.chunk",
                ReflectionUtils.supports(21) ? "LevelChunkSection" : "ChunkSection");
        Class<?> levelHeightAccessor = ReflectionUtils.supports(17)
                ? ReflectionUtils.getNMSClass("world.level", "LevelHeightAccessor")
                : null;
        Class<?> craftBlockData = ReflectionUtils.getCraftClass("block.data.CraftBlockData");
        Class<?> blockDataReference = ReflectionUtils.supports(21)
                ? craftBlockData
                : (ReflectionUtils.supports(13) ? craftBlock : block);
        Class<?> craftBlockEntityState = ReflectionUtils.supports(12)
                ? ReflectionUtils.getCraftClass("block.CraftBlockEntityState")
                : ReflectionUtils.getCraftClass("block.CraftBlockState");
        Class<?> nbtTagCompound = ReflectionUtils.getNMSClass("nbt", "NBTTagCompound");

        Method getNMSBlockMethod = null;

        if (ReflectionUtils.MINOR_NUMBER <= 12) {
            try {
                getNMSBlockMethod = craftBlock.getDeclaredMethod("getNMSBlock");
                getNMSBlockMethod.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e2) {
                e2.printStackTrace();
            }
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Object airBlockData = null;
        try {
            if (ReflectionUtils.supports(21)) {
                Field airField = blocks.getDeclaredField("AIR");
                airField.setAccessible(true);
                Object airBlock = airField.get(null);
                MethodHandle defaultBlockStateMH = lookup.findVirtual(block, "defaultBlockState",
                        MethodType.methodType(blockData));
                airBlockData = defaultBlockStateMH.invoke(airBlock);
            } else {
                airBlockData = lookup
                        .findStatic(block, ReflectionUtils.supports(18) ? "a" : "getByCombinedId",
                                MethodType.methodType(blockData, int.class))
                        .invoke(0);
            }
        } catch (Throwable e1) {
            e1.printStackTrace();
        }
        AIR_BLOCK_DATA = airBlockData;

        MethodHandle worldGetHandle = null;
        MethodHandle blockPositionXYZ = null;
        MethodHandle nmsItemStackCopy = null;
        MethodHandle blockFromItem = null;
        MethodHandle blockFromName = null;
        MethodHandle blockName = null;
        MethodHandle nmsItemStackToItem = null;
        MethodHandle itemToBlockData = null;
        MethodHandle setTypeAndData = null;
        MethodHandle worldGetChunk = null;
        MethodHandle chunkSetTypeM = null;
        MethodHandle blockNotify = null;
        MethodHandle chunkGetSections = null;
        MethodHandle chunkSectionSetType = null;
        MethodHandle getLevelHeightAccessor = null;
        MethodHandle getSectionIndex = null;
        MethodHandle setSectionElement = null;
        MethodHandle chunkSectionConstructor = null;
        MethodHandle blockDataFromLegacyData = null;
        MethodHandle mutableBlockPositionSet = null;
        MethodHandle mutableBlockPositionXYZ = null;
        MethodHandle craftBlockGetNMSBlock = null;
        MethodHandle nmsBlockGetBlockData = null;
        MethodHandle worldRemoveTileEntity = null;
        MethodHandle worldCapturedTileEntities = null;
        MethodHandle capturedTileEntitiesContainsKey = null;
        MethodHandle getNMSTileEntity = null;
        MethodHandle getSnapshot = null;
        MethodHandle getSnapshotNBT = null;


        String asBlock, blockGetByName, blockGetName, getBlockData, getItem, setType, getChunkAt,
                chunkSetType, notify, getSections, sectionSetType, setXYZ, getBlockData2, removeTileEntity;

        if (ReflectionUtils.supports(21)) {
            // 1.21.X - Mojang mapping
            asBlock = "byItem";
            blockGetByName = "getByName";
            blockGetName = null;
            getBlockData = "defaultBlockState";
            getItem = "getItem";
            setType = "setBlock";
            getChunkAt = "getChunk";
            chunkSetType = "setBlockState";
            notify = "sendBlockUpdated";
            getSections = "getSections";
            sectionSetType = "setBlockState";
            setXYZ = "set";
            getBlockData2 = "getState";
            removeTileEntity = "removeBlockEntity";
        } else {
            // ≤ 1.20.4 existing logic
            asBlock = ReflectionUtils.supports(18) || ReflectionUtils.MINOR_NUMBER < 8 ? "a" : "asBlock";
            blockGetByName = ReflectionUtils.supports(8) ? "getByName" : "idk";
            blockGetName = ReflectionUtils.supports(20) ? ReflectionUtils.supportsPatch(4) ? "h" : "f"
                    : ReflectionUtils.supports(18) ? "h" : "a";
            getBlockData = ReflectionUtils.supports(20) ? ReflectionUtils.supportsPatch(4) ? "o" : "n"
                    : ReflectionUtils.supports(19) ? ReflectionUtils.supportsPatch(3) ? "o" : "m"
                    : ReflectionUtils.supports(18) ? "n" : "getBlockData";
            getItem = ReflectionUtils.supports(20) ? "d" : ReflectionUtils.supports(18) ? "c" : "getItem";
            setType = ReflectionUtils.supports(18) ? "a" : "setTypeAndData";
            getChunkAt = ReflectionUtils.supports(18) ? "d" : "getChunkAt";
            chunkSetType = ReflectionUtils.supports(18) ? "a" : ReflectionUtils.MINOR_NUMBER < 8 ? "setTypeId"
                    : ReflectionUtils.MINOR_NUMBER <= 12 ? "a" : "setType";
            notify = ReflectionUtils.supports(18) ? "a" : "notify";
            getSections = ReflectionUtils.supports(18) ? "d" : "getSections";
            sectionSetType = ReflectionUtils.supports(18) ? "a" : ReflectionUtils.MINOR_NUMBER < 8 ? "setTypeId"
                    : "setType";
            setXYZ = ReflectionUtils.supports(13) ? "d" : "c";
            getBlockData2 = ReflectionUtils.supports(13) ? "getNMS" : "getBlockData";
            removeTileEntity = ReflectionUtils.supports(20) && ReflectionUtils.supportsPatch(4) ? "o"
                    : ReflectionUtils.supports(19) ? "n" : ReflectionUtils.supports(18) ? "m"
                    : ReflectionUtils.supports(14) ? "removeTileEntity" : ReflectionUtils.supports(13) ? "n"
                    : ReflectionUtils.supports(9) ? "s" : ReflectionUtils.supports(8) ? "t" : "p";
        }

        MethodType notifyMethodType = ReflectionUtils.MINOR_NUMBER >= 14 ? MethodType.methodType(void.class,
                blockPosition, blockData, blockData, int.class)
                : ReflectionUtils.MINOR_NUMBER < 8 ? MethodType.methodType(void.class, int.class, int.class, int.class)
                : ReflectionUtils.MINOR_NUMBER == 8 ? MethodType.methodType(void.class, blockPosition)
                : MethodType.methodType(void.class, blockPosition, blockData, blockData, int.class);

        MethodType chunkSetTypeMethodType = ReflectionUtils.MINOR_NUMBER <= 12
                ? ReflectionUtils.MINOR_NUMBER >= 8 ? MethodType.methodType(blockData, blockPosition, blockData)
                : MethodType.methodType(boolean.class, int.class, int.class, int.class, block, int.class)
                : MethodType.methodType(blockData, blockPosition, blockData, boolean.class);

        MethodType chunkSectionSetTypeMethodType = ReflectionUtils.MINOR_NUMBER >= 14 ? MethodType.methodType(blockData,
                int.class, int.class, int.class, blockData)
                : ReflectionUtils.MINOR_NUMBER < 8
                ? MethodType.methodType(void.class, int.class, int.class, int.class, block)
                : MethodType.methodType(void.class, int.class, int.class, int.class, blockData);

        MethodType chunkSectionConstructorMT = ReflectionUtils.supports(18) ? null
                : ReflectionUtils.supports(14) ? MethodType.methodType(void.class, int.class)
                : MethodType.methodType(void.class, int.class, boolean.class);

        MethodType removeTileEntityMethodType = ReflectionUtils.supports(8)
                ? MethodType.methodType(void.class, blockPosition)
                : MethodType.methodType(void.class, int.class, int.class, int.class);

        MethodType fromLegacyDataMethodType = ReflectionUtils.MINOR_NUMBER <= 12
                ? MethodType.methodType(blockData, int.class) : null;

        BlockPositionConstructor blockPositionConstructor = null;

        try {
            worldGetHandle = lookup.findVirtual(craftWorld, "getHandle", MethodType.methodType(worldServer));
            worldGetChunk = lookup.findVirtual(worldServer, getChunkAt,
                    MethodType.methodType(chunk, int.class, int.class));
            nmsItemStackCopy = lookup.findStatic(craftItemStack, "asNMSCopy",
                    MethodType.methodType(worldItemStack, ItemStack.class));
            blockFromItem = lookup.findStatic(block, asBlock, MethodType.methodType(block, item));

            // Safety net for 1.21.X
            if (blockGetName != null)
                blockName = lookup.findVirtual(block, blockGetName, MethodType.methodType(String.class));
            if (ReflectionUtils.MINOR_NUMBER < 18) {
                blockFromName = lookup.findStatic(block, blockGetByName, MethodType.methodType(block, String.class));
            }
            if (ReflectionUtils.supports(8)) {
                blockPositionXYZ = lookup.findConstructor(blockPosition,
                        MethodType.methodType(void.class, int.class, int.class, int.class));
                mutableBlockPositionXYZ = lookup.findConstructor(mutableBlockPosition,
                        MethodType.methodType(void.class, int.class, int.class, int.class));
                itemToBlockData = lookup.findVirtual(block, getBlockData, MethodType.methodType(blockData));
                setTypeAndData = lookup.findVirtual(worldServer, setType,
                        MethodType.methodType(boolean.class, blockPosition, blockData, int.class));
                mutableBlockPositionSet = lookup.findVirtual(mutableBlockPosition, setXYZ,
                        MethodType.methodType(mutableBlockPosition, int.class, int.class, int.class));
                blockPositionConstructor = new BlockPositionNormal(blockPositionXYZ, mutableBlockPositionXYZ,
                        mutableBlockPositionSet);
            } else {
                blockPositionXYZ = lookup.findConstructor(Location.class,
                        MethodType.methodType(void.class, World.class, double.class, double.class, double.class));
                mutableBlockPositionXYZ = lookup.findConstructor(Location.class,
                        MethodType.methodType(void.class, World.class, double.class, double.class, double.class));
                blockPositionConstructor = new BlockPositionAncient(blockPositionXYZ, mutableBlockPositionXYZ);
            }
            nmsItemStackToItem = lookup.findVirtual(worldItemStack, getItem, MethodType.methodType(item));
            blockDataFromLegacyData = ReflectionUtils.MINOR_NUMBER <= 12
                    ? lookup.findVirtual(block, "fromLegacyData", fromLegacyDataMethodType) : null;
            chunkSetTypeM = lookup.findVirtual(chunk, chunkSetType, chunkSetTypeMethodType);
            blockNotify = lookup.findVirtual(worldServer, notify, notifyMethodType);
            chunkGetSections = lookup.findVirtual(chunk, getSections,
                    MethodType.methodType(ReflectionUtils.toArrayClass(chunkSection)));
            chunkSectionSetType = lookup.findVirtual(chunkSection, sectionSetType, chunkSectionSetTypeMethodType);
            setSectionElement = MethodHandles.arrayElementSetter(ReflectionUtils.toArrayClass(chunkSection));
            chunkSectionConstructor = !ReflectionUtils.supports(18)
                    ? lookup.findConstructor(chunkSection, chunkSectionConstructorMT) : null;

            if (ReflectionUtils.supports(21)) {
                // 1) Try the simple route: LevelChunk#getSectionIndex(int) directly.
                try {
                    getSectionIndex = lookup.findVirtual(
                            chunk,                         // net.minecraft.world.level.chunk.LevelChunk
                            "getSectionIndex",
                            MethodType.methodType(int.class, int.class)
                    );

                    // Make "levelHeightAccessorGetter" return the chunk itself (identity),
                    // so BlockUpdaterLatest can call it and get the same object back.
                    // We use Object identity and adapt the type to (chunk) -> Object.
                    MethodHandle id = MethodHandles.identity(Object.class);
                    getLevelHeightAccessor = id.asType(MethodType.methodType(Object.class, chunk));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    // 2) Fallback: LevelChunk#getLevelHeightAccessor() -> LevelHeightAccessor#getSectionIndex(int)
                    getLevelHeightAccessor = lookup.findVirtual(
                            chunk,
                            "getLevelHeightAccessor",
                            MethodType.methodType(levelHeightAccessor)
                    );
                    getSectionIndex = lookup.findVirtual(
                            levelHeightAccessor,
                            "getSectionIndex",
                            MethodType.methodType(int.class, int.class)
                    );
                }
            } else if (ReflectionUtils.supports(18)) {
                // obfuscated path for 1.18–1.20.x
                getLevelHeightAccessor = lookup.findVirtual(
                        chunk,
                        "z",
                        MethodType.methodType(levelHeightAccessor)
                );
                getSectionIndex = lookup.findVirtual(
                        levelHeightAccessor,
                        "e",
                        MethodType.methodType(int.class, int.class)
                );
            } else if (ReflectionUtils.supports(17)) {
                getSectionIndex = lookup.findVirtual(
                        chunk,
                        "getSectionIndex",
                        MethodType.methodType(int.class, int.class)
                );
            }
            craftBlockGetNMSBlock = ReflectionUtils.MINOR_NUMBER <= 12 ? lookup.unreflect(getNMSBlockMethod) : null;
            nmsBlockGetBlockData = lookup.findVirtual(blockDataReference, getBlockData2,
                    MethodType.methodType(blockData));
            worldRemoveTileEntity = lookup.findVirtual(world, removeTileEntity, removeTileEntityMethodType);
            worldCapturedTileEntities = ReflectionUtils.supports(8)
                    ? lookup.findGetter(world, "capturedTileEntities", Map.class) : null;
            capturedTileEntitiesContainsKey = ReflectionUtils.supports(8)
                    ? lookup.findVirtual(Map.class, "containsKey", MethodType.methodType(boolean.class, Object.class))
                    : null;
            Method getTileEntityMethod = craftBlockEntityState.getDeclaredMethod("getTileEntity");
            Method getSnapshotMethod = ReflectionUtils.supports(12)
                    ? craftBlockEntityState.getDeclaredMethod("getSnapshot") : null;
            if (getTileEntityMethod != null) getTileEntityMethod.setAccessible(true);
            if (getSnapshotMethod != null) getSnapshotMethod.setAccessible(true);
            getNMSTileEntity = lookup.unreflect(getTileEntityMethod);
            getSnapshot = ReflectionUtils.supports(12) ? lookup.unreflect(getSnapshotMethod) : null;
            getSnapshotNBT = ReflectionUtils.supports(12)
                    ? lookup.findVirtual(craftBlockEntityState, "getSnapshotNBT", MethodType.methodType(nbtTagCompound))
                    : null;
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        WORLD_GET_HANDLE = worldGetHandle;
        WORLD_GET_CHUNK = worldGetChunk;
        NMS_ITEM_STACK_COPY = nmsItemStackCopy;
        NMS_BLOCK_FROM_ITEM = blockFromItem;
        NMS_BLOCK_FROM_NAME = blockFromName;
        NMS_BLOCK_NAME = blockName;
        NMS_ITEM_STACK_TO_ITEM = nmsItemStackToItem;
        ITEM_TO_BLOCK_DATA = itemToBlockData;
        SET_TYPE_AND_DATA = setTypeAndData;
        CHUNK_SET_TYPE = chunkSetTypeM;
        BLOCK_NOTIFY = blockNotify;
        CHUNK_GET_SECTIONS = chunkGetSections;
        CHUNK_SECTION_SET_TYPE = chunkSectionSetType;
        GET_LEVEL_HEIGHT_ACCESSOR = getLevelHeightAccessor;
        GET_SECTION_INDEX = getSectionIndex;
        SET_SECTION_ELEMENT = setSectionElement;
        CHUNK_SECTION = chunkSectionConstructor;
        BLOCK_POSITION_CONSTRUCTOR = blockPositionConstructor;
        BLOCK_DATA_FROM_LEGACY_DATA = blockDataFromLegacyData;
        CRAFT_BLOCK_GET_NMS_BLOCK = craftBlockGetNMSBlock;
        NMS_BLOCK_GET_BLOCK_DATA = nmsBlockGetBlockData;
        WORLD_REMOVE_TILE_ENTITY = worldRemoveTileEntity;
        WORLD_CAPTURED_TILE_ENTITIES = worldCapturedTileEntities;
        IS_TILE_ENTITY = capturedTileEntitiesContainsKey;
        GET_NMS_TILE_ENTITY = getNMSTileEntity;
        GET_SNAPSHOT = getSnapshot;
        GET_SNAPSHOT_NBT = getSnapshotNBT;

        BLOCK_DATA_GETTER = ReflectionUtils.supports(13) ? new BlockDataGetter()
                : ReflectionUtils.supports(8) ? new BlockDataGetterLegacy() : new BlockDataGetterAncient();

        BLOCK_UPDATER = ReflectionUtils.supports(18) ? new BlockUpdaterLatest(BLOCK_NOTIFY, CHUNK_SET_TYPE,
                GET_SECTION_INDEX, GET_LEVEL_HEIGHT_ACCESSOR)
                : ReflectionUtils.supports(17) ? new BlockUpdater17(BLOCK_NOTIFY, CHUNK_SET_TYPE, GET_SECTION_INDEX,
                CHUNK_SECTION, SET_SECTION_ELEMENT)
                : ReflectionUtils.supports(13)
                ? new BlockUpdater13(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT)
                : ReflectionUtils.supports(9)
                ? new BlockUpdater9(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT)
                : ReflectionUtils.supports(8)
                ? new BlockUpdaterLegacy(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT)
                : new BlockUpdaterAncient(BLOCK_NOTIFY, CHUNK_SET_TYPE, CHUNK_SECTION, SET_SECTION_ELEMENT);

        TILE_ENTITY_MANAGER = ReflectionUtils.supports(8) ? new TileEntityManagerSupported()
                : new TileEntityManagerDummy();

        Arrays.stream(Material.values()).filter(Material::isBlock)
                .filter(Material::isItem)
                .forEach(BlockChanger::addNMSBlockData);

        NMS_BLOCK_MATERIALS.put(Material.AIR, AIR_BLOCK_DATA);

        AVAILABLE_BLOCKS = String.join(", ",
                NMS_BLOCK_MATERIALS.keySet()
                        .stream()
                        .map(Material::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()));


        if (NMS_BLOCK_NAME != null) {
            Arrays.stream(blocks.getDeclaredFields())
                    .filter(field -> field.getType() == block)
                    .map(field -> {
                        try {
                            return field.get(block);
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .forEach(nmsBlock -> {
                        try {
                            String name = (String) NMS_BLOCK_NAME.invoke(nmsBlock);
                            name = name.substring(name.lastIndexOf(".") + 1).toUpperCase();
                            NMS_BLOCK_NAMES.put(name, nmsBlock);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
        }

        Bukkit.getWorlds().forEach(BlockChanger::addNMSWorld);

        UNCHECKED_SETTERS = new UncheckedSetters();

        WORKLOAD_RUNNABLE = new WorkloadRunnable();

        PLUGIN = JavaPlugin.getProvidingPlugin(BlockChanger.class);

        Bukkit.getScheduler().runTaskTimer(PLUGIN, WORKLOAD_RUNNABLE, 1, 1);

    }

    /**
     * Simply calls <b>static {}</b> so methods get cached, and ensures that the
     * first setBlock method call is executed as fast as possible. In addition to
     * that, it checks whether methods have been initalized correctly or not by
     * spitting exceptions if there is any issue.
     * <p>
     * This already happens when calling a method for the first time.
     * </p>
     * <p>
     * Added for debugging purposes.
     * </p>
     */
    public static void test() {
    }

    private static void addNMSBlockData(Material material) {
        if (material == null || !material.isBlock() || !material.isItem()) return;
        try {
            ItemStack itemStack = new ItemStack(material);
            Object nmsData = getNMSBlockData(itemStack);
            if (nmsData != null) NMS_BLOCK_MATERIALS.put(material, nmsData);
        } catch (Throwable t) {
            // Some exotic materials may still fail in edge cases;
        }
    }

    private static void addNMSWorld(World world) {
        if (world == null) return;
        Object nmsWorld = getNMSWorld(world);
        if (nmsWorld != null) {
            NMS_WORLDS.put(world, nmsWorld);
            NMS_WORLD_NAMES.put(world.getName(), nmsWorld);
        }
    }

    /**
     * If a material fails to pass this method, then it cannot be placed using any
     * of the setBlock methods.
     *
     * @param material to check
     * @return whether the given material can be placed or not
     */
    public static boolean isPlaceable(Material material) {
        try {
            return NMS_BLOCK_MATERIALS.containsKey(material) || NMS_BLOCK_NAMES.containsKey(material.name())
                    || BLOCK_DATA_GETTER.getNMSItem(new ItemStack(material)) != null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * If an ItemStack fails to pass this method, then it cannot be placed using any
     * of this class methods.
     *
     * @param itemStack to check
     * @return whether the given ItemStack can be placed or not
     */
    public static boolean isPlaceable(ItemStack itemStack) {
        Material mat = itemStack.getType();
        try {
            return NMS_BLOCK_MATERIALS.containsKey(mat) || NMS_BLOCK_NAMES.containsKey(mat.name())
                    || BLOCK_DATA_GETTER.getNMSItem(itemStack) != null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isValidNMSBlockName(String name) {
        return NMS_BLOCK_NAMES.containsKey(name);
    }

    public static boolean isValidNMSBlockName(ItemStack itemStack) {
        return NMS_BLOCK_NAMES.containsKey(itemStack.getType().name());
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world    world where the block is located
     * @param x        x location point
     * @param y        y location point
     * @param z        z location point
     * @param material block material to apply on the created block
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     */
    public static void setBlock(World world, int x, int y, int z, Material material) {
        setBlock(world, x, y, z, material, true);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world     world where the block is located
     * @param x         x location point
     * @param y         y location point
     * @param z         z location point
     * @param itemStack ItemStack to apply on the created block
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     */
    public static void setBlock(World world, int x, int y, int z, ItemStack itemStack) {
        setBlock(world, x, y, z, itemStack, true);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world    world where the block is located
     * @param x        x location point
     * @param y        y location point
     * @param z        z location point
     * @param material block material to apply on the created block
     * @param physics  whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     */
    public static void setBlock(World world, int x, int y, int z, Material material, boolean physics) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        Object nmsWorld = getWorld(world);
        Object blockPosition = newBlockPosition(world, x, y, z);
        Object blockData = getBlockData(material);
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world     world where the block is located
     * @param x         x location point
     * @param y         y location point
     * @param z         z location point
     * @param itemStack ItemStack to apply on the created block
     * @param physics   whether physics such as gravity should be applied or not
     */
    public static void setBlock(World world, int x, int y, int z, ItemStack itemStack, boolean physics) {
        Object nmsWorld = getWorld(world);
        Object blockPosition = newBlockPosition(world, x, y, z);
        Object blockData = getBlockData(itemStack);
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world    world where the block is located
     * @param location location to put the block at
     * @param material block material to apply on the created block
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static void setBlock(World world, Location location, Material material) {
        setBlock(world, location, material, true);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world     world where the block is located
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the created block
     */
    public static void setBlock(World world, Location location, ItemStack itemStack) {
        setBlock(world, location, itemStack, true);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world    world where the block is located
     * @param location location to put the block at
     * @param material block material to apply on the created block
     * @param physics  whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static void setBlock(World world, Location location, Material material, boolean physics) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        Object nmsWorld = getWorld(world);
        Object blockPosition = newBlockPosition(world, location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(material);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world     world where the block is located
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the created block
     * @param physics   whether physics such as gravity should be applied or not
     */
    public static void setBlock(World world, Location location, ItemStack itemStack, boolean physics) {
        Object nmsWorld = getWorld(world);
        Object blockPosition = newBlockPosition(world, location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(itemStack);
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param location location to put the block at
     * @param material block material to apply on the created block
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static void setBlock(Location location, Material material) {
        setBlock(location, material, true);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the created block
     */
    public static void setBlock(Location location, ItemStack itemStack) {
        setBlock(location, itemStack, true);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param location location to put the block at
     * @param material block material to apply on the created block
     * @param physics  whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static void setBlock(Location location, Material material, boolean physics) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        Object nmsWorld = getWorld(location.getWorld());
        Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(material);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
    }

    /**
     * Changes block type using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the created block
     * @param physics   whether physics such as gravity should be applied or not
     */
    public static void setBlock(Location location, ItemStack itemStack, boolean physics) {
        Object nmsWorld = getWorld(location.getWorld());
        Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(itemStack);
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeAndData(nmsWorld, blockPosition, blockData, physics ? 3 : 2);
    }

    /**
     * Asynchronously changes block type using native NMS world block type and data
     * setter {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     * <br>
     * <br>
     * Async within this context means:
     * <ul>
     * <li>There won't be any TPS loss no matter the amount of blocks being set</li>
     * <li>It can be safely executed inside an asynchronous task</li>
     * </ul>
     *
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the created block
     * @param physics   whether physics such as gravity should be applied or not
     */
    public static CompletableFuture<Void> setBlockAsynchronously(Location location, ItemStack itemStack,
                                                                 boolean physics) {
        Object nmsWorld = getWorld(location.getWorld());
        Object blockPosition = newMutableBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(itemStack);
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
        WORKLOAD_RUNNABLE.addWorkload(new BlockSetWorkload(nmsWorld, blockPosition, blockData, location, physics));
        WORKLOAD_RUNNABLE.whenComplete(() -> workloadFinishFuture.complete(null));
        return workloadFinishFuture;
    }

    /**
     * Mass changes block types using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world     world where the blocks are located at
     * @param locations locations to put the block at
     * @param material  block material to apply on the created blocks
     * @param physics   whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static void setBlocks(World world, Collection<Location> locations, Material material, boolean physics) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(material);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        int applyPhysics = physics ? 3 : 2;
        locations.forEach(location -> {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            setBlockPosition(blockPosition, x, y, z);
            removeIfTileEntity(nmsWorld, blockPosition);
            setTypeAndData(nmsWorld, blockPosition, blockData, applyPhysics);
        });
    }

    /**
     * Mass changes block types using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     *
     * @param world     world where the blocks are located at
     * @param locations locations to put the block at
     * @param itemStack ItemStack to apply on the created blocks
     * @param physics   whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static void setBlocks(World world, Collection<Location> locations, ItemStack itemStack, boolean physics) {
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        int applyPhysics = physics ? 3 : 2;
        locations.forEach(location -> {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            setBlockPosition(blockPosition, x, y, z);
            removeIfTileEntity(nmsWorld, blockPosition);
            setTypeAndData(nmsWorld, blockPosition, blockData, applyPhysics);
        });
    }

    /**
     * Asynchronously changes block types using native NMS world block type and data
     * setter {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     * <br>
     * <br>
     * Async within this context means:
     * <ul>
     * <li>There won't be any TPS loss no matter the amount of blocks being set</li>
     * <li>It can be safely executed inside an asynchronous task</li>
     * </ul>
     *
     * @param world     world where the blocks are located at
     * @param locations locations to put the block at
     * @param itemStack ItemStack to apply on the created blocks
     * @param physics   whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static CompletableFuture<Void> setBlocksAsynchronously(World world, Collection<Location> locations,
                                                                  ItemStack itemStack, boolean physics) {
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
        WorkloadRunnable workloadRunnable = new WorkloadRunnable();
        BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
        locations.forEach(location -> workloadRunnable
                .addWorkload(new BlockSetWorkload(nmsWorld, blockPosition, blockData, location, physics)));
        workloadRunnable.whenComplete(() -> {
            workloadFinishFuture.complete(null);
            workloadTask.cancel();
        });
        return workloadFinishFuture;
    }

    /**
     * Asynchronously fills a cuboid from a corner to another with blocks retrieved
     * from the given ItemStack
     * using native NMS world block type and data setter
     * {@code nmsWorld.setTypeAndData(...)},
     * which surpasses bukkit's {@linkplain org.bukkit.block.Block#setType(Material)
     * Block.setType(Material)} speed.
     * <br>
     * <br>
     * Async within this context means:
     * <ul>
     * <li>There won't be any TPS loss no matter the amount of blocks being set</li>
     * <li>It can be safely executed inside an asynchronous task</li>
     * </ul>
     *
     * @param world     world where the blocks are located at
     * @param loc1      first corner
     * @param loc2      second corner
     * @param itemStack ItemStack to apply on the created blocks
     * @param physics   whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static CompletableFuture<Void> setCuboidAsynchronously(Location loc1, Location loc2, ItemStack itemStack,
                                                                  boolean physics) {
        World world = loc1.getWorld();
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        int baseX = x1;
        int baseY = y1;
        int baseZ = z1;
        int sizeX = Math.abs(x2 - x1) + 1;
        int sizeY = Math.abs(y2 - y1) + 1;
        int sizeZ = Math.abs(z2 - z1) + 1;
        int x3 = 0, y3 = 0, z3 = 0;
        Location location = new Location(world, baseX + x3, baseY + y3, baseZ + z3);
        int cuboidSize = sizeX * sizeY * sizeZ;
        Object blockPosition = newMutableBlockPosition(location);
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
        WorkloadRunnable workloadRunnable = new WorkloadRunnable();
        BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
        for (int i = 0; i < cuboidSize; i++) {
            BlockSetWorkload workload = new BlockSetWorkload(nmsWorld, blockPosition, blockData, location.clone(),
                    physics);
            if (++x3 >= sizeX) {
                x3 = 0;
                if (++y3 >= sizeY) {
                    y3 = 0;
                    ++z3;
                }
            }
            location.setX(baseX + x3);
            location.setY(baseY + y3);
            location.setZ(baseZ + z3);
            workloadRunnable.addWorkload(workload);
        }
        workloadRunnable.whenComplete(() -> {
            workloadFinishFuture.complete(null);
            workloadTask.cancel();
        });
        return workloadFinishFuture;
    }

    /**
     * <p>
     * Changes block type using Chunk block setter, which in an NMS code, reads as
     * follows {@code nmsChunk.setType(...)} which surpasses
     * {@code nmsWorld.setTypeAndData(...)}
     * speed due to absence of light updates, the method that
     * {@link #setBlock(Location, Material)} uses. Then,
     * notifies the world of the updated blocks so they can be seen by the players.
     *
     * @param location location to put the block at
     * @param material material to apply on the block
     */
    public static void setChunkBlock(Location location, Material material) {
        setChunkBlock(location, material, false);
    }

    /**
     * <p>
     * Changes block type using Chunk block setter, which in an NMS code, reads as
     * follows {@code nmsChunk.setType(...)} which surpasses
     * {@code nmsWorld.setTypeAndData(...)}
     * speed due to absence of light updates, the method that
     * {@link #setBlock(Location, ItemStack)} uses. Then,
     * notifies the world of the updated blocks so they can be seen by the players.
     *
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the block
     */
    public static void setChunkBlock(Location location, ItemStack itemStack) {
        setChunkBlock(location, itemStack, false);
    }

    /**
     * <p>
     * Changes block type using Chunk block setter, which in an NMS code, reads as
     * follows {@code nmsChunk.setType(...)} which surpasses
     * {@code nmsWorld.setTypeAndData(...)}
     * speed due to absence of light updates, the method that
     * {@link #setBlock(Location, ItemStack)} uses. Then,
     * notifies the world of the updated blocks so they can be seen by the players.
     *
     * @param location location to put the block at
     * @param material material to apply on the block
     * @param physics  whether physics should be applied or not
     */
    public static void setChunkBlock(Location location, Material material, boolean physics) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        Object nmsWorld = getWorld(location.getWorld());
        Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(material);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        Object chunk = getChunkAt(nmsWorld, location);
        removeIfTileEntity(nmsWorld, blockPosition);
        setType(chunk, blockPosition, blockData, physics);
        updateBlock(nmsWorld, blockPosition, blockData, physics);
    }

    /**
     * <p>
     * Changes block type using Chunk block setter, which in an NMS code, reads as
     * follows {@code nmsChunk.setType(...)} which surpasses
     * {@code nmsWorld.setTypeAndData(...)}
     * speed due to absence of light updates, the method that
     * {@link #setBlock(Location, ItemStack)} uses. Then,
     * notifies the world of the updated blocks so they can be seen by the players.
     *
     * @param location  location to put the block at
     * @param itemStack itemStack to apply on the block
     * @param physics   whether physics should be applied or not
     */
    public static void setChunkBlock(Location location, ItemStack itemStack, boolean physics) {
        Object nmsWorld = getWorld(location.getWorld());
        Object blockPosition = newBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(itemStack);
        Object chunk = getChunkAt(nmsWorld, location);
        removeIfTileEntity(nmsWorld, blockPosition);
        setType(chunk, blockPosition, blockData, physics);
        updateBlock(nmsWorld, blockPosition, blockData, physics);
    }

    /**
     * As stated in {@link #setChunkBlock(Location, ItemStack, boolean)}:
     * <p>
     * Changes block type using Chunk block setter, which in an NMS code, reads as
     * follows {@code nmsChunk.setType(...)} which surpasses
     * {@code nmsWorld.setTypeAndData(...)}
     * speed due to absence of light updates, the method that
     * {@link #setBlock(Location, ItemStack)} uses. Then,
     * notifies the world of the updated blocks so they can be seen by the players.
     *
     * <p>
     * In addition to that, it makes sure that there is no TPS loss due to the
     * amount of blocks being changed.
     *
     * @param location  location to put the block at
     * @param itemStack itemStack to apply on the block
     * @param physics   whether physics should be applied or not
     */
    public static CompletableFuture<Void> setChunkBlockAsynchronously(Location location, ItemStack itemStack,
                                                                      boolean physics) {
        Object nmsWorld = getWorld(location.getWorld());
        Object blockPosition = newMutableBlockPosition(location.getWorld(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
        Object blockData = getBlockData(itemStack);
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
        WORKLOAD_RUNNABLE.addWorkload(new ChunkSetWorkload(nmsWorld, blockPosition, blockData, location, physics));
        WORKLOAD_RUNNABLE.whenComplete(() -> workloadFinishFuture.complete(null));
        return workloadFinishFuture;
    }

    /**
     * Mass change blocks at the given locations using Chunk block setter which
     * doesn't apply light updates but offers
     * better performance in comparison to setBlocks(...)
     *
     * @param world     world where the blocks are located at
     * @param locations locations to put the block at
     * @param itemStack ItemStack to apply on the created blocks
     * @param physics   whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static void setChunkBlocks(World world, Collection<Location> locations, ItemStack itemStack,
                                      boolean physics) {
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        locations.forEach(location -> {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            Object chunk = getChunkAt(nmsWorld, x, z);
            setBlockPosition(blockPosition, x, y, z);
            removeIfTileEntity(nmsWorld, blockPosition);
            setType(chunk, blockPosition, blockData, physics);
        });
    }

    /**
     * A thread safe version of
     * {@link #setChunkBlocks(World, Collection, ItemStack, boolean)}*
     * <p>
     * * Mass change blocks at the given locations using Chunk block setter which
     * doesn't apply light updates but offers
     * better performance in comparison to setBlocks(...).
     * <p>
     * With an eye on the server TPS, this method won't degrade the server
     * performance regardless of the
     * amount of blocks being changed in contrast to the regular one.
     *
     * @param world     world where the blocks are located at
     * @param locations locations to put the block at
     * @param itemStack ItemStack to apply on the created blocks
     * @param physics   whether physics such as gravity should be applied or not
     * @throws IllegalArgumentException if material is not perceived as a block
     *                                  material
     * @throws NullPointerException     if the specified material has no block data
     *                                  assigned to it
     */
    public static CompletableFuture<Void> setChunkBlocksAsynchronously(World world, Collection<Location> locations,
                                                                       ItemStack itemStack, boolean physics) {
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
        WorkloadRunnable workloadRunnable = new WorkloadRunnable();
        BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
        locations.forEach(location -> workloadRunnable
                .addWorkload(new ChunkSetWorkload(nmsWorld, blockPosition, blockData, location, physics)));
        workloadRunnable.whenComplete(() -> {
            workloadFinishFuture.complete(null);
            workloadTask.cancel();
        });
        return workloadFinishFuture;
    }

    /**
     * Changes block type using the fastest method that can set blocks without the
     * need to restart the server
     * {@code chunkSection.setType(...)}
     *
     * @param location location to put the block at
     * @param material material to apply on the block
     */
    public static void setSectionBlock(Location location, Material material) {
        setSectionBlock(location, material, false);
    }

    /**
     * Changes block type using the fastest method that can set blocks without the
     * need to restart the server
     * {@code chunkSection.setType(...)}
     *
     * @param location location to put the block at
     * @param material material to apply on the block
     * @param physics  whether physics should be applied or not
     */
    public static void setSectionBlock(Location location, Material material, boolean physics) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        World world = location.getWorld();
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(material);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Object nmsChunk = getChunkAt(nmsWorld, location);
        int j = x & 15;
        int k = y & 15;
        int l = z & 15;
        Object[] sections = getSections(nmsChunk);
        Object section = getSection(nmsChunk, sections, y);
        Object blockPosition = newBlockPosition(world, x, y, z);
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeChunkSection(section, j, k, l, blockData);
        updateBlock(nmsWorld, blockPosition, blockData, physics);
    }

    /**
     * Changes block type using the fastest method that can set blocks without the
     * need to restart the server
     * {@code chunkSection.setType(...)}
     *
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the block
     */
    public static void setSectionBlock(Location location, ItemStack itemStack) {
        setSectionBlock(location, itemStack, false);
    }

    /**
     * Changes block type using the fastest method that can set blocks without the
     * need to restart the server
     * {@code chunkSection.setType(...)}
     *
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the block
     * @param physics   whether physics should be applied or not
     */
    public static void setSectionBlock(Location location, ItemStack itemStack, boolean physics) {
        World world = location.getWorld();
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Object nmsChunk = getChunkAt(nmsWorld, location);
        int j = x & 15;
        int k = y & 15;
        int l = z & 15;
        Object[] sections = getSections(nmsChunk);
        Object section = getSection(nmsChunk, sections, y);
        Object blockPosition = newBlockPosition(world, x, y, z);
        removeIfTileEntity(nmsWorld, blockPosition);
        setTypeChunkSection(section, j, k, l, blockData);
        updateBlock(nmsWorld, blockPosition, blockData, physics);
    }

    /**
     * Changes block type using the fastest method that can set blocks without the
     * need to restart the server
     * {@code chunkSection.setType(...)} asynchronously
     *
     * @param location  location to put the block at
     * @param itemStack ItemStack to apply on the block
     * @param physics   whether physics should be applied or not
     */
    public static CompletableFuture<Void> setSectionBlockAsynchronously(Location location, ItemStack itemStack,
                                                                        boolean physics) {
        World world = location.getWorld();
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<Void>();
        WORKLOAD_RUNNABLE.addWorkload(new SectionSetWorkload(nmsWorld, blockPosition, blockData, location, physics));
        WORKLOAD_RUNNABLE.whenComplete(() -> workloadFinishFuture.complete(null));
        return workloadFinishFuture;
    }

    /**
     * Mass changes block types using the fastest method that can set blocks without
     * the need to restart the server
     * {@code chunkSection.setType(...)}
     *
     * @param locations locations to put the blocks at
     * @param material  material to apply on the blocks
     * @param world     world where locations are taken from
     */
    public static void setSectionBlocks(World world, Collection<Location> locations, Material material) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(material);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        locations.forEach(location -> {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            Object nmsChunk = getChunkAt(nmsWorld, location);
            int j = x & 15;
            int k = y & 15;
            int l = z & 15;
            Object[] sections = getSections(nmsChunk);
            Object section = getSection(nmsChunk, sections, y);
            removeIfTileEntity(nmsWorld, blockPosition);
            setTypeChunkSection(section, j, k, l, blockData);
            setBlockPosition(blockPosition, x, y, z);
            updateBlock(nmsWorld, blockPosition, blockData, false);
        });
    }

    /**
     * Mass changes block types using the fastest method that can set blocks without
     * the need to restart the server
     * {@code chunkSection.setType(...)}
     *
     * @param locations locations to put the blocks at
     * @param itemStack ItemStack to apply on the blocks
     * @param world     world where locations are taken from
     */
    public static void setSectionBlocks(World world, Collection<Location> locations, ItemStack itemStack) {
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        locations.forEach(location -> {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            Object nmsChunk = getChunkAt(nmsWorld, location);
            int j = x & 15;
            int k = y & 15;
            int l = z & 15;
            Object[] sections = getSections(nmsChunk);
            Object section = getSection(nmsChunk, sections, y);
            removeIfTileEntity(nmsWorld, blockPosition);
            setTypeChunkSection(section, j, k, l, blockData);
            setBlockPosition(blockPosition, x, y, z);
            updateBlock(nmsWorld, blockPosition, blockData, false);
        });
    }

    /**
     * Mass changes block types using the fastest method that can set blocks without
     * the need to restart the server
     * {@code chunkSection.setType(...)} asynchronously
     *
     * @param locations locations to put the blocks at
     * @param itemStack ItemStack to apply on the blocks
     * @param world     world where locations are taken from
     */
    public static CompletableFuture<Void> setSectionBlocksAsynchronously(World world, Collection<Location> locations,
                                                                         ItemStack itemStack) {
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        Object blockPosition = newMutableBlockPosition(world, 0, 0, 0);
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
        WorkloadRunnable workloadRunnable = new WorkloadRunnable();
        BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
        locations.forEach(location -> workloadRunnable
                .addWorkload(new SectionSetWorkload(nmsWorld, blockPosition, blockData, location, false)));
        workloadRunnable.whenComplete(() -> {
            workloadFinishFuture.complete(null);
            workloadTask.cancel();
        });
        return workloadFinishFuture;
    }

    /**
     * Has the same behavior as {@link #setSectionBlocks(World, Location, Material)}
     * but creates a cuboid from a location
     * to another as if using the vanilla command <b>/fill</b>
     *
     * @param loc1     point 1
     * @param loc2     point 2
     * @param material material to apply on the blocks
     */
    public static void setSectionCuboid(Location loc1, Location loc2, Material material) {
        setSectionCuboid(loc1, loc2, material, false);
    }

    /**
     * Has the same behavior as
     * but creates a cuboid from a location
     * to another as if using the vanilla command <b>/fill</b>
     *
     * @param loc1     point 1
     * @param loc2     point 2
     * @param material material to apply on the blocks
     * @param physics  whether to apply physics or not
     */
    public static void setSectionCuboid(Location loc1, Location loc2, Material material, boolean physics) {
        if (!material.isBlock()) throw new IllegalArgumentException("The specified material is not a placeable block!");
        World world = loc1.getWorld();
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(material);
        if (blockData == null)
            throw new NullPointerException("Unable to retrieve block data for the corresponding material.");
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        int baseX = x1;
        int baseY = y1;
        int baseZ = z1;
        int sizeX = Math.abs(x2 - x1) + 1;
        int sizeY = Math.abs(y2 - y1) + 1;
        int sizeZ = Math.abs(z2 - z1) + 1;
        int x3 = 0, y3 = 0, z3 = 0;
        Location location = new Location(loc1.getWorld(), baseX + x3, baseY + y3, baseZ + z3);
        int cuboidSize = sizeX * sizeY * sizeZ;
        Object blockPosition = newMutableBlockPosition(location);
        for (int i = 0; i < cuboidSize; i++) {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            Object nmsChunk = getChunkAt(nmsWorld, location);
            int j = x & 15;
            int k = y & 15;
            int l = z & 15;
            Object[] sections = getSections(nmsChunk);
            Object section = getSection(nmsChunk, sections, y);
            removeIfTileEntity(nmsWorld, blockPosition);
            setTypeChunkSection(section, j, k, l, blockData);
            setBlockPosition(blockPosition, x, y, z);
            updateBlock(nmsWorld, blockPosition, blockData, physics);
            if (++x3 >= sizeX) {
                x3 = 0;
                if (++y3 >= sizeY) {
                    y3 = 0;
                    ++z3;
                }
            }
            location.setX(baseX + x3);
            location.setY(baseY + y3);
            location.setZ(baseZ + z3);
        }
    }

    /**
     * Has the same behavior as {@link #setSectionBlocks(World, Location, Material)}
     * but creates a cuboid from a location
     * to another as if using the vanilla command <b>/fill</b>
     *
     * @param loc1      point 1
     * @param loc2      point 2
     * @param itemStack ItemStack to apply on the blocks
     */
    public static void setSectionCuboid(Location loc1, Location loc2, ItemStack itemStack) {
        setSectionCuboid(loc1, loc2, itemStack, false);
    }

    /**
     * Has the same behavior as {@link #setSectionBlocks(World, Location, Material)}
     * but creates a cuboid from a location
     * to another as if using the vanilla command <b>/fill</b>
     *
     * @param loc1      point 1
     * @param loc2      point 2
     * @param itemStack ItemStack to apply on the blocks
     */
    public static void setSectionCuboid(Location loc1, Location loc2, ItemStack itemStack, boolean physics) {
        World world = loc1.getWorld();
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        int baseX = x1;
        int baseY = y1;
        int baseZ = z1;
        int sizeX = Math.abs(x2 - x1) + 1;
        int sizeY = Math.abs(y2 - y1) + 1;
        int sizeZ = Math.abs(z2 - z1) + 1;
        int x3 = 0, y3 = 0, z3 = 0;
        Location location = new Location(loc1.getWorld(), baseX + x3, baseY + y3, baseZ + z3);
        int cuboidSize = sizeX * sizeY * sizeZ;
        Object blockPosition = newMutableBlockPosition(location);
        for (int i = 0; i < cuboidSize; i++) {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            Object nmsChunk = getChunkAt(nmsWorld, location);
            int j = x & 15;
            int k = y & 15;
            int l = z & 15;
            Object[] sections = getSections(nmsChunk);
            Object section = getSection(nmsChunk, sections, y);
            removeIfTileEntity(nmsWorld, blockPosition);
            setTypeChunkSection(section, j, k, l, blockData);
            setBlockPosition(blockPosition, x, y, z);
            updateBlock(nmsWorld, blockPosition, blockData, physics);
            if (++x3 >= sizeX) {
                x3 = 0;
                if (++y3 >= sizeY) {
                    y3 = 0;
                    ++z3;
                }
            }
            location.setX(baseX + x3);
            location.setY(baseY + y3);
            location.setZ(baseZ + z3);
        }
    }

    /**
     * Has the same behavior as {@link #setSectionBlocks(World, Location, Material)}
     * but creates a cuboid from a location
     * to another as if using the vanilla command <b>/fill</b> asynchronously
     *
     * @param loc1      point 1
     * @param loc2      point 2
     * @param itemStack ItemStack to apply on the blocks
     */
    public static CompletableFuture<Void> setSectionCuboidAsynchronously(Location loc1, Location loc2,
                                                                         ItemStack itemStack) {
        return setSectionCuboidAsynchronously(loc1, loc2, itemStack, false);
    }

    /**
     * Has the same behavior as {@link #setSectionBlocks(World, Location, Material)}
     * but creates a cuboid from a location
     * to another as if using the vanilla command <b>/fill</b> asynchronously
     *
     * @param loc1      point 1
     * @param loc2      point 2
     * @param itemStack ItemStack to apply on the blocks
     */
    public static CompletableFuture<Void> setSectionCuboidAsynchronously(Location loc1, Location loc2,
                                                                         ItemStack itemStack, boolean physics) {
        World world = loc1.getWorld();
        Object nmsWorld = getWorld(world);
        Object blockData = getBlockData(itemStack);
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        int baseX = x1;
        int baseY = y1;
        int baseZ = z1;
        int sizeX = Math.abs(x2 - x1) + 1;
        int sizeY = Math.abs(y2 - y1) + 1;
        int sizeZ = Math.abs(z2 - z1) + 1;
        int x3 = 0, y3 = 0, z3 = 0;
        Location location = new Location(loc1.getWorld(), baseX + x3, baseY + y3, baseZ + z3);
        int cuboidSize = sizeX * sizeY * sizeZ;
        Object blockPosition = newMutableBlockPosition(location);
        CompletableFuture<Void> workloadFinishFuture = new CompletableFuture<>();
        WorkloadRunnable workloadRunnable = new WorkloadRunnable();
        BukkitTask workloadTask = Bukkit.getScheduler().runTaskTimer(PLUGIN, workloadRunnable, 1, 1);
        for (int i = 0; i < cuboidSize; i++) {
            SectionSetWorkload workload = new SectionSetWorkload(nmsWorld, blockPosition, blockData, location.clone(),
                    physics);
            if (++x3 >= sizeX) {
                x3 = 0;
                if (++y3 >= sizeY) {
                    y3 = 0;
                    ++z3;
                }
            }
            location.setX(baseX + x3);
            location.setY(baseY + y3);
            location.setZ(baseZ + z3);
            workloadRunnable.addWorkload(workload);
        }
        workloadRunnable.whenComplete(() -> {
            workloadFinishFuture.complete(null);
            workloadTask.cancel();
        });
        return workloadFinishFuture;
    }

    private static Object getSection(Object nmsChunk, Object[] sections, int y) {
        return BLOCK_UPDATER.getSection(nmsChunk, sections, y);
    }

    private static Object[] getSections(Object nmsChunk) {
        try {
            return (Object[]) CHUNK_GET_SECTIONS.invoke(nmsChunk);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setTypeChunkSection(Object chunkSection, int x, int y, int z, Object blockData) {
        try {
            CHUNK_SECTION_SET_TYPE.invoke(chunkSection, x, y, z, blockData);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void setTypeAndData(Object nmsWorld, Object blockPosition, Object blockData, int physics) {
        try {
            SET_TYPE_AND_DATA.invoke(nmsWorld, blockPosition, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
        BLOCK_UPDATER.setType(chunk, blockPosition, blockData, physics);
    }

    private static Object getChunkAt(Object world, Location loc) {
        try {
            return WORLD_GET_CHUNK.invoke(world, loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getChunkAt(Object world, int x, int z) {
        try {
            return WORLD_GET_CHUNK.invoke(world, x >> 4, z >> 4);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getNMSWorld(@Nonnull World world) {
        try {
            return WORLD_GET_HANDLE.invoke(world);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static @Nullable Object getNMSBlockData(@Nullable ItemStack itemStack) {
        try {
            if (itemStack == null) return null;
            Object nmsItemStack = NMS_ITEM_STACK_COPY.invoke(itemStack);
            if (nmsItemStack == null) return null;
            Object nmsItem = NMS_ITEM_STACK_TO_ITEM.invoke(nmsItemStack);
            Object block = NMS_BLOCK_FROM_ITEM.invoke(nmsItem);
            if (ReflectionUtils.MINOR_NUMBER < 8) return block;
            return ITEM_TO_BLOCK_DATA.invoke(block);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isTileEntity(Object nmsWorld, Object blockPosition) {
        return TILE_ENTITY_MANAGER.isTileEntity(nmsWorld, blockPosition);
    }

    private static boolean removeIfTileEntity(Object nmsWorld, Object blockPosition) {
        if (!isTileEntity(nmsWorld, blockPosition)) return false;
        TILE_ENTITY_MANAGER.destroyTileEntity(nmsWorld, blockPosition);
        return true;
    }

    public static Object getTileEntity(Block block) {
        try {
            return GET_NMS_TILE_ENTITY.invoke(block.getState());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    // 1.12+ only
    public static Object getSnapshotNBT(Block block) {
        try {
            return GET_SNAPSHOT_NBT.invoke(block.getState());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    // 1.12+ only
    public static String debugSnapshotNBT(Block block) {
        try {
            return GET_SNAPSHOT_NBT.invoke(block.getState()).toString();
        } catch (Throwable e) {
            return "{" + block.getType() + "} is not a tile entity!";
        }
    }

    public static String debugTileEntity(Block block) {
        try {
            return GET_NMS_TILE_ENTITY.invoke(block.getState()).toString() + " (Tile Entity)";
        } catch (Throwable e) {
            return "{" + block.getType() + "} is not a tile entity!";
        }
    }

    // 1.12+ only
    public static Object getSnapshot(Block block) {
        try {
            return GET_SNAPSHOT.invoke(block.getState());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    // 1.12+ only
    public static String debugStoredSnapshot(Block block) {
        try {
            return GET_SNAPSHOT.invoke(block.getState()).toString() + " (Tile Entity)";
        } catch (Throwable e) {
            return "{" + block.getType() + "} is not a tile entity!";
        }
    }

    /**
     * Refreshes a block so it appears to the players
     *
     * @param world         nms world {@link #getWorld(World)}
     * @param blockPosition nms block position
     *                      {@link #newBlockPosition(Object, Object, Object, Object)}
     * @param blockData     nms block data {@link #getBlockData(Material)}
     * @param physics       whether physics should be applied or not
     */
    public static void updateBlock(Object world, Object blockPosition, Object blockData, boolean physics) {
        BLOCK_UPDATER.update(world, blockPosition, blockData, physics ? 3 : 2);
    }

    /**
     *
     * @param world (Bukkit world) can be null for versions 1.8+
     * @param x     point
     * @param y     point
     * @param z     point
     * @return constructs an unmodifiable block position
     */
    public static Object newBlockPosition(@Nullable Object world, Object x, Object y, Object z) {
        try {
            return BLOCK_POSITION_CONSTRUCTOR.newBlockPosition(world, x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param world (Bukkit world) can be null for 1.8+
     * @param x     x pos
     * @param y     y pos
     * @param z     z pos
     * @return constructs a mutable block position that can be modified using
     * {@link #setBlockPosition(Object, Object, Object, Object)}
     */
    public static Object newMutableBlockPosition(@Nullable Object world, Object x, Object y, Object z) {
        try {
            return BLOCK_POSITION_CONSTRUCTOR.newMutableBlockPosition(world, x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param location Location to get coordinates from
     * @return constructs a mutable block position that can be modified using
     * {@link #setBlockPosition(Object, Object, Object, Object)}
     */
    public static Object newMutableBlockPosition(Location location) {
        try {
            return BLOCK_POSITION_CONSTRUCTOR.newMutableBlockPosition(location.getWorld(), location.getBlockX(),
                    location.getBlockY(), location.getBlockZ());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param mutableBlockPosition MutableBlockPosition to modify
     * @param x                    new x pos
     * @param y                    new y pos
     * @param z                    new z pos
     * @return modified MutableBlockPosition (no need to set the variable to the
     * returned MutableBlockPosition)
     */
    public static Object setBlockPosition(Object mutableBlockPosition, Object x, Object y, Object z) {
        try {
            return BLOCK_POSITION_CONSTRUCTOR.set(mutableBlockPosition, x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param itemStack bukkit ItemStack
     * @return nms block data from bukkit item stack
     * @throws IllegalArgumentException if material is not a block
     */
    public static @Nonnull Object getBlockData(@Nonnull ItemStack itemStack) {
        Object blockData = BLOCK_DATA_GETTER.fromItemStack(itemStack);
        if (blockData == null) throw new IllegalArgumentException("Couldn't convert specified itemstack to block data");
        return blockData;
    }

    /**
     *
     * @param material to get block data for
     * @return stored nms block data for the specified material
     */
    public static @Nullable Object getBlockData(@Nullable Material material) {
        return NMS_BLOCK_MATERIALS.get(material);
    }

    /**
     * This method should get block data even if block is not actually placed i.e
     * doesn't have location
     * <p>
     * Doesn't retrieve the tile entity as of now
     * </p>
     *
     * @param block bukkit block to cast to nms block data
     * @return nms block data from bukkit block
     */
    public static @Nonnull Object getBlockData(Block block) {
        Object blockData = BLOCK_DATA_GETTER.fromBlock(block);
        return blockData != null ? blockData : AIR_BLOCK_DATA;
    }

    /**
     *
     * @return nms air block data
     */
    public static Object getAirBlockData() {
        return AIR_BLOCK_DATA;
    }

    /**
     *
     * @param world to get nms world for
     * @return stored nms world for the specified world
     */
    public static Object getWorld(World world) {
        return NMS_WORLDS.get(world);
    }

    /**
     *
     * @param worldName to get nms world for
     * @return stored nms world for the specified world name
     */
    public static Object getWorld(String worldName) {
        return NMS_WORLD_NAMES.get(worldName);
    }

    /**
     * @return all available block materials for the current version separated by
     * commas as follows:
     * <p>
     * <i>dirt, stone, glass, etc...</i>
     */
    public static String getAvailableBlockMaterials() {
        return AVAILABLE_BLOCKS;
    }

    /**
     *
     * @return a list of nms block materials including block materials that cannot
     * be given as
     * items in an inventory,
     * such as lava and water.
     */
    public static Set<String> getAllNMSBlockMaterials() {
        return NMS_BLOCK_NAMES.keySet();
    }

    /**
     * physics: 3 = yes, 2 = no
     *
     * @return methods that accept nms objects
     */
    public static UncheckedSetters getUncheckedSetters() {
        return UNCHECKED_SETTERS;
    }

    /**
     *
     * @apiNote physics: 3 = yes, 2 = no
     *
     */
    public static class UncheckedSetters {

        /**
         *
         * @param nmsWorld      using {@link BlockChanger#getWorld(World)
         *                      getWorld(World)} or {@link BlockChanger#getWorld(String)
         *                      getWorld(String)}
         * @param blockPosition using
         *                      {@link BlockChanger#newMutableBlockPosition(Location)
         *                      newMutableBlockPosition(Location)}
         * @param nmsBlockData  {@link BlockChanger#getBlockData(ItemStack)
         *                      getBlockData(ItemStack)} or
         *                      {@link BlockChanger#getBlockData(Material)
         *                      getBlockData(Material)}
         * @param physics       3 = applies physics, 2 = doesn't
         *                      <p>
         *                      <i>blockPosition</i> can be further modified with new
         *                      coordinates using
         *                      {@link BlockChanger#setBlockPosition(Object, Object, Object, Object)}
         */
        public void setBlock(Object nmsWorld, Object blockPosition, Object nmsBlockData, int physics) {
            setTypeAndData(nmsWorld, blockPosition, nmsBlockData, physics);
        }

        /**
         *
         * @param nmsWorld      using {@link BlockChanger#getWorld(World)
         *                      getWorld(World)} or {@link BlockChanger#getWorld(String)
         *                      getWorld(String)}
         * @param blockPosition using
         *                      {@link BlockChanger#newMutableBlockPosition(Location)
         *                      newMutableBlockPosition(Location)}
         * @param nmsBlockData  {@link BlockChanger#getBlockData(ItemStack)
         *                      getBlockData(ItemStack)} or
         *                      {@link BlockChanger#getBlockData(Material)
         *                      getBlockData(Material)}
         * @param x             x coordinate of the block
         * @param z             z coordinate of the block
         * @param physics       3 = applies physics, 2 = doesn't
         *                      <p>
         *                      <i>blockPosition</i> can be further modified with new
         *                      coordinates using
         *                      {@link BlockChanger#setBlockPosition(Object, Object, Object, Object)}
         */
        public void setChunkBlock(Object nmsWorld, Object blockPosition, Object nmsBlockData, int x, int z,
                                  boolean physics) {
            Object chunk = getChunkAt(nmsWorld, x, z);
            setType(chunk, blockPosition, nmsBlockData, physics);
            updateBlock(nmsWorld, blockPosition, nmsBlockData, physics);
        }

        /**
         *
         * @param nmsWorld      using {@link BlockChanger#getWorld(World)
         *                      getWorld(World)} or {@link BlockChanger#getWorld(String)
         *                      getWorld(String)}
         * @param blockPosition using
         *                      {@link BlockChanger#newMutableBlockPosition(Location)
         *                      newMutableBlockPosition(Location)}
         * @param nmsBlockData  {@link BlockChanger#getBlockData(ItemStack)
         *                      getBlockData(ItemStack)} or
         *                      {@link BlockChanger#getBlockData(Material)
         *                      getBlockData(Material)}
         * @param x             x coordinate of the block
         * @param y             y coordinate of the block
         * @param z             z coordinate of the block
         * @param physics       3 = applies physics, 2 = doesn't
         *                      <p>
         *                      <i>blockPosition</i> can be further modified with new
         *                      coordinates using
         *                      {@link BlockChanger#setBlockPosition(Object, Object, Object, Object)}
         */
        public void setSectionBlock(Object nmsWorld, Object blockPosition, Object nmsBlockData, int x, int y, int z,
                                    boolean physics) {
            Object nmsChunk = getChunkAt(nmsWorld, x, z);
            int j = x & 15;
            int k = y & 15;
            int l = z & 15;
            Object[] sections = getSections(nmsChunk);
            Object section = getSection(nmsChunk, sections, y);
            setTypeChunkSection(section, j, k, l, nmsBlockData);
            updateBlock(nmsWorld, blockPosition, nmsWorld, physics);
        }

    }

    private interface TileEntityManager {

        default Object getCapturedTileEntities(Object nmsWorld) {
            try {
                return WORLD_CAPTURED_TILE_ENTITIES.invoke(nmsWorld);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        default boolean isTileEntity(Object nmsWorld, Object blockPosition) {
            try {
                return (boolean) IS_TILE_ENTITY.invoke(getCapturedTileEntities(nmsWorld), blockPosition);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return false;
        }

        default void destroyTileEntity(Object nmsWorld, Object blockPosition) {
            try {
                WORLD_REMOVE_TILE_ENTITY.invoke(nmsWorld, blockPosition);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        /*
         * Store bukkit block variable {
         * Block block = ...
         * }
         * Get block data (title entity data still exists in old the bukkit block
         * variable) {
         * Object blockData = BlockChanger.getBlockData(block);
         * }
         * Set block using BlockChanger within the method {
         * setType(...)
         * }
         * Check if block is a title entity {
         * isTitleEntity
         * }
         * Get tile entity that was stored in the bukkit block variable {
         * CraftBlockState craftBlockState = (CraftBlockState)block.getState();
         * // getState() creates a new block state with the location of that block
         * TileEntity nmsTileEntity = craftBlockState.getTileEntity();
         * }
         * Set tile entity using BlockChanger {
         * <Use nms method that applies tile entity on the block>
         * }
         */

    }

    private static class TileEntityManagerSupported implements TileEntityManager {
    }

    private static class TileEntityManagerDummy implements TileEntityManager {

        @Override
        public Object getCapturedTileEntities(Object nmsWorld) {
            return null;
        }

        @Override
        public boolean isTileEntity(Object nmsWorld, Object blockPosition) {
            return false;
        }

        @Override
        public void destroyTileEntity(Object nmsWorld, Object blockPosition) {
        }

    }

    private interface BlockDataRetriever {

        default Object getNMSItem(ItemStack itemStack) throws Throwable {
            if (itemStack == null) throw new NullPointerException("ItemStack is null!");
            if (itemStack.getType() == org.bukkit.Material.AIR) return null;
            if (NMS_BLOCK_NAMES.containsKey(itemStack.getType().name())) return null;
            Object nmsItemStack = NMS_ITEM_STACK_COPY.invoke(itemStack);
            if (nmsItemStack == null) return null;
            return NMS_ITEM_STACK_TO_ITEM.invoke(nmsItemStack);
        }

        // 1.7-1.12 requires 2 methods to get block data
        default Object fromBlock(Block block) {
            try {
                Object nmsBlock = CRAFT_BLOCK_GET_NMS_BLOCK.invoke(block);
                return NMS_BLOCK_GET_BLOCK_DATA.invoke(nmsBlock);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        Object fromItemStack(ItemStack itemStack);

    }

    // 1.13+ or 1.8+ without data support
    private static class BlockDataGetter implements BlockDataRetriever {

        @Override
        public Object fromItemStack(ItemStack itemStack) {
            try {
                Object nmsItem = getNMSItem(itemStack);
                Object block = nmsItem != null ? NMS_BLOCK_FROM_ITEM.invoke(nmsItem)
                        : NMS_BLOCK_NAMES.get(itemStack.getType().name());
                return ITEM_TO_BLOCK_DATA.invoke(block);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        // 1.13+ one method to get block data (getNMS())
        @Override
        public Object fromBlock(Block block) {
            try {
                return NMS_BLOCK_GET_BLOCK_DATA.invoke(block);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    // 1.8-1.12
    private static class BlockDataGetterLegacy implements BlockDataRetriever {

        @Override
        public Object fromItemStack(ItemStack itemStack) {
            try {
                Object nmsItem = getNMSItem(itemStack);
                Object possibleBlock = NMS_BLOCK_FROM_NAME.invoke(itemStack.getType().name().toLowerCase());
                if (nmsItem == null && possibleBlock == null) return AIR_BLOCK_DATA;
                Object block = possibleBlock == null ? NMS_BLOCK_FROM_ITEM.invoke(nmsItem) : possibleBlock;
                short data = itemStack.getDurability();
                return data > 0 ? BLOCK_DATA_FROM_LEGACY_DATA.invoke(block, data) : ITEM_TO_BLOCK_DATA.invoke(block);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    // 1.7
    private static class BlockDataGetterAncient implements BlockDataRetriever {

        @Override
        public Object fromItemStack(ItemStack itemStack) {
            try {
                Object nmsItem = getNMSItem(itemStack);
                if (nmsItem == null) return AIR_BLOCK_DATA;
                return NMS_BLOCK_FROM_ITEM.invoke(nmsItem);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    private static interface Workload {

        boolean compute();

    }

    private static class WorkloadRunnable implements Runnable {

        private static final double MAX_MILLIS_PER_TICK = 10.0;
        private static final int MAX_NANOS_PER_TICK = (int) (MAX_MILLIS_PER_TICK * 1E6);

        private final Deque<Workload> workloadDeque = new ArrayDeque<>();

        public void addWorkload(Workload workload) {
            this.workloadDeque.add(workload);
        }

        public void whenComplete(Runnable runnable) {
            WhenCompleteWorkload workload = new WhenCompleteWorkload(runnable);
            this.workloadDeque.add(workload);
        }

        @Override
        public void run() {
            long stopTime = System.nanoTime() + MAX_NANOS_PER_TICK;

            Workload nextLoad;

            while (System.nanoTime() <= stopTime && (nextLoad = this.workloadDeque.poll()) != null) {
                nextLoad.compute();
            }
        }

    }

    private static class BlockSetWorkload implements Workload {

        private Object nmsWorld;
        private Object blockPosition;
        private Object blockData;
        private Location location;
        private int physics;

        public BlockSetWorkload(Object nmsWorld, Object blockPosition, Object blockData, Location location,
                                boolean physics) {
            this.nmsWorld = nmsWorld;
            this.blockPosition = blockPosition;
            this.blockData = blockData;
            this.location = location;
            this.physics = physics ? 3 : 2;
        }

        @Override
        public boolean compute() {
            BlockChanger.setBlockPosition(blockPosition, location.getBlockX(), location.getBlockY(),
                    location.getBlockZ());
            BlockChanger.removeIfTileEntity(nmsWorld, blockPosition);
            BlockChanger.setTypeAndData(nmsWorld, blockPosition, blockData, physics);
            return true;
        }

    }

    private static class ChunkSetWorkload implements Workload {

        private Object nmsWorld;
        private Object blockPosition;
        private Object blockData;
        private Location location;
        private boolean physics;

        public ChunkSetWorkload(Object nmsWorld, Object blockPosition, Object blockData, Location location,
                                boolean physics) {
            this.nmsWorld = nmsWorld;
            this.blockPosition = blockPosition;
            this.blockData = blockData;
            this.location = location;
            this.physics = physics;
        }

        @Override
        public boolean compute() {
            BlockChanger.setBlockPosition(blockPosition, location.getBlockX(), location.getBlockY(),
                    location.getBlockZ());
            Object chunk = BlockChanger.getChunkAt(nmsWorld, location.getBlockX(), location.getBlockZ());
            BlockChanger.removeIfTileEntity(nmsWorld, blockPosition);
            BlockChanger.setType(chunk, blockPosition, blockData, physics);
            BlockChanger.updateBlock(nmsWorld, blockPosition, blockData, physics);
            return true;
        }

    }

    private static class SectionSetWorkload implements Workload {

        private Object nmsWorld;
        private Object blockPosition;
        private Object blockData;
        private Location location;
        private boolean physics;

        public SectionSetWorkload(Object nmsWorld, Object blockPosition, Object blockData, Location location,
                                  boolean physics) {
            this.nmsWorld = nmsWorld;
            this.blockPosition = blockPosition;
            this.blockData = blockData;
            this.location = location;
            this.physics = physics;
        }

        @Override
        public boolean compute() {
            BlockChanger.setBlockPosition(blockPosition, location.getBlockX(), location.getBlockY(),
                    location.getBlockZ());
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            Object nmsChunk = BlockChanger.getChunkAt(nmsWorld, x, z);
            int j = x & 15;
            int k = y & 15;
            int l = z & 15;
            Object[] sections = BlockChanger.getSections(nmsChunk);
            Object section = BlockChanger.getSection(nmsChunk, sections, y);
            BlockChanger.removeIfTileEntity(nmsWorld, blockPosition);
            BlockChanger.setTypeChunkSection(section, j, k, l, blockData);
            BlockChanger.updateBlock(nmsWorld, blockPosition, blockData, physics);
            return true;
        }

    }

    private static class WhenCompleteWorkload implements Workload {

        private Runnable runnable;

        public WhenCompleteWorkload(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public boolean compute() {
            runnable.run();
            return false;
        }

    }

}

interface BlockPositionConstructor {

    Object newBlockPosition(Object world, Object x, Object y, Object z);

    Object newMutableBlockPosition(Object world, Object x, Object y, Object z);

    Object set(Object mutableBlockPosition, Object x, Object y, Object z);

}

interface BlockUpdater {

    void setType(Object chunk, Object blockPosition, Object blockData, boolean physics);

    void update(Object world, Object blockPosition, Object blockData, int physics);

    Object getSection(Object nmsChunk, Object[] sections, int y);

    int getSectionIndex(Object nmsChunk, int y);

}

class BlockPositionNormal implements BlockPositionConstructor {

    private MethodHandle blockPositionConstructor;
    private MethodHandle mutableBlockPositionConstructor;
    private MethodHandle mutableBlockPositionSet;

    public BlockPositionNormal(MethodHandle blockPositionXYZ, MethodHandle mutableBlockPositionXYZ,
                               MethodHandle mutableBlockPositionSet) {
        this.blockPositionConstructor = blockPositionXYZ;
        this.mutableBlockPositionConstructor = mutableBlockPositionXYZ;
        this.mutableBlockPositionSet = mutableBlockPositionSet;
    }

    @Override
    public Object newBlockPosition(Object world, Object x, Object y, Object z) {
        try {
            return blockPositionConstructor.invoke(x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object newMutableBlockPosition(Object world, Object x, Object y, Object z) {
        try {
            return mutableBlockPositionConstructor.invoke(x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object set(Object mutableBlockPosition, Object x, Object y, Object z) {
        try {
            return mutableBlockPositionSet.invoke(mutableBlockPosition, x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

}

class BlockPositionAncient implements BlockPositionConstructor {

    private MethodHandle blockPositionConstructor;
    private MethodHandle mutableBlockPositionConstructor;

    public BlockPositionAncient(MethodHandle blockPositionXYZ, MethodHandle mutableBlockPositionXYZ) {
        this.blockPositionConstructor = blockPositionXYZ;
        this.mutableBlockPositionConstructor = mutableBlockPositionXYZ;
    }

    @Override
    public Object newBlockPosition(Object world, Object x, Object y, Object z) {
        try {
            return blockPositionConstructor.invoke(world, x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object newMutableBlockPosition(Object world, Object x, Object y, Object z) {
        try {
            return mutableBlockPositionConstructor.invoke(x, y, z);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object set(Object mutableBlockPosition, Object x, Object y, Object z) {
        try {
            Location loc = (Location) mutableBlockPosition;
            loc.setX((double) x);
            loc.setY((double) y);
            loc.setZ((double) z);
            return loc;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

}

class BlockUpdaterAncient implements BlockUpdater {

    private MethodHandle blockNotify;
    private MethodHandle chunkSetType;
    private MethodHandle chunkSection;
    private MethodHandle setSectionElement;

    public BlockUpdaterAncient(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle chunkSection,
                               MethodHandle setSectionElement) {
        this.blockNotify = blockNotify;
        this.chunkSetType = chunkSetType;
        this.chunkSection = chunkSection;
        this.setSectionElement = setSectionElement;
    }

    @Override
    public void update(Object world, Object blockPosition, Object blockData, int physics) {
        try {
            Location loc = (Location) blockPosition;
            blockNotify.invoke(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
        try {
            chunkSetType.invoke(chunk, blockPosition, blockData);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getSection(Object nmsChunk, Object[] sections, int y) {
        Object section = sections[getSectionIndex(null, y)];
        if (section == null) {
            try {
                section = chunkSection.invoke(y >> 4 << 4, true);
                setSectionElement.invoke(sections, y >> 4, section);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return section;
    }

    @Override
    public int getSectionIndex(Object nmsChunk, int y) {
        int i = y >> 4;
        return i <= 15 ? i : 15;
    }

}

class BlockUpdaterLegacy implements BlockUpdater {

    private MethodHandle blockNotify;
    private MethodHandle chunkSetType;
    private MethodHandle chunkSection;
    private MethodHandle setSectionElement;

    public BlockUpdaterLegacy(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle chunkSection,
                              MethodHandle setSectionElement) {
        this.blockNotify = blockNotify;
        this.chunkSetType = chunkSetType;
        this.chunkSection = chunkSection;
        this.setSectionElement = setSectionElement;
    }

    @Override
    public void update(Object world, Object blockPosition, Object blockData, int physics) {
        try {
            blockNotify.invoke(world, blockPosition);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
        try {
            chunkSetType.invoke(chunk, blockPosition, blockData);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getSection(Object nmsChunk, Object[] sections, int y) {
        Object section = sections[getSectionIndex(null, y)];
        if (section == null) {
            try {
                section = chunkSection.invoke(y >> 4 << 4, true);
                setSectionElement.invoke(sections, y >> 4, section);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return section;
    }

    @Override
    public int getSectionIndex(Object nmsChunk, int y) {
        int i = y >> 4;
        return i <= 15 ? i : 15;
    }

}

class BlockUpdater9 implements BlockUpdater {

    private MethodHandle blockNotify;
    private MethodHandle chunkSetType;
    private MethodHandle chunkSection;
    private MethodHandle setSectionElement;

    public BlockUpdater9(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle chunkSection,
                         MethodHandle setSectionElement) {
        this.blockNotify = blockNotify;
        this.chunkSetType = chunkSetType;
        this.chunkSection = chunkSection;
        this.setSectionElement = setSectionElement;
    }

    @Override
    public void update(Object world, Object blockPosition, Object blockData, int physics) {
        try {
            blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
        try {
            chunkSetType.invoke(chunk, blockPosition, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getSection(Object nmsChunk, Object[] sections, int y) {
        Object section = sections[getSectionIndex(null, y)];
        if (section == null) {
            try {
                section = chunkSection.invoke(y >> 4 << 4, true);
                setSectionElement.invoke(sections, y >> 4, section);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return section;
    }

    @Override
    public int getSectionIndex(Object nmsChunk, int y) {
        int i = y >> 4;
        return i <= 15 ? i : 15;
    }

}

class BlockUpdater13 implements BlockUpdater {

    private MethodHandle blockNotify;
    private MethodHandle chunkSetType;
    private MethodHandle chunkSection;
    private MethodHandle setSectionElement;

    public BlockUpdater13(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle chunkSection,
                          MethodHandle setSectionElement) {
        this.blockNotify = blockNotify;
        this.chunkSetType = chunkSetType;
        this.chunkSection = chunkSection;
        this.setSectionElement = setSectionElement;
    }

    @Override
    public void update(Object world, Object blockPosition, Object blockData, int physics) {
        try {
            blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
        try {
            chunkSetType.invoke(chunk, blockPosition, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getSection(Object nmsChunk, Object[] sections, int y) {
        Object section = sections[getSectionIndex(null, y)];
        if (section == null) {
            try {
                section = chunkSection.invoke(y >> 4 << 4);
                setSectionElement.invoke(sections, y >> 4, section);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return section;
    }

    @Override
    public int getSectionIndex(Object nmsChunk, int y) {
        int i = y >> 4;
        return i <= 15 ? i : 15;
    }

}

class BlockUpdater17 implements BlockUpdater {

    private MethodHandle blockNotify;
    private MethodHandle chunkSetType;
    private MethodHandle sectionIndexGetter;
    private MethodHandle chunkSection;
    private MethodHandle setSectionElement;

    public BlockUpdater17(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle sectionIndexGetter,
                          MethodHandle chunkSection, MethodHandle setSectionElement) {
        this.blockNotify = blockNotify;
        this.chunkSetType = chunkSetType;
        this.sectionIndexGetter = sectionIndexGetter;
        this.chunkSection = chunkSection;
        this.setSectionElement = setSectionElement;
    }

    @Override
    public void update(Object world, Object blockPosition, Object blockData, int physics) {
        try {
            blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
        try {
            chunkSetType.invoke(chunk, blockPosition, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getSection(Object nmsChunk, Object[] sections, int y) {
        Object section = sections[getSectionIndex(nmsChunk, y)];
        if (section == null) {
            try {
                section = chunkSection.invoke(y >> 4 << 4);
                setSectionElement.invoke(sections, y >> 4, section);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return section;
    }

    @Override
    public int getSectionIndex(Object nmsChunk, int y) {
        int sectionIndex = -1;
        try {
            sectionIndex = (int) sectionIndexGetter.invoke(nmsChunk, y);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return sectionIndex <= 15 ? sectionIndex : 15;
    }

}

class BlockUpdaterLatest implements BlockUpdater {

    private MethodHandle blockNotify;
    private MethodHandle chunkSetType;
    private MethodHandle sectionIndexGetter;
    private MethodHandle levelHeightAccessorGetter;

    public BlockUpdaterLatest(MethodHandle blockNotify, MethodHandle chunkSetType, MethodHandle sectionIndexGetter,
                              MethodHandle levelHeightAccessorGetter) {
        this.blockNotify = blockNotify;
        this.chunkSetType = chunkSetType;
        this.sectionIndexGetter = sectionIndexGetter;
        this.levelHeightAccessorGetter = levelHeightAccessorGetter;
    }

    @Override
    public void update(Object world, Object blockPosition, Object blockData, int physics) {
        try {
            blockNotify.invoke(world, blockPosition, blockData, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setType(Object chunk, Object blockPosition, Object blockData, boolean physics) {
        try {
            chunkSetType.invoke(chunk, blockPosition, blockData, physics);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getSection(Object nmsChunk, Object[] sections, int y) {
        int idx = getSectionIndex(nmsChunk, y);

        // Clamp to keep index in bounds (prevents -1 and overflow)
        if (idx < 0 || idx >= sections.length) {
            idx = Math.max(0, Math.min(sections.length - 1, idx));
        }

        // On modern MC, sections are usually pre-initialized. If it's null, we let the
        // actual setType path (LevelChunk#setBlockState) handle section creation.
        return sections[idx];
    }

    public Object getLevelHeightAccessor(Object nmsChunk) {
        try {
            return levelHeightAccessorGetter.invoke(nmsChunk);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getSectionIndex(Object nmsChunk, int y) {
        Object levelHeightAccessor = getLevelHeightAccessor(nmsChunk);
        try {
            return (int) sectionIndexGetter.invoke(levelHeightAccessor, y);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

}

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Crypto Morin
 *
 * <b>ReflectionUtils</b> - Reflection handler for NMS and CraftBukkit.
 * (Relocation-safe for Paper 1.20.5+ and Mojang-mapped 1.21.x)
 * *
 * @author Crypto Morin
 * @version 7.1.0.0.2-reloc
 */

final class ReflectionUtils {

    // ===== Relocation-safe CraftBukkit package detection =====
    public static final String CRAFTBUKKIT_BASE = "org.bukkit.craftbukkit";
    public static final String CRAFTBUKKIT_PACKAGE; // org.bukkit.craftbukkit.v1_20_R3 or org.bukkit.craftbukkit.
    public static final boolean HAS_CB_RELOCATION;

    /**
     * The original “vX_Y_RZ” token if relocation exists; otherwise a synthesized token,
     * only used for logs and legacy code paths that want something for display.
     */
    public static final String NMS_VERSION;

    /**
     * Minor version (e.g., 21 for 1.21.4).
     */
    public static final int MINOR_NUMBER;

    /**
     * Patch version (e.g., 4 for 1.21.4).
     */
    public static final int PATCH_NUMBER;

    static {
        String serverPkg = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
        boolean relocated = serverPkg.matches("org\\.bukkit\\.craftbukkit\\.v\\d+_\\d+_R\\d+");
        HAS_CB_RELOCATION = relocated;
        CRAFTBUKKIT_PACKAGE = relocated ? (serverPkg + ".") : (CRAFTBUKKIT_BASE + ".");

        // Get Minecraft version from Bukkit, e.g. "1.21.4"
        String mc = org.bukkit.Bukkit.getMinecraftVersion();
        String[] parts = mc.split("\\.");
        int major = 1, minor = 0, patch = 0;
        try {
            if (parts.length > 0) major = Integer.parseInt(parts[0]);
            if (parts.length > 1) minor = Integer.parseInt(parts[1]);
            if (parts.length > 2) patch = Integer.parseInt(parts[2].replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
        }

        MINOR_NUMBER = minor;
        PATCH_NUMBER = patch;

        // Keep a token for logging/compatibility: real from CB when present; else synthesize “v<major>_<minor>_R0”
        if (relocated) {
            NMS_VERSION = serverPkg.substring(serverPkg.lastIndexOf('.') + 1); // e.g. v1_20_R3
        } else {
            NMS_VERSION = "v" + major + "_" + minor + "_R0";
        }
    }

    /**
     * NMS base package: Mojang-mapped since 1.17 (“net.minecraft.”),
     * or legacy obf (“net.minecraft.server.vX_Y_RZ”) for < 1.17.
     */
    public static final String NMS_PACKAGE =
            v(17, "net.minecraft.").orElse("net.minecraft.server." + NMS_VERSION + '.');

    /**
     * A nullable public accessible field only available in EntityPlayer.
     */
    private static final MethodHandle PLAYER_CONNECTION;

    /**
     * CraftPlayer#getHandle() -> EntityPlayer
     */
    private static final MethodHandle GET_HANDLE;

    /**
     * ServerCommonPacketListenerImpl/PlayerConnection#sendPacket(Packet)
     */
    private static final MethodHandle SEND_PACKET;

    static {
        Class<?> entityPlayer = getNMSClass("server.level", "EntityPlayer");
        Class<?> craftPlayer = getCraftClass("entity.CraftPlayer");
        Class<?> playerConnection = getNMSClass("server.network", "PlayerConnection");
        Class<?> playerCommonConnection;
        if (supports(20) && supportsPatch(2)) {
            playerCommonConnection = getNMSClass("server.network", "ServerCommonPacketListenerImpl");
        } else {
            playerCommonConnection = playerConnection;
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle sendPacket = null, getHandle = null, connection = null;

        try {
            if (supports(21)) {
                Field connField = entityPlayer.getDeclaredField("connection");
                connField.setAccessible(true);
                connection = lookup.unreflectGetter(connField);
            } else {
                connection = lookup.findGetter(
                        entityPlayer,
                        v(20, "c").v(17, "b").orElse("playerConnection"),
                        playerConnection
                );
            }
            getHandle = lookup.findVirtual(craftPlayer, "getHandle", MethodType.methodType(entityPlayer));

            sendPacket = lookup.findVirtual(
                    (supports(20) && supportsPatch(2)) ? getNMSClass("server.network", "ServerCommonPacketListenerImpl")
                            : playerConnection,
                    v(20, 2, "b").v(18, "a").orElse("sendPacket"),
                    MethodType.methodType(void.class, getNMSClass("network.protocol", "Packet"))
            );
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            try {
                String fallback = supports(17) ? "b" : "playerConnection";
                java.lang.reflect.Field f = entityPlayer.getDeclaredField(fallback);
                f.setAccessible(true);
                connection = lookup.unreflectGetter(f);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        PLAYER_CONNECTION = connection;
        SEND_PACKET = sendPacket;
        GET_HANDLE = getHandle;
    }

    private ReflectionUtils() {
    }

    // ---------------- Version helpers ----------------

    public static <T> VersionHandler<T> v(int version, T handle) {
        return new VersionHandler<>(version, handle);
    }

    public static <T> VersionHandler<T> v(int version, int patch, T handle) {
        return new VersionHandler<>(version, patch, handle);
    }

    public static <T> CallableVersionHandler<T> v(int version, Callable<T> handle) {
        return new CallableVersionHandler<>(version, handle);
    }

    /**
     * Is server minor >= given minor?
     */
    public static boolean supports(int minorNumber) {
        return MINOR_NUMBER >= minorNumber;
    }

    /**
     * Is server equal/newer than given minor.patch?
     */
    public static boolean supports(int minorNumber, int patchNumber) {
        return MINOR_NUMBER == minorNumber ? supportsPatch(patchNumber) : supports(minorNumber);
    }

    /**
     * Is server patch >= given patch (same minor)?
     */
    public static boolean supportsPatch(int patchNumber) {
        return PATCH_NUMBER >= patchNumber;
    }

    // ---------------- Class loaders ----------------

    /**
     * Get a NMS (net.minecraft) class (supports 1.17+ sub-packages).
     *
     * @param packageName 1.17+ sub-package (e.g., "server.level") or null.
     */
    @Nullable
    public static Class<?> getNMSClass(@Nullable String packageName, @Nonnull String name) {
        if (packageName != null && supports(17)) name = packageName + '.' + name;
        try {
            return Class.forName(NMS_PACKAGE + name);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get a NMS class using the base package.
     */
    @Nullable
    public static Class<?> getNMSClass(@Nonnull String name) {
        return getNMSClass(null, name);
    }

    /**
     * Get a CraftBukkit class. Works with and without relocation.
     * e.g. "entity.CraftPlayer" or "block.data.CraftBlockData"
     */
    @Nullable
    public static Class<?> getCraftClass(@Nonnull String name) {
        try {
            return Class.forName(CRAFTBUKKIT_PACKAGE + name);
        } catch (ClassNotFoundException ex) {
            // Fallback: try without relocation in case of shading or unusual environments
            try {
                return Class.forName(CRAFTBUKKIT_BASE + "." + name);
            } catch (ClassNotFoundException ignored) {
            }
            throw new RuntimeException(ex);
        }
    }

    // ---------------- Packet send helpers ----------------

    @Nonnull
    public static CompletableFuture<Void> sendPacket(@Nonnull Player player, @Nonnull Object... packets) {
        return CompletableFuture.runAsync(() -> sendPacketSync(player, packets)).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    public static void sendPacketSync(@Nonnull Player player, @Nonnull Object... packets) {
        try {
            Object handle = GET_HANDLE.invoke(player);
            Object connection = PLAYER_CONNECTION.invoke(handle);
            if (connection != null) {
                for (Object packet : packets) SEND_PACKET.invoke(connection, packet);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Nullable
    public static Object getHandle(@Nonnull Player player) {
        Objects.requireNonNull(player, "Cannot get handle of null player");
        try {
            return GET_HANDLE.invoke(player);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static Object getConnection(@Nonnull Player player) {
        Objects.requireNonNull(player, "Cannot get connection of null player");
        try {
            Object handle = GET_HANDLE.invoke(player);
            return PLAYER_CONNECTION.invoke(handle);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    // ---------------- Misc helpers ----------------

    public static String getVersionInformation() {
        return "(NMS: " + NMS_VERSION + " | " + "Minecraft: " + org.bukkit.Bukkit.getVersion() + " | " +
                "Bukkit: " + org.bukkit.Bukkit.getBukkitVersion() + ')';
    }

    public static Integer getLatestPatchNumberOf(int minorVersion) {
        if (minorVersion <= 0) throw new IllegalArgumentException("Minor version must be positive: " + minorVersion);
        int[] patches = {1, 5, 2, 7, 2, 4, 10, 8, 4, 2, 2, 2, 2, 4, 2, 5, 1, 2, 4, 4};
        if (minorVersion > patches.length) return null;
        return patches[minorVersion - 1];
    }

    @Deprecated
    public static Class<?> getArrayClass(String clazz, boolean nms) {
        clazz = "[L" + (nms ? NMS_PACKAGE : CRAFTBUKKIT_PACKAGE) + clazz + ';';
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Class<?> toArrayClass(Class<?> clazz) {
        try {
            return Class.forName("[L" + clazz.getName() + ';');
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // ---------------- VersionHandler classes ----------------

    public static final class VersionHandler<T> {
        private int version, patch;
        private T handle;

        private VersionHandler(int version, T handle) {
            this(version, 0, handle);
        }

        private VersionHandler(int version, int patch, T handle) {
            if (supports(version) && supportsPatch(patch)) {
                this.version = version;
                this.patch = patch;
                this.handle = handle;
            }
        }

        public VersionHandler<T> v(int version, T handle) {
            return v(version, 0, handle);
        }

        public VersionHandler<T> v(int version, int patch, T handle) {
            if (version == this.version && patch == this.patch)
                throw new IllegalArgumentException("Duplicate version handle for " + version + '.' + patch);
            if (version > this.version && supports(version) && patch >= this.patch && supportsPatch(patch)) {
                this.version = version;
                this.patch = patch;
                this.handle = handle;
            }
            return this;
        }

        public T orElse(T handle) {
            return this.version == 0 ? handle : this.handle;
        }
    }

    public static final class CallableVersionHandler<T> {
        private int version;
        private Callable<T> handle;

        private CallableVersionHandler(int version, Callable<T> handle) {
            if (supports(version)) {
                this.version = version;
                this.handle = handle;
            }
        }

        public CallableVersionHandler<T> v(int version, Callable<T> handle) {
            if (version == this.version) throw new IllegalArgumentException("Duplicate version handle for " + version);
            if (version > this.version && supports(version)) {
                this.version = version;
                this.handle = handle;
            }
            return this;
        }

        public T orElse(Callable<T> handle) {
            try {
                return (this.version == 0 ? handle : this.handle).call();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}
