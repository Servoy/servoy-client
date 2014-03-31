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

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.component.WebComponentSpec;
import com.servoy.j2db.server.ngclient.component.WebComponentSpecProvider;
import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.utils.JSONUtils;
import com.servoy.j2db.server.ngclient.utils.MiniMap;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
public final class FormElement
{
	private final IPersist persist;
	private final Map<String, Object> propertyValues;

	public FormElement(Form form)
	{
		this.persist = form;
		Map<String, Object> map = ((AbstractBase)persist).getPropertiesMap();
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
	}

	/**
	 * @param persist
	 */
	public FormElement(IFormElement persist, FlattenedSolution fs)
	{
		this.persist = persist;
		if (persist instanceof Bean)
		{
			String customJSONString = ((Bean)persist).getBeanXML();
			if (customJSONString != null)
			{
				Map<String, Object> jsonMap = getConvertedPropertiesMap(((AbstractBase)persist).getPropertiesMap(), fs);
				try
				{
					Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
					Map<String, PropertyDescription> eventProperties = getWebComponentSpec().getHandlers();
					JSONObject jsonProperties = new JSONObject(customJSONString);
					Iterator keys = jsonProperties.keys();
					while (keys.hasNext())
					{
						String key = (String)keys.next();
						// is it a property
						PropertyDescription pd = specProperties.get(key);
						if (pd == null)
						{
							// or an event
							pd = eventProperties.get(key);
						}
						if (pd != null)
						{
							// TODO where the handle PropertyType.form properties? (see tabpanel below)
							//toJavaObject shoudl accept application because it is needed for format
							jsonMap.put(key, JSONUtils.toJavaObject(jsonProperties.get(key), pd, fs));
							//jsonMap.put(key, JSONUtils.toJavaObject(jsonProperties.get(key), pd.getType(), fs));
						}
					}
					fillPropertiesWithDefaults(specProperties, jsonMap);
				}
				catch (Exception ex)
				{
					Debug.error("Error while parsing json", ex);
				}
				propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(jsonMap, jsonMap.size()));
			}
			else
			{
				propertyValues = Collections.emptyMap();
			}
		}
		else if (persist instanceof AbstractBase)
		{
			Map<String, Object> map = getConvertedPropertiesMap(((AbstractBase)persist).getPropertiesMap(), fs);
			if (persist instanceof TabPanel)
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
					int containsFormID = tab.getContainsFormID();
					// TODO should this be resolved way later on?
					// if solution model then this form can change..
					Form form = fs.getForm(containsFormID);
					tabMap.put("containsFormId", form);
					tabList.add(tabMap);
					active = false;
				}
				map.put("tabs", tabList);
			}
			else if (persist instanceof Field && ((Field)persist).getDisplayType() == Field.MULTISELECT_LISTBOX)
			{
				map.put("multiselectListbox", Boolean.TRUE);
			}
			Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
			fillPropertiesWithDefaults(specProperties, map);
			propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
		}
		else
		{
			propertyValues = Collections.emptyMap();
		}
	}

	private void fillPropertiesWithDefaults(Map<String, PropertyDescription> specProperties, Map<String, Object> map)
	{
		if (specProperties != null && map != null)
		{
			for (PropertyDescription pd : specProperties.values())
			{
				if (pd.getDefaultValue() != null && !map.containsKey(pd.getName()))
				{
					map.put(pd.getName(), pd.getDefaultValue());
				}
			}
		}
	}

	public Map<String, Object> getProperties()
	{
		return propertyValues;
	}

	/**
	 * @return
	 */
	public WebComponentSpec getWebComponentSpec()
	{
		WebComponentSpec spec = null;
		if (persist instanceof IFormElement)
		{
			spec = WebComponentSpecProvider.getInstance().getWebComponentDescription(FormTemplateGenerator.getComponentTypeName((IFormElement)persist));
		}
		if (spec == null)
		{
			String errorMessage = "Component spec for " +
				((persist instanceof IFormElement) ? FormTemplateGenerator.getComponentTypeName((IFormElement)persist) : persist.toString()) +
				" not found, check your component spec file";
			Debug.error(errorMessage);
			throw new IllegalStateException(errorMessage);
		}
		return spec;
	}

	/**
	 * @return
	 */
	public String getName()
	{
		String name = ((ISupportName)persist).getName();
		if (name == null)
		{
			name = "svy_" + persist.getID();
		}
		return name;
	}

	/**
	 * @param name
	 */
	public Object getProperty(String name)
	{
		return propertyValues.get(name);
	}

	public Object getPropertyWithDefault(String name)
	{
		// TODO remove this delegation when going with tree structure , this is needed for DataAdapterList which 'thinks' everything is flat
		String[] split = name.split("\\.");
		if (split.length > 1)
		{
			return ((Map)propertyValues.get(split[0])).get(split[1]);
		}// end toRemove

		if (propertyValues.containsKey(name))
		{
			return propertyValues.get(name);
		}

		PropertyDescription propertyDescription = getWebComponentSpec().getProperties().get(name);

		if (propertyDescription != null)
		{
			switch (propertyDescription.getType())
			{
				case bool :
					return Boolean.FALSE;
				case valuelist :
				case bytenumber :
				case doublenumber :
				case floatnumber :
				case intnumber :
				case longnumber :
				case shortnumber :
					return Integer.valueOf(0);
				case dimension :
					return new Dimension(0, 0);
				case point :
					return new Point(0, 0);
			}
		}
		return null;
	}

	/**
	 * @return
	 */
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

	/**
	 * @return
	 */
	public boolean isLegacy()
	{
		return !(persist instanceof Bean);
	}

	public String getTagname()
	{
		if (persist instanceof IFormElement)
		{
			return "data-" + FormTemplateGenerator.getComponentTypeName((IFormElement)persist);
		}
		return "data-form";

	}

	public IFormElement getLabel()
	{
		IFormElement label = null;
		if (persist instanceof IFormElement)
		{
			String name = ((IFormElement)persist).getName();
			if (name != null)
			{
				ISupportChilds persistParent = ((IFormElement)persist).getParent();
				if (persistParent instanceof Form)
				{
					Iterator<IPersist> formElementsIte = ((Form)persistParent).getAllObjects();
					IPersist p;
					while (formElementsIte.hasNext())
					{
						p = formElementsIte.next();
						if (p instanceof GraphicalComponent && name.equals(((GraphicalComponent)p).getLabelFor()))
						{
							label = (GraphicalComponent)p;
							break;
						}
					}
				}
			}
		}
		return label;
	}

	public Collection<String> getHandlers()
	{
		List<String> handlers = new ArrayList<>();
		WebComponentSpec componentSpec = getWebComponentSpec();
		Set<String> events = componentSpec.getHandlers().keySet();
		for (String eventName : events)
		{
			if (getProperty(eventName) != null)
			{
				handlers.add(eventName);
			}
		}
		return handlers;
	}

	public Collection<String> getValuelistProperties()
	{
		List<String> valuelistProperties = new ArrayList<>();
		WebComponentSpec componentSpec = getWebComponentSpec();
		Map<String, PropertyDescription> properties = componentSpec.getProperties(PropertyType.valuelist);

		for (PropertyDescription pd : properties.values())
		{
			if (getProperty(pd.getName()) != null)
			{
				valuelistProperties.add(pd.getName());
			}
		}

		return valuelistProperties;
	}

	@SuppressWarnings("nls")
	public String getPropertiesString() throws JSONException
	{
		Map<String, Object> properties = new HashMap<>();
		WebComponentSpec componentSpec = getWebComponentSpec();
		Map<String, PropertyDescription> propDescription = componentSpec.getProperties();
		for (PropertyDescription pd : propDescription.values())
		{
			Object val = getProperty(pd.getName());
			if (val == null) continue;
			switch (pd.getType())
			{
			// dataprovider,formats,valuelist are always only pushed through the components
				case dataprovider :
				case format :
				case valuelist :
					continue;
				case tagstring :
					// tagstring if it has tags then it is data then it should be skipped.
					if (((String)val).contains("%%")) continue;
				default :
					properties.put(pd.getName(), val);
					break;

			}
		}

		if (persist instanceof BaseComponent)
		{
			Map<String, Object> propertiesMap = new HashMap<>(propertyValues);
			Point location = (Point)propertiesMap.get(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
			if (location != null)
			{
				Form form = getForm();
				Part part = form.getPartAt((int)location.getY());
				if (part != null)
				{
					int startPos = form.getPartStartYPos(part.getID());
					properties.put(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), new Point(location.x, location.y - startPos));
				}
			}
			Dimension dim = ((BaseComponent)persist).getSize();
			properties.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), dim);
			Integer anchor = (Integer)propertiesMap.get(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName());
			if (anchor != null)
			{
				properties.put(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(), anchor);
			}
		}

		JSONWriter propertyWriter = new JSONStringer();
		try
		{
			propertyWriter.object();
			JSONUtils.addObjectPropertiesToWriter(propertyWriter, properties);
			return propertyWriter.endObject().toString();
		}
		catch (JSONException | IllegalArgumentException e)
		{
			Debug.error("Problem detected when handling a component's (" + getTagname() + ") properties / events.", e);
			throw e;
		}
	}

	/**
	 * AbstractBase.getPropertiesMap() returns for format,borderType  etc the STRING representation instead of the high level class representation used in ngClient
	 * Converts string representation to high level Class representation of properties
	 *  
	 * Initially only for border
	 */
	private Map<String, Object> getConvertedPropertiesMap(Map<String, Object> propertiesMap, FlattenedSolution fs)
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
					Media media = fs.getMedia(Utils.getAsInteger(val));
					if (media != null)
					{
						convPropertiesMap.put(pv, "resources/" + media.getRootObject().getName() + "/" + media.getBlobId() + "/" + media.getName());
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return String.format(
			"<%1$s name=\"%2$s\" svy-model=\"model.%2$s\" svy-api=\"api.%2$s\" svy-handlers=\"handlers.%2$s\" svy-apply=\"handlers.%2$s.svy_apply\" svy-servoyApi=\"handlers.%2$s.svy_servoyApi\"></%1$s>",
			getTagname(), getName());
	}
}
