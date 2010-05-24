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
package com.servoy.j2db.scripting.solutionmodel;

import com.servoy.j2db.persistence.ISupportPrintSliding;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.ServoyDocumented;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class PRINTSLIDING implements IPrefixedConstantsObject
{
	/**
	 * Constant to be used when specifiying the print sliding for components.
	 * Makes the component not slide during printing. The component will
	 * maintain its designtime location and size.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var noSlidingLabel = form.newLabel('No sliding -- long text', 10, 10, 30, 20);
	 * noSlidingLabel.printSliding = SM_PRINT_SLIDING.NO_SLIDING;
	 * noSlidingLabel.background = 'red';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public final static int NO_SLIDING = ISupportPrintSliding.NO_SLIDING;

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.PRINTSLIDING#ALLOW_MOVE_X
	 */
	@Deprecated
	public final static int ALLOW_MOVE_PLUS_X = ISupportPrintSliding.ALLOW_MOVE_PLUS_X;

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.PRINTSLIDING#ALLOW_MOVE_X
	 */
	@Deprecated
	public final static int ALLOW_MOVE_MIN_X = ISupportPrintSliding.ALLOW_MOVE_MIN_X;

	/**
	 * Constant to be used when specifiying the print sliding for components.
	 * The component will move horizontally to align with its left neighbor,
	 * if that left neighbor moves or increases/decreases its size.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var growHorizLabel = form.newLabel('Grow horizontal -- long text', 10, 30, 30, 20);
	 * growHorizLabel.printSliding = SM_PRINT_SLIDING.GROW_WIDTH;
	 * growHorizLabel.background = 'blue';
	 * var moveHorizRightLabel = form.newLabel('Move horizontal right', 50, 30, 100, 20);
	 * moveHorizRightLabel.printSliding = SM_PRINT_SLIDING.ALLOW_MOVE_X;
	 * moveHorizRightLabel.background = 'pink';
	 * var shrinkHorizLabel = form.newLabel('Short', 10, 50, 100, 20);
	 * shrinkHorizLabel.printSliding = SM_PRINT_SLIDING.SHRINK_WIDTH;
	 * shrinkHorizLabel.background = 'green';
	 * var moveHorizLeftLabel = form.newLabel('Move horizontal left', 100, 50, 150, 20);
	 * moveHorizLeftLabel.printSliding = SM_PRINT_SLIDING.ALLOW_MOVE_X;
	 * moveHorizLeftLabel.background = 'magenta';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public final static int ALLOW_MOVE_X = ISupportPrintSliding.ALLOW_MOVE_MIN_X;

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.PRINTSLIDING#ALLOW_MOVE_Y
	 */
	@Deprecated
	public final static int ALLOW_MOVE_PLUS_Y = ISupportPrintSliding.ALLOW_MOVE_PLUS_Y;

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.PRINTSLIDING#ALLOW_MOVE_Y
	 */
	@Deprecated
	public final static int ALLOW_MOVE_MIN_Y = ISupportPrintSliding.ALLOW_MOVE_MIN_Y;

	/**
	 * Constant to be used when specifiying the print sliding for components.
	 * The component will move vertically to align with its top neighbor,
	 * if that neighbor moves or increases/decreases its size.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var growVertLabel = form.newLabel('Grow vertical', 10, 70, 100, 5);
	 * growVertLabel.printSliding = SM_PRINT_SLIDING.GROW_HEIGHT;
	 * growVertLabel.background = 'orange';
	 * var moveVertDownLabel = form.newLabel('Move vertical down', 10, 75, 100, 20);
	 * moveVertDownLabel.printSliding = SM_PRINT_SLIDING.ALLOW_MOVE_Y;
	 * moveVertDownLabel.background = 'cyan';
	 * var shrinkVertLabel = form.newLabel('Shrink vertical', 10, 110, 100, 40);
	 * shrinkVertLabel.printSliding = SM_PRINT_SLIDING.SHRINK_HEIGHT;
	 * shrinkVertLabel.background = 'yellow';
	 * var moveVertUpLabel = form.newLabel('Move vertical up', 10, 160, 100, 20);
	 * moveVertUpLabel.printSliding = SM_PRINT_SLIDING.ALLOW_MOVE_Y;
	 * moveVertUpLabel.background = 'purple';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public final static int ALLOW_MOVE_Y = ISupportPrintSliding.ALLOW_MOVE_MIN_Y;

	/**
	 * Constant to be used when specifiying the print sliding for components.
	 * The component will increase its width to adapt its content, if the 
	 * content is too large.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var growHorizLabel = form.newLabel('Grow horizontal -- long text', 10, 30, 30, 20);
	 * growHorizLabel.printSliding = SM_PRINT_SLIDING.GROW_WIDTH;
	 * growHorizLabel.background = 'blue';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public final static int GROW_WIDTH = ISupportPrintSliding.GROW_WIDTH;

	/**
	 * Constant to be used when specifiying the print sliding for components.
	 * The component will decrease its width to adapt its content, if the
	 * content is too small.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var shrinkHorizLabel = form.newLabel('Short', 10, 50, 100, 20);
	 * shrinkHorizLabel.printSliding = SM_PRINT_SLIDING.SHRINK_WIDTH;
	 * shrinkHorizLabel.background = 'green';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public final static int SHRINK_WIDTH = ISupportPrintSliding.SHRINK_WIDTH;

	/**
	 * Constant to be used when specifiying the print sliding for components.
	 * The component will increase its height to adapt its content, if
	 * the content is too large.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var growVertLabel = form.newLabel('Grow vertical', 10, 70, 100, 5);
	 * growVertLabel.printSliding = SM_PRINT_SLIDING.GROW_HEIGHT;
	 * growVertLabel.background = 'orange';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public final static int GROW_HEIGHT = ISupportPrintSliding.GROW_HEIGHT;

	/**
	 * Constant to be used when specifiying the print sliding for components.
	 * The component will decrease its height to adapt its content, if
	 * the content is too small.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var shrinkVertLabel = form.newLabel('Shrink vertical', 10, 110, 100, 40);
	 * shrinkVertLabel.printSliding = SM_PRINT_SLIDING.SHRINK_HEIGHT;
	 * shrinkVertLabel.background = 'yellow';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public final static int SHRINK_HEIGHT = ISupportPrintSliding.SHRINK_HEIGHT;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_PRINT_SLIDING"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Printsliding Constants"; //$NON-NLS-1$
	}

}
