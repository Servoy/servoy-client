/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.solutionmodel;

import com.servoy.base.solutionmodel.IBaseSMGraphicalComponent;


/**
 * Solution model base interface for graphical components.
 *
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMGraphicalComponent extends IBaseSMGraphicalComponent, ISMComponent
{

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getHorizontalAlignment()
	 * @see com.servoy.j2db.solutionmodel.ISMField#getHorizontalAlignment()
	 *
	 * @sample
	 * var leftAlignedLabel = form.newLabel('LEFT', 10, 10, 300, 20);
	 * leftAlignedLabel.horizontalAlignment = SM_ALIGNMENT.LEFT;
	 * var hCenteredLabel = form.newLabel('CENTER', 10, 40, 300, 20);
	 * hCenteredLabel.horizontalAlignment = SM_ALIGNMENT.CENTER;
	 * var rightAlignedLabel = form.newLabel('RIGHT', 10, 70, 300, 20);
	 * rightAlignedLabel.horizontalAlignment = SM_ALIGNMENT.RIGHT;
	 */
	public int getHorizontalAlignment();

	/**
	 * The image Media object that should be displayed inside the component.
	 *
	 * @sample
	 * var ballBytes = plugins.file.readFile('d:/ball.jpg');
	 * var ballImage = solutionModel.newMedia('ball.jpg', ballBytes);
	 * var label = form.newLabel('', 10, 10, 100, 100);
	 * label.imageMedia = ballImage;
	 */
	public ISMMedia getImageMedia();

	/**
	 * @see com.servoy.j2db.solutionmodel.ISMField#getMargin()
	 *
	 * @sample
	 * var label = form.newLabel('Label', 10, 10, 150, 150);
	 * label.background = 'yellow';
	 * label.margin = '10,20,30,40';
	 */
	public String getMargin();

	/**
	 * Options to scale the image Media object that is displayed inside the component.
	 * Can be set to one or a combination of CROP, REDUCE, ENLARGE and KEEPASPECT.
	 *
	 * REDUCE will scale down the image if the component is smaller than the image.
	 * REDUCE combined with KEEPASPECT will reduce the image, but keep its aspect ratio.
	 * This is useful when the component has other proportions than the image.
	 *
	 * ENLARGE will scale up the image if the component is larger than the image.
	 * ENLARGE combined with KEEPASPECT will scale up the image while keeping its aspect ratio.
	 *
	 * CROP will leave the image at its original size. If the component is smaller than
	 * the image this will result in only a part of the image showing up.
	 *
	 * @sample
	 * 	// Load two images, a big one and a small one.
	 * var bigBytes = plugins.file.readFile('d:/big.jpg');
	 * var bigImage = solutionModel.newMedia('big.jpg', bigBytes);
	 * var smallBytes = plugins.file.readFile('d:/small.jpg');
	 * var smallImage = solutionModel.newMedia('small.jpg', smallBytes);
	 * // Put the big image in several small labels, with different media options.
	 * var smallLabelWithBigImageReduceKeepAspect = form.newLabel('', 10, 10, 50, 50);
	 * smallLabelWithBigImageReduceKeepAspect.imageMedia = bigImage;
	 * smallLabelWithBigImageReduceKeepAspect.background = 'yellow';
	 * smallLabelWithBigImageReduceKeepAspect.mediaOptions = SM_MEDIAOPTION.REDUCE | SM_MEDIAOPTION.KEEPASPECT;
	 * var smallLabelWithBigImageReduceNoAspect = form.newLabel('', 70, 10, 50, 50);
	 * smallLabelWithBigImageReduceNoAspect.imageMedia = bigImage;
	 * smallLabelWithBigImageReduceNoAspect.background = 'yellow';
	 * smallLabelWithBigImageReduceNoAspect.mediaOptions = SM_MEDIAOPTION.REDUCE;
	 * var smallLabelWithBigImageCrop = form.newLabel('', 130, 10, 50, 50);
	 * smallLabelWithBigImageCrop.imageMedia = bigImage;
	 * smallLabelWithBigImageCrop.background = 'yellow';
	 * smallLabelWithBigImageCrop.mediaOptions = SM_MEDIAOPTION.CROP;
	 * // Put the small image in several big labels, with different media options.
	 * var bigLabelWithSmallImageEnlargeKeepAspect = form.newLabel('', 10, 70, 200, 100);
	 * bigLabelWithSmallImageEnlargeKeepAspect.imageMedia = smallImage;
	 * bigLabelWithSmallImageEnlargeKeepAspect.background = 'yellow';
	 * bigLabelWithSmallImageEnlargeKeepAspect.mediaOptions = SM_MEDIAOPTION.ENLARGE | SM_MEDIAOPTION.KEEPASPECT;
	 * var bigLabelWithSmallImageEnlargeNoAspect = form.newLabel('', 10, 180, 200, 100);
	 * bigLabelWithSmallImageEnlargeNoAspect.imageMedia = smallImage;
	 * bigLabelWithSmallImageEnlargeNoAspect.background = 'yellow';
	 * bigLabelWithSmallImageEnlargeNoAspect.mediaOptions = SM_MEDIAOPTION.ENLARGE;
	 * var bigLabelWithSmallImageCrop = form.newLabel('', 10, 290, 200, 100);
	 * bigLabelWithSmallImageCrop.imageMedia = smallImage;
	 * bigLabelWithSmallImageCrop.background = 'yellow';
	 * bigLabelWithSmallImageCrop.mediaOptions = SM_MEDIAOPTION.CROP; // This does not do any cropping actually if the label is larger than the image.
	 */
	public int getMediaOptions();

	/**
	 * The keyboard shortcut that activates this component. A letter must be specified,
	 * and the actual shortcut will be combination of ALT + the specified letter.
	 *
	 * This property can be used in two ways. Normally the keyboard shortcut activates
	 * the onClick event of the component. But if the "labelFor" property is set for the
	 * component, then the keyboard shortcut will move the focus to the component whose
	 * label this component is.
	 *
	 * @sample
	 * var m = form.newMethod('function onClick() { application.output("I was clicked."); }');
	 * var btn = form.newButton('I am a button', 10, 40, 200, 20, m);
	 * btn.mnemonic = 'B'; // When ALT-B is pressed the mouse will respond as if clicked.
	 * var labelOne = form.newLabel('Label One', 10, 10, 100, 20);
	 * var fieldOne = form.newField('parent_table_id', JSField.TEXT_FIELD, 120, 10, 100, 20);
	 * fieldOne.name = 'fieldOne';
	 * labelOne.labelFor = 'fieldOne';
	 * labelOne.mnemonic = 'O'; // When ALT-O is pressed the focus will move to fieldOne.
	 */
	public String getMnemonic();

	/**
	 * The cursor that is shown as the mouse is rolled over the component.
	 * Possible options are DEFAULT and HAND.
	 *
	 * @sample
	 * var label = form.newLabel('Move the mouse over me', 10, 10, 200, 200);
	 * label.rolloverCursor = SM_CURSOR.HAND_CURSOR;
	 */
	public int getRolloverCursor();

	/**
	 * The roll over image Media object used. It will only work if a property image is also used.
	 * When the mouse is moved over the component, this image Media will be displayed.
	 * When the mouse is moved out of the component, whatever text or image was being initially
	 * displayed will be restored.
	 *
	 * @sample
	 * var ballBytes = plugins.file.readFile('d:/ball.jpg');
	 * var ballImage = solutionModel.newMedia('ball.jpg', ballBytes);
	 * var mapBytes = plugins.file.readFile('d:/map.jpg');
	 * var mapImage = solutionModel.newMedia('map.jpg', mapBytes);
	 * var label = form.newLabel('', 10, 10, 200, 200);
	 * label.imageMedia = mapImage;
	 * label.rolloverImageMedia = ballImage;
	 */
	public ISMMedia getRolloverImageMedia();

	/**
	 * The rotation of the element. You can choose 0, 90, 180, or 270 and the label is rotated accordingly.
	 * This property also applies to buttons and images.
	 *
	 * @sample
	 * var m = form.newMethod('function onClick() { application.output("I was clicked."); }');
	 * var label = form.newLabel('I am a label', 10, 10, 200, 200, m);
	 * label.rotation = 90;
	 * var btn = form.newButton('And I am a button', 10, 220, 200, 20, m);
	 * btn.rotation = 180;
	 */
	public int getRotation();

	/**
	 * When set, the element will show the clicked state when selected.
	 * Applies to labels and buttons and images only.
	 *
	 * @sample
	 * // Create a form method.
	 * var m = form.newMethod('function onClick() { application.output("I was clicked."); }');
	 * // Create a label with the method attached to its onClick event.
	 * var label = form.newLabel('I am a label', 10, 10, 200, 20, m);
	 * // By default the label does not visually react to clicks, but we can enable this.
	 * // Basically the label will now behave as a button does.
	 * label.showClick = true;
	 * // Create a button with the same method attached to its onClick event.
	 * var btn = form.newButton('And I am a button', 10, 40, 200, 20, m);
	 * // By default the button visually reacts to onClick, but we can disable this.
	 * // Then the button will behave like a label does.
	 * btn.showClick = false;
	 */
	public boolean getShowClick();

	/**
	 * When set the text of an element will showfocus when selected.
	 * Applies to labels and buttons only.
	 * The text property for the element MUST be filled in first.
	 *
	 * NOTE: The TAB key may also be used to select the element, depending
	 * on the operating system being used and the selected LAF.
	 *
	 * @sample
	 * var m = form.newMethod('function onClick() { application.output("I was clicked."); }');
	 * var label = form.newLabel('I am a label', 10, 10, 200, 20, m);
	 * label.showFocus = false;
	 * var btn = form.newButton('And I am a button', 10, 40, 200, 20, m);
	 * btn.showFocus = false;
	 */
	public boolean getShowFocus();

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getTabSeq()
	 * @see com.servoy.j2db.solutionmodel.ISMField#getTabSeq()
	 *
	 * @sample
	 * // Create three fields. Based on how they are placed, by default they will come one
	 * // after another in the tab sequence.
	 * var fieldOne = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var fieldTwo = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * var fieldThree = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 70, 100, 20);
	 * // Set the third field come before the first in the tab sequence, and remove the
	 * // second field from the tab sequence.
	 * fieldOne.tabSeq = 2;
	 * fieldTwo.tabSeq = SM_DEFAULTS.IGNORE;
	 * fieldThree.tabSeq = 1;
	 */
	public int getTabSeq();

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMGraphicalComponent#getText()
	 * @see com.servoy.base.solutionmodel.IBaseSMGraphicalComponent#getText()
	 *
	 * @sample
	 * // In general the text is specified when creating the component.
	 * var label = form.newLabel('Initial text', 10, 10, 100, 20);
	 * // But it can be changed later if needed.
	 * label.text = 'Changed text';
	 */
	public String getText();

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getToolTipText()
	 * @see com.servoy.j2db.solutionmodel.ISMField#getToolTipText()
	 *
	 * @sample
	 * var label = form.newLabel('Stop the mouse over me!', 10, 10, 200, 20);
	 * label.toolTipText = 'I\'m the tooltip. Do you see me?';
	 */
	public String getToolTipText();

	/**
	 * The vertical alignment of the text inside the component. Can be one of
	 * TOP, CENTER or BOTTOM.
	 *
	 * Note that this property does not refer to the vertical alignment of the
	 * component inside the form.
	 *
	 * @sample
	 * var topAlignedLabel = form.newLabel('TOP', 400, 10, 50, 300);
	 * topAlignedLabel.verticalAlignment = SM_ALIGNMENT.TOP;
	 * var vCenterAlignedLabel = form.newLabel('CENTER', 460, 10, 50, 300);
	 * vCenterAlignedLabel.verticalAlignment = SM_ALIGNMENT.CENTER
	 * var bottomAlignedLabel = form.newLabel('BOTTOM', 520, 10, 50, 300);
	 * bottomAlignedLabel.verticalAlignment = SM_ALIGNMENT.BOTTOM;
	 */
	public int getVerticalAlignment();

	public void setHorizontalAlignment(int arg);

	public void setImageMedia(ISMMedia media);

	public void setMargin(String margin);

	public void setMediaOptions(int i);

	public void setMnemonic(String arg);

	public void setRolloverCursor(int i);

	public void setRolloverImageMedia(ISMMedia media);

	public void setRotation(int i);

	public void setShowClick(boolean arg);

	public void setShowFocus(boolean arg);

	public void setTabSeq(int i);

	public void setToolTipText(String arg);

	public void setVerticalAlignment(int arg);

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponent#getOnActionMethodID()
	 * @see com.servoy.base.solutionmodel.IBaseSMField#getOnAction()
	 *
	 * @sample
	 * var doNothingMethod = form.newMethod('function doNothing() { application.output("Doing nothing."); }');
	 * var onClickMethod = form.newMethod('function onClick(event) { application.output("I was clicked at " + event.getTimestamp()); }');
	 * var onDoubleClickMethod = form.newMethod('function onDoubleClick(event) { application.output("I was double-clicked at " + event.getTimestamp()); }');
	 * var onRightClickMethod = form.newMethod('function onRightClick(event) { application.output("I was right-clicked at " + event.getTimestamp()); }');
	 * // At creation the button has the 'doNothing' method as onClick handler, but we'll change that later.
	 * var btn = form.newButton('I am a button', 10, 40, 200, 20, doNothingMethod);
	 * btn.onAction = onClickMethod;
	 * btn.onDoubleClick = onDoubleClickMethod;
	 * btn.onRightClick = onRightClickMethod;
	 */
	public ISMMethod getOnAction();

	public void setOnDoubleClick(ISMMethod method);

	/**
	 * The method that is executed when the component is double clicked.
	 *
	 * @sampleas getOnAction()
	 * @see #getOnAction()
	 */
	public ISMMethod getOnDoubleClick();

	public void setOnRender(ISMMethod method);

	/**
	 * The method that is executed when the component is rendered.
	 *
	 * @sample
	 * label.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	public ISMMethod getOnRender();

	public void setOnRightClick(ISMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnRightClickMethodID()
	 * @see com.servoy.j2db.solutionmodel.ISMField#getOnRightClick()
	 *
	 * @sampleas getOnAction()
	 * @see #getOnAction()
	 */
	public ISMMethod getOnRightClick();

}