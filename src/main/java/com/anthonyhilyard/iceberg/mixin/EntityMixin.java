package com.anthonyhilyard.iceberg.mixin;

import java.util.Objects;

import com.anthonyhilyard.iceberg.events.EntityFluidEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
import net.minecraftforge.common.MinecraftForge;

@Mixin(Entity.class)
public class EntityMixin extends net.minecraftforge.common.capabilities.CapabilityProvider<Entity>
{
	private Fluid previousFluidOnEyes = null;

	@Shadow
	protected ITag<Fluid> fluidOnEyes;

	protected EntityMixin(Class<Entity> baseClass) { super(baseClass); }

	@Inject(method = "updateFluidOnEyes", at = @At(value = "RETURN"))
	public void onUpdateFluidOnEyes(CallbackInfo callbackInfo)
	{
		if (fluidOnEyes != null && fluidOnEyes.getValues().size() > 0)
		{
			previousFluidOnEyes = fluidOnEyes.getValues().get(0);
		}
		else if (previousFluidOnEyes != null)
		{
			// We were submerged in a fluid that we no longer are.
			if (previousFluidOnEyes != null)
			{
				MinecraftForge.EVENT_BUS.post(new EntityFluidEvent.Exited((Entity)(Object)this, previousFluidOnEyes));
			}
			previousFluidOnEyes = null;
		}
	}

	@Inject(method = "updateFluidOnEyes",
			at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;fluidOnEyes:Lnet/minecraft/tags/ITag;", ordinal = 1, shift = Shift.AFTER))
	public void onUpdateFluidOnEyeAssign(CallbackInfo callbackInfo)
	{
		Fluid currentFluid = null;
		if (fluidOnEyes != null && fluidOnEyes.getValues().size() > 0)
		{
			currentFluid = fluidOnEyes.getValues().get(0);
		}

		if (!Objects.equals(previousFluidOnEyes, currentFluid))
		{
			// We are now submerged in a fluid that doesn't match the previous one.
			if (currentFluid != null)
			{
				MinecraftForge.EVENT_BUS.post(new EntityFluidEvent.Entered((Entity)(Object)this, currentFluid));
			}
		}
	}
}
