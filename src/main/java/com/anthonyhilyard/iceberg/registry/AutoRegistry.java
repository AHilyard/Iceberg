package com.anthonyhilyard.iceberg.registry;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

/**
 * Extend this class to have all registerable fields be automatically registered in Forge.  Easy.
 */
public abstract class AutoRegistry
{
	protected static String MODID = null;

	private static boolean entityCreationRegistered = false;

	private static Map<EntityType<?>, Supplier<AttributeModifierMap.MutableAttribute>> entityAttributes = new HashMap<>();

	public static void init(String ModID)
	{
		MODID = ModID;
	}

	@SuppressWarnings("unchecked")
	protected AutoRegistry()
	{
		try
		{
			// Iterate through every built-in Forge registry...
			for (Field field : ForgeRegistries.class.getDeclaredFields())
			{
				Object fieldObj = field.get(null);
				if (fieldObj instanceof IForgeRegistry)
				{
					// Grab the registry's supertype and add a generic listener for registry events.
					Class<IForgeRegistryEntry<?>> clazz = (Class<IForgeRegistryEntry<?>>)((IForgeRegistry<?>)fieldObj).getRegistrySuperType();
					FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(clazz, (Consumer<RegistryEvent.Register<? extends IForgeRegistryEntry<?>>>)(e) -> registerAllOfType(clazz, e) );
				}
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private final <T extends IForgeRegistryEntry<T>> void registerAllOfType(Class<IForgeRegistryEntry<?>> type, RegistryEvent.Register<T> event)
	{
		try
		{
			// Loop through all fields we've declared and register them.
			for (Field field : this.getClass().getDeclaredFields())
			{
				// Grab the field and check if it is a Forge registry-compatible type.
				Object obj = field.get(this);
				if (type.isAssignableFrom(obj.getClass()))
				{
					// If this is an entity type field and we haven't already registered for the entity creation event, do so now.
					if (obj instanceof EntityType && !entityCreationRegistered)
					{
						FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onEntityCreation);
						entityCreationRegistered = true;
					}

					// If this field has a registry name, register it now.
					T entry = (T)obj;
					if (entry != null && entry.getRegistryName() != null)
					{
						event.getRegistry().register(entry);
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	protected static <T extends Entity> EntityType<T> registerEntity(String name, EntityType.Builder<T> builder, IRenderFactory<? super T> renderFactory)
	{
		return registerEntity(name, builder, renderFactory, null);
	}

	@SuppressWarnings("unchecked")
	protected static <T extends Entity> EntityType<T> registerEntity(String name, EntityType.Builder<T> builder, IRenderFactory<? super T> renderFactory, Supplier<AttributeModifierMap.MutableAttribute> attributes)
	{
		if (MODID == null)
		{
			throw new RuntimeException("AutoRegistry was not initialized with mod id!");
		}

		// Build the entity type.
		ResourceLocation resourceLocation = new ResourceLocation(MODID, name);
		EntityType<T> entityType = (EntityType<T>) builder.build(name).setRegistryName(resourceLocation);

		// Register the rendering handler.
		RenderingRegistry.registerEntityRenderingHandler(entityType, renderFactory);

		// Store mob attributes if provided.  These will be added in the attribute creation event below.
		if (attributes != null)
		{
			entityAttributes.put(entityType, attributes);
		}


		return entityType;
	}

	protected static SoundEvent registerSound(String name)
	{
		if (MODID == null)
		{
			throw new RuntimeException("AutoRegistry was not initialized with mod id!");
		}

		return new SoundEvent(new ResourceLocation(MODID, name));
	}

	@SuppressWarnings("unchecked")
	private void onEntityCreation(EntityAttributeCreationEvent event)
	{
		for (Field field : this.getClass().getDeclaredFields())
		{
			try
			{
				// Grab the field and check if it is a Forge registry-compatible type.
				Object obj = field.get(this);
				if (EntityType.class.isAssignableFrom(obj.getClass()) && entityAttributes.containsKey(obj))
				{
					EntityType<? extends LivingEntity> entityType = (EntityType<? extends LivingEntity>) obj;
					if (entityType != null)
					{
						event.put(entityType, entityAttributes.get(obj).get().build());
					}
				}
			}
			catch (ClassCastException e)
			{
				// The class cast exception likely just means that we tried to convert an EntityType with generic type
				// parameter of something other than a LivingEntity subclass.  This is fine, so continue.
				continue;
			}
			catch (IllegalAccessException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}