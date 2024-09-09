package com.anthonyhilyard.iceberg.fabric.mixin;

import java.util.List;
import java.util.Optional;

import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.PreExtResult;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.Tooltips;
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
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin
{
	@Shadow
	@Final
	private Minecraft minecraft;

	private static ItemStack icebergTooltipStack = ItemStack.EMPTY;

	private int storedTooltipWidth, storedTooltipHeight;
	private Vector2ic storedPostPos;

	@Shadow
	private void renderTooltipInternal(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner) {}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "HEAD"))
	protected void renderTooltipHead(Font font, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		icebergTooltipStack = itemStack;
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "TAIL"))
	protected void renderTooltipTail(Font font, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		icebergTooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
			shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	public void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> itemComponent, int x, int y, CallbackInfo info, List<ClientTooltipComponent> components)
	{
		Screen currentScreen = minecraft.screen;

		if (currentScreen != null && currentScreen instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;

			// If the tooltip stack is empty, try to get the stack from the slot under the mouse.
			// This is needed for the creative inventory screen, which doesn't set the tooltip stack.
			if (icebergTooltipStack.isEmpty() && hoveredSlot != null)
			{
				icebergTooltipStack = hoveredSlot.getItem();
			}
		}

		int screenWidth = currentScreen != null ? currentScreen.width : minecraft.getWindow().getGuiScaledWidth();
		int screenHeight = currentScreen != null ? currentScreen.height : minecraft.getWindow().getGuiScaledHeight();

		List<ClientTooltipComponent> newComponents = Tooltips.gatherTooltipComponents(icebergTooltipStack, textComponents, itemComponent, x, screenWidth, screenHeight, null, font, -1);
		if (newComponents != null && !newComponents.isEmpty())
		{
			components.clear();
			components.addAll(newComponents);
		}
	}


	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V", at = @At(value = "HEAD"))
	public void renderTooltipTail(Font font, Component components, int x, int y, CallbackInfo info)
	{
		// Not an item tooltip, so clear the stack.
		icebergTooltipStack = ItemStack.EMPTY;
	}

	private int xChange = 0, yChange = 0;

	@Inject(method = "renderTooltipInternal", at = @At(value = "HEAD"), cancellable = true)
	private void preRenderTooltipInternal(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		Screen currentScreen = minecraft.screen;

		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();
		
		if (currentScreen != null)
		{
			width = currentScreen.width;
			height = currentScreen.height;
		}
		else
		{
			if (Services.getPlatformHelper().isModLoaded("andromeda") && icebergTooltipStack != null && !icebergTooltipStack.isEmpty())
			{
				List<ClientTooltipComponent> newComponents = Tooltips.gatherTooltipComponents(icebergTooltipStack, Screen.getTooltipFromItem(minecraft, icebergTooltipStack), icebergTooltipStack.getTooltipImage(), x, width, height, null, font, -1);
				if (newComponents != null && !newComponents.isEmpty())
				{
					int oldWidth = 0, oldHeight = 0;

					for (ClientTooltipComponent tooltipComponent : components)
					{
						oldHeight += tooltipComponent.getHeight();
						int thisWidth = tooltipComponent.getWidth(font);
						if (thisWidth > oldWidth)
						{
							oldWidth = thisWidth;
						}
					}
					
					components.clear();
					components.addAll(newComponents);
					Rect2i newRect = Tooltips.calculateRect(icebergTooltipStack, self, positioner, components, x, y, width, height, width, font, 0, true);

					if (minecraft.player != null && icebergTooltipStack == minecraft.player.getMainHandItem())
					{
						Matrix4fStack poseStack = RenderSystem.getModelViewStack();
						xChange = (oldWidth - newRect.getWidth()) / 2;
						yChange = oldHeight - newRect.getHeight();

						poseStack.translate(xChange, yChange, 0);
						RenderSystem.applyModelViewMatrix();
					}
				}
			}
		}

		if (!components.isEmpty())
		{
			PreExtResult eventResult = null;
			InteractionResult result = InteractionResult.PASS;

			eventResult = RenderTooltipEvents.PREEXT.invoker().onPre(icebergTooltipStack, self, x, y, width, height, font, components, positioner, false, 0);
			result = eventResult.result();

			if (result != InteractionResult.PASS)
			{
				info.cancel();
			}
		}
	}

	@Inject(method = "renderTooltipInternal",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V",
			ordinal = 0, shift = Shift.BEFORE))
	private void preFillGradient(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		Screen currentScreen = minecraft.screen;
		ItemStack containerStack = ItemStack.EMPTY;
		if (currentScreen != null && currentScreen instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;
			if (hoveredSlot != null)
			{
				containerStack = hoveredSlot.getItem();
			}
		}

		if (containerStack.isEmpty())
		{
			containerStack = icebergTooltipStack;
		}

		if (!containerStack.isEmpty())
		{
			int backgroundStart = TooltipRenderUtil.BACKGROUND_COLOR;
			int backgroundEnd = backgroundStart;
			int borderStart = TooltipRenderUtil.BORDER_COLOR_TOP;
			int borderEnd = TooltipRenderUtil.BORDER_COLOR_BOTTOM;

			// Do colors now, sure why not.
			ColorExtResult result = RenderTooltipEvents.COLOREXT.invoker().onColor(containerStack, self, x, y, font, backgroundStart, backgroundEnd, borderStart, borderEnd, components, false, 0);
			if (result != null)
			{
				backgroundStart = result.backgroundStart();
				backgroundEnd = result.backgroundEnd();
				borderStart = result.borderStart();
				borderEnd = result.borderEnd();
			}

			Tooltips.currentColors = new Tooltips.TooltipColors(TextColor.fromRgb(backgroundStart), TextColor.fromRgb(backgroundEnd), TextColor.fromRgb(borderStart), TextColor.fromRgb(borderEnd));
		}
		else
		{
			Tooltips.currentColors = Tooltips.DEFAULT_COLORS;
		}
	}

	@Group(name = "storeLocals", min = 1, max = 1)
	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lorg/joml/Vector2ic;x()I", shift = Shift.BEFORE, remap = false), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void storeLocals(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, int tooltipWidth, int tooltipHeight, int tooltipWidth2, int tooltipHeight2, Vector2ic postPos)
	{
		storedTooltipWidth = tooltipWidth2;
		storedTooltipHeight = tooltipHeight2;
		storedPostPos = postPos;
	}

	// @Group(name = "storeLocals", min = 1, max = 1)
	// @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lorg/joml/Vector2ic;x()I", shift = Shift.BEFORE, remap = false), locals = LocalCapture.CAPTURE_FAILSOFT)
	// private void storeLocalsOptifine(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info, Object preEvent, int tooltipWidth, int tooltipHeight, int tooltipWidth2, int tooltipHeight2, Vector2ic postPos)
	// {
	// 	storeLocals(font, components, x, y, positioner, info, tooltipWidth, tooltipHeight, tooltipWidth2, tooltipHeight2, postPos);
	// }

	@Inject(method = "renderTooltipInternal", at = @At(value = "TAIL"))
	private void renderTooltipInternalTail(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		Screen currentScreen = minecraft.screen;
		ItemStack containerStack = ItemStack.EMPTY;
		if (currentScreen != null && currentScreen instanceof AbstractContainerScreen<?> containerScreen)
		{
			Slot hoveredSlot = containerScreen.hoveredSlot;
			if (hoveredSlot != null)
			{
				containerStack = hoveredSlot.getItem();
			}
		}

		if (containerStack.isEmpty())
		{
			containerStack = icebergTooltipStack;
		}

		if (!containerStack.isEmpty() && !components.isEmpty())
		{
			RenderTooltipEvents.POSTEXT.invoker().onPost(containerStack, self, storedPostPos.x(), storedPostPos.y(), font, storedTooltipWidth, storedTooltipHeight, components, false, 0);
		}

		if (Services.getPlatformHelper().isModLoaded("andromeda") && minecraft.player != null && icebergTooltipStack == minecraft.player.getMainHandItem())
		{
			Matrix4fStack poseStack = RenderSystem.getModelViewStack();
			poseStack.translate(-xChange, -yChange, 0);
			RenderSystem.applyModelViewMatrix();
		}

		icebergTooltipStack = ItemStack.EMPTY;
		xChange = 0;
		yChange = 0;
	}
}
