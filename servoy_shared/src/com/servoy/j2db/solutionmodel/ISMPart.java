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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMPart;

/**
 * Solution model form part.
 *
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMPart extends ISMHasUUID, IBaseSMPart
{

	/**
	 * When set, the remainder of a selected part that does not fit on the page currently
	 * being printed, will not be transported to the next page - it will break where the page
	 * ends and continue on the next page.
	 *
	 * NOTE: Make sure to set this option when you are printing more than one page per record.
	 *
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.allowBreakAcrossPageBounds = true;
	 * body.discardRemainderAfterBreak = true;
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getAllowBreakAcrossPageBounds();

	/**
	 * The background color of the form part.
	 *
	 * NOTE: When no background color has been set, the default background
	 * color will be determined by the Look and Feel (LAF) that has been selected
	 * in Application Preferences.
	 *
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.background = 'green';
	 */
	public String getBackground();

	/**
	 * When set, the remainder of a selected part that is broken due to the page
	 * ending will not be printed on the next page - it will be discarded.
	 *
	 * @sampleas getAllowBreakAcrossPageBounds()
	 * @see #getAllowBreakAcrossPageBounds()
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getDiscardRemainderAfterBreak();

	/**
	 * For Leading Subsummary or Trailing Subsummary parts, one or more
	 * dataproviders can be added as Break (GroupBy) dataproviders. The
	 * Leading/Trailing Subsummary parts will be displayed once for each
	 * resulted group of data.
	 *
	 * @sample
	 * var leadingSubsummary = form.newPart(JSPart.LEADING_SUBSUMMARY, 160);
	 * leadingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 */
	public String getGroupbyDataProviderIDs();

	/**
	 * The height of a selected part; specified in pixels.
	 *
	 * This height property is the lowerbound as its ending Y value (0 == top of the form).
	 *
	 * @sample
	 * var part = form.newPart(JSPart.HEADER, 100);
	 * part.height = 200;
	 */
	public int getHeight();

	/**
	 * A page break will be inserted after a specified number of occurences of a selected part.
	 *
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.pageBreakAfterOccurrence = 2;
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getPageBreakAfterOccurrence();

	/**
	 * When set, a page break will be inserted before each occurrence of a selected part.
	 *
	 * @sample
	 * var leadingSubsummary = form.newPart(JSPart.LEADING_SUBSUMMARY, 160);
	 * leadingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 * leadingSubsummary.pageBreakBefore = true;
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getPageBreakBefore();

	/**
	 * The Y offset of the part on the form, this will include all the super forms parts if this form extends a form.
	 *
	 * @sample
	 * var allParts = form.getParts()
	 * for (var i=0; i<allParts.length; i++) {
	 *	if (allParts[i].getPartType() == JSPart.BODY)
	 *		application.output('body Y offset: ' + allParts[i].getPartYOffset());
	 * }
	 *
	 * @return A number holding the Y offset of the form part.
	 */
	public int getPartYOffset();

	/**
	 * The type of this part.
	 *
	 * @sampleas getPartYOffset()
	 * @see #getPartYOffset()
	 *
	 * @return A number representing the type of the form part.
	 */
	public int getPartType();

	/**
	 * When set, page numbering will be restarted after each occurrence of a selected part.
	 *
	 * @sample
	 * var trailingSubsummary = form.newPart(JSPart.TRAILING_SUBSUMMARY, 360);
	 * trailingSubsummary.groupbyDataProviderIDs = 'my_table_text';
	 * trailingSubsummary.restartPageNumber = true;
	 */
	public boolean getRestartPageNumber();

	/**
	 * When set, the last part on a page (such as a Trailing Grand Summary part) will
	 * "sink" to the lowest part of the page when there is free space.
	 *
	 * @sample
	 * var trailingGrandSummary = form.newPart(JSPart.TRAILING_GRAND_SUMMARY, 400);
	 * trailingGrandSummary.sinkWhenLast = true;
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getSinkWhenLast();

	public void setAllowBreakAcrossPageBounds(boolean b);

	public void setBackground(String arg);

	public void setDiscardRemainderAfterBreak(boolean b);

	public void setGroupbyDataProviderIDs(String arg);

	public void setHeight(int arg);

	public void setPageBreakAfterOccurrence(int i);

	public void setPageBreakBefore(boolean b);

	public void setRestartPageNumber(boolean b);

	public void setSinkWhenLast(boolean b);

}