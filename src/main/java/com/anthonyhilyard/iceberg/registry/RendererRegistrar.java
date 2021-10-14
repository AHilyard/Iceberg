package com.anthonyhilyard.iceberg.registry;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.client.event.EntityRenderersEvent;

public abstract class RendererRegistrar
{
	private static Map<EntityType<? extends Entity>, EntityRendererProvider<?>> entityRenderers = new HashMap<>();

	protected static <T extends Entity> void registerRenderer(String name, EntityRendererProvider<T> rendererProvider)
	{
		if (AutoRegistry.isEntityTypeRegistered(name))
		{
			// Store this renderer provider.
			entityRenderers.put(AutoRegistry.getEntityType(name), rendererProvider);
		}
		else
		{
			throw new RuntimeException("Tried to register a renderer for an unregistered entity type!  Make sure you register renderers after entities.");
		}
	}

	@SuppressWarnings({"unchecked", "unused"})
	private <T extends Entity> void onEntityCreation(EntityRenderersEvent.RegisterRenderers event)
	{
		for (EntityType<? extends Entity> entityType : entityRenderers.keySet())
		{
			event.registerEntityRenderer((EntityType<T>)entityType, (EntityRendererProvider<T>)entityRenderers.get(entityType));
		}
	}
}
