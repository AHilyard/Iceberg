package com.anthonyhilyard.iceberg.fabric.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.PreExtResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.mixin.AbstractContainerScreenAccessor;
import com.anthonyhilyard.iceberg.util.INestedTooltipAccess;
import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin implements ITooltipAccess
{

	@Shadow @Final private Minecraft minecraft;

	@Unique private static ItemStack icebergTooltipStack = ItemStack.EMPTY;

	@Unique private int renderTooltipDepth = 0;
	@Unique private int xChange = 0;
	@Unique private int yChange = 0;

	@Override
	public void setIcebergTooltipStack(ItemStack stack)
	{
		icebergTooltipStack = stack != null ? stack : ItemStack.EMPTY;
	}

	@ModifyVariable(method = "tooltip", at = @At("HEAD"), argsOnly = true)
	private List<ClientTooltipComponent> makeComponentsMutable(List<ClientTooltipComponent> components)
	{
		return new ArrayList<>(components);
	}

	@Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
	protected void captureTooltipStack(Font font, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		icebergTooltipStack = itemStack;
	}

	@Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V", at = @At("HEAD"))
	public void clearTooltipStack(Font font, Component component, int x, int y, CallbackInfo info)
	{
		icebergTooltipStack = ItemStack.EMPTY;
	}

	@Inject(method = "setTooltipForNextFrameInternal", at = @At("HEAD"))
	public void modifyGatheredComponents(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, boolean bl, CallbackInfo info)
	{
		Screen currentScreen = minecraft.gui.screen();
		if (currentScreen instanceof AbstractContainerScreen<?> containerScreen && ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot() != null)
		{
			if (icebergTooltipStack.isEmpty()) icebergTooltipStack = ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot().getItem();
		}
	}

	@Inject(method = "tooltip", at = @At("HEAD"), cancellable = true)
	private void preRenderTooltip(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, CallbackInfo info)
	{
		this.renderTooltipDepth++;
		GuiGraphicsExtractor self = (GuiGraphicsExtractor)(Object)this;

		ItemStack containerStack = ItemStack.EMPTY;
		ItemStack nestedTooltipStack = ((INestedTooltipAccess) self).getIcebergNestedTooltipStack();

		// Use the nested stack if present, else use the current hovered item.
		if (!nestedTooltipStack.isEmpty())
		{
			containerStack = nestedTooltipStack;
		}
		else if (this.renderTooltipDepth == 1)
		{
			if (minecraft.gui.screen() instanceof AbstractContainerScreen<?> containerScreen && ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot() != null)
			{
				containerStack = ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot().getItem();
			}
			if (containerStack.isEmpty()) containerStack = icebergTooltipStack;
		}

		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();

		if (minecraft.gui.screen() != null)
		{
			width = minecraft.gui.screen().width;
			height = minecraft.gui.screen().height;
		}

		if (!containerStack.isEmpty())
		{
			// GATHER EVENT
			Item.TooltipContext context = Item.TooltipContext.of(minecraft.level);
			TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;

			List<Component> textComponents = containerStack.getTooltipLines(context, minecraft.player, flag);
			Optional<TooltipComponent> itemComponent = containerStack.getTooltipImage();

			List<ClientTooltipComponent> newComponents = Tooltips.gatherTooltipComponents(containerStack, textComponents, itemComponent, x, width, height, null, font, -1);

			if (newComponents != null && !newComponents.isEmpty())
			{
				components.clear();
				components.addAll(newComponents);
			}

			// COLOREXT EVENT
			int backgroundStart = Tooltips.DEFAULT_COLORS.backgroundColorStart().getValue();
			int backgroundEnd = Tooltips.DEFAULT_COLORS.backgroundColorEnd().getValue();
			int borderStart = Tooltips.DEFAULT_COLORS.borderColorStart().getValue();
			int borderEnd = Tooltips.DEFAULT_COLORS.borderColorEnd().getValue();

			ColorExtResult colorResult = RenderTooltipEvents.COLOREXT.invoker().onColor(containerStack, self, x, y, font, backgroundStart, backgroundEnd, borderStart, borderEnd, components, false, 0, resource, false, false);

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

			// PREEXT EVENT
			PreExtResult preResult = RenderTooltipEvents.PREEXT.invoker().onPre(containerStack, self, x, y, width, height, font, components, positioner, false, 0);
			if (preResult.result() != InteractionResult.PASS)
			{
				this.renderTooltipDepth--;
				if (this.renderTooltipDepth == 0)
				{
					icebergTooltipStack = ItemStack.EMPTY;
				}
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

	@Inject(method = "tooltip", at = @At("TAIL"))
	private void postRenderTooltip(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resource, CallbackInfo info)
	{
		GuiGraphicsExtractor self = (GuiGraphicsExtractor)(Object)this;
		ItemStack containerStack = ItemStack.EMPTY;
		ItemStack nestedTooltipStack = ((INestedTooltipAccess) self).getIcebergNestedTooltipStack();

		if (!nestedTooltipStack.isEmpty())
		{
			containerStack = nestedTooltipStack;
		}
		else if (this.renderTooltipDepth == 1 && minecraft.gui.screen() instanceof AbstractContainerScreen<?> containerScreen && ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot() != null)
		{
			containerStack = ((AbstractContainerScreenAccessor)containerScreen).getHoveredSlot().getItem();
		}

		if (containerStack.isEmpty() && this.renderTooltipDepth == 1)
		{
			containerStack = icebergTooltipStack;
		}

		if (!containerStack.isEmpty() && !components.isEmpty())
		{
			int tooltipWidth = 0;
			int tooltipHeight = components.size() == 1 ? -2 : 0;
			for (ClientTooltipComponent c : components)
			{
				int w = c.getWidth(font);
				if (w > tooltipWidth) tooltipWidth = w;
				tooltipHeight += c.getHeight(font);
			}

			Vector2ic pos = positioner.positionTooltip(self.guiWidth(), self.guiHeight(), x, y, tooltipWidth, tooltipHeight);

			RenderTooltipEvents.POSTEXT.invoker().onPost(containerStack, self, pos.x(), pos.y(), font, tooltipWidth, tooltipHeight, components, false, 0);
		}

		this.renderTooltipDepth--;
		if (this.renderTooltipDepth == 0)
		{
			icebergTooltipStack = ItemStack.EMPTY;
			xChange = 0;
			yChange = 0;
		}
	}
}