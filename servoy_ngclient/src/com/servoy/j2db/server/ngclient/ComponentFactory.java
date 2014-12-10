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

package com.servoy.j2db.server.ngclient;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.Container;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.CustomJSONArrayType;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractPersistFactory;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ISupportTemplateValue;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * @author lvostinar
 *
 */
public class ComponentFactory
{
	public static WebFormComponent createComponent(IApplication application, IDataAdapterList dataAdapterList, FormElement fe, Container parentToAddTo)
	{
		String name = fe.getName();
		if (name != null)
		{
			IPersist persist = fe.getPersistIfAvailable();
			int access = 0;
			if (persist != null)
			{
				// don't add the component to the form ui if component is not visible due to security settings
				access = application.getFlattenedSolution().getSecurityAccess(persist.getUUID());
				if (!((access & IRepository.VIEWABLE) != 0)) return null;
			}

			// TODO anything to do here for custom special types?
			WebFormComponent webComponent = new WebFormComponent(name, fe, dataAdapterList);
			if (parentToAddTo != null) parentToAddTo.add(webComponent);

			WebComponentSpecification componentSpec = fe.getWebComponentSpec(false);

			for (String propName : fe.getRawPropertyValues().keySet())
			{
				if (componentSpec.getProperty(propName) == null) continue; //TODO this if should not be necessary. currently in the case of "printable" hidden property
				Object value = fe.getPropertyValueConvertedForWebComponent(propName, webComponent, (DataAdapterList)dataAdapterList);
				if (value == null) continue;
				fillProperty(value, fe.getPropertyValue(propName), componentSpec.getProperty(propName), webComponent);
			}

			// overwrite accessible
			if (persist != null && !((access & IRepository.ACCESSIBLE) != 0)) webComponent.setProperty("enabled", false);

			// TODO should this be a part of type conversions for handlers instead?
			for (String eventName : componentSpec.getHandlers().keySet())
			{
				Object eventValue = fe.getPropertyValue(eventName);
				if (eventValue instanceof String)
				{
					UUID uuid = UUID.fromString((String)eventValue);
					try
					{
						webComponent.add(eventName, ((AbstractPersistFactory)ApplicationServerRegistry.get().getLocalRepository()).getElementIdForUUID(uuid));
					}
					catch (RepositoryException e)
					{
						Debug.error(e);
					}
				}
				else if (eventValue instanceof Number && ((Number)eventValue).intValue() > 0)
				{
					webComponent.add(eventName, ((Number)eventValue).intValue());
				}
			}
			// just created, it should have no changes.
			webComponent.clearChanges();
			return webComponent;
		}
		return null;
	}

	protected static void fillProperty(Object propertyValue, Object formElementValue, PropertyDescription propertySpec, WebFormComponent component)
	{
		String propName = propertySpec.getName();
		if (propertyValue != null)
		{
			boolean templatevalue = true;
			if (propertySpec.getType() instanceof ISupportTemplateValue)
			{
				templatevalue = ((ISupportTemplateValue)propertySpec.getType()).valueInTemplate(formElementValue);
			}
			if (templatevalue)
			{
				component.setDefaultProperty(propName, propertyValue);
			}
			else
			{
				component.setProperty(propName, propertyValue);
			}
		}
	}

	// todo identity key? SolutionModel persist shouldn't be cached at all?
	private static ConcurrentMap<IPersist, FormElement> persistWrappers = new ConcurrentHashMap<>();
	private static ConcurrentMap<String, FlattenedSolution> globalFlattendSolutions = new ConcurrentHashMap<>();

	/**
	 * @param iterator
	 */
	public static List<FormElement> getFormElements(Iterator<IPersist> iterator, IServoyDataConverterContext context)
	{
		if (Boolean.valueOf(Settings.getInstance().getProperty("servoy.internal.reloadSpecsAllTheTime", "false")).booleanValue())
		{
			WebComponentSpecProvider.reload();
		}
		List<FormElement> lst = new ArrayList<>();
		while (iterator.hasNext())
		{
			IPersist persist = iterator.next();
			if (persist instanceof IFormElement)
			{
				lst.add(getFormElement((IFormElement)persist, context, null));
			}
		}
		return lst;
	}

	private static FlattenedSolution getSharedFlattenedSolution(FlattenedSolution fs)
	{
		FlattenedSolution flattenedSolution = globalFlattendSolutions.get(fs.getName());
		if (flattenedSolution == null)
		{
			try
			{
				flattenedSolution = new FlattenedSolution(true);
				flattenedSolution.setSolution(fs.getMainSolutionMetaData(), false, true,
					new AbstractActiveSolutionHandler(ApplicationServerRegistry.getService(IApplicationServer.class))
					{
						@Override
						public IRepository getRepository()
						{
							return ApplicationServerRegistry.get().getLocalRepository();
						}
					});
				FlattenedSolution alreadyCreated = globalFlattendSolutions.putIfAbsent(flattenedSolution.getName(), flattenedSolution);
				if (alreadyCreated != null)
				{
					flattenedSolution.close(null);
					flattenedSolution = alreadyCreated;
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException("Can't create FlattenedSolution for: " + fs, e);
			}
		}
		return flattenedSolution;
	}

	public static FormElement getFormElement(IFormElement formElement, IServoyDataConverterContext context, PropertyPath propertyPath)
	{
		return getFormElement(formElement, context.getSolution(), propertyPath, (context.getApplication() != null && context.getApplication().isInDesigner()));
	}

	public static FormElement getFormElement(IFormElement formElement, FlattenedSolution fs, PropertyPath propertyPath, final boolean designer)
	{

		// dont cache if solution model is used (media,valuelist,relations can be changed for a none changed element)
		if (designer || (fs.getSolutionCopy(false) != null))
		{
			if (propertyPath == null)
			{
				propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
			}
			if (formElement instanceof BodyPortal) return createBodyPortalFormElement((BodyPortal)formElement, fs, designer);
			else return new FormElement(formElement, fs, propertyPath, designer);
		}
		FormElement persistWrapper = persistWrappers.get(formElement);
		if (persistWrapper == null)
		{
			if (propertyPath == null)
			{
				propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
			}
			if (formElement instanceof BodyPortal) persistWrapper = createBodyPortalFormElement((BodyPortal)formElement, getSharedFlattenedSolution(fs),
				designer);
			else persistWrapper = new FormElement(formElement, getSharedFlattenedSolution(fs), propertyPath, false);
			FormElement existing = persistWrappers.putIfAbsent(formElement, persistWrapper);
			if (existing != null)
			{
				persistWrapper = existing;
			}
		}
		return persistWrapper;

	}

	private static FormElement createBodyPortalFormElement(BodyPortal listViewPortal, FlattenedSolution fs, final boolean isInDesginer)
	{
		Form form = listViewPortal.getForm();
		Part bodyPart = null;
		for (Part prt : Utils.iterate(form.getParts()))
		{
			if (prt.getPartType() == Part.BODY)
			{
				bodyPart = prt;
				break;
			}
		}
		if (bodyPart != null)
		{
			try
			{
				String name = "svy_lvp_" + form.getName();
				int startPos = form.getPartStartYPos(bodyPart.getID());
				int endPos = bodyPart.getHeight();
				int bodyheight = endPos - startPos;
				boolean fillsWidth = fillsWidth(form);

				JSONObject portal = new JSONObject();
				portal.put("name", name);
				portal.put("multiLine", !listViewPortal.isTableview());
				portal.put("rowHeight", !listViewPortal.isTableview() ? bodyheight : getRowHeight(form));
				portal.put("scrollbars", form.getScrollbars());
				if (listViewPortal.isTableview())
				{
					int headerHeight = 30;
					if (form.hasPart(Part.HEADER))
					{
						headerHeight = 0;
					}
					portal.put("headerHeight", headerHeight);
				}

				portal.put("anchors", listViewPortal.isTableview() ? (fillsWidth ? IAnchorConstants.ALL
					: (IAnchorConstants.NORTH + IAnchorConstants.WEST + IAnchorConstants.SOUTH)) : IAnchorConstants.ALL);
				JSONObject location = new JSONObject();
				location.put("x", 0);
				location.put("y", isInDesginer ? startPos : 0);
				portal.put("location", location);
				JSONObject size = new JSONObject();
				size.put("width", (listViewPortal.isTableview() && !fillsWidth) ? getGridWidth(form) : form.getWidth());
				size.put("height", bodyheight);
				portal.put("size", size);
				portal.put("visible", listViewPortal.getVisible());
				portal.put("enabled", listViewPortal.getEnabled());
				portal.put("childElements", new JSONArray()); // empty contents; will be updated afterwards directly with form element values for components

				JSONObject relatedFoundset = new JSONObject();
				relatedFoundset.put("foundsetSelector", "");
				portal.put("relatedFoundset", relatedFoundset);

				PropertyPath propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
				FormElement portalFormElement = new FormElement("servoydefault-portal", portal, form, name, fs, propertyPath, isInDesginer);
				PropertyDescription pd = portalFormElement.getWebComponentSpec().getProperties().get("childElements");
				if (pd != null) pd = ((CustomJSONArrayType< ? , ? >)pd.getType()).getCustomJSONTypeDefinition();
				if (pd == null)
				{
					Debug.error(new RuntimeException("Cannot find component definition special type to use for portal."));
					return null;
				}
				ComponentPropertyType type = ((ComponentPropertyType)pd.getType());

				Map<String, Object> portalFormElementProperties = new HashMap<>(portalFormElement.getRawPropertyValues());
				portalFormElementProperties.put("offsetY", startPos);
				portalFormElementProperties.put("partHeight", bodyPart.getHeight());
				// now put real child component form element values in "childElements"
				Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				List<Object> children = new ArrayList<>(); // contains actually ComponentTypeFormElementValue objects
				propertyPath.add(portalFormElement.getName());
				propertyPath.add("childElements");
				while (it.hasNext())
				{
					IPersist persist = it.next();
					if (persist instanceof IFormElement)
					{
						Point loc = ((IFormElement)persist).getLocation();
						if (startPos <= loc.y && endPos >= loc.y)
						{
							if (listViewPortal.isTableview() && persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null) continue;
							propertyPath.add(children.size());
							FormElement fe = ComponentFactory.getFormElement((IFormElement)persist, fs, propertyPath, isInDesginer);
							children.add(type.getFormElementValue(null, pd, propertyPath, fe, fs));
							propertyPath.backOneLevel();
						}
					}
				}
				propertyPath.backOneLevel();
				propertyPath.backOneLevel();
				portalFormElementProperties.put("childElements", children.toArray());
				portalFormElement.updatePropertyValuesDontUse(portalFormElementProperties);

				return portalFormElement;
			}
			catch (JSONException ex)
			{
				Debug.error("Cannot create list view portal component", ex);
			}
		}

		return null;
	}

	private static int getRowHeight(Form form)
	{
		int rowHeight = 0;
		Part part = getBodyPart(form);
		int startPos = form.getPartStartYPos(part.getID());
		int endPos = part.getHeight();
		Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (it.hasNext())
		{
			IPersist persist = it.next();
			if (persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null) continue;
			if (persist instanceof BaseComponent)
			{
				BaseComponent bc = (BaseComponent)persist;
				Point location = bc.getLocation();
				if (startPos <= location.y && endPos >= location.y)
				{
					if (rowHeight == 0)
					{
						rowHeight = bc.getSize().height;
						break;
					}
				}
			}
		}

		return rowHeight == 0 ? 20 : rowHeight;
	}

	private static int getGridWidth(Form form)
	{
		int rowWidth = 0;
		Part part = getBodyPart(form);
		int startPos = form.getPartStartYPos(part.getID());
		int endPos = part.getHeight();
		Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (it.hasNext())
		{
			IPersist persist = it.next();
			if (persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null) continue;
			if (persist instanceof BaseComponent)
			{
				BaseComponent bc = (BaseComponent)persist;
				Point location = bc.getLocation();
				if (startPos <= location.y && endPos >= location.y)
				{
					rowWidth += bc.getSize().width + 0.5;//+borders
				}
			}
		}
		return rowWidth;
	}

	private static boolean fillsWidth(Form form)
	{
		if ((form.getScrollbars() & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
		{
			Part part = getBodyPart(form);
			int startPos = form.getPartStartYPos(part.getID());
			int endPos = part.getHeight();
			Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			while (it.hasNext())
			{
				IPersist persist = it.next();
				if (persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null) continue;
				if (persist instanceof BaseComponent)
				{
					BaseComponent bc = (BaseComponent)persist;
					if ((bc.getAnchors() & (IAnchorConstants.WEST + IAnchorConstants.EAST)) == (IAnchorConstants.WEST + IAnchorConstants.EAST))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public static Part getBodyPart(Form form)
	{
		for (Part prt : Utils.iterate(form.getParts()))
		{
			if (prt.getPartType() == Part.BODY)
			{
				return prt;
			}
		}
		return null;
	}

	public static String getMarkupId(String formName, String elementName)
	{
		return Utils.calculateMD5HashBase16(formName + elementName);
	}

	public static void reload()
	{
		persistWrappers.clear();
		for (FlattenedSolution fs : globalFlattendSolutions.values())
		{
			fs.close(null);
		}
		globalFlattendSolutions.clear();
		WebComponentSpecProvider.reload();
	}

}
