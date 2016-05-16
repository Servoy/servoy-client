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


import java.awt.Dimension;
import java.awt.Point;
import java.util.Map;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * One Tab from a tabpanel
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.TABS)
@ServoyClientSupport(mc = false, wc = true, sc = true)
public class Tab extends AbstractBase
	implements ISupportBounds, IPersistCloneable, ISupportUpdateableName, ISupportMedia, ICloneable, ISupportExtendsID, IContainsFormID
{

	private static final long serialVersionUID = 1L;

	private java.awt.Dimension dimension;

	/**
	 * Constructor I
	 */
	Tab(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.TABS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/**
	 * Update the name
	 *
	 * @param arg the name
	 */
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		if (arg != null)
		{
			validator.checkName(arg, getID(), new ValidatorSearchContext(getParent(), IRepository.TABS), false);
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractBase#fillClone(com.servoy.j2db.persistence.AbstractBase)
	 */
	@Override
	protected void fillClone(AbstractBase cloned)
	{
		super.fillClone(cloned);
		Tab clonedTab = (Tab)cloned;

		Dimension size = getSize();
		if (size != null) clonedTab.setSize(size);
		Point location = getLocation();
		if (location != null) clonedTab.setLocation(location);
	}

	/**
	 * The name of the tab.
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the containsFormID
	 *
	 * @param arg the containsFormID
	 */
	public void setContainsFormID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_CONTAINSFORMID, arg);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMTab#getContainsForm()
	 */
	public int getContainsFormID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_CONTAINSFORMID).intValue();
	}

	/**
	 * Set the relationID
	 *
	 * @param arg the relationID
	 */
	public void setRelationName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_RELATIONNAME, arg);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMTab#getRelationName()
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public String getRelationName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_RELATIONNAME);
	}

	public void setText(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT, arg);
	}

	/**
	 * The text on the tab.
	 */
	public String getText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT);
	}

	public void setToolTipText(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT, arg);
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT);
	}

	public void setImageMediaID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_IMAGEMEDIAID, arg);
	}

	/**
	 * The name of the image Media file used.
	 */
	public int getImageMediaID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_IMAGEMEDIAID).intValue();
	}


//	public boolean getMultiLine()
//	{
//		return false;
//	}
//
//	public boolean getAllowsTabs()
//	{
//		return false;
//	}
	public void setLocation(java.awt.Point p)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION, p);
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

	public void setSize(java.awt.Dimension d)
	{
		dimension = d;
	}

	public java.awt.Dimension getSize()
	{
		// size doesn't exist in repository
		if (dimension == null) dimension = new Dimension(80, 20);
		return new Dimension(dimension);
	}

	public String getFont()
	{
		return ((TabPanel)parent).getFontType();
	}

	/**
	 * Returns the groupID.
	 *
	 * @return int
	 */
	public String getGroupID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_GROUPID);
	}

	/**
	 * Returns the locked.
	 *
	 * @return boolean
	 */
	public boolean getLocked()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCKED).booleanValue();
	}

	/**
	 * Sets the groupID.
	 *
	 * @param groupID The groupID to set
	 */
	public void setGroupID(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_GROUPID, arg);
	}

	/**
	 * Sets the locked.
	 *
	 * @param locked The locked to set
	 */
	public void setLocked(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCKED, arg);
	}

	@Deprecated
	public java.awt.Color getBackground()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND);
	}

	@Deprecated
	public void setBackground(java.awt.Color arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND, arg);
	}

	/**
	 * The foreground color of the tab.
	 */
	public java.awt.Color getForeground()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FOREGROUND);
	}

	public void setForeground(java.awt.Color arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FOREGROUND, arg);
	}

	public boolean getUseNewFormInstance()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_USENEWFORMINSTANCE).booleanValue();
	}

	public void setUseNewFormInstance(boolean useNewFormInstance)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_USENEWFORMINSTANCE, useNewFormInstance);
	}

	public int getExtendsID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID).intValue();
	}

	public void setExtendsID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID, arg);
	}


	/**
	 * Mnemonic used to switch to tab.
	 */
	public String getMnemonic()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_MNEMONIC);
	}

	public void setMnemonic(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_MNEMONIC, arg);
	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		return PersistHelper.getFlattenedPropertiesMap(this);
	}

}
