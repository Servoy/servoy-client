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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
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
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptTextAreaMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 *  Represents a textarea field in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebDataTextArea extends TextArea implements IFieldComponent, IDisplayData, IScriptTextAreaMethods, IProviderStylePropertyChanges,
	ISupportWebBounds, IRightClickListener
{
	private static final long serialVersionUID = 1L;

	private boolean selectOnEnter;
	private Cursor cursor;
	private boolean needEntireState;
	private Insets margin;
	private int horizontalAlignment;
	private String inputId;
	private final WebEventExecutor eventExecutor;

	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING);

	private final IApplication application;

	private SimpleAttributeModifier maxLengthBehavior;

	/**
	 * @param id
	 */
	public WebDataTextArea(IApplication application, String id)
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
		return jsChangeRecorder;
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
		jsChangeRecorder.setRendered();
		IModel model = getInnermostModel();
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
			Object oe = js_getClientProperty("ajax.enabled");
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
		this.selectOnEnter = selectOnEnter;
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
				"onkeyup", "Servoy.Validation.imposeMaxLength(this, " + +maxLength + ");"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			add(maxLengthBehavior);
		}
	}

	public void setFormat(int type, String format)
	{
		this.dataType = type;
		this.format = format;
	}

	private int dataType;
	private String format;

	public String getFormat()
	{
		return format;
	}

	public int getDataType()
	{
		return dataType;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		return getDefaultModelObjectAsString();
	}

	public void setValueObject(Object value)
	{
		jsChangeRecorder.testChanged(this, value);
	}

	public boolean needEditListner()
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
			ServoyForm form = findParent(ServoyForm.class);
			form.addDelayedAction(new ServoyForm.IDelayedAction()
			{
				public void execute()
				{
					Object value = oldVal;
					if (previousValidValue != null) value = oldVal;

					eventExecutor.fireChangeCommand(value, newVal, false, WebDataTextArea.this);
				}

				public Component getComponent()
				{
					return WebDataTextArea.this;
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
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;

		eventExecutor.setValidationEnabled(b);

		boolean prevEditState = editState;
		if (b)
		{
			setEditable(wasEditable);
		}
		else
		{
			wasEditable = editable;
			setEditable(true);//allow search
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
	public void js_requestFocus(Object[] vargs)
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

	public void js_setCaretPosition(int pos)
	{
		// TODO ignore for the web??
	}

	public String js_getSelectedText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "TEXT_AREA"; //$NON-NLS-1$
	}

	public void js_selectAll()
	{
		// TODO Auto-generated method stub
	}

	public void js_replaceSelectedText(String s)
	{
		// TODO ignore in web?
	}

	public int js_getCaretPosition()
	{
		// TODO ignore for the web??
		return -1;
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

	public boolean js_isEditable()
	{
		return editable;
	}

	public boolean isReadOnly()
	{
		return !editable;
	}

	public void js_setEditable(boolean editable)
	{
		this.editable = editable;
	}

	public boolean js_isReadOnly()
	{
		return !editable;
	}

	private boolean editState;

	public void js_setReadOnly(boolean b)
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
		jsChangeRecorder.setChanged();
	}


	/*
	 * scrolling---------------------------------------------------
	 */
	public int js_getScrollX()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int js_getScrollY()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public void js_setScroll(int x, int y)
	{
		// TODO Auto-generated method stub
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

	public String js_getDataProviderID()
	{
		return dataProviderID;
	}

	private String dataProviderID;


	/*
	 * name---------------------------------------------------
	 */
	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
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

	public boolean js_isTransparent()
	{
		return !opaque;
	}

	public void js_setTransparent(boolean b)
	{
		opaque = !b;
		jsChangeRecorder.setTransparent(b);
	}

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

	public String js_getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	/*
	 * tooltip---------------------------------------------------
	 */
	public String js_getToolTipText()
	{
		return tooltip;
	}

	private String tooltip;

	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			tooltip = null;
		}
		this.tooltip = tooltip;
	}

	public void js_setToolTipText(String tooltip)
	{
		setToolTipText(tooltip);
		jsChangeRecorder.setChanged();
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

	public void js_setFont(String spec)
	{
		font = PersistHelper.createFont(spec);
		jsChangeRecorder.setFont(spec);
	}

	public Font getFont()
	{
		return font;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(background);
	}

	public void js_setBgcolor(String bgcolor)
	{
		background = PersistHelper.createColor(bgcolor);
		jsChangeRecorder.setBgcolor(bgcolor);
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


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(foreground);
	}

	public void js_setFgcolor(String fgcolor)
	{
		foreground = PersistHelper.createColor(fgcolor);
		jsChangeRecorder.setFgcolor(fgcolor);
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

	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
		jsChangeRecorder.setBorder(spec);
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

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean visible)
	{
		setVisible(visible);
		jsChangeRecorder.setVisible(visible);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label instanceof IScriptBaseMethods)
				{
					((IScriptBaseMethods)label).js_setVisible(visible);
				}
				else
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

	public String[] js_getLabelForElementNames()
	{
		if (labels != null)
		{
			List<String> al = new ArrayList<String>(labels.size());
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX)) //$NON-NLS-1$
				{
					al.add(label.getName());
				}
			}
			return al.toArray(new String[al.size()]);
		}
		return new String[0];
	}


	/*
	 * enabled---------------------------------------------------
	 */
	public void js_setEnabled(final boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			jsChangeRecorder.setChanged();
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

	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}


	/*
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public void js_setLocation(int x, int y)
	{
		location = new Point(x, y);
		jsChangeRecorder.setLocation(x, y);
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
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	private Map<Object, Object> clientProperties;

	public Object js_getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	public Dimension getSize()
	{
		return size;
	}

	public void js_setSize(int width, int height)
	{
		size = new Dimension(width, height);
		jsChangeRecorder.setSize(width, height, border, margin, 0);
	}

	public Rectangle getWebBounds()
	{
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, margin, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, margin, 0, null);
	}

	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public int js_getWidth()
	{
		return size.width;
	}

	public int js_getHeight()
	{
		return size.height;
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

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getDefaultModelObjectAsString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
