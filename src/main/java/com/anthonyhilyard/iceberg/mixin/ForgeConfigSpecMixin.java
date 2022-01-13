package com.anthonyhilyard.iceberg.mixin;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.anthonyhilyard.iceberg.util.DynamicSubconfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;


@Mixin(value = ForgeConfigSpec.class, remap = false)
public class ForgeConfigSpecMixin
{
	@Shadow(remap = false)
	private Map<List<String>, String> levelComments;

	@Shadow(remap = false)
	private boolean stringsMatchIgnoringNewlines(@Nullable Object obj1, @Nullable Object obj2) { return false; }

	/**
	 * @author iceberg
	 * @reason Overwrite the correct method to fix subconfigs not being handled properly.
	 */
	@Overwrite(remap = false)
	private int correct(UnmodifiableConfig spec, CommentedConfig config, LinkedList<String> parentPath, List<String> parentPathUnmodifiable, CorrectionListener listener, CorrectionListener commentListener, boolean dryRun)
	{
		int count = 0;

		Map<String, Object> specMap = spec.valueMap();
		Map<String, Object> configMap = config.valueMap();

		for (Map.Entry<String, Object> specEntry : specMap.entrySet())
		{
			final String key = specEntry.getKey();
			Object specValue = specEntry.getValue();
			final Object configValue = configMap.get(key);
			final CorrectionAction action = configValue == null ? CorrectionAction.ADD : CorrectionAction.REPLACE;

			parentPath.addLast(key);

			String subConfigComment = null;

			// If this value is a config, use that as the spec value to support subconfigs.
			if (specValue instanceof ValueSpec valueSpec && valueSpec.getDefault() instanceof UnmodifiableConfig)
			{
				subConfigComment = valueSpec.getComment();
				specValue = valueSpec.getDefault();
			}

			if (specValue instanceof UnmodifiableConfig)
			{
				if (configValue instanceof CommentedConfig)
				{
					count += correct((UnmodifiableConfig)specValue, (CommentedConfig)configValue, parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
					if (count > 0 && dryRun)
					{
						return count;
					}
				}
				else if (dryRun)
				{
					return 1;
				}
				else
				{
					CommentedConfig newValue = config.createSubConfig();
					configMap.put(key, newValue);
					listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
					count++;
					count += correct((UnmodifiableConfig)specValue, newValue, parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
				}

				String newComment = subConfigComment == null ? levelComments.get(parentPath) : subConfigComment;
				String oldComment = config.getComment(key);
				if (!stringsMatchIgnoringNewlines(oldComment, newComment))
				{
					if (commentListener != null)
					{
						commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, newComment);
					}

					if (dryRun)
					{
						return 1;
					}

					config.setComment(key, newComment);
				}
			}
			else
			{
				ValueSpec valueSpec = (ValueSpec)specValue;
				if (!valueSpec.test(configValue))
				{
					if (dryRun)
					{
						return 1;
					}

					Object newValue = valueSpec.correct(configValue);
					configMap.put(key, newValue);
					listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
					count++;
				}
				String oldComment = config.getComment(key);
				if (!stringsMatchIgnoringNewlines(oldComment, valueSpec.getComment()))
				{
					if (commentListener != null)
					{
						commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, valueSpec.getComment());
					}

					if (dryRun)
					{
						return 1;
					}

					config.setComment(key, valueSpec.getComment());
				}
			}

			parentPath.removeLast();
		}

		// Second step: removes the unspecified values
		for (Iterator<Map.Entry<String, Object>> ittr = configMap.entrySet().iterator(); ittr.hasNext();)
		{
			Map.Entry<String, Object> entry = ittr.next();

			// If the spec is a dynamic subconfig, don't bother checking the spec since that's the point.
			if (!(spec instanceof DynamicSubconfig) && !specMap.containsKey(entry.getKey()))
			{
				if (dryRun)
				{
					return 1;
				}

				ittr.remove();
				parentPath.addLast(entry.getKey());
				listener.onCorrect(CorrectionAction.REMOVE, parentPathUnmodifiable, entry.getValue(), null);
				parentPath.removeLast();
				count++;
			}
		}
		return count;
	}
}
