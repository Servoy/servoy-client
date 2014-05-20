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

package com.servoy.j2db.server.ngclient;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sablo.specification.PropertyDescription;
import org.sablo.websocket.ConversionLocation;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.property.ComponentTypeImpl;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Form element that is based on a persist.
 * 
 * @author acostescu
 */
class PersistBasedFormElementImpl
{
	private final IPersist persist;
	private final FormElement formElement;

	PersistBasedFormElementImpl(IPersist persist, FormElement formElement)
	{
		this.persist = persist;
		this.formElement = formElement;
	}

	public Form getForm()
	{
		if (persist instanceof Form) return (Form)persist;
		ISupportChilds parent = persist.getParent();
		while (parent != null && !(parent instanceof Form))
		{
			parent = parent.getParent();
		}
		return (Form)parent;
	}

	public Map<String, Object> getConvertedProperties(IDataConverterContext context, Map<String, PropertyDescription> specProperties)
	{
		if (persist instanceof Bean)
		{
			String customJSONString = ((Bean)persist).getBeanXML();
			if (customJSONString != null)
			{
				Map<String, Object> jsonMap = getConvertedPropertiesMap(((AbstractBase)persist).getPropertiesMap(), context);
				jsonMap.remove(StaticContentSpecLoader.PROPERTY_BEANXML); // this is handled separately as NG component definition
				try
				{
					JSONObject jsonProperties = new JSONObject(customJSONString);
					formElement.getConvertedJSONDefinitionProperties(context, specProperties, jsonMap, formElement.getWebComponentSpec().getHandlers(),
						jsonProperties);
				}
				catch (Exception ex)
				{
					Debug.error("Error while parsing bean design json", ex);
					jsonMap.put("text", "Error while parsing bean design json(bean not supported in NGClient?): " + persist);
				}
				return jsonMap;
			}
			else
			{
				Map<String, Object> defaultProperties = new HashMap<String, Object>();
				defaultProperties.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), ((Bean)persist).getSize());
				defaultProperties.put("text", "Bean not supported in NGClient: " + persist);
				return defaultProperties;
			}
		}
		else if (persist instanceof AbstractBase)
		{
			Map<String, Object> map = getConvertedPropertiesMap(getFlattenedPropertiesMap(), context);
			if (persist instanceof Field && ((Field)persist).getDisplayType() == Field.MULTISELECT_LISTBOX)
			{
				map.put("multiselectListbox", Boolean.TRUE);
			}
			else if (persist instanceof TabPanel)
			{
				convertFromTabPanelToNGProperties((IFormElement)persist, context, map);
			}
			else if (persist instanceof Portal)
			{
				convertFromPortalToNGProperties((IFormElement)persist, context, map, specProperties);
			}
			return map;
		}
		else
		{
			return Collections.emptyMap();
		}
	}

	/**
	 * AbstractBase.getPropertiesMap() returns for format,borderType  etc the STRING representation instead of the high level class representation used in ngClient
	 * Converts string representation to high level Class representation of properties
	 *
	 * Initially only for border
	 */
	private Map<String, Object> getConvertedPropertiesMap(Map<String, Object> propertiesMap, final IDataConverterContext context)
	{
		Map<String, Object> convPropertiesMap = new HashMap<>();
		for (String pv : propertiesMap.keySet())
		{
			Object val = propertiesMap.get(pv);
			if (val == null) continue;
			switch (pv)
			{
				case com.servoy.j2db.persistence.IContentSpecConstants.PROPERTY_BORDERTYPE : //PropertyType.border.getType
					convPropertiesMap.put(pv, ComponentFactoryHelper.createBorder((String)val));
					break;
				case com.servoy.j2db.persistence.IContentSpecConstants.PROPERTY_FONTTYPE : //PropertyType.font.getType
					convPropertiesMap.put(pv, PersistHelper.createFont((String)val));
					break;
				case com.servoy.j2db.persistence.IContentSpecConstants.PROPERTY_ROLLOVERIMAGEMEDIAID :
				case com.servoy.j2db.persistence.IContentSpecConstants.PROPERTY_IMAGEMEDIAID :
				{
					Media media = context.getSolution().getMedia(Utils.getAsInteger(val));
					if (media != null)
					{
						String url = "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + media.getRootObject().getName() + "/" +
							media.getName();
						Dimension imageSize = ImageLoader.getSize(media.getMediaData());
						boolean paramsAdded = false;
						if (imageSize != null)
						{
							paramsAdded = true;
							url += "?imageWidth=" + imageSize.width + "&imageHeight=" + imageSize.height;
						}
						if (context.getApplication() != null)
						{
							Solution sc = context.getSolution().getSolutionCopy(false);
							if (sc != null && sc.getMedia(media.getName()) != null)
							{
								if (paramsAdded) url += "&";
								else url += "?";
								url += "uuid=" + context.getApplication().getWebsocketSession().getUuid() + "&lm:" + sc.getLastModifiedTime();
							}
						}
						convPropertiesMap.put(pv, url);
					}
					else
					{
						Debug.log("media " + val + " not found for component: " + persist);
					}
					break;
				}

				default :
					convPropertiesMap.put(pv, val);
					break;
			}
		}

		return convPropertiesMap;
	}

	Map<String, Object> getFlattenedPropertiesMap()
	{
		IPersist p = persist;
		if (p instanceof IFlattenedPersistWrapper)
		{
			p = ((IFlattenedPersistWrapper)p).getWrappedPersist();
		}
		if (p instanceof ISupportExtendsID)
		{
			Map<String, Object> map = new HashMap<String, Object>();
			List<AbstractBase> hierarchy = PersistHelper.getOverrideHierarchy((ISupportExtendsID)p);
			for (int i = hierarchy.size() - 1; i >= 0; i--)
			{
				map.putAll(hierarchy.get(i).getPropertiesMap());
			}
			return map;
		}
		return ((AbstractBase)p).getPropertiesMap();
	}

	private void convertFromTabPanelToNGProperties(IFormElement persist, final IDataConverterContext context, Map<String, Object> map)
	{
		ArrayList<Map<String, Object>> tabList = new ArrayList<>();
		// add the tabs.
		Iterator<IPersist> tabs = ((TabPanel)persist).getTabs();
		boolean active = true;
		while (tabs.hasNext())
		{
			Map<String, Object> tabMap = new HashMap<>();
			Tab tab = (Tab)tabs.next();
			tabMap.put("text", tab.getText());
			tabMap.put("relationName", tab.getRelationName());
			tabMap.put("active", Boolean.valueOf(active));
			tabMap.put("foreground", tab.getForeground());
			tabMap.put("name", tab.getName());
			tabMap.put("mnemonic", tab.getMnemonic());
			int containsFormID = tab.getContainsFormID();
			// TODO should this be resolved way later on?
			// if solution model then this form can change..
			Form form = context.getSolution().getForm(containsFormID);
			tabMap.put("containsFormId", form.getName());

			tabList.add(tabMap);
			active = false;
		}
		map.put("tabs", tabList);
	}

	private void convertFromPortalToNGProperties(IFormElement portalPersist, final IDataConverterContext context, Map<String, Object> map,
		Map<String, PropertyDescription> specProperties)
	{
		try
		{
			map.remove("relationName");

			Portal portal = ((Portal)portalPersist);
			JSONObject relatedFoundset = new JSONObject();
			relatedFoundset.put("foundsetSelector", portal.getRelationName());

			// get property type 'foundset'
			PropertyDescription pd = specProperties.get("relatedFoundset");
			if (pd == null)
			{
				Debug.error(new RuntimeException("Cannot find foundset special type to use for portal."));
				return;
			}
			else
			{
				map.put("relatedFoundset", NGClientForJsonConverter.toJavaObject(relatedFoundset, pd, context, ConversionLocation.DESIGN, null));
			}

//			components: 'component[]',
//			component: {
//				definition: 'componentDef',
//				(...)
//			},
			JSONArray componentJSONs = new JSONArray();
			JSONObject componentJSON;
			List<IPersist> components = ComponentFactory.sortElementsOnPositionAndGroup(portal.getAllObjectsAsList());
			for (IPersist component : components)
			{
				if (component instanceof IFormElement)
				{
					// generate the NG definition of this persist and put it in there;
					JSONStringer jsonWriter = new JSONStringer();
					FormElement nfe = new FormElement((IFormElement)component, context);
					JSONUtils.toJSONValue(jsonWriter, nfe.getProperties(), NGClientForJsonConverter.INSTANCE, ConversionLocation.DESIGN);

					componentJSON = new JSONObject();
					componentJSON.put(ComponentTypeImpl.TYPE_NAME_KEY, nfe.getTypeName());
					componentJSON.put(ComponentTypeImpl.DEFINITION_KEY, new JSONObject(jsonWriter.toString()));
					componentJSONs.put(componentJSON);
				}
			}

			// get property type 'component definition'
			pd = specProperties.get("childElements");
			if (pd == null)
			{
				Debug.error(new RuntimeException("Cannot find component definition special type to use for portal."));
				return;
			}
			else
			{
				map.put("childElements", NGClientForJsonConverter.toJavaObject(componentJSONs, pd, context, ConversionLocation.DESIGN, null));
			}
		}
		catch (IllegalArgumentException e)
		{
			Debug.error(e);
			return;
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return;
		}
	}

	public boolean isLegacy()
	{
		return !(persist instanceof Bean);
	}

	public boolean isForm()
	{
		return persist instanceof Form;
	}

	IPersist getPersist()
	{
		return persist;
	}

}
