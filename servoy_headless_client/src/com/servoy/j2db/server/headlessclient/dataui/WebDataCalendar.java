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

import java.text.ParseException;
import java.util.Date;

import org.apache.wicket.Component;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;
import org.wicketstuff.calendar.markup.html.form.DatePicker;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.scripting.RuntimeDataCalendar;
import com.servoy.j2db.ui.scripting.RuntimeDataField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Utils;

/**
 * Represents a Calendar component in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebDataCalendar extends WebDataCompositeTextField
{

	private static final long serialVersionUID = 1L;

	public WebDataCalendar(IApplication application, RuntimeDataCalendar scriptable, String id)
	{
		super(application, scriptable, id);

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
					return shouldShowExtraComponents();
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
					return shouldShowExtraComponents();
				}
			});
		}
	}

	@Override
	protected WebDataField createTextField(RuntimeDataField fieldScriptable)
	{
		DatePicker settings = new DatePicker();
		return new DateField(application, fieldScriptable, settings);
	}

	@Override
	protected void onRemove()
	{
		MainPage mp = (MainPage)findPage();
		if (mp != null)
		{
			// the calendar was removed
			mp.getPageContributor().addDynamicJavaScript("if(typeof calendarDiv !== 'undefined' && calendarDiv){closeCalendar();}"); //$NON-NLS-1$
		}
		super.onRemove();
	}

	class DateField extends AugmentedTextField implements ITextFormatProvider
	{
		private static final long serialVersionUID = 1L;

		private final DatePicker settings;

		public DateField(IApplication application, RuntimeDataField scriptable, DatePicker settings)
		{
			super(application, scriptable);
			this.settings = settings;
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
				if (((DateField)field).getTextFormat().indexOf("h") == -1 && ((DateField)field).getTextFormat().indexOf("H") == -1) sb.append("'),false,'" +
					field.getMarkupId() + "',true);");
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
					// make sure the the validation variables are set to blank (edit/display formats)
					sb.append("calendarTextField.editValue  = ''; calendarTextField.displayValue  = '';");
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
			return shouldShowExtraComponents() && super.isEnabled(component);
		}

		@Override
		public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
		{
			return super.getCallbackUrl(true);
		}
	}

}
