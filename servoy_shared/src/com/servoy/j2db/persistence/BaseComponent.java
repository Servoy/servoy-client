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
 * The base component ,providing default properties for graphical components
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class BaseComponent extends AbstractBase implements IFormElement, ISupportAnchors, ISupportPrintSliding, IPersistCloneable, ICloneable
{
	private static final long serialVersionUID = 1L;

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private int anchors;
	private int formIndex;
	private java.awt.Color background = null;
	private java.awt.Color foreground = null;
	private String borderType = null;
	protected java.awt.Dimension size = null;
	private java.awt.Point location = null;
	private String fontType = null;
	private boolean printable = true;//remark not default!
	private String name = null;
	private String groupID;
	private boolean locked;
	private int printSliding;
	private boolean transparent;
	private String styleClass;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	/**
	 * Constructor I
	 */
	BaseComponent(int type, ISupportChilds parent, int element_id, UUID uuid)
	{
		super(type, parent, element_id, uuid);
	}

	/**
	 * Set the anchors (bitset)
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

	public void setBackground(java.awt.Color arg)
	{
		checkForChange(background, arg);
		background = arg;
	}

	/**
	 * The background color of the component.
	 */
	public java.awt.Color getBackground()
	{
		return background;
	}

	public void setForeground(java.awt.Color arg)
	{
		checkForChange(foreground, arg);
		foreground = arg;
	}

	/**
	 * The foreground color of the component.
	 */
	public java.awt.Color getForeground()
	{
		return foreground;
	}

	/**
	 * Set the border
	 * 
	 * @param arg the border
	 * @see com.servoy.j2db.dataui.ComponentFactoryHelper
	 */
	public void setBorderType(String arg)
	{
		checkForChange(borderType, arg);
		borderType = arg;
	}

	/**
	 * The type, color and style of border of the component.
	 */
	public String getBorderType()
	{
		return borderType;
	}

	/**
	 * Set the font
	 * 
	 * @param arg the font
	 * @see com.servoy.j2db.util.PersistHelper
	 */
	public void setFontType(String arg)
	{
		checkForChange(fontType, arg);
		fontType = arg;
	}

	/**
	 * The font type of the component.
	 */
	public String getFontType()
	{
		return fontType;
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
		if (size == null)
		{
			return new java.awt.Dimension(140, 20);
		}
		else
		{
			return new Dimension(size);
		}
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
		if (location == null)
		{
			return new Point(10, 10);
		}
		return new Point(location);
	}

	/**
	 * Check if a component contains the x and y.
	 * 
	 * @param x
	 * @param y
	 * @return true if so
	 */
	public boolean contains(int x, int y)
	{
		if (size != null)
		{
			return (x >= 0) && (x < size.width) && (y >= 0) && (y < size.height);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Check if a component contains the abs x and y.
	 * 
	 * @param x
	 * @param y
	 * @return true if so
	 */
	public boolean containsAbsolute(int x, int y)
	{
		if (location != null && size != null)
		{
			return (x >= location.x && x < location.x + size.width && y >= location.y && y < location.y + size.height);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Check if a component contains the rect.
	 * 
	 * @return true if so
	 */
	public boolean contains(java.awt.Rectangle r)
	{
		return new java.awt.Rectangle(getLocation(), getSize()).contains(r);
	}

	public void setPrintable(boolean arg)
	{
		checkForChange(printable, arg);
		printable = arg;
	}

	public boolean getPrintable()
	{
		return printable;
	}


	public void setFormIndex(int arg)
	{
		checkForChange(formIndex, arg);
		formIndex = arg;
	}

	public int getFormIndex()
	{
		return formIndex;
	}

	/**
	 * Set the name
	 * 
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		if (name != null) throw new UnsupportedOperationException("Can't set name 2x, use updateName"); //$NON-NLS-1$
		checkForChange(null, arg);
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

	/**
	 * The name of the component. Through this name it can also accessed in methods.
	 */
	public String getName()
	{
		return name;
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
	 * @param arg
	 */
	public void setGroupID(String arg)
	{
		checkForChange(groupID, arg);
		groupID = arg;
	}

	/**
	 * Sets the locked.
	 * 
	 * @param arg
	 */
	public void setLocked(boolean arg)
	{
		checkForChange(locked, arg);
		locked = arg;
	}

	/**
	 * Enables an element to resize based on its content and/or move when printing.
	 * The component can move horizontally or vertically and can grow or shrink in 
	 * height and width, based on its content and the content of neighboring 
	 * components.
	 */
	public int getPrintSliding()
	{
		return printSliding;
	}

	/**
	 * Set the printsliding (bitset)
	 * 
	 * @param i
	 */
	public void setPrintSliding(int i)
	{
		checkForChange(printSliding, i);
		printSliding = i;
	}

	/**
	 * Set the transparent
	 * 
	 * @param arg the transparent
	 */
	public void setTransparent(boolean arg)
	{
		checkForChange(transparent, arg);
		transparent = arg;
	}

	/**
	 * Flag that tells if the component is transparent or not.
	 * 
	 * The default value is "false", that is the components 
	 * are not transparent.
	 */
	public boolean getTransparent()
	{
		return transparent;
	}

	/**
	 * The name of the style class that should be applied to this component.
	 * 
	 * When defining style classes for specific component types, their names
	 * must be prefixed according to the type of the component. For example 
	 * in order to define a class names 'fancy' for fields, in the style
	 * definition the class must be named 'field.fancy'. If it would be 
	 * intended for labels, then it would be named 'label.fancy'. When specifying
	 * the class name for a component, the prefix is dropped however. Thus the
	 * field or the label will have its styleClass property set to 'fancy' only.
	 */
	public String getStyleClass()
	{
		return styleClass;
	}

	/**
	 * Set the style class name
	 */
	public void setStyleClass(String arg)
	{
		checkForChange(styleClass, arg);
		styleClass = arg;
	}

	@Override
	public IPersist clonePersist()
	{
		BaseComponent baseComponentClone = (BaseComponent)super.clonePersist();
		if (size != null) baseComponentClone.setSize(new Dimension(size));
		if (location != null) baseComponentClone.setLocation(new Point(location));

		return baseComponentClone;
	}
}
