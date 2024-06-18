package com.anthonyhilyard.iceberg.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.anthonyhilyard.iceberg.util.EntityCollector;
import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.iceberg.util.ItemUtil;
import com.google.common.collect.Maps;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import com.mojang.math.MatrixUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.AnimalArmorItem.BodyType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;


/**
 * An extended ItemRenderer with extra functionality, such as allowing items to be rendered to a RenderTarget
 * before drawing to screen for alpha support, and allowing handheld item models to be rendered into the gui.
 */
public class CustomItemRenderer extends ItemRenderer
{
	/* Cylindrical bounds for a model. */
	private record ModelBounds(Vector3f center, float height, float radius) {}

	private static RenderTarget iconFrameBuffer = null;
	private static ArmorStand armorStand = null;
	private static Wolf wolf = null;
	private static Horse horse = null;
	private static Entity entity = null;
	private static Pair<Item, DataComponentMap> cachedArmorStandItem = null;
	private static Pair<Item, DataComponentMap> cachedHorseArmorItem = null;
	private static Pair<Item, DataComponentMap> cachedWolfArmorItem = null;
	private static Pair<Item, DataComponentMap> cachedEntityItem = null;
	private static Map<Pair<Item, DataComponentMap>, ModelBounds> modelBoundsCache = Maps.newHashMap();

	private static final List<Direction> quadDirections;

	static
	{
		quadDirections = new ArrayList<>(Arrays.asList(Direction.values()));
		quadDirections.add(null);
	}

	private Minecraft minecraft;
	private final ModelManager modelManager;
	private final BlockEntityWithoutLevelRenderer blockEntityRenderer;

	public CustomItemRenderer(TextureManager textureManagerIn, ModelManager modelManagerIn, ItemColors itemColorsIn, BlockEntityWithoutLevelRenderer blockEntityRendererIn, Minecraft mcIn)
	{
		super(mcIn, textureManagerIn, modelManagerIn, itemColorsIn, blockEntityRendererIn);
		minecraft = mcIn;
		modelManager = modelManagerIn;
		blockEntityRenderer = blockEntityRendererIn;
		if (minecraft.getResourceManager() instanceof ReloadableResourceManager resourceManager)
		{
			resourceManager.registerReloadListener(this);
		}

		// Initialize the icon framebuffer if needed.
		if (iconFrameBuffer == null)
		{
			// Use 96 x 96 pixels for the icon frame buffer so at 1.5 scale we get 4x resolution (for smooth icons on larger gui scales).
			iconFrameBuffer = new MainTarget(96, 96);
			iconFrameBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			iconFrameBuffer.clear(Minecraft.ON_OSX);
		}
	}

	private void renderGuiModel(ItemStack itemStack, int x, int y, Quaternionf rotation, BakedModel bakedModel, GuiGraphics graphics)
	{
		minecraft.getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.translate(x + 8.0f, y + 8.0f, 150.0f);
		modelViewStack.mul((new Matrix4f()).scaling(1.0f, -1.0f, 1.0f));
		modelViewStack.scale(16.0f, 16.0f, 16.0f);
		RenderSystem.applyModelViewMatrix();

		BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		boolean flatLighting = !bakedModel.usesBlockLight();
		if (flatLighting) { Lighting.setupForFlatItems(); }

		PoseStack poseStack = new PoseStack();
		renderModel(itemStack, ItemDisplayContext.GUI, false, poseStack, rotation, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bakedModel);

		RenderSystem.disableDepthTest();
		bufferSource.endBatch();
		RenderSystem.enableDepthTest();

		if (flatLighting) { Lighting.setupFor3DItems(); }

		modelViewStack.popMatrix();
		RenderSystem.applyModelViewMatrix();
	}

	@SuppressWarnings("deprecation")
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
			RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, poseStack, bufferSource, packedLight));
		}
		catch (Exception e) {}

		poseStack.popPose();

		if (bufferSource instanceof BufferSource source)
		{
			source.endBatch();
		}

		entityRenderDispatcher.setRenderShadow(true);

		RenderSystem.applyModelViewMatrix();
		Lighting.setupFor3DItems();
	}

	private <T extends MultiBufferSource> void renderModelInternal(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack,
																   Quaternionf rotation, T bufferSource, int packedLight, int packedOverlay, BakedModel bakedModel,
																   Predicate<T> bufferSourceReady)
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

		if (!bakedModel.isCustomRenderer() && !itemStack.is(Items.TRIDENT))
		{
			boolean fabulous;
			if (displayContext != ItemDisplayContext.GUI && !displayContext.firstPerson() && itemStack.getItem() instanceof BlockItem blockItem)
			{
				Block block = blockItem.getBlock();
				fabulous = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
			}
			else
			{
				fabulous = true;
			}

			if (bufferSourceReady.test(bufferSource) && itemStack.getItem() instanceof BlockItem blockItem)
			{
				Block block = blockItem.getBlock();
				BakedModel blockModel = null;
				BlockModelShaper blockModelShaper = minecraft.getBlockRenderer().getBlockModelShaper();
				boolean isBlockEntity = false;

				blockModel = blockModelShaper.getBlockModel(block.defaultBlockState());
				if (blockModel != modelManager.getMissingModel())
				{
					// First try rendering via the BlockEntityWithoutLevelRenderer.
					try
					{
						blockEntityRenderer.renderByItem(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
					}
					catch (Exception e)
					{
						// Do nothing if an exception occurs.
					}
				}
				else
				{
					blockModel = null;
				}

				if (block.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
				{
					// This is a double block, so we'll need to render both halves.
					// First render the bottom half.
					BlockState bottomState = block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
					BakedModel bottomModel = blockModelShaper.getBlockModel(bottomState);
					renderBakedModel(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, bottomModel, fabulous);

					// Then render the top half.
					poseStack.pushPose();
					poseStack.translate(0.0f, 1.0f, 0.0f);
					BlockState topState = block.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
					BakedModel topModel = blockModelShaper.getBlockModel(topState);
					renderBakedModel(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, topModel, fabulous);
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
					renderBakedModel(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, blockModel, fabulous);
				}
			}

			// Now try rendering entity models for items that spawn entities.
			if (bufferSourceReady.test(bufferSource) && EntityCollector.itemCreatesEntity(itemStack, Entity.class))
			{
				if (updateEntity(itemStack))
				{
					renderEntityModel(entity, poseStack, bufferSource, packedLight);
				}
			}

			// If this is animal armor, render it here.
			if (bufferSourceReady.test(bufferSource) && itemStack.getItem() instanceof AnimalArmorItem animalArmor)
			{
				switch (animalArmor.getBodyType())
				{
					case EQUESTRIAN:
						if (updateHorseArmor(itemStack))
						{
							renderEntityModel(horse, poseStack, bufferSource, packedLight);
						}
						break;
					case CANINE:
						if (updateWolfArmor(itemStack))
						{
							renderEntityModel(wolf, poseStack, bufferSource, packedLight);
						}
						break;
				}
			}

			// Finally, fall back to just rendering the item model.
			if (bufferSourceReady.test(bufferSource))
			{
				renderBakedModel(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay, bakedModel, fabulous);
			}
		}
		else if (bufferSourceReady.test(bufferSource))
		{
			blockEntityRenderer.renderByItem(itemStack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
		}
	}

	private void renderModel(ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack, Quaternionf rotation, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel bakedModel)
	{
		if (!itemStack.isEmpty())
		{
			// If this model doesn't have a special transform for the given context type (most likely GUI), default to ground instead.
			ItemDisplayContext previewContext = displayContext;
			if (!bakedModel.getTransforms().hasTransform(displayContext))
			{
				previewContext = ItemDisplayContext.GROUND;
			}

			boolean isBlockItem = false, spawnsEntity = false, isArmor = false;
			if (itemStack.getItem() instanceof BlockItem)
			{
				isBlockItem = true;
			}
			else if (EntityCollector.itemCreatesEntity(itemStack, Entity.class))
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
				bakedModel.getTransforms().getTransform(previewContext).apply(leftHanded, poseStack);
			}
			poseStack.translate(-0.5f, -0.5f, -0.5f);

			// Get the model bounds.
			ModelBounds modelBounds = getModelBounds(itemStack, previewContext, leftHanded, poseStack, rotation, bufferSource, packedLight, packedOverlay, bakedModel);

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
				bakedModel.getTransforms().getTransform(previewContext).apply(leftHanded, poseStack);
			}
			poseStack.translate(-0.5f, -0.5f, -0.5f);

			CheckedBufferSource checkedBufferSource = CheckedBufferSource.create(bufferSource);
			renderModelInternal(itemStack, previewContext, leftHanded, poseStack, rotation, checkedBufferSource, packedLight, packedOverlay, bakedModel, b -> !b.hasRendered());

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
				renderer.render(blockEntity, minecraft.getTimer().getRealtimeDeltaTicks(), poseStack, bufferSource, packedLight, packedOverlay);
			}
		}
	}

	private void renderBakedModel(ItemStack itemStack, ItemDisplayContext displayContext, PoseStack poseStack,
								  MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel bakedModel, boolean fabulous)
	{
		RenderType renderType = ItemBlockRenderTypes.getRenderType(itemStack, fabulous);
		VertexConsumer vertexConsumer;
		if (hasAnimatedTexture(itemStack) && itemStack.hasFoil())
		{
			PoseStack.Pose pose = poseStack.last().copy();
			if (displayContext == ItemDisplayContext.GUI)
			{
				MatrixUtil.mulComponentWise(pose.pose(), 0.5F);
			}
			else if (displayContext.firstPerson())
			{
				MatrixUtil.mulComponentWise(pose.pose(), 0.75F);
			}

			vertexConsumer = getCompassFoilBuffer(bufferSource, renderType, pose);
		}
		else if (fabulous)
		{
			vertexConsumer = getFoilBufferDirect(bufferSource, renderType, true, itemStack.hasFoil());
		}
		else
		{
			vertexConsumer = getFoilBuffer(bufferSource, renderType, true, itemStack.hasFoil());
		}

		renderModelLists(bakedModel, itemStack, packedLight, packedOverlay, poseStack, vertexConsumer);
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
			armorStand = EntityType.ARMOR_STAND.create(minecraft.level);
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
		Entity collectedEntity = null;
		List<Entity> collectedEntities = EntityCollector.collectEntitiesFromItem(itemStack);
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
		if (!(horseArmorItem.getItem() instanceof AnimalArmorItem animalArmor) || animalArmor.getBodyType() != BodyType.EQUESTRIAN)
		{
			return false;
		}

		if (horse == null)
		{
			Minecraft minecraft = Minecraft.getInstance();
			horse = EntityType.HORSE.create(minecraft.level);
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
		if (!(wolfArmorItem.getItem() instanceof AnimalArmorItem animalArmor) || animalArmor.getBodyType() != BodyType.CANINE)
		{
			return false;
		}

		if (wolf == null)
		{
			Minecraft minecraft = Minecraft.getInstance();
			wolf = EntityType.WOLF.create(minecraft.level);
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
									   Quaternionf rotation, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel bakedModel)
	{
		Pair<Item, DataComponentMap> key = Pair.of(itemStack.getItem(), ItemUtil.getItemComponents(itemStack));
		if (!modelBoundsCache.containsKey(key))
		{
			VertexCollector vertexCollector = VertexCollector.create();
			renderModelInternal(itemStack, displayContext, leftHanded, poseStack, rotation, vertexCollector, packedLight, packedOverlay, bakedModel, b -> b.getVertices().isEmpty());

			// Now store the bounds in the cache.
			modelBoundsCache.put(key, boundsFromVertices(vertexCollector.getVertices()));
		}

		return modelBoundsCache.get(key);
	}

	public void renderDetailModelIntoGUI(ItemStack stack, int x, int y, Quaternionf rotation, GuiGraphics graphics)
	{
		Minecraft minecraft = Minecraft.getInstance();
		BakedModel bakedModel = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);

		try
		{
			renderGuiModel(stack, x, y, rotation, bakedModel, graphics);
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

	public void renderItemModelIntoGUIWithAlpha(PoseStack poseStack, ItemStack stack, int x, int y, float alpha)
	{
		BakedModel bakedModel = minecraft.getItemRenderer().getModel(stack, null, null, 0);
		RenderTarget lastFrameBuffer = minecraft.getMainRenderTarget();

		// Bind the icon framebuffer so we can render to texture.
		iconFrameBuffer.clear(Minecraft.ON_OSX);
		iconFrameBuffer.bindWrite(true);

		Matrix4f matrix = new Matrix4f();
		matrix.setOrtho(0.0f, iconFrameBuffer.width, iconFrameBuffer.height, 0.0f, 1000.0f, 3000.0f);

		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(matrix, VertexSorting.ORTHOGRAPHIC_Z);

		minecraft.getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.identity();
		modelViewStack.translate(48.0f, 48.0f, -2000.0f);
		modelViewStack.scale(96.0f, 96.0f, 96.0f);
		RenderSystem.applyModelViewMatrix();

		BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

		boolean flatLighting = !bakedModel.usesBlockLight();
		if (flatLighting)
		{
			Lighting.setupForFlatItems();
		}

		render(stack, ItemDisplayContext.GUI, false, new PoseStack(), bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, bakedModel);
		bufferSource.endBatch();
		RenderSystem.enableDepthTest();
		if (flatLighting)
		{
			Lighting.setupFor3DItems();
		}

		modelViewStack.popMatrix();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.restoreProjectionMatrix();

		// Rebind the previous framebuffer, if there was one.
		if (lastFrameBuffer != null)
		{
			lastFrameBuffer.bindWrite(true);

			// Blit from the texture we just rendered to, respecting the alpha value given.
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

			RenderSystem.setShaderTexture(0, iconFrameBuffer.getColorTextureId());
			GuiHelper.blit(poseStack, x, y, 16, 16, 0, 0, iconFrameBuffer.width, iconFrameBuffer.height, iconFrameBuffer.width, iconFrameBuffer.height);

			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

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
		super.onResourceManagerReload(resourceManager);

		// Clear the model bounds cache.
		modelBoundsCache.clear();
	}

}