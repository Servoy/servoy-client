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


import java.awt.Color;
import java.awt.Dimension;

import org.json.JSONException;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Bean wrapper class, build the bean from the XML
 *
 * @author jblok
 */
public class Bean extends BaseComponent implements ISupportTabSeq, IBasicWebComponent
{
	/**
	 * Constructor I
	 */
	protected Bean(ISupportChilds parent, int element_id, UUID uuid)
	{
		this(IRepository.BEANS, parent, element_id, uuid);
	}

	protected Bean(int type, ISupportChilds parent, int element_id, UUID uuid)
	{
		super(type, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	@Override
	public void clearProperty(String propertyName)
	{
		// innerinnerHTML is mapped to beanXML
		super.clearProperty("innerHTML".equals(propertyName) ? StaticContentSpecLoader.PROPERTY_BEANXML.getPropertyName() : propertyName); //$NON-NLS-1$
	}

	@Override
	public boolean hasProperty(String propertyName)
	{
		// innerinnerHTML is mapped to beanXML
		return super.hasProperty("innerHTML".equals(propertyName) ? StaticContentSpecLoader.PROPERTY_BEANXML.getPropertyName() : propertyName); //$NON-NLS-1$
	}

	/**
	 * Set the actionMethodID
	 *
	 * @param arg the actionMethodID
	 */
	public void setOnActionMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID, arg);
	}

	/**
	 * Get the method that is triggered when an onAction event occurs.
	 *
	 * @return the method that is triggered when an onAction event occurs
	 */
	public int getOnActionMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID).intValue();
	}

	/**
	 * Set the beanXML
	 *
	 * @param xml the beanXML
	 */
	public void setBeanXML(String arg)
	{
		String xml = Utils.stringReplace(arg, "javax.beans.XML", "java.beans.XML");//fix for remove of compat141.jar //$NON-NLS-1$ //$NON-NLS-2$
		xml = Utils.stringReplace(xml, "com.servoy.r2", "com.servoy.extensions");//fix for path rename in 3.1  //$NON-NLS-1$//$NON-NLS-2$
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BEANXML, xml);
	}

	/**
	 * Get the bean object in XML format.
	 *
	 * @return a String object containing the bean object in XML format
	 */
	public String getBeanXML()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BEANXML);
	}

	/**
	 * Get the mobile bean innerHTML.
	 *
	 * @return a String object containing the mobile bean innerHTML
	 */
	@ServoyClientSupport(mc = true, wc = false, sc = false)
	public String getInnerHTML()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BEANXML);
	}

	/**
	 * Set the mobile bean innerHTML
	 *
	 * @param innerHTML the mobile bean innerHTML
	 */
	@ServoyClientSupport(mc = true, wc = false, sc = false)
	public void setInnerHTML(String innerHTML)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BEANXML, innerHTML);
	}

	/**
	 * Set the beanClassName
	 *
	 * @param arg the beanClassName
	 */
	public void setBeanClassName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BEANCLASSNAME, arg);
	}

	/**
	 * Get the class name of the bean.
	 *
	 * @return the class name of the bean
	 */
	public String getBeanClassName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BEANCLASSNAME);
	}

	/**
	 * Set the parameters
	 *
	 * @param arg the parameters
	 */
	public void setParameters(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PARAMETERS, arg);
	}

	/**
	 * Get the parameters.
	 *
	 * @return the parameters
	 */
	public String getParameters()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PARAMETERS);
	}

	/**
	 * Set the usesUI
	 *
	 * @param arg the usesUI
	 */
	public void setUsesUI(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_USESUI, arg);
	}

	/**
	 * Get the usesUI.
	 *
	 * @return the usesUI
	 */
	public boolean getUsesUI()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_USESUI).booleanValue();
	}


	@Override
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public java.awt.Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null) size = new Dimension(80, 80);
		return size;
	}

	@Override
	public java.awt.Point getLocation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION);
	}

	/**
	 * Set the tabSeq
	 *
	 * @param arg the tabSeq
	 */
	public void setTabSeq(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ, arg);
	}

	public int getTabSeq()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TABSEQ).intValue();
	}

	@Deprecated
	@Override
	public Color getBackground()
	{
		return null;
	}

	@Deprecated
	@Override
	public String getBorderType()
	{
		return null;
	}

	@Deprecated
	@Override
	public String getFontType()
	{
		return null;
	}

	@Override
	@Deprecated
	public Color getForeground()
	{
		return null;
	}

	@Deprecated
	@Override
	public int getPrintSliding()
	{
		return 0;
	}

	@Deprecated
	@Override
	public String getStyleClass()
	{
		return null;
	}

	@Override
	@Deprecated
	public boolean getTransparent()
	{
		return false;
	}

	@Override
	public String toString()
	{
		String name = getName();
		if (name == null || name.trim().length() == 0)
		{
			return getBeanClassName();
		}
		return name + " [" + getBeanClassName() + ']'; //$NON-NLS-1$
	}

	@Override
	public ServoyJSONObject getJson()
	{
		try
		{
			if (getBeanXML() != null) return new ServoyJSONObject(getBeanXML(), false);
		}
		catch (JSONException ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	@Override
	public void setJson(ServoyJSONObject o)
	{
		String beanXML = null;
		if (o.length() > 0)
		{
			beanXML = o.toString(false);
		}
		setBeanXML(beanXML);
	}

	@Override
	public void updateJSON()
	{
		// not supported by legacy Bean impl of web components
	}

	@Override
	public void setJsonSubproperty(String key, Object value)
	{
		try
		{
			ServoyJSONObject jsonObject = getJson() == null ? new ServoyJSONObject(true, true) : getJson();
			if (!jsonObject.has(getName()) || !jsonObject.get(getName()).equals(value))
			{
				jsonObject.put(getName(), value);
				setJson(jsonObject);
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public String getTypeName()
	{
		return getBeanClassName();
	}

	@Override
	public void setTypeName(String arg)
	{
		setBeanClassName(arg);
	}

	@Override
	public IBasicWebComponent getParentComponent()
	{
		return this;
	}

}
