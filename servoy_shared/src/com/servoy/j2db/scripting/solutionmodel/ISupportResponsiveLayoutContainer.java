/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;

/**
 * @author lvostinar
 *
 */
public interface ISupportResponsiveLayoutContainer
{
	/**
	 * Returns all JSResponsiveLayoutContainer objects of this container
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var containers = frm.getResponsiveLayoutContainers();
	 * for (var c in containers)
	 * {
	 * 		var fname = containers[c].name;
	 * 		application.output(fname);
	 * }
	 *
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return all JSResponsiveLayoutContainer objects of this container
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	default public JSResponsiveLayoutContainer[] getResponsiveLayoutContainers(boolean returnInheritedElements)
	{
		List<JSResponsiveLayoutContainer> containers = new ArrayList<JSResponsiveLayoutContainer>();
		AbstractContainer container = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<CSSPositionLayoutContainer> iterator = container.getResponsiveLayoutContainers();
		while (iterator.hasNext())
		{
			containers.add(getApplication().getScriptEngine().getSolutionModifier().createResponsiveLayoutContainer((IJSParent)this, iterator.next(), false));
		}
		return containers.toArray(new JSResponsiveLayoutContainer[containers.size()]);
	}


	/**
	 * Returns a JSResponsiveLayoutContainer that has the given name of this container.
	 *
	 * @sample
	 * var container = myForm.getResponsiveLayoutContainer("row1");
	 * application.output(container.name);
	 *
	 * @param name the specified name of the container
	 *
	 * @return a JSResponsiveLayoutContainer object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	default public JSResponsiveLayoutContainer getResponsiveLayoutContainer(String name)
	{
		if (name == null) return null;

		Iterator<CSSPositionLayoutContainer> containers = getFlattenedContainer().getResponsiveLayoutContainers();
		while (containers.hasNext())
		{
			CSSPositionLayoutContainer container = containers.next();
			if (name.equals(container.getName()))
			{
				return getApplication().getScriptEngine().getSolutionModifier().createResponsiveLayoutContainer((IJSParent)this, container, false);
			}
		}
		return null;
	}

	public IApplication getApplication();

	public AbstractContainer getFlattenedContainer();

	public AbstractContainer getContainer();
}
