package com.anthonyhilyard.iceberg.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.common.LevelEvents;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jspecify.annotations.Nullable;

public class EntityCollector extends Level
{
	private final Level wrappedLevel;
	private final List<Entity> collectedEntities = Lists.newArrayList();
	private BlockState blockState = Blocks.AIR.defaultBlockState();

	private static final Map<Level, EntityCollector> wrappedLevelsMap = Maps.newHashMap();
	private static final Map<ItemClassPair, Boolean> itemCreatesEntityResultCache = Maps.newHashMap();

	private static Map<Pair<Item, DataComponentMap>, List<Entity>> entityCache = Maps.newHashMap();
	private static boolean listenerRegistered = false;

	private record ItemClassPair(Item item, DataComponentMap components, Class<?> targetClass) {}

	protected EntityCollector(Level wrapped)
	{
		super(new WritableLevelData() {
			@Override public RespawnData getRespawnData() { return RespawnData.of(wrapped.dimension(), BlockPos.ZERO, 0, 0);}
			@Override public long getGameTime() { return 0; }
			@Override public boolean isHardcore() { return false; }
			@Override public Difficulty getDifficulty() { return Difficulty.EASY; }
			@Override public boolean isDifficultyLocked() { return false; }
			@Override public void setSpawn(RespawnData respawnData) {}
		}, wrapped.dimension(), wrapped.registryAccess(), wrapped.dimensionTypeRegistration(), false, wrapped.isDebug(), 0, 0);
		wrappedLevel = wrapped;

		if (!listenerRegistered)
		{
			LevelEvents.UNLOAD.register(level -> wrappedLevelsMap.clear());
			listenerRegistered = true;
		}
	}

	public static EntityCollector of(Level wrappedLevel)
	{
		if (!wrappedLevelsMap.containsKey(wrappedLevel))
		{
			wrappedLevelsMap.put(wrappedLevel, new EntityCollector(wrappedLevel));
		}

		return wrappedLevelsMap.get(wrappedLevel);
	}

	public static List<Entity> collectEntitiesFromItem(ItemStack itemStack, HolderLookup.Provider provider)
	{
		Pair<Item, DataComponentMap> key = Pair.of(itemStack.getItem(), ItemUtil.getItemComponents(itemStack));

		if (!entityCache.containsKey(key))
		{
			Minecraft minecraft = Minecraft.getInstance();
			List<Entity> entities = Lists.newArrayList();
			Item item = itemStack.getItem();
			ItemStack dummyStack = new ItemStack(item, itemStack.getCount());
			DataComponentMap itemComponents = ItemUtil.getItemComponents(itemStack);
			dummyStack.applyComponents(itemComponents);

			try
			{
				EntityCollector levelWrapper = EntityCollector.of(minecraft.player.level());

				Player dummyPlayer = new Player(levelWrapper, new GameProfile(UUID.randomUUID(), "_dummy")) {
					@Override public @Nullable GameType gameMode() { return GameType.DEFAULT_MODE; }
					@Override public boolean isSpectator() { return false; }
					@Override public boolean isCreative() { return false; }
				};

				dummyPlayer.setItemInHand(InteractionHand.MAIN_HAND, dummyStack);

				if (item instanceof SpawnEggItem spawnEggItem)
				{
					entities.add(spawnEggItem.getType(dummyStack).create(levelWrapper, EntitySpawnReason.COMMAND));
				}
				else
				{
					dummyStack.use(levelWrapper, dummyPlayer, InteractionHand.MAIN_HAND);
				}

				entities.addAll(levelWrapper.getCollectedEntities());

				// If we didn't spawn any entities, try again but this time on a simulated rail for minecart-like items.
				if (entities.isEmpty())
				{
					levelWrapper.setBlockState(Blocks.RAIL.defaultBlockState());
					dummyStack.useOn(new UseOnContext(levelWrapper, dummyPlayer, InteractionHand.MAIN_HAND, dummyStack, new BlockHitResult(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO, false)));
					levelWrapper.setBlockState(Blocks.AIR.defaultBlockState());

					entities.addAll(levelWrapper.getCollectedEntities());
				}

				// If we still didn't spawn any entities, try again but this time simulate placing on a solid wall for painting-like items.
				if (entities.isEmpty())
				{
					levelWrapper.setBlockState(Blocks.STONE.defaultBlockState());
					dummyStack.useOn(new UseOnContext(levelWrapper, dummyPlayer, InteractionHand.MAIN_HAND, dummyStack, new BlockHitResult(Vec3.ZERO, Direction.NORTH, BlockPos.ZERO, false)));
					levelWrapper.setBlockState(Blocks.AIR.defaultBlockState());

					entities.addAll(levelWrapper.getCollectedEntities());

					CustomData customData = (CustomData)itemComponents.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
					if (!customData.isEmpty())
					{
						// Entities collected here should be updated in case the item used a specific variant.
						CompoundTag tag = customData.copyTag();
						HolderLookup.Provider registries = levelWrapper.registryAccess();
						for (Entity entity : entities)
						{
							entity.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
						}
					}
					else
					{
						entities.clear();
					}
				}

				// Now iterate through all the collected entities and remove any projectiles to prevent some crashes with modded bobbers, etc.
				entities.removeIf(entity -> entity instanceof Projectile);
			}
			catch (Exception e)
			{
				// Log any errors.
				Iceberg.LOGGER.info("Unable to collect entities from \"" + itemStack.getItem().getName(itemStack).getString() + "\": " + e.getMessage());
			}

			entityCache.put(key, entities);
		}

		return entityCache.get(key);
	}

	public static <T extends Entity> boolean itemCreatesEntity(ItemStack itemStack, Class<T> targetClass, HolderLookup.Provider provider)
	{
		ItemClassPair key = new ItemClassPair(itemStack.getItem(), ItemUtil.getItemComponents(itemStack), targetClass);
		boolean result = false;
		if (!itemCreatesEntityResultCache.containsKey(key))
		{
			// Return true if any collected entities from this item are a subclass of the given type.
			for (Entity entity : collectEntitiesFromItem(itemStack, provider))
			{
				if (targetClass.isInstance(entity))
				{
					result = true;
					break;
				}
			}

			itemCreatesEntityResultCache.put(key, result);
		}

		return itemCreatesEntityResultCache.get(key);
	}

	public List<Entity> getCollectedEntities()
	{
		// Clear the collected entities after this method is called.
		List<Entity> entities = Lists.newArrayList();
		entities.addAll(collectedEntities);
		collectedEntities.clear();
		return entities;
	}

	public void setBlockState(BlockState blockState)
	{
		this.blockState = blockState;
	}

	@Override
	public BlockState getBlockState(BlockPos blockPos)
	{
		return blockState;
	}

	@Override
	public int getBrightness(LightLayer lightLayer, BlockPos blockPos)
	{
		return 15;
	}

	@Override
	public boolean noCollision(Entity entity, AABB boundingBox)
	{
		return true;
	}

	@Override
	public boolean addFreshEntity(Entity entity)
	{
		entity.setYRot(180.0f);
		collectedEntities.add(entity);
		return false;
	}

	@Override
	public LevelTickAccess<Block> getBlockTicks() { return wrappedLevel.getBlockTicks(); }

	@Override
	public LevelTickAccess<Fluid> getFluidTicks() { return wrappedLevel.getFluidTicks(); }

	@Override
	public ChunkSource getChunkSource() { return wrappedLevel.getChunkSource(); }

	@Override
	public void levelEvent(@Nullable Entity entity, int i, BlockPos blockPos, int j) { /* No events. */ }

	@Override
	public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, Context context) { /* No events. */ }

	@Override
	public List<? extends Player> players() { return wrappedLevel.players(); }

	@Override
	public Holder<Biome> getUncachedNoiseBiome(int p_204159_, int p_204160_, int p_204161_) { return wrappedLevel.getUncachedNoiseBiome(p_204159_, p_204160_, p_204161_); }

	@Override
	public FeatureFlagSet enabledFeatures() { return wrappedLevel.enabledFeatures(); }

	@Override
	public void sendBlockUpdated(BlockPos p_46612_, BlockState p_46613_, BlockState p_46614_, int p_46615_) { /* No block updates. */ }

	@Override
	public void playSeededSound(@Nullable Entity entity, double d, double e, double f, Holder<SoundEvent> holder, SoundSource soundSource, float g, float h, long l) { /* No sounds. */ }

	@Override
	public void playSeededSound(@Nullable Entity entity, Entity entity2, Holder<SoundEvent> holder, SoundSource soundSource, float f, float g, long l) { /* No sounds. */ }

	@Override
	public String gatherChunkSourceStats() { return wrappedLevel.gatherChunkSourceStats(); }

	@Override
	public Entity getEntity(int p_46492_) { return null; }

	@Override
	public MapItemSavedData getMapData(MapId mapId) { return wrappedLevel.getMapData(mapId); }

	@Override
	public void destroyBlockProgress(int p_46506_, BlockPos p_46507_, int p_46508_) { /* No block updates. */ }

	@Override
	public Scoreboard getScoreboard() { return wrappedLevel.getScoreboard(); }

	@Override
	public TickRateManager tickRateManager() { return wrappedLevel.tickRateManager(); }

	@Override
	public PotionBrewing potionBrewing() { return wrappedLevel.potionBrewing(); }

	@Override
	public LevelEntityGetter<Entity> getEntities()
	{
		return new LevelEntityGetter<Entity>() {

			@Override
			public Entity get(int p_156931_) { return null; }

			@Override
			public Entity get(UUID p_156939_) { return null; }

			@Override
			public Iterable<Entity> getAll() { return List.of(); }

			@Override
			public <U extends Entity> void get(EntityTypeTest<Entity, U> p_156935_, AbortableIterationConsumer<U> p_261602_) {}

			@Override
			public void get(AABB p_156937_, Consumer<Entity> p_156938_) {}

			@Override
			public <U extends Entity> void get(EntityTypeTest<Entity, U> p_156932_, AABB p_156933_, AbortableIterationConsumer<U> p_261542_) {}
		};

	}

	@Override
	public ClockManager clockManager() { return wrappedLevel.clockManager(); }

	@Override
	public int getSeaLevel() { return wrappedLevel.getSeaLevel(); }

	@Override
	public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator,
						double d, double e, double f, float g, boolean bl, ExplosionInteraction explosionInteraction, ParticleOptions particleOptions,
						ParticleOptions particleOptions2, WeightedList<ExplosionParticleInfo> weightedList, Holder<SoundEvent> holder) {}

	@Override
	public FuelValues fuelValues() { return wrappedLevel.fuelValues(); }

	@Override
	public RecipeAccess recipeAccess() { return wrappedLevel.recipeAccess(); }

	@Override
	public Collection<EnderDragonPart> dragonParts() { return wrappedLevel.dragonParts(); }

	@Override
	public EnvironmentAttributeSystem environmentAttributes() { return wrappedLevel.environmentAttributes(); }

	@Override
	public WorldBorder getWorldBorder() { return wrappedLevel.getWorldBorder(); }

	@Override
	public void setRespawnData(LevelData.RespawnData respawnData) { wrappedLevel.setRespawnData(respawnData); }

	@Override
	public LevelData.RespawnData getRespawnData() { return wrappedLevel.getRespawnData(); }
}
