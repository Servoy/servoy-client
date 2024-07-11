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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.SwingConstants;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportScroll;
import com.servoy.j2db.ui.scripting.AbstractRuntimeTextEditor;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents the HTML Field in the webbrowser. Has support for call backs of scripts through javascript:methodname links or other tags.
 *
 * @author jcompagner
 */
public class WebDataHtmlView extends WebDataSubmitLink implements IFieldComponent, ISupportScroll
{
	private static final long serialVersionUID = 1L;

	public WebDataHtmlView(IApplication application, AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, String id)
	{
		super(application, scriptable, id);
		setHorizontalAlignment(SwingConstants.LEFT);
		setVerticalAlignment(SwingConstants.TOP);
	}


	/**
	 * This is because in the super classes we add a container <span> tag to fix the horizontal/vertical
	 * alignment, and that tag is missing the 'width' and 'height' attributes by default. By returning true
	 * here we force the two attributes to be added.
	 */
	@Override
	protected boolean hasHtmlOrImage()
	{
		return true;
	}

	@Override
	protected String getCSSId()
	{
		return null;
	}

	@Override
	protected boolean isAnchored()
	{
		return Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$
	}

	public void setActionCmd(String actionCmd, Object[] args)
	{
		IEventExecutor eventExecutor = getEventExecutor();
		if (eventExecutor instanceof WebEventExecutor) ((WebEventExecutor)eventExecutor).setActionCmd(actionCmd, args);
	}

	public void setChangeCmd(String changeCmd, Object[] args)
	{
	}

	public void setEditable(boolean editable)
	{
	}

	public boolean isEditable()
	{
		return false;
	}

	public void setEnterCmds(String[] enterCmds, Object[][] args)
	{
		IEventExecutor eventExecutor = getEventExecutor();
		if (eventExecutor instanceof WebEventExecutor) ((WebEventExecutor)eventExecutor).setEnterCmds(enterCmds, args);
	}

	public void setLeaveCmds(String[] leaveCmds, Object[][] args)
	{
		IEventExecutor eventExecutor = getEventExecutor();
		if (eventExecutor instanceof WebEventExecutor) ((WebEventExecutor)eventExecutor).setLeaveCmds(leaveCmds, args);
	}

	public void setMaxLength(int maxLength)
	{
	}

	public void setSelectOnEnter(boolean selectOnEnter)
	{
	}

	private List<ILabel> labels;

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		if (isViewable() || !visible)
		{
			super.setComponentVisible(visible);
			if (labels != null)
			{
				for (ILabel label : labels)
				{
					label.setComponentVisible(visible);
				}
			}
		}
	}

	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setComponentEnabled(b);
			if (labels != null)
			{
				for (ILabel label : labels)
				{
					label.setComponentEnabled(b);
				}
			}
		}
	}

	@Override
	public int getFontSize()
	{
		// Since the fontSize is used only for calculating web size, it is safe to just return 0
		// in order to avoid any padding adjustments. (issue 169037)
		return 0;
	}

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	public void setReadOnly(boolean b)
	{

	}

	@Override
	public boolean isReadOnly()
	{
		return true;
	}

	@Override
	public void setTitleText(String title)
	{
		super.setTitleText(title);
		//	see ComponentFactory createField
		if (needEntireState() && getDataProviderID() == null)
		{
			setTagText(application.getI18NMessageIfPrefixed(title));
		}
	}

	private final Point scroll = new Point(0, 0);

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportScroll#setScroll(int, int)
	 */
	@Override
	public void setScroll(int x, int y)
	{
		scroll.x = x;
		scroll.y = y;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportScroll#getScroll()
	 */
	@Override
	public Point getScroll()
	{
		return scroll;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportScroll#getScrollComponentMarkupId()
	 */
	@Override
	public String getScrollComponentMarkupId()
	{
		return getMarkupId();
	}
}