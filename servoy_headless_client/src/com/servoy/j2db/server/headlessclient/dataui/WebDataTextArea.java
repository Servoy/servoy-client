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

import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.string.Strings;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportInputSelection;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 *  Represents a textarea field in the webbrowser.
 * 
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class WebDataTextArea extends TextArea implements IFieldComponent, IDisplayData, IProviderStylePropertyChanges, ISupportWebBounds, IRightClickListener,
	ISupportInputSelection
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

	private FindModeDisabledSimpleAttributeModifier maxLengthBehavior;
	private final AbstractRuntimeField<IFieldComponent> scriptable;

	public WebDataTextArea(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, String id)
	{
		super(id);
		this.application = application;
		setVersioned(false);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);

		add(new AttributeModifier("readonly", true, new Model<String>() //$NON-NLS-1$
			{
				@Override
				public String getObject()
				{
					return (editable ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE : AttributeModifier.VALUELESS_ATTRIBUTE_ADD);
				}
			}));

		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
		add(new FilterBackspaceKeyAttributeModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1332637522687352873L;

			@Override
			public String getObject()
			{
				return editable ? null : FilterBackspaceKeyAttributeModifier.SCRIPT;
			}
		}));
		this.scriptable = scriptable;
	}

	public final AbstractRuntimeField<IFieldComponent> getScriptObject()
	{
		return scriptable;
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

	/**
	 * @see wicket.Component#getComparator()
	 */
	@Override
	public IModelComparator getModelComparator()
	{
		return ComponentValueComparator.COMPARATOR;
	}

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
	protected void onComponentTag(final ComponentTag tag)
	{
		super.onComponentTag(tag);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		if (useAJAX)
		{
			Object oe = scriptable.js_getClientProperty("ajax.enabled");
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

	@Override
	public void renderHead(final HtmlHeaderContainer container)
	{
		super.renderHead(container);
		container.getHeaderResponse().renderOnDomReadyJavascript(
			"$(function(){$(\"#" + getMarkupId() + "\").numpadDecSeparator({useRegionalSettings: true});});"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected Object convertValue(String[] value) throws ConversionException
	{
		String tmp = value != null && value.length > 0 ? value[0] : null;
		if (getConvertEmptyInputStringToNull() && Strings.isEmpty(tmp))
		{
			return null;
		}
		return tmp;
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
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	public void setMaxLength(int maxLength)
	{
		if (maxLengthBehavior != null) remove(maxLengthBehavior);
		if (maxLength > 0)
		{
			maxLengthBehavior = new FindModeDisabledSimpleAttributeModifier(getEventExecutor(),
				"onkeyup", ("Servoy.Validation.imposeMaxLength(this, " + +maxLength + ");").intern()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			add(maxLengthBehavior);
		}
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

	public void setCursor(Cursor cursor)
	{
//		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		return getDefaultModelObjectAsString();
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
			requestFocus();
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
					WebEventExecutor.setSelectedIndex(WebDataTextArea.this, null, IEventExecutor.MODIFIERS_UNSPECIFIED);

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
			if (!Boolean.TRUE.equals(application.getUIProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
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
			requestFocus();
			return false;
		}
		return true;
	}


	/*
	 * jsmethods---------------------------------------------------
	 */
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
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(this);
		}
	}

	public void selectAll()
	{
		Page page = findPage();
		if (page instanceof MainPage)
		{
			((MainPage)page).getPageContributor().addDynamicJavaScript("document.getElementById('" + getMarkupId() + "').select();");
		}
	}

	public void replaceSelectedText(String s)
	{
		Page page = findPage();
		if (page instanceof MainPage)
		{
			((MainPage)page).getPageContributor().addDynamicJavaScript("Servoy.Utils.replaceSelectedText('" + getMarkupId() + "','" + s + "');");
		}
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

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

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

	public List<ILabel> getLabelForElementNames()
	{
		return labels;
	}


	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			((ChangesRecorder)getStylePropertyChanges()).setChanged();
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
		Form< ? > f = getForm();
		if (f != null)
		{
			// If form validation fails, we don't execute the method.
			if (f.process()) eventExecutor.onEvent(JSEvent.EventType.rightClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
	}

	@Override
	public String toString()
	{
		return scriptable.toString("value:" + getDefaultModelObjectAsString()); //$NON-NLS-1$ 
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		if (scriptable != null)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			scriptable.getRenderEventExecutor().fireOnRender(isFocused);
		}
	}
}
