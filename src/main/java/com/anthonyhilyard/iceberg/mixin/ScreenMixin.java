package com.anthonyhilyard.iceberg.mixin;

import java.util.ArrayList;
import java.util.List;

import com.anthonyhilyard.iceberg.util.StringRecomposer;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;

@Mixin(Screen.class)
public class ScreenMixin extends AbstractContainerEventHandler
{
	@Shadow(remap = false)
	private Font tooltipFont = null;

	@Shadow(remap = false)
	private ItemStack tooltipStack = ItemStack.EMPTY;

	@Final
	@Shadow
	private final List<GuiEventListener> children = Lists.newArrayList();

	@SuppressWarnings("removal")
	@Inject(method = "renderTooltipInternal",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;blitOffset:F", ordinal = 2, shift = Shift.AFTER),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void renderTooltipInternal(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, CallbackInfo info, RenderTooltipEvent.Pre pre, int tooltipWidth, int tooltipHeight, int postX, int postY)
	{
		if (!components.isEmpty())
		{
			List<FormattedText> standinLines = new ArrayList<>();
			for (ClientTooltipComponent component : components)
			{
				if (component instanceof ClientTextTooltip)
				{
					StringRecomposer recomposer = new StringRecomposer();
					((ClientTextTooltip)component).text.accept(recomposer);
					standinLines.add(recomposer.getFormattedText());
				}
			}

			MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostText(tooltipStack, standinLines, poseStack, postX, postY, tooltipFont, tooltipWidth, tooltipHeight));
		}
	}

	@Override
	public List<? extends GuiEventListener> children()
	{
		return children;
	}
}
