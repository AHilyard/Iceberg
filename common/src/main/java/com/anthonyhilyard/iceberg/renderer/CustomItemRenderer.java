package com.anthonyhilyard.iceberg.renderer;

import com.anthonyhilyard.iceberg.util.EntityCollector;
import com.anthonyhilyard.iceberg.util.IGuiRenderStateAccess;
import com.anthonyhilyard.iceberg.util.ItemUtil;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.joml.*;

import java.lang.Math;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class CustomItemRenderer
{
	private final Minecraft minecraft;
	private RenderTarget renderTarget;
	private final ProjectionMatrixBuffer projectionMatrixBuffer;
	private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
	private boolean isClosed = false;

	private static ArmorStand armorStand = null;
	private static Wolf wolf = null;
	private static Horse horse = null;
	private static Entity cachedSpawnEntity = null;

	private static Pair<Item, DataComponentMap> cachedArmorStandItem = null;
	private static Pair<Item, DataComponentMap> cachedHorseArmorItem = null;
	private static Pair<Item, DataComponentMap> cachedWolfArmorItem = null;
	private static Pair<Item, DataComponentMap> cachedEntityItem = null;

	private final BlockModelRenderState blockRenderState = new BlockModelRenderState();

	public CustomItemRenderer(Minecraft mc)
	{
		this.minecraft = mc;
		this.projectionMatrixBuffer = new ProjectionMatrixBuffer("iceberg_custom_item");
	}

	public void renderDetailModelIntoGUI(ItemStack stack, int x, int y, Quaternionf rotation, GuiGraphicsExtractor graphics)
	{
		drawAndBlit(graphics, stack, x, y, 1.0f, rotation, true);
	}

	public void renderItemModelIntoGUIWithAlpha(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float alpha)
	{
		drawAndBlit(graphics, stack, x, y, alpha, null, false);
	}

	public void renderItemIntoGUI(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float alpha, Quaternionf rotation)
	{
		drawAndBlit(graphics, stack, x, y, alpha, rotation, false);
	}

	private static final List<Item> horseArmor = List.of(Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR);

	private void drawAndBlit(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float alpha, Quaternionf rotation, boolean renderEntities)
	{
		if (isClosed || stack.isEmpty())
		{
			return;
		}

		TrackingItemStackRenderState itemState = new TrackingItemStackRenderState();
		minecraft.getItemModelResolver().updateForTopItem(
			itemState, stack, ItemDisplayContext.GUI,
			minecraft.level, minecraft.player, 0
		);

		if (itemState.isEmpty())
		{
			return;
		}

		int fboSize = 96;
		if (renderTarget == null || renderTarget.width != fboSize)
		{
			if (renderTarget != null)
			{
				renderTarget.destroyBuffers();
			}
			renderTarget = new TextureTarget("Iceberg Item Renderer", fboSize, fboSize, true, GpuFormat.RGBA8_UNORM);
		}

		try (RenderPass clearPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
			() -> "Item",
			// 0x00000000 is a transparent black (outline).
			renderTarget.getColorTextureView(), Optional.of(new Vector4f(0, 0, 0, 0)),
			renderTarget.getDepthTextureView(), OptionalDouble.of(1.0)
		)) {}

		RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
		RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

		RenderSystem.backupProjectionMatrix();
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.identity();

		Matrix4f ortho = new Matrix4f().setOrtho(0f, 16f, 16f, 0f, -1000f, 1000f);
		RenderSystem.setProjectionMatrix(
			this.projectionMatrixBuffer.getBuffer(ortho),
			ProjectionType.ORTHOGRAPHIC
		);

		PoseStack poseStack = new PoseStack();
		poseStack.translate(8.0, 8.0, 150.0);
		poseStack.scale(16.0f, -16.0f, 16.0f);
		if (rotation != null)
		{
			poseStack.mulPose(rotation);
		}

		boolean isSpinning = rotation != null;
		boolean is3D = itemState.usesBlockLight() || isSpinning;
		boolean renderedEntity = false;

		if (renderEntities)
		{
			if (tryEntityRenderers(stack, poseStack, rotation))
			{
				renderedEntity = true;
				is3D = true;
			}
		}

		if (!renderedEntity && rotation != null)
		{
			poseStack.scale(0.80f, 0.80f, 0.80f);
		}

		if (!is3D)
		{
			minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
		}

		if (!renderedEntity)
		{
			itemState.submit(poseStack, submitNodeStorage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
		}

		minecraft.gameRenderer.featureRenderDispatcher().renderAllFeatures(submitNodeStorage);

		if (!is3D)
		{
			// We must set the renderer lighting back to 3D.
			minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
		}

		modelViewStack.popMatrix();
		RenderSystem.restoreProjectionMatrix();
		RenderSystem.outputColorTextureOverride = null;
		RenderSystem.outputDepthTextureOverride = null;

		int color = ARGB.color((int)(alpha * 255), 255, 255, 255);
		GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

		((IGuiRenderStateAccess) graphics).getGuiRenderState().addGuiElement(new BlitRenderState(
			RenderPipelines.GUI_TEXTURED,
			TextureSetup.singleTexture(renderTarget.getColorTextureView(), sampler),
			new Matrix3x2f(graphics.pose()),
			x, y, x + 16, y + 16,
			0f, 1f,
			1f, 0f,
			color,
			null
		));
	}

	private boolean tryEntityRenderers(ItemStack stack, PoseStack poseStack, Quaternionf rotation)
	{
		boolean renderedEntity = false;

		// Renderer for armor items.
		if (ItemUtil.getEquipmentSlot(stack).isArmor())
		{
			if (renderArmor(stack, poseStack))
			{
				renderedEntity = true;
			}
		}

		// Renderer for items that spawn entities (spawn eggs, boats etc).
		if (!renderedEntity && minecraft.level != null && EntityCollector.itemCreatesEntity(stack, Entity.class, minecraft.level.registryAccess()))
		{
			if (updateEntity(stack))
			{
				poseStack.pushPose();

				poseStack.mulPose(Axis.XP.rotationDegrees(30.0f));
				poseStack.mulPose(Axis.YP.rotationDegrees(225.0f));

				float entityWidth  = cachedSpawnEntity.getBbWidth();
				float entityHeight = cachedSpawnEntity.getBbHeight();
				float maxDimension = Math.max(entityWidth, entityHeight);
				float scale = (maxDimension != 0) ? 0.5f / maxDimension : 1;

				poseStack.scale(scale, scale, scale);
				poseStack.translate(0, -entityHeight / 2.0f, 0);

				renderEntityModel(cachedSpawnEntity, poseStack, LightCoordsUtil.FULL_BRIGHT);
				poseStack.popPose();
				renderedEntity = true;
			}
		}

		// Renderer for block items.
		if (!renderedEntity && stack.getItem() instanceof BlockItem blockItem)
		{
			poseStack.pushPose();

			poseStack.mulPose(Axis.XP.rotationDegrees(30.0f));
			poseStack.mulPose(Axis.YP.rotationDegrees(225.0f));

			BlockState blockState = blockItem.getBlock().defaultBlockState();

			if (blockState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
			{
				// Double-blocks logic.
				poseStack.scale(0.4f, 0.4f, 0.4f);
				poseStack.translate(-0.5f, -1f, -0.5f);

				BlockState bottomState = blockState.setValue(
					BlockStateProperties.DOUBLE_BLOCK_HALF,
					DoubleBlockHalf.LOWER);
				renderBlockState(bottomState, poseStack, submitNodeStorage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

				poseStack.pushPose();
				poseStack.translate(0.0f, 1.0f, 0.0f);
				BlockState topState = blockState.setValue(
					BlockStateProperties.DOUBLE_BLOCK_HALF,
					DoubleBlockHalf.UPPER);
				renderBlockState(topState, poseStack, submitNodeStorage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
				poseStack.popPose();
			}
			else
			{
				// Normal blocks logic.
				poseStack.scale(0.5f, 0.5f, 0.5f);
				poseStack.translate(-0.5f, -0.5f, -0.5f);
				renderBlockState(blockState, poseStack, submitNodeStorage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			}

			// Renderer for block entities.
			if (blockItem.getBlock() instanceof EntityBlock entityBlock)
			{
				renderBlockEntity(stack, poseStack, entityBlock, blockState);
			}

			poseStack.popPose();
			renderedEntity = true;
		}

		return renderedEntity;
	}

	private void renderBlockState(BlockState blockState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords)
	{
		blockRenderState.clear();
		BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(blockState);
		List<BlockStateModelPart> parts = blockRenderState.setupModel(new Matrix4f(), model.hasMaterialFlag(1));
		model.collectParts(blockRenderState.scratchRandomSource(1), parts);
		blockRenderState.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, 0);
	}

	private boolean renderArmor(ItemStack stack, PoseStack poseStack)
	{
		if (horseArmor.contains(stack.getItem()) && updateHorseArmor(stack))
		{
			poseStack.pushPose();
			poseStack.scale(0.28f, 0.28f, 0.28f);
			poseStack.translate(0, -1.2f, 0);
			renderEntityModel(horse, poseStack, LightCoordsUtil.FULL_BRIGHT);
			poseStack.popPose();
			return true;
		}
		else if (stack.getItem() == Items.WOLF_ARMOR && updateWolfArmor(stack))
		{
			poseStack.pushPose();
			poseStack.scale(0.65f, 0.65f, 0.65f);
			poseStack.translate(0, -0.5f, 0);
			renderEntityModel(wolf, poseStack, LightCoordsUtil.FULL_BRIGHT);
			poseStack.popPose();
			return true;
		}
		else if (updateArmorStand(stack))
		{
			poseStack.pushPose();

			float scale = 0.5f;
			float yOffset = -1.0f;
			switch (ItemUtil.getEquipmentSlot(stack))
			{
				case HEAD:
					scale = 0.85f;
					yOffset = -1.75f;
					break;
				case CHEST:
					scale = 0.65f;
					yOffset = -1.15f;
					break;
				case LEGS:
					scale = 0.7f;
					yOffset = -0.7f;
					break;
				case FEET:
					scale = 0.85f;
					yOffset = -0.2f;
					break;
				default: break;
			}

			poseStack.scale(scale, scale, scale);
			poseStack.translate(0, yOffset, 0);
			poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));

			renderEntityModel(armorStand, poseStack, LightCoordsUtil.FULL_BRIGHT);
			poseStack.popPose();
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T extends Entity, S extends EntityRenderState> void renderEntityModel(T entity, PoseStack poseStack, int packedLight)
	{
		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		EntityRenderer<T, S> renderer = (EntityRenderer<T, S>) dispatcher.getRenderer(entity);
		if (renderer == null)
		{
			return;
		}

		S state = renderer.createRenderState(entity, 1.0f);

		state.lightCoords = packedLight;

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));

		try
		{
			renderer.submit(state, poseStack, submitNodeStorage, null);
		} catch (Exception e) {}

		poseStack.popPose();
	}

	@SuppressWarnings("unchecked")
	private <T extends BlockEntity, S extends BlockEntityRenderState> boolean renderBlockEntity(ItemStack itemStack, PoseStack poseStack, EntityBlock entityBlock, BlockState blockState)
	{
		try
		{
			BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, blockState);
			if (blockEntity != null)
			{
				blockEntity.applyComponentsFromItemStack(itemStack);

				BlockEntityRenderer<T, S> renderer = (BlockEntityRenderer<T, S>) minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);

				if (renderer != null)
				{
					S state = renderer.createRenderState();

					float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaTicks();
					renderer.extractRenderState((T) blockEntity, state, partialTicks, net.minecraft.world.phys.Vec3.ZERO, null);

					renderer.submit(state, poseStack, submitNodeStorage, null);

					return true;
				}
			}
		}
		catch (Exception e) {}
		return false;
	}

	private Entity getEntityFromItem(ItemStack itemStack)
	{
		Entity collectedEntity = null;
		List<Entity> collectedEntities = minecraft.level == null ? List.of() : EntityCollector.collectEntitiesFromItem(itemStack, minecraft.level.registryAccess());
		if (!collectedEntities.isEmpty())
		{
			collectedEntity = collectedEntities.get(0);
		}
		return collectedEntity;
	}

	private boolean updateEntity(ItemStack itemStack)
	{
		Pair<Item, DataComponentMap> entityItem = Pair.of(itemStack.getItem(), itemStack.getComponents());
		if (cachedSpawnEntity == null || cachedEntityItem == null || !cachedEntityItem.equals(entityItem))
		{
			cachedSpawnEntity = getEntityFromItem(itemStack);
			cachedEntityItem = entityItem;
		}
		return cachedSpawnEntity != null;
	}

	private boolean updateArmorStand(ItemStack itemStack)
	{
		EquipmentSlot equipmentSlot = ItemUtil.getEquipmentSlot(itemStack);
		if (!equipmentSlot.isArmor())
		{
			return false;
		}

		if (armorStand == null && minecraft.level != null)
		{
			armorStand = EntityTypes.ARMOR_STAND.create(minecraft.level, EntitySpawnReason.COMMAND);
			if (armorStand != null)
			{
				armorStand.setInvisible(true);
			}
		}

		if (armorStand == null)
		{
			return false;
		}

		Pair<Item, DataComponentMap> currentItem = Pair.of(itemStack.getItem(), itemStack.getComponents());
		if (!currentItem.equals(cachedArmorStandItem))
		{
			for (EquipmentSlot slot : EquipmentSlot.values())
			{
				armorStand.setItemSlot(slot, ItemStack.EMPTY);
			}
			armorStand.setItemSlot(equipmentSlot, itemStack);
			cachedArmorStandItem = currentItem;
		}
		return true;
	}

	private boolean updateHorseArmor(ItemStack horseArmorItem)
	{
		if (horse == null && minecraft.level != null)
		{
			horse = EntityTypes.HORSE.create(minecraft.level, EntitySpawnReason.COMMAND);
			if (horse != null)
			{
				horse.setInvisible(true);
			}
		}

		if (horse == null)
		{
			return false;
		}

		Pair<Item, DataComponentMap> currentItem = Pair.of(horseArmorItem.getItem(), horseArmorItem.getComponents());
		if (!currentItem.equals(cachedHorseArmorItem))
		{
			horse.setItemSlot(EquipmentSlot.BODY, horseArmorItem);
			cachedHorseArmorItem = currentItem;
		}
		return true;
	}

	private boolean updateWolfArmor(ItemStack wolfArmorItem)
	{
		if (wolf == null && minecraft.level != null)
		{
			wolf = EntityTypes.WOLF.create(minecraft.level, EntitySpawnReason.COMMAND);
			if (wolf != null)
			{
				wolf.setInvisible(true);
			}
		}

		if (wolf == null)
		{
			return false;
		}

		Pair<Item, DataComponentMap> currentItem = Pair.of(wolfArmorItem.getItem(), wolfArmorItem.getComponents());
		if (!currentItem.equals(cachedWolfArmorItem))
		{
			wolf.setItemSlot(EquipmentSlot.BODY, wolfArmorItem);
			cachedWolfArmorItem = currentItem;
		}
		return true;
	}

	public void close()
	{
		if (isClosed)
		{
			return;
		}
		isClosed = true;

		if (renderTarget != null)
		{
			renderTarget.destroyBuffers();
			renderTarget = null;
		}
		projectionMatrixBuffer.close();
	}
}