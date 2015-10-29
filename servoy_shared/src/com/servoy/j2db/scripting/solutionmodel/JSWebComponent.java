/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.j2db.scripting.solutionmodel;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Debug;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "JSComponent")
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public class JSWebComponent extends JSComponent<WebComponent> implements IJavaScriptType
{

	private final IApplication application;

	public JSWebComponent(IJSParent< ? > parent, WebComponent baseComponent, IApplication application, boolean isNew)
	{
		super(parent, baseComponent, isNew);
		this.application = application;
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getBackground()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getBackground()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getBackground()
	{
		return super.getBackground();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getBorderType()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getBorderType()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getBorderType()
	{
		return super.getBorderType();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getFontType()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getFontType()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getFontType()
	{
		return super.getFontType();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getForeground()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getForeground()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getForeground()
	{
		return super.getForeground();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getGroupID()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getGroupID()
	 *
	 * @deprecated not supported
	 */
	@Deprecated
	@JSGetter
	@Override
	public String getGroupID()
	{
		return super.getGroupID();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getPrintSliding()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getPrintSliding()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public int getPrintSliding()
	{
		return super.getPrintSliding();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getPrintable()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getPrintable()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public boolean getPrintable()
	{
		return super.getPrintable();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getStyleClass()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getStyleClass()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public String getStyleClass()
	{
		return super.getStyleClass();
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSComponent#getTransparent()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSComponent#getTransparent()
	 *
	 * @deprecated not supported
	 */
	@Override
	@Deprecated
	@JSGetter
	public boolean getTransparent()
	{
		return super.getTransparent();
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSWebComponent[name:" + getName() + ",type name:" + getTypeName() + ']';
	}

	/**
	 * The webcomponent type (name from the spec).
	 *
	 * @sample
	 * var wc = form.getWebComponent('mycomponent');
	 * application.output(bean.typeName);
	 */
	@JSGetter
	public String getTypeName()
	{
		return getBaseComponent(false).getTypeName();
	}

	@JSSetter
	public void setTypeName(String typeName)
	{
		getBaseComponent(true).setTypeName(typeName);
	}

	/**
	 * Set the design-time value for the given property. For primitive property types you can just set the value.
	 * For more complex property types you can set a JSON value similar to what would be generated in the .frm file if you would design what you need using editor/properties view.
	 * Some property types can be assigned values in the runtime accepted format (for example border, font typed properties have a string representation at runtime and here as well).
	 *
	 * @sample
	 * var wc = form.getWebComponent('mycomponent');
	 * wc.setJSONProperty('mytext', 'Hello World!');
	 * wc.setJSONProperty('mynumber', 1);
	 * wc.setJSONProperty('myborder', 'LineBorder,1,#ccffcc');
	 * wc.setJSONProperty('mydynamicfoundset', { dataproviders: { dp1: "city", dp2: "country" }, foundsetSelector: "" }); // foundset property type using
	 *     // the parent form's foundset and providing two columns of the foundset to client; see foundset property type wiki page for more information
	 */
	@JSFunction
	public void setJSONProperty(String propertyName, Object value)
	{
	}

	public static Object defaultRhinoToDesignValue(Object value, IApplication application)
	{
		// default - stringify what we get from rhino and convert that to org.json usable value
		Scriptable topLevelScope = ScriptableObject.getTopLevelScope(application.getScriptEngine().getSolutionScope());

		Context cx = Context.enter();
		try
		{
			String stringified = (String)ScriptableObject.callMethod(cx, (Scriptable)topLevelScope.get("JSON", topLevelScope), "stringify",
				new Object[] { value });
			value = new JSONObject("{ \"a\" : " + stringified + " }").get("a");
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		finally
		{
			Context.exit();
		}
		return value;
	}

	/**
	 * Get the design-time value of the given property.
	 *
	 * @sample
	 * var wc = form.getWebComponent('mycomponent');
	 * application.output(wc.getJSONProperty('mytext')); // will output a string value if present for a string typed property
	 * application.output(wc.getJSONProperty('mynumber')); // getter will return a number if present
	 * application.output(JSON.stringify(wc.getJSONProperty('mycustomtype'), null, '  ')); // getter returns an object if present for custom types is spec files
	 * application.output(JSON.stringify(wc.getJSONProperty('myarray'), null, '  ')); // getter returns an array type if present for array types
	 * application.output(JSON.stringify(wc.getJSONProperty('myfoundset'), null, '  ')); // getter returns an object representing the design settings of the given property if present
	 */
	@JSFunction
	public Object getJSONProperty(String propertyName)
	{
		return null;
	}

	protected IApplication getApplication()
	{
		return this.application;
	}
}
