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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.annotations.JSFunction;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
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
public abstract class JSBaseContainer<T extends AbstractContainer> implements IJSParent<T>, IBaseContainer
{
	private final IApplication application;
	private final AtomicInteger id = new AtomicInteger();

	public JSBaseContainer(IApplication application)
	{
		this.application = application;
	}

	public abstract void checkModification();

	public abstract AbstractContainer getContainer();

	public abstract AbstractContainer getFlattenedContainer();

	public int getNextId()
	{
		return id.incrementAndGet();
	}

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
		return createLayoutContainer(x, y, null, null);
	}

	protected JSLayoutContainer createLayoutContainer(int x, int y, String spec, JSONObject config)
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
							if (specName.contains(".")) specName = specName.split("\\.")[1];
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
				JSLayoutContainer jsLayoutContainer = application.getScriptEngine().getSolutionModifier().createLayoutContainer(this, layoutContainer);
				JSONObject conf = config;
				if (config == null)
				{
					Map<String, PackageSpecification<WebLayoutSpecification>> layoutSpecifications = WebComponentSpecProvider.getSpecProviderState()
						.getLayoutSpecifications();
					if (packageName != null && specName != null && layoutSpecifications != null &&
						layoutSpecifications.get(packageName) != null)
					{
						WebLayoutSpecification layoutSpec = layoutSpecifications.get(packageName)
							.getSpecification(specName);
						if (layoutSpec != null && (layoutSpec.getConfig() instanceof JSONObject || layoutSpec.getConfig() instanceof String))
						{
							conf = layoutSpec.getConfig() instanceof String ? new JSONObject((String)layoutSpec.getConfig())
								: ((JSONObject)layoutSpec.getConfig());
						}
					}
				}
				return configLayoutContainer(jsLayoutContainer, layoutContainer, conf);
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
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private JSLayoutContainer configLayoutContainer(JSLayoutContainer jsLayoutContainer, LayoutContainer layoutContainer, JSONObject config)
		throws Exception
	{
		if (config != null)
		{
			for (String key : config.keySet())
			{
				if ("children".equals(key))
				{
					JSONArray array = config.optJSONArray("children");
					for (int i = 0; i < array.length(); i++)
					{
						JSONObject jsonObject = array.getJSONObject(i);
						if (jsonObject.has("layoutName"))
						{
							jsLayoutContainer.createLayoutContainer(i + 1, i + 1, jsonObject.optString("layoutName"), jsonObject.optJSONObject("model"));
						}
						else if (jsonObject.has("componentName"))
						{
							jsLayoutContainer.newWebComponent(jsonObject.optString("componentName"));//TODO config web component
						}
					}
				}
				else if (key.equals("layoutName"))
				{
					layoutContainer.setSpecName(config.optString(key));
				}
				else
				{
					layoutContainer.putAttribute(key, config.optString(key));
				}
			}
		}
		return jsLayoutContainer;
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
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer newLayoutContainer(int position)
	{
		return createLayoutContainer(position, position, null, null);
	}

	/**
	 * Create a new layout container as the last child of its parent container.
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
	 * var container = form.newLayoutContainer();
	 * container.packageName = "12grid";
	 * container.specName = "row";
	 * @return the new layout container
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer newLayoutContainer()
	{
		int pos = getLastPosition();
		return createLayoutContainer(pos, pos, null, null);
	}

	/**
	 * Create a new layout container as the last child in its parent container.
	 * This method can only be used in responsive forms.
	 *
	 * @sample
	 * var container = form.newLayoutContainer(1, "12grid-row");
	 * container.newLayoutContainer(1, "column");
	 *
	 * @param spec a string of the form 'packageName-layoutName', or 'layoutName'
	 * @return the new layout container
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer newLayoutContainer(String spec) throws Exception
	{
		int pos = getLastPosition();
		return createLayoutContainer(pos, pos, spec, null);
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
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer newLayoutContainer(int position, String spec) throws Exception
	{
		return createLayoutContainer(position, position, spec, null);
	}

	/**
	 * Returns all JSLayoutContainers objects of this container.
	 * Does not return the inherited containers, use #getLayoutContainers(true) to get the inherited as well.
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
		return getLayoutContainers(false);
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
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return all JSLayoutContainers objects of this container
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer[] getLayoutContainers(boolean returnInheritedElements)
	{
		List<JSLayoutContainer> containers = new ArrayList<JSLayoutContainer>();
		AbstractContainer container = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<LayoutContainer> iterator = container.getLayoutContainers();
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
	 * Will receive a generated name.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var container = myForm.getLayoutContainer("row1")
	 * var bean = container.newWebComponent('mypackage-testcomponent',1);
	 *
	 * @param type the webcomponent name as it appears in the spec
	 * @param position the position of JSWebComponent object in its parent container
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent newWebComponent(String type, int position)
	{
		return newWebComponent(null, type, position);
	}

	/**
	 * Creates a new JSWebComponent (spec based component) object on the RESPONSIVE form,
	 * as the last component in its parent container.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var container = myForm.getLayoutContainer("row1")
	 * var bean = container.newWebComponent('bean','mypackage-testcomponent');
	 *
	 * @param name the specified name of the JSWebComponent object
	 * @param type the webcomponent name as it appears in the spec
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent newWebComponent(String name, String type)
	{
		return newWebComponent(name, type, getLastPosition());
	}

	/**
	 * Creates a new JSWebComponent (spec based component) object on the RESPONSIVE form.
	 * Will receive a generated name. Will be added as last position in container.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var container = myForm.getLayoutContainer("row1")
	 * var bean = container.newWebComponent('mypackage-testcomponent');
	 *
	 * @param type the webcomponent name as it appears in the spec
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent newWebComponent(String type)
	{
		return newWebComponent(null, type, getLastPosition());
	}

	private int getLastPosition()
	{
		int position = 0;
		Iterator<IPersist> components = getFlattenedContainer().getAllObjects();
		while (components.hasNext())
		{
			IPersist component = components.next();
			if (component instanceof ISupportBounds)
			{
				Point location = ((ISupportBounds)component).getLocation();
				if (location != null)
				{
					if (location.x > position)
					{
						position = location.x;
					}
					if (location.y > position)
					{
						position = location.y;
					}
				}
			}
		}
		return position + 1;
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
	 * Returns all JSWebComponents of this form/container.
	 * If this method is called on a form, then it will return all web components on that form.
	 * If the form is responsive, it will return the web components from all the containers.
	 * It does not return the inherited components, use #getWebComponents(true) to get the inherited as well.
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

	/**
	 * Returns a JSComponent that has the given name; if found it will be a JSField, JSLabel, JSButton, JSPortal, JSBean, JSWebComponent or JSTabPanel.
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var cmp = frm.getComponent("componentName");
	 * application.output("Component type and name: " + cmp);
	 *
	 * @param name the specified name of the component
	 *
	 * @return a JSComponent object (might be a JSField, JSLabel, JSButton, JSPortal, JSBean, JSWebComponent or JSTabPanel)
	 */
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public JSComponent< ? > getComponent(String name)
	{
		JSComponent< ? > comp = getWebComponent(name);
		if (comp != null) return comp;
		return null;
	}

	/**
	 * Removes a component (JSLabel, JSButton, JSField, JSPortal, JSBean, JSTabpanel, JSWebComponent) that has the given name. It is the same as calling "if(!removeLabel(name) &amp;&amp; !removeButton(name) ....)".
	 * Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX','db:/server1/parent_table',null,true,1000,750);
	 * var jsbutton = form.newButton('JSButton to delete',100,100,200,50,null);
	 * jsbutton.name = 'jsb';
	 * var jslabel = form.newLabel('JSLabel to delete',100,200,200,50,null);
	 * jslabel.name = 'jsl';
	 * jslabel.transparent = false;
	 * jslabel.background = 'green';
	 * var jsfield = form.newField('scopes.globals.myGlobalVariable',JSField.TEXT_FIELD,100,300,200,50);
	 * jsfield.name = 'jsf';
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('parent_table_id', '=', 'child_table_id');
	 * var jsportal = form.newPortal('jsp',relation,100,400,300,300);
	 * jsportal.newField('child_table_id',JSField.TEXT_FIELD,200,200,120);
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_id', JSField.TEXT_FIELD,10,10,100,20);
	 * var childTwo = solutionModel.newForm('childTwo','server1','other_table',null,false,400,300);
	 * childTwo.newField('some_table_id', JSField.TEXT_FIELD,10,10,100,100);
	 * var jstabpanel = form.newTabPanel('jst',450,30,620,460);
	 * jstabpanel.newTab('tab1','Child One',childOne,relation);
	 * jstabpanel.newTab('tab2','Child Two',childTwo);
	 * var jsmethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if ((form.removeComponent('jsb') == true) && (form.removeComponent('jsl') == true) && (form.removeComponent('jsf') == true) && (form.removeComponent('jsp') == true) & (form.removeComponent('jst') == true)) application.output('Components removed ok'); else application.output('Some component(s) could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove form components',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the component to be deleted
	 *
	 * @return true if component has been successfully deleted; false otherwise
	 */
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public boolean removeComponent(String name)
	{
		if (name == null) return false;
		if (removeWebComponent(name)) return true;
		return false;
	}

	/**
	 * Returns a array of all the JSComponents that a form has; they are of type JSField,JSLabel,JSButton,JSPortal,JSBean, JSWebComponent or JSTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]);
	 *
	 * @param returnInheritedElements boolean true to also return the elements from the parent form
	 * @return an array of all the JSComponents on the form.
	 */
	@JSFunction
	public JSComponent< ? >[] getComponents(boolean returnInheritedElements)
	{
		List<JSComponent< ? >> lst = new ArrayList<JSComponent< ? >>();
		lst.addAll(Arrays.asList(getWebComponents(returnInheritedElements)));
		return lst.toArray(new JSComponent[lst.size()]);
	}

	/**
	 * Returns a array of all the JSComponents that a form has; they are of type JSField,JSLabel,JSButton,JSPortal,JSBean, JSWebComponents or JSTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]);
	 *
	 * @return an array of all the JSComponents on the form.
	 */
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public JSComponent< ? >[] getComponents()
	{
		return getComponents(false);
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

	/**
	 * Returns the comment of this container.
	 *
	 * @sample
	 * var comment = solutionModel.getForm("my_form").getComment();
	 * application.output(comment);
	 *
	 * @return the comment of this container.
	 */
	@JSFunction
	public String getComment()
	{
		return getContainer().getComment();
	}
}
