package com.anthonyhilyard.iceberg.registry;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public abstract class RendererRegistrar
{
	protected static <T extends Entity> void registerRenderer(String name, IRenderFactory<? super T> renderFactory)
	{
		if (AutoRegistry.isEntityTypeRegistered(name))
		{
			// Register the rendering handler.
			RenderingRegistry.registerEntityRenderingHandler(AutoRegistry.<T>getEntityType(name), renderFactory);
		}
		else
		{
			throw new RuntimeException("Tried to register a renderer for an unregistered entity type!  Make sure you register renderers after entities.");
		}
	}
}
