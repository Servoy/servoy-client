package com.servoy.j2db.scripting;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

import com.servoy.j2db.util.IntHashMap;

public class ServoyContextFactory extends ContextFactory
{
	private final IntHashMap<Boolean> features = new IntHashMap<Boolean>(2);

	@Override
	protected boolean hasFeature(Context context, int featureIndex)
	{
		Boolean value = features.get(featureIndex);
		if (value != null) return value.booleanValue();
		if (featureIndex == Context.FEATURE_LOCATION_INFORMATION_IN_ERROR) return true;
		return super.hasFeature(context, featureIndex);
	}

	public void setFeature(int featureIndex, boolean value)
	{
		features.put(featureIndex, Boolean.valueOf(value));
	}
}