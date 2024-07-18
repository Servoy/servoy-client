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

package com.servoy.j2db.persistence;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
@SuppressWarnings("nls")
public class LayoutContainer extends AbstractContainer implements ISupportBounds, ISupportAttributes
{

	private static final long serialVersionUID = 1L;

	protected LayoutContainer(ISupportChilds parent, int element_id, UUID uuid)
	{
		this(IRepository.LAYOUTCONTAINERS, parent, element_id, uuid);
	}

	protected LayoutContainer(int type, ISupportChilds parent, int element_id, UUID uuid)
	{
		super(type, parent, element_id, uuid);
	}

	/**
	 * set the layout spec name
	 *
	 * @param name
	 */
	public void setSpecName(String name)
	{
		putCustomProperty(new String[] { "properties", "specname" }, name);
	}

	/**
	 * returns the layouts spec name
	 *
	 * @return String
	 */
	public String getSpecName()
	{
		return (String)getCustomProperty(new String[] { "properties", "specname" });
	}

	/**
	 * set the layout package name
	 *
	 * @param name
	 */
	public void setPackageName(String name)
	{
		putCustomProperty(new String[] { "properties", "packagename" }, name);
	}

	/**
	 * returns the layouts package name
	 *
	 * @return String
	 */
	public String getPackageName()
	{
		return (String)getCustomProperty(new String[] { "properties", "packagename" });
	}

	/**
	 * Set the location
	 *
	 * @param arg the location
	 */
	public void setLocation(java.awt.Point arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION, arg);
	}

	public java.awt.Point getLocation()
	{
		java.awt.Point point = getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION);
		if (point == null)
		{
			point = new Point(10, 10);
		}
		return point;
	}

	public void setTagType(String tagType)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TAGTYPE, tagType);
	}

	/**
	 * The tag type for html output. Default value is 'div'.
	 */
	public String getTagType()
	{
		String tag = getTypedProperty(StaticContentSpecLoader.PROPERTY_TAGTYPE);
		if (tag == null)
		{
			String defaultValue = (String)getDefaultValue(StaticContentSpecLoader.PROPERTY_TAGTYPE);
			if (defaultValue != null)
			{
				return defaultValue;
			}
			return "div";
		}
		return tag;
	}

	@Override
	public java.awt.Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null)
		{
			JSONObject defaultValue = (JSONObject)getDefaultValue(StaticContentSpecLoader.PROPERTY_SIZE);
			if (defaultValue != null)
			{
				return new java.awt.Dimension(defaultValue.optInt("width"), defaultValue.optInt("height"));
			}
			return super.getSize();
		}
		return size;
	}

	public <T> Object getDefaultValue(TypedProperty<T> propType)
	{
		Map<String, PackageSpecification<WebLayoutSpecification>> layouts = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications();
		if (layouts != null && getPackageName() != null && layouts.get(getPackageName()) != null)
		{
			WebLayoutSpecification spec = layouts.get(getPackageName()).getSpecification(getSpecName());
			if (spec != null && spec.getProperty(propType.getPropertyName()) != null &&
				spec.getProperty(propType.getPropertyName()).hasDefault())
			{
				return spec.getProperty(propType.getPropertyName()).getDefaultValue();
			}
		}
		return null;
	}

	public List<String> getAllowedChildren()
	{
		Map<String, PackageSpecification<WebLayoutSpecification>> layouts = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications();
		if (layouts != null && getPackageName() != null && layouts.get(getPackageName()) != null &&
			layouts.get(getPackageName()).getSpecification(getSpecName()) != null)
		{
			WebLayoutSpecification spec = layouts.get(getPackageName()).getSpecification(getSpecName());
			return spec.getAllowedChildren();
		}
		return null;
	}

	public void setElementId(String id)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ELEMENTID, id);
	}

	/**
	 * The id to be output for html tag.
	 */
	public String getElementId()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ELEMENTID);
	}

	public void setCssClasses(String cls)
	{
		String newCls = cls.trim();
		newCls = newCls.replaceAll("\\s+", " ");
		putAttribute("class", newCls);
	}

	/**
	 * The css classes to be output for html tag.
	 */
	public String getCssClasses()
	{
		return getAttribute("class");
	}

	public void setStyle(String style)
	{
		putAttribute("style", style);
	}

	/**
	 * The style definition to be output in html tag.
	 */
	public String getStyle()
	{
		return getAttribute("style");
	}

	/**
	 * returns the attribute value of the given attribute name.
	 * these attributes will be generated on the tag.
	 *
	 * @param name
	 * @return the value of the attribute
	 */
	public String getAttribute(String name)
	{
		Object value = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES, name });
		if (value instanceof String) return (String)value;
		return null;
	}

	/**
	 * sets an attribute value for the given name that will be generated on this layout containers html tag.
	 *
	 * @param name
	 * @param value
	 */
	public void putAttribute(String name, String value)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES, name }, value);
	}

	public void clearAttribute(String name)
	{
		clearCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES, name });
	}

	@Override
	public Map<String, String> getAttributes()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES });
		if (customProperty instanceof Map)
		{
			return Collections.unmodifiableMap((Map<String, String>)customProperty);
		}
		return Collections.emptyMap();

	}

	@Override
	public void putAttributes(Map<String, String> value)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES }, value);
	}


	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getMergedAttributes()
	{
		return (Map<String, String>)getMergedCustomPropertiesInternal(IContentSpecConstants.PROPERTY_ATTRIBUTES, new TreeMap<String, String>());
	}

	@Override
	public void putUnmergedAttributes(Map<String, String> value)
	{
		setUnmergedCustomPropertiesInternal(IContentSpecConstants.PROPERTY_ATTRIBUTES, value);
	}

	@Override
	public List<IPersist> getHierarchyChildren()
	{
		return PersistHelper.getHierarchyChildren(this);
	}


	@Override
	protected boolean validateName(String newName)
	{
		// allow null name
		return newName != null;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " -> " + getPackageName() + " [" + getSpecName() + "]";
	}

	@Override
	public ISupportChilds getRealParent()
	{
		return PersistHelper.getRealParent(this);
	}
}
