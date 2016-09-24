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
import java.util.Locale;

import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListDataListener;
import javax.swing.text.Document;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.INullableAware;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
@SuppressWarnings("nls")
public abstract class WebBaseSelectBox extends MarkupContainer implements IFieldComponent, IDisplayData, IProviderStylePropertyChanges, INullableAware,
	ISupportWebBounds, IRightClickListener, ISupplyFocusChildren<Component>, ISupportValueList, ISupportSimulateBoundsProvider, ISupportOnRender
{
	protected static final long serialVersionUID = 1L;
	protected static final String NO_COLOR = "NO_COLOR";

	protected IValueList onValue;
	protected final WebEventExecutor eventExecutor;

	protected Cursor cursor;
	protected boolean needEntireState;
	protected int maxLength;
	protected Insets margin;
	protected int horizontalAlignment;
	protected Point loc;
	protected String inputId;
	protected boolean allowNull = true;
	protected String tmpForeground = NO_COLOR;

	protected final IApplication application;
	protected final FormComponent< ? > selector;
	protected final AbstractRuntimeField<IFieldComponent> scriptable;

	public WebBaseSelectBox(final IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, String id, String text)
	{
		super(id);
		this.scriptable = scriptable;
		this.application = application;
		selector = getSelector("check_" + id); //$NON-NLS-1$

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX"));
		eventExecutor = new WebEventExecutor(selector, useAJAX);
		setOutputMarkupPlaceholderTag(true);

		add(selector);
		Label selectLabel = new Label("text_" + id, "")
		{
			@Override
			protected void onComponentTag(ComponentTag tag)
			{
				super.onComponentTag(tag);
				if (tag.getName().compareToIgnoreCase("label") == 0) //$NON-NLS-1$
				{
					tag.put("for", selector.getMarkupId()); //$NON-NLS-1$
				}
			}

			@Override
			protected void onBeforeRender()
			{
				super.onBeforeRender();
				String txt = getDefaultModelObjectAsString();
				if (HtmlUtils.startsWithHtml(txt))
				{
					txt = StripHTMLTagsConverter.convertBodyText(this, txt, WebBaseSelectBox.this.scriptable.trustDataAsHtml(),
						application.getFlattenedSolution()).getBodyTxt().toString();
					setDefaultModelObject(txt);
				}
			}

		};
		selectLabel.setOutputMarkupId(true);
		add(selectLabel);
		setText(Text.processTags(text, null));

		selector.add(new FocusIfInvalidAttributeModifier(selector));
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
	}

	protected abstract FormComponent< ? > getSelector(String id);

	private String text;

	public void setText(String txt)
	{
		this.text = txt;
		Component c = get("text_" + getId()); //$NON-NLS-1$
		if (txt == null || txt.trim().length() == 0)
		{
			c.setVisible(false);
		}
		else
		{
			c.setDefaultModelObject(txt);
			c.setVisible(true);
			c.setEscapeModelStrings(false);
		}
	}

	public String getText()
	{
		return text;
	}

	/**
	 * @see org.apache.wicket.Component#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return application.getLocale();
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
		//we cannot add focus in web on checkbox, onAction runs at same time as onfocus, making onfocus useless, and giving unprdicted behaviour
		//eventExecutor.setEnterCmds(ids);
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
				scriptable.setFgcolor("red");
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

	public Component[] getFocusChildren()
	{
		Component component = null;
		if (onValue != null) component = this;
		else component = selector;
		return new Component[] { component };
	}

	public void notifyLastNewValueWasChange(final Object oldVal, final Object newVal)
	{
		if (eventExecutor.hasChangeCmd())
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					WebEventExecutor.setSelectedIndex(WebBaseSelectBox.this, null, IEventExecutor.MODIFIERS_UNSPECIFIED);
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebBaseSelectBox.this);
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

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;

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
		eventExecutor.setValidationEnabled(b);
		getStylePropertyChanges().setChanged();
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	/**
	 * @see wicket.markup.html.WebMarkupContainer#onRender()
	 */
	@Override
	protected void onRender(final MarkupStream markupStream)
	{
		super.onRender(markupStream);

		getStylePropertyChanges().setRendered();
		IModel< ? > model = getInnermostModel();

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
			Object oe = scriptable.getClientProperty("ajax.enabled");
			if (oe != null) useAJAX = Utils.getAsBoolean(oe);
		}
		if (!useAJAX)
		{
			Form< ? > f = getForm();
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

	protected boolean editable;
	protected boolean editState;

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
	}

	public boolean isEditable()
	{
		return !isReadOnly();
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
		return getDefaultModelObject();
	}

	public void setAllowNull(boolean allowNull)
	{
		this.allowNull = allowNull;
	}

	public boolean getAllowNull()
	{
		return allowNull;
	}

	public boolean needEditListener()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
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

	public void requestFocusToComponent()
	{
		// is the current container always the right one...
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(selector);
		}
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
	protected Border border;

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

	protected boolean opaque;

	// Searches for a parent form, up the hierarchy of controls in the page.
	protected Form< ? > getForm()
	{
		Component c = this;
		while ((c != null) && !(c instanceof Form))
			c = c.getParent();
		return (Form< ? >)c;
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		fireOnRender(false);
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

	public IValueList getValueList()
	{
		return onValue;
	}

	public ListDataListener getListener()
	{
		return null;
	}

	public void setValueList(IValueList vl)
	{
		this.onValue = vl;
		getStylePropertyChanges().setChanged();

	}

	/*
	 * readonly---------------------------------------------------
	 */

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

	public boolean isReadOnly()
	{
		return !editable;
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	protected String dataProviderID;

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public boolean isOpaque()
	{
		return opaque;
	}

	private String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
		setText(Text.processTags(title, resolver));
	}

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	private String tooltip;

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
		if (viewable || !visible)
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
		if (accessible || !b)
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
		if (size != null)
		{
			Dimension d = ((ChangesRecorder)scriptable.getChangesRecorder()).calculateWebSize(size.width, size.height, border, margin, 0, null, true,
				SwingConstants.CENTER);
			return new Rectangle(location, d);
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)scriptable.getChangesRecorder()).getPaddingAndBorder(size == null ? 0 : size.height, border, margin, 0, null, true,
			SwingConstants.CENTER);
	}


	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
		add(new ServoyAjaxEventBehavior("oncontextmenu", "SelectBox", true) //$NON-NLS-1$
		{
			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				eventExecutor.onEvent(JSEvent.EventType.rightClick, target, WebBaseSelectBox.this,
					Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)));
			}

			@Override
			public boolean isEnabled(Component component)
			{
				if (super.isEnabled(component))
				{
					Object oe = WebBaseSelectBox.this.scriptable.getClientProperty("ajax.enabled");
					if (oe != null) return Utils.getAsBoolean(oe);
					return true;
				}
				return false;
			}

			// We need to return false, otherwise the context menu of the browser is displayed.
			@Override
			protected IAjaxCallDecorator getAjaxCallDecorator()
			{
				return new AjaxCallDecorator()
				{
					@Override
					public CharSequence decorateScript(CharSequence script)
					{
						return script + " return false;";
					}
				};
			}
		});
	}

	public void onRightClick()
	{
		Form< ? > f = getForm();
		if (f != null)
		{
			// If form validation fails, we don't execute the method.
			if (f.process()) eventExecutor.onEvent(JSEvent.EventType.rightClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}

	interface ISelector
	{
		public WebBaseSelectBox getSelectBox();
	}
}