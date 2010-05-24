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

import com.servoy.j2db.scripting.ServoyDocumented;
import com.servoy.j2db.util.UUID;

/**
 * Portal to view multiple related fields
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Portal extends BaseComponent implements ISupportFormElements, ISupportScrollbars, ISupportTabSeq
{
	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private String relationName;
	private boolean reorderable;
	private boolean resizeble;
	private boolean sortable;
	private boolean multiLine;
	private int rowHeight;
	private boolean showVerticalLines;
	private boolean showHorizontalLines;
	private Dimension intercellSpacing;
	private String initialSort;
	private int scrollbars;
	private String rowBGColorCalculation;
	private int tabSeq = ISupportTabSeq.DEFAULT;
	private int onDragMethodID;
	private int onDragOverMethodID;
	private int onDropMethodID;

	/**
	 * Constructor I
	 */
	Portal(ISupportChilds parent, int element_id, UUID uuid)
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
		checkForChange(relationName, arg);
		relationName = arg;
	}

	/**
	 * The name of the relationship between the table related to the currently active 
	 * form and the table you want to show data from in the portal.
	 */
	public String getRelationName()
	{
		return relationName;
	}

	/**
	 * Set the rowHeight
	 * 
	 * @param arg the rowHeight
	 */
	public void setRowHeight(int arg)
	{
		checkForChange(rowHeight, arg);
		rowHeight = arg;
	}

	/**
	 * The height of each row in pixels. If 0 or not set, the height defaults to 10.
	 */
	public int getRowHeight()
	{
		return rowHeight;
	}

	/**
	 * Set the reorderable
	 * 
	 * @param arg the reorderable
	 */
	public void setReorderable(boolean arg)
	{
		checkForChange(reorderable, arg);
		reorderable = arg;
	}

	/**
	 * When set, the portal rows can be re-ordered by dragging the column headers.  
	 */
	public boolean getReorderable()
	{
		return reorderable;
	}

	/**
	 * Set the resizeble
	 * 
	 * @param arg the resizeble
	 */
	public void setResizeble(boolean arg)
	{
		checkForChange(resizeble, arg);
		resizeble = arg;
	}

	/**
	 * When set the portal rows can be resized by users.
	 */
	public boolean getResizeble()
	{
		return resizeble;
	}

	/**
	 * Set the sortable
	 * 
	 * @param arg the sortable
	 */
	public void setSortable(boolean arg)
	{
		checkForChange(sortable, arg);
		sortable = arg;
	}

	/**
	 * When set, users can sort the contents of the portal by clicking on the column headings.
	 */
	public boolean getSortable()
	{
		return sortable;
	}

	/**
	 * Set the multiLine
	 * 
	 * @param arg the multiLine
	 */
	public void setMultiLine(boolean arg)
	{
		checkForChange(multiLine, arg);
		multiLine = arg;
	}

	/**
	 * When set, portal rows can have a custom layout of fields, buttons, etc. displayed for each 
	 * matching row (rather than the default "grid").
	 */
	public boolean getMultiLine()
	{
		return multiLine;
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
		return showVerticalLines;
	}

	/**
	 * Set the showVerticalLine
	 * 
	 * @param arg the showVerticalLine
	 */
	public void setShowVerticalLines(boolean arg)
	{
		checkForChange(showVerticalLines, arg);
		showVerticalLines = arg;
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
		return showHorizontalLines;
	}

	/**
	 * Set the showHorizontalLines
	 * 
	 * @param arg the showHorizontalLines
	 */
	public void setShowHorizontalLines(boolean arg)
	{
		checkForChange(showHorizontalLines, arg);
		showHorizontalLines = arg;
	}

	/**
	 * Set the intercellSpacing
	 * 
	 * @param arg the intercellSpacing
	 */
	public void setIntercellSpacing(Dimension arg)
	{
		checkForChange(intercellSpacing, arg);
		intercellSpacing = arg;
	}


	/**
	 * The additional spacing between cell rows. Is composed from the horizontal spacing
	 * and the vertical spacing.
	 */
	public Dimension getIntercellSpacing()
	{
		return intercellSpacing;
	}

	/**
	 * Sets the defaultSort.
	 * 
	 * @param arg The defaultSort to set
	 */
	public void setInitialSort(String arg)
	{
		checkForChange(initialSort, arg);
		initialSort = arg;
	}

	/**
	 * The default sort order for the rows displayed in the portal.
	 */
	public String getInitialSort()
	{
		return initialSort;
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
	public Iterator getRectangles() throws RepositoryException
	{
		return getObjects(IRepository.RECTSHAPES);
	}

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
		return scrollbars;
	}

	/**
	 * Set the scrollbars (bitset)
	 * 
	 * @param i the bitset
	 */
	public void setScrollbars(int i)
	{
		checkForChange(scrollbars, i);
		scrollbars = i;
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
	 */
	public String getRowBGColorCalculation()
	{
		return rowBGColorCalculation;
	}

	/**
	 * Set the name of the bgcolorcalc
	 * 
	 * @param arg the bgcolorcalc name
	 */
	public void setRowBGColorCalculation(String arg)
	{
		checkForChange(rowBGColorCalculation, arg);
		rowBGColorCalculation = arg;
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
	public int getOnDragMethodID()
	{
		return onDragMethodID;
	}

	public void setOnDragMethodID(int arg)
	{
		checkForChange(onDragMethodID, arg);
		onDragMethodID = arg;
	}

	/**
	 * The method that is triggered when (non Design Mode) dragging over a component occurs.
	 */
	public int getOnDragOverMethodID()
	{
		return onDragOverMethodID;
	}

	public void setOnDragOverMethodID(int arg)
	{
		checkForChange(onDragOverMethodID, arg);
		onDragOverMethodID = arg;
	}

	/**
	 * The method that is triggered when (non Design Mode) dropping occurs.
	 */
	public int getOnDropMethodID()
	{
		return onDropMethodID;
	}

	public void setOnDropMethodID(int arg)
	{
		checkForChange(onDropMethodID, arg);
		onDropMethodID = arg;
	}
}
