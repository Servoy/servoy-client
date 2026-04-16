package com.servoy.j2db.scripting;

import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

/**
 * this is a dummy scope that sets the associated value for TypeInfoFactory to the global one
 * @author jcompagner
 * @since 2026.6
 *
 */
@SuppressWarnings("nls")
public class DummyScope extends ScriptableObject
{
	public DummyScope()
	{
		associateValue("TypeInfoFactory", TypeInfoFactory.GLOBAL);
	}

	@Override
	public String getClassName()
	{
		return "DummyScope";
	}
}