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

import java.awt.Color;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.border.Border;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualCSS;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.FixedStyleSheet;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.OrientationApplier;

public abstract class AbstractFormLayoutProvider implements IFormLayoutProvider
{
	private final Solution solution;
	protected final Form f;

	private boolean addHeaders;
	protected int defaultNavigatorShift;
	private Border border;
	private Color bgColor;
	protected String orientation;
	int viewType;

	public AbstractFormLayoutProvider(IServiceProvider sp, Solution solution, Form f)
	{
		this.solution = solution;
		this.f = f;

		addHeaders = true;
		defaultNavigatorShift = 0;
		viewType = f.getView();
		if (viewType == IForm.LIST_VIEW || viewType == FormController.LOCKED_LIST_VIEW)
		{
			viewType = FormController.LOCKED_TABLE_VIEW;
			addHeaders = false;//list views do not have headers
		}
		if (sp != null && sp.getFlattenedSolution().isInDesign(f) && viewType == FormController.LOCKED_TABLE_VIEW)
		{
			viewType = IForm.RECORD_VIEW;
		}
		if ((viewType == IForm.RECORD_VIEW || viewType == IForm.LOCKED_RECORD_VIEW) && f.getNavigatorID() == Form.NAVIGATOR_DEFAULT)
		{
			defaultNavigatorShift = WebDefaultRecordNavigator.DEFAULT_WIDTH;
		}

		// Initially get the border from the form and the background color from the body part.
		border = ComponentFactoryHelper.createBorder(f.getBorderType());
		Iterator<Part> parts = f.getParts();
		while (parts.hasNext())
		{
			Part p = parts.next();
			if (p.getPartType() == Part.HEADER)
			{
				addHeaders = false;
			}
			else if (p.getPartType() == Part.BODY)
			{
				bgColor = p.getBackground();
			}
		}

		// If at least one of border/background is null, look into styles.
		if (border == null || bgColor == null)
		{
			FixedStyleSheet ss = ComponentFactory.getCSSStyleForForm(sp, f);
			if (ss != null)
			{
				String lookupname = "form"; //$NON-NLS-1$
				if (f.getStyleClass() != null && !"".equals(f.getStyleClass())) //$NON-NLS-1$
				{
					lookupname += "." + f.getStyleClass(); //$NON-NLS-1$
				}
				javax.swing.text.Style style = ss.getStyle(lookupname);
				if (style != null)
				{
					if (border == null)
					{
						border = ss.getBorder(style);
					}
					if (bgColor == null)
					{
						bgColor = ss.getBackground(style);
					}
				}
			}
		}

		// If still no background color, default to white.
		if (bgColor == null) bgColor = Color.WHITE;

		orientation = OrientationApplier.getHTMLContainerOrientation(sp != null ? sp.getLocale() : Locale.getDefault(), solution.getTextOrientation());
	}

	public int getViewType()
	{
		return viewType;
	}

	public boolean needsHeaders()
	{
		return addHeaders;
	}

	public Color getBackgroundColor()
	{
		return bgColor;
	}

	public void renderOpenFormHTML(StringBuffer html, TextualCSS css)
	{
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"); //$NON-NLS-1$ 
		html.append("<!-- Servoy webclient page Copyright "); //$NON-NLS-1$ 
		html.append(Utils.formatTime(System.currentTimeMillis(), "yyyy")); //$NON-NLS-1$ 
		html.append(" Servoy -->\n"); //$NON-NLS-1$ 
		html.append("<html xmlns:servoy>\n"); //$NON-NLS-1$ 
		html.append("<head>\n"); //$NON-NLS-1$ 
		html.append("<title>"); //$NON-NLS-1$ 
		html.append((f.getTitleText() != null ? TemplateGenerator.getSafeText(f.getTitleText()) : f.getName()));
		html.append(" - Servoy"); //$NON-NLS-1$ 
		html.append("</title>\n"); //$NON-NLS-1$ 
		html.append("<servoy:head>\n"); //$NON-NLS-1$ 
		html.append("\t<link rel='stylesheet' type='text/css' href='/servoy-webclient/formcss/"); //$NON-NLS-1$ 
		html.append(solution.getName());
		html.append('/');
		html.append(f.getName());
		html.append("_t"); //$NON-NLS-1$ 
		html.append(System.currentTimeMillis());
		html.append("t.css'/>\n"); //$NON-NLS-1$ 
		html.append("</servoy:head>\n"); //$NON-NLS-1$ 
		html.append("</head>\n"); //$NON-NLS-1$ 
		html.append("<body id='servoy_page'>\n"); //$NON-NLS-1$ 
		html.append("<form id='servoy_dataform'>\n"); //$NON-NLS-1$ 
		html.append("<servoy:panel>\n"); //$NON-NLS-1$ 
		html.append("<div servoy:id='servoywebform' id='"); //$NON-NLS-1$ 
		html.append(buildFormID());
		html.append("'>\n"); //$NON-NLS-1$ 		

		// Put CSS properties for background color and border (if any).
		TextualStyle formStyle = css.addStyle("#" + buildFormID()); //$NON-NLS-1$ 
		formStyle.setProperty("background-color", PersistHelper.createColorString(bgColor)); //$NON-NLS-1$
		if (border != null)
		{
			String type = ComponentFactoryHelper.createBorderString(border);
			ComponentFactoryHelper.createBorderCSSProperties(type, formStyle);
		}

		fillFormLayoutCSS(formStyle);
	}

	public void renderCloseFormHTML(StringBuffer html)
	{
		html.append("</div>\n"); //close form div //$NON-NLS-1$ 
		html.append("</servoy:panel>\n"); //$NON-NLS-1$ 
		html.append("</form>\n"); //$NON-NLS-1$ 
		html.append("</body>\n"); //$NON-NLS-1$ 
		html.append("</html>\n"); //$NON-NLS-1$ 
	}

	public void renderOpenPartHTML(StringBuffer html, TextualCSS css, Part part)
	{
		if (part.getPartType() == Part.BODY)
		{
			html.append("<div servoy:id='View'>\n"); //$NON-NLS-1$ 
			renderNavigator(html, part);
		}

		String partID = ComponentFactory.getWebID(f, part);
		html.append("<div servoy:id='"); //$NON-NLS-1$ 
		html.append(partID);//Part.getDisplayName(part.getPartType()));		
		html.append("' id='"); //$NON-NLS-1$ 
		html.append(partID);
		html.append("'>\n"); //$NON-NLS-1$ 

		TextualStyle partStyle = css.addStyle('#' + partID);
		fillPartStyle(partStyle, part);
	}

	public void renderClosePartHTML(StringBuffer html, Part part)
	{
		html.append("</div>\n"); //close part div //$NON-NLS-1$
		if (part.getPartType() == Part.BODY)
		{
			html.append("</div>\n"); //close view div //$NON-NLS-1$ 
		}
	}

	public void renderOpenTableViewHTML(StringBuffer html, TextualCSS css, Part part)
	{
		TextualStyle wrapperStyle = new TextualStyle();
		fillPartStyle(wrapperStyle, part);

		html.append("<div servoy:id='View' "); //$NON-NLS-1$
		html.append(wrapperStyle.toString());
		html.append(">\n"); //$NON-NLS-1$ 
	}

	public void renderCloseTableViewHTML(StringBuffer html)
	{
		html.append("</div>\n"); //$NON-NLS-1$
	}

	private void fillPartStyle(TextualStyle partStyle, Part part)
	{
		if (part.getBackground() != null)
		{
			partStyle.setProperty("background-color", PersistHelper.createColorString(part.getBackground())); //$NON-NLS-1$ 
		}

		if (part.getPartType() == Part.BODY)
		{
			final int scrollBars = f.getScrollbars();

			String overflowX = "auto"; //$NON-NLS-1$
			if ((scrollBars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) overflowX = "hidden"; //$NON-NLS-1$ 
			else if ((scrollBars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) overflowX = "scroll"; //$NON-NLS-1$
			partStyle.setProperty("overflow-x", overflowX); //$NON-NLS-1$

			String overflowY = "auto"; //$NON-NLS-1$
			if ((scrollBars & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) overflowY = "hidden"; //$NON-NLS-1$
			else if ((scrollBars & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) overflowY = "scroll"; //$NON-NLS-1$
			partStyle.setProperty("overflow-y", overflowY); //$NON-NLS-1$
		}
		else
		{
			partStyle.setProperty("overflow", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$ 
		}

		int spaceUsedOnlyInPrintAbove = 0;
		int spaceUsedOnlyInPrintBelow = 0;
		Iterator<Part> allParts = f.getParts();
		while (allParts.hasNext())
		{
			Part otherPart = allParts.next();
			if (Part.rendersOnlyInPrint(otherPart.getPartType()))
			{
				int otherPartHeight = otherPart.getHeight() - f.getPartStartYPos(otherPart.getID());
				if (part.getPartType() > otherPart.getPartType()) spaceUsedOnlyInPrintAbove += otherPartHeight;
				if (part.getPartType() < otherPart.getPartType()) spaceUsedOnlyInPrintBelow += otherPartHeight;
			}
		}

		partStyle.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
		fillPartLayoutCSS(partStyle, part, spaceUsedOnlyInPrintAbove, spaceUsedOnlyInPrintBelow);
	}

	private void renderNavigator(StringBuffer html, Part bodyPart)
	{
		if (defaultNavigatorShift != 0)
		{
			TextualStyle navigatorStyle = new TextualStyle();
			if (bodyPart.getBackground() != null)
			{
				navigatorStyle.setProperty("background-color", PersistHelper.createColorString(bodyPart.getBackground())); //$NON-NLS-1$ 
			}
			navigatorStyle.setProperty("overflow", "auto"); //$NON-NLS-1$ //$NON-NLS-2$
			navigatorStyle.setProperty("position", "absolute"); //$NON-NLS-1$ //$NON-NLS-2$
			fillNavigatorLayoutCSS(navigatorStyle);

			html.append("<div servoy:id='default_navigator' "); //$NON-NLS-1$
			html.append(navigatorStyle.toString());
			html.append("></div>"); //$NON-NLS-1$ 
		}
	}

	private String buildFormID()
	{
		return "form_" + ComponentFactory.stripIllegalCSSChars(f.getName()); //$NON-NLS-1$
	}

	protected abstract void fillFormLayoutCSS(TextualStyle formStyle);

	protected abstract void fillPartLayoutCSS(TextualStyle partStyle, Part part, int spaceUsedOnlyInPrintAbove, int spaceUsedOnlyInPrintBelow);

	protected abstract void fillNavigatorLayoutCSS(TextualStyle navigatorStyle);
}
