package com.anthonyhilyard.iceberg.config;

import javax.annotation.Nonnull;

import com.anthonyhilyard.iceberg.Loader;
import com.electronwill.nightconfig.core.Config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public abstract class IcebergConfig<T extends IcebergConfig<?>>
{
	private static IcebergConfigSpec SPEC = null;
	private static IcebergConfig<?> INSTANCE = null;
	private static String modId = null;
	private static boolean registered = false;

	protected abstract <I extends IcebergConfig<?>> void setInstance(I instance);
	protected void onLoad() {}
	protected void onReload() {}

	static
	{
		Config.setInsertionOrderPreserved(true);
	}

	@SubscribeEvent
	protected final static void onLoadEvent(ModConfigEvent.Loading event)
	{
		if (modId != null && INSTANCE != null && event.getConfig().getModId().contentEquals(modId))
		{
			INSTANCE.onLoad();
		}
	}

	@SubscribeEvent
	protected final static void onReloadEvent(ModConfigEvent.Reloading event)
	{
		if (modId != null && INSTANCE != null && event.getConfig().getModId().contentEquals(modId))
		{
			INSTANCE.onReload();
		}
	}

	public static final boolean register(Class<? extends IcebergConfig<?>> superClass, @Nonnull String modId)
	{
		if (registered)
		{
			return false;
		}

		IcebergConfig.modId = modId;

		FMLJavaModLoadingContext.get().getModEventBus().register(superClass);

		Pair<IcebergConfig<?>, IcebergConfigSpec> specPair = new IcebergConfigSpec.Builder().finish((builder) -> 
		{
			IcebergConfig<?> result = null;
			try
			{
				result = (IcebergConfig<?>)superClass.getConstructor(IcebergConfigSpec.Builder.class).newInstance(builder);
			}
			catch (Exception e)
			{
				Loader.LOGGER.warn("Failed to register configuration: {}", e);
			}
			return result;
		});

		if (specPair.getRight() == null || specPair.getLeft() == null)
		{
			return false;
		}

		SPEC = specPair.getRight();
		INSTANCE = specPair.getLeft();
		INSTANCE.setInstance(specPair.getLeft());

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);

		registered = true;
		return true;
	}
}
