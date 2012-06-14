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

import java.util.Iterator;
import java.util.Locale;

import javax.swing.border.Border;

import org.apache.wicket.ResourceReference;
import org.xhtmlrenderer.css.constants.CSSName;

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
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyStyleSheet;
import com.servoy.j2db.util.Utils;

/**
 * Generic superclass of available layout providers for web client. Holds the common
 * functionality.
 * 
 * @author gerzse
 */
public abstract class AbstractFormLayoutProvider implements IFormLayoutProvider
{
	private final Solution solution;
	protected final Form f;
	private final String formInstanceName;

	private boolean addHeaders;
	protected int defaultNavigatorShift;
	private final Border border;
	protected String orientation;
	int viewType;
	private final IServiceProvider sp;
	private IStyleRule style = null;
	private boolean hasImage = false;

	public AbstractFormLayoutProvider(IServiceProvider sp, Solution solution, Form f, String formInstanceName)
	{
		this.sp = sp;
		this.solution = solution;
		this.f = f;
		this.formInstanceName = formInstanceName;

		addHeaders = true;
		defaultNavigatorShift = 0;
		viewType = f.getView();
		if (viewType == IForm.LIST_VIEW || viewType == FormController.LOCKED_LIST_VIEW)
		{
			addHeaders = false;//list views do not have headers
		}
		if (sp != null && sp.getFlattenedSolution().isInDesign(f) &&
			(viewType == FormController.LOCKED_TABLE_VIEW || viewType == IForm.LIST_VIEW || viewType == FormController.LOCKED_LIST_VIEW))
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
		}

		// Look into styles.
		Pair<IStyleSheet, IStyleRule> pairStyle = ComponentFactory.getCSSPairStyleForForm(sp, f);
		if (pairStyle != null)
		{
			style = pairStyle.getRight();
		}

		orientation = OrientationApplier.getHTMLContainerOrientation(sp != null ? sp.getLocale() : Locale.getDefault(), solution.getTextOrientation());
	}

	public String getFormInstanceName()
	{
		return this.formInstanceName;
	}

	public int getViewType()
	{
		return viewType;
	}

	public boolean needsHeaders()
	{
		return addHeaders;
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
		html.append((f.getTitleText() != null ? TemplateGenerator.getSafeText(f.getTitleText()) : getFormInstanceName()));
		html.append(" - Servoy"); //$NON-NLS-1$ 
		html.append("</title>\n"); //$NON-NLS-1$ 
		html.append("<servoy:head>\n"); //$NON-NLS-1$ 
		html.append("</servoy:head>\n"); //$NON-NLS-1$ 
		html.append("</head>\n"); //$NON-NLS-1$ 
		html.append("<body id='servoy_page'>\n"); //$NON-NLS-1$ 
		html.append("<form id='servoy_dataform'>\n"); //$NON-NLS-1$ 
		html.append("<servoy:panel>\n"); //$NON-NLS-1$

		String buildFormID = buildFormID();

		html.append("<div servoy:id='servoywebform' id='"); //$NON-NLS-1$ 
		html.append(buildFormID);
		html.append("'>\n"); //$NON-NLS-1$ 		

		// following two divs are here only because a bug in IE7 made divs that were anchored on all sides break iframe behavior (so dialogs)
		html.append("<div id='sfw_"); //$NON-NLS-1$
		html.append(buildFormID);
		html.append("' style='position: absolute; height: 0px; right: 0px; left: 0px;'/>"); //$NON-NLS-1$ 
		html.append("<div id='sfh_"); //$NON-NLS-1$
		html.append(buildFormID);
		html.append("' style='position: absolute; bottom: 0px; top: 0px; width: 0px;'/>"); //$NON-NLS-1$ 
		// the 2 divs above are used to keep track of the form's size when browser resizes		

		// Put CSS properties for background color and border (if any).
		TextualStyle formStyle = css.addStyle("#" + buildFormID()); //$NON-NLS-1$ 
		if (style != null && style.getValue(CSSName.BACKGROUND_COLOR.toString()) != null && !f.getTransparent())
		{
			formStyle.setProperty(CSSName.BACKGROUND_COLOR.toString(), style.getValues(CSSName.BACKGROUND_COLOR.toString()), true);
		}
		if (border != null)
		{
			String type = ComponentFactoryHelper.createBorderString(border);
			ComponentFactoryHelper.createBorderCSSProperties(type, formStyle);
		}
		else if (style != null)
		{
			copyBorderAttributes(style, formStyle);
		}
		hasImage = addBackgroundImageAttributeIfExists(style, formStyle);
		fillFormLayoutCSS(formStyle);
	}

	private void copyBorderAttributes(IStyleRule source, TextualStyle destination)
	{
		if (source != null && destination != null)
		{
			for (String property : ServoyStyleSheet.BORDER_CSS)
			{
				if (source.hasAttribute(property))
				{
					destination.setProperty(property, source.getValues(property), true);
				}
			}
			for (String property : ServoyStyleSheet.borderAttributesExtensions)
			{
				if (source.hasAttribute(property))
				{
					destination.setProperty(property, source.getValues(property), true);
				}
			}
		}
	}

	private boolean addBackgroundImageAttributeIfExists(IStyleRule styleRule, TextualStyle textStyle)
	{
		boolean exists = false;
		if (styleRule != null)
		{
			for (String attName : ServoyStyleSheet.BACKGROUND_IMAGE_CSS)
			{
				if (styleRule.hasAttribute(attName))
				{
					textStyle.setProperty(attName, styleRule.getValues(attName), true);
					exists = true;
				}
			}
		}
		return exists;
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
		html.append("' class='formpart'>\n"); //$NON-NLS-1$ 

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
		html.append(StripHTMLTagsConverter.convertMediaReferences(wrapperStyle.toString(), solution.getName(), new ResourceReference("media"), "").toString());
		html.append(">\n"); //$NON-NLS-1$ 
	}

	public void renderCloseTableViewHTML(StringBuffer html)
	{
		html.append("</div>\n"); //$NON-NLS-1$
	}

	private void fillPartStyle(TextualStyle partStyle, Part part)
	{
		Pair<IStyleSheet, IStyleRule> pairStyle = ComponentFactory.getStyleForBasicComponent(sp, part, f);
		if (pairStyle != null)
		{
			addBackgroundImageAttributeIfExists(pairStyle.getRight(), partStyle);
		}

		fillPartBackground(partStyle, part);

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

		if (pairStyle != null && pairStyle.getLeft() != null && pairStyle.getRight() != null)
		{
			copyBorderAttributes(pairStyle.getRight(), partStyle);
		}
	}

	private void renderNavigator(StringBuffer html, Part bodyPart)
	{
		if (defaultNavigatorShift != 0)
		{
			TextualStyle navigatorStyle = new TextualStyle();
			fillPartBackground(navigatorStyle, bodyPart);

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
		return "form_" + ComponentFactory.stripIllegalCSSChars(getFormInstanceName()); //$NON-NLS-1$
	}

	private void fillPartBackground(TextualStyle partStyle, Part part)
	{
		Pair<IStyleSheet, IStyleRule> pairStyle = ComponentFactory.getStyleForBasicComponent(sp, part, f);
		if (!hasImage || part.getBackground() != null ||
			(pairStyle != null && pairStyle.getRight() != null && pairStyle.getRight().hasAttribute("background-color"))) //$NON-NLS-1$
		{
			String[] cssValues = ComponentFactory.getPartBackgroundCSSDeclarations(sp, part, f);
			if (cssValues != null)
			{
				// for fallback mechanism
				partStyle.setProperty("background-color", cssValues, true);
			}
			else if (part.getBackground() != null && !f.getTransparent())
			{
				partStyle.setProperty("background-color", PersistHelper.createColorString(part.getBackground())); //$NON-NLS-1$ 
			}
		}
	}

	protected abstract void fillFormLayoutCSS(TextualStyle formStyle);

	protected abstract void fillPartLayoutCSS(TextualStyle partStyle, Part part, int spaceUsedOnlyInPrintAbove, int spaceUsedOnlyInPrintBelow);

	protected abstract void fillNavigatorLayoutCSS(TextualStyle navigatorStyle);
}
