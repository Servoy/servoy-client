/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.awt.Point;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
@SuppressWarnings("nls")
public class CSSPositionPropertyType extends DefaultPropertyType<CSSPosition>
	implements IClassPropertyType<CSSPosition>, IFormElementToTemplateJSON<CSSPosition, CSSPosition>, IDesignToFormElement<Object, CSSPosition, CSSPosition>,
	IDesignValueConverter<CSSPosition>, ISabloComponentToRhino<CSSPosition>, IFormElementToSabloComponent<CSSPosition, CSSPosition>
{

	public static final CSSPositionPropertyType INSTANCE = new CSSPositionPropertyType();
	public static final String TYPE_NAME = "CSSPosition";

	protected CSSPositionPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public CSSPosition fromJSON(Object newValue, CSSPosition previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof JSONObject)
		{
			JSONObject json = (JSONObject)newValue;
			return new CSSPosition(json.optString("top"), json.optString("right"), json.optString("bottom"), json.optString("left"), json.optString("width"),
				json.optString("height"));
		}
		return null;
	}

	public JSONWriter toJSON(JSONWriter writer, String key, CSSPosition object, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		FormElement fe = null;
		if (dataConverterContext != null && dataConverterContext.getWebObject() instanceof WebFormComponent)
		{
			fe = ((WebFormComponent)dataConverterContext.getWebObject()).getFormElement();
		}
		FlattenedSolution fs = null;
		Form form = null;
		if (dataConverterContext != null && dataConverterContext.getWebObject() instanceof IContextProvider)
		{
			fs = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication().getFlattenedSolution();
			if (((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getForm() != null)
			{
				form = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getForm().getForm();
			}
		}

		return toJSON(writer, key, object, pd, fe, fs, form);
	}

	private String addPixels(String value)
	{
		if (Utils.getAsInteger(value, -1) != -1) return value + "px";
		return value;
	}

	private boolean isSet(String value)
	{
		return value != null && !value.equals("-1") && !value.trim().isEmpty();
	}


	@Override
	public CSSPosition toSabloComponentValue(CSSPosition formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		if (formElementValue == null) return null;

		// else - "cssPosition" is a mutable object - and, as it can be modified at runtime (rhino access), and it's not a primitive,
		// we want to make a copy of the value from FormElement for runtime, so that changing it in one client will not change it in
		// another or directly in the Persist (so form designer etc.); that would be wrong

		return formElementValue.cloneForRuntime();
	}

	private JSONWriter toJSON(JSONWriter writer, String key, CSSPosition object, PropertyDescription pd, FormElement formElement, FlattenedSolution fs,
		Form context) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		if (object != null)
		{
			writer.key("position").value("absolute");

			String top = object.top;
			String bottom = object.bottom;
			if (formElement != null && fs != null && context != null && !formElement.isInDesigner())
			{
				// adjust the top for parts.
				IPersist persist = formElement.getPersistIfAvailable();
				if (persist instanceof BaseComponent && !Utils.getAsBoolean(((Form)persist.getAncestor(IRepository.FORMS)).isFormComponent()))
				{
					Form form = fs.getFlattenedForm(context);
					Point location = CSSPositionUtils.getLocation(object, form.getSize());

					Part part = form.getPartAt(location.y);
					if (part != null)
					{
						if (isSet(top))
						{
							int topStart = form.getPartStartYPos(part.getID());
							if (topStart > 0)
							{
								if (top.endsWith("px"))
								{
									top = top.substring(0, top.length() - 2);
								}
								int topInteger = Utils.getAsInteger(top, -1);
								if (topInteger != -1)
								{
									top = String.valueOf(topInteger - topStart);
								}
								else
								{
									top = "calc(" + top + " - " + topStart + "px)";
								}
							}
						}
						if (isSet(bottom))
						{
							int extraHeight = form.getSize().height - part.getHeight();
							if (extraHeight > 0)
							{
								if (bottom.endsWith("px"))
								{
									bottom = bottom.substring(0, bottom.length() - 2);
								}
								int bottomInteger = Utils.getAsInteger(bottom, -1);
								if (bottomInteger != -1)
								{
									bottom = String.valueOf(bottomInteger - extraHeight);
								}
								else
								{
									bottom = "calc(" + bottom + " - " + extraHeight + "px)";
								}
							}
						}
					}
				}
			}

			if (isSet(top)) writer.key("top").value(addPixels(top));
			if (isSet(object.left)) writer.key("left").value(addPixels(object.left));
			if (isSet(bottom)) writer.key("bottom").value(addPixels(bottom));
			if (isSet(object.right)) writer.key("right").value(addPixels(object.right));
			if (isSet(object.height))
			{
				if (isSet(top) && isSet(object.bottom))
				{
					writer.key("min-height").value(addPixels(object.height));
				}
				else writer.key("height").value(addPixels(object.height));
			}
			if (isSet(object.width))
			{
				if (isSet(object.left) && isSet(object.right))
				{
					writer.key("min-width").value(addPixels(object.width));
				}
				else writer.key("width").value(addPixels(object.width));
			}
		}
		writer.endObject();
		return writer;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, CSSPosition formElementValue, PropertyDescription pd,
		FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, formElementContext.getFormElement(), formElementContext.getFlattenedSolution(),
			(formElementContext.getContext() != null && formElementContext.getContext().getForm() != null) ? formElementContext.getContext().getForm().getForm()
				: null);
	}

	@Override
	public CSSPosition toFormElementValue(Object designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		if (designValue instanceof JSONObject)
		{
			return fromJSON(designValue, null, pd, null, null);
		}
		if (!(designValue instanceof CSSPosition))
		{
			Debug.error("Wrong design value for css position:" + designValue);
			return defaultValue(pd);
		}
		return (CSSPosition)designValue;
	}

	@Override
	public CSSPosition defaultValue(PropertyDescription pd)
	{
		return new CSSPosition("0", "-1", "-1", "0", "80", "20");
	}

	@Override
	public Class<CSSPosition> getTypeClass()
	{
		return CSSPosition.class;
	}

	@Override
	public CSSPosition fromDesignValue(Object designValue, PropertyDescription propertyDescription, IPersist persit)
	{
		try
		{
			return fromJSON((designValue instanceof String && ((String)designValue).startsWith("{")) ? new JSONObject((String)designValue) : designValue, null,
				propertyDescription, null, null);
		}
		catch (Exception e)
		{
			Debug.error("can't parse '" + designValue + "' to the real type for property converter: " + propertyDescription.getType(), e);
			return null;
		}
	}

	@Override
	public Object toDesignValue(Object javaValue, PropertyDescription pd)
	{
		if (javaValue instanceof CSSPosition)
		{
			CSSPosition cssPosition = (CSSPosition)javaValue;
			JSONObject json = new JSONObject();
			json.put("top", cssPosition.top);
			json.put("left", cssPosition.left);
			json.put("bottom", cssPosition.bottom);
			json.put("right", cssPosition.right);
			json.put("width", cssPosition.width);
			json.put("height", cssPosition.height);
			return json;
		}
		return javaValue;
	}

	@Override
	public boolean isValueAvailableInRhino(CSSPosition webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(CSSPosition webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext, Scriptable startScriptable)
	{
		return new UpdateableCSSPosition(webComponentValue, webObjectContext, pd);
	}
}
