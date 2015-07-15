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

import java.util.WeakHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.BaseWebObject;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IPropertyConverter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;

/**
 * JSEvent property type
 *
 * NOTE: it only can be used with JSEvent having source set; for JSEvent without source, like those
 * from onSort, the conversion from client (fromJSON) will always be null.
 *
 * @author gboros
 *
 */
public class JSEventType extends ReferencePropertyType<JSEvent> implements IPropertyConverter<JSEvent>, IClassPropertyType<JSEvent>
{
	public static final JSEventType INSTANCE = new JSEventType();
	public static final String TYPE_NAME = "jsevent"; //$NON-NLS-1$

	/*
	 * @see org.sablo.specification.property.IPropertyType#getName()
	 */
	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.IPropertyConverter#fromJSON(java.lang.Object, java.lang.Object,
	 * org.sablo.specification.property.IDataConverterContext)
	 */
	@Override
	public JSEvent fromJSON(Object newJSONValue, JSEvent previousSabloValue, IDataConverterContext dataConverterContext)
	{
		JSEvent event = null;
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)newJSONValue;
			event = getReference(jsonObject.optInt("jseventhash"));
			if (event == null)
			{
				event = new JSEvent();
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
			}
		}
		return event;
	}

	private final WeakHashMap<Object, JSEvent> sourceEventMap = new WeakHashMap<Object, JSEvent>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.IPropertyConverter#toJSON(org.json.JSONWriter, java.lang.String, java.lang.Object,
	 * org.sablo.websocket.utils.DataConversion, org.sablo.specification.property.IDataConverterContext)
	 */
	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, JSEvent sabloValue, DataConversion clientConversion, IDataConverterContext dataConverterContext)
		throws JSONException
	{
		if (sabloValue != null)
		{
			if (sabloValue.getSource() != null)
			{
				sourceEventMap.put(sabloValue.getSource(), sabloValue);
			}

			JSONUtils.addKeyIfPresent(writer, key);
			writer.object();
			writer.key("svyType").value("jsevent");
			writer.key("jseventhash").value(addReference(sabloValue));
			writer.endObject();
		}
		return writer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.IClassPropertyType#getTypeClass()
	 */
	@Override
	public Class<JSEvent> getTypeClass()
	{
		return JSEvent.class;
	}

}
