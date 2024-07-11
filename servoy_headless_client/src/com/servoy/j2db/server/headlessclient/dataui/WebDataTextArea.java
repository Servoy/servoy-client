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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.Component;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportInputSelection;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportScroll;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 *  Represents a textarea field in the webbrowser.
 *
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class WebDataTextArea extends Component implements IFieldComponent, IDisplayData, IProviderStylePropertyChanges, ISupportWebBounds,
	ISupportInputSelection, ISupportSimulateBoundsProvider, ISupportOnRender, ISupportScroll
{
	private static final long serialVersionUID = 1L;

//	private boolean selectOnEnter;
//	private Cursor cursor;
	private boolean needEntireState;
	private Insets margin;
//	private int horizontalAlignment;
	private String inputId;
	private final WebEventExecutor eventExecutor;

	private final IApplication application;

	private AbstractRuntimeField<IFieldComponent> scriptable;

	public WebDataTextArea(final IApplication application, final AbstractRuntimeField<IFieldComponent> scriptable, String id)
	{
		super(id);
		this.application = application;

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
	}

	public final AbstractRuntimeField<IFieldComponent> getScriptObject()
	{
		return scriptable;
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}


	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setSelectOnEnter(boolean)
	 */
	public void setSelectOnEnter(boolean selectOnEnter)
	{
//		this.selectOnEnter = selectOnEnter;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#addScriptExecuter(com.servoy.j2db.IScriptExecuter)
	 */
	public void addScriptExecuter(IScriptExecuter scriptExecuter)
	{
		eventExecutor.setScriptExecuter(scriptExecuter);
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setEnterCmds(java.lang.String, Object[][])
	 */
	public void setEnterCmds(String[] enterCmds, Object[][] args)
	{
		eventExecutor.setEnterCmds(enterCmds, args);

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setLeaveCmds(String[], Object[][])
	 */
	public void setLeaveCmds(String[] leaveCmds, Object[][] args)
	{
		eventExecutor.setLeaveCmds(leaveCmds, args);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setActionCmd(java.lang.String, Object[])
	 */
	public void setActionCmd(String actionCmd, Object[] args)
	{
		eventExecutor.setActionCmd(actionCmd, args);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setChangeCmd(java.lang.String, Object[])
	 */
	public void setChangeCmd(String changeCmd, Object[] args)
	{
		eventExecutor.setChangeCmd(changeCmd, args);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setNeedEntireState(boolean)
	 */
	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEntireState()
	 */
	public boolean needEntireState()
	{
		return needEntireState;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public Insets getMargin()
	{
		return margin;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int horizontalAlignment)
	{
//		this.horizontalAlignment = horizontalAlignment;
	}

	@Override
	public void setCursor(Cursor cursor)
	{
//		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		return null;
	}

	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
	}

	public boolean needEditListener()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
	}

	public boolean isValueValid()
	{
		return isValueValid;
	}

	private boolean isValueValid = true;
	private Object previousValidValue;

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		isValueValid = valid;
		if (!isValueValid)
		{
			previousValidValue = oldVal;
			requestFocusToComponent();
		}
		else
		{
			previousValidValue = null;
		}
	}

	public void notifyLastNewValueWasChange(final Object oldVal, final Object newVal)
	{
		if (eventExecutor.hasChangeCmd())
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebDataTextArea.this);
				}
			});
		}
		else
		{
			setValueValid(true, null);
		}
	}

	private boolean wasEditable;

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;
		if (ScopesUtils.isVariableScope(dataProviderID)) return;

		eventExecutor.setValidationEnabled(b);

		boolean prevEditState = editState;
		if (b)
		{
			setEditable(wasEditable);
		}
		else
		{
			wasEditable = editable;
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
		}
		editState = prevEditState;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplay#stopUIEditing(boolean)
	 */
	public boolean stopUIEditing(boolean looseFocus)
	{
		if (!isValueValid)
		{
			requestFocusToComponent();
			return false;
		}
		return true;
	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public void requestFocusToComponent()
	{
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(this);
		}
	}

	private String textSelection;

	public void setSelectedText(String sel)
	{
		textSelection = sel;
	}

	public String getSelectedText()
	{
		if (textSelection != null)
		{
			String sel = textSelection;
			setSelectedText(null);
			return sel;
		}
		return null;
	}

	/*
	 * readonly/editable---------------------------------------------------
	 */
	private boolean editable;

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
	}

	public boolean isEditable()
	{
		return editable;
	}

	public boolean isReadOnly()
	{
		return !editable;
	}

	private boolean editState;

	public void setReadOnly(boolean b)
	{
		if (b && !editable) return;
		if (b)
		{
			setEditable(false);
			editState = true;
		}
		else
		{
			setEditable(editState);
		}
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	private String dataProviderID;


	@Override
	public void setName(String n)
	{
		name = n;
	}

	private String name;

	@Override
	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	@Override
	public void setBorder(Border border)
	{
		this.border = border;
	}

	@Override
	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	@Override
	public boolean isOpaque()
	{
		return opaque;
	}

	/*
	 * titleText---------------------------------------------------
	 */

	private String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	private String tooltip;

	@Override
	public void setToolTipText(String tooltip)
	{
		this.tooltip = Utils.stringIsEmpty(tooltip) ? null : tooltip;
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		return tooltip;
	}


	/*
	 * font---------------------------------------------------
	 */
	@Override
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	@Override
	public Font getFont()
	{
		return font;
	}


	private Color background;

	@Override
	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	@Override
	public Color getBackground()
	{
		return background;
	}


	private Color foreground;

	private List<ILabel> labels;

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	@Override
	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	@Override
	public Color getForeground()
	{
		return foreground;
	}

	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		if (viewable || !visible)
		{
			setVisible(visible);
			if (labels != null)
			{
				for (ILabel label : labels)
				{
					label.setComponentVisible(visible);
				}
			}
		}
	}


	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public List<ILabel> getLabelForElementNames()
	{
		return labels;
	}


	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			((ChangesRecorder)getStylePropertyChanges()).setChanged();
			if (labels != null)
			{
				for (ILabel label : labels)
				{
					label.setComponentEnabled(b);
				}
			}
		}
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	private boolean viewable = true;

	public void setViewable(boolean b)
	{
		if (!b) setComponentVisible(b);
		this.viewable = b;
	}

	public boolean isViewable()
	{
		return viewable;
	}

	/*
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	@Override
	public void setLocation(Point location)
	{
		this.location = location;
	}

	@Override
	public Point getLocation()
	{
		return location;
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	@Override
	public Dimension getSize()
	{
		return size;
	}

	public Rectangle getWebBounds()
	{
		Dimension d = ((ChangesRecorder)getStylePropertyChanges()).calculateWebSize(size.width, size.height, border, margin, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPaddingAndBorder(size.height, border, margin, 0, null);
	}

	@Override
	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
	}


	@Override
	public String toString()
	{
		return scriptable.toString("value:"); //$NON-NLS-1$
	}


	public void fireOnRender(boolean force)
	{
		if (scriptable != null)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(isFocused);
		}
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportInputSelection#selectAll()
	 */
	@Override
	public void selectAll()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.ISupportInputSelection#replaceSelectedText(java.lang.String)
	 */
	@Override
	public void replaceSelectedText(String s)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	@Override
	public void setMaxLength(int maxLength)
	{
		// TODO Auto-generated method stub

	}
}
