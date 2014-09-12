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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.headlessclient.dataui.AbstractFormLayoutProvider;
import com.servoy.j2db.server.headlessclient.dataui.AnchoredFormLayoutProvider;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.ListViewPortal;

/**
 * @author lvostinar
 *
 */
public class PartWrapper
{
	private final Part part;
	private final AbstractFormLayoutProvider layoutProvider;
	private final Form context;
	IServoyDataConverterContext converterContext;

	public PartWrapper(Part part, Form context, IServoyDataConverterContext converterContext)
	{
		this.part = part;
		this.context = context;
		this.converterContext = converterContext;
		layoutProvider = new AnchoredFormLayoutProvider(null, (Solution)context.getAncestor(IRepository.SOLUTIONS), context, null);
		layoutProvider.setDefaultNavigatorShift(0);
	}

	public String getStyle()
	{
		TextualStyle style = new TextualStyle()
		{
			@Override
			protected void appendValue(StringBuffer retval, String pSelector, String name, String value)
			{
				retval.append("\"");
				retval.append(name);
				retval.append("\"");
				retval.append(": ");
				retval.append("\"");
				retval.append(value);
				retval.append("\"");
				retval.append(',');
			}
		};
		layoutProvider.fillPartStyle(style, part);
		String partStyle = style.getValuesAsString(null);
		if (partStyle.endsWith(","))
		{
			partStyle = partStyle.substring(0, partStyle.length() - 1);
		}
		return "{" + partStyle + "}";
	}

	public String getName()
	{
		String name = Part.getDisplayName(part.getPartType());
		name = name.replace(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return name.toLowerCase();
	}

	public Collection<BaseComponent> getBaseComponents()
	{
		if (part.getPartType() == Part.BODY)
		{
			switch (context.getView())
			{
				case FormController.TABLE_VIEW :
				case FormController.LOCKED_TABLE_VIEW :
					return null;
				case IForm.LIST_VIEW :
				case FormController.LOCKED_LIST_VIEW :
					return Arrays.asList(new BaseComponent[] { new ListViewPortal(context) });
			}
		}

		List<BaseComponent> baseComponents = new ArrayList<>();
		int startPos = context.getPartStartYPos(part.getID());
		int endPos = part.getHeight();
		List<IFormElement> persists = context.getFlattenedObjects();
		for (IFormElement persist : persists)
		{
			Point location = persist.getLocation();
			if (startPos <= location.y && endPos > location.y)
			{
				if (isSecurityVisible(persist)) baseComponents.add((BaseComponent)persist);
			}
		}
		return baseComponents;
	}

	public boolean isSecurityVisible(IPersist persist)
	{
		if (converterContext.getApplication() == null) return true;
		int access = converterContext.getApplication().getFlattenedSolution().getSecurityAccess(persist.getUUID());
		boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
		return b_visible;
	}
}
