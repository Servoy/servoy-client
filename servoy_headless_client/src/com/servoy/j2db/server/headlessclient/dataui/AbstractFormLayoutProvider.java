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

import java.awt.Insets;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.border.Border;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISupportNavigator;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.OrientationApplier;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyStyleSheet;
import com.servoy.j2db.util.Settings;

/**
 * Generic superclass of available layout providers for web client. Holds the common
 * functionality.
 *
 * @author gerzse
 */
public abstract class AbstractFormLayoutProvider implements IFormLayoutProvider
{
	public static final Insets DEFAULT_LABEL_PADDING = new Insets(0, 0, 0, 0);
	public static final Insets DEFAULT_BUTTON_PADDING = new Insets(0, 0, 0, 0);
	public static final Insets DEFAULT_FIELD_PADDING = new Insets(1, 1, 1, 1);
	public static final Insets DEFAULT_BUTTON_BORDER_SIZE = new Insets(1, 1, 1, 1);
	public static final Insets DEFAULT_FIELD_BORDER_SIZE = new Insets(2, 2, 2, 2);


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
	private final boolean hasImage = false;

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
			defaultNavigatorShift = ISupportNavigator.DEFAULT_NAVIGATOR_WIDTH;
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
		String userDefinedClass = "";
		if ("true".equals(Settings.getInstance().getProperty("servoy.webclient.pushClassToHTMLElement", "false")))
			userDefinedClass = (part.getStyleClass() == null
				? "" : part.getStyleClass());
		html.append("' class='formpart " + userDefinedClass + "'>\n"); //$NON-NLS-1$

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
	}

	public void renderCloseTableViewHTML(StringBuffer html)
	{
		html.append("</div>\n"); //$NON-NLS-1$
	}

	public void fillPartStyle(TextualStyle partStyle, Part part)
	{
		Pair<IStyleSheet, IStyleRule> pairStyle = ComponentFactory.getStyleForBasicComponent(sp, part, f);
		if (pairStyle != null)
		{
			addBackgroundImageAttributeIfExists(pairStyle.getRight(), partStyle);
		}

		fillPartBackground(partStyle, part);

		if (part.getPartType() == Part.BODY)
		{
			partStyle.setProperty("overflow-x", getCSSScrolling(f.getScrollbars(), true)); //$NON-NLS-1$
			partStyle.setProperty("overflow-y", getCSSScrolling(f.getScrollbars(), false)); //$NON-NLS-1$
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

	/**
	 * @param defaultNavigatorShift the defaultNavigatorShift to set
	 */
	public void setDefaultNavigatorShift(int defaultNavigatorShift)
	{
		this.defaultNavigatorShift = defaultNavigatorShift;
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

	public static String getCSSScrolling(int scrollBars, boolean horizontal)
	{
		if (horizontal)
		{
			String overflowX = "auto"; //$NON-NLS-1$
			if ((scrollBars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) overflowX = "hidden"; //$NON-NLS-1$
			else if ((scrollBars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) overflowX = "scroll"; //$NON-NLS-1$
			return overflowX;
		}
		else
		{
			String overflowY = "auto"; //$NON-NLS-1$
			if ((scrollBars & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) overflowY = "hidden"; //$NON-NLS-1$
			else if ((scrollBars & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) overflowY = "scroll"; //$NON-NLS-1$
			return overflowY;
		}
	}


	protected abstract void fillFormLayoutCSS(TextualStyle formStyle);

	protected abstract void fillPartLayoutCSS(TextualStyle partStyle, Part part, int spaceUsedOnlyInPrintAbove, int spaceUsedOnlyInPrintBelow);

	protected abstract void fillNavigatorLayoutCSS(TextualStyle navigatorStyle);
}
