/*
` This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.convert.IConverter;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.GlobalMethodValueList;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListChangeListener;
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
	protected LookupListChangeListener changeListener;

	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, LookupValueList list)
	{
		super(application, scriptable, id, list);
		createLookupListModel(list);
		init();
	}

	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, final String serverName, String tableName,
		String dataProviderID)
	{
		super(application, scriptable, id);
		dlm = new LookupListModel(application, serverName, tableName, dataProviderID);
		init();
	}

	public WebDataLookupField(IApplication application, RuntimeDataLookupField scriptable, String id, CustomValueList list)
	{
		super(application, scriptable, id, list);
		createCustomListModel(list);
		init();
	}

	protected void createCustomListModel(CustomValueList vList)
	{
		dlm = new LookupListModel(application, vList);

		if (changeListener == null) changeListener = new LookupListChangeListener(this);
		vList.addListDataListener(changeListener);
	}

	protected void createLookupListModel(LookupValueList vlist)
	{
		dlm = new LookupListModel(application, vlist);

		if (dlm.isShowValues() != dlm.isReturnValues())
		{
			try
			{
				if (changeListener == null) changeListener = new LookupListChangeListener(this);
				vlist.addListDataListener(changeListener);
			}
			catch (Exception e)
			{
				Debug.error("Error registering table listener for web lookup"); //$NON-NLS-1$
				Debug.error(e);
			}
		}
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
				createCustomListModel((CustomValueList)vlist);
			}
			else
			{
				createLookupListModel((LookupValueList)vlist);
			}
		}
		super.setValidationEnabled(validation);
	}

	private void init()
	{

		add(new HeaderContributor(new IHeaderContributor()
		{
			private static final long serialVersionUID = 1L;

			public void renderHead(IHeaderResponse response)
			{
				response.renderCSSReference(new CompressedResourceReference(WebDataLookupField.class, "servoy_lookupfield.css")); //$NON-NLS-1$
			}
		})
		{
			@Override
			public boolean isEnabled(Component component)
			{
				return !getScriptObject().isReadOnly() && getScriptObject().isEnabled();
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

		IAutoCompleteRenderer<Object> renderer = new IAutoCompleteRenderer<Object>()
		{
			protected String getTextValue(Object object)
			{
				String str = ""; //$NON-NLS-1$
				if (object != null)
				{
					IConverter con = getConverter(object.getClass());
					if (con != null)
					{
						str = con.convertToString(object, getLocale());
					}
					else
					{
						str = object.toString();
					}
				}
				if (str.trim().equals("")) str = "&nbsp;"; //$NON-NLS-1$//$NON-NLS-2$
				return str;
			}

			protected void renderChoice(Object object, Response response, String criteria)
			{
				if (IValueList.SEPARATOR_DESIGN_VALUE.equals(object)) return;
				String renderedObject = getTextValue(object);
				if (!HtmlUtils.hasHtmlTag(renderedObject)) renderedObject = HtmlUtils.escapeMarkup(renderedObject, true, false).toString();
				response.write(renderedObject);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer#render(java.lang.Object, org.apache.wicket.Response,
			 * java.lang.String)
			 */
			public void render(Object object, Response response, String criteria)
			{
				String textValue = getTextValue(object);
				if (textValue == null)
				{
					throw new IllegalStateException("A call to textValue(Object) returned an illegal value: null for object: " + object.toString());
				}
				textValue = textValue.replaceAll("\\\"", "&quot;");

				response.write("<li textvalue=\"" + textValue + "\"");
				response.write(">");
				renderChoice(object, response, criteria);
				response.write("</li>");
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer#renderHeader(org.apache.wicket.Response)
			 */
			@SuppressWarnings("nls")
			public void renderHeader(Response response)
			{
				StringBuffer listStyle = new StringBuffer();
				listStyle.append("style=\"");

				String fFamily = "'Lucida Grande','Lucida Sans Unicode',Tahoma,Verdana";
				String bgColor = "#ffffff";
				String fgColor = "#000000";
				String fSize = "12px";
				String padding = "2px";
				String margin = "0px";
				if (getFont() != null)
				{
					Font f = getFont();
					if (f != null)
					{
						if (f.getFamily() != null)
						{
							fFamily = f.getFamily();
							if (fFamily.contains(" ")) fFamily = "'" + fFamily + "'";
						}
						if (f.getName() != null)
						{
							String fName = f.getName();
							if (fName.contains(" ")) fName = "'" + fName + "'";
							fFamily = fName + "," + fFamily;
						}
						if (f.isBold()) listStyle.append("font-weight:bold; ");
						if (f.isItalic()) listStyle.append("font-style:italic; ");

						fSize = Integer.toString(f.getSize()) + "px";
					}
				}

				if (getBackground() != null)
				{
					bgColor = getWebColor(getBackground().getRGB());
				}
				if (getForeground() != null)
				{
					fgColor = getWebColor(getForeground().getRGB());
				}
				Insets _padding = getPadding();
				if (getPadding() != null) padding = _padding.top + "px " + _padding.right + "px " + _padding.bottom + "px " + _padding.left + "px";
				Insets _margin = getMargin();
				if (_margin != null) margin = _margin.top + "px " + _margin.right + "px " + _margin.bottom + "px " + _margin.left + "px ";

				listStyle.append("font-family:" + fFamily + "; ");
				listStyle.append("background-color: " + bgColor + "; ");
				listStyle.append("color: " + fgColor + "; ");
				listStyle.append("font-size:" + fSize + "; ");
				listStyle.append("min-width:" + (getSize().width - 6) + "px; "); // extract padding and border
				listStyle.append("margin: " + margin + "; ");
				listStyle.append("padding: " + padding + "; ");
				listStyle.append("text-align:" + TemplateGenerator.getHorizontalAlignValue(getHorizontalAlignment()));
				listStyle.append("\"");

				response.write("<ul " + listStyle + ">");
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer#renderFooter(org.apache.wicket.Response)
			 */
			public void renderFooter(Response response)
			{
				response.write("</ul>"); //$NON-NLS-1$
			}

			/**
			 * Returns web color representation of int rgba color by
			 * removing the alpha value
			 * 
			 * @param color int representation of rgba color
			 * @return web color of form #rrggbb
			 */
			private String getWebColor(int color)
			{
				String webColor = Integer.toHexString(color);
				int startIdx = webColor.length() - 6;
				if (startIdx < 0) startIdx = 0;
				webColor = webColor.substring(startIdx);

				StringBuilder sb = new StringBuilder();
				sb.append('#');
				int nrMissing0 = 6 - webColor.length();
				for (int i = 0; i < nrMissing0; i++)
				{
					sb.append('0');
				}
				sb.append(webColor);

				return sb.toString();
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
				if (!getScriptObject().isReadOnly() && getScriptObject().isEnabled())
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
				IFormUIInternal< ? > formui = findParent(IFormUIInternal.class);
				if (formui != null && formui.isDesignMode())
				{
					return false;
				}
				return super.isEnabled(component) && WebClientSession.get().useAjax();
			}
		};
		add(beh);
	}

	@Override
	public void setClientProperty(Object key, Object value)
	{
		if ((IApplication.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN.equals(key) || IApplication.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY.equals(key)) &&
			!Utils.equalObjects(getScriptObject().getClientProperty(key), value))
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
			createCustomListModel((CustomValueList)list);
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
			if ("\u00A0".equals(trimmed) || trimmed.length() == 0) return trimmed;
			// Grab all values that start with the value entered by the user.
			String result = matchValueListValue(trimmed, false);
			if (result == null)
			{
				dlm.fill(parentState, getDataProviderID(), trimmed, false);
				result = matchValueListValue(trimmed, false);
				if (result == null && list.hasRealValues())
				{
					dlm.fill(parentState, getDataProviderID(), null, false);
					//if it doesn't have real values, just keep what is typed
					// now just try to match it be start with matching instead of equals:
					result = matchValueListValue(trimmed, true);
					// if this is found then it is a commit of data of a partial string, make sure that the field is updated with the complete value.
					String displayValue = result == null ? "" : result;
					// if this is found then it is a commit of data of a partial string, make sure that the field is updated with the complete value.
					if (displayValue != null && !displayValue.equals(trimmed) && RequestCycle.get() != null)
					{
						IRequestTarget requestTarget = RequestCycle.get().getRequestTarget();
						if (requestTarget instanceof AjaxRequestTarget)
						{
							((AjaxRequestTarget)requestTarget).appendJavascript("if (document.getElementById('" + getMarkupId() + "').value == '" + value +
								"') document.getElementById('" + getMarkupId() + "').value='" + displayValue + "'");
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
		// if not found, and it must be an exact match, try the fallback valuelist.
		if (!startsWidth && result == null && dlm.getValueList().getFallbackValueList() != null)
		{
			IValueList fallbackValueList = dlm.getValueList().getFallbackValueList();
			int index = fallbackValueList.indexOf(trimmed);
			if (index >= 0)
			{
				return trimmed;
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
