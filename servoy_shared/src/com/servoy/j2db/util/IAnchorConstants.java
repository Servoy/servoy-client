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
package com.servoy.j2db.util;

public interface IAnchorConstants
{
	/**
	 * Constant to be used when specifying anchoring for components.
	 * Makes the component anchored to the top side of the window,
	 * which means that the component will keep a constant distance
	 * from the top side of the window. If SOUTH anchoring is also 
	 * enabled, then the component will grow/shrink as the window 
	 * is vertically resized.
	 * 
	 * This constant is used also for setting tab orientation on tab panels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var strechVerticallyLabel = form.newLabel('Strech vertically', 10, 10, 190, 280);
	 * strechVerticallyLabel.background = 'green';
	 * strechVerticallyLabel.anchors = SM_ANCHOR.WEST | SM_ANCHOR.NORTH | SM_ANCHOR.SOUTH;
	 * var stickToTopLeftCornerLabel = form.newLabel('Stick to top-left corner', 10, 10, 200, 100);
	 * stickToTopLeftCornerLabel.background = 'orange';
	 * stickToTopLeftCornerLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST;
	 */
	public static final int NORTH = 1;

	/**
	 * Constant to be used when specifying anchoring for components.
	 * Makes the component anchored to the right side of the window,
	 * which means that the component will keep a constant distance
	 * from the right side of the window. If WEST anchoring is also 
	 * enabled, then the component will grow/shrink as the window 
	 * is horizontally resized.
	 * 
	 * This constant is used also for setting tab orientation on tab panels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var strechHorizontallyLabel = form.newLabel('Strech horizontally', 10, 10, 380, 140);
	 * strechHorizontallyLabel.background = 'blue';
	 * strechHorizontallyLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST | SM_ANCHOR.EAST;
	 * var stickToBottomRightCornerLabel = form.newLabel('Stick to bottom-right corner', 190, 190, 200, 100);
	 * stickToBottomRightCornerLabel.background = 'pink';
	 * stickToBottomRightCornerLabel.anchors = SM_ANCHOR.SOUTH | SM_ANCHOR.EAST;
	 */
	public static final int EAST = 2;

	/**
	 * Constant to be used when specifying anchoring for components.
	 * Makes the component anchored to the bottom side of the window,
	 * which means that the component will keep a constant distance
	 * from the bottom side of the window. If NORTH anchoring is also 
	 * enabled, then the component will grow/shrink as the window 
	 * is vertically resized.
	 * 
	 * This constant is used also for setting tab orientation on tab panels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var strechVerticallyLabel = form.newLabel('Strech vertically', 10, 10, 190, 280);
	 * strechVerticallyLabel.background = 'green';
	 * strechVerticallyLabel.anchors = SM_ANCHOR.WEST | SM_ANCHOR.NORTH | SM_ANCHOR.SOUTH;
	 * var stickToBottomRightCornerLabel = form.newLabel('Stick to bottom-right corner', 190, 190, 200, 100);
	 * stickToBottomRightCornerLabel.background = 'pink';
	 * stickToBottomRightCornerLabel.anchors = SM_ANCHOR.SOUTH | SM_ANCHOR.EAST;
	 */
	public static final int SOUTH = 4;

	/**
	 * Constant to be used when specifying anchoring for components.
	 * Makes the component anchored to the left side of the window,
	 * which means that the component will keep a constant distance
	 * from the left side of the window. If EAST anchoring is also 
	 * enabled, then the component will grow/shrink as the window 
	 * is vertically resized.
	 * 
	 * This constant is used also for setting tab orientation on tab panels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var strechHorizontallyLabel = form.newLabel('Strech horizontally', 10, 10, 380, 140);
	 * strechHorizontallyLabel.background = 'blue';
	 * strechHorizontallyLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST | SM_ANCHOR.EAST;
	 * var stickToTopLeftCornerLabel = form.newLabel('Stick to top-left corner', 10, 10, 200, 100);
	 * stickToTopLeftCornerLabel.background = 'orange';
	 * stickToTopLeftCornerLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST;
	 */
	public static final int WEST = 8;

	/**
	 * Constant to be used when specifying anchoring for components.
	 * It is equivalent to a combination of NORTH and WEST anchoring.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var stickToTopLeftCornerLabel = form.newLabel('Stick to top-left corner', 10, 10, 200, 100);
	 * stickToTopLeftCornerLabel.background = 'orange';
	 * stickToTopLeftCornerLabel.anchors = SM_ANCHOR.DEFAULT;
	 */
	public static final int DEFAULT = NORTH + WEST;

	/**
	 * Constant to be used when specifying anchoring for components.
	 * Makes the component anchored on all sides. This means that
	 * the component will keep a constant distance from all sides
	 * of the window and will grow/shrink as the window is resized.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var strechAllDirectionsLabel = form.newLabel('Strech all directions', 10, 10, 380, 280);
	 * strechAllDirectionsLabel.background = 'red';
	 * strechAllDirectionsLabel.anchors = SM_ANCHOR.ALL;	
	 */
	public static final int ALL = NORTH + WEST + EAST + SOUTH;
}
