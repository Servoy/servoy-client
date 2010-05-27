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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.UUID;

/**
 * One Tab from a tabpanel
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Tab extends AbstractBase implements ISupportBounds, IPersistCloneable, ISupportUpdateableName, ISupportMedia, ICloneable
{
	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private int containsFormID;
	private java.awt.Point location = null;// x-as is display_sequence
	private java.awt.Dimension dimension;
	private String relationName;
	private String text = null;
	private String name = null;
	private int imageMediaID;
	private String toolTipText = null;
	private String groupID;
	private boolean locked;
	private java.awt.Color foreground = null;
	private java.awt.Color background = null;
	private boolean useNewFormInstance;

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
		if (name != null) throw new UnsupportedOperationException("Can't set name 2x, use updateName"); //$NON-NLS-1$
		name = arg;
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
		checkForNameChange(name, arg);
		name = arg;
	}

	@Override
	public IPersist clonePersist()
	{
		Tab baseComponentClone = (Tab)super.clonePersist();
		if (dimension != null) baseComponentClone.setSize(new Dimension(dimension));
		if (location != null) baseComponentClone.setLocation(new Point(location));

		return baseComponentClone;
	}

	/**
	 * The name of the tab.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the containsFormID
	 * 
	 * @param arg the containsFormID
	 */
	public void setContainsFormID(int arg)
	{
		checkForChange(containsFormID, arg);
		containsFormID = arg;
	}

	/**
	 * The name of the form displayed in the tab.
	 */
	public int getContainsFormID()
	{
		return containsFormID;
	}

	/**
	 * Set the relationID
	 * 
	 * @param arg the relationID
	 */
	public void setRelationName(String arg)
	{
		checkForChange(relationName, arg);
		relationName = arg;
	}

	/**
	 * The name of the relation that links the form which contains the tab 
	 * with the form displayed in the tab.
	 */
	public String getRelationName()
	{
		return relationName;
	}

	public void setText(String arg)
	{
		checkForChange(text, arg);
		text = arg;
	}

	/**
	 * The text on the tab.
	 */
	public String getText()
	{
		return text;
	}

	public void setToolTipText(String arg)
	{
		checkForChange(toolTipText, arg);
		toolTipText = arg;
	}

	/**
	 * @sameas com.servoy.j2db.persistence.GraphicalComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return toolTipText;
	}

	public void setImageMediaID(int arg)
	{
		checkForChange(imageMediaID, arg);
		imageMediaID = arg;
	}

	/**
	 * The name of the image Media file used.
	 */
	public int getImageMediaID()
	{
		return imageMediaID;
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
		checkForChange(location, p);
		location = p;
	}

	public java.awt.Point getLocation()
	{
		if (location == null) return new Point(0, 0);
		return new Point(location);
	}

	public void setSize(java.awt.Dimension d)
	{
		checkForChange(dimension, d);
		dimension = d;
	}

	public java.awt.Dimension getSize()
	{
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
		return groupID;
	}

	/**
	 * Returns the locked.
	 * 
	 * @return boolean
	 */
	public boolean getLocked()
	{
		return locked;
	}

	/**
	 * Sets the groupID.
	 * 
	 * @param groupID The groupID to set
	 */
	public void setGroupID(String arg)
	{
		checkForChange(groupID, arg);
		groupID = arg;
	}

	/**
	 * Sets the locked.
	 * 
	 * @param locked The locked to set
	 */
	public void setLocked(boolean arg)
	{
		checkForChange(locked, arg);
		locked = arg;
	}

	@Deprecated
	public java.awt.Color getBackground()
	{
		return background;
	}

	@Deprecated
	public void setBackground(java.awt.Color arg)
	{
		checkForChange(background, arg);
		background = arg;
	}

	/**
	 * The foreground color of the tab.
	 */
	public java.awt.Color getForeground()
	{
		return foreground;
	}

	public void setForeground(java.awt.Color arg)
	{
		checkForChange(foreground, arg);
		foreground = arg;
	}

	public boolean getUseNewFormInstance()
	{
		return this.useNewFormInstance;
	}

	public void setUseNewFormInstance(boolean useNewFormInstance)
	{
		checkForChange(this.useNewFormInstance, useNewFormInstance);
		this.useNewFormInstance = useNewFormInstance;
	}

}
