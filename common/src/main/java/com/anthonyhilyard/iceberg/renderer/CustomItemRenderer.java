package com.anthonyhilyard.iceberg.renderer;

import com.anthonyhilyard.iceberg.util.IGuiRenderStateAccess;
import com.anthonyhilyard.iceberg.util.ItemUtil;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class CustomItemRenderer {
    private final Minecraft minecraft;
    private RenderTarget renderTarget;
    private final PerspectiveProjectionMatrixBuffer projectionMatrixBuffer;
    private boolean isClosed = false;
    public boolean render3DArmor = false;

    private static ArmorStand armorStand = null;
    private static Wolf wolf = null;
    private static Horse horse = null;
    private static Pair<Item, DataComponentMap> cachedArmorStandItem = null;
    private static Pair<Item, DataComponentMap> cachedHorseArmorItem = null;
    private static Pair<Item, DataComponentMap> cachedWolfArmorItem = null;

    public CustomItemRenderer(Minecraft mc) {
        this.minecraft = mc;
        this.projectionMatrixBuffer = new PerspectiveProjectionMatrixBuffer("iceberg_custom_item");
    }

    public void renderDetailModelIntoGUI(ItemStack stack, int x, int y, Quaternionf rotation, GuiGraphics graphics) {
        drawAndBlit(graphics, stack, x, y, 1.0f, rotation);
    }

    public void renderItemModelIntoGUIWithAlpha(GuiGraphics graphics, ItemStack stack, int x, int y, float alpha) {
        drawAndBlit(graphics, stack, x, y, alpha, null);
    }

    public void renderItemIntoGUI(GuiGraphics graphics, ItemStack stack, int x, int y, float alpha, Quaternionf rotation) {
        drawAndBlit(graphics, stack, x, y, alpha, rotation);
    }

    private static final List<Item> horseArmor = List.of(Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR);
    private void drawAndBlit(GuiGraphics graphics, ItemStack stack, int x, int y, float alpha, Quaternionf rotation) {
        if (isClosed || stack.isEmpty()) return;

        TrackingItemStackRenderState itemState = new TrackingItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(
                itemState, stack, ItemDisplayContext.GUI,
                minecraft.level, minecraft.player, 0
        );
        if (itemState.isEmpty()) return;

        int fboSize = 96;
        if (renderTarget == null || renderTarget.width != fboSize) {
            if (renderTarget != null) {
                renderTarget.destroyBuffers();
            }
            renderTarget = new TextureTarget("Iceberg Item Renderer", fboSize, fboSize, true);
        }

        try (RenderPass clearPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Item",
                renderTarget.getColorTextureView(), OptionalInt.of(0x00000000), // Transparent BLACK outline (0x00000000)
                renderTarget.getDepthTextureView(), OptionalDouble.of(1.0)
        )) {}

        RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();

        Matrix4f ortho = new Matrix4f().setOrtho(0f, 16f, 16f, 0f, 1000f, -1000f);
        RenderSystem.setProjectionMatrix(
                this.projectionMatrixBuffer.getBuffer(ortho),
                ProjectionType.ORTHOGRAPHIC
        );

        PoseStack poseStack = new PoseStack();
        poseStack.translate(8.0, 8.0, 150.0);
        poseStack.scale(16.0f, -16.0f, 16.0f);
        if (rotation != null) {
            poseStack.mulPose(rotation);
        }

        boolean isSpinning = rotation != null;
        boolean is3D = itemState.usesBlockLight() || isSpinning;
        boolean renderedEntity = false;

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        if (ItemUtil.getEquipmentSlot(stack).isArmor() && render3DArmor) {
            if (horseArmor.contains(stack.getItem()) && updateHorseArmor(stack)) {
                poseStack.pushPose();
                poseStack.scale(0.45f, 0.45f, 0.45f);
                poseStack.translate(0, -0.8f, 0);
                renderEntityModel(horse, poseStack, LightTexture.FULL_BRIGHT);
                poseStack.popPose();
                renderedEntity = true;
                is3D = true;
            } else if (stack.getItem() == Items.WOLF_ARMOR && updateWolfArmor(stack)) {
                poseStack.pushPose();
                poseStack.scale(0.7f, 0.7f, 0.7f);
                poseStack.translate(0, -0.4f, 0);
                renderEntityModel(wolf, poseStack, LightTexture.FULL_BRIGHT);
                poseStack.popPose();
                renderedEntity = true;
                is3D = true;
            }
            else if (updateArmorStand(stack)) {
                poseStack.pushPose();

                float scale = 0.5f;
                float yOffset = -1.0f;
                switch (ItemUtil.getEquipmentSlot(stack)) {
                    case HEAD: scale = 0.85f; yOffset = -1.75f; break;
                    case CHEST: scale = 0.65f; yOffset = -1.15f; break;
                    case LEGS: scale = 0.7f; yOffset = -0.7f; break;
                    case FEET: scale = 0.85f; yOffset = -0.2f; break;
                }

                poseStack.scale(scale, scale, scale);
                poseStack.translate(0, yOffset, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));

                renderEntityModel(armorStand, poseStack, LightTexture.FULL_BRIGHT);
                poseStack.popPose();
                renderedEntity = true;
                is3D = true;
            }
        }
        else if (rotation != null) {
            poseStack.scale(0.80f, 0.80f, 0.80f);
        }

        if (!is3D) {
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }

        if (!renderedEntity) {
            SubmitNodeStorage submitNodeStorage = minecraft.gameRenderer.getSubmitNodeStorage();
            itemState.submit(poseStack, submitNodeStorage, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        }

        minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        bufferSource.endBatch();

        if (!is3D) {
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }
        modelViewStack.popMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;

        int color = ARGB.color((int)(alpha * 255), 255, 255, 255);
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        ((IGuiRenderStateAccess) graphics).getRenderState().submitGuiElement(new BlitRenderState(
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

    private <T extends Entity, S extends EntityRenderState> void renderEntityModel(T entity, PoseStack poseStack, int packedLight) {
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<T, S> renderer = (EntityRenderer<T, S>) dispatcher.getRenderer(entity);
        if (renderer == null) return;

        S state = renderer.createRenderState(entity, 1.0f);

        state.lightCoords = packedLight;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));

        try {
            SubmitNodeCollector submitNodeCollector = minecraft.gameRenderer.getSubmitNodeStorage();
            renderer.submit(state, poseStack, submitNodeCollector, null);
        } catch (Exception e) {}

        poseStack.popPose();
    }

    private boolean updateArmorStand(ItemStack itemStack) {
        EquipmentSlot equipmentSlot = ItemUtil.getEquipmentSlot(itemStack);
        if (!equipmentSlot.isArmor()) return false;

        if (armorStand == null && minecraft.level != null) {
            armorStand = EntityType.ARMOR_STAND.create(minecraft.level, EntitySpawnReason.COMMAND);
            if (armorStand != null) armorStand.setInvisible(true);
        }
        if (armorStand == null) return false;

        Pair<Item, DataComponentMap> currentItem = Pair.of(itemStack.getItem(), itemStack.getComponents());
        if (!currentItem.equals(cachedArmorStandItem)) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                armorStand.setItemSlot(slot, ItemStack.EMPTY);
            }
            armorStand.setItemSlot(equipmentSlot, itemStack);
            cachedArmorStandItem = currentItem;
        }
        return true;
    }

    private boolean updateHorseArmor(ItemStack horseArmorItem) {
        if (horse == null && minecraft.level != null) {
            horse = EntityType.HORSE.create(minecraft.level, EntitySpawnReason.COMMAND);
            if (horse != null) horse.setInvisible(true);
        }
        if (horse == null) return false;

        Pair<Item, DataComponentMap> currentItem = Pair.of(horseArmorItem.getItem(), horseArmorItem.getComponents());
        if (!currentItem.equals(cachedHorseArmorItem)) {
            horse.setBodyArmorItem(horseArmorItem);
            cachedHorseArmorItem = currentItem;
        }
        return true;
    }

    private boolean updateWolfArmor(ItemStack wolfArmorItem) {
        if (wolf == null && minecraft.level != null) {
            wolf = EntityType.WOLF.create(minecraft.level, EntitySpawnReason.COMMAND);
            if (wolf != null) wolf.setInvisible(true);
        }
        if (wolf == null) return false;

        Pair<Item, DataComponentMap> currentItem = Pair.of(wolfArmorItem.getItem(), wolfArmorItem.getComponents());
        if (!currentItem.equals(cachedWolfArmorItem)) {
            wolf.setBodyArmorItem(wolfArmorItem);
            cachedWolfArmorItem = currentItem;
        }
        return true;
    }

    public void close() {
        if (isClosed) return;
        isClosed = true;

        if (renderTarget != null) {
            renderTarget.destroyBuffers();
            renderTarget = null;
        }
        projectionMatrixBuffer.close();
    }
}