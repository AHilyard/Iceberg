package com.anthonyhilyard.iceberg.renderer;

import com.anthonyhilyard.iceberg.util.IGuiRenderStateAccess;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class CustomItemRenderer {
    private final Minecraft minecraft;
    private RenderTarget renderTarget;
    private final PerspectiveProjectionMatrixBuffer projectionMatrixBuffer;
    private boolean isClosed = false;

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

        // Lighting for flat items
        boolean is3D = itemState.usesBlockLight();
        if (!is3D) {
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        }

        SubmitNodeStorage submitNodeStorage = minecraft.gameRenderer.getSubmitNodeStorage();
        itemState.submit(poseStack, submitNodeStorage, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

        minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        minecraft.renderBuffers().bufferSource().endBatch();

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