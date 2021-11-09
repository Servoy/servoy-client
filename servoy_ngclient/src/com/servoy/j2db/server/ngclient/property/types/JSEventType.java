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
import java.util.Date;
import java.util.WeakHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.JSBaseEvent;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.component.RuntimeLegacyComponent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.util.Debug;

/**
 * JSEvent property type
 *
 * NOTE: it only can be used with JSEvent having source set; for JSEvent without source, like those
 * from onSort, the conversion from client (fromJSON) will always be null.
 *
 * @author gboros
 *
 */
public class JSEventType extends UUIDReferencePropertyType<JSEvent> implements IPropertyConverterForBrowser<JSEvent>, IClassPropertyType<JSEvent>
{
	public static final JSEventType INSTANCE = new JSEventType();
	public static final String TYPE_NAME = "JSEvent"; //$NON-NLS-1$

	/*
	 * @see org.sablo.specification.property.IPropertyType#getName()
	 */
	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public JSEvent fromJSON(Object newJSONValue, JSEvent previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		JSEvent event = null;
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)newJSONValue;
			event = getReference(jsonObject.optString("jseventhash")); //$NON-NLS-1$
			if (event == null)
			{
				BaseWebObject webObject = dataConverterContext.getWebObject();
				event = new JSEvent();
				fillJSEvent(event, jsonObject, webObject, null);
			}
		}
		else if (newJSONValue instanceof JSEvent)
		{
			event = (JSEvent)newJSONValue;
		}
		return event;
	}

	/**
	 * @param event Event that needs to be filled
	 * @param jsonObject The json data for that has the event data.
	 * @param webObject The webObject element (WEbFormComponent or WebFormUI)
	 * @param controller Optional the controller object if the caller knows this already
	 * @return
	 */
	@SuppressWarnings("nls")
	public static void fillJSEvent(JSBaseEvent event, JSONObject jsonObject, BaseWebObject webObject, IWebFormController controller)
	{
		event.setType(jsonObject.optString("eventType"));
		String formName = controller != null ? controller.getName() : "";
		String elementName = "";

		if (webObject instanceof WebFormComponent)
		{
			elementName = ((WebFormComponent)webObject).getFormElement().getRawName();
			if (elementName == null) elementName = "";
			if (formName.isEmpty())
			{
				BaseWebObject parentWebObject = ((WebFormComponent)webObject).getParent();
				while (parentWebObject != null && !(parentWebObject instanceof WebFormUI))
				{
					parentWebObject = ((WebFormComponent)parentWebObject).getParent();
				}
				if (parentWebObject instanceof WebFormUI)
				{
					formName = parentWebObject.getName();
				}
			}
		}

		if (formName.isEmpty())
		{
			formName = jsonObject.optString("formName");
		}

		if (formName.isEmpty() && webObject instanceof WebFormUI)
		{
			// executeInlineScript with an event in params tells a DAL to execute it and it gives a formui as context to the fromJSON conversion
			// for JSEventType problem is that it can give any formName from the client (the component/service can give anything there as an arg); or, if the function
			// (sent to client before through "function" type) that will be used to execute the script is a global/scope function
			// then formName can be null and in that case the WebFormUI will be the main form or the window (not the most nested one)

			// so this is just a fallback and we do give priority to the form name determined on client through $window.createJSEvent(...) an put into
			// "jsonObject" (see if above) - to target the correct form - closest one to event
			formName = webObject.getName();
		}

		if (elementName.isEmpty())
		{
			elementName = jsonObject.optString("elementName");
		}
		if (!formName.isEmpty()) event.setFormName(formName);
		if (!elementName.isEmpty()) event.setElementName(elementName);
		if (!formName.isEmpty())
		{
			INGApplication application = ((IContextProvider)webObject).getDataConverterContext().getApplication();
			IWebFormController formController = controller != null ? controller : application.getFormManager().getForm(formName);
			if (formController != null)
			{
				FormScope formScope = formController.getFormScope();
				if (formScope != null)
				{
					ElementScope elementsScope = (ElementScope)formScope.get("elements", null);
					if (elementsScope != null)
					{
						Object scriptableElement = !elementName.isEmpty() ? elementsScope.get(elementName, null) : null;
						if (scriptableElement != null && scriptableElement != Scriptable.NOT_FOUND)
						{
							event.setSource(scriptableElement);
						}
						else if (webObject instanceof WebFormComponent)
						{
							// quickly create a scriptable wrapper around the component so that the source can be set to a value that we expect.
							FormElement fe = ((WebFormComponent)webObject).getFormElement();
							RuntimeWebComponent runtimeComponent = new RuntimeWebComponent((WebFormComponent)webObject, webObject.getSpecification());
							if (fe.isLegacy() || ((fe.getForm().getView() == IForm.LIST_VIEW ||
								fe.getForm().getView() == FormController.LOCKED_LIST_VIEW ||
								fe.getForm().getView() == FormController.TABLE_VIEW || fe.getForm().getView() == FormController.LOCKED_TABLE_VIEW) &&
								fe.getTypeName().startsWith("svy-")))
							{
								// add legacy behavior
								runtimeComponent.setPrototype(new RuntimeLegacyComponent((WebFormComponent)webObject));
							}
							event.setSource(runtimeComponent);
						}
					}
				}
			}
		}
		try
		{
			if (jsonObject.has("x")) event.setLocation(new Point(jsonObject.optInt("x"), jsonObject.optInt("y")));
			if (jsonObject.has("modifiers")) event.setModifiers(jsonObject.optInt("modifiers"));
			if (jsonObject.has("data")) event.setData(jsonObject.opt("data"));
			if (jsonObject.has("timestamp")) event.setTimestamp(new Timestamp(jsonObject.getLong("timestamp")));
			else event.setTimestamp(new Date());
		}
		catch (Exception e)
		{
			Debug.error("error setting event properties from " + jsonObject + ", for component: " + elementName + " on form " + formName, e);
		}
	}

	private final WeakHashMap<Object, JSEvent> sourceEventMap = new WeakHashMap<Object, JSEvent>();

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, JSEvent sabloValue, PropertyDescription pd, DataConversion clientConversion,
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
	public Class<JSEvent> getTypeClass()
	{
		return JSEvent.class;
	}

}
