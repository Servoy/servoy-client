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
package com.servoy.j2db.server.headlessclient.dataui;

import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualCSS;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;

/**
 * Generic interface for a layout provider used for forms in web client. A layout
 * provider provides HTML for begin/end of HTML page, begin/end of form part,
 * begin/end of table view, plus some additional information like CSS styles or 
 * the background color of the form.
 * 
 * @author gerzse
 */
public interface IFormLayoutProvider
{
	void renderOpenFormHTML(StringBuffer html, TextualCSS css);

	void renderCloseFormHTML(StringBuffer html);

	void renderOpenPartHTML(StringBuffer html, TextualCSS css, Part part);

	void renderClosePartHTML(StringBuffer html, Part part);

	void renderOpenTableViewHTML(StringBuffer html, TextualCSS css, Part part);

	void renderCloseTableViewHTML(StringBuffer html);

	int getViewType();

	boolean needsHeaders();

	/**
	 * This method should provide CSS style for the "webform" level container. In general it will layout the form
	 * to occupy the entire page. However when a custom navigator is present, the form and the navigator need to be
	 * laid out one next to the other. If the form is inside a tabpanel, then again the form should occupy all available
	 * space.
	 * 
	 * @param customNavigatorWidth The width of the custom navigator, if any. If a default navigator is used, then this value will be 0. In tabpanels the custom navigators are dropped, so again this value will be 0.
	 * @param isNavigator If this form is a custom navigator, then this value will be true, otherwise it will be false.
	 * @param isInTabPanel If the form is located inside a tabpanel, then this value will be true, otherwise false.
	 * 
	 * @return A TextualStyle instance that holds the CSS that should be applied to the "webform" level <div>.
	 */
	TextualStyle getLayoutForForm(int customNavigatorWidth, boolean isNavigator, boolean isInTabPanel);

}
