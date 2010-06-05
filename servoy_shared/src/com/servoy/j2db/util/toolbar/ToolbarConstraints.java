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



import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Vector;

/** An object that encapsulates position and (optionally) size for
 * Absolute positioning of components.
 *
 * Each toolbar constraints (TC) is component of toolbar row(s) and configuration.
 * Every TC has cached the nearest neighbournhood, so there is a list of rows for TC is part,
 * list of previous and next toolbars.
 * So when there is some motion all of those cached attributes are recomputed.
 *
 * @author Libor Kramolis
 */
public class ToolbarConstraints {
    static final long serialVersionUID =3065774641403311880L;

    /** Toolbar anchor status. */
    static final int LEFT_ANCHOR  = -1;
    static final int NO_ANCHOR    =  0;

    /** Toolbar name */
    private String    name;
    /** Toolbar horizontal position */
    private int       position;
    /** Which anchor toolbar use. */
    private int       anchor;    // LEFT_ANCHOR | NO_ANCHOR
    /** Is toolbar visible. */
    private boolean   visible;
    /** Toolbar is part of those rows. */
    private Vector    ownRows;   // Vector of ToolbarRows
    /** List of previous toolbars. */
    private Vector    prevBars;  // Vector of ToolbarConstraints
    /** List of next toolbars. */
    private Vector    nextBars;  // Vector of ToolbarConstraints
    /** The nearest end of previous toolbars. */
    private int       prevEnd;   // nejblizsi konec predchozich toolbaru
    /** The nearest begin of next toolbars. */
    private int       nextBeg;   // nejblizsi zacatek nasledujicich toolbaru
    /** The nearest begin of previous toolbars. */
    private int       prevBeg;   // nejblizsi zacatek predchozich toolbaru
    /** The nearest end of next toolbars. */
    private int       nextEnd;   // nejblizsi konec nasledujicich toolbaru

    /** Preferred size. */
    private Dimension prefSize;
    /** Toolbar bounds. */
    private Rectangle bounds;
    /** Toolbar constraints is part of ToolbarConfiguration. */
    private ToolbarPanel toolbarConfig;
    /** Number of rows. */
    private int       rowCount;
    /** Width of last toolbar. */
    private int       prefLastWidth;
    /** Last row index. */
    private int       lastRowIndex;

    /** Create new ToolbarConstraints
     * @param conf own ToolbarConfiguration
     * @param nam name of toolbar
     * @param pos wanted position of toolbar
     * @param vis visibility of toolbar
     */
    ToolbarConstraints (ToolbarPanel conf, String nam, Integer pos, Boolean vis) {
        toolbarConfig = conf;
        name = nam;
        if (pos == null) {
            position = 0;
            anchor = LEFT_ANCHOR;
        } else {
            position = pos.intValue();
            anchor = NO_ANCHOR;
        }
        visible = vis.booleanValue();

        prefSize = new Dimension ();
        rowCount = 0;
        prefLastWidth = 0;
        bounds = new Rectangle ();

        initValues();
    }

    /** Init neighbourhood values. */
    void initValues () {
        ownRows = new Vector();
        prevBars = new Vector();
        nextBars = new Vector();

        resetPrev();
        resetNext();
    }

    /** Checks position and visibility of multirow toolbar.
     * @param position maybe new position
     * @param visible maybe new visibility
     */
    void checkNextPosition (Integer position, Boolean visible) {
        if (position == null) {
            this.position = 0;
            this.anchor = LEFT_ANCHOR;
        } else {
            if (anchor == NO_ANCHOR)
                this.position = (this.position + position.intValue()) / 2;
            else
                this.position = position.intValue();
            this.anchor = NO_ANCHOR;
        }
        this.visible = this.visible || visible.booleanValue();
    }

    /** @return name of toolbar. */
    String getName () {
        return name;
    }

    /** @return anchor of toolbar. */
    int getAnchor () {
        return anchor;
    }

    /** Set anchor of toolbar.
     * @param anch new toolbar anchor.
     */
    void setAnchor (int anch) {
        anchor = anch;
    }

    /** @return toolbar visibility. */
    boolean isVisible () {
        return visible;
    }

    /** Set new toolbar visibility.
     * @param v new toolbar visibility
     */
    void setVisible (boolean v) {
        visible = v;
    }

    /** @return horizontal toolbar position. */
    int getPosition () {
        return position;
    }

    /** Set new toolbar position.
     * @param pos new toolbar position
     */
    void setPosition (int pos) {
        position = pos;
    }

    /** @return toolbar width. */
    int getWidth () {
        return prefSize.width;
    }

    /** @return number toolbar rows. */
    int getRowCount () {
        return rowCount;
    }

    /** @return toolbar bounds. */
    Rectangle getBounds () {
        return new Rectangle (bounds);
    }

    /** Destroy toolbar and it's neighbourhood (row context).
     * @return true if after destroy stay some empty row.
     */
    boolean destroy () {
        lastRowIndex = rowIndex();
        rowCount = ownRows.size();

        Iterator it = ownRows.iterator();
        ToolbarRow row;
        boolean emptyRow = false;
        while (it.hasNext()) {
            row = (ToolbarRow)it.next();
            row.removeToolbar (this);
            emptyRow = emptyRow || row.isEmpty();
        }
        initValues();
        return emptyRow;
    }

    /** Add row to owned rows.
     * @param row new owned row.
     */
    void addOwnRow (ToolbarRow row) {
        ownRows.add (row);
    }

    /** Add toolbar to list of previous toolbars.
     * @param prev new previous toolbar
     */
    void addPrevBar (ToolbarConstraints prev) {
        if (prev == null)
            return;
        prevBars.add (prev);
    }

    /** Add toolbar to list of next toolbars.
     * @param next new next toolbar
     */
    void addNextBar (ToolbarConstraints next) {
        if (next == null)
            return;
        nextBars.add (next);
    }

    /** Remove toolbar from previous toolbars.
     * @param prev toolbar for remove.
     */
    void removePrevBar (ToolbarConstraints prev) {
        if (prev == null)
            return;
        prevBars.remove (prev);
    }

    /** Remove toolbar from next toolbars.
     * @param next toolbar for remove.
     */
    void removeNextBar (ToolbarConstraints next) {
        if (next == null)
            return;
        nextBars.remove (next);
    }

    /** Set preferred size of toolbar. There is important recompute toolbar neighbourhood.
     * @param size preferred size
     */
    void setPreferredSize (Dimension size) {
        prefSize = size;
        rowCount = Toolbar.rowCount (prefSize.height);

        if (ownRows.isEmpty())
            return;

        ToolbarRow row;

        if (visible) {
            boolean emptyRow = false;
            while (rowCount < ownRows.size()) {
                row = (ToolbarRow)ownRows.lastElement();
                row.removeToolbar (this);
                ownRows.remove (row);
                emptyRow = emptyRow || row.isEmpty();
            }
            if (emptyRow)
                toolbarConfig.checkToolbarRows();
            while (rowCount > ownRows.size()) {
                row = (ToolbarRow)ownRows.lastElement();
                ToolbarRow nR = row.getNextRow();
                if (nR == null)
                    nR = toolbarConfig.createLastRow();
                nR.addToolbar (this, position);
            }
        }
        updatePosition();
    }

    /** @return index of first toolbar row. */
    int rowIndex () {
        if (!visible)
            return toolbarConfig.getRowCount();
        if (ownRows.isEmpty())
            return lastRowIndex;
        return toolbarConfig.rowIndex (((ToolbarRow)ownRows.firstElement()));
    }

    /** @return true if toolbar is alone at row(s). */
    boolean isAlone () {
        Iterator it = ownRows.iterator();
        ToolbarRow row;
        while (it.hasNext()) {
            row = (ToolbarRow)it.next();
            if (row.toolbarCount() != 1)
                return false;
        }
        return true;
    }

    /** Update preferred size of toolbar.
     * @param size new preferred size
     */
    void updatePreferredSize (Dimension size) {
        if (!prefSize.equals (size)) {
            setPreferredSize (size);
        }
    }

    /** Update toolbar bounds. */
    void updateBounds () {
        int rI = rowIndex();
        int rC = getRowCount();
        bounds = new Rectangle (position,
                                ((Toolbar.BASIC_HEIGHT + ToolbarLayout.VGAP) * rI) + ToolbarLayout.VGAP,
                                nextBeg - position - ToolbarLayout.HGAP,
                                (Toolbar.BASIC_HEIGHT * rC) + ((rC - 1) * ToolbarLayout.VGAP));
    }

    /** Update toolbar position and it's neighbourhood. */
    void updatePosition () {
        updatePrev();
        if (anchor == NO_ANCHOR) {
            if (position < (prevEnd + ToolbarLayout.HGAP)) {
                position = prevEnd + ToolbarLayout.HGAP;
                anchor = LEFT_ANCHOR;
            }
        } else {
            position = prevEnd + ToolbarLayout.HGAP;
        }
        updatePrevBars();
        updateNextBars();
        updateBounds();
        updatePrefWidth();
    }

    /** Update positions of previous toolbars. */
    void updatePrevPosition () {
        Iterator it = prevBars.iterator();
        ToolbarConstraints tc;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            tc.updatePosition();
        }
    }

    /** Update next position of previous toolbars. */
    void updatePrevBars () {
        Iterator it = prevBars.iterator();
        ToolbarConstraints tc;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            tc.updateNext();
        }
    }

    /** Update previous position of next toolbars. */
    void updateNextBars () {
        Iterator it = nextBars.iterator();
        ToolbarConstraints tc;
        if (!it.hasNext()) {
            resetNext();
            updatePrefWidth();
        }
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            tc.updatePosition();
        }
    }

    /** Update width of prevoius toolbars. */
    void updatePrefWidth () {
        if (nextBars.size() == 0) {
            prefLastWidth = getPosition() + getWidth() + ToolbarLayout.HGAP;
            toolbarConfig.updatePrefWidth();
        }
    }

    /** @return preferred toolbar width. */
    int getPrefWidth () {
        return prefLastWidth;
    }

    /** Update values about next toolbars. */
    void updateNext () {
        resetNext();
        Iterator it = nextBars.iterator();
        ToolbarConstraints tc;
        int nextPos;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            nextBeg = Math.min (nextBeg, nextPos = tc.getPosition());
            nextEnd = Math.min (nextEnd, nextPos + tc.getWidth());
        }
        updateBounds();
    }

    /** Update values about previous toolbars. */
    void updatePrev () {
        resetPrev();
        Iterator it = prevBars.iterator();
        ToolbarConstraints tc;
        int prevPos;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            prevBeg = Math.max (prevBeg, prevPos = tc.getPosition());
            prevEnd = Math.max (prevEnd, prevPos + tc.getWidth());
        }
    }

    /** Reset values about previous toolbars. */
    void resetPrev () {
        prevBeg = 0;
        prevEnd = 0;
    }

    /** Reset values about next toolbars. */
    void resetNext () {
        nextBeg = Integer.MAX_VALUE;
        nextEnd = Integer.MAX_VALUE;
    }

    /** Move toolbar left if it's possible.
     * @param dx horizontal distance
     */
    void moveLeft (int dx) {
        int wantX = position - dx;

        position = wantX;
        anchor = NO_ANCHOR;
        if (wantX > prevEnd) { // no problem to move left
            setAnchorTo (NO_ANCHOR, nextBars);
        } else {
            if (canSwitchLeft (getPosition(), getWidth(), prevBeg, prevEnd - prevBeg)) { // can switch left ?
                switchToolbarLeft ();
            }
        }
    }

    /** Move toolbar right if it's possible.
     * @param dx horizontal distance
     */
    void moveRight (int dx) {
        int wantX = position + dx;
        int wantXpWidth = wantX + getWidth(); // wantX plus width

        if (wantXpWidth < nextBeg) { // no problem to move right
            anchor = NO_ANCHOR;
            position = wantX;
        } else {
            if (canSwitchRight (wantX, getWidth(), nextBeg, nextEnd - nextBeg)) { // can switch right ?
                position = wantX;
                anchor = NO_ANCHOR;
                switchToolbarRight ();
            } else {
                position = nextBeg - getWidth() - ToolbarLayout.HGAP;
                anchor = NO_ANCHOR;
            }
        }

        updatePrevPosition();
    }

    /** Move toolbar left with all followers. */
    void moveLeft2End (int dx) {
        int wantX = position - dx;

        anchor = NO_ANCHOR;
        if (wantX < (prevEnd + ToolbarLayout.HGAP)) {
            wantX = prevEnd + ToolbarLayout.HGAP;
        }
        move2End (wantX - position);
    }

    /** Move toolbar right with all followers. */
    void moveRight2End (int dx) {
        move2End (dx);
    }

    /** Move toolbar horizontal with all followers. */
    void move2End (int dx) {
        position += dx;
        Iterator it = nextBars.iterator();
        ToolbarConstraints tc;
        int nextPos;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            tc.move2End (dx);
        }
    }

    /** Set anchor to list of toolbars.
     * @param anch type of anchor
     * @param bars list of toolbars
     */
    void setAnchorTo (int anch, Vector bars) {
        Iterator it = bars.iterator();
        ToolbarConstraints tc;
        while (it.hasNext()) {
            tc = (ToolbarConstraints)it.next();
            tc.setAnchor (anch);
        }
    }

    /** Switch toolbar left if it's possible. */
    void switchToolbarLeft () {
        Iterator it = ownRows.iterator();
        ToolbarRow row;
        while (it.hasNext()) {
            row = (ToolbarRow)it.next();
            row.trySwitchLeft (this);
        }
    }

    /** Switch toolbar right if it's possible. */
    void switchToolbarRight () {
        Iterator it = ownRows.iterator();
        ToolbarRow row;
        while (it.hasNext()) {
            row = (ToolbarRow)it.next();
            row.trySwitchRight (this);
        }
    }

    /** Can switch toolbar left?
     * @param p1 toolbar1 position
     * @param w1 toolbar1 width
     * @param p2 toolbar2 position
     * @param w2 toolbar2 width
     * @return true if possible switch toolbar left.
     */
    static boolean canSwitchLeft (int p1, int w1, int p2, int w2) {
        return (p1 < (p2));
    }

    /** Can switch toolbar right?
     * @param p1 toolbar1 position
     * @param w1 toolbar1 width
     * @param p2 toolbar2 position
     * @param w2 toolbar2 width
     * @return true if possible switch toolbar right.
     */
    static boolean canSwitchRight (int p1, int w1, int p2, int w2) {
        return (p1 > (p2));
    }

} // end of class ToolbarConstraints

