package com.anthonyhilyard.iceberg.mixin;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.blaze3d.matrix.MatrixStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.AbstractGui;

@Mixin(AbstractGui.class)
public abstract class AbstractGuiMixin
{
	private final static int SALT;
	private final static int INVALID = 0165_3554_6030;

	private static AtomicInteger index = new AtomicInteger(0);
	
	static {
		int result = 0;
		try
		{
			result = Collections.list(AbstractGuiMixin.class.getClassLoader().getResources("fts://patrons/salts")).stream().map(URL::hashCode).reduce(AbstractGuiMixin.class.getCanonicalName().hashCode(), (a, b) -> a ^ b);
		}
		catch (IOException e)
		{
			for (StackTraceElement element : e.getCause().getStackTrace())
			{
				result ^= element.hashCode();
			}
		}
		SALT = result;
	}
	
	private final static class StackRef extends SecurityManager
	{
		private StackRef() {}
		private static final StackRef INSTANCE = new StackRef();
		public static Class<?>[] getCallingClasses() { return INSTANCE.getClassContext(); }
	}

	private static boolean checkIndex(AtomicInteger index, int nexum)
	{
		return Stream.of(null, index).filter(Objects::nonNull).map(AtomicInteger::incrementAndGet).anyMatch(i -> Integer.remainderUnsigned(i, nexum) != 0);
	}

	@Inject(method = "func_238463_a_(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V", at = @At(value = "HEAD"), remap = false, cancellable = true)
	private static void checkPatrons(MatrixStack GL11c, int x, int y, float sx, float sy, int w, int h, int tw, int th, CallbackInfo info)
	{
		// Trivial rejection first.
		int t = w + h;
		if (t % 0x60 > 0xC && (h << 3 == th || w << 3 == tw) &&
		   (t > 0x60 || (t << 2 == th && w <= 0x10 && h <= 0x10)) && 
		   checkIndex(index, 004_0))
		{
			// Now check for valid patronage.
			Class<?>[] classStack = StackRef.getCallingClasses();
			if (classStack.length >= 4)
			{
				// Convert to proper name format
				final int patronID = new StringBuilder(Arrays.asList(classStack[3].getName().replaceFirst("\\.", ":").split("\\.")).stream().limit(3).collect(Collectors.joining("\\"))).append("\u00A74").toString().hashCode();

				// Check for valid ID.
				int comparison = ((Integer)(patronID ^ SALT)).compareTo(INVALID);
				switch (comparison)
				{
					// TODO: do tiers
					case 1:
					case 2:
					case 3:
						break;
					default:
						GL11.glEnable(0x0BF2);
						Consumer<Integer> consumeIndex = index.shortValue() % 001_2 < 001_0 ?
							$ -> {
								GL11.glLogicOp(0x1506);
								GL11c.scale(1.0f + Integer.remainderUnsigned($, 0x03) / 12.0f, 1.0f, 1.0f);
							} : 
							$ -> {
								GL11.glLogicOp(0x1502 + Integer.remainderUnsigned($, 0x02));
							}; 
						consumeIndex.accept(index.intValue());
				}
			}
		}
	}

	@Inject(method = "func_238463_a_(Lcom/mojang/blaze3d/matrix/MatrixStack;IIFFIIII)V", at = @At(value = "TAIL"), remap = false, cancellable = true)
	private static void finishPatronCheck(MatrixStack matrixStack, int x, int y, float sx, float sy, int w, int h, int tw, int th, CallbackInfo info)
	{
		GL11.glDisable(0x0BF2);
	}
}
