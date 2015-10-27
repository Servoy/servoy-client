/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.FunctionPropertyType;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.Utils;

/**
 * Some extra Servoy specific behavior added to the sablo function property type.
 *
 * @author acostescu
 */
public class NGFunctionPropertyType extends FunctionPropertyType implements IRhinoDesignConverter
{

	public final static NGFunctionPropertyType NG_INSTANCE = new NGFunctionPropertyType();

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		// TODO move some code here from JSWebComponent.setJSONProperty maybe?
		return value;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		return JSForm.getEventHandler(application, webComponent.getBaseComponent(false), Utils.getAsInteger(value), webComponent.getJSParent(), pd.getName());
	}

}
