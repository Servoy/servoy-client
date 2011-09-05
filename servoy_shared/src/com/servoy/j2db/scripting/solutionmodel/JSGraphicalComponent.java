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

import org.mozilla.javascript.Function;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public abstract class JSGraphicalComponent extends JSComponent<GraphicalComponent>
{

	private final IApplication application;

	/**
	 * @param gc
	 */
	public JSGraphicalComponent(IJSParent< ? > parent, GraphicalComponent gc, IApplication application, boolean isNew)
	{
		super(parent, gc, isNew);
		this.application = application;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getDataProviderID()
	 * 
	 * @sample
	 * // Normally the dataprovider is specified when a component is created.
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * // But it can be modified later if needed.
	 * field.dataProviderID = 'parent_table_id';
	 */
	public String js_getDataProviderID()
	{
		return getBaseComponent(false).getDataProviderID();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getDisplaysTags()
	 * 
	 * @sample
	 * var label = form.newLabel('You are viewing record no. %%parent_table_id%%. You are running on server %%serverURL%%.', 
	 *					10, 10, 600, 100);
	 * label.displaysTags = true;
	 */
	public boolean js_getDisplaysTags()
	{
		return getBaseComponent(false).getDisplaysTags();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getHorizontalAlignment()
	 * 
	 * @sample 
	 * var leftAlignedLabel = form.newLabel('LEFT', 10, 10, 300, 20);
	 * leftAlignedLabel.horizontalAlignment = SM_ALIGNMENT.LEFT;
	 * var hCenteredLabel = form.newLabel('CENTER', 10, 40, 300, 20);
	 * hCenteredLabel.horizontalAlignment = SM_ALIGNMENT.CENTER;
	 * var rightAlignedLabel = form.newLabel('RIGHT', 10, 70, 300, 20);
	 * rightAlignedLabel.horizontalAlignment = SM_ALIGNMENT.RIGHT;
	 */
	public int js_getHorizontalAlignment()
	{
		return getBaseComponent(false).getHorizontalAlignment();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getImageMediaID()
	 * 
	 * @sample
	 * var ballBytes = plugins.file.readFile('d:/ball.jpg');
	 * var ballImage = solutionModel.newMedia('ball.jpg', ballBytes);
	 * var label = form.newLabel('', 10, 10, 100, 100);
	 * label.imageMedia = ballImage;
	 */
	public JSMedia js_getImageMedia()
	{
		Media media = application.getFlattenedSolution().getMedia(getBaseComponent(false).getImageMediaID());
		if (media != null)
		{
			return new JSMedia(media, application.getFlattenedSolution(), false);
		}
		return null;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getLabelFor()
	 * 
	 * @sample
	 * var labelOne = form.newLabel('Label One', 10, 10, 100, 20);
	 * var fieldOne = form.newField('parent_table_id', JSField.TEXT_FIELD, 120, 10, 100, 20);
	 * fieldOne.name = 'fieldOne';
	 * labelOne.labelFor = 'fieldOne';
	 * labelOne.mnemonic = 'O';
	 */
	public String js_getLabelFor()
	{
		return getBaseComponent(false).getLabelFor();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getMargin()
	 * 
	 * @sample
	 * var label = form.newLabel('Label', 10, 10, 150, 150);
	 * label.background = 'yellow';
	 * label.margin = '10,20,30,40';
	 */
	public String js_getMargin()
	{
		return PersistHelper.createInsetsString(getBaseComponent(false).getMargin());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getMediaOptions()
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
	public int js_getMediaOptions()
	{
		return getBaseComponent(false).getMediaOptions();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getMnemonic()
	 * 
	 * @sample
	 * var m = form.newFormMethod('function onClick() { application.output("I was clicked."); }');
	 * var btn = form.newButton('I am a button', 10, 40, 200, 20, m);
	 * btn.mnemonic = 'B'; // When ALT-B is pressed the mouse will respond as if clicked.
	 * var labelOne = form.newLabel('Label One', 10, 10, 100, 20);
	 * var fieldOne = form.newField('parent_table_id', JSField.TEXT_FIELD, 120, 10, 100, 20);
	 * fieldOne.name = 'fieldOne';
	 * labelOne.labelFor = 'fieldOne';
	 * labelOne.mnemonic = 'O'; // When ALT-O is pressed the focus will move to fieldOne.
	 */
	public String js_getMnemonic()
	{
		return getBaseComponent(false).getMnemonic();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getRolloverCursor()
	 *
	 * @sample 
	 * var label = form.newLabel('Move the mouse over me', 10, 10, 200, 200);
	 * label.rolloverCursor = SM_CURSOR.HAND_CURSOR;
	 */
	public int js_getRolloverCursor()
	{
		return getBaseComponent(false).getRolloverCursor();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getRolloverImageMediaID()
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
	public JSMedia js_getRolloverImageMedia()
	{
		Media media = application.getFlattenedSolution().getMedia(getBaseComponent(false).getRolloverImageMediaID());
		if (media != null)
		{
			return new JSMedia(media, application.getFlattenedSolution(), false);
		}
		return null;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getRotation()
	 * 
	 * @sample
	 * var m = form.newFormMethod('function onClick() { application.output("I was clicked."); }');
	 * var label = form.newLabel('I am a label', 10, 10, 200, 200, m);
	 * label.rotation = 90;
	 * var btn = form.newButton('And I am a button', 10, 220, 200, 20, m);
	 * btn.rotation = 180;
	 */
	public int js_getRotation()
	{
		return getBaseComponent(false).getRotation();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getShowClick()
	 * 
	 * @sample
	 * // Create a form method.
	 * var m = form.newFormMethod('function onClick() { application.output("I was clicked."); }');
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
	public boolean js_getShowClick()
	{
		return getBaseComponent(false).getShowClick();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getShowFocus()
	 * 
	 * @sample
	 * var m = form.newFormMethod('function onClick() { application.output("I was clicked."); }');
	 * var label = form.newLabel('I am a label', 10, 10, 200, 20, m);
	 * label.showFocus = false;
	 * var btn = form.newButton('And I am a button', 10, 40, 200, 20, m);
	 * btn.showFocus = false;
	 */
	public boolean js_getShowFocus()
	{
		return getBaseComponent(false).getShowFocus();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getTabSeq()
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
	public int js_getTabSeq()
	{
		return getBaseComponent(false).getTabSeq();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getText()
	 * 
	 * @sample
	 * // In general the text is specified when creating the component.
	 * var label = form.newLabel('Initial text', 10, 10, 100, 20);
	 * // But it can be changed later if needed.
	 * label.text = 'Changed text';
	 */
	public String js_getText()
	{
		return getBaseComponent(false).getText();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getToolTipText()
	 * 
	 * @sample
	 * var label = form.newLabel('Stop the mouse over me!', 10, 10, 200, 20);
	 * label.toolTipText = 'I\'m the tooltip. Do you see me?';
	 */
	public String js_getToolTipText()
	{
		return getBaseComponent(false).getToolTipText();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getVerticalAlignment()
	 *
	 * @sample 
	 * var topAlignedLabel = form.newLabel('TOP', 400, 10, 50, 300);
	 * topAlignedLabel.verticalAlignment = SM_ALIGNMENT.TOP;
	 * var vCenterAlignedLabel = form.newLabel('CENTER', 460, 10, 50, 300);
	 * vCenterAlignedLabel.verticalAlignment = SM_ALIGNMENT.CENTER
	 * var bottomAlignedLabel = form.newLabel('BOTTOM', 520, 10, 50, 300);
	 * bottomAlignedLabel.verticalAlignment = SM_ALIGNMENT.BOTTOM;
	 */
	public int js_getVerticalAlignment()
	{
		return getBaseComponent(false).getVerticalAlignment();
	}

	public void js_setDataProviderID(String arg)
	{
		getBaseComponent(true).setDataProviderID(arg);
	}

	public void js_setFormat(String arg)
	{
		getBaseComponent(true).setFormat(arg);
	}

	public void js_setDisplaysTags(boolean arg)
	{
		getBaseComponent(true).setDisplaysTags(arg);
	}


	public void js_setHorizontalAlignment(int arg)
	{
		getBaseComponent(true).setHorizontalAlignment(arg);
	}

	public void js_setImageMedia(JSMedia media)
	{
		if (media == null)
		{
			getBaseComponent(true).setImageMediaID(0);
		}
		else
		{
			getBaseComponent(true).setImageMediaID(media.getMedia().getID());
		}
	}

	public void js_setLabelFor(String arg)
	{
		getBaseComponent(true).setLabelFor(arg);
	}

	public void js_setMargin(String margin)
	{
		getBaseComponent(true).setMargin(PersistHelper.createInsets(margin));
	}

	public void js_setMediaOptions(int i)
	{
		getBaseComponent(true).setMediaOptions(i);
	}

	public void js_setMnemonic(String arg)
	{
		getBaseComponent(true).setMnemonic(arg);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getOnAction() 
	 */
	@Deprecated
	public void js_setOnActionMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnActionMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnActionMethodID(0);
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getOnDoubleClick() 
	 */
	@Deprecated
	public void js_setOnDoubleClickMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnDoubleClickMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnDoubleClickMethodID(0);
		}

	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getOnRightClick() 
	 */
	@Deprecated
	public void js_setOnRightClickMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnRightClickMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnRightClickMethodID(0);
		}
	}

	public void js_setRolloverCursor(int i)
	{
		getBaseComponent(true).setRolloverCursor(i);
	}

	public void js_setRolloverImageMedia(JSMedia media)
	{
		if (media == null)
		{
			getBaseComponent(true).setRolloverImageMediaID(0);
		}
		else
		{
			getBaseComponent(true).setRolloverImageMediaID(media.getMedia().getID());
		}
	}

	public void js_setRotation(int i)
	{
		getBaseComponent(true).setRotation(i);
	}

	public void js_setShowClick(boolean arg)
	{
		getBaseComponent(true).setShowClick(arg);
	}

	public void js_setShowFocus(boolean arg)
	{
		getBaseComponent(true).setShowFocus(arg);
	}

	public void js_setTabSeq(int i)
	{
		getBaseComponent(true).setTabSeq(i);
	}

	public void js_setText(String arg)
	{
		getBaseComponent(true).setText(arg);
	}

	public void js_setToolTipText(String arg)
	{
		getBaseComponent(true).setToolTipText(arg);
	}

	public void js_setVerticalAlignment(int arg)
	{
		getBaseComponent(true).setVerticalAlignment(arg);
	}

	public void js_setOnAction(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnActionMethodID()
	 * 
	 * @sample
	 * var doNothingMethod = form.newFormMethod('function doNothing() { application.output("Doing nothing."); }');
	 * var onClickMethod = form.newFormMethod('function onClick(event) { application.output("I was clicked at " + event.getTimestamp()); }');
	 * var onDoubleClickMethod = form.newFormMethod('function onDoubleClick(event) { application.output("I was double-clicked at " + event.getTimestamp()); }');
	 * var onRightClickMethod = form.newFormMethod('function onRightClick(event) { application.output("I was right-clicked at " + event.getTimestamp()); }');
	 * // At creation the button has the 'doNothing' method as onClick handler, but we'll change that later.
	 * var btn = form.newButton('I am a button', 10, 40, 200, 20, doNothingMethod);
	 * btn.onAction = onClickMethod;
	 * btn.onDoubleClick = onDoubleClickMethod;
	 * btn.onRightClick = onRightClickMethod;
	 */
	public JSMethod js_getOnAction()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID);
	}

	public void js_setOnDoubleClick(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnDoubleClickMethodID()
	 * 
	 * @sampleas js_getOnAction()
	 */
	public JSMethod js_getOnDoubleClick()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID);
	}

	public void js_setOnRender(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnRenderMethodID()
	 * 
	 * @sample
	 * label.onRender = form.newFormMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	public JSMethod js_getOnRender()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID);
	}

	public void js_setOnRightClick(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnRightClickMethodID()
	 * 
	 * @sampleas js_getOnAction()
	 */
	public JSMethod js_getOnRightClick()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getFormat()
	 * 
	 * @sample
	 * var label = form.newLabel('', 10, 10, 100, 100);
	 * label.format = '$#.00';
	 */
	public String js_getFormat()
	{
		return getBaseComponent(false).getFormat();
	}

}