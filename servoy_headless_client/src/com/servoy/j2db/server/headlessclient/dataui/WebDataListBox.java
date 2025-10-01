/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
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
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportScroll;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeScrollableValuelistComponent;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents a single/multiple choice list field in the web browser.
 *
 * @author lvostinar
 *
 */
public class WebDataListBox extends Component
	implements IDisplayData, IFieldComponent, IDisplayRelatedData, IResolveObject, IProviderStylePropertyChanges, IScrollPane, ISupportWebBounds,
	IOwnTabSequenceHandler, ISupportValueList, IFormattingComponent, ISupportSimulateBoundsProvider, ISupportScroll
{
	private static final long serialVersionUID = 1L;
	private static final String NO_COLOR = "NO_COLOR"; //$NON-NLS-1$

	private final IApplication application;
	private final WebComboModelListModelWrapper list;
	private final WebEventExecutor eventExecutor;

//	private Cursor cursor;
	private boolean needEntireState;
//	private int maxLength;
	private Insets margin;
//	private int horizontalAlignment;
	private String inputId;
	private String tmpForeground = NO_COLOR;

	private IValueList vl;
	private int tabIndex = -1;
	private final boolean multiSelection;
	private int vScrollPolicy;
	private final AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> scriptable;

	public WebDataListBox(IApplication application, AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> scriptable, String id,
		IValueList vl, boolean multiSelection)
	{
		super(id);
		this.application = application;
		this.vl = vl;

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);

		list = new WebComboModelListModelWrapper(vl, true, true);
		list.addListDataListener(new ListDataListener()
		{
			@Override
			public void intervalAdded(ListDataEvent e)
			{
				getStylePropertyChanges().setChanged();
			}

			@Override
			public void intervalRemoved(ListDataEvent e)
			{
				getStylePropertyChanges().setChanged();
			}

			@Override
			public void contentsChanged(ListDataEvent e)
			{
				getStylePropertyChanges().setChanged();
			}
		});
		list.setMultiValueSelect(multiSelection);

		this.multiSelection = multiSelection;
		this.scriptable = scriptable;
		scriptable.setList(list);
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(AbstractFormLayoutProvider.DEFAULT_LABEL_PADDING,
			AbstractFormLayoutProvider.DEFAULT_LABEL_PADDING);
	}

	public final AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> getScriptObject()
	{
		return scriptable;
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
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

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		isValueValid = valid;
		if (!isValueValid)
		{
			previousValidValue = oldVal;
			requestFocusToComponent();
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
		if (eventExecutor.hasChangeCmd() || eventExecutor.hasActionCmd())
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebDataListBox.this);

					//if change cmd is not succeeded also don't call action cmd?
					if (isValueValid)
					{
						eventExecutor.fireActionCommand(false, WebDataListBox.this);
					}
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

	public void setValidationEnabled(boolean b)
	{
		if (vl.getFallbackValueList() != null)
		{
			if (b)
			{
				list.register(vl);
			}
			else
			{
				list.register(vl.getFallbackValueList());
			}
		}

		eventExecutor.setValidationEnabled(b);
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
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
	public void setMaxLength(int maxLength)
	{
//		this.maxLength = maxLength;
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

	/**
	 * @see com.servoy.j2db.ui.IComponent#setCursor(java.awt.Cursor)
	 */
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
		// add this code in order for js_getSelectedItems to work
		List<Integer> selectedRows = new ArrayList<Integer>();
		if (value instanceof String)
		{
			String delim = (eventExecutor.getValidationEnabled() ? "\n" : "%\n"); //$NON-NLS-1$//$NON-NLS-2$
			StringTokenizer tk = new StringTokenizer(value.toString(), delim);
			while (tk.hasMoreTokens())
			{
				int row = list.realValueIndexOf(tk.nextToken());
				if (row >= 0) selectedRows.add(Integer.valueOf(row));
			}
		}
		else
		{
			int row = list.realValueIndexOf(value);
			if (row >= 0) selectedRows.add(Integer.valueOf(row));
		}
		// first select then unselect because list cannot remain without selection
		for (Integer selectedRow : selectedRows)
		{
			list.setElementAt(Boolean.TRUE, selectedRow.intValue());
		}
		for (int i = 0; i < list.size(); i++)
		{
			if (!selectedRows.contains(Integer.valueOf(i)))
			{
				list.setElementAt(Boolean.FALSE, i);
			}
		}
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
		if (getStylePropertyChanges().isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEditListener()
	 */
	public boolean needEditListener()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
	}

	/*
	 * format---------------------------------------------------
	 */
	public void installFormat(ComponentFormat componentFormat)
	{
	}


	@Override
	public String toString()
	{
		return scriptable.toString("value:" + getValueObject()); //$NON-NLS-1$
	}

	/*
	 * _____________________________________________________________ Methods for IDisplayRelatedData
	 */
	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		boolean listContentChanged = false;
		Object[] oldListValue = list.toArray();
		list.fill(state);
		listContentChanged = !list.compareTo(oldListValue);
		if (listContentChanged)
		{
			getStylePropertyChanges().setChanged();
		}
	}

	public String getSelectedRelationName()
	{
		if (relationName == null && list != null)
		{
			relationName = list.getRelationName();
		}
		return relationName;
	}

	private String relationName = null;

	public String[] getAllRelationNames()
	{
		String selectedRelationName = getSelectedRelationName();
		if (selectedRelationName == null)
		{
			return new String[0];
		}
		else
		{
			return new String[] { selectedRelationName };
		}
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (!isValueValid)
		{
			requestFocusToComponent();
			return false;
		}
		return true;
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		//ignore
	}

	public void destroy()
	{
		list.deregister();
	}

	public List<SortColumn> getDefaultSort()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
	}

	/*
	 * _____________________________________________________________ Methods for model object resolve
	 */

	public Object resolveRealValue(Object displayVal)
	{
		if (displayVal instanceof List)
		{
			return getScriptObject().getChoiceValue(((List< ? >)displayVal).toArray(), false);
		}
		return null;
	}

	public Object resolveDisplayValue(Object realVal)
	{
		return getScriptObject().resolveChoiceValues(realVal);
	}

	/**
	 * @see com.servoy.j2db.ui.IScrollPane#setHorizontalScrollBarPolicy(int)
	 */
	public void setHorizontalScrollBarPolicy(int policy)
	{
	}

	/**
	 * @see com.servoy.j2db.ui.IScrollPane#setVerticalScrollBarPolicy(int)
	 */
	public void setVerticalScrollBarPolicy(int policy)
	{
		this.vScrollPolicy = policy;
	}

	public void requestFocusToComponent()
	{
		// TODO this doesn't work as expected right now - should request focus on one of the inner
		// input tags - created during onComponentTagBody
		// is the current container always the right one...
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(this);
		}
	}

	public IValueList getValueList()
	{
		return vl;
	}

	public ListDataListener getListener()
	{
		return null;
	}

	public void setValueList(IValueList vl)
	{
		this.vl = vl;
		list.register(vl);
		getStylePropertyChanges().setChanged();

	}

	/*
	 * dataprovider---------------------------------------------------
	 */
	private String dataProviderID;

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
		list.setDataProviderID(dataProviderID);
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	private boolean editState;
	private boolean editable = true;

	public void setReadOnly(boolean b)
	{
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

	public boolean isReadOnly()
	{
		return !editable;
	}


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

	public boolean isEditable()
	{
		return !isReadOnly();
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
		Dimension d = ((ChangesRecorder)getStylePropertyChanges()).calculateWebSize(size.width, size.height, null, null, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPaddingAndBorder(size.height, null, null, 0, null);
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


	public void handleOwnTabIndex(int newTabIndex)
	{
		this.tabIndex = newTabIndex;
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
}
