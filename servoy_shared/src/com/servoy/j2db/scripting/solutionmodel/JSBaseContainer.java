/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;


/**
 * @author lvostinar
 *
 */
public abstract class JSBaseContainer<T extends AbstractContainer> implements IJSParent<T>
{
	private final IApplication application;

	public JSBaseContainer(IApplication application)
	{
		this.application = application;
	}

	public abstract void checkModification();

	public abstract AbstractContainer getContainer();

	public abstract AbstractContainer getFlattenedContainer();

	/**
	 * Create a new layout container. The location is used to determine the generated order in html markup.
	 *
	 * @deprecated use newLayoutContainer(position) instead
	 * @sample
	 * var container = form.newLayoutContainer(0,0);
	 * @param x location x
	 * @param y location y
	 * @return the new layout container
	 */
	@Deprecated
	@JSFunction
	public JSLayoutContainer newLayoutContainer(int x, int y)
	{
		return createLayoutContainer(x, y, null);
	}

	protected JSLayoutContainer createLayoutContainer(int x, int y, String spec)
	{
		checkModification();
		try
		{
			AbstractContainer container = getContainer();
			Form form = (Form)container.getAncestor(IRepository.FORMS);
			if (form.isResponsiveLayout())
			{
				String packageName = null;
				String specName = null;
				if (spec != null)
				{
					String[] split = spec.split("-");
					if (split.length == 1)
					{
						specName = spec.trim();
					}
					else if (split.length == 2)
					{
						specName = split[1].trim();
						packageName = split[0].trim();
					}
					else
					{
						Debug.warn("Illegal spec given: " + spec);
					}

				}

				if (specName == null || packageName == null)
				{
					if (container instanceof LayoutContainer)
					{
						LayoutContainer parent = (LayoutContainer)(getContainer());
						packageName = parent.getPackageName();
						if (specName == null && parent.getAllowedChildren() != null && !parent.getAllowedChildren().isEmpty())
						{
							specName = parent.getAllowedChildren().get(0);
						}
					}
				}
				if (specName == null)
				{
					//the parent container could be a layout container or the form
					//check if we have a sibling of the container we want to create
					LayoutContainer sibling = container.getLayoutContainers().hasNext() ? container.getLayoutContainers().next() : null;
					if (sibling != null)
					{
						packageName = sibling.getPackageName();
						specName = sibling.getSpecName();
					}
				}
				LayoutContainer layoutContainer = getContainer().createNewLayoutContainer();
				layoutContainer.setLocation(new Point(x, y));
				layoutContainer.setPackageName(packageName);
				layoutContainer.setSpecName(specName);
				return application.getScriptEngine().getSolutionModifier().createLayoutContainer(this, layoutContainer);
			}
			else
			{
				//check if form was just created with the solution model and suggest correct method to use
				if (application.getFlattenedSolution().getSolutionCopy().getAllObjectsAsList().contains(form) &&
					!application.getSolution().getAllObjectsAsList().contains(form))
				{
					throw new RuntimeException(
						"Form " + form.getName() + " is not a responsive form, cannot add a layout container on it. Please use solutionModel.newForm('" +
							form.getName() + "',true) to create a responsive form.");
				}

				throw new RuntimeException("Form " + form.getName() +
					" is not responsive, cannot add a layout container on it. Please use a responsive form or create a responsive form with solutionModel.newForm(formName, true);");
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a new layout container. The position is used to determine the generated order in html markup.
	 * This method can only be used in responsive forms.
	 *
	 * If you want to use default values and so on from a layout package (like 12grid) or if you use the solution model
	 * to create a form that is saved back into the workspace (servoyDeveloper.save(form)) then you have to set the
	 * packageName and specName properties. So that it works later on in the designer.
	 *
	 * If the packageName and specName are not provided, then:
	 *    the packageName is the same as for the parent container
	 *    the specName is the first allowed child defined in the specification of the parent container
	 *
	 * If the specification of the parent container does not defined allowed children, then if it is not empty
	 *    the packageName and the specName are copied from the first child layout container.
	 * @sample
	 * var container = form.newLayoutContainer(1);
	 * container.packageName = "12grid";
	 * container.specName = "row";
	 * @param position the position of JSWebComponent object in its parent container
	 * @return the new layout container
	 */
	@JSFunction
	public JSLayoutContainer newLayoutContainer(int position)
	{
		return createLayoutContainer(position, position, null);
	}

	/**
	 * Create a new layout container. The position is used to determine the generated order in html markup.
	 * This method can only be used in responsive forms.
	 *
	 * @sample
	 * var container = form.newLayoutContainer(1, "12grid-row");
	 * container.newLayoutContainer(1, "column");
	 *
	 * @param position the position of JSWebComponent object in its parent container
	 * @param spec a string of the form 'packageName-layoutName', or 'layoutName'
	 * @return the new layout container
	 *
	 */
	@JSFunction
	public JSLayoutContainer newLayoutContainer(int position, String spec) throws Exception
	{
		return createLayoutContainer(position, position, spec);
	}

	/**
	 * Returns all JSLayoutContainers objects of this container
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var containers = frm.getLayoutContainers();
	 * for (var c in containers)
	 * {
	 * 		var fname = containers[c].name;
	 * 		application.output(fname);
	 * }
	 *
	 * @return all JSLayoutContainers objects of this container
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer[] getLayoutContainers()
	{
		List<JSLayoutContainer> containers = new ArrayList<JSLayoutContainer>();
		Iterator<LayoutContainer> iterator = getContainer().getLayoutContainers();
		while (iterator.hasNext())
		{
			containers.add(application.getScriptEngine().getSolutionModifier().createLayoutContainer(this, iterator.next()));
		}
		return containers.toArray(new JSLayoutContainer[containers.size()]);
	}

	/**
	 * Returns a JSLayoutContainer that has the given name of this container.
	 * Use findLayoutContainer() method to find a JSLayoutContainter through the hierarchy
	 *
	 * @sample
	 * var container = myForm.getLayoutContainer("row1");
	 * application.output(container.name);
	 *
	 * @param name the specified name of the container
	 *
	 * @return a JSLayoutContainer object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer getLayoutContainer(String name)
	{
		if (name == null) return null;

		Iterator<LayoutContainer> containers = getFlattenedContainer().getLayoutContainers();
		while (containers.hasNext())
		{
			LayoutContainer container = containers.next();
			if (name.equals(container.getName()))
			{
				return application.getScriptEngine().getSolutionModifier().createLayoutContainer(this, container);
			}
		}
		return null;
	}

	/**
	 * Returns a JSLayoutContainer that has the given name throughout the whole form hierarchy.
	 *
	 * @sample
	 * var container = myForm.findLayoutContainer("row1");
	 * application.output(container.name);
	 *
	 * @param name the specified name of the container
	 *
	 * @return a JSLayoutContainer object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer findLayoutContainer(final String name)
	{
		if (name == null) return null;

		return (JSLayoutContainer)getFlattenedContainer().acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof LayoutContainer && name.equals(((LayoutContainer)o).getName()))
				{
					LayoutContainer lc = (LayoutContainer)o;
					return application.getScriptEngine().getSolutionModifier().createLayoutContainer(getCorrectIJSParent(JSBaseContainer.this, lc), lc);
				}
				return o instanceof ISupportFormElements ? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}


		});
	}


	/**
	 * Creates a new JSWebComponent (spec based component) object on the form.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var bean = form.newWebComponent('bean','mypackage-testcomponent',200,200,300,300);
	 * forms['newForm1'].controller.show();
	 *
	 * @param name the specified name of the JSWebComponent object
	 * @param type the webcomponent name as it appears in the spec
	 * @param x the horizontal "x" position of the JSWebComponent object in pixels
	 * @param y the vertical "y" position of the JSWebComponent object in pixels
	 * @param width the width of the JSWebComponent object in pixels
	 * @param height the height of the JSWebComponent object in pixels
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent newWebComponent(String name, String type, int x, int y, int width, int height)
	{
		checkModification();
		try
		{
			WebComponent webComponent = getContainer().createNewWebComponent(IdentDocumentValidator.checkName(name), type);
			CSSPositionUtils.setSize(webComponent, width, height);
			CSSPositionUtils.setLocation(webComponent, x, y);
			return createWebComponent(this, webComponent, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new JSWebComponent (spec based component) object on the RESPONSIVE form.
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
	public JSWebComponent newWebComponent(String name, String type, int position)
	{
		checkModification();
		try
		{
			AbstractContainer container = getContainer();
			Form form = (Form)container.getAncestor(IRepository.FORMS);
			if (form.isResponsiveLayout())
			{
				WebComponent webComponent = container.createNewWebComponent(IdentDocumentValidator.checkName(name), type);
				webComponent.setLocation(new Point(position, position));
				return createWebComponent(this, webComponent, application, true);
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
	 * Returns a JSWebComponent that has the given name that is a child of this layout container.
	 * Use findWebComponent() to find a webcomponent through the hierarchy
	 *
	 * @sample
	 * var btn = myForm.getWebComponent("mycomponent");
	 * application.output(mybean.typeName);
	 *
	 * @param name the specified name of the web component
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent getWebComponent(String name)
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
					return createWebComponent(getCorrectIJSParent(this, webComponent), webComponent, application, false);
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
	 * Returns a JSWebComponent that has the given name through the whole hierarchy of JSLayoutContainers
	 *
	 * @sample
	 * var btn = myForm.findWebComponent("mycomponent");
	 * application.output(mybean.typeName);
	 *
	 * @param name the specified name of the web component
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent findWebComponent(final String name)
	{
		if (name == null) return null;

		return (JSWebComponent)getFlattenedContainer().acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof WebComponent && name.equals(((WebComponent)o).getName()))
				{
					return createWebComponent(getCorrectIJSParent(JSBaseContainer.this, o), (WebComponent)o, application, false);
				}
				return o instanceof ISupportFormElements ? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});
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
	public boolean removeWebComponent(String name)
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
	public JSWebComponent[] getWebComponents(boolean returnInheritedElements)
	{
		List<JSWebComponent> webComponents = new ArrayList<JSWebComponent>();
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<WebComponent> iterator = form2use.getWebComponents();
		while (iterator.hasNext())
		{
			WebComponent webComponent = iterator.next();
			webComponents.add(createWebComponent(getCorrectIJSParent(this, webComponent), webComponent, application, false));
		}
		return webComponents.toArray(new JSWebComponent[webComponents.size()]);
	}

	/**
	 * When creating a JSWebComponent from a who-knows-how-deeply-nested (in case of responsive forms) webComponent in a form and we only know the form,
	 * then we need to get step by step the solution model objects in order to use the correct direct parent for the child SM object creation.
	 *
	 * @param startingContainer
	 * @param possiblyNestedChild nested child component
	 * @return the direct parent (IJSParent) of the given webComponent.
	 */
	private IJSParent< ? > getCorrectIJSParent(JSBaseContainer< ? > startingContainer, IPersist possiblyNestedChild)
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
			if (container instanceof LayoutContainer)
			{
				startingContainer = application.getScriptEngine().getSolutionModifier().createLayoutContainer((IJSParent< ? >)startingContainer,
					(LayoutContainer)container);
			}
			else
			{
				throw new RuntimeException("unexpected parent: " + container); //$NON-NLS-1$
			}
		}
		return startingContainer;
	}

	/**
	 * Returns all JSWebComponents of this form/container.
	 * If this method is called on a form, then it will return all web components on that form.
	 * If the form is responsive, it will return the web components from all the containers.
	 *
	 * @sample
	 * var webComponents = myForm.getWebComponents();
	 * for (var i in webComponents)
	 * {
	 * 	if (webComponents[i].name != null)
	 * 		application.output(webComponents[i].name);
	 * }
	 *
	 * @return the list of all JSWebComponent on this forms
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent[] getWebComponents()
	{
		return getWebComponents(false);
	}

	public static JSWebComponent createWebComponent(IJSParent< ? > parent, WebComponent baseComponent, IApplication application, boolean isNew)
	{
		if (application.getApplicationType() == IApplication.NG_CLIENT ||
			Utils.getAsBoolean(application.getRuntimeProperties().get("JSUnit")))
		{
			return new JSNGWebComponent(parent, baseComponent, application, isNew);
		}
		else
		{
			return new JSWebComponent(parent, baseComponent, application, isNew);
		}
	}
}
