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
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.RuntimeDataCombobox;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents a drop down field in the webbrowser.
 *
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class WebDataComboBox extends Component implements IFieldComponent, IDisplayData, IDisplayRelatedData, IProviderStylePropertyChanges,
	ISupportWebBounds, ISupportValueList, IFormattingComponent, ISupportSimulateBoundsProvider, ISupportOnRender
{
	private static final long serialVersionUID = 1L;

	private static final String NO_COLOR = "NO_COLOR"; //$NON-NLS-1$

	private final WebComboModelListModelWrapper list;
	private final WebEventExecutor eventExecutor;

//	private String completeFormat;
//	protected String displayFormat;

	private String inputId;
//	private Cursor cursor;
//	private int maxLength;
	private Insets margin;
//	private int horizontalAlignment;
	private final IApplication application;
	private final RuntimeDataCombobox scriptable;

	private boolean editable = true;
	private boolean readOnly = false;
	private boolean enabledState = true;
	private boolean editState = true;


	private IValueList valueList;

	public WebDataComboBox(IApplication application, RuntimeDataCombobox scriptable, String name, IValueList valueList)
	{
		super(name);
		this.application = application;
		this.valueList = valueList;
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);

		list = new WebComboModelListModelWrapper(valueList, false, true)
		{
			private static final long serialVersionUID = 1L;
			private boolean valueInList = true;

			/**
			 * @see com.servoy.j2db.util.model.ComboModelListModelWrapper#getSize()
			 */
			@Override
			public int size()
			{
				if (!valueInList)
				{
					return super.size() + 1;
				}
				return super.size();
			}

			/**
			 * @see com.servoy.j2db.util.model.ComboModelListModelWrapper#getElementAt(int)
			 */
			@Override
			public Object getElementAt(int index)
			{
				int idx = index;
				if (!valueInList)
				{
					if (idx == 0)
					{
						if (hasRealValues())
						{
							return null;
						}
						return getRealSelectedItem();
					}
					if (idx >= 0) idx--;
				}
				return super.getElementAt(idx);
			}

			/**
			 * @see com.servoy.j2db.util.model.ComboModelListModelWrapper#getRealElementAt(int)
			 */
			@Override
			public Object getRealElementAt(int index)
			{
				int idx = index;
				if (!valueInList)
				{
					if (idx == 0)
					{
						return getRealSelectedItem();
					}
					if (idx >= 0) idx--;
				}
				return super.getRealElementAt(idx);
			}

			/**
			 * @see com.servoy.j2db.util.model.ComboModelListModelWrapper#realValueIndexOf(java.lang.Object)
			 */
			@Override
			public int realValueIndexOf(Object obj)
			{
				if (!valueInList)
				{
					Object realSelectedItem = getRealSelectedItem();
					if (Utils.equalObjects(realSelectedItem, obj))
					{
						return 0;
					}
					int index = super.realValueIndexOf(obj);
					if (index != -1) return index + (index >= 0 ? 1 : 0);
					return -1;
				}
				else
				{
					return super.realValueIndexOf(obj);
				}
			}

			/**
			 * @see com.servoy.j2db.util.model.ComboModelListModelWrapper#indexOf(java.lang.Object)
			 */
			@Override
			public int indexOf(Object o)
			{
				// the web always will call List.indexOf instead of realindex of because o == the real object here.
				int index = listModel.realValueIndexOf(o);
				if (!valueInList && index >= -1)
				{
					index = index + 1;
				}
				if (hideFirstValue) index--;
				return index;
			}

			/**
			 * @see com.servoy.j2db.util.model.ComboModelListModelWrapper#setSelectedItem(java.lang.Object)
			 */
			@Override
			public void setSelectedItem(Object anObject)
			{
				super.setSelectedItem(anObject);
				Object realSelectedItem = getRealSelectedItem();
				valueInList = (getValueList().getAllowEmptySelection() && realSelectedItem == null) || (super.realValueIndexOf(realSelectedItem) != -1);
			}
		};

		list.addListDataListener(new ListDataListener()
		{
			public void intervalRemoved(ListDataEvent e)
			{
				if (ignoreChanges) return;
				getStylePropertyChanges().setChanged();
				Object obj = list.getSelectedItem();
				list.setSelectedItem(obj);
			}

			public void intervalAdded(ListDataEvent e)
			{
				if (ignoreChanges) return;
				getStylePropertyChanges().setChanged();
				Object obj = list.getSelectedItem();
				list.setSelectedItem(obj);
			}

			public void contentsChanged(ListDataEvent e)
			{
				if (ignoreChanges) return;
				getStylePropertyChanges().setChanged();
				Object obj = list.getSelectedItem();
				list.setSelectedItem(obj);
			}
		});

		this.scriptable = scriptable;
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(AbstractFormLayoutProvider.DEFAULT_LABEL_PADDING,
			AbstractFormLayoutProvider.DEFAULT_LABEL_PADDING);
	}

	public final RuntimeDataCombobox getScriptObject()
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
	private String tmpForeground = NO_COLOR;

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		if (!valid)
		{
			getStylePropertyChanges().setChanged();
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
		if (eventExecutor.hasChangeCmd() || eventExecutor.hasActionCmd())
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebDataComboBox.this);

					//if change cmd is not succeeded also don't call action cmd?
					if (isValueValid)
					{
						eventExecutor.fireActionCommand(false, WebDataComboBox.this);
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
		if (eventExecutor.getValidationEnabled() == b) return;
		if (dataProviderID != null && ScopesUtils.isVariableScope(dataProviderID)) return;
		eventExecutor.setValidationEnabled(b);

		if (valueList.getFallbackValueList() != null)
		{
			if (b)
			{
				list.register(valueList);
			}
			else
			{
				list.register(valueList.getFallbackValueList());
			}
		}

		boolean old;
		if (b)
		{
			old = enabledState;
			setComponentEnabled(enabledState, false, readOnly);
			enabledState = old;
			setEditable(editState);
		}
		else if (enabledState && (!isEditable() || !isEnabled()))
		{
			old = editState;
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
			editState = old;

			old = enabledState;
			setComponentEnabled(true, false, false);
			enabledState = old;
		}
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}


	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;
	}

	private boolean needEntireState;

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public void setMaxLength(int maxLength)
	{
//		this.maxLength = maxLength;
	}

	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public Insets getMargin()
	{
		return margin;
	}

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
		refreshValueInList();
		if (getStylePropertyChanges().isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	public void refreshValueInList()
	{
		Object modelObject = getValueObject();
		int realValueIndexOf = list.realValueIndexOf(modelObject);
		if (realValueIndexOf == -1)
		{
			if (modelObject == null || "".equals(modelObject) || list.hasRealValues()) //$NON-NLS-1$
			{
				list.setSelectedItem(null);
			}
			else
			{
				list.setSelectedItem(modelObject);
			}
		}
		else
		{
			list.setSelectedItem(list.getElementAt(realValueIndexOf));
		}
	}

	public boolean needEditListener()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
	}


	/*
	 * _____________________________________________________________ Methods for IDisplayRelatedData
	 */
	private boolean ignoreChanges;

	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		Object selectedItem = list.getSelectedItem();
		boolean listContentChanged = false;
		try
		{
			ignoreChanges = true;
			Object[] oldListValue = list.toArray();
			list.fill(state);
			listContentChanged = !list.compareTo(oldListValue);
		}
		finally
		{
			ignoreChanges = false;
		}
		if (listContentChanged || !Utils.equalObjects(list.getSelectedItem(), selectedItem))
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


	public void requestFocusToComponent()
	{
		// is the current container always the right one...
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(this);
		}
	}

	public IValueList getValueList()
	{
		return valueList;
	}

	public void setValueList(IValueList vl)
	{
		this.valueList = vl;
		list.register(vl);
		getStylePropertyChanges().setChanged();
	}

	public ListDataListener getListener()
	{
		return null;
	}


	/*
	 * format---------------------------------------------------
	 */
	public void installFormat(ComponentFormat componentFormat)
	{
		if (!componentFormat.parsedFormat.isEmpty() && getScriptObject().getComponentFormat().parsedFormat.hasEditFormat())
		{
			Debug.log("WARNING Display and Edit formats are not used in browser comboboxes. Such browser controls do not support editing"); //$NON-NLS-1$
		}
	}

	private List<ILabel> labels;

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
	public void setComponentEnabled(boolean b)
	{
		setComponentEnabled(b, true, readOnly);
	}

	public void setComponentEnabled(boolean b, boolean setLabels, boolean readOnly)
	{
		if (accessible || !b)
		{
			enabledState = b;
			super.setEnabled(b && !readOnly);
			getStylePropertyChanges().setChanged();
			if (labels != null && setLabels)
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
	 * readonly/editable---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEnabled();
	}

	public void setReadOnly(boolean b)
	{
		if (b == readOnly) return;
		readOnly = b;

		if (b)
		{
			if (isEnabled())
			{
				boolean old;
				if (isEditable())
				{
					old = editState;
					setEditable(false);
					editState = old;
				}
				old = enabledState;
				setComponentEnabled(false, false, readOnly);
				enabledState = old;
			}
		}
		else
		{
			setComponentEnabled(enabledState, false, readOnly);
			setEditable(editState);
		}
	}

	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable(boolean b)
	{
		// never allow combos with real values to be editable
		editState = b && (!list.hasRealValues());
		editable = b && (!list.hasRealValues());
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
		list.setDataProviderID(dataProviderID);
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public String js_getDataProviderID()
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
	public Point getLocation()
	{
		return location;
	}

	@Override
	public void setLocation(Point location)
	{
		this.location = location;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

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


	@Override
	public String toString()
	{
		return scriptable.toString("value:" + getValueObject()); //$NON-NLS-1$
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
}
