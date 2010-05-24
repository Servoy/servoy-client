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

import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.ServoyDocumented;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class MEDIAOPTION implements IPrefixedConstantsObject
{
	/**
	 * Constant used when setting the media option for components which display images.
	 * Makes the image be displayed at its original size. If the component is smaller
	 * than the image, then only a part of the image will show up.
	 * 
	 * @sample
	 * var bigBytes = plugins.file.readFile('d:/big.jpg');
	 * var bigImage = solutionModel.newMedia('big.jpg', bigBytes);
	 * var smallBytes = plugins.file.readFile('d:/small.jpg');
	 * var smallImage = solutionModel.newMedia('small.jpg', smallBytes);
	 * var smallLabelWithBigImageCrop = form.newLabel('', 130, 10, 50, 50);
	 * smallLabelWithBigImageCrop.imageMedia = bigImage;
	 * smallLabelWithBigImageCrop.background = 'yellow';	
	 * smallLabelWithBigImageCrop.mediaOptions = SM_MEDIAOPTION.CROP;
	 * var bigLabelWithSmallImageCrop = form.newLabel('', 10, 290, 200, 100);
	 * bigLabelWithSmallImageCrop.imageMedia = smallImage;
	 * bigLabelWithSmallImageCrop.background = 'yellow';
	 * bigLabelWithSmallImageCrop.mediaOptions = SM_MEDIAOPTION.CROP; // This does not do any cropping actually if the label is larger than the image.
	 */
	public static final int CROP = 1;

	/**
	 * Constant used when setting the media option for components which display images.
	 * Makes the image be scaled down to fit the size of the component, if the component
	 * is smaller than the image. It can be used in combination with KEEPASPECT in order
	 * to preserve the aspect ratio of the image.
	 * 
	 * It can also be used in combination with ENLARGE, to cover all possibilities when
	 * the size of the component is not known upfront.
	 * 
	 * @sample
	 * var bigBytes = plugins.file.readFile('d:/big.jpg');
	 * var bigImage = solutionModel.newMedia('big.jpg', bigBytes);
	 * var smallLabelWithBigImageReduceKeepAspect = form.newLabel('', 10, 10, 50, 50);
	 * smallLabelWithBigImageReduceKeepAspect.imageMedia = bigImage;
	 * smallLabelWithBigImageReduceKeepAspect.background = 'yellow';	
	 * smallLabelWithBigImageReduceKeepAspect.mediaOptions = SM_MEDIAOPTION.REDUCE | SM_MEDIAOPTION.KEEPASPECT;
	 * var smallLabelWithBigImageReduceNoAspect = form.newLabel('', 70, 10, 50, 50);
	 * smallLabelWithBigImageReduceNoAspect.imageMedia = bigImage;
	 * smallLabelWithBigImageReduceNoAspect.background = 'yellow';	
	 * smallLabelWithBigImageReduceNoAspect.mediaOptions = SM_MEDIAOPTION.REDUCE;
	 */
	public static final int REDUCE = 2;

	/**
	 * Constant used when setting the media option for components which display images.
	 * Makes the image be scaled up to fit the size of the component, if the component is
	 * larger than the image. Can be used in combination with KEEPASPECT in order to preserve
	 * the aspect ratio of the image.
	 * 
	 * It can also be used in combination with REDUCE, to cover all possibilities when
	 * the size of the component is not known upfront.
	 * 
	 * @sample
	 * var smallBytes = plugins.file.readFile('d:/small.jpg');
	 * var smallImage = solutionModel.newMedia('small.jpg', smallBytes);
	 * var bigLabelWithSmallImageEnlargeKeepAspect = form.newLabel('', 10, 70, 200, 100);
	 * bigLabelWithSmallImageEnlargeKeepAspect.imageMedia = smallImage;
	 * bigLabelWithSmallImageEnlargeKeepAspect.background = 'yellow';
	 * bigLabelWithSmallImageEnlargeKeepAspect.mediaOptions = SM_MEDIAOPTION.ENLARGE | SM_MEDIAOPTION.KEEPASPECT;
	 * var bigLabelWithSmallImageEnlargeNoAspect = form.newLabel('', 10, 180, 200, 100);
	 * bigLabelWithSmallImageEnlargeNoAspect.imageMedia = smallImage;
	 * bigLabelWithSmallImageEnlargeNoAspect.background = 'yellow';
	 * bigLabelWithSmallImageEnlargeNoAspect.mediaOptions = SM_MEDIAOPTION.ENLARGE;
	 */
	public static final int ENLARGE = 4;

	/**
	 * Constant used when setting the media option for components which display images.
	 * Can be used in combination with REDUCE and/or ENLARGE, to maintain the aspect
	 * ratio of the image while it is scaled down or up.
	 * 
	 * @sample
	 * var bigBytes = plugins.file.readFile('d:/big.jpg');
	 * var bigImage = solutionModel.newMedia('big.jpg', bigBytes);
	 * var smallBytes = plugins.file.readFile('d:/small.jpg');
	 * var smallImage = solutionModel.newMedia('small.jpg', smallBytes);
	 * var smallLabelWithBigImageReduceKeepAspect = form.newLabel('', 10, 10, 50, 50);
	 * smallLabelWithBigImageReduceKeepAspect.imageMedia = bigImage;
	 * smallLabelWithBigImageReduceKeepAspect.background = 'yellow';	
	 * smallLabelWithBigImageReduceKeepAspect.mediaOptions = SM_MEDIAOPTION.REDUCE | SM_MEDIAOPTION.KEEPASPECT;
	 * var bigLabelWithSmallImageEnlargeKeepAspect = form.newLabel('', 10, 70, 200, 100);
	 * bigLabelWithSmallImageEnlargeKeepAspect.imageMedia = smallImage;
	 * bigLabelWithSmallImageEnlargeKeepAspect.background = 'yellow';
	 * bigLabelWithSmallImageEnlargeKeepAspect.mediaOptions = SM_MEDIAOPTION.ENLARGE | SM_MEDIAOPTION.KEEPASPECT;
	 */
	public static final int KEEPASPECT = 8;


	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_MEDIAOPTION"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Media option Constants"; //$NON-NLS-1$
	}

}
