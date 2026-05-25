package com.anthonyhilyard.iceberg.mixin;

import com.anthonyhilyard.iceberg.util.IGuiRenderStateAccess;

import com.anthonyhilyard.iceberg.util.INestedTooltipAccess;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements IGuiRenderStateAccess, INestedTooltipAccess
{

	@Shadow
	private GuiRenderState guiRenderState;

	@Override
	public GuiRenderState getRenderState()
	{
		return this.guiRenderState;
	}

	@Unique
	private static ItemStack icebergNestedTooltipStack = ItemStack.EMPTY;

	@Override
	public void setIcebergNestedTooltipStack(ItemStack stack)
	{
		icebergNestedTooltipStack = stack != null ? stack : ItemStack.EMPTY;
	}

	@Override
	public ItemStack getIcebergNestedTooltipStack()
	{
		return icebergNestedTooltipStack;
	}
}