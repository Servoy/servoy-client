/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.persistence;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * @author gboros
 */
public class WebComponent extends BaseComponent implements IWebComponent
{
	/**
	 * Constructor I
	 */
	protected WebComponent(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.WEBCOMPONENTS, parent, element_id, uuid);
	}

	@Override
	public boolean isChanged()
	{
		boolean changed = super.isChanged();

		if (!changed)
		{
			for (IPersist p : getAllCustomProperties())
			{
				if (p.isChanged())
				{
					changed = true;
					updateCustomProperties();
					break;
				}
			}
		}

		return changed;
	}

	private void updateCustomProperties()
	{
		if (isCustomTypePropertiesLoaded && getCustomTypeProperties().size() > 0)
		{
			ServoyJSONObject entireModel = getJson() != null ? getJson() : new ServoyJSONObject();
			try
			{
				for (Map.Entry<String, Object> wo : getCustomTypeProperties().entrySet())
				{
					if (wo.getValue() instanceof WebCustomType)
					{
						entireModel.put(wo.getKey(), ((WebCustomType)wo.getValue()).getJson());
					}
					else
					{
						ServoyJSONArray jsonArray = new ServoyJSONArray();
						for (WebCustomType wo1 : (WebCustomType[])wo.getValue())
						{
							jsonArray.put(wo1.getJson());
						}
						entireModel.put(wo.getKey(), jsonArray);
					}
				}
			}
			catch (JSONException ex)
			{
				Debug.error(ex);
			}
			setJson(entireModel);
		}
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		if (val instanceof WebCustomType || val instanceof WebCustomType[])
		{
			Map<String, Object> ctp = getCustomTypeProperties();
			ctp.put(propertyName, val);
		}
		else super.setProperty(propertyName, val);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		if (!"json".equals(propertyName) && !"typeName".equals(propertyName)) //$NON-NLS-1$//$NON-NLS-2$
		{
			Map<String, Object> ctp = getCustomTypeProperties();
			if (ctp.containsKey(propertyName)) return ctp.get(propertyName);
		}
		return super.getProperty(propertyName);
	}

	public List<IPersist> getAllCustomProperties()
	{
		ArrayList<IPersist> allCustomProperties = new ArrayList<IPersist>();
		for (Object wo : getCustomTypeProperties().values())
		{
			if (wo instanceof WebCustomType[])
			{
				allCustomProperties.addAll(Arrays.asList((WebCustomType[])wo));
			}
			else
			{
				allCustomProperties.add((WebCustomType)wo);
			}
		}

		return allCustomProperties;
	}

	private final Map<String, Object> customTypeProperties = new HashMap<String, Object>();
	private boolean isCustomTypePropertiesLoaded = false;

	private Map<String, Object> getCustomTypeProperties()
	{
		if (!isCustomTypePropertiesLoaded)
		{
			String typeName = getTypeName();
			WebComponentSpecification spec = WebComponentSpecProvider.getInstance() != null
				? WebComponentSpecProvider.getInstance().getWebComponentSpecification(typeName) : null;

			if (typeName != null && spec != null)
			{
				if (getJson() != null && JSONObject.getNames(getJson()) != null)
				{
					JSONObject beanJSON = getJson();
					Map<String, IPropertyType< ? >> foundTypes = spec.getFoundTypes();
					try
					{

						for (String beanJSONKey : JSONObject.getNames(beanJSON))
						{
							Object object = beanJSON.get(beanJSONKey);
							if (object != null && spec.getProperty(beanJSONKey) != null)
							{
								IPropertyType< ? > propertyType = spec.getProperty(beanJSONKey).getType();
								String simpleTypeName = propertyType.getName().replaceFirst(spec.getName() + ".", ""); //$NON-NLS-1$//$NON-NLS-2$
								if (foundTypes.containsKey(simpleTypeName))
								{
									boolean arrayReturnType = spec.isArrayReturnType(beanJSONKey);
									if (!arrayReturnType)
									{
										WebCustomType webObject = new WebCustomType(this, beanJSONKey, simpleTypeName, -1, false);
										webObject.setTypeName(simpleTypeName);
										customTypeProperties.put(beanJSONKey, webObject);
									}
									else if (object instanceof JSONArray)
									{
										ArrayList<WebCustomType> webObjects = new ArrayList<WebCustomType>();
										for (int i = 0; i < ((JSONArray)object).length(); i++)
										{
											WebCustomType webObject = new WebCustomType(this, beanJSONKey, simpleTypeName, i, false);
											webObject.setTypeName(simpleTypeName);
											webObjects.add(webObject);
										}
										customTypeProperties.put(beanJSONKey, webObjects.toArray(new WebCustomType[webObjects.size()]));
									}
								}
							}
						}
					}
					catch (JSONException e)
					{
						Debug.error(e);
					}
				}
				isCustomTypePropertiesLoaded = true;
			}
		}

		return customTypeProperties;
	}

	public void setTypeName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME, arg);
	}

	public String getTypeName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME);
	}

	public void setJson(ServoyJSONObject arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
	}

	public ServoyJSONObject getJson()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_JSON);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.AbstractBase#internalRemoveChild(com.servoy.j2db.persistence.IPersist)
	 */
	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		if (obj instanceof WebCustomType)
		{
			WebCustomType customType = (WebCustomType)obj;
			Object children = getCustomTypeProperties().get(customType.getJsonKey());
			if (children != null)
			{
				if (children instanceof WebCustomType[])
				{
					List<WebCustomType> t = new ArrayList<WebCustomType>(Arrays.asList((WebCustomType[])children));
					t.remove(customType);
					for (int i = customType.getIndex(); i < t.size(); i++)
					{
						WebCustomType ct = t.get(i);
						ct.setIndex(i);
					}
					getCustomTypeProperties().put(customType.getJsonKey(), t.toArray(new WebCustomType[t.size()]));
				}
				else
				{
					getCustomTypeProperties().remove(customType.getJsonKey());
					if (getCustomTypeProperties().isEmpty()) setJson(new ServoyJSONObject());
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractBase#internalAddChild(com.servoy.j2db.persistence.IPersist)
	 */
	@Override
	public void internalAddChild(IPersist obj)
	{
		if (obj instanceof WebCustomType)
		{
			WebCustomType customType = (WebCustomType)obj;
			String typeName = getTypeName();
			WebComponentSpecification spec = WebComponentSpecProvider.getInstance() != null
				? WebComponentSpecProvider.getInstance().getWebComponentSpecification(typeName) : null;

			if (spec.isArrayReturnType(customType.getJsonKey()))
			{
				Object children = getCustomTypeProperties().get(customType.getJsonKey());
				if (children != null)
				{
					if (children instanceof WebCustomType[])
					{
						List<WebCustomType> t = new ArrayList<WebCustomType>(Arrays.asList((WebCustomType[])children));
						t.add(customType.getIndex(), customType);
						for (int i = customType.getIndex() + 1; i < t.size(); i++)
						{
							WebCustomType ct = t.get(i);
							ct.setIndex(i);
						}
						getCustomTypeProperties().put(customType.getJsonKey(), t.toArray(new WebCustomType[t.size()]));
					}
				}
			}
			else
			{
				getCustomTypeProperties().put(customType.getJsonKey(), customType);
			}
		}
	}

	@Override
	public String toString()
	{
		String name = getName();
		if (name == null || name.trim().length() == 0)
		{
			return getTypeName();
		}
		return name + " [" + getTypeName() + ']'; //$NON-NLS-1$
	}

}