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



import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents one row of toolbars.
 *
 * Toolbar row is part of toolbar configuration and contains list of toolbars,
 * it is possible to add, remove and switch constraints.
 * There is cached row's neighbournhood, so when there is some row motion
 * those cached values are recomputed.
 *
 * @author Libor Kramolis
 */
public class ToolbarRow {
    /** ToolbarConfiguration */
    ToolbarPanel toolbarConfig;
    /** Previous row of toolbars. */
    ToolbarRow prevRow;
    /** Next row of toolbars. */
    ToolbarRow nextRow;

    /** List of toolbars (ToolbarConstraints) in row. */
    private Vector toolbars;

    /** Create new ToolbarRow.
     * @param own ToolbarConfiguration
     */
    ToolbarRow (ToolbarPanel config) {
        toolbarConfig = config;
        toolbars = new Vector();
        prevRow = nextRow = null;
    }

    /** Add toolbar to end of row.
     * @param tc ToolbarConstraints
     */
    void addToolbar (ToolbarConstraints tc) {
        addToolbar2 (tc, toolbars.size());
    }

    /** Add toolbar to specific position
     * @param newTC ToolbarConstraints
     * @param pos specified position of new toolbar
     */
    void addToolbar (ToolbarConstraints newTC, int pos) {
        int index = 0;
        Iterator it = toolbars.iterator();
        ToolbarConstraints tc;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            if (pos < tc.getPosition())
                break;
            index++;
        }
        addToolbar2 (newTC, index);
    }

    /** Add toolbar to specific index int row
     * @param tc ToolbarConstraints
     * @param index specified index of new toolbar
     */
    private void addToolbar2 (ToolbarConstraints tc, int index) {
        if (toolbars.contains (tc))
            return;

        ToolbarConstraints prev = null;
        ToolbarConstraints next = null;
        if (index != 0) {
            prev = (ToolbarConstraints)toolbars.elementAt (index - 1);
            prev.addNextBar (tc);
            tc.addPrevBar (prev);
        }
        if (index < toolbars.size()) {
            next = (ToolbarConstraints)toolbars.elementAt (index);
            tc.addNextBar (next);
            next.addPrevBar (tc);
        }
        if ((prev != null) && (next != null)) {
            prev.removeNextBar (next);
            next.removePrevBar (prev);
        }

        tc.addOwnRow (this);
        toolbars.insertElementAt (tc, index);

        tc.updatePosition();
    }

    /** Remove toolbar from row.
     * @param tc toolbar for remove
     */
    void removeToolbar (ToolbarConstraints tc) {
        int index = toolbars.indexOf (tc);

        ToolbarConstraints prev = null;
        ToolbarConstraints next = null;
        try {
            prev = (ToolbarConstraints)toolbars.elementAt (index - 1);
            prev.removeNextBar (tc);
        } catch (ArrayIndexOutOfBoundsException e) { }
        try {
            next = (ToolbarConstraints)toolbars.elementAt (index + 1);
            next.removePrevBar (tc);
            next.setAnchor (ToolbarConstraints.NO_ANCHOR);
        } catch (ArrayIndexOutOfBoundsException e) { }
        if ((prev != null) && (next != null)) {
            prev.addNextBar (next);
            next.addPrevBar (prev);
        }

        toolbars.removeElement (tc);

        if (prev != null) {
            prev.updatePosition();
        } else {
            if (next != null) {
                next.updatePosition();
            }
        }
    }

    /** @return Iterator of toolbars int row. */
    Iterator iterator () {
        return toolbars.iterator();
    }

    /** Set a previous row.
     * @param prev new previous row.
     */
    void setPrevRow (ToolbarRow prev) {
        prevRow = prev;
    }

    /** @return previous row. */
    ToolbarRow getPrevRow () {
        return prevRow;
    }

    /** Set a next row.
     * @param next new next row.
     */
    void setNextRow (ToolbarRow next) {
        nextRow = next;
    }

    /** @return next row. */
    ToolbarRow getNextRow () {
        return nextRow;
    }

    /** @return preferred width of row. */
    int getPrefWidth () {
        if (toolbars.isEmpty())
            return -1;
        return ((ToolbarConstraints)toolbars.lastElement()).getPrefWidth();
    }

    /** @return true if row is empty */
    boolean isEmpty () {
        return toolbars.isEmpty();
    }

    /** @return number of toolbars int row. */
    int toolbarCount () {
        return toolbars.size();
    }

    /** Update bounds of all row toolbars. */
    void updateBounds () {
        Iterator it = toolbars.iterator();
        ToolbarConstraints tc;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            tc.updateBounds();
        }
    }

    /** Switch two toolbars.
     * @param left ToolbarConstraints
     * @param right ToolbarConstraints
     */
    void switchBars (ToolbarConstraints left, ToolbarConstraints right) {
        int leftIndex = toolbars.indexOf (left);
        int rightIndex = toolbars.indexOf (right);
        ToolbarConstraints leftPrev = null;
        ToolbarConstraints rightNext = null;

        try {
            leftPrev = (ToolbarConstraints)toolbars.elementAt (leftIndex - 1);
        } catch (ArrayIndexOutOfBoundsException e) { }
        try {
            rightNext = (ToolbarConstraints)toolbars.elementAt (rightIndex + 1);
        } catch (ArrayIndexOutOfBoundsException e) { }

        if (leftPrev != null)
            leftPrev.removeNextBar (left);
        left.removePrevBar (leftPrev);
        left.removeNextBar (right);

        right.removePrevBar (left);
        right.removeNextBar (rightNext);
        if (rightNext != null)
            rightNext.removePrevBar (right);

        if (leftPrev != null)
            leftPrev.addNextBar (right);
        left.addPrevBar (right);
        left.addNextBar (rightNext);

        right.addPrevBar (leftPrev);
        right.addNextBar (left);
        if (rightNext != null)
            rightNext.addPrevBar (left);

        toolbars.setElementAt (left, rightIndex);
        toolbars.setElementAt (right, leftIndex);
    }

    /** Let's try switch toolbar left.
     * @param ToolbarConstraints
     */
    void trySwitchLeft (ToolbarConstraints tc) {
        int index = toolbars.indexOf (tc);
        if (index == 0)
            return;

        try {
            ToolbarConstraints prev = (ToolbarConstraints)toolbars.elementAt (index - 1);
            if (ToolbarConstraints.canSwitchLeft (tc.getPosition(), tc.getWidth(), prev.getPosition(), prev.getWidth())) {
                switchBars (prev, tc);
            }
        } catch (ArrayIndexOutOfBoundsException e) { /* No left toolbar - it means tc is toolbar like Palette (:-)) */ }
    }

    /** Let's try switch toolbar right.
     * @param ToolbarConstraints
     */
    void trySwitchRight (ToolbarConstraints tc) {
        int index = toolbars.indexOf (tc);

        try {
            ToolbarConstraints next = (ToolbarConstraints)toolbars.elementAt (index + 1);
            if (ToolbarConstraints.canSwitchRight (tc.getPosition(), tc.getWidth(), next.getPosition(), next.getWidth())) {
                switchBars (tc, next);
                next.setPosition (tc.getPosition() - next.getWidth() - ToolbarLayout.HGAP);
            }
        } catch (ArrayIndexOutOfBoundsException e) { /* No right toolbar - it means tc is toolbar like Palette (:-)) */ }
    }

	/**
	 * @param name
	 * @return
	 */
	public int indexOf(String name)
	{
		for (int i = 0; i < toolbars.size(); i++)
		{
			ToolbarConstraints toolbar = (ToolbarConstraints) toolbars.get(i);
			if(name.equals(toolbar.getName()))
			{
				return i;
			}
		}
		return -1;
	}

} // end of class ToolbarRow

