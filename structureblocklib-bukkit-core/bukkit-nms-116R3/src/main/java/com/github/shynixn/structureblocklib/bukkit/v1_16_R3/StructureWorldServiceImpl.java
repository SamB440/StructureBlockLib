package com.github.shynixn.structureblocklib.bukkit.v1_16_R3;

import com.github.shynixn.structureblocklib.api.entity.StructureEntity;
import com.github.shynixn.structureblocklib.api.entity.StructurePlaceMeta;
import com.github.shynixn.structureblocklib.api.entity.StructurePlacePart;
import com.github.shynixn.structureblocklib.api.entity.StructureReadMeta;
import com.github.shynixn.structureblocklib.api.service.StructureWorldService;
import com.github.shynixn.structureblocklib.api.service.TypeConversionService;
import com.github.shynixn.structureblocklib.core.entity.GenericWrapper;
import org.bukkit.Bukkit;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

/**
 * Implementation to interact with structures in the world.
 */
public class StructureWorldServiceImpl implements StructureWorldService {
    private final TypeConversionService conversionService;

    /**
     * Creates a new service with dependencies.
     *
     * @param conversionService dependency.
     */
    public StructureWorldServiceImpl(TypeConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Places the blocks in the world defined by the given structure.
     *
     * @param meta      Meta data to describe the placement.
     * @param structure NMS structure.
     */
    @Override
    public void placeStructureToWorld(StructurePlaceMeta meta, Object structure) throws Exception {
        if (!(structure instanceof DefinedStructure)) {
            throw new IllegalArgumentException("DefinedStructure has to be an NMS handle!");
        }

        DefinedStructure definedStructure = (DefinedStructure) structure;
        org.bukkit.World bukkitWorld = Bukkit.getWorld(meta.getLocation().getWorldName());
        World world = ((CraftWorld) bukkitWorld).getHandle();
        BlockPosition cornerBlock = new BlockPosition((int) meta.getLocation().getX(), (int) meta.getLocation().getY(), (int) meta.getLocation().getZ());
        DefinedStructureInfo info = new DefinedStructureInfo();
        info.a(!meta.isIncludeEntitiesEnabled());
        info.a((EnumBlockMirror) conversionService.convertToMirrorHandle(meta.getMirrorType()));
        info.a((EnumBlockRotation) conversionService.convertToRotationHandle(meta.getRotationType()));

        if (meta.getIntegrity() < 1.0F) {
            info.b();
            float rotation = MathHelper.a(meta.getIntegrity(), 0.0F, 1.0F);
            DefinedStructureProcessorRotation rotationProcessor = new DefinedStructureProcessorRotation(rotation);
            Random random = new Random();

            if (meta.getSeed() != 0L) {
                random = new Random(meta.getSeed());
            }

            info.a(rotationProcessor);
            info.a(random);
        }

        executeProcessors(bukkitWorld, meta, info);
        executeEntityProcessor(meta, bukkitWorld, definedStructure);
        definedStructure.a((WorldAccess) world, cornerBlock, info, new Random());
    }

    /**
     * Reads the blocks in the world into an NMS Structure definition.
     *
     * @param meta Meta data to describe the block selection.
     * @return A new NMS Structure definition.
     */
    @Override
    public Object readStructureFromWorld(StructureReadMeta meta) throws Exception {
        World world = ((CraftWorld) Bukkit.getWorld(meta.getLocation().getWorldName())).getHandle();
        BlockPosition cornerBlock = new BlockPosition((int) meta.getLocation().getX(), (int) meta.getLocation().getY(), (int) meta.getLocation().getZ());
        BlockPosition offsetBlock = new BlockPosition((int) meta.getOffset().getX(), (int) meta.getOffset().getY(), (int) meta.getOffset().getZ());
        Block structureVoid = (Block) Blocks.class.getDeclaredField(meta.getStructureVoidTypeName()).get(null);

        DefinedStructure definedStructure = new DefinedStructure();
        definedStructure.a(world, cornerBlock, offsetBlock, meta.isIncludeEntitiesEnabled(), structureVoid);
        definedStructure.a(meta.getAuthor());
        return definedStructure;
    }

    /**
     * Executes attached processors.
     *
     * @param bukkitWorld World.
     * @param meta        Meta.
     * @param info        Info.
     */
    private void executeProcessors(org.bukkit.World bukkitWorld, StructurePlaceMeta meta, DefinedStructureInfo info) {
        info.a(new DefinedStructureProcessor() {
            @Nullable
            @Override
            public DefinedStructure.BlockInfo a(IWorldReader iWorldReader, BlockPosition blockPosition, BlockPosition blockPosition1, DefinedStructure.BlockInfo blockInfo, DefinedStructure.BlockInfo blockInfo1, DefinedStructureInfo definedStructureInfo) {
                // Source and target contain the same block state.
                GenericWrapper<IBlockData> targetBlockState = new GenericWrapper<>(blockInfo.b);
                CraftBlock sourceCraftBlock = new CraftBlock(null, blockInfo.a) {
                    @Override
                    public IBlockData getNMS() {
                        return targetBlockState.item;
                    }

                    @Override
                    public void setBlockData(BlockData data, boolean applyPhysics) {
                        targetBlockState.item = ((CraftBlockData) data).getState();
                    }
                };

                org.bukkit.block.Block targetBlock = bukkitWorld.getBlockAt(new Location(bukkitWorld, blockPosition1.getX(), blockPosition1.getY(), blockPosition1.getZ()));
                StructurePlacePart<org.bukkit.block.Block, org.bukkit.World> structurePlacePart = new StructurePlacePart<org.bukkit.block.Block, org.bukkit.World>() {
                    @NotNull
                    @Override
                    public org.bukkit.block.Block getSourceBlock() {
                        return sourceCraftBlock;
                    }

                    @NotNull
                    @Override
                    public org.bukkit.block.Block getTargetBlock() {
                        return targetBlock;
                    }

                    @Override
                    public @NotNull org.bukkit.World getWorld() {
                        return bukkitWorld;
                    }
                };

                for (Function<?, Boolean> processor : meta.getProcessors()) {
                    Function<Object, Boolean> processHandle = (Function<Object, Boolean>) processor;
                    boolean result = processHandle.apply(structurePlacePart);

                    if (!result) {
                        return null;
                    }
                }

                if (!meta.isIncludeBlockEnabled()) {
                    return null;
                }

                return new DefinedStructure.BlockInfo(blockInfo1.a, targetBlockState.item, blockInfo1.c);
            }

            @Override
            protected DefinedStructureStructureProcessorType<?> a() {
                return () -> null;
            }
        });
    }

    /**
     * Executes the entity processors.
     */
    private void executeEntityProcessor(StructurePlaceMeta meta, org.bukkit.World bukkitWorld, DefinedStructure definedStructure) {
        List<DefinedStructure.EntityInfo> structureEntityInfos;
        try {
            Field field = DefinedStructure.class.getDeclaredField("b");
            field.setAccessible(true);
            structureEntityInfos = (List<DefinedStructure.EntityInfo>) field.get(definedStructure);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        for (DefinedStructure.EntityInfo entityInfo : structureEntityInfos) {
            final GenericWrapper<org.bukkit.entity.Entity> peekEntity = new GenericWrapper<>(null);
            StructureEntity<org.bukkit.entity.Entity, Location> structureEntity = new StructureEntity<org.bukkit.entity.Entity, Location>() {
                @Override
                public Optional<org.bukkit.entity.Entity> spawnEntity(Location location) {
                    Optional<net.minecraft.server.v1_16_R3.Entity> optEntity = EntityTypes.a(entityInfo.c, ((CraftWorld) location.getWorld()).getHandle());
                    if (optEntity.isPresent()) {
                        optEntity.get().a_(UUID.randomUUID());
                        ((CraftWorld) location.getWorld()).addEntity(optEntity.get(), CreatureSpawnEvent.SpawnReason.CUSTOM);
                        optEntity.get().getBukkitEntity().teleport(location);
                        return Optional.of(optEntity.get().getBukkitEntity());
                    }
                    return Optional.empty();
                }

                @Override
                public Optional<Entity> getEntity() {
                    if (peekEntity.item == null) {
                        Optional<net.minecraft.server.v1_16_R3.Entity> optEntity = EntityTypes.a(entityInfo.c, ((CraftWorld) bukkitWorld).getHandle());
                        if (optEntity.isPresent()) {
                            peekEntity.item = optEntity.get().getBukkitEntity();
                        }
                    }

                    return Optional.ofNullable(peekEntity.item);
                }

                @Override
                public Location getSourceLocation() {
                    BlockPosition sourcePos = entityInfo.b;
                    return new Location(null, sourcePos.getX(), sourcePos.getY(), sourcePos.getZ());
                }

                @Override
                public String getNbtData() {
                    return entityInfo.c.toString();
                }
            };

            for (Function<?, Boolean> processor : meta.getEntityProcessors()) {
                Function<Object, Boolean> processHandle = (Function<Object, Boolean>) processor;
                boolean result = processHandle.apply(structureEntity);

                if (!result) {
                    structureEntityInfos.remove(entityInfo);
                }
            }

            if (peekEntity.item != null) {
                peekEntity.item.remove();
            }
        }
    }
}
