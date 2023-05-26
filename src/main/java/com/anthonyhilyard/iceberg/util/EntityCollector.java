package com.anthonyhilyard.iceberg.util;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

public class EntityCollector extends Level
{
	private final Level wrappedLevel;
	private final List<Entity> collectedEntities = Lists.newArrayList();
	private BlockState blockState = Blocks.AIR.defaultBlockState();

	private static final Map<Level, EntityCollector> wrappedLevelsMap = Maps.newHashMap();
	private static final Map<ItemClassPair, Boolean> itemCreatesEntityResultCache = Maps.newHashMap();

	private record ItemClassPair(Item item, CompoundTag tag, Class<?> targetClass) {}

	protected EntityCollector(Level wrapped)
	{
		super(new WritableLevelData() {
			@Override public int getXSpawn() { return 0; }
			@Override public int getYSpawn() { return 0; }
			@Override public int getZSpawn() { return 0; }
			@Override public float getSpawnAngle() { return 0.0f; }
			@Override public long getGameTime() { return 0; }
			@Override public long getDayTime() { return 0; }
			@Override public boolean isThundering() { return false; }
			@Override public boolean isRaining() { return false; }
			@Override public void setRaining(boolean isRaining) {}
			@Override public boolean isHardcore() { return false; }
			@Override public GameRules getGameRules() { return new GameRules(); }
			@Override public Difficulty getDifficulty() { return Difficulty.EASY; }
			@Override public boolean isDifficultyLocked() { return false; }
			@Override public void setXSpawn(int x) {}
			@Override public void setYSpawn(int p_78652_) {}
			@Override public void setZSpawn(int p_78653_) {}
			@Override public void setSpawnAngle(float p_78648_) {}
		}, null, wrapped.dimensionTypeRegistration(), wrapped.getProfilerSupplier(), false, wrapped.isDebug(), 0, 0);
		wrappedLevel = wrapped;
	}

	public static EntityCollector of(Level wrappedLevel)
	{
		if (!wrappedLevelsMap.containsKey(wrappedLevel))
		{
			wrappedLevelsMap.put(wrappedLevel, new EntityCollector(wrappedLevel));
		}

		return wrappedLevelsMap.get(wrappedLevel);
	}

	public static List<Entity> collectEntitiesFromItem(ItemStack itemStack)
	{
		Minecraft minecraft = Minecraft.getInstance();
		List<Entity> entities = Lists.newArrayList();
		Item item = itemStack.getItem();
		ItemStack dummyStack = new ItemStack(item, itemStack.getCount());
		dummyStack.setTag(itemStack.getTag());

		try
		{
			Player dummyPlayer = new Player(minecraft.player.level, BlockPos.ZERO, 0.0f, new GameProfile(null, "_dummy"), null) {
				@Override public boolean isSpectator() { return false; }
				@Override public boolean isCreative() { return false; }
			};

			dummyPlayer.setItemInHand(InteractionHand.MAIN_HAND, dummyStack);

			EntityCollector levelWrapper = EntityCollector.of(dummyPlayer.level);

			if (item instanceof SpawnEggItem spawnEggItem)
			{
				entities.add(spawnEggItem.getType(new CompoundTag()).create(levelWrapper));
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
				dummyStack.useOn(new UseOnContext(levelWrapper, dummyPlayer, InteractionHand.MAIN_HAND, dummyPlayer.getItemInHand(InteractionHand.MAIN_HAND), new BlockHitResult(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO, false)));
				levelWrapper.setBlockState(Blocks.AIR.defaultBlockState());

				entities.addAll(levelWrapper.getCollectedEntities());
			}

			// If we still didn't spawn any entities, try again but this time simulate placing on a solid wall for painting-like items.
			if (entities.isEmpty())
			{
				levelWrapper.setBlockState(Blocks.STONE.defaultBlockState());
				dummyStack.useOn(new UseOnContext(levelWrapper, dummyPlayer, InteractionHand.MAIN_HAND, dummyPlayer.getItemInHand(InteractionHand.MAIN_HAND), new BlockHitResult(Vec3.ZERO, Direction.NORTH, BlockPos.ZERO, false)));
				levelWrapper.setBlockState(Blocks.AIR.defaultBlockState());

				entities.addAll(levelWrapper.getCollectedEntities());
			}

			// Now iterate through all the collected entities and remove any projectiles to prevent some crashes with modded bobbers, etc.
			entities.removeIf(entity -> entity instanceof Projectile);
		}
		catch (Exception e)
		{
			// Ignore any errors.
		}

		return entities;
	}

	public static <T extends Entity> boolean itemCreatesEntity(ItemStack itemStack, Class<T> targetClass)
	{
		ItemClassPair key = new ItemClassPair(itemStack.getItem(), itemStack.getTag(), targetClass);
		boolean result = false;
		if (!itemCreatesEntityResultCache.containsKey(key))
		{
			// Return true if any collected entities from this item are a subclass of the given type.
			for (Entity entity : collectEntitiesFromItem(itemStack))
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
	public void levelEvent(Player p_46771_, int p_46772_, BlockPos p_46773_, int p_46774_) { /* No events. */ }

	@Override
	public void gameEvent(GameEvent p_220404_, Vec3 p_220405_, Context p_220406_) { /* No events. */  }


	@Override
	public List<? extends Player> players() { return wrappedLevel.players(); }


	@Override
	public Holder<Biome> getUncachedNoiseBiome(int p_204159_, int p_204160_, int p_204161_) { return wrappedLevel.getUncachedNoiseBiome(p_204159_, p_204160_, p_204161_); }


	@Override
	public RegistryAccess registryAccess() { return wrappedLevel.registryAccess(); }

	@Override
	public float getShade(Direction p_45522_, boolean p_45523_) { return wrappedLevel.getShade(p_45522_, p_45523_); }


	@Override
	public void sendBlockUpdated(BlockPos p_46612_, BlockState p_46613_, BlockState p_46614_, int p_46615_) { /* No block updates. */ }

	@Override
	public void playSeededSound(@Nullable Player p_220363_, double p_220364_, double p_220365_, double p_220366_, SoundEvent p_220367_, SoundSource p_220368_, float p_220369_, float p_220370_, long p_220371_) { /* No sounds. */ }

	@Override
	public void playSeededSound(@Nullable Player p_220372_, Entity p_220373_, SoundEvent p_220374_, SoundSource p_220375_, float p_220376_, float p_220377_, long p_220378_) {/* No sounds. */ }

	@Override
	public String gatherChunkSourceStats() { return wrappedLevel.gatherChunkSourceStats(); }

	@Override
	public Entity getEntity(int p_46492_) { return null; }


	@Override
	public MapItemSavedData getMapData(String p_46650_) { return wrappedLevel.getMapData(p_46650_); }

	@Override
	public void setMapData(String p_151533_, MapItemSavedData p_151534_) { /* No map data updates. */ }

	@Override
	public int getFreeMapId() { return wrappedLevel.getFreeMapId(); }

	@Override
	public void destroyBlockProgress(int p_46506_, BlockPos p_46507_, int p_46508_) { /* No block updates. */ }


	@Override
	public Scoreboard getScoreboard() { return wrappedLevel.getScoreboard(); }

	@Override
	public RecipeManager getRecipeManager() { return wrappedLevel.getRecipeManager(); }

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
			public <U extends Entity> void get(EntityTypeTest<Entity, U> p_156935_, Consumer<U> p_156936_) {}

			@Override
			public void get(AABB p_156937_, Consumer<Entity> p_156938_) {}

			@Override
			public <U extends Entity> void get(EntityTypeTest<Entity, U> p_156932_, AABB p_156933_, Consumer<U> p_156934_) {}
		};

	}
}
