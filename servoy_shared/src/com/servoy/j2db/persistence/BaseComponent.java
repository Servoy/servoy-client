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

import com.servoy.base.persistence.IBaseComponent;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;


/**
 * The base component, providing default properties for graphical components
 *
 * @author jblok
 */
public class BaseComponent extends AbstractBase implements IFormElement, ISupportAnchors, ISupportPrintSliding, IPersistCloneable, ICloneable, IBaseComponent
{
	private static final long serialVersionUID = 1L;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	/**
	 * Constructor I
	 */
	protected BaseComponent(int type, ISupportChilds parent, int element_id, UUID uuid)
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
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ANCHORS, arg);
	}

	public int getAnchors()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ANCHORS).intValue();
	}

	public void setBackground(java.awt.Color arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND, arg);
	}

	/**
	 * The background color of the component.
	 */
	public java.awt.Color getBackground()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND);
	}

	public void setForeground(java.awt.Color arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FOREGROUND, arg);
	}

	/**
	 * The foreground color of the component.
	 */
	public java.awt.Color getForeground()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FOREGROUND);
	}

	/**
	 * Set the border
	 *
	 * @param arg the border
	 * @see com.servoy.j2db.dataui.ComponentFactoryHelper
	 */
	public void setBorderType(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BORDERTYPE, arg);
	}

	/**
	 * The type, color and style of border of the component.
	 */
	public String getBorderType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BORDERTYPE);
	}

	/**
	 * Set the font
	 *
	 * @param arg the font
	 * @see com.servoy.j2db.util.PersistHelper
	 */
	public void setFontType(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FONTTYPE, arg);
	}

	/**
	 * The font type of the component.
	 */
	public String getFontType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FONTTYPE);
	}

	/**
	 * Set the size
	 *
	 * @param arg the size
	 */
	public void setSize(java.awt.Dimension arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE, arg);

	}

	public java.awt.Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null)
		{
			return new java.awt.Dimension(140, 20);
		}
		return size;
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

	/**
	 * Check if a component contains the x and y.
	 *
	 * @param x
	 * @param y
	 * @return true if so
	 */
	public boolean contains(int x, int y)
	{
		Dimension size = getSize();
		return (size != null) && (x >= 0) && (x < size.width) && (y >= 0) && (y < size.height);
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
		Point location = getLocation();
		Dimension size = getSize();
		return location != null && size != null && x >= location.x && x < location.x + size.width && y >= location.y && y < location.y + size.height;
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

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPrintable(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PRINTABLE, arg);
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getPrintable()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PRINTABLE).booleanValue();
	}

	public void setVisible(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_VISIBLE, arg);
	}

	public boolean getVisible()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VISIBLE).booleanValue();
	}

	public void setEnabled(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ENABLED, arg);
	}

	public boolean getEnabled()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ENABLED).booleanValue();
	}

	public void setFormIndex(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FORMINDEX, arg);
	}

	public int getFormIndex()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FORMINDEX).intValue();
	}

	/**
	 * Set the name
	 *
	 * @param arg the name
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
			validator.checkName(arg, getID(), new ValidatorSearchContext(getAncestor(IRepository.FORMS), IRepository.ELEMENTS), false);
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}


	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

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
	 * @param arg
	 */
	public void setGroupID(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_GROUPID, arg);
	}

	/**
	 * Sets the locked.
	 *
	 * @param arg
	 */
	public void setLocked(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LOCKED, arg);
	}

	/**
	 * Enables an element to resize based on its content and/or move when printing.
	 * The component can move horizontally or vertically and can grow or shrink in
	 * height and width, based on its content and the content of neighboring
	 * components.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getPrintSliding()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PRINTSLIDING).intValue();
	}

	/**
	 * Set the printsliding (bitset)
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPrintSliding(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PRINTSLIDING, i);
	}

	/**
	 * Set the transparent
	 *
	 * @param arg the transparent
	 */
	public void setTransparent(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TRANSPARENT, arg);
	}

	/**
	 * Flag that tells if the component is transparent or not.
	 *
	 * The default value is "false", that is the components
	 * are not transparent.
	 */
	public boolean getTransparent()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TRANSPARENT).booleanValue();
	}

	public String getStyleClass()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS);
	}

	/**
	 * Set the style class name
	 */
	public void setStyleClass(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS, arg);
	}

	@Override
	protected void fillClone(AbstractBase cloned)
	{
		super.fillClone(cloned);

		BaseComponent baseComponentClone = (BaseComponent)cloned;
		if (hasProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName())) baseComponentClone.setSize(getSize());
		if (hasProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName())) baseComponentClone.setLocation(getLocation());
	}

	// TODO these might be better off in AbstractBase as for example Forms can use them as well
	public static boolean isCommandProperty(String propertyName)
	{
		return propertyName != null && propertyName.endsWith("CmdMethodID"); //$NON-NLS-1$
	}

	public static boolean isEventOrCommandProperty(String propertyName)
	{
		return propertyName != null && propertyName.endsWith("MethodID"); //$NON-NLS-1$
	}

	public static boolean isEventProperty(String propertyName)
	{
		return isEventOrCommandProperty(propertyName) && !isCommandProperty(propertyName);
	}

	public int getExtendsID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID).intValue();
	}

	public void setExtendsID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID, arg);
	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		return PersistHelper.getFlattenedPropertiesMap(this);
	}

	@Override
	public ISupportChilds getRealParent()
	{
		if (getExtendsID() > 0 && getParent() instanceof Form)//TODO check if responsive form?
		{
			IPersist superPersist = PersistHelper.getSuperPersist(this);
			if (superPersist != null) return superPersist.getParent();
		}
		return getParent();
	}
}
