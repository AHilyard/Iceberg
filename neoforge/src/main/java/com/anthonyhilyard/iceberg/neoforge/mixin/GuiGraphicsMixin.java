package com.anthonyhilyard.iceberg.neoforge.mixin;

import java.util.ArrayList;
import java.util.List;

import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.PreExtResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.util.INestedTooltipAccess;
import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.network.chat.TextColor;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin
{

	@Shadow @Final private Minecraft minecraft;
	@Shadow public abstract int guiWidth();
	@Shadow public abstract int guiHeight();

	@ModifyVariable(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), argsOnly = true)
	private List<ClientTooltipComponent> makeComponentsMutableForge(List<ClientTooltipComponent> components)
	{
		return new ArrayList<>(components);
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
	private void preRenderTooltipForge(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, ItemStack itemStack, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;

		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();

		if (minecraft.screen != null)
		{
			width = minecraft.screen.width;
			height = minecraft.screen.height;
		}

		if (itemStack != null && itemStack.isEmpty())
		{
			// Possible nested item.
			itemStack = ((INestedTooltipAccess) self).getIcebergNestedTooltipStack();
		}

		if (itemStack != null && !itemStack.isEmpty())
		{
			int backgroundStart = Tooltips.DEFAULT_COLORS.backgroundColorStart().getValue();
			int backgroundEnd = Tooltips.DEFAULT_COLORS.backgroundColorEnd().getValue();
			int borderStart = Tooltips.DEFAULT_COLORS.borderColorStart().getValue();
			int borderEnd = Tooltips.DEFAULT_COLORS.borderColorEnd().getValue();

			ColorExtResult colorResult = RenderTooltipEvents.COLOREXT.invoker().onColor(itemStack, self, x, y, font, backgroundStart, backgroundEnd, borderStart, borderEnd, components, false, 0, resource, false, false);

			if (colorResult != null)
			{
				Tooltips.currentColors = new Tooltips.TooltipColors(TextColor.fromRgb(colorResult.backgroundStart()), TextColor.fromRgb(colorResult.backgroundEnd()), TextColor.fromRgb(colorResult.borderStart()), TextColor.fromRgb(colorResult.borderEnd()));
				Tooltips.gradientBackground = colorResult.gradientBackground();
				Tooltips.gradientBorder = colorResult.gradientBorder();
			}
			else
			{
				Tooltips.currentColors = Tooltips.DEFAULT_COLORS;
				Tooltips.gradientBackground = false;
				Tooltips.gradientBorder = false;
			}

			PreExtResult preResult = RenderTooltipEvents.PREEXT.invoker().onPre(itemStack, self, x, y, width, height, font, components, positioner, false, 0);
			if (preResult.result() != InteractionResult.PASS)
			{
				info.cancel();
			}
		}
		else
		{
			Tooltips.currentColors = Tooltips.DEFAULT_COLORS;
			Tooltips.gradientBackground = false;
			Tooltips.gradientBorder = false;
		}
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
	private void postRenderTooltipForge(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, ItemStack itemStack, CallbackInfo info)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;

		if (itemStack != null && itemStack.isEmpty())
		{
			// Possible nested item.
			itemStack = ((INestedTooltipAccess) self).getIcebergNestedTooltipStack();
		}

		if (itemStack != null && !itemStack.isEmpty() && !components.isEmpty())
		{
			int tooltipWidth = 0;
			int tooltipHeight = components.size() == 1 ? -2 : 0;
			for (ClientTooltipComponent c : components)
			{
				int w = c.getWidth(font);
				if (w > tooltipWidth) tooltipWidth = w;
				tooltipHeight += c.getHeight(font);
			}

			Vector2ic pos = positioner.positionTooltip(this.guiWidth(), this.guiHeight(), x, y, tooltipWidth, tooltipHeight);
			RenderTooltipEvents.POSTEXT.invoker().onPost(itemStack, self, pos.x(), pos.y(), font, tooltipWidth, tooltipHeight, components, false, 0);
		}
	}
}