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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.Component;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;
import org.wicketstuff.calendar.markup.html.form.DatePicker;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptDataCalendarMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Utils;

/**
 * Represents a Calendar component in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebDataCalendar extends WebMarkupContainer implements IFieldComponent, IDisplayData, IScriptDataCalendarMethods, IDelegate, ISupportWebBounds,
	IRightClickListener, IProviderStylePropertyChanges, ISupplyFocusChildren<Component>
{
	private static final long serialVersionUID = 1L;

	private final DateField field;
	private Cursor cursor;
	private final IApplication application;
	private final ChangesRecorder jsChangeRecorder;
	private boolean readOnly = false;
	private boolean showPicker = true;
	private boolean editable;
	private Insets margin;

	/**
	 * @param id
	 */
	public WebDataCalendar(IApplication application, String id)
	{
		super(id);
		this.application = application;
		DatePicker settings = new DatePicker();
		field = new DateField(application, "datefield", settings);
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		if (useAJAX)
		{
			final FeedSimpleDateFormatToChooserBehavior feedSimpleDateToJS;
			field.add(feedSimpleDateToJS = new FeedSimpleDateFormatToChooserBehavior());
			field.add(new DatePicker()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onRendered(Component component)
				{
					Response response = component.getResponse();
					response.write("</td><td style = \"margin: 0px; padding: 0px; width: 5px;\">&nbsp</td><td style = \"margin: 0px; padding: 0px; width: 16px;\">");
					response.write("\n<img style=\"");
					response.write(getIconStyle());
					response.write("\" id=\"");
					response.write(getIconId());
					response.write("\" src=\"");
					CharSequence iconUrl = getIconUrl();
					response.write(Strings.escapeMarkup(iconUrl == null ? "" : iconUrl.toString()));
					response.write("\" onclick=\"if (!isValidationFailed('" + WebDataCalendar.this.getMarkupId() + "'))	wicketAjaxGet('" +
						feedSimpleDateToJS.getCallbackUrl() + "&currentDateValue='+wicketEncode(document.getElementById('");
					response.write(component.getMarkupId());
					response.write("').value), null, function() { onAjaxError(); }.bind(this), function() { return Wicket.$(this.id) != null; }.bind(this));\"/>");
				}

				@Override
				public boolean isEnabled(Component component)
				{
					return isChooserEnabled();
				}

			});
		}
		else
		{
			field.add(new DatePicker()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onRendered(Component component)
				{
					Response response = component.getResponse();
					response.write("</td><td style = \"margin: 0px; padding: 0px; width: 5px;\">&nbsp</td><td style = \"margin: 0px; padding: 0px; width: 16px;\">");
					response.write("\n<img style=\"");
					response.write(getIconStyle());
					response.write("\" id=\"");
					response.write(getIconId());
					response.write("\" src=\"");
					CharSequence iconUrl = getIconUrl();
					response.write(Strings.escapeMarkup(iconUrl == null ? "" : iconUrl.toString()));
					response.write("\" onclick=\"if (!isValidationFailed('" + WebDataCalendar.this.getMarkupId() +
						"'))displayCalendar(document.getElementById('");
					response.write(component.getMarkupId());
					response.write("'),'");
					String datePattern = getDatePattern().replaceAll("mm", "ii").toLowerCase();
					datePattern = datePattern.replace('s', '0');
					response.write(datePattern);
					if (datePattern.indexOf("h") == -1) response.write("',this,false,'" + component.getMarkupId() + "',true)\"");
					else response.write("',this,true,null,true)\"");
					response.write(" />");
				}

				@Override
				public boolean isEnabled(Component component)
				{
					return WebDataCalendar.this.isEnabled() && showPicker;
				}
			});
		}
		add(field);
		jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING)
		{
			@Override
			public boolean isChanged()
			{
				// because the DataPicker behavior will add a tag to the end of the field component
				// each time that component is rendered, we must make sure that we render the whole container;
				// otherwise, each independent render of the field component will add one more div tag with the calendar popup image
				// to the HTML tag of this container
				return super.isChanged() || field.getStylePropertyChanges().isChanged();
			}

			@Override
			public void setRendered()
			{
				super.setRendered();
				field.getStylePropertyChanges().setRendered();
			}
		};

		setOutputMarkupPlaceholderTag(true);

		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
	}

	protected boolean isChooserEnabled()
	{
		return isEnabled() && showPicker;
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { field };
	}

	/**
	 * @see org.apache.wicket.Component#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return application.getLocale();
	}

	/**
	 * @see wicket.MarkupContainer#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
	}

	@Override
	protected void onRemove()
	{
		MainPage mp = (MainPage)findPage();
		if (mp != null)
		{
			// the calendar was removed
			mp.getPageContributor().addDynamicJavaScript("closeCalendar('" + field.getMarkupId() + "');");
		}
		super.onRemove();
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}

	public Object getDelegate()
	{
		return field;
	}

	public Document getDocument()
	{
		return field.getDocument();
	}

	public void setMargin(Insets i)
	{
		this.margin = i;
	}

	public void addScriptExecuter(IScriptExecuter el)
	{
		field.addScriptExecuter(el);
	}

	public IEventExecutor getEventExecutor()
	{
		return field.getEventExecutor();
	}

	public void setEnterCmds(String[] ids, Object[][] args)
	{
		field.setEnterCmds(ids, null);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		field.setLeaveCmds(ids, null);
	}

	public void setActionCmd(String id, Object[] args)
	{
		field.setActionCmd(id, args);
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		field.notifyLastNewValueWasChange(oldVal, newVal);
	}

	public boolean isValueValid()
	{
		return field.isValueValid();
	}

	public void setValueValid(boolean valid, Object oldVal)
	{
		field.setValueValid(valid, oldVal);
	}

	public void setChangeCmd(String id, Object[] args)
	{
		field.setChangeCmd(id, args);
	}

	public void setHorizontalAlignment(int a)
	{
		field.setHorizontalAlignment(a);
	}

	public void setMaxLength(int i)
	{
		field.setMaxLength(i);
	}

	public void addEditListener(IEditListener l)
	{
		if (field != null) field.addEditListener(l);
	}

	public void setValueObject(Object obj)
	{
		field.setValueObject(obj);
	}

	public Object getValueObject()
	{
		return field.getValue();
	}

	public boolean needEditListner()
	{
		return true;
	}

	public boolean needEntireState()
	{
		return field.needEntireState();
	}

	public void setNeedEntireState(boolean b)
	{
		field.setNeedEntireState(b);
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValidationEnabled(boolean b)
	{
		field.setValidationEnabled(b);
		if (b)
		{
			if (showPicker && readOnly)
			{
				showPicker = false;
				jsChangeRecorder.setChanged();
			}
		}
		else
		{
			showPicker = true;
		}
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (field != null) return field.stopUIEditing(looseFocus);
		return true;
	}

	public void setSelectOnEnter(boolean b)
	{
		if (field != null) field.setSelectOnEnter(b);
	}

	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getDefaultModelObjectAsString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	class DateField extends WebDataField implements ITextFormatProvider
	{
		private static final long serialVersionUID = 1L;

		private final DatePicker settings;

		/**
		 * @param id
		 */
		public DateField(IApplication application, String id, DatePicker settings)
		{
			super(application, id);
			this.settings = settings;
		}

		// When the calendar field is neither editable nor read-only, we want the text field to be not editable, but
		// we want to let the user change the date using the calendar popup.
		// In this case we need the read only and filter backspace behaviors of the text field to work normally (they are enabled based
		// on the "editable" member - which is set from the calendar field), but we also need the normal onChange behavior to be enabled - so as the data is updated on the server even if the text field itself is read-only.
		// Because the onChange uses accessor methods, if we overwrite those to always return editable = true and read-only = false, we should
		// get the expected behavior.
		@Override
		public boolean isReadOnly()
		{
			return false;
		}

		@Override
		public boolean js_isReadOnly()
		{
			return false;
		}

		@Override
		public boolean js_isEditable()
		{
			return true;
		}

		public DatePicker getDatePickerSettings()
		{
			return settings;
		}

		/**
		 * @see wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider#getTextFormat()
		 */
		public String getTextFormat()
		{
			return parsedFormat.getDisplayFormat() != null ? parsedFormat.getDisplayFormat() : application.getSettings().getProperty("locale.dateformat", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		/**
		 * @see com.servoy.j2db.server.headlessclient.dataui.WebDataField#getMarkupId()
		 */
		@Override
		public String getMarkupId()
		{
			return WebDataCalendar.this.getMarkupId() + "datefield";
		}

	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public void js_requestFocus(Object[] vargs)
	{
		field.js_requestFocus(vargs);
	}

	/*
	 * dataprovider---------------------------------------------------
	 */
	public String js_getDataProviderID()
	{
		return getDataProviderID();
	}

	public String getDataProviderID()
	{
		return field.getDataProviderID();
	}

	public void setDataProviderID(String id)
	{
		field.setDataProviderID(id);
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "CALENDAR";
	}

	/*
	 * format---------------------------------------------------
	 */
	public void setFormat(int type, String format)
	{
		if (format != null) field.setFormat(type, format);
	}

	public String getFormat()
	{
		return field.getFormat();
	}

	public void js_setFormat(String format)
	{
		field.setFormat(field.getDataType(), application.getI18NMessageIfPrefixed(format));
	}

	public int getDataType()
	{
		return field.getDataType();
	}

	public String js_getFormat()
	{
		return field.getFormat();
	}


	/*
	 * readonly/editable---------------------------------------------------
	 */
	public boolean js_isEditable()
	{
		return editable;
	}

	public void js_setEditable(boolean b)
	{
		setEditable(b);
	}

	public void setEditable(boolean b)
	{
		field.setEditable(b);
		editable = b;
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	public void js_setReadOnly(boolean b)
	{
		if (readOnly != b)
		{
			readOnly = b;
			showPicker = !b;
			field.js_setReadOnly(b);
			jsChangeRecorder.setChanged();
		}
	}

	public boolean isReadOnly()
	{
		return !showPicker;
	}


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
		field.setName(n);
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

	public void setTitleText(String title)
	{
		field.setTitleText(title);
	}

	public String js_getTitleText()
	{
		return field.js_getTitleText();
	}

	/*
	 * tooltip---------------------------------------------------
	 */

	public void setToolTipText(String tip)
	{
		field.setToolTipText(tip);
	}

	public void js_setToolTipText(String txt)
	{
		setToolTipText(txt);
		jsChangeRecorder.setChanged();
	}

	public String js_getToolTipText()
	{
		return field.js_getToolTipText();
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return field.getToolTipText();
	}

	/*
	 * font---------------------------------------------------
	 */
	private Font font;

	public void js_setFont(String spec)
	{
		font = PersistHelper.createFont(spec);
		jsChangeRecorder.setFont(spec);
	}

	public String js_getFont()
	{
		return PersistHelper.createFontString(font);
	}

	public Font getFont()
	{
		return font;
	}

	public void setFont(Font f)
	{
		if (f != null && field != null) field.setFont(f);
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

	private ArrayList<ILabel> labels;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
		if (field != null) field.setForeground(cfg);
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

	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getBorder());
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
			ArrayList<String> al = new ArrayList<String>(labels.size());
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
			field.setEnabled(b);
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
		if (IApplication.DATE_FORMATTERS_LENIENT.equals(key))
		{
			field.js_putClientProperty(key, value);
		}
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
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, null, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, null, 0, null);
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
		field.setRightClickCommand(rightClickCmd, args);
	}

	public void onRightClick()
	{
		field.onRightClick();
	}

	/**
	 * Behavior that is meant to extend the formats calendar fields can work with in web client. Needed because all smart client knows is not supported by the JS DHTMLgoodies widget we
	 * are using in WC.<BR>
	 * The idea is to feed a simple date format to the calendar widget, and to receive back the same simple format. And this behavior is a proxy between the widget and the field.<br>
	 * So the sequence is: user pushes calendar button in browser, this behavior receives a request containing the (maybe complicated formatted) value of the calendar field, it translates
	 * it to something simple, sends it back to the browser and opens the calendar using it, user chooses a date in the calendar field, a request comes back to this behavior with the chosen date
	 * (encoded with the same simple format), then we translate this to the calendar field's format (that may be complicated) and send it back to populate the text field in the browser as the widget would.
	 * 
	 * @author acostescu
	 */
	private class FeedSimpleDateFormatToChooserBehavior extends AbstractServoyDefaultAjaxBehavior
	{

		private static final String simpleFormatAsSeenBySmartClient = "dd/MM/yyyy HH:mm";
		private static final String simpleFormatAsSeenByCalendarWidget = "dd/mm/yyyy hh:ii";
		private final StateFullSimpleDateFormat simpleFormatConverter = new StateFullSimpleDateFormat(simpleFormatAsSeenBySmartClient, null, getLocale(), true);

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			String dateValue = getRequestCycle().getRequest().getParameter("currentDateValue"); //$NON-NLS-1$
			if (dateValue != null)
			{
				IConverter fieldConverter = field.getConverter(null); // this is the converter that is using this calendar field's format; we use it to get the Date object
				// and then we convert that date back to a String using a simple format that we know the JS calendar chooser is aware of (this way we can use all formats available in
				// smart client for web client calendar fields as well)

				Date d = null;
				try
				{
					Object dObj = fieldConverter.convertToObject(dateValue, getLocale());
					if (dObj instanceof Date) d = (Date)dObj;
				}
				catch (ConversionException e)
				{
					// so it's probably in the process of being edited / not valid => calendar widget will not be given the initial date it should show
				}

				if (d != null)
				{
					// convert it back to simple String representation that can be altered using calendar widget
					dateValue = simpleFormatConverter.format(d);
				}
				else
				{
					dateValue = "";
				}

				// trigger the opening of the calendar widget
				StringBuffer sb = new StringBuffer(150);
				sb.append("displayCalendar(new CalendarTextFieldPlaceholder('");
				sb.append(dateValue);
				sb.append("', '");
				sb.append(getCallbackUrl());
				sb.append("', '");
				sb.append(field.getMarkupId());
				sb.append("'), '");
				sb.append(simpleFormatAsSeenByCalendarWidget);
				sb.append("', document.getElementById('");
				sb.append(field.getMarkupId());
				if (field.getTextFormat().indexOf("h") == -1 && field.getTextFormat().indexOf("H") == -1) sb.append("'),false,'" + field.getMarkupId() +
					"',true);");
				else sb.append("'),true,null,true);");
				target.appendJavascript(sb.toString());
			}
			else
			{
				dateValue = getRequestCycle().getRequest().getParameter("newDateValue"); //$NON-NLS-1$

				IConverter fieldConverter = field.getConverter(null); // this is the converter that is using this calendar field's format; we use it to transform the date back into
				// the calendar field's format

				try
				{
					Date d = simpleFormatConverter.parse(dateValue);

					// convert it back to simple String representation that can be altered using calendar widget
					dateValue = fieldConverter.convertToString(d, getLocale());

					// trigger the opening of the calendar widget
					StringBuffer sb = new StringBuffer(100);
					sb.append("var calendarTextField = document.getElementById('");
					sb.append(field.getMarkupId());
					sb.append("'); calendarTextField.value = '");
					sb.append(dateValue);
					sb.append("'; calendarTextField.onchange();");
					target.appendJavascript(sb.toString());
				}
				catch (ConversionException e)
				{
					// shouldn't happen
					Debug.error(e);
				}
				catch (ParseException e)
				{
					// shouldn't happen as calendar should give correctly formatted date
					Debug.error(e);
				}
			}
		}

		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);

			// create & add to header the CalendarTextFieldPlaceholder javascript
			StringBuffer sb = new StringBuffer(100);
			sb.append("function CalendarTextFieldPlaceholder(value, callBackURL, dateFieldId) {\n");
			sb.append("  this.callBackURL = callBackURL;\n");
			sb.append("  this.value = value;\n");
			sb.append("  this.dateFieldId = dateFieldId;\n");
			sb.append("}\n");

			sb.append("function CalendarTextFieldPlaceholder_onchange() {\n");
			sb.append("  var textField = document.getElementById(this.dateFieldId);\n");
			sb.append("  wicketAjaxGet(this.callBackURL + '&newDateValue=' + wicketEncode(this.value), null, function() { onAjaxError(); }.bind(textField), function() { return Wicket.$(textField.id) != null; }.bind(textField));\n");
			sb.append("}\n");
			sb.append("CalendarTextFieldPlaceholder.prototype = new Object();\n");
			sb.append("CalendarTextFieldPlaceholder.prototype.onchange = CalendarTextFieldPlaceholder_onchange;");
			response.renderJavascript(sb.toString(), "CalendarTextFieldPlaceholderID"); //$NON-NLS-1$ 
		}

		@Override
		public boolean isEnabled(Component component)
		{
			return isChooserEnabled() && super.isEnabled(component);
		}

		@Override
		public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
		{
			return super.getCallbackUrl(true);
		}
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		RenderEventExecutor eventExecutor = getRenderEventExecutor();
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
		IEventExecutor eventExecutor = getEventExecutor();
		return eventExecutor instanceof RenderEventExecutor ? (RenderEventExecutor)eventExecutor : null;
	}

}
