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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.AggregatedPropertyType;
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
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.property.ComponentTypeValue;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;

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

	public Map<String, Object> getConvertedProperties(IServoyDataConverterContext context, Map<String, PropertyDescription> specProperties)
	{
		if (persist instanceof Bean)
		{
			String customJSONString = ((Bean)persist).getBeanXML();
			if (customJSONString != null)
			{
				// convert from persist design-time value (which might be non-json) to the expected value
				Map<String, Object> jsonMap = getConvertedPropertiesMap(((AbstractBase)persist).getPropertiesMap(), context, specProperties);

				jsonMap.remove(StaticContentSpecLoader.PROPERTY_BEANXML); // this is handled separately as NG component definition
				try
				{
					// add beanXML (which is actually a JSON string here) defined properties to the map
					JSONObject jsonProperties = new JSONObject(customJSONString);
					formElement.getConvertedJSONDefinitionProperties(context, specProperties, jsonMap, formElement.getWebComponentSpec().getHandlers(),
						jsonProperties);
				}
				catch (Exception ex)
				{
					Debug.error("Error while parsing bean design json", ex);
					jsonMap.put("error", "Error while parsing bean design json(bean not supported in NGClient?): " + persist);
				}
				return jsonMap;
			}
			else
			{
				Map<String, Object> defaultProperties = new HashMap<String, Object>();
				defaultProperties.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), ((Bean)persist).getSize());
				defaultProperties.put(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), ((Bean)persist).getName());
				defaultProperties.put("error", "Bean not supported in NGClient: " + persist);
				return defaultProperties;
			}
		}
		else if (persist instanceof AbstractBase)
		{
			Map<String, Object> map = getConvertedPropertiesMap(getFlattenedPropertiesMap(), context, specProperties);
			if (persist instanceof Field && ((Field)persist).getDisplayType() == Field.MULTISELECT_LISTBOX)
			{
				map.put("multiselectListbox", Boolean.TRUE);
			}
			else if (persist instanceof TabPanel)
			{
				convertFromTabPanelToNGProperties((IFormElement)persist, context, map, specProperties);
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
	 * @param specProperties
	 */
	private Map<String, Object> getConvertedPropertiesMap(Map<String, Object> propertiesMap, final IServoyDataConverterContext context,
		Map<String, PropertyDescription> specProperties)
	{
		// it is a bit strange here as from Persist we get
		// 1. primitive values (some of which might need no conversion and some of which in NG client world need to be wrapped - for example TagStringPropertyType)
		// 2. actual values (like Color) - that might need no conversion or might need to be wrapped
		// 3. (serialized to String) values like borders (see ComponentFactoryHelper.createBorder()) that need to be converted to actual values and maybe then wrapped if needed for NG
		// but then we need to set in the NG FormElement properties map only actual values that can be serialized back to JSON (for form template generation)
		Map<String, Object> convPropertiesMap = new HashMap<>();
		for (String pv : propertiesMap.keySet())
		{
			Object val = propertiesMap.get(pv);
			if (val == null) continue;
			// the ones in this switch are converted from
			switch (pv)
			{
				case com.servoy.j2db.persistence.IContentSpecConstants.PROPERTY_BORDERTYPE : //PropertyType.border.getType
					val = ComponentFactoryHelper.createBorder((String)val);
					break;
				case com.servoy.j2db.persistence.IContentSpecConstants.PROPERTY_FONTTYPE : //PropertyType.font.getType
					val = PersistHelper.createFont((String)val);
					break;
				default :
					// use default
					break;
			}

			// see if it needs wrapping/further conversion
			val = NGClientForJsonConverter.toJavaObject(val, specProperties.get(pv), context, ConversionLocation.DESIGN, null, false);

			convPropertiesMap.put(pv, val);
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

	private void putAndConvertProperty(String propName, Object val, Map<String, Object> map, final IServoyDataConverterContext context, PropertyDescription desc)
	{
		map.put(propName, NGClientForJsonConverter.toJavaObject(val, desc, context, ConversionLocation.DESIGN, null, true));
	}

	private void convertFromTabPanelToNGProperties(IFormElement persist, final IServoyDataConverterContext context, Map<String, Object> map,
		Map<String, PropertyDescription> specProperties)
	{
		ArrayList<Map<String, Object>> tabList = new ArrayList<>();
		// add the tabs.
		Iterator<IPersist> tabs = ((TabPanel)persist).getTabs();
		putAndConvertProperty("tabIndex", 1, map, context, specProperties.get("tabIndex"));
		PropertyDescription tabSpecProperties = specProperties.get("tabs");
		boolean active = true;
		while (tabs.hasNext())
		{
			Map<String, Object> tabMap = new HashMap<>();
			Tab tab = (Tab)tabs.next();
			putAndConvertProperty("text", tab.getText(), tabMap, context, tabSpecProperties.getProperty("text"));
			putAndConvertProperty("relationName", tab.getRelationName(), tabMap, context, tabSpecProperties.getProperty("relationName"));
			putAndConvertProperty("active", Boolean.valueOf(active), tabMap, context, tabSpecProperties.getProperty("active"));
			putAndConvertProperty("foreground", tab.getForeground(), tabMap, context, tabSpecProperties.getProperty("foreground"));
			putAndConvertProperty("name", tab.getName(), tabMap, context, tabSpecProperties.getProperty("name"));
			putAndConvertProperty("mnemonic", tab.getMnemonic(), tabMap, context, tabSpecProperties.getProperty("mnemonic"));
			int containsFormID = tab.getContainsFormID();
			// TODO should this be resolved way later on?
			// if solution model then this form can change..
			Form form = context.getSolution().getForm(containsFormID);
			putAndConvertProperty("containsFormId", form.getName(), tabMap, context, tabSpecProperties.getProperty("containsFormId"));
			putAndConvertProperty("disabled", false, tabMap, context, tabSpecProperties.getProperty("disabled"));
			int orient = ((TabPanel)persist).getTabOrientation();
			if (orient != TabPanel.SPLIT_HORIZONTAL && orient != TabPanel.SPLIT_VERTICAL)
			{
				putAndConvertProperty("imageMediaID", "", tabMap, context, tabSpecProperties.getProperty("imageMediaID"));
				int tabMediaID = tab.getImageMediaID();
				if (tabMediaID > 0)
				{
					Media tabMedia = context.getSolution().getMedia(tabMediaID);
					if (tabMedia != null)
					{
						putAndConvertProperty("imageMediaID", "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" +
							context.getSolution().getName() + "/" + tabMedia.getName(), tabMap, context, tabSpecProperties.getProperty("imageMediaID"));
					}
				}
			}
			tabList.add(tabMap);
			active = false;
		}
		map.put("tabs", tabList);
	}

	private void convertFromPortalToNGProperties(IFormElement portalPersist, final IServoyDataConverterContext context, Map<String, Object> map,
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
				map.put("relatedFoundset", NGClientForJsonConverter.toJavaObject(relatedFoundset, pd, context, ConversionLocation.DESIGN, null, true));
			}

//			components: 'component[]',
//			component: {
//				definition: 'componentDef',
//				(...)
//			},
			JSONArray componentJSONs = new JSONArray();
			List<IPersist> components = ComponentFactory.sortElementsOnPositionAndGroup(portal.getAllObjectsAsList());
			for (IPersist component : components)
			{
				if (component instanceof IFormElement)
				{
					FormElement nfe = new FormElement((IFormElement)component, context);

					// remove the name of relation prefix from child dataproviders as it only stands in the way later on...
					List<String> dataProviders = WebGridFormUI.getWebComponentPropertyType(nfe.getWebComponentSpec(), DataproviderPropertyType.INSTANCE);
					String relationPrefix = portal.getRelationName() + '.';
					Map<String, Object> elementProperties = new HashMap<>(nfe.getProperties());
					for (String dpPropertyName : dataProviders)
					{
						String dp = (String)nfe.getProperty(dpPropertyName);
						if (dp != null && dp.startsWith(relationPrefix)) elementProperties.put(dpPropertyName, NGClientForJsonConverter.toJavaObject(
							dp.substring(relationPrefix.length()), nfe.getWebComponentSpec().getProperty(dpPropertyName), context, ConversionLocation.DESIGN,
							null, true)); // portal always prefixes comp. dataproviders with related fs name
					}

					componentJSONs.put(getPureSabloJSONForFormElement(nfe, elementProperties));
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
				map.put("childElements", NGClientForJsonConverter.toJavaObject(componentJSONs, pd, context, ConversionLocation.DESIGN, null, true));
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

	/**
	 * Can also be used during debug to generate nice JSON for standard persist components - so replace them
	 * with a pure JSON designer definition (turn default components into real beans); tried this with a whole portal...
	 */
	public static String getPureSabloJSONForFormElementAsString(FormElement nfe, Map<String, Object> elementProperties) throws JSONException
	{
		// generate the NG definition of this persist and put it in there;
		JSONStringer jsonWriter = new JSONStringer();
		jsonWriter.object();
		jsonWriter.key(ComponentTypeValue.TYPE_NAME_KEY).value(nfe.getTypeName());
		jsonWriter.key(ComponentTypeValue.DEFINITION_KEY);

		// get types for conversion
		Map<String, Object> properties = (elementProperties != null ? elementProperties : nfe.getProperties());
		PropertyDescription propertyTypes = AggregatedPropertyType.newAggregatedProperty();
		for (Entry<String, Object> p : properties.entrySet())
		{
			PropertyDescription t = nfe.getWebComponentSpec().getProperty(p.getKey());
			if (t != null) propertyTypes.putProperty(p.getKey(), t);
		}
		if (!propertyTypes.hasChildProperties()) propertyTypes = null;

		JSONUtils.toDesignJSONValue(jsonWriter, properties, /* null */propertyTypes); // don't use types here! they aren't converted
		jsonWriter.endObject();
		return jsonWriter.toString();
	}

	public static JSONObject getPureSabloJSONForFormElement(FormElement nfe, Map<String, Object> elementProperties) throws JSONException
	{
		return new JSONObject(getPureSabloJSONForFormElementAsString(nfe, elementProperties));
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
