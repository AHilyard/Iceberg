package com.anthonyhilyard.iceberg.neoforge.mixin;

import java.util.List;

import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.services.Services;
import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4fStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin
{
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow(remap = false)
	private ItemStack tooltipStack = ItemStack.EMPTY;

	private int storedTooltipWidth, storedTooltipHeight;
	private Vector2ic storedPostPos;

	private int xChange = 0, yChange = 0;

	@Group(name = "storeLocals", min = 1, max = 1)
	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lorg/joml/Vector2ic;x()I", shift = Shift.BEFORE, remap = false), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void storeLocals(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, RenderTooltipEvent.Pre preEvent, int tooltipWidth, int tooltipHeight, int tooltipWidth2, int tooltipHeight2, Vector2ic postPos)
	{
		storedTooltipWidth = tooltipWidth2;
		storedTooltipHeight = tooltipHeight2;
		storedPostPos = postPos;
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "TAIL"))
	private void renderTooltipInternalTail(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		Screen currentScreen = minecraft.screen;
		ItemStack containerStack = ItemStack.EMPTY;
		if (currentScreen != null && currentScreen instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.getSlotUnderMouse();
			if (hoveredSlot != null)
			{
				containerStack = hoveredSlot.getItem();
			}
		}

		if (containerStack.isEmpty())
		{
			containerStack = tooltipStack;
		}

		if (!containerStack.isEmpty() && !components.isEmpty())
		{
			RenderTooltipEvents.POSTEXT.invoker().onPost(containerStack, self, storedPostPos.x(), storedPostPos.y(), font, storedTooltipWidth, storedTooltipHeight, components, false, 0);
		}

		if (Services.PLATFORM.isModLoaded("andromeda") && minecraft.player != null && tooltipStack == minecraft.player.getMainHandItem())
		{
			Matrix4fStack poseStack = RenderSystem.getModelViewStack();
			poseStack.translate(-xChange, -yChange, 0);
			RenderSystem.applyModelViewMatrix();
		}

		tooltipStack = ItemStack.EMPTY;
		xChange = 0;
		yChange = 0;
	}
}
