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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author lvostinar
 *
 */
public interface IBaseContainer
{
	/**
	 * Creates a new JSWebComponent (spec based component) object on the RESPONSIVE container(form , layout container or responsive layout container).
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var container = myForm.getLayoutContainer("row1")
	 * var bean = container.newWebComponent('bean','mypackage-testcomponent',1);
	 *
	 * @param name the specified name of the JSWebComponent object
	 * @param type the webcomponent name as it appears in the spec
	 * @param position the position of JSWebComponent object in its parent container
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	default public JSWebComponent newWebComponent(String name, String type, int position)
	{
		checkModification();
		try
		{
			AbstractContainer container = getContainer();
			Form form = (Form)container.getAncestor(IRepository.FORMS);
			if (form.isResponsiveLayout() || container instanceof LayoutContainer)
			{
				if (name == null)
				{
					String componentName = type;
					int index = componentName.indexOf("-");
					if (index != -1)
					{
						componentName = componentName.substring(index + 1);
					}
					componentName = componentName.replaceAll("-", "_"); //$NON-NLS-1$//$NON-NLS-2$
					name = componentName + "_" + getNextId(); //$NON-NLS-1$
					IJSParent< ? > parent = (IJSParent)this;
					while (!(parent instanceof JSForm))
					{
						parent = parent.getJSParent();
					}
					if (parent instanceof JSForm)
					{
						while (findComponent((JSForm)parent, name) != null)
						{
							name = componentName + "_" + getNextId();
						}
					}

				}
				WebComponent webComponent = container.createNewWebComponent(IdentDocumentValidator.checkName(name), type);
				webComponent.setLocation(new Point(position, position));
				return JSBaseContainer.createWebComponent((IJSParent)this, webComponent, getApplication(), true);
			}
			else
			{
				throw new RuntimeException("Form " + form.getName() + " is not responsive. Cannot create component without specifying the location and size.");
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a JSWebComponent that has the given name that is a child of this form, layout container or responsive layout container.
	 * Use findWebComponent() to find a webcomponent through the hierarchy
	 *
	 * @sample
	 * var btn = container.getWebComponent("mycomponent");
	 * application.output(mybean.typeName);
	 *
	 * @param name the specified name of the web component
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	default public JSWebComponent getWebComponent(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<WebComponent> webComponents = getFlattenedContainer().getWebComponents();
			while (webComponents.hasNext())
			{
				WebComponent webComponent = webComponents.next();
				if (name.equals(webComponent.getName()))
				{
					return JSBaseContainer.createWebComponent(getCorrectIJSParent(this, webComponent), webComponent, getApplication(), false);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.log(ex);
		}
		return null;
	}

	/**
	 * Removes a JSWebComponent that has the specified name. Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * form.removeWebComponent('mybean')
	 *
	 * @param name the specified name of the JSWebComponent to be removed
	 *
	 * @return true if the JSWebComponent has been removed; false otherwise
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	default public boolean removeWebComponent(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<WebComponent> webComponents = getContainer().getWebComponents();
		while (webComponents.hasNext())
		{
			WebComponent webComponent = webComponents.next();
			if (name.equals(webComponent.getName()))
			{
				getContainer().removeChild(webComponent);
				return true;

			}
		}
		return false;
	}

	/**
	 * Returns all JSWebComponents of this form/container.
	 * If this method is called on a form, then it will return all web components on that form.
	 * If the form is responsive, it will return the web components from all the containers.
	 *
	 * @sample
	  * var webComponents = myForm.getWebComponents(false);
	 * for (var i in webComponents)
	 * {
	 * 	if (webComponents[i].name != null)
	 * 		application.output(webComponents[i].name);
	 * }
	 *
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return the list of all JSWebComponents on this forms
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	default public JSWebComponent[] getWebComponents(boolean returnInheritedElements)
	{
		List<JSWebComponent> webComponents = new ArrayList<JSWebComponent>();
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<WebComponent> iterator = form2use.getWebComponents();
		while (iterator.hasNext())
		{
			WebComponent webComponent = iterator.next();
			webComponents.add(JSBaseContainer.createWebComponent(getCorrectIJSParent(this, webComponent), webComponent, getApplication(), false));
		}
		return webComponents.toArray(new JSWebComponent[webComponents.size()]);
	}

	default public IPersist findComponent(JSForm jsform, final String name)
	{
		return (IPersist)jsform.getFlattenedContainer().acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof ISupportName && name.equals(((ISupportName)o).getName()))
				{
					return o;
				}
				return o instanceof ISupportFormElements ? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});
	}

	/**
	 * When creating a JSWebComponent from a who-knows-how-deeply-nested (in case of responsive forms) webComponent in a form and we only know the form,
	 * then we need to get step by step the solution model objects in order to use the correct direct parent for the child SM object creation.
	 *
	 * @param startingContainer
	 * @param possiblyNestedChild nested child component
	 * @return the direct parent (IJSParent) of the given webComponent.
	 */
	default IJSParent< ? > getCorrectIJSParent(IBaseContainer startingContainer, IPersist possiblyNestedChild)
	{
		ArrayList<ISupportChilds> parentHierarchy = new ArrayList<>();
		ISupportChilds parent = possiblyNestedChild.getParent();
		while (parent != (startingContainer.getContainer() instanceof IFlattenedPersistWrapper< ? >
			? ((IFlattenedPersistWrapper< ? >)startingContainer.getContainer()).getWrappedPersist() : startingContainer.getContainer()) &&
			!(parent instanceof Form))
		{
			parentHierarchy.add(parent);
			parent = parent.getParent();
		}
		for (int i = parentHierarchy.size(); --i >= 0;)
		{
			ISupportChilds container = parentHierarchy.get(i);
			if (container instanceof CSSPositionLayoutContainer)
			{
				startingContainer = getApplication().getScriptEngine().getSolutionModifier().createResponsiveLayoutContainer((IJSParent< ? >)startingContainer,
					(CSSPositionLayoutContainer)container, false);
			}
			else if (container instanceof LayoutContainer)
			{
				startingContainer = getApplication().getScriptEngine().getSolutionModifier().createLayoutContainer((IJSParent< ? >)startingContainer,
					(LayoutContainer)container);
			}
			else
			{
				throw new RuntimeException("unexpected parent: " + container); //$NON-NLS-1$
			}
		}
		return (IJSParent)startingContainer;
	}

	void checkModification();

	IApplication getApplication();

	AbstractContainer getFlattenedContainer();

	AbstractContainer getContainer();

	int getNextId();
}
