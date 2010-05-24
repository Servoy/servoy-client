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

import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.ServoyDocumented;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSPart extends JSBase<Part> implements IConstantsObject
{
	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * This is the default part that is repeated for each record (being 
	 * displayed and/or printed).
	 *
	 * @sample 
	 * var body = form.newPart(JSPart.BODY, 320);
	 */
	public static final int BODY = Part.BODY;

	/**
	 * Constant use for specifying the type of form parts.
	 * 
	 * A footer is displayed at the bottom of each page when printed ad can
	 * contain summaries of the current selection of records. In List view, the 
	 * footer is displayed at the bottom of the list of records.
	 * 
	 * @sample
	 * var footer = form.newPart(JSPart.FOOTER, 440); 
	 */
	public static final int FOOTER = Part.FOOTER;

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * A header is displayed at the top of each page when printed and can contain
	 * summaries of the current selection of records. In List view the header is
	 * displayed above the list of records.
	 * 
	 * @sample
	 * var header = form.newPart(JSPart.HEADER, 80);
	 */
	public static final int HEADER = Part.HEADER;

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * A Leading Grand Summary can be placed before the body part. It can contain
	 * summary fields that will generate summaries for the entire foundset.
	 * 
	 * @sample
	 * var leadingGrandSummary = form.newPart(JSPart.LEADING_GRAND_SUMMARY, 120);
	 */
	public static final int LEADING_GRAND_SUMMARY = Part.LEADING_GRAND_SUMMARY;

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * A Leading Subsummary can be placed before the body part. There can be multiple Leading Subsummaries
	 * per form. Each Subsummary part has a set of Group By fields which are used to group data together.
	 * Each Subsummary part can contain summary fields, which will be printed once for each group of data.
	 *
	 * @sample
	 * var leadingSubsummary = form.newPart(JSPart.LEADING_SUBSUMMARY, 160);
	 */
	public static final int LEADING_SUBSUMMARY = Part.LEADING_SUBSUMMARY;

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * Appears once on the first page of a printed report. If a Footer is available, it is
	 * replaced by the Title Footer on the first page.
	 *
	 * @sample 
	 * var titleFooter = form.newPart(JSPart.TITLE_FOOTER, 500);
	 */
	public static final int TITLE_FOOTER = Part.TITLE_FOOTER;

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * Appears only once on the first page of a printed report or on top of the first screen
	 * of a foundset. If a Header is available it is replace by the Title Header on the first
	 * page.
	 *
	 * @sample
	 * var titleHeader = form.newPart(JSPart.TITLE_HEADER, 40);
	 */
	public static final int TITLE_HEADER = Part.TITLE_HEADER;

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * A Trailing Grand Summary can be placed after the body part. It can contain
	 * summary fields that will generate summaries for the entire foundset.
	 *
	 * @sample
	 * var trailingGrandSummary = form.newPart(JSPart.TRAILING_GRAND_SUMMARY, 400);
	 */
	public static final int TRAILING_GRAND_SUMMARY = Part.TRAILING_GRAND_SUMMARY;

	/**
	 * Constant use for specifying the type of form parts. 
	 * 
	 * A Trailing Subsummary can be placed before the body part. There can be multiple Trailing Subsummaries
	 * per form. Each Subsummary part has a set of Group By fields which are used to group data together.
	 * Each Subsummary part can contain summary fields, which will be printed once for each group of data.
	 *
	 * @sample
	 * var trailingSubsummary = form.newPart(JSPart.TRAILING_SUBSUMMARY, 360);
	 */
	public static final int TRAILING_SUBSUMMARY = Part.TRAILING_SUBSUMMARY;

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
	public boolean js_getAllowBreakAcrossPageBounds()
	{
		return getBaseComponent(false).getAllowBreakAcrossPageBounds();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getBackground()
	 * 
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.background = 'green';
	 */
	public String js_getBackground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getBackground());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getDiscardRemainderAfterBreak()
	 * 
	 * @sampleas js_getAllowBreakAcrossPageBounds()
	 */
	public boolean js_getDiscardRemainderAfterBreak()
	{
		return getBaseComponent(false).getDiscardRemainderAfterBreak();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getGroupbyDataProviderIDs()
	 * 
	 * @sample
	 * var leadingSubsummary = form.newPart(JSPart.LEADING_SUBSUMMARY, 160);
	 * leadingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 */
	public String js_getGroupbyDataProviderIDs()
	{
		return getBaseComponent(false).getGroupbyDataProviderIDs();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getHeight()
	 *
	 * @sample 
	 * var part = form.newPart(JSPart.HEADER, 100);
	 * part.height = 200;
	 */
	public int js_getHeight()
	{
		return getBaseComponent(false).getHeight();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getPageBreakAfterOccurrence()
	 * 
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.pageBreakAfterOccurrence = 2;
	 */
	public int js_getPageBreakAfterOccurrence()
	{
		return getBaseComponent(false).getPageBreakAfterOccurrence();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getPageBreakBefore()
	 * 
	 * @sample
	 * var leadingSubsummary = form.newPart(JSPart.LEADING_SUBSUMMARY, 160);
	 * leadingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 * leadingSubsummary.pageBreakBefore = true;
	 */
	public boolean js_getPageBreakBefore()
	{
		return getBaseComponent(false).getPageBreakBefore();
	}

	/**
	 * The Y offset of the part on the form, this will include all the super forms parts if this form extends a form.
	 *
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSForm#js_getParts()
	 * 
	 * @return A number holding the Y offset of the form part.
	 */
	public int js_getPartYOffset()
	{
		Part part = getBaseComponent(false);
		return ((JSForm)getJSParent()).js_getPartYOffset(part.getPartType(), part.getHeight());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getPartType()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSForm#js_getParts()
	 * 
	 * @return A number representing the type of the form part.
	 */
	public int js_getPartType()
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
	public boolean js_getRestartPageNumber()
	{
		return getBaseComponent(false).getRestartPageNumber();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Part#getSinkWhenLast()
	 * 
	 * @sample
	 * var trailingGrandSummary = form.newPart(JSPart.TRAILING_GRAND_SUMMARY, 400);
	 * trailingGrandSummary.sinkWhenLast = true;
	 */
	public boolean js_getSinkWhenLast()
	{
		return getBaseComponent(false).getSinkWhenLast();
	}

	public void js_setAllowBreakAcrossPageBounds(boolean b)
	{
		getBaseComponent(true).setAllowBreakAcrossPageBounds(b);
	}

	public void js_setBackground(String arg)
	{
		getBaseComponent(true).setBackground(PersistHelper.createColor(arg));
	}

	public void js_setDiscardRemainderAfterBreak(boolean b)
	{
		getBaseComponent(true).setDiscardRemainderAfterBreak(b);
	}

	public void js_setGroupbyDataProviderIDs(String arg)
	{
		getBaseComponent(true).setGroupbyDataProviderIDs(arg);
	}

	public void js_setHeight(int arg)
	{
		getBaseComponent(true).setHeight(arg);
	}

	public void js_setPageBreakAfterOccurrence(int i)
	{
		getBaseComponent(true).setPageBreakAfterOccurrence(i);
	}

	public void js_setPageBreakBefore(boolean b)
	{
		getBaseComponent(true).setPageBreakBefore(b);
	}

//	public void js_setPartType(int arg)
//	{
//		part.setPartType(arg);
//	}

	public void js_setRestartPageNumber(boolean b)
	{
		getBaseComponent(true).setRestartPageNumber(b);
	}

	public void js_setSinkWhenLast(boolean b)
	{
		getBaseComponent(true).setSinkWhenLast(b);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Part: " + Part.getDisplayName(getBaseComponent(false).getPartType());
	}
}
