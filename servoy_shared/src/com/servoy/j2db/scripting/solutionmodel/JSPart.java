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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.solutionmodel.ISMPart;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSPart extends JSBase<Part> implements IConstantsObject, ISMPart
{
	public JSPart(JSForm form, Part part, boolean isNew)
	{
		super(form, part, isNew);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getAllowBreakAcrossPageBounds()
	 * 
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.allowBreakAcrossPageBounds = true;
	 * body.discardRemainderAfterBreak = true;
	 */
	@JSGetter
	public boolean getAllowBreakAcrossPageBounds()
	{
		return getBaseComponent(false).getAllowBreakAcrossPageBounds();
	}

	@JSSetter
	public void setAllowBreakAcrossPageBounds(boolean b)
	{
		getBaseComponent(true).setAllowBreakAcrossPageBounds(b);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getBackground()
	 * 
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.background = 'green';
	 */
	@JSGetter
	public String getBackground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getBackground());
	}

	@JSSetter
	public void setBackground(String arg)
	{
		getBaseComponent(true).setBackground(PersistHelper.createColor(arg));
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getDiscardRemainderAfterBreak()
	 * 
	 * @sampleas getAllowBreakAcrossPageBounds()
	 */
	@JSGetter
	public boolean getDiscardRemainderAfterBreak()
	{
		return getBaseComponent(false).getDiscardRemainderAfterBreak();
	}

	@JSSetter
	public void setDiscardRemainderAfterBreak(boolean b)
	{
		getBaseComponent(true).setDiscardRemainderAfterBreak(b);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getGroupbyDataProviderIDs()
	 * 
	 * @sample
	 * var leadingSubsummary = form.newPart(JSPart.LEADING_SUBSUMMARY, 160);
	 * leadingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 */
	@JSGetter
	public String getGroupbyDataProviderIDs()
	{
		return getBaseComponent(false).getGroupbyDataProviderIDs();
	}

	@JSSetter
	public void setGroupbyDataProviderIDs(String arg)
	{
		getBaseComponent(true).setGroupbyDataProviderIDs(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getHeight()
	 *
	 * @sample 
	 * var part = form.newPart(JSPart.HEADER, 100);
	 * part.height = 200;
	 */
	@JSGetter
	public int getHeight()
	{
		return getBaseComponent(false).getHeight();
	}

	@JSSetter
	public void setHeight(int arg)
	{
		getBaseComponent(true).setHeight(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getPageBreakAfterOccurrence()
	 * 
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.pageBreakAfterOccurrence = 2;
	 */
	@JSGetter
	public int getPageBreakAfterOccurrence()
	{
		return getBaseComponent(false).getPageBreakAfterOccurrence();
	}

	@JSSetter
	public void setPageBreakAfterOccurrence(int i)
	{
		getBaseComponent(true).setPageBreakAfterOccurrence(i);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getPageBreakBefore()
	 * 
	 * @sample
	 * var leadingSubsummary = form.newPart(JSPart.LEADING_SUBSUMMARY, 160);
	 * leadingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 * leadingSubsummary.pageBreakBefore = true;
	 */
	@JSGetter
	public boolean getPageBreakBefore()
	{
		return getBaseComponent(false).getPageBreakBefore();
	}

	@JSSetter
	public void setPageBreakBefore(boolean b)
	{
		getBaseComponent(true).setPageBreakBefore(b);
	}

	/**
	 * The Y offset of the part on the form, this will include all the super forms parts if this form extends a form.
	 *
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSForm#getParts()
	 * 
	 * @return A number holding the Y offset of the form part.
	 */
	@JSFunction
	public int getPartYOffset()
	{
		Part part = getBaseComponent(false);
		return ((JSForm)getJSParent()).getPartYOffset(part.getPartType(), part.getHeight());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getPartType()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSForm#getParts()
	 * 
	 * @return A number representing the type of the form part.
	 */
	@JSFunction
	public int getPartType()
	{
		return getBaseComponent(false).getPartType();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getRestartPageNumber()
	 * 
	 * @sample
	 * var trailingSubsummary = form.newPart(JSPart.TRAILING_SUBSUMMARY, 360);
	 * trailingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 * trailingSubsummary.restartPageNumber = true;
	 */
	@JSGetter
	public boolean getRestartPageNumber()
	{
		return getBaseComponent(false).getRestartPageNumber();
	}

	@JSSetter
	public void setRestartPageNumber(boolean b)
	{
		getBaseComponent(true).setRestartPageNumber(b);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getSinkWhenLast()
	 * 
	 * @sample
	 * var trailingGrandSummary = form.newPart(JSPart.TRAILING_GRAND_SUMMARY, 400);
	 * trailingGrandSummary.sinkWhenLast = true;
	 */
	@JSGetter
	public boolean getSinkWhenLast()
	{
		return getBaseComponent(false).getSinkWhenLast();
	}

	@JSSetter
	public void setSinkWhenLast(boolean b)
	{
		getBaseComponent(true).setSinkWhenLast(b);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getStyleClass()
	 * 
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.styleClass = 'myBody';
	 */
	@JSGetter
	public String getStyleClass()
	{
		return getBaseComponent(false).getStyleClass();
	}

	@JSSetter
	public void setStyleClass(String styleClass)
	{
		getBaseComponent(true).setStyleClass(styleClass);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSPart[name:" + Part.getDisplayName(getBaseComponent(false).getPartType()) + ",form:" +
			((ISupportName)getBaseComponent(false).getParent()).getName() + ']';
	}
}
