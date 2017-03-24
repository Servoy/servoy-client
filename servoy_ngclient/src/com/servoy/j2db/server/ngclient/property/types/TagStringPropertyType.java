/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.HTMLTagsConverter;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.ICanBeLinkedToFoundset;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;


/**
 * Property type that handles smart text properties (aware of i18n and dataprovider/special tag usage %%...%%).
 *
 * @author jcompagner
 * @author acostescu
 */
public class TagStringPropertyType extends DefaultPropertyType<BasicTagStringTypeSabloValue> implements
	IFormElementToTemplateJSON<String, BasicTagStringTypeSabloValue>, ISupportTemplateValue<String>, IDataLinkedType<String, BasicTagStringTypeSabloValue>,
	IFormElementToSabloComponent<String, BasicTagStringTypeSabloValue>, IConvertedPropertyType<BasicTagStringTypeSabloValue>,
	ISabloComponentToRhino<BasicTagStringTypeSabloValue>, IRhinoToSabloComponent<BasicTagStringTypeSabloValue>,
	ICanBeLinkedToFoundset<String, BasicTagStringTypeSabloValue>, II18NPropertyType<BasicTagStringTypeSabloValue>
{

	public static final TagStringPropertyType INSTANCE = new TagStringPropertyType();
	public static final String TYPE_NAME = "tagstring";

	protected TagStringPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public TagStringConfig parseConfig(JSONObject json)
	{
		// see TagStringConfig docs for what the defaults mean
		String displayTagsPropertyName = null;
		boolean displayTags = true;
		boolean useParsedValueInRhino = false;
		// String forFoundsetPropertyName = null; // see FoundsetLinkedPropertyType for how tagstrings linked to foundsets work

		if (json != null)
		{
			// see TagStringConfig docs for what the defaults mean
			displayTagsPropertyName = json.optString(TagStringConfig.DISPLAY_TAGS_PROPERTY_NAME_CONFIG_OPT, null);
			displayTags = json.has(TagStringConfig.DISPLAY_TAGS_CONFIG_OPT) ? json.optBoolean(TagStringConfig.DISPLAY_TAGS_CONFIG_OPT, true) : true;
			useParsedValueInRhino = json.has(TagStringConfig.USE_PARSED_VALUE_IN_RHINO_CONFIG_OPT)
				? json.optBoolean(TagStringConfig.USE_PARSED_VALUE_IN_RHINO_CONFIG_OPT, false) : false;
		}

		return new TagStringConfig(displayTagsPropertyName, displayTags, useParsedValueInRhino);
	}

	protected TagStringConfig getConfig(PropertyDescription pd)
	{
		return (TagStringConfig)pd.getConfig();
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		// TODO when type has more stuff added to it, see if this needs to be changed (what is put in form cached templates for such properties)
		JSONUtils.addKeyIfPresent(writer, key);
		if (formElementValue != null && valueInTemplate(formElementValue, pd, formElementContext))
		{
			writer.value(formElementValue);
		}
		else
		{
			writer.value("");
		}

		return writer;
	}

	@Override
	public BasicTagStringTypeSabloValue fromJSON(Object newValue, BasicTagStringTypeSabloValue previousValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		BaseWebObject webObject = dataConverterContext.getWebObject();
		return createNewTagStringTypeSabloValue((String)newValue, (previousValue != null ? previousValue.getDataAdapterList() : null), false, false, pd,
			webObject instanceof WebFormComponent ? ((WebFormComponent)webObject) : null,
			((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication(), false);
	}

	protected BasicTagStringTypeSabloValue createNewTagStringTypeSabloValue(String designValue, DataAdapterList dataAdapterList, boolean tagParsingAllowed,
		boolean htmlParsingAllowed, PropertyDescription propertyDescription, WebFormComponent component, INGApplication application,
		boolean basedOnFormElementValue)
	{
		BasicTagStringTypeSabloValue sabloValue;
		TagStringConfig config = (TagStringConfig)propertyDescription.getConfig();
		boolean wouldLikeToParseTags = wouldLikeToParseTags(config, component.getFormElement()); // this setting is decided at design/form-element time and won't change even if the value gets changed from rhino

		// if "wouldLikeToParseTags && !config.useParsedValueInRhino()" is true, we will never have a null previous value so we can still reach DAL
		// the "&& !config.useParsedValueInRhino()" is an optimization; because if config.useParsedValueInRhino() is true, then no new value set from Rhino or scripting will be able to handle tags any more - so there's no need to hang on to DAL (if this changes, you can remove this check)
		boolean needsToKeepDAL = (wouldLikeToParseTags && !config.useParsedValueInRhino());
		DataAdapterList dal = (needsToKeepDAL ? dataAdapterList : null);

		String newDesignValue = designValue != null && designValue.startsWith("i18n:") ? application.getI18NMessage(designValue.toString().substring(5))
			: designValue;
		if (newDesignValue == null)
		{
			sabloValue = needsToKeepDAL ? new BasicTagStringTypeSabloValue(null, dal) : null;
		}
		else if (tagParsingAllowed && wouldLikeToParseTags && newDesignValue.contains("%%")) // tagParsingAllowed is a security feature so that browsers cannot change tagStrings to something that is then able to show random server-side data
		{
			// TODO currently htmlParsingAllowed will be true here as well (the method is never called with true/false); but if that is needed in the future, we need to let TagStringTypeSabloValue of htmlParsingAllowed == false as well)
			// data links are required; register them to DAL; normally DAL can't be null here
			if (designValue != newDesignValue || designValue.contains("%%i18n:"))
			{
				sabloValue = new I18NTagStringTypeSabloValue(newDesignValue, dal, component.getDataConverterContext(), propertyDescription,
					component.getFormElement(), designValue);
			}
			else
			{
				sabloValue = new TagStringTypeSabloValue(newDesignValue, dal, component.getDataConverterContext(), propertyDescription,
					component.getFormElement());
			}
		}
		else
		// just some static string
		{
			String staticValue = newDesignValue;
			if (htmlParsingAllowed && HtmlUtils.startsWithHtml(staticValue)) // htmlParsingAllowed is a security feature so that browsers cannot change tagStrings to something that is then able to execute random server-side javascript
			{
				staticValue = HTMLTagsConverter.convert(staticValue, component.getDataConverterContext(), false);
			}

			// no data links required
			if (designValue != newDesignValue || designValue.contains("%%i18n:"))
			{
				sabloValue = new BasicI18NTagStringTypeSabloValue(staticValue, dal, designValue);
			}
			else
			{
				sabloValue = new BasicTagStringTypeSabloValue(staticValue, dal);
			}
		}

		return sabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, BasicTagStringTypeSabloValue object, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (object != null)
		{
			object.toJSON(writer, key, clientConversion, dataConverterContext);
		}
		else
		{
			JSONUtils.addKeyIfPresent(writer, key);
			writer.value("");
		}
		return writer;
	}

	@Override
	public boolean valueInTemplate(String formElementVal, PropertyDescription pd, FormElementContext formElementContext)
	{
		if (formElementVal == null) return true;
		if (formElementContext.getFormElement().getDesignId() != null) return true;

		TagStringConfig config = ((TagStringConfig)pd.getConfig());

		// TODO - it could still return "value" even for HTML if we know HTMLTagsConverter.convert() would not want to touch that (so simple HTML)
		// but we don't want to expose the actual design-time stuff that would normally get encrypted by HTMLTagsConverter.convert() or is not yet valid (blobloader without an application instance for example).

		return !((wouldLikeToParseTags(config, formElementContext.getFormElement()) && formElementVal.contains("%%")) || formElementVal.startsWith("i18n:") ||
			HtmlUtils.startsWithHtml(formElementVal));
	}

	/**
	 * Checks the component's spec. configurations options and form element properties (if needed)
	 * to see if this property should parse tags (%%x%%) or not.
	 */
	protected boolean wouldLikeToParseTags(TagStringConfig config, FormElement formElement)
	{
		String dtpn = config.getDisplayTagsPropertyName();
		Object dtPropVal = null;
		if (dtpn != null)
		{
			dtPropVal = formElement.getPropertyValue(dtpn);
			if (dtPropVal == null) dtPropVal = Boolean.FALSE;
		}
		return (dtpn != null && ((Boolean)dtPropVal).booleanValue() == true) || (dtpn == null && config.shouldDisplayTags());
	}

	@Override
	public TargetDataLinks getDataLinks(String formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, final INGFormElement formElement)
	{
		final Set<String> dataProviders = new HashSet<>();
		final boolean recordDP[] = new boolean[1];

		Text.processTags(formElementValue, new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				String dp = name;
				if (dp.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
				{
					dp = ScriptVariable.SCOPES_DOT_PREFIX + dp;
				}

				dataProviders.add(dp);
				// TODO Can't it be something special like record count or current record which are special cases and could still not depend on record...?
				recordDP[0] = recordDP[0] || (!ScopesUtils.isVariableScope(dp) && formElement.getForm().getScriptVariable(dp) == null);

				return dp;
			}
		});

		return dataProviders.size() == 0 ? TargetDataLinks.NOT_LINKED_TO_DATA
			: new TargetDataLinks(dataProviders.toArray(new String[dataProviders.size()]), recordDP[0]);
	}

	@Override
	public BasicTagStringTypeSabloValue toSabloComponentValue(String formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return createNewTagStringTypeSabloValue(formElementValue, dataAdapterList, true, true, pd, component,
			((IContextProvider)component).getDataConverterContext().getApplication(), true);
	}

	@Override
	public BasicTagStringTypeSabloValue toSabloComponentValue(Object rhinoValue, BasicTagStringTypeSabloValue previousComponentValue, PropertyDescription pd,
		BaseWebObject componentOrService)
	{
		if (rhinoValue != null)
		{
			// this code can interpret the new value as a static one or a a tag-aware one depending on the property's config: USE_PARSED_VALUE_IN_RHINO_CONFIG_OPT
			String newDesignValue = rhinoValue instanceof String ? (String)rhinoValue : rhinoValue.toString();
			return createNewTagStringTypeSabloValue(newDesignValue, (previousComponentValue != null ? previousComponentValue.getDataAdapterList() : null),
				!((TagStringConfig)pd.getConfig()).useParsedValueInRhino(), true, pd,
				componentOrService instanceof WebFormComponent ? ((WebFormComponent)componentOrService) : null,
				((IContextProvider)componentOrService).getDataConverterContext().getApplication(), false);
		}
		return null;
	}

	@Override
	public boolean isValueAvailableInRhino(BasicTagStringTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(BasicTagStringTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService,
		Scriptable startScriptable)
	{
		if (webComponentValue == null) return null;

		if (((TagStringConfig)pd.getConfig()).useParsedValueInRhino()) return webComponentValue.getTagReplacedValue();
		else return webComponentValue.getDesignValue();
	}

	@Override
	public BasicTagStringTypeSabloValue resetI18nValue(BasicTagStringTypeSabloValue value, PropertyDescription pd, WebFormComponent component)
	{
		if (value instanceof II18NValue)
		{
			String i18nKey = ((II18NValue)value).getI18NKey();
			BasicTagStringTypeSabloValue sabloComponentValue = TagStringPropertyType.INSTANCE.toSabloComponentValue(i18nKey, pd, component.getFormElement(),
				component, ((II18NValue)value).getDataAdapterList());
			return sabloComponentValue;
		}
		return value;
	}
}
