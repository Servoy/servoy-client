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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Bean wrapper class, build the bean from the XML
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Bean extends BaseComponent implements ISupportTabSeq
{
	/**
	 * Constructor I
	 */
	Bean(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.BEANS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.BaseComponent#getBackground()
	 */
	@Deprecated
	@Override
	public Color getBackground()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.BaseComponent#getBorderType()
	 */
	@Deprecated
	@Override
	public String getBorderType()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.BaseComponent#getFontType()
	 */
	@Deprecated
	@Override
	public String getFontType()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.BaseComponent#getForeground()
	 */
	@Override
	@Deprecated
	public Color getForeground()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.BaseComponent#getPrintSliding()
	 */
	@Deprecated
	@Override
	public int getPrintSliding()
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.BaseComponent#getStyleClass()
	 */
	@Deprecated
	@Override
	public String getStyleClass()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.BaseComponent#getTransparent()
	 */
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

}
