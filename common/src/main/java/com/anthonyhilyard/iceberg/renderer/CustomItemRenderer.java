package com.anthonyhilyard.iceberg.renderer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.anthonyhilyard.iceberg.util.EntityCollector;
import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.iceberg.util.ItemUtil;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;


/**
 * An extended ItemRenderer with extra functionality, such as allowing items to be rendered to a RenderTarget
 * before drawing to screen for alpha support, and allowing handheld item models to be rendered into the gui.
 */
public class CustomItemRenderer extends ItemRenderer// implements ResourceManagerReloadListener
{
	//TODO
	/*
	private static CustomItemRenderer INSTANCE = null;
	public static CustomItemRenderer getInstance()
	{
		if (INSTANCE == null)
		{
			Minecraft minecraft = Minecraft.getInstance();
			INSTANCE = new CustomItemRenderer(minecraft, minecraft.getModelManager(), minecraft.getItemModelResolver());
		}

		return INSTANCE;
	}

	// Cylindrical bounds for a model.
	private record ModelBounds(Vector3f center, float height, float radius) {}

	public static boolean swapFrameBuffer = false;
	public static RenderTarget iconFrameBuffer = null;
	private static ArmorStand armorStand = null;
	private static Wolf wolf = null;
	private static Horse horse = null;
	private static Entity entity = null;
	private static Pair<Item, DataComponentMap> cachedArmorStandItem = null;
	private static Pair<Item, DataComponentMap> cachedHorseArmorItem = null;
	private static Pair<Item, DataComponentMap> cachedWolfArmorItem = null;
	private static Pair<Item, DataComponentMap> cachedEntityItem = null;
	private static Map<Pair<Item, DataComponentMap>, ModelBounds> modelBoundsCache = Maps.newHashMap();
	private static Map<BakedModel, Boolean> testedModels = Maps.newHashMap();

	private Minecraft minecraft;
	private final ModelManager modelManager;

	@Deprecated(forRemoval = true, since = "1.2.12")
	public CustomItemRenderer(Minecraft mcIn, ModelManager modelManagerIn, ItemModelResolver itemModelResolver)
	{
		super(itemModelResolver);

		minecraft = mcIn;
		modelManager = modelManagerIn;
	}

	private void renderGuiModel(ItemStack itemStack, int x, int y, Quaternionf rotation, GuiGraphics graphics)
	{
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.translate(x + 8.0f, y + 8.0f, 150.0f);
		modelViewStack.mul((new Matrix4f()).scaling(1.0f, -1.0f, 1.0f));
		modelViewStack.scale(16.0f, 16.0f, 16.0f);

		RenderSystem.disableDepthTest();
		graphics.drawSpecial(bufferSource -> renderModel(itemStack, ItemDisplayContext.GUI, false, new PoseStack(), rotation, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));
		RenderSystem.enableDepthTest();

		modelViewStack.popMatrix();
	}

	private void renderEntityModel(Entity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		Minecraft minecraft = Minecraft.getInstance();
		EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
		Lighting.setupForEntityInInventory();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		entityRenderDispatcher.setRenderShadow(false);

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));

		try
		{
			RenderSystem.recordRenderCall(() -> entityRenderDispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, poseStack, bufferSource, packedLight));
			RenderSystem.replayQueue();
		}
		catch (Exception e) {}

		poseStack.popPose();

		if (bufferSource instanceof BufferSource source)
		{
			source.endBatch();
		}

		entityRenderDispatcher.setRenderShadow(true);

		Lighting.setupFor3DItems();
	}

	private static final List<Item> horseArmor = List.of(Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR,Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR);
	private <T extends MultiBufferSource> void renderModelInternal(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack,
																   T bufferSource, int packedLight, int packedOverlay, Predicate<T> bufferSourceReady)
	{
		Minecraft minecraft = Minecraft.getInstance();

		if (ItemUtil.getEquipmentSlot(itemStack).isArmor())
		{
			if (updateArmorStand(itemStack))
			{
				poseStack.pushPose();
				poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
				renderEntityModel(armorStand, poseStack, bufferSource, packedLight);
				poseStack.popPose();
			}
		}

		if (bufferSourceReady.test(bufferSource) && itemStack.getItem() instanceof BlockItem blockItem)
		{
			Block block = blockItem.getBlock();
			BakedModel blockModel = null;
			BlockModelShaper blockModelShaper = minecraft.getBlockRenderer().getBlockModelShaper();
			boolean isBlockEntity = false;

			blockModel = blockModelShaper.getBlockModel(block.defaultBlockState());
			if (blockModel == modelManager.getMissingModel())
			{
				blockModel = minecraft.getModelManager().getModel(BlockModelShaper.stateToModelLocation(blockItem.getBlock().defaultBlockState()));
			}

			if (blockModel == modelManager.getMissingModel())
			{
				blockModel = null;
			}

			if (block.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
			{
				// This is a double block, so we'll need to render both halves.
				// First render the bottom half.
				BlockState bottomState = block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
				BakedModel bottomModel = blockModelShaper.getBlockModel(bottomState);
				renderBakedModelSafe(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, bottomModel);

				// Then render the top half.
				poseStack.pushPose();
				poseStack.translate(0.0f, 1.0f, 0.0f);
				BlockState topState = block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
				BakedModel topModel = blockModelShaper.getBlockModel(topState);
				renderBakedModelSafe(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, topModel);
				poseStack.popPose();
			}

			if (blockItem.getBlock() instanceof EntityBlock entityBlock)
			{
				isBlockEntity = true;
				try
				{
					renderBlockEntity(itemStack, poseStack, bufferSource, packedLight, packedOverlay, minecraft, entityBlock, blockItem.getBlock().defaultBlockState());
				}
				catch (Exception e)
				{
					// This can fail for things like beacons that require a level.  We'll just ignore it.
				}
			}

			// If we still haven't rendered anything or this is a block entity, try rendering the block model.
			if (blockModel != null && (bufferSourceReady.test(bufferSource) || isBlockEntity))
			{
				renderBakedModelSafe(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, blockModel);
			}
		}

		// Now try rendering entity models for items that spawn entities.
		if (minecraft.level != null && bufferSourceReady.test(bufferSource) && EntityCollector.itemCreatesEntity(itemStack, Entity.class, minecraft.level.registryAccess()))
		{
			if (updateEntity(itemStack))
			{
				renderEntityModel(entity, poseStack, bufferSource, packedLight);
			}
		}

		// If this is animal armor, render it here.
		if (bufferSourceReady.test(bufferSource))
		{
			if (horseArmor.contains(itemStack.getItem())) {
				if (updateHorseArmor(itemStack))
				{
					renderEntityModel(horse, poseStack, bufferSource, packedLight);
				}
			}
			else if (itemStack.getItem() == Items.WOLF_ARMOR) {
				if (updateWolfArmor(itemStack))
				{
					renderEntityModel(wolf, poseStack, bufferSource, packedLight);
				}
			}
		}

		// Finally, fall back to just rendering the item model.
		if (bufferSourceReady.test(bufferSource))
		{
			displayContext = ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			renderModelNoTransform(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
		}
	}

	private ItemTransform getItemTransform(ItemStack itemStack, ItemDisplayContext displayContext)
	{
		resolver.updateForTopItem(scratchItemStackRenderState, itemStack, displayContext, false, null, null, 0);
		return scratchItemStackRenderState.transform();
	}

	private void renderModel(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack, Quaternionf rotation, MultiBufferSource bufferSource, int packedLight, int packedOverlay)
	{
		if (!itemStack.isEmpty())
		{
			Minecraft minecraft = Minecraft.getInstance();

			// If this model doesn't have a special transform for the given context type (most likely GUI), default to ground instead.
			ItemDisplayContext previewContext = displayContext;
			ItemTransform itemTransform = getItemTransform(itemStack, previewContext);

			boolean isBlockItem = false, spawnsEntity = false, isArmor = false;
			if (itemStack.getItem() instanceof BlockItem)
			{
				isBlockItem = true;

				if (itemTransform != ItemTransform.NO_TRANSFORM)
				{
					previewContext = ItemDisplayContext.GROUND;
					itemTransform = getItemTransform(itemStack, previewContext);
				}
			}
			else if (minecraft.level != null && EntityCollector.itemCreatesEntity(itemStack, Entity.class, minecraft.level.registryAccess()))
			{
				spawnsEntity = true;
			}

			if (ItemUtil.getEquipmentSlot(itemStack).isArmor())
			{
				isArmor = true;
			}

			poseStack.pushPose();

			poseStack.translate(0.5f, 0.5f, 0.5f);
			if (isBlockItem || spawnsEntity)
			{
				// Apply the standard block rotation so block entities match other blocks.
				poseStack.mulPose(new Quaternionf().rotationXYZ((float)Math.toRadians(30.0f), (float)Math.toRadians(225.0f), 0.0f));
			}
			else
			{
				itemTransform.apply(leftHanded, poseStack);
			}
			poseStack.translate(-0.5f, -0.5f, -0.5f);

			// Get the model bounds.
			ModelBounds modelBounds = getModelBounds(itemStack, previewContext, leftHanded, poseStack, rotation, packedLight, packedOverlay);

			// Undo the camera transforms now that we have the model bounds.
			poseStack.popPose();
			poseStack.pushPose();

			// Rotate the model.
			poseStack.mulPose(rotation);

			// Scale the model to fit.
			float scale = 0.8f / Math.max(modelBounds.height, modelBounds.radius * 2.0f);

			// Adjust the scale based on the armor type.
			if (isArmor)
			{
				switch (ItemUtil.getEquipmentSlot(itemStack))
				{
					case HEAD:
						scale *= 0.75f;
						break;
					case LEGS:
						scale *= 1.3f;
						break;
					case FEET:
						scale *= 0.85f;
						break;
					default:
						break;
				}
			}

			poseStack.scale(scale, scale, scale);

			// Translate the model to the center of the item.
			poseStack.translate(-modelBounds.center.x(), -modelBounds.center.y(), -modelBounds.center.z());

			poseStack.translate(0.5f, 0.5f, 0.5f);
			// Reapply the camera transforms.
			if (isBlockItem || spawnsEntity)
			{
				// Apply the standard block rotation so block entities match other blocks.
				poseStack.mulPose(new Quaternionf().rotationXYZ((float)Math.toRadians(30.0f), (float)Math.toRadians(225.0f), 0.0f));
			}
			else
			{
				itemTransform.apply(leftHanded, poseStack);
			}
			poseStack.translate(-0.5f, -0.5f, -0.5f);

			CheckedBufferSource checkedBufferSource = CheckedBufferSource.create(bufferSource);
			renderModelInternal(itemStack, previewContext, leftHanded, poseStack, checkedBufferSource, packedLight, packedOverlay, b -> !b.hasRendered());

			poseStack.popPose();
		}
	}

	private void renderBlockEntity(ItemStack itemStack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
								   int packedOverlay, Minecraft minecraft, EntityBlock entityBlock, BlockState blockState) throws Exception
	{
		// If we didn't render via the BlockEntityWithoutLevelRenderer, now check if this is a block entity and render that.
		BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, blockState);
		if (blockEntity != null)
		{
			blockEntity.applyComponentsFromItemStack(itemStack);
			
			BlockEntityRenderer<BlockEntity> renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
			if (renderer != null)
			{
				renderer.render(blockEntity, minecraft.getDeltaTracker().getRealtimeDeltaTicks(), poseStack, bufferSource, packedLight, packedOverlay);
			}
		}
	}

	private void renderBakedModelSafe(ItemStack itemStack, ItemDisplayContext displayContext, PoseStack poseStack,
									  MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel bakedModel)
	{
		if (!testedModels.containsKey(bakedModel))
		{
			try
			{
				renderBakedModel(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, bakedModel);
				testedModels.put(bakedModel, true);
			}
			catch (Exception e)
			{
				// If the model failed to render, make a note of it.
				testedModels.put(bakedModel, false);
			}
		}
		else if (testedModels.get(bakedModel))
		{
			renderBakedModel(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, bakedModel);
		}
	}

	private static int[] tints = new int[0];

	private void renderBakedModel(ItemStack itemStack, ItemDisplayContext displayContext, PoseStack poseStack,
								  MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel bakedModel)
	{
		RenderType renderType = ItemBlockRenderTypes.getRenderType(itemStack);
		renderItem(displayContext, poseStack, bufferSource, packedLight, packedOverlay, tints, bakedModel, renderType, FoilType.NONE);
	}

	private void renderModelNoTransform(ItemStack itemStack, ItemDisplayContext displayContext, PoseStack poseStack,
								  MultiBufferSource multiBufferSource, int packedLight, int packedOverlay)
	{
		resolver.updateForTopItem(scratchItemStackRenderState, itemStack, displayContext, false, null, null, 0);

		boolean flatLighting = !scratchItemStackRenderState.usesBlockLight();
		if (flatLighting)
		{
			if (multiBufferSource instanceof BufferSource bufferSource)
			{
				bufferSource.endBatch();
			}
			Lighting.setupForFlatItems();
		}

		for (int k = 0; k < scratchItemStackRenderState.activeLayerCount; ++k)
		{
			ILayerRenderState layer = (ILayerRenderState)scratchItemStackRenderState.layers[k];
			layer.renderWithoutTransform(poseStack, displayContext, multiBufferSource, packedLight, packedOverlay);
		}

		if (multiBufferSource instanceof BufferSource bufferSource)
		{
			bufferSource.endBatch();
		}

		if (flatLighting)
		{
			Lighting.setupFor3DItems();
		}
	}

	private boolean updateArmorStand(ItemStack itemStack)
	{
		EquipmentSlot equipmentSlot = ItemUtil.getEquipmentSlot(itemStack);
		if (!equipmentSlot.isArmor())
		{
			// This isn't armor, so don't render anything.
			return false;
		}

		if (armorStand == null)
		{
			Minecraft minecraft = Minecraft.getInstance();
			armorStand = EntityType.ARMOR_STAND.create(minecraft.level, EntitySpawnReason.COMMAND);
			armorStand.setInvisible(true);
		}

		// If somehow the armor stand is still null, then we can't render anything.
		if (armorStand == null)
		{
			return false;
		}

		// If the item has changed, then we need to update the armor stand.
		if (cachedArmorStandItem != Pair.of(itemStack.getItem(), ItemUtil.getItemComponents(itemStack)))
		{
			// Clear the armor stand.
			for (EquipmentSlot slot : EquipmentSlot.values())
			{
				armorStand.setItemSlot(slot, ItemStack.EMPTY);
			}

			// Equip the armor stand with the armor.
			armorStand.setItemSlot(equipmentSlot, itemStack);

			cachedArmorStandItem = Pair.of(itemStack.getItem(), ItemUtil.getItemComponents(itemStack));
		}
		return true;
	}

	private Entity getEntityFromItem(ItemStack itemStack)
	{
		Minecraft minecraft = Minecraft.getInstance();

		Entity collectedEntity = null;
		List<Entity> collectedEntities = minecraft.level == null ? List.of() : EntityCollector.collectEntitiesFromItem(itemStack, minecraft.level.registryAccess());
		if (!collectedEntities.isEmpty())
		{
			// Just return the first entity collected.
			// TODO: Should all entities be considered for weird items that spawn multiple?
			collectedEntity = collectedEntities.get(0);
		}

		return collectedEntity;
	}

	private boolean updateEntity(ItemStack itemStack)
	{
		Pair<Item, DataComponentMap> entityItem = Pair.of(itemStack.getItem(), ItemUtil.getItemComponents(itemStack));
		if (entity == null || cachedEntityItem != entityItem)
		{
			entity = getEntityFromItem(itemStack);
			cachedEntityItem = entityItem;
		}

		// If somehow the entity is still null, then we can't render anything.
		return entity != null;
	}

	private boolean updateHorseArmor(ItemStack horseArmorItem)
	{
		// If this isn't a horse armor item, we can't render anything.
		if (!horseArmor.contains(horseArmorItem.getItem()))
		{
			return false;
		}

		if (horse == null)
		{
			Minecraft minecraft = Minecraft.getInstance();
			horse = EntityType.HORSE.create(minecraft.level, EntitySpawnReason.COMMAND);
			horse.setInvisible(true);
		}

		// If somehow the horse is still null, then we can't render anything.
		if (horse == null)
		{
			return false;
		}

		// If the item has changed, then we need to update the horse.
		if (cachedHorseArmorItem != Pair.of(horseArmorItem.getItem(), ItemUtil.getItemComponents(horseArmorItem)))
		{
			// Equip the horse with the armor.
			horse.setBodyArmorItem(horseArmorItem);;

			cachedHorseArmorItem = Pair.of(horseArmorItem.getItem(), ItemUtil.getItemComponents(horseArmorItem));
		}
		return true;
	}

	private boolean updateWolfArmor(ItemStack wolfArmorItem)
	{
		// If this isn't a wolf armor item, we can't render anything.
		if (wolfArmorItem.getItem() != Items.WOLF_ARMOR)
		{
			return false;
		}

		if (wolf == null)
		{
			Minecraft minecraft = Minecraft.getInstance();
			wolf = EntityType.WOLF.create(minecraft.level, EntitySpawnReason.COMMAND);
			wolf.setInvisible(true);
		}

		// If somehow the wolf is still null, then we can't render anything.
		if (wolf == null)
		{
			return false;
		}

		// If the item has changed, then we need to update the wolf.
		if (cachedWolfArmorItem != Pair.of(wolfArmorItem.getItem(), ItemUtil.getItemComponents(wolfArmorItem)))
		{
			// Equip the horse with the armor.
			wolf.setBodyArmorItem(wolfArmorItem);

			cachedWolfArmorItem = Pair.of(wolfArmorItem.getItem(), ItemUtil.getItemComponents(wolfArmorItem));
		}
		return true;
	}

	private ModelBounds boundsFromVertices(Set<Vector3f> vertices)
	{
		Vector3f center = new Vector3f();
		float radius = 0.0f;
		float height = 0.0f;

		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxX = Float.MIN_VALUE;
		float maxY = Float.MIN_VALUE;
		float maxZ = Float.MIN_VALUE;

		for (Vector3f vertex : vertices)
		{
			minX = Math.min(minX, vertex.x);
			minY = Math.min(minY, vertex.y);
			minZ = Math.min(minZ, vertex.z);
			maxX = Math.max(maxX, vertex.x);
			maxY = Math.max(maxY, vertex.y);
			maxZ = Math.max(maxZ, vertex.z);
		}

		center = new Vector3f((minX + maxX) / 2.0f, (minY + maxY) / 2.0f, (minZ + maxZ) / 2.0f);
		height = maxY - minY;

		for (Vector3f vertex : vertices)
		{
			radius = Math.max(radius, (float) Math.sqrt((vertex.x - center.x) * (vertex.x - center.x) + (vertex.z - center.z) * (vertex.z - center.z)));
		}

		return new ModelBounds(center, height, radius);
	}

	private ModelBounds getModelBounds(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack,
									   Quaternionf rotation, int packedLight, int packedOverlay)
	{
		Pair<Item, DataComponentMap> key = Pair.of(itemStack.getItem(), ItemUtil.getItemComponents(itemStack));
		if (!modelBoundsCache.containsKey(key))
		{
			VertexCollector vertexCollector = VertexCollector.create();
			renderModelInternal(itemStack, displayContext, leftHanded, poseStack, vertexCollector, packedLight, packedOverlay, b -> b.getVertices().isEmpty());

			// Now store the bounds in the cache.
			modelBoundsCache.put(key, boundsFromVertices(vertexCollector.getVertices()));
		}

		return modelBoundsCache.get(key);
	}

	public void renderDetailModelIntoGUI(ItemStack stack, int x, int y, Quaternionf rotation, GuiGraphics graphics)
	{
		try
		{
			renderGuiModel(stack, x, y, rotation, graphics);
		}
		catch (Throwable throwable)
		{
			CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering item");
			CrashReportCategory crashReportCategory = crashReport.addCategory("Item being rendered");
			crashReportCategory.setDetail("Item Type", () -> { return String.valueOf(stack.getItem()); });
			crashReportCategory.setDetail("Item Components", () -> { return String.valueOf(stack.getComponents()); });
			crashReportCategory.setDetail("Item Foil", () -> { return String.valueOf(stack.hasFoil()); });
			throw new ReportedException(crashReport);
		}
	}

	public void renderItemModelIntoGUIWithAlpha(GuiGraphics graphics, ItemStack stack, int x, int y, float alpha)
	{
		// Initialize the icon framebuffer if needed.
		if (iconFrameBuffer == null)
		{
			// Use 96 x 96 pixels for the icon frame buffer so at 1.5 scale we get 4x resolution (for smooth icons on larger gui scales).
			iconFrameBuffer = new MainTarget(96, 96);
			iconFrameBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		}

		RenderTarget lastFrameBuffer = minecraft.getMainRenderTarget();

		// Bind the icon framebuffer so we can render to texture.
		iconFrameBuffer.clear();
		iconFrameBuffer.bindWrite(true);

		Matrix4f matrix = new Matrix4f();
		matrix.setOrtho(0.0f, iconFrameBuffer.width, iconFrameBuffer.height, 0.0f, 1000.0f, 3000.0f);

		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(matrix, ProjectionType.ORTHOGRAPHIC);

		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.identity();
		modelViewStack.translate(48.0f, 48.0f, -2000.0f);
		modelViewStack.scale(96.0f, 96.0f, 96.0f);

		swapFrameBuffer = true;

		resolver.updateForTopItem(scratchItemStackRenderState, stack, ItemDisplayContext.GUI, false, null, null, 0);

		boolean flatLighting = !scratchItemStackRenderState.usesBlockLight();
		if (flatLighting)
		{
			graphics.flush();
			Lighting.setupForFlatItems();
		}

		graphics.drawSpecial(bufferSource -> scratchItemStackRenderState.render(new PoseStack(), bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));

		if (flatLighting)
		{
			Lighting.setupFor3DItems();
		}

		swapFrameBuffer = false;

		modelViewStack.popMatrix();
		RenderSystem.restoreProjectionMatrix();

		// Rebind the previous framebuffer, if there was one.
		if (lastFrameBuffer != null)
		{
			lastFrameBuffer.bindWrite(true);

			// Blit from the texture we just rendered to, respecting the alpha value given.
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();7
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

			GuiHelper.blit(graphics, iconFrameBuffer.getColorTextureId(), x, y, 16, 16, 0, 0, iconFrameBuffer.width, iconFrameBuffer.height, iconFrameBuffer.width, iconFrameBuffer.height);

			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			graphics.flush();

			iconFrameBuffer.unbindRead();
		}
		else
		{
			iconFrameBuffer.unbindWrite();
		}
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager)
	{
		// Clear the model bounds cache.
		modelBoundsCache.clear();
	}
	*/
}