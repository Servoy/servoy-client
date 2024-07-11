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
import javax.swing.event.ListDataListener;
import javax.swing.text.Document;

import org.apache.wicket.Component;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportInputSelection;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportSpecialClientProperty;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents a text (single line) field in the webbrowser.
 *
 * @author jcompagner
 */
public class WebDataField extends Component
	implements IFieldComponent, IDisplayData, IProviderStylePropertyChanges, ISupportWebBounds, ISupportValueList, ISupportInputSelection,
	ISupportSpecialClientProperty, IFormattingComponent, ISupportSimulateBoundsProvider, IDestroyable, ISupportOnRender
{

	/**
		 * @author jcompagner
		 *
		 */
	private interface ITestFormatsCallback
	{

		/**
		 * @param displayValue
		 * @param editValue
		 */
		void differentEditAndDisplay(String displayValue, String editValue);

	}

	private static final long serialVersionUID = 1L;

	private static final String NO_COLOR = "NO_COLOR"; //$NON-NLS-1$

//	private Cursor cursor;
	private boolean needEntireState;
	private int dataType;
	private Insets margin;
	private int horizontalAlignment;

	private final WebEventExecutor eventExecutor;
	private String inputId;

	protected ParsedFormat parsedFormat = FormatParser.parseFormatProperty(null);

//	private String completeFormat;
//	private String editFormat;
//	protected String displayFormat;
	protected IValueList list;
	private final AbstractRuntimeField<IFieldComponent> scriptable;
	protected final IApplication application;

	public WebDataField(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, String id, IValueList list)
	{
		this(application, scriptable, id);
		this.list = list;
		this.horizontalAlignment = ISupportTextSetup.LEFT;
	}

	public WebDataField(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, String id)
	{
		this(application, scriptable, id, (IComponent)null);
	}

	/**
	 * @param id
	 */
	public WebDataField(final IApplication application, final AbstractRuntimeField<IFieldComponent> scriptable, String id, final IComponent enclosingComponent)
	{
		super(id);
		this.horizontalAlignment = ISupportTextSetup.LEFT;
		this.application = application;
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX)
		{
			@Override
			protected Object getSource(Object display)
			{
				return enclosingComponent != null ? enclosingComponent : super.getSource(display);
			}
		};
		this.scriptable = scriptable;
	}

	public void destroy()
	{
		if (list != null) list.deregister();
	}

	public final AbstractRuntimeField<IFieldComponent> getScriptObject()
	{
		return scriptable;
	}


	/*
	 * _____________________________________________________________ Methods for event handling
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}


	public void setEnterCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setEnterCmds(ids, args);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setLeaveCmds(ids, args);
	}

	public boolean isValueValid()
	{
		return isValueValid;
	}

	private boolean isValueValid = true;
	private Object previousValidValue;
	private String tmpForeground = NO_COLOR;

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		if (!valid)
		{
			if (valid != isValueValid)
			{
				getStylePropertyChanges().setChanged();
			}
			requestFocusToComponent();
		}
		if (valid == isValueValid)
		{
			return;
		}
		isValueValid = valid;
		if (!isValueValid)
		{
			previousValidValue = oldVal;
			if (tmpForeground == NO_COLOR)
			{
				tmpForeground = scriptable.getFgcolor();
				scriptable.setFgcolor("red"); //$NON-NLS-1$
			}
		}
		else
		{
			previousValidValue = null;
			if (tmpForeground != NO_COLOR)
			{
				scriptable.setFgcolor(tmpForeground);
				tmpForeground = NO_COLOR;
			}
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
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebDataField.this);
				}
			});
		}
		else
		{
			setValueValid(true, null);
		}
	}

	public void setChangeCmd(String id, Object[] args)
	{
		eventExecutor.setChangeCmd(id, args);
	}

	public void setActionCmd(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
	}

	private boolean wasEditable;

	public void setValidationEnabled(boolean validation)
	{
		if (eventExecutor.getValidationEnabled() == validation)
		{
			// web cell based view - reinitializing a component onBeforeRender can result in readonly being set back to true while in find; when validation is reset to false, we must restore find state for the control
			if (!validation && !editable && !Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				boolean prevEditState = editState;
				setEditable(true);// allow search
				editState = prevEditState;
			}
			return;
		}
		eventExecutor.setValidationEnabled(validation);
		boolean prevEditState = editState;
		if (validation)
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

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
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
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	private int maxLength = -1;

	private Object value;

	public void setMaxLength(int maxLength)
	{
		this.maxLength = maxLength;
	}

	protected boolean needsFormatOnchange()
	{
		return false;
	}

	public Insets getMargin()
	{
		return margin;
	}

	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public int getHorizontalAlignment()
	{
		return this.horizontalAlignment;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	@Override
	public void setCursor(Cursor cursor)
	{
//		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		return value;
	}

	public void setValueObject(Object value)
	{
		this.value = value;
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

	public void requestFocusToComponent()
	{
		if (!isReadOnly() && isEnabled())
		{
			// is the current main container always the right one?
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				((MainPage)currentContainer).componentToFocus(this);
			}
		}
	}

	@SuppressWarnings("nls")
	public void selectAll()
	{
	}

	@SuppressWarnings("nls")
	public void replaceSelectedText(String s)
	{
	}

	public String getSelectedText()
	{
		return null;
	}

	public IValueList getValueList()
	{
		return list;
	}

	public void setValueList(IValueList vl)
	{
		this.list = vl;

	}

	public ListDataListener getListener()
	{
		return null;
	}

	/*
	 * readonly/editable---------------------------------------------------
	 */
	private boolean editable;

	public boolean isReadOnly()
	{
		return !editable;
	}

	public boolean isEditable()
	{
		return editable;
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

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
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
		if (Utils.stringIsEmpty(tooltip))
		{
			this.tooltip = null;
		}
		else
		{
			this.tooltip = tooltip;
		}
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

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}


	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{

			super.setEnabled(b);
			getStylePropertyChanges().setChanged();
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
	 * client properties for ui---------------------------------------------------
	 */

	public void setClientProperty(Object key, Object value)
	{
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

	public Insets getPadding()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPadding(border, margin);
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
		return scriptable.toString("value:" + getValueObject()); //$NON-NLS-1$
	}


	public void fireOnRender(boolean force)
	{
		if (!isIgnoreOnRender && scriptable != null)
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

	private boolean isIgnoreOnRender;

	public void setIgnoreOnRender(boolean isIgnoreOnRender)
	{
		this.isIgnoreOnRender = isIgnoreOnRender;
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IFormattingComponent#installFormat(com.servoy.j2db.component.ComponentFormat)
	 */
	@Override
	public void installFormat(ComponentFormat componentFormat)
	{
		// TODO Auto-generated method stub

	}
}
