/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.template;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONStringer;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.DefaultNavigator;
import com.servoy.j2db.server.ngclient.WebGridFormUI;
import com.servoy.j2db.server.websocket.utils.JSONUtils;
import com.servoy.j2db.util.Utils;

/**
 * Wrapper around form for use in templates.
 *
 * @author rgansevles
 *
 */
public class FormWrapper
{
	private final Form form;
	private final boolean isTableView;
	private final boolean useControllerProvider;
	private final String realName;

	public FormWrapper(Form form, String realName, boolean useControllerProvider)
	{
		this.form = form;
		this.realName = realName;
		this.useControllerProvider = useControllerProvider;
		isTableView = (form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED);
	}

	public String getControllerName()
	{
		return realName == null ? form.getName() : realName.replace('-', '_');
	}

	public String getName()
	{
		return realName == null ? form.getName() : realName;
	}

	public String getRegisterMethod()
	{
		if (useControllerProvider)
		{
			return "controllerProvider.register";
		}
		return "angular.module('servoyApp').controller";
	}

	public int getPageSize()
	{
		return WebGridFormUI.PAGE_SIZE;
	}

	public Collection<Part> getParts()
	{
		List<Part> parts = new ArrayList<>();
		Iterator<Part> it = form.getParts();
		while (it.hasNext())
		{
			Part part = it.next();
			if (!Part.rendersOnlyInPrint(part.getPartType()))
			{
				parts.add(part);
			}
		}
		return parts;
	}

	public Collection<BaseComponent> getBaseComponents()
	{
		List<BaseComponent> baseComponents = new ArrayList<>();
		Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (it.hasNext())
		{
			IPersist persist = it.next();
			if (persist instanceof BaseComponent)
			{
				baseComponents.add((BaseComponent)persist);
			}
		}
		if (form.getNavigatorID() == Form.NAVIGATOR_DEFAULT)
		{
			baseComponents.add(DefaultNavigator.INSTANCE);
		}
		return baseComponents;
	}

	public Collection<BaseComponent> getBodyComponents()
	{
		Part part = null;
		for (Part prt : Utils.iterate(form.getParts()))
		{
			if (prt.getPartType() == Part.BODY)
			{
				part = prt;
				break;
			}
		}

		List<BaseComponent> baseComponents = new ArrayList<>();
		if (part == null) return baseComponents;

		int startPos = form.getPartStartYPos(part.getID());
		int endPos = part.getHeight();
		Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (it.hasNext())
		{
			IPersist persist = it.next();
			if (persist instanceof GraphicalComponent && isTableView && ((GraphicalComponent)persist).getLabelFor() != null) continue;
			if (persist instanceof BaseComponent)
			{
				Point location = ((BaseComponent)persist).getLocation();
				if (startPos <= location.y && endPos >= location.y)
				{
					baseComponents.add((BaseComponent)persist);
				}
			}
		}
		return baseComponents;
	}

	public String getPropertiesString() throws JSONException, IllegalArgumentException
	{
		Map<String, Object> properties = form.getPropertiesMap(); // a copy of form properties
		if (!properties.containsKey("size")) properties.put("size", form.getSize());
		removeUnneededFormProperties(properties);

		return JSONUtils.addObjectPropertiesToWriter(new JSONStringer().object(), properties).endObject().toString();
	}

	private static void removeUnneededFormProperties(Map<String, Object> properties)
	{
		properties.remove(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_SHOWINMENU.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ENCAPSULATION.getPropertyName());
	}
}
