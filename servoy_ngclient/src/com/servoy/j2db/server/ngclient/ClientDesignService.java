/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.awt.Dimension;
import java.awt.Point;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.mozilla.javascript.Undefined;
import org.sablo.websocket.IServerService;

import com.servoy.base.scripting.api.IJSEvent;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;

/**
 * @author jcomp
 *
 */
public class ClientDesignService implements IServerService
{
	public static final String NAME = "clientdesign"; //$NON-NLS-1$
	private final INGApplication application;

	/**
	 * @param ngClient
	 */
	public ClientDesignService(INGApplication application)
	{
		this.application = application;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		Object retValue = null;
		String formname = args.optString("formname");
		IWebFormController form = application.getFormManager().getForm(formname);
		if (form != null && form.getDesignModeCallbacks() != null)
		{
			String layoutWrapperName = args.optString("element");
			JSEvent event = new JSEvent();
			event.setFormName(formname);
			JSONObject jsevent = args.optJSONObject("event");
			if (jsevent != null)
			{
				event.setTimestamp(new Timestamp(jsevent.getLong("timestamp")));
				if (jsevent.has("x")) event.setLocation(new Point(jsevent.getInt("x"), jsevent.getInt("y")));
				if (jsevent.has("modifiers")) event.setModifiers(jsevent.getInt("modifiers"));
			}

			List<RuntimeWebComponent> selection = new ArrayList<>();
			if (layoutWrapperName != null && layoutWrapperName.startsWith("layout."))
			{
				String name = layoutWrapperName.substring(7);
				RuntimeWebComponent[] webComponentElements = form.getWebComponentElements();
				for (RuntimeWebComponent component : webComponentElements)
				{
					if (component.getComponent().getName().equals(name))
					{
						JSONObject location = args.optJSONObject("location");
						if (location != null)
						{
							component.getComponent().setProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(),
								new Point(location.optInt("x"), location.optInt("y")));
						}
						JSONObject size = args.optJSONObject("size");
						if (size != null)
						{
							component.getComponent().setProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(),
								new Dimension(size.optInt("width"), size.optInt("height")));
						}
						selection.add(component);
						break;
					}
				}
			}
			event.setData(selection.toArray());
			event.setName(methodName);
			switch (methodName)
			{
				case "onselect" : //$NON-NLS-1$
					event.setType(IJSEvent.ACTION);
					retValue = form.getDesignModeCallbacks().executeOnSelect(event);
					if (retValue == null || Undefined.isUndefined(retValue)) retValue = Boolean.TRUE; // selection is allowed, only disallow it when retValue is false directly
					break;
				case "ondrag" : //$NON-NLS-1$
					event.setType(IJSEvent.EventType.onDrag);
					retValue = form.getDesignModeCallbacks().executeOnDrag(event);
					break;
				case "ondrop" : //$NON-NLS-1$
					event.setType(IJSEvent.EventType.onDrop);
					retValue = form.getDesignModeCallbacks().executeOnDrop(event);
					break;
				case "onresize" : //$NON-NLS-1$
					event.setType(IJSEvent.EventType.onDrop);
					retValue = form.getDesignModeCallbacks().executeOnResize(event);
					break;
				case "onrightclick" : //$NON-NLS-1$
					event.setType(IJSEvent.EventType.rightClick);
					retValue = form.getDesignModeCallbacks().executeOnRightClick(event);
					break;
				case "ondoubleclick" : //$NON-NLS-1$
					event.setType(IJSEvent.EventType.doubleClick);
					retValue = form.getDesignModeCallbacks().executeOnDblClick(event);
					break;
				default :
					break;
			}
		}
		return Undefined.isUndefined(retValue) ? null : retValue;
	}

}
