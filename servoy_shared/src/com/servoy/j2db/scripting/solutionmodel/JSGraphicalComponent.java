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
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.solutionmodel.ISMGraphicalComponent;
import com.servoy.j2db.solutionmodel.ISMMedia;
import com.servoy.j2db.solutionmodel.ISMMethod;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
public abstract class JSGraphicalComponent extends JSComponent<GraphicalComponent> implements ISMGraphicalComponent
{
	private final IApplication application;

	public JSGraphicalComponent(IJSParent< ? > parent, GraphicalComponent gc, IApplication application, boolean isNew)
	{
		super(parent, gc, isNew);
		this.application = application;
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getDataProviderID()
	 *
	 * @sample
	 * // Normally the dataprovider is specified when a component is created.
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * // But it can be modified later if needed.
	 * field.dataProviderID = 'parent_table_id';
	 */
	@JSGetter
	public String getDataProviderID()
	{
		return getBaseComponent(false).getDataProviderID();
	}

	@JSSetter
	public void setDataProviderID(String arg)
	{
		getBaseComponent(true).setDataProviderID(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getText()
	 *
	 * @sample
	 * // In general the text is specified when creating the component.
	 * var label = form.newLabel('Initial text', 10, 10, 100, 20);
	 * // But it can be changed later if needed.
	 * label.text = 'Changed text';
	 */
	@JSGetter
	public String getText()
	{
		return getBaseComponent(false).getText();
	}

	@JSSetter
	public void setText(String arg)
	{
		getBaseComponent(true).setText(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getDisplaysTags()
	 *
	 * @sample
	 * var label = form.newLabel('You are viewing record no. %%parent_table_id%%. You are running on server %%serverURL%%.',
	 *					10, 10, 600, 100);
	 * label.displaysTags = true;
	 */
	@JSGetter
	public boolean getDisplaysTags()
	{
		return getBaseComponent(false).getDisplaysTags();
	}

	@JSSetter
	public void setDisplaysTags(boolean arg)
	{
		getBaseComponent(true).setDisplaysTags(arg);
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
	@Override
	@JSGetter
	public int getHorizontalAlignment()
	{
		return getBaseComponent(false).getHorizontalAlignment();
	}

	@Override
	@JSSetter
	public void setHorizontalAlignment(int arg)
	{
		getBaseComponent(true).setHorizontalAlignment(arg);
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
	@Override
	@JSGetter
	public JSMedia getImageMedia()
	{
		Media media = application.getFlattenedSolution().getMedia(getBaseComponent(false).getImageMediaID());
		if (media != null)
		{
			return new JSMedia(media, application.getFlattenedSolution(), false);
		}
		return null;
	}

	@JSSetter
	public void setImageMedia(JSMedia media)
	{
		doSetImageMedia(media);
	}

	@Override
	public void setImageMedia(ISMMedia media)
	{
		doSetImageMedia((JSMedia)media);
	}

	protected void doSetImageMedia(JSMedia media)
	{
		if (media == null)
		{
			getBaseComponent(true).setImageMediaID(null);
		}
		else
		{
			getBaseComponent(true).setImageMediaID(media.getMedia().getUUID().toString());
		}
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
	@JSGetter
	public String getLabelFor()
	{
		return getBaseComponent(false).getLabelFor();
	}

	@JSSetter
	public void setLabelFor(String arg)
	{
		getBaseComponent(true).setLabelFor(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getMargin()
	 *
	 * @sample
	 * var label = form.newLabel('Label', 10, 10, 150, 150);
	 * label.background = 'yellow';
	 * label.margin = '10,20,30,40';
	 */
	@Override
	@JSGetter
	public String getMargin()
	{
		return PersistHelper.createInsetsString(getBaseComponent(false).getMargin());
	}

	@Override
	@JSSetter
	public void setMargin(String margin)
	{
		getBaseComponent(true).setMargin(PersistHelper.createInsets(margin));
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
	@Override
	@JSGetter
	public int getMediaOptions()
	{
		return getBaseComponent(false).getMediaOptions();
	}

	@Override
	@JSSetter
	public void setMediaOptions(int i)
	{
		getBaseComponent(true).setMediaOptions(i);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getMnemonic()
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
	@Override
	@JSGetter
	public String getMnemonic()
	{
		return getBaseComponent(false).getMnemonic();
	}

	@Override
	@JSSetter
	public void setMnemonic(String arg)
	{
		getBaseComponent(true).setMnemonic(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getRolloverCursor()
	 *
	 * @sample
	 * var label = form.newLabel('Move the mouse over me', 10, 10, 200, 200);
	 * label.rolloverCursor = SM_CURSOR.HAND_CURSOR;
	 */
	@Override
	@JSGetter
	public int getRolloverCursor()
	{
		return getBaseComponent(false).getRolloverCursor();
	}

	@Override
	@JSSetter
	public void setRolloverCursor(int i)
	{
		getBaseComponent(true).setRolloverCursor(i);
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
	@Override
	@JSGetter
	public JSMedia getRolloverImageMedia()
	{
		Media media = application.getFlattenedSolution().getMedia(getBaseComponent(false).getRolloverImageMediaID());
		if (media != null)
		{
			return new JSMedia(media, application.getFlattenedSolution(), false);
		}
		return null;
	}

	@Override
	@JSSetter
	public void setRolloverImageMedia(ISMMedia media)
	{
		if (media == null)
		{
			getBaseComponent(true).setRolloverImageMediaID(null);
		}
		else
		{
			getBaseComponent(true).setRolloverImageMediaID(((JSMedia)media).getMedia().getUUID().toString());
		}
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getRotation()
	 *
	 * @sample
	 * var m = form.newMethod('function onClick() { application.output("I was clicked."); }');
	 * var label = form.newLabel('I am a label', 10, 10, 200, 200, m);
	 * label.rotation = 90;
	 * var btn = form.newButton('And I am a button', 10, 220, 200, 20, m);
	 * btn.rotation = 180;
	 */
	@Override
	@JSGetter
	public int getRotation()
	{
		return getBaseComponent(false).getRotation();
	}

	@Override
	@JSSetter
	public void setRotation(int i)
	{
		getBaseComponent(true).setRotation(i);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getShowClick()
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
	@Override
	@JSGetter
	public boolean getShowClick()
	{
		return getBaseComponent(false).getShowClick();
	}

	@Override
	@JSSetter
	public void setShowClick(boolean arg)
	{
		getBaseComponent(true).setShowClick(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getShowFocus()
	 *
	 * @sample
	 * var m = form.newMethod('function onClick() { application.output("I was clicked."); }');
	 * var label = form.newLabel('I am a label', 10, 10, 200, 20, m);
	 * label.showFocus = false;
	 * var btn = form.newButton('And I am a button', 10, 40, 200, 20, m);
	 * btn.showFocus = false;
	 */
	@Override
	@JSGetter
	public boolean getShowFocus()
	{
		return getBaseComponent(false).getShowFocus();
	}

	@Override
	@JSSetter
	public void setShowFocus(boolean arg)
	{
		getBaseComponent(true).setShowFocus(arg);
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
	@Override
	@JSGetter
	public int getTabSeq()
	{
		return getBaseComponent(false).getTabSeq();
	}

	@Override
	@JSSetter
	public void setTabSeq(int i)
	{
		getBaseComponent(true).setTabSeq(i);
	}

	/**
	 * @sameas com.servoy.j2db.solutionmodel.ISMField#getToolTipText()
	 */
	@Override
	@JSGetter
	public String getToolTipText()
	{
		return getBaseComponent(false).getToolTipText();
	}

	@Override
	@JSSetter
	public void setToolTipText(String arg)
	{
		getBaseComponent(true).setToolTipText(arg);
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
	@Override
	@JSGetter
	public int getVerticalAlignment()
	{
		return getBaseComponent(false).getVerticalAlignment();
	}

	@Override
	@JSSetter
	public void setVerticalAlignment(int arg)
	{
		getBaseComponent(true).setVerticalAlignment(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getFormat()
	 *
	 * @sample
	 * var label = form.newLabel('', 10, 10, 100, 100);
	 * label.format = '$#.00';
	 */
	@Override
	@JSGetter
	public String getFormat()
	{
		return getBaseComponent(false).getFormat();
	}

	@Override
	@JSSetter
	public void setFormat(String arg)
	{
		getBaseComponent(true).setFormat(arg);
	}

	/**
	 * @deprecated As of release 4.1, replaced by setOnAction(JSMethod).
	 */
	@Deprecated
	public void js_setOnActionMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnActionMethodID(scriptMethod.getUUID().toString());
		}
		else
		{
			getBaseComponent(true).setOnActionMethodID(null);
		}
	}

	/**
	 * @deprecated As of release 4.1, replaced by setOnDoubleClick(JSMethod).
	 */
	@Deprecated
	public void js_setOnDoubleClickMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnDoubleClickMethodID(scriptMethod.getUUID().toString());
		}
		else
		{
			getBaseComponent(true).setOnDoubleClickMethodID(null);
		}
	}

	/**
	 * @deprecated As of release 4.1, replaced by setOnRightClick(JSMethod).
	 */
	@Deprecated
	public void js_setOnRightClickMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnRightClickMethodID(scriptMethod.getUUID().toString());
		}
		else
		{
			getBaseComponent(true).setOnRightClickMethodID(null);
		}
	}

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMField#getOnAction()
	 */
	@Override
	@JSGetter
	public JSMethod getOnAction()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID);
	}

	@Override
	@JSSetter
	public void setOnAction(IBaseSMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnDoubleClickMethodID()
	 *
	 * @sampleas getOnAction()
	 */
	@Override
	@JSGetter
	public JSMethod getOnDoubleClick()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID);
	}

	@Override
	@JSSetter
	public void setOnDoubleClick(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnRenderMethodID()
	 *
	 * @sample
	 * label.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	@Override
	@JSGetter
	public JSMethod getOnRender()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID);
	}

	@Override
	@JSSetter
	public void setOnRender(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.GraphicalComponent#getOnRightClickMethodID()
	 *
	 * @sampleas getOnAction()
	 */
	@Override
	@JSGetter
	public JSMethod getOnRightClick()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID);
	}

	@Override
	@JSSetter
	public void setOnRightClick(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID, (JSMethod)method);
	}
}