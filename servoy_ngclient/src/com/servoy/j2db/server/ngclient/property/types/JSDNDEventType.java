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

package com.servoy.j2db.server.ngclient.property.types;

import java.awt.Point;
import java.sql.Timestamp;
import java.util.WeakHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dnd.JSDNDEvent;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.util.Debug;

/**
 * JSDNDEvent property type
 *
 * NOTE: it only can be used with JSEvent having source set; for JSEvent without source, like those
 * from onSort, the conversion from client (fromJSON) will always be null.
 *
 * @author jcompagner
 *
 */
public class JSDNDEventType extends ReferencePropertyType<JSDNDEvent> implements IPropertyConverterForBrowser<JSDNDEvent>, IClassPropertyType<JSDNDEvent>
{
	public static final JSDNDEventType INSTANCE = new JSDNDEventType();
	public static final String TYPE_NAME = "JSDNDEvent"; //$NON-NLS-1$

	/*
	 * @see org.sablo.specification.property.IPropertyType#getName()
	 */
	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public JSDNDEvent fromJSON(Object newJSONValue, JSDNDEvent previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		JSDNDEvent event = null;
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)newJSONValue;
			event = getReference(jsonObject.optInt("jseventhash"));
			if (event == null)
			{
				event = new JSDNDEvent();
				BaseWebObject webObject = dataConverterContext.getWebObject();
				event.setType(jsonObject.optString("eventType")); //$NON-NLS-1$
				String formName = jsonObject.optString("formName");
				if (formName.length() == 0)
				{
					if (webObject instanceof WebFormComponent)
					{
						formName = ((WebFormComponent)webObject).getFormElement().getForm().getName();
					}
					else if (webObject instanceof WebFormUI)
					{
						formName = ((WebFormUI)webObject).getName();
					}
				}
				if (formName.length() > 0) event.setFormName(formName);
				String elementName = jsonObject.optString("elementName"); //$NON-NLS-1$
				if (elementName.length() > 0) event.setElementName(elementName);
				if (formName.length() > 0 && elementName.length() > 0)
				{
					INGApplication application = ((IContextProvider)webObject).getDataConverterContext().getApplication();
					IWebFormController formController = application.getFormManager().getForm(formName);
					if (formController != null)
					{
						for (RuntimeWebComponent c : formController.getWebComponentElements())
						{
							if (elementName.equals(c.getComponent().getName()))
							{
								event.setSource(c);
							}
						}
					}
				}
				try
				{
					if (jsonObject.has("x")) event.setLocation(new Point(jsonObject.optInt("x"), jsonObject.optInt("y")));
					if (jsonObject.has("modifiers")) event.setModifiers(jsonObject.optInt("modifiers"));
					if (jsonObject.has("data")) event.setData(jsonObject.opt("data"));
					event.setTimestamp(new Timestamp(jsonObject.optLong("timestamp")));
				}
				catch (Exception e)
				{
					Debug.error("error setting event properties from " + jsonObject + ", for component: " + elementName + " on form " + formName, e);
				}
			}
		}
		return event;
	}

	private final WeakHashMap<Object, JSDNDEvent> sourceEventMap = new WeakHashMap<>();

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, JSDNDEvent sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			if (sabloValue.getSource() != null)
			{
				sourceEventMap.put(sabloValue.getSource(), sabloValue);
			}

			JSONUtils.addKeyIfPresent(writer, key);
			writer.object();
			writer.key("svyType").value("JSEvent");
			writer.key("jseventhash").value(addReference(sabloValue));
			writer.endObject();
		}
		return writer;
	}

	@Override
	public Class<JSDNDEvent> getTypeClass()
	{
		return JSDNDEvent.class;
	}

}
