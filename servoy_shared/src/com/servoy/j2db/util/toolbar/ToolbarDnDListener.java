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
package com.servoy.j2db.util.toolbar;


/**
 * Listener for toolbar motion.
 * 
 * When somebody works (drag and drop) with any toolbar and this work comes up to this listener. Listener is adherented to ToolbarConfiguration so every
 * toolbar's motion is reflected in this ToolbarConfiguration and it's appropriate ToolbarConstraints. So this is only place, where the ToolbarConfiguration is
 * changed by toolbar's motion.
 * 
 * @author Libor Kramolis
 */
public class ToolbarDnDListener extends Object implements Toolbar.DnDListener
{
	protected static final int BASIC_HEIGHT_2 = (Toolbar.BASIC_HEIGHT / 2) + 2;
	protected static final int BASIC_HEIGHT_4 = (Toolbar.BASIC_HEIGHT / 4) + 1;

	/** now dragged toolbar */
	private ToolbarConstraints draggedToolbar;
	private final ToolbarPanel configuration;

	/**
	 * Create new Toolbar listener.
	 * 
	 * @param conf specified toolbat configuration.
	 */
	public ToolbarDnDListener(ToolbarPanel conf)
	{
		configuration = conf;
	}

	/**
	 * Move toolbar and followers horizontaly.
	 * 
	 * @param tc first moved toolbar
	 * @param dx horizontaly distance
	 */
	protected void moveToolbar2EndHorizontally(ToolbarConstraints tc, int dx)
	{
		if (!configuration.getComponentOrientation().isLeftToRight())
		{
			dx = -dx;
		}
		if (dx == 0) // no move
		return;

		if (dx < 0) tc.moveLeft2End(-dx);
		if (dx > 0) tc.moveRight2End(dx);
	}

	/**
	 * Move toolbar horizontaly.
	 * 
	 * @param tc moved toolbar
	 * @param dx horizontal distance
	 */
	protected void moveToolbarHorizontally(ToolbarConstraints tc, int dx)
	{
		if (!configuration.getComponentOrientation().isLeftToRight())
		{
			dx = -dx;
		}
		if (dx == 0) // no move
		return;

		if (dx < 0) tc.moveLeft(-dx);
		if (dx > 0) tc.moveRight(dx);
	}

	/**
	 * Move toolbar verticaly.
	 * 
	 * @param tc moved toolbar
	 * @param dy vertical distance
	 */
	protected void moveToolbarVertically(ToolbarConstraints tc, int dy)
	{
		if (dy == 0) // no move
		return;

		if (dy < 0) moveUp(tc, -dy);
		if (dy > 0) moveDown(tc, dy);
	}

	/**
	 * Try move toolbar up.
	 * 
	 * @param tc moved toolbar
	 * @param dy vertical distance
	 */
	protected void moveUp(ToolbarConstraints tc, int dy)
	{
		if (dy < BASIC_HEIGHT_2) return;

		int rI = tc.rowIndex();
		if (draggedToolbar.isAlone())
		{ // is alone on row(s) -> no new rows
			if (rI == 0) // in first row
			return;
		}

		int pos = rI - 1;
		tc.destroy();

		int plus = 0;
		int rowCount = configuration.getRowCount();
		for (int i = pos; i < pos + tc.getRowCount(); i++)
		{
			configuration.getRow(i + plus).addToolbar(tc, tc.getPosition());
			if (rowCount != configuration.getRowCount())
			{
				rowCount = configuration.getRowCount();
				plus++;
			}
		}
		configuration.checkToolbarRows();
	}

	/**
	 * Try move toolbar down.
	 * 
	 * @param tc moved toolbar
	 * @param dy vertical distance
	 */
	protected void moveDown(ToolbarConstraints tc, int dy)
	{
		int rI = tc.rowIndex();

		int step = BASIC_HEIGHT_2;

		if (draggedToolbar.isAlone())
		{ // is alone on row(s) -> no new rows
			if (rI == (configuration.getRowCount() - tc.getRowCount())) // in last rows
			return;
			step = BASIC_HEIGHT_4;
		}

		if (dy < step) return;

		int pos = rI + 1;
		tc.destroy();

		for (int i = pos; i < pos + tc.getRowCount(); i++)
			configuration.getRow(i).addToolbar(tc, tc.getPosition());

		configuration.checkToolbarRows();
	}

	///////////////////////////
	// from Toolbar.DnDListener

	/** Invoced when toolbar is dragged. */
	public void dragToolbar(Toolbar.DnDEvent e)
	{
		if (draggedToolbar == null)
		{
			draggedToolbar = configuration.getToolbarConstraints(e.getName());
		}

		switch (e.getType())
		{
			case Toolbar.DnDEvent.DND_LINE :
				// not implemented yet - it's bug [1]
				// not implemented in this version
				return; // only Toolbar.DnDEvent.DND_LINE
			case Toolbar.DnDEvent.DND_END :
				moveToolbar2EndHorizontally(draggedToolbar, e.getDX());
				break;
			case Toolbar.DnDEvent.DND_ONE :
				moveToolbarVertically(draggedToolbar, e.getDY());
				break;
		}
		if (e.getType() == Toolbar.DnDEvent.DND_ONE) moveToolbarHorizontally(draggedToolbar, e.getDX());

		draggedToolbar.updatePosition();

		configuration.revalidateWindow();
	}

	/** Invoced when toolbar is dropped. */
	public void dropToolbar(Toolbar.DnDEvent e)
	{
		dragToolbar(e);

		//configuration.reflectChanges();
		draggedToolbar = null;
	}
} // end of class ToolbarDnDListener

