/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.solutionmodel;

import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * @author lvostinar
 *
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseSMPart
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
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int BODY = IPartConstants.BODY;
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
	public static final int FOOTER = IPartConstants.FOOTER;
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
	public static final int HEADER = IPartConstants.HEADER;
	/**
	 * Constant use for specifying the type of form parts.
	 *
	 * A Leading Grand Summary can be placed before the body part. It can contain
	 * summary fields that will generate summaries for the entire foundset.
	 *
	 * @sample
	 * var leadingGrandSummary = form.newPart(JSPart.LEADING_GRAND_SUMMARY, 120);
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	public static final int LEADING_GRAND_SUMMARY = IPartConstants.LEADING_GRAND_SUMMARY;
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
	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	public static final int LEADING_SUBSUMMARY = IPartConstants.LEADING_SUBSUMMARY;
	/**
	 * Constant use for specifying the type of form parts.
	 *
	 * Appears once on the first page of a printed report. If a Footer is available, it is
	 * replaced by the Title Footer on the first page.
	 *
	 * @sample
	 * var titleFooter = form.newPart(JSPart.TITLE_FOOTER, 500);
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	public static final int TITLE_FOOTER = IPartConstants.TITLE_FOOTER;
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
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int TITLE_HEADER = IPartConstants.TITLE_HEADER;
	/**
	 * Constant use for specifying the type of form parts.
	 *
	 * A Trailing Grand Summary can be placed after the body part. It can contain
	 * summary fields that will generate summaries for the entire foundset.
	 *
	 * @sample
	 * var trailingGrandSummary = form.newPart(JSPart.TRAILING_GRAND_SUMMARY, 400);
	 */
	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	public static final int TRAILING_GRAND_SUMMARY = IPartConstants.TRAILING_GRAND_SUMMARY;
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
	@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
	public static final int TRAILING_SUBSUMMARY = IPartConstants.TRAILING_SUBSUMMARY;

	/**
	 * The Cascading Style Sheet (CSS) class name applied to the part.
	 *
	 * @sample
	 * var body = form.newPart(JSPart.BODY, 320);
	 * body.styleClass = 'myBody';
	 */
	public String getStyleClass();

	public void setStyleClass(String styleClass);
}
