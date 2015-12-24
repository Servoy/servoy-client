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
import java.util.Iterator;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.util.UUID;

/**
 * Portal to view multiple related fields
 *
 * @author jblok
 */
public class Portal extends BaseComponent implements ISupportFormElements, ISupportScrollbars, ISupportTabSeq
{

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor I
	 */
	protected Portal(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.PORTALS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */


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
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMPortal#getRelationName()
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public String getRelationName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_RELATIONNAME);
	}

	/**
	 * Set the rowHeight
	 *
	 * @param arg the rowHeight
	 */
	public void setRowHeight(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ROWHEIGHT, arg);
	}

	/**
	 * The height of each row in pixels. If 0 or not set, the height defaults to 10.
	 */
	public int getRowHeight()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ROWHEIGHT).intValue();
	}

	/**
	 * Set the reorderable
	 *
	 * @param arg the reorderable
	 */
	public void setReorderable(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_REORDERABLE, arg);
	}

	/**
	 * When set, the portal rows can be re-ordered by dragging the column headers.
	 */
	public boolean getReorderable()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_REORDERABLE).booleanValue();
	}

	/**
	 * Set the resizeble
	 *
	 * @param arg the resizeble
	 */
	@Deprecated
	public void setResizeble(boolean arg)
	{
		setResizable(arg);
	}

	/**
	 * When set the portal rows can be resized by users.
	 */
	@Deprecated
	public boolean getResizeble()
	{
		return getResizable();
	}

	public void setResizable(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_RESIZABLE, arg);
	}

	/**
	 * When set the portal rows can be resized by users.
	 */
	public boolean getResizable()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_RESIZABLE).booleanValue();
	}

	/**
	 * Set the sortable
	 *
	 * @param arg the sortable
	 */
	public void setSortable(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SORTABLE, arg);
	}

	/**
	 * When set, users can sort the contents of the portal by clicking on the column headings.
	 */
	public boolean getSortable()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SORTABLE).booleanValue();
	}

	/**
	 * Set the multiLine
	 *
	 * @param arg the multiLine
	 */
	public void setMultiLine(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_MULTILINE, arg);
	}

	/**
	 * When set, portal rows can have a custom layout of fields, buttons, etc. displayed for each
	 * matching row (rather than the default "grid").
	 */
	public boolean getMultiLine()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_MULTILINE).booleanValue();
	}

	/**
	 * When set the portal displays vertical lines between the columns.
	 *
	 * NOTE:
	 * In a multi-line portal, a vertical line is only displayed
	 * in the selected row; to display a vertical line in all rows, add
	 * a line to the portal.
	 */
	public boolean getShowVerticalLines()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWVERTICALLINES).booleanValue();
	}

	/**
	 * Set the showVerticalLine
	 *
	 * @param arg the showVerticalLine
	 */
	public void setShowVerticalLines(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWVERTICALLINES, arg);
	}

	/**
	 * When set, the portal displays horizontal lines between the rows.
	 *
	 * NOTE:
	 * In a multi-line portal, a horizontal line is only displayed
	 * in the selected row; to display a horizontal line in all rows, add a
	 * line to the portal.
	 */
	public boolean getShowHorizontalLines()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWHORIZONTALLINES).booleanValue();
	}

	/**
	 * Set the showHorizontalLines
	 *
	 * @param arg the showHorizontalLines
	 */
	public void setShowHorizontalLines(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWHORIZONTALLINES, arg);
	}

	/**
	 * Set the intercellSpacing
	 *
	 * @param arg the intercellSpacing
	 */
	public void setIntercellSpacing(Dimension arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_INTERCELLSPACING, arg);
	}


	/**
	 * The additional spacing between cell rows. Is composed from the horizontal spacing
	 * and the vertical spacing.
	 */
	public Dimension getIntercellSpacing()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_INTERCELLSPACING);
	}

	/**
	 * Sets the defaultSort.
	 *
	 * @param arg The defaultSort to set
	 */
	public void setInitialSort(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT, arg);
	}

	/**
	 * The default sort order for the rows displayed in the portal.
	 */
	public String getInitialSort()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT);
	}

	/*
	 * _____________________________________________________________ Methods for Field handling
	 */

	/**
	 * Get all fields from this portal
	 *
	 * @return iterator with Fields
	 */
	public Iterator<Field> getFields()
	{
		return getObjects(IRepository.FIELDS);
	}

	/**
	 * Create a new Field
	 *
	 * @return the new Field
	 */
	public Field createNewField(Point location) throws RepositoryException
	{
		Field obj = (Field)getRootObject().getChangeHandler().createNewObject(this, IRepository.FIELDS);

		//set all the required properties
		obj.setLocation(location);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for GraphicalComponent handling
	 */
	/**
	 * Get all graphicalComponents from this portal
	 *
	 * @return iterator with graphicalComponents
	 */
	public Iterator<GraphicalComponent> getGraphicalComponents()
	{
		return getObjects(IRepository.GRAPHICALCOMPONENTS);
	}

	/**
	 * Create a new graphicalComponents
	 *
	 * @return the new graphicalComponents
	 */
	public GraphicalComponent createNewGraphicalComponent(Point location) throws RepositoryException
	{
		GraphicalComponent obj = (GraphicalComponent)getRootObject().getChangeHandler().createNewObject(this, IRepository.GRAPHICALCOMPONENTS);

		//set all the required properties
		obj.setLocation(location);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for Rectagle handling
	 */
	/**
	 * @deprecated
	 */
	@Deprecated
	public RectShape createNewRectangle(Point location) throws RepositoryException
	{
		RectShape obj = (RectShape)getRootObject().getChangeHandler().createNewObject(this, IRepository.RECTSHAPES);
		//set all the required properties

		obj.setLocation(location);
		obj.setLineSize(1);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */
	public int getScrollbars()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLBARS).intValue();
	}

	/**
	 * Set the scrollbars (bitset)
	 *
	 * @param i the bitset
	 */
	public void setScrollbars(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLBARS, i);
	}

	/**
	 * Create a new Shape
	 *
	 * @param location
	 * @return the new shape
	 */
	public Shape createNewShape(Point location) throws RepositoryException
	{
		Shape obj = (Shape)getRootObject().getChangeHandler().createNewObject(this, IRepository.SHAPES);
		//set all the required properties

		obj.setLocation(location);
		obj.setLineSize(1);
		addChild(obj);
		return obj;
	}

	/**
	 * The calculation dataprovider (such as servoy_row_bgcolor) used to add background
	 * color and highlight selected or alternate portal rows.
	 *
	 * NOTE: This property has been deprecated and is kept visible for legacy purposes. Use CSS Row Styling & onRender event instead.
	 */
	public String getRowBGColorCalculation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION);
	}

	/**
	 * Set the name of the bgcolorcalc.
	 *
	 * NOTE: This property has been deprecated and is kept visible for legacy purposes. Use CSS Row Styling & onRender event instead.
	 *
	 * @param arg the bgcolorcalc name
	 */
	public void setRowBGColorCalculation(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION, arg);
	}

	public Solution getSolution()
	{
		return (Solution)getRootObject();
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

	@Override
	public String toString()
	{
		String name = getName();
		if (name != null && !(name = getName().trim()).equals("")) //$NON-NLS-1$
		{
			return name;
		}
		else
		{
			return "no name/provider"; //$NON-NLS-1$
		}
	}

	/**
	 * The method that is triggered when (non Design Mode) dragging occurs.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDragMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID).intValue();
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDragMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID, arg);
	}

	/**
	 * The method that is triggered when (non Design Mode) dragging end occurs.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDragEndMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID).intValue();
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDragEndMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID, arg);
	}

	/**
	 * The method that is triggered when (non Design Mode) dragging over a component occurs.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDragOverMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID).intValue();
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDragOverMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID, arg);
	}

	/**
	 * The method that is triggered when (non Design Mode) dropping occurs.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDropMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID).intValue();
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDropMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID, arg);
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnRenderMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, arg);
	}

	/**
	 * The method that is executed when the component is rendered.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnRenderMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID).intValue();
	}

	public boolean isMobileInsetList()
	{
		return Boolean.TRUE.equals(getCustomMobileProperty(IMobileProperties.LIST_COMPONENT.propertyName));
	}

	public Boolean getNgReadOnlyMode()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_NG_READONLY_MODE });
		if (customProperty instanceof Boolean) return (Boolean)customProperty;
		return null;
	}

	public void setNgReadOnlyMode(Boolean readOnly)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_NG_READONLY_MODE }, readOnly);
	}
}
