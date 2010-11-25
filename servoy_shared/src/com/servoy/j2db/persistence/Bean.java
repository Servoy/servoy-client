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
	 * Set the name
	 * 
	 * @param arg the name
	 */
	@Override
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/**
	 * Update the name
	 * 
	 * @param arg the name
	 */
	@Override
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		if (arg != null)
		{
			validator.checkName(arg, getID(), new ValidatorSearchContext(getParent(), IRepository.ELEMENTS), false);
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	@Override
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the anchors
	 * 
	 * @param arg the anchors
	 */
	@Override
	public void setAnchors(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ANCHORS, arg);
	}

	@Override
	public int getAnchors()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ANCHORS).intValue();
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


	/**
	 * Set the size
	 * 
	 * @param arg the size
	 */
	@Override
	public void setSize(java.awt.Dimension arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE, arg);
	}

	@Override
	public java.awt.Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null) size = new Dimension(80, 80);
		return size;
	}

	/**
	 * Set the location
	 * 
	 * @param arg the location
	 */
	@Override
	public void setLocation(java.awt.Point arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION, arg);
	}

	@Override
	public java.awt.Point getLocation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION);
	}

	/**
	 * Set the printable
	 * 
	 * @param arg the printable
	 */
	@Override
	public void setPrintable(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PRINTABLE, arg);
	}

	@Override
	public boolean getPrintable()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PRINTABLE).booleanValue();
	}


	/**
	 * Set the formIndex
	 * 
	 * @param arg the formIndex
	 */
	@Override
	public void setFormIndex(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FORMINDEX, arg);
	}

	/**
	 * Get the formIndex
	 * 
	 * @return the formIndex
	 */
	@Override
	public int getFormIndex()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FORMINDEX).intValue();
	}

	/**
	 * Returns the groupID.
	 * 
	 * @return int
	 */
	@Override
	public String getGroupID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_GROUPID);
	}

	/**
	 * Returns the locked.
	 * 
	 * @return boolean
	 */
	@Override
	public boolean getLocked()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCKED).booleanValue();
	}

	/**
	 * Sets the groupID.
	 * 
	 * @param groupID The groupID to set
	 */
	@Override
	public void setGroupID(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_GROUPID, arg);
	}

	/**
	 * Sets the locked.
	 * 
	 * @param locked The locked to set
	 */
	@Override
	public void setLocked(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCKED, arg);
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
