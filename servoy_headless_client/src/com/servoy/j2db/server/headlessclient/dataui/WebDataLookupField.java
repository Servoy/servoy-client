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

import java.awt.Font;
import java.awt.Insets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.request.WebClientInfo;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.GlobalMethodValueList;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.ui.scripting.RuntimeDataLookupField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;

/**
 * Represents the typeahead/lookup field in the browser.
 * 
 * @author jcompagner
 */
public class WebDataLookupField extends WebDataField implements IDisplayRelatedData
{

	private static final long serialVersionUID = 1L;
	IRecordInternal parentState;
	private LookupListModel dlm;

	/**
	 * @param application
	 * @param id
	 * @param list
	 */
	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, LookupValueList list)
	{
		super(application, scriptable, id, list);
		dlm = new LookupListModel(application, list);
		init();
	}

	/**
	 * @param application
	 * @param id
	 * @param list
	 */
	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, final String serverName, String tableName,
		String dataProviderID)
	{
		super(application, scriptable, id);
		dlm = new LookupListModel(application, serverName, tableName, dataProviderID);
		init();
	}

	/**
	 * @param application
	 * @param list
	 * @param name
	 */
	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, CustomValueList list)
	{
		super(application, scriptable, id, list);
		dlm = new LookupListModel(application, list);
		init();
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebDataField#setValidationEnabled(boolean)
	 */
	@Override
	public void setValidationEnabled(boolean validation)
	{
		if (list != null && list.getFallbackValueList() != null)
		{
			IValueList vlist = list;
			if (!validation)
			{
				vlist = list.getFallbackValueList();
			}
			if (vlist instanceof CustomValueList)
			{
				dlm = new LookupListModel(application, ((CustomValueList)vlist));
			}
			else
			{
				dlm = new LookupListModel(application, ((LookupValueList)vlist));
			}
		}
		super.setValidationEnabled(validation);
	}

	private void init()
	{
		add(new HeaderContributor(new IHeaderContributor()
		{
			private static final long serialVersionUID = 1L;

			@SuppressWarnings("nls")
			private StringBuffer appendPadding(StringBuffer sb)
			{
				Insets padding = getPadding();
				if (padding != null) sb.append("padding:" + padding.top + "px " + padding.right + "px " + padding.bottom + "px " + padding.left + "px; ");
				else sb.append("padding:2px; ");
				return sb;
			}

			@SuppressWarnings("nls")
			private StringBuffer appendMargins(StringBuffer sb)
			{
				Insets marginz = getMargin();
				if (marginz != null) sb.append("margin:" + marginz.top + "px " + marginz.right + "px " + marginz.bottom + "px " + marginz.left + "px;  ");
				else sb.append("margin:0; ");
				return sb;
			}

			@SuppressWarnings("nls")
			private StringBuffer appendPaddingAndMarginsToComponent(StringBuffer sb, String who)
			{
				sb.append("#" + getMarkupId() + who + " { ");
				sb.append(appendMargins(appendPadding(sb)));
				sb.append("} ");
				return sb;
			}

			@SuppressWarnings("nls")
			public void renderHead(IHeaderResponse response)
			{
				response.renderCSSReference(new CompressedResourceReference(WebDataLookupField.class, "servoy_lookupfield.css")); //$NON-NLS-1$

				StringBuffer headerStyle = new StringBuffer();
				headerStyle.append("<style type=\"text/css\"> ");
				headerStyle.append("#" + getMarkupId() + "-autocomplete.wicket-aa" + " { ");

				String fontFamily = "\"Lucida Grande\",\"Lucida Sans Unicode\",Tahoma,Verdana";
				if (getFont() != null)
				{
					if (getFont().getFamily() != null) fontFamily = "\"" + getFont().getFamily() + "\",Tahoma,Verdana";

					int style = getFont().getStyle();
					if ((style & Font.ITALIC) == Font.ITALIC) headerStyle.append("font-style:italic; ");
					if ((style & Font.BOLD) == Font.BOLD) headerStyle.append("font-weight:bold; ");
				}
				headerStyle.append("font-family:" + fontFamily + "; ");

				headerStyle.append("font-size:" + (getFont() == null ? "12" : new Integer(getFont().getSize())) + "px; ");

				String bgcolor = "#ffffff";//default background color to white
				if (getBackground() != null)
				{
					bgcolor = Integer.toHexString(getBackground().getRGB());
					bgcolor = "#" + bgcolor.substring(2, bgcolor.length());
				}
				headerStyle.append("background-color:" + bgcolor + "; ");

				String fgcolor = "#000000";//default foreground color to black
				if (getForeground() != null)
				{
					fgcolor = Integer.toHexString(getForeground().getRGB());
					fgcolor = "#" + fgcolor.substring(2, fgcolor.length());
				}
				headerStyle.append("color: " + fgcolor + "; ");

				headerStyle = appendPadding(headerStyle);

				headerStyle.append("min-width:" + (getSize().width - 6) + "px; "); // extract padding and border
				headerStyle.append("text-align:" + TemplateGenerator.getHorizontalAlignValue(getHorizontalAlignment()) + "; } ");

				headerStyle = appendPaddingAndMarginsToComponent(headerStyle, "-autocomplete.wicket-aa ul");

				headerStyle = appendPaddingAndMarginsToComponent(headerStyle, "-autocomplete.wicket-aa ul li.selected");

				headerStyle.append("</style>");
				response.renderString(headerStyle.toString());
			}
		})
		{
			@Override
			public boolean isEnabled(Component component)
			{
				return !getScriptObject().js_isReadOnly() && getScriptObject().js_isEnabled();
			}
		});

		setOutputMarkupPlaceholderTag(true);

		AutoCompleteSettings behSettings = new AutoCompleteSettings();
		behSettings.setMaxHeightInPx(200);
		behSettings.setPreselect(true);
		behSettings.setShowCompleteListOnFocusGain(true);
		behSettings.setAdjustInputWidth(false);

		ClientProperties clp = (application.getApplicationType() != IApplication.HEADLESS_CLIENT
			? ((WebClientInfo)Session.get().getClientInfo()).getProperties() : null); // in case of batch processors/jsp, we can't get browser info because UI is not given by web client components
		if (clp != null && (!clp.isBrowserInternetExplorer() || clp.getBrowserVersionMajor() >= 8))
		{
			// smart positioning doesn't work on IE < 8 (probably because of unreliable clientWidth/clientHeight browser element js properties)
			behSettings.setUseSmartPositioning(true);
			behSettings.setUseHideShowCoveredIEFix(false); // don't know if the problem this setting is for can still be reproduced (I couldn't reproduce it)... this is true by default and makes fields in IE and Opera appear/dissapear if they would be covered by type-ahead popup
		}
		else
		{
			behSettings.setUseSmartPositioning(false);
			behSettings.setUseHideShowCoveredIEFix(true);
		}
		behSettings.setThrottleDelay(500);
		AbstractAutoCompleteTextRenderer<Object> renderer = new AbstractAutoCompleteTextRenderer<Object>()
		{
			@Override
			protected String getTextValue(Object object)
			{
				String str = (object == null ? "" : object.toString()); //$NON-NLS-1$
				if (!HtmlUtils.hasHtmlTag(str)) str = HtmlUtils.escapeMarkup(str, true, false).toString();
				if (str.trim().equals("")) str = "&nbsp;"; //$NON-NLS-1$//$NON-NLS-2$
				return str;
			}

			@Override
			protected void renderChoice(Object object, Response response, String criteria)
			{
				if (IValueList.SEPARATOR_DESIGN_VALUE.equals(object)) return;
				super.renderChoice(object, response, criteria);
			}
		};

		AutoCompleteBehavior<Object> beh = new AutoCompleteBehavior<Object>(renderer, behSettings)
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior#getChoices(java.lang.String)
			 */
			@Override
			protected Iterator<Object> getChoices(String input)
			{
				try
				{
					dlm.fill(parentState, getDataProviderID(), input, false);
					return dlm.iterator();
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
				return Collections.emptyList().iterator();
			}

			/**
			 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#getFailureScript()
			 */
			@Override
			protected CharSequence getFailureScript()
			{
				return "onAjaxError();"; //$NON-NLS-1$
			}

			/**
			 * @see org.apache.wicket.ajax.AbstractDefaultAjaxBehavior#getPreconditionScript()
			 */
			@Override
			protected CharSequence getPreconditionScript()
			{
				return "onAjaxCall();" + super.getPreconditionScript(); //$NON-NLS-1$
			}

			// need to set this behavior to true (enterHidesWithNoSelection) because otherwise the onKeyDown events
			// or other events for the component with type ahead would be null in Firefox, and would not execute as
			// expected on the other browsers...
			@Override
			public void renderHead(IHeaderResponse response)
			{
				settings.setShowListOnEmptyInput(Boolean.TRUE.equals(UIUtils.getUIProperty(getScriptObject(), application,
					IApplication.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY, Boolean.TRUE)));
				settings.setShowListOnFocusGain(Boolean.TRUE.equals(UIUtils.getUIProperty(getScriptObject(), application,
					IApplication.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN, Boolean.TRUE)));
				if (!getScriptObject().js_isReadOnly() && getScriptObject().js_isEnabled())
				{
					super.renderHead(response);
					response.renderJavascript("Wicket.AutoCompleteSettings.enterHidesWithNoSelection = true;", "AutocompleteSettingsID"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			/**
			 * @see org.apache.wicket.behavior.AbstractBehavior#isEnabled(org.apache.wicket.Component)
			 */
			@Override
			public boolean isEnabled(Component component)
			{
				return super.isEnabled(component) && WebClientSession.get().useAjax();
			}
		};
		add(beh);
	}

	@Override
	public void setClientProperty(Object key, Object value)
	{
		if ((IApplication.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN.equals(key) || IApplication.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY.equals(key)) &&
			!Utils.equalObjects(getScriptObject().js_getClientProperty(key), value))
		{
			getStylePropertyChanges().setChanged();
		}
		super.setClientProperty(key, value);
	}

	@Override
	public void setValueList(IValueList vl)
	{
		super.setValueList(vl);
		if (list instanceof CustomValueList)
		{
			dlm = new LookupListModel(application, (CustomValueList)list);
		}
		converter = null; // clear old converter, so a new one is created  for the new list
	}

	/**
	 * Maps a trimmed value entered by the user to a value stored in the value list. The value stored in the value list may contain some trailing spaces, while
	 * the value entered by the user is trimmed.
	 * 
	 * @param value
	 * @return
	 */
	@SuppressWarnings("nls")
	public String mapTrimmedToNotTrimmed(String value)
	{
		// Although the value entered by the user should be trimmed, make sure it is so.
		String trimmed = value.trim();
		try
		{
			// this is the &nbsp character we set for empty value
			if ("\u00A0".equals(trimmed)) trimmed = "";
			// Grab all values that start with the value entered by the user.
			String result = matchValueListValue(trimmed, false);
			if (result == null)
			{
				dlm.fill(parentState, getDataProviderID(), trimmed, false);
				result = matchValueListValue(trimmed, false);
				if (result == null)
				{
					dlm.fill(parentState, getDataProviderID(), null, false);
					// now just try to match it be start with matching instead of equals:
					result = matchValueListValue(trimmed, true);
					// if this is found then it is a commit of data of a partial string, make sure that the field is updated with the complete value.
					String displayValue = (result == null && list.hasRealValues()) ? "" : result;
					// if this is found then it is a commit of data of a partial string, make sure that the field is updated with the complete value.
					if (displayValue != null && !displayValue.equals(trimmed) && RequestCycle.get() != null)
					{
						IRequestTarget requestTarget = RequestCycle.get().getRequestTarget();
						if (requestTarget instanceof AjaxRequestTarget)
						{
							((AjaxRequestTarget)requestTarget).appendJavascript("document.getElementById('" + getMarkupId() + "').value='" + displayValue + "'");
						}
					}
				}
			}
			// If no match was found then return back the value, otherwise return the found match.
			return result == null ? trimmed : result;
		}
		catch (Exception e)
		{
			Debug.error(e);
			return trimmed;
		}
	}

	/**
	 * @param trimmed
	 * @param startsWidth TODO
	 * @return
	 */
	private String matchValueListValue(String trimmed, boolean startsWidth)
	{
		int size = dlm.getSize();
		// Find a match in the value list.
		String result = null;
		if (startsWidth) trimmed = trimmed.toLowerCase();
		for (int i = 0; i < size; i++)
		{
			String currentValue = dlm.getElementAt(i).toString();
			if (startsWidth && currentValue.trim().toLowerCase().startsWith(trimmed))
			{
				result = currentValue;
				break;
			}
			else if (currentValue.trim().equals(trimmed))
			{
				result = currentValue;
				break;
			}
		}
		return result;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#setFoundSet(com.servoy.j2db.dataprocessing.IRecordInternal,
	 *      com.servoy.j2db.dataprocessing.IFoundSetInternal, boolean)
	 */
	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		if (this.parentState == parentState) return;
		this.parentState = parentState;
		if (list instanceof LookupValueList || list instanceof GlobalMethodValueList)
		{
			int index = -1;
			if (!ScopesUtils.isVariableScope(getDataProviderID()))
			{
				index = getDataProviderID().lastIndexOf('.');
			}
			if (index == -1 || parentState == null)
			{
				list.fill(parentState);
			}
			else
			{
				IFoundSetInternal relatedFoundSet = parentState.getRelatedFoundSet(getDataProviderID().substring(0, index));
				if (relatedFoundSet == null || relatedFoundSet.getSize() == 0)
				{
					list.fill(null);
				}
				else
				{
					IRecordInternal relatedRecord = relatedFoundSet.getRecord(relatedFoundSet.getSelectedIndex());
//					if (relatedRecord != null) relatedRecord.addModificationListener(this);
					list.fill(relatedRecord);
				}
			}
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

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#getDefaultSort()
	 */
	public List<SortColumn> getDefaultSort()
	{
		return dlm.getDefaultSort();
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#destroy()
	 */
	public void destroy()
	{
		parentState = null;
		detachModel();
	}

}
