package com.anthonyhilyard.iceberg.mixin;

import java.util.ArrayList;
import java.util.List;

import com.anthonyhilyard.iceberg.config.IcebergConfigSpec;
import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;


import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;


import com.mrcrayfish.configured.util.ConfigHelper;


@Mixin(ConfigHelper.class)
public class ConfiguredConfigHelperMixin
{
	@Inject(method = "gatherAllConfigValues(Lnet/minecraftforge/fml/config/ModConfig;)Ljava/util/List;",
			at = @At(value = "HEAD"), cancellable = true, remap = false, require = 0)
	private static void gatherAllConfigValuesIcebergSupport(ModConfig config, CallbackInfoReturnable<List<?>> info)
	{
		if (config.getSpec() instanceof IcebergConfigSpec icebergConfigSpec)
		{
			List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values = new ArrayList<>();
			gatherValuesFromIcebergConfig(icebergConfigSpec.getValues(), icebergConfigSpec, values);
			info.setReturnValue(values);
		}
	}

	private static void gatherValuesFromIcebergConfig(UnmodifiableConfig config, IcebergConfigSpec spec, List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values)
	{
		config.valueMap().forEach((s, o) ->
		{
			if (o instanceof AbstractConfig)
			{
				gatherValuesFromIcebergConfig((UnmodifiableConfig) o, spec, values);
			}
			else if (o instanceof ForgeConfigSpec.ConfigValue<?>)
			{
				ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) o;
				ForgeConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
				values.add(Pair.of(configValue, valueSpec));
			}
		});
	}
}