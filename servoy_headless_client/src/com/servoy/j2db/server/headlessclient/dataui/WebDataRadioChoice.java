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

import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ListDataListener;
import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.ui.scripting.RuntimeRadioChoice;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;


/**
 * Represents a multiply choice single select field in the webbrowser
 * 
 * @author jcompagner
 */
public class WebDataRadioChoice extends RadioChoice implements IDisplayData, IFieldComponent, IDisplayRelatedData, IProviderStylePropertyChanges, IScrollPane,
	ISupportWebBounds, IRightClickListener, IOwnTabSequenceHandler, ISupportValueList
{
	private static final long serialVersionUID = 1L;
	private static final String NO_COLOR = "NO_COLOR"; //$NON-NLS-1$

	private final WebComboModelListModelWrapper list;
	private final WebEventExecutor eventExecutor;

	private Cursor cursor;
	private boolean needEntireState;
	private int maxLength;
	private Insets margin;
	private int horizontalAlignment;
	private String inputId;
	private final IApplication application;
	private String tmpForeground = NO_COLOR;
	private IValueList vl;
	private int tabIndex = -1;
	private int vScrollPolicy;
	protected AbstractRuntimeField scriptable;

	public WebDataRadioChoice(IApplication application, String id, IValueList vl)
	{
		super(id);
		this.application = application;
		this.vl = vl;
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);

		list = new WebComboModelListModelWrapper(vl, true);
		setChoices(list);

		setChoiceRenderer(new WebChoiceRenderer(null, list)); // null because this component does not use a converter (for date/number formats)

		add(new AttributeModifier("readonly", true, new Model<String>() //$NON-NLS-1$
			{
				@Override
				public String getObject()
				{
					return (isEnabled() ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE : AttributeModifier.VALUELESS_ATTRIBUTE_ADD);
				}
			}));

		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);

		updatePrefix();

		scriptable = new RuntimeRadioChoice(this, new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING),
			application, null, list);
	}

	public IScriptable getScriptObject()
	{
		return scriptable;
	}


	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}

	/**
	 * @see wicket.markup.html.form.AbstractSingleSelectChoice#getDefaultChoice(java.lang.Object)
	 */
	@Override
	protected CharSequence getDefaultChoice(Object selected)
	{
		return ""; //$NON-NLS-1$
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
			requestFocus();
			if (tmpForeground == NO_COLOR)
			{
				tmpForeground = scriptable.js_getFgcolor();
				scriptable.js_setFgcolor("red"); //$NON-NLS-1$
			}
		}
		else
		{
			previousValidValue = null;
			if (tmpForeground != NO_COLOR)
			{
				scriptable.js_setFgcolor(tmpForeground);
				tmpForeground = NO_COLOR;
			}
		}
	}

	public void notifyLastNewValueWasChange(final Object oldVal, final Object newVal)
	{
		if (eventExecutor.hasChangeCmd() || eventExecutor.hasActionCmd())
		{
			ServoyForm form = findParent(ServoyForm.class);
			form.addDelayedAction(new ServoyForm.IDelayedAction()
			{
				public void execute()
				{
					Object value = oldVal;
					if (previousValidValue != null) value = oldVal;

					eventExecutor.fireChangeCommand(value, newVal, false, WebDataRadioChoice.this);

					//if change cmd is not succeeded also don't call action cmd?
					if (isValueValid)
					{
						eventExecutor.fireActionCommand(false, WebDataRadioChoice.this);
					}

				}

				public Component getComponent()
				{
					return WebDataRadioChoice.this;
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

	//_____________________________________________________________

	@Override
	protected void onRender(final MarkupStream markupStream)
	{
		super.onRender(markupStream);
		getStylePropertyChanges().setRendered();
		IModel model = getInnermostModel();
		if (model instanceof RecordItemModel)
		{
			((RecordItemModel)model).updateRenderedValue(this);
		}
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		if (useAJAX)
		{
			Object oe = scriptable.js_getClientProperty("ajax.enabled"); //$NON-NLS-1$
			if (oe != null) useAJAX = Utils.getAsBoolean(oe);
		}
		if (!useAJAX)
		{
			Form f = getForm();
			if (f != null)
			{
				if (eventExecutor.hasRightClickCmd())
				{
					CharSequence urlr = urlFor(IRightClickListener.INTERFACE);
					// We need a "return false;" so that the context menu is not displayed in the browser.
					tag.put("oncontextmenu", f.getJsForInterfaceUrl(urlr) + " return false;"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	/**
	 * @see org.apache.wicket.markup.html.form.FormComponent#updateModel()
	 */
	@Override
	public void updateModel()
	{
		boolean b = getStylePropertyChanges().isChanged();
		super.updateModel();
		// if before updating the model the changed flag was false make sure it stays that way.
		if (!b)
		{
			getStylePropertyChanges().setRendered();
		}
	}

	/**
	 * @see wicket.markup.html.form.FormComponent#getInputName()
	 */
	@Override
	public String getInputName()
	{
		if (inputId == null)
		{
			Page page = findPage();
			if (page instanceof MainPage)
			{
				inputId = ((MainPage)page).nextInputNameId();
			}
			else
			{
				return super.getInputName();
			}
		}
		return inputId;
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
		this.maxLength = maxLength;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	/**
	 * @return the margin
	 */
	public Insets getMargin()
	{
		return margin;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setCursor(java.awt.Cursor)
	 */
	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		//ignore.. pull models
		return null;
	}

	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
		if (getStylePropertyChanges().isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEditListner()
	 */
	public boolean needEditListner()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public String toString()
	{
		return scriptable.js_getElementType() +
			"(web)[name:" + scriptable.js_getName() + ",x:" + scriptable.js_getLocationX() + ",y:" + scriptable.js_getLocationY() + ",width:" + scriptable.js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + scriptable.js_getHeight() + ",value:" + getDefaultModelObjectAsString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	/*
	 * _____________________________________________________________ Methods for IDisplayRelatedData
	 */
	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		list.fill(state);
		getStylePropertyChanges().setChanged();
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
			requestFocus();
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

	public void setHorizontalScrollBarPolicy(int policy)
	{

	}

	public void setVerticalScrollBarPolicy(int policy)
	{
		this.vScrollPolicy = policy;
		updatePrefix();
	}

	private void updatePrefix()
	{
		StringBuffer prefix = new StringBuffer();
		prefix.append("<div"); //$NON-NLS-1$
		if (tabIndex != -1) prefix.append(" tabindex='").append(tabIndex).append("'"); //$NON-NLS-1$//$NON-NLS-2$
		prefix.append(" class='"); //$NON-NLS-1$
		if (vScrollPolicy == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) prefix.append("inl"); //$NON-NLS-1$
		else prefix.append("blk"); //$NON-NLS-1$
		prefix.append("'"); //$NON-NLS-1$
		prefix.append(">"); //$NON-NLS-1$
		setPrefix(prefix.toString());
		setSuffix("</div>"); //$NON-NLS-1$
	}


	public void requestFocus(Object[] vargs)
	{
		if (vargs != null && vargs.length >= 1 && !Utils.getAsBoolean(vargs[0]))
		{
			eventExecutor.skipNextFocusGain();
		}
		requestFocus();
	}

	public void requestFocus()
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
		return vl;
	}

	public ListDataListener getListener()
	{
		return null;
	}

	public String js_getValueListName()
	{
		if (list != null)
		{
			return list.getName();
		}
		return null;
	}

	public void setValueList(IValueList vl)
	{
		this.vl = vl;
		list.register(vl);
		getStylePropertyChanges().setChanged();

	}

	/*
	 * format---------------------------------------------------
	 */
	public void setFormat(int type, String format)
	{
		this.dataType = type;
		this.format = format;
	}

	public String getFormat()
	{
		return format;
	}

	private int dataType;
	private String format;

	public int getDataType()
	{
		return dataType;
	}

	private boolean editState;

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

	public void setEditable(boolean b)
	{
		editState = b;
		setEnabled(b);
	}

	public boolean isEditable()
	{
		return !isReadOnly();
	}

	public boolean isReadOnly()
	{
		return !isEnabled();
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
		list.setDataProviderID(dataProviderID);
	}

	private String dataProviderID;

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setName(String n)
	{
		name = n;
	}

	private String name;

	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

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
	public String getToolTipText()
	{
		if (tooltip != null && getInnermostModel() instanceof RecordItemModel)
		{
			return Text.processTags(tooltip, resolver);
		}
		return tooltip;
	}

	/*
	 * font---------------------------------------------------
	 */
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	public Font getFont()
	{
		return font;
	}


	private Color background;

	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	public Color getBackground()
	{
		return background;
	}


	private Color foreground;

	private List<ILabel> labels;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	public Color getForeground()
	{
		return foreground;
	}

	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				label.setComponentVisible(visible);
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

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			getStylePropertyChanges().setChanged();
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = labels.get(i);
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
		this.viewable = b;
		setComponentVisible(b);
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

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public Point getLocation()
	{
		return location;
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

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


	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
	}

	public void onRightClick()
	{
		Form f = getForm();
		if (f != null)
		{
			// If form validation fails, we don't execute the method.
			if (f.process()) eventExecutor.onEvent(JSEvent.EventType.rightClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
	}

	public void handleOwnTabIndex(int newTabIndex)
	{
		this.tabIndex = newTabIndex;
		updatePrefix();
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		if (eventExecutor != null)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			eventExecutor.fireOnRender(this, isFocused);
		}
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getRenderEventExecutor()
	 */
	public RenderEventExecutor getRenderEventExecutor()
	{
		return eventExecutor;
	}
}
