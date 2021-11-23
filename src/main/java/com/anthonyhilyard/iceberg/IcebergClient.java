package com.anthonyhilyard.iceberg;

import net.fabricmc.api.ClientModInitializer;

public class IcebergClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		// Event testing.
		// CriterionCallback.EVENT.register((player, advancement, criterion) -> { Loader.LOGGER.info("CriterionCallback: {}, {}, {}", player.getName().getString(), advancement.getId().toString(), criterion); });
		// EntityFluidEvents.ENTERED.register((entity, fluid) -> { Loader.LOGGER.info("EntityFluidEvents.ENTERED: {}, {}", entity.getName().getString(), Registry.FLUID.getKey(fluid).toString()); });
		// EntityFluidEvents.EXITED.register((entity, fluid) -> { Loader.LOGGER.info("EntityFluidEvents.EXITED: {}, {}", entity.getName().getString(), Registry.FLUID.getKey(fluid).toString()); });
		// NewItemPickupCallback.EVENT.register((uuid, itemStack) -> { Loader.LOGGER.info("NewItemPickupCallback: {}, {}", uuid.toString(), itemStack.getDisplayName().getString()); });
		// RenderTickEvents.START.register((timer) -> { Loader.LOGGER.info("RenderTickEvents.START: {}", timer); });
		// RenderTooltipEvents.PRE.register((stack, components, poseStack, x, y, screenWidth, screenHeight, maxWidth, font, comparison) -> {
		// 	Loader.LOGGER.info("RenderTooltipEvents.PRE: {}, {}, {}, {}, {}, {}, {}, {}, {}, {}", stack.getDisplayName().getString(), components.stream().map(Object::toString).collect(Collectors.joining()), poseStack, x, y, screenWidth, screenHeight, maxWidth, font, comparison);
		// 	return InteractionResult.SUCCESS;
		// });
		// RenderTooltipEvents.COLOR.register((stack, components, poseStack, x, y, font, background, borderStart, borderEnd, comparison) -> {
		// 	Loader.LOGGER.info("RenderTooltipEvents.COLOR: {}, {}, {}, {}, {}, {}, {}, {}, {}, {}", stack.getDisplayName().getString(), components.stream().map(Object::toString).collect(Collectors.joining()), poseStack, x, y, font, borderStart, borderEnd, comparison);
		// 	return null;
		// });
		// RenderTooltipEvents.POST.register((stack, components, poseStack, x, y, font, width, height, comparison) -> {
		// 	Loader.LOGGER.info("RenderTooltipEvents.POST: {}, {}, {}, {}, {}, {}, {}, {}, {}", stack.getDisplayName().getString(), components.stream().map(Object::toString).collect(Collectors.joining()), poseStack, x, y, font, width, height, comparison);
		// });
	}
}
