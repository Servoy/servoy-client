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

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Bean wrapper class, build the bean from the XML
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Bean extends AbstractBase implements IFormElement, IPersistCloneable, ISupportPrinting, ISupportBounds, ISupportAnchors, ISupportTabSeq
{
	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private int anchors;
	private String name = null;
	private String beanXML = null;
	private String beanClassName = null;
	private String parameters = null;
	private int onActionMethodID;
	private boolean usesUI;
	private java.awt.Dimension size = null;
	private java.awt.Point location = null;
	private boolean printable = true;//remark not default!
	private int formIndex;
	private String groupID;
	private boolean locked;
	private int tabSeq = ISupportTabSeq.DEFAULT;

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
			validator.checkName(arg, getID(), new ValidatorSearchContext(getParent(), IRepository.ELEMENTS), false);
		}
		checkForNameChange(name, arg);
		name = arg;
	}

	public String getName()
	{
		return name;
	}

	/**
	 * Set the anchors
	 * 
	 * @param arg the anchors
	 */
	public void setAnchors(int arg)
	{
		checkForChange(anchors, arg);
		anchors = arg;
	}

	public int getAnchors()
	{
		return anchors;
	}

	/**
	 * Set the actionMethodID
	 * 
	 * @param arg the actionMethodID
	 */
	public void setOnActionMethodID(int arg)
	{
		checkForChange(onActionMethodID, arg);
		onActionMethodID = arg;
	}

	/**
	 * Get the method that is triggered when an onAction event occurs.
	 * 
	 * @return the method that is triggered when an onAction event occurs
	 */
	public int getOnActionMethodID()
	{
		return onActionMethodID;
	}

	/**
	 * Set the beanXML
	 * 
	 * @param arg the beanXML
	 */
	public void setBeanXML(String arg)
	{
		arg = Utils.stringReplace(arg, "javax.beans.XML", "java.beans.XML");//fix for remove of compat141.jar //$NON-NLS-1$ //$NON-NLS-2$
		arg = Utils.stringReplace(arg, "com.servoy.r2", "com.servoy.extensions");//fix for path rename in 3.1
		checkForChange(beanXML, arg);
		beanXML = arg;
	}

	/**
	 * Get the bean object in XML format.
	 * 
	 * @return a String object containing the bean object in XML format
	 */
	public String getBeanXML()
	{
		beanXML = Utils.stringReplace(beanXML, "com.servoy.r2", "com.servoy.extensions");//fix for path rename in 3.1
		return beanXML;
	}

	/**
	 * Set the beanClassName
	 * 
	 * @param arg the beanClassName
	 */
	public void setBeanClassName(String arg)
	{
		checkForChange(beanClassName, arg);
		beanClassName = arg;
	}

	/**
	 * Get the class name of the bean.
	 * 
	 * @return the class name of the bean
	 */
	public String getBeanClassName()
	{
		String retval = beanClassName;
		if (retval != null && retval.startsWith("com.servoy.r2"))
		{
			retval = Utils.stringReplace(retval, "com.servoy.r2", "com.servoy.extensions");//fix for path rename in 3.1
		}
		return retval;
	}

	/**
	 * Set the parameters
	 * 
	 * @param arg the parameters
	 */
	public void setParameters(String arg)
	{
		checkForChange(parameters, arg);
		parameters = arg;
	}

	/**
	 * Get the parameters.
	 * 
	 * @return the parameters
	 */
	public String getParameters()
	{
		return parameters;
	}

	/**
	 * Set the usesUI
	 * 
	 * @param arg the usesUI
	 */
	public void setUsesUI(boolean arg)
	{
		checkForChange(usesUI, arg);
		usesUI = arg;
	}

	/**
	 * Get the usesUI.
	 * 
	 * @return the usesUI
	 */
	public boolean getUsesUI()
	{
		return usesUI;
	}


	/**
	 * Set the size
	 * 
	 * @param arg the size
	 */
	public void setSize(java.awt.Dimension arg)
	{
		checkForChange(size, arg);
		size = arg;
	}

	public java.awt.Dimension getSize()
	{
		if (size == null) return new Dimension(80, 80);
		return new Dimension(size);
	}

	/**
	 * Set the location
	 * 
	 * @param arg the location
	 */
	public void setLocation(java.awt.Point arg)
	{
		checkForChange(location, arg);
		location = arg;
	}

	public java.awt.Point getLocation()
	{
		if (location == null) return new Point(0, 0);
		return new Point(location);
	}

	/**
	 * Set the printable
	 * 
	 * @param arg the printable
	 */
	public void setPrintable(boolean arg)
	{
		checkForChange(printable, arg);
		printable = arg;
	}

	public boolean getPrintable()
	{
		return printable;
	}


	/**
	 * Set the formIndex
	 * 
	 * @param arg the formIndex
	 */
	public void setFormIndex(int arg)
	{
		checkForChange(formIndex, arg);
		formIndex = arg;
	}

	/**
	 * Get the formIndex
	 * 
	 * @return the formIndex
	 */
	public int getFormIndex()
	{
		return formIndex;
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

	/**
	 * Set the tabSeq
	 * 
	 * @param arg the tabSeq
	 */
	public void setTabSeq(int arg)
	{
		if (arg < 1 && arg != ISupportTabSeq.DEFAULT && arg != ISupportTabSeq.SKIP) return;//irrelevant value from editor
		checkForChange(tabSeq, arg);
		tabSeq = arg;
	}

	public int getTabSeq()
	{
		return tabSeq;
	}

	@Override
	public String toString()
	{
		if (name == null || name.trim().length() == 0)
		{
			return getBeanClassName();
		}
		return name + " [" + getBeanClassName() + ']';
	}

}
