package com.anthonyhilyard.iceberg.mixin;

import com.anthonyhilyard.iceberg.util.IGuiRenderStateAccess;

import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements IGuiRenderStateAccess
{

	@Shadow
	private GuiRenderState guiRenderState;

	@Override
	public GuiRenderState getRenderState()
	{
		return this.guiRenderState;
	}
}