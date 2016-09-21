/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.IYieldingType;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.ISupportsGranularUpdates;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.IFindModeAwareType;
import com.servoy.j2db.server.ngclient.property.types.ISupportTemplateValue;
import com.servoy.j2db.server.ngclient.property.types.IWrapperDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Pair;

/**
 * This property type can be used to give "foundset linked" capabilities to existing property types.<br/>
 * So many property types can be wrapped by this type to be able to provide values for an entire foundset instead of just one value.<br/><br/>
 *
 * When config option "forFoundset" is not present in the .spec file for such a property, the PropertyDescription will fall back to the wrapped
 * property type.
 *
 * For example the DataproviderPropertyType can be wrapped in FoundsetLinkedPropertyType and registered as "dataprovider". Then when used in spec. files
 * like "myProp: 'dataprovider'" it will be treated directly as DataproviderPropertyType (that is what the {@link PropertyDescription#getType()} will return) avoiding any
 * overhead a proxy-like implementation of FoundsetLinkedPropertyType would add.<br/><br/>
 *
 * But when a property is declared as "myProp: {type: 'dataprovider', forFoundset: ...}" then {@link PropertyDescription#getType()} will return and work with this FoundsetLinkedPropertyType.
 *
 * It will work with a the wrapped type that uses a IServoyAwarePropertyValue (for YT - as this is it's main goal - record linked values) but can work with non-record linked values as well (if wrapped
 * property type decides to change this at runtime for example).
 *
 * FIXME NOTE: as more ICanBeLinkedToFoundset types are added we must make sure that FoundsetLinkedPropertyType implements all type interfaces of the wrapped types for it to work correctly!
 *
 * @author acostescu
 */
public class FoundsetLinkedPropertyType<YF, YT>
	implements IYieldingType<FoundsetLinkedTypeSabloValue<YF, YT>, YT>, IFormElementToTemplateJSON<YF, FoundsetLinkedTypeSabloValue<YF, YT>>,
	ISupportTemplateValue<YF>, IWrapperDataLinkedType<YF, FoundsetLinkedTypeSabloValue<YF, YT>>,
	IFormElementToSabloComponent<YF, FoundsetLinkedTypeSabloValue<YF, YT>>, IConvertedPropertyType<FoundsetLinkedTypeSabloValue<YF, YT>>,
	IFindModeAwareType<YF, FoundsetLinkedTypeSabloValue<YF, YT>>, ISabloComponentToRhino<FoundsetLinkedTypeSabloValue<YF, YT>>,
	IRhinoToSabloComponent<FoundsetLinkedTypeSabloValue<YF, YT>>, ISupportsGranularUpdates<FoundsetLinkedTypeSabloValue<YF, YT>>
{

	protected static final String SINGLE_VALUE = "sv"; //$NON-NLS-1$
	protected static final String SINGLE_VALUE_UPDATE = "svu"; //$NON-NLS-1$
	protected static final String VIEWPORT_VALUE = "vp"; //$NON-NLS-1$
	protected static final String VIEWPORT_VALUE_UPDATE = "vpu"; //$NON-NLS-1$
	protected static final String CONVERSION_NAME = "fsLinked";

	public static final String FOR_FOUNDSET_PROPERTY_NAME = "forFoundset"; //$NON-NLS-1$

	protected final String name;
	protected ICanBeLinkedToFoundset<YF, YT> wrappedType;

	public FoundsetLinkedPropertyType(String name, ICanBeLinkedToFoundset<YF, YT> wrappedType)
	{
		this.name = name;
		this.wrappedType = wrappedType;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public IPropertyType< ? > yieldToOtherIfNeeded(String propertyName, YieldDescriptionArguments parameters)
	{
		FoundsetLinkedConfig cfg = (FoundsetLinkedConfig)parameters.getConfig();
		if (cfg == null || cfg.forFoundset == null)
		{
			// the wrapped type can do it's thing then; it's not linked to a foundset so we yield to it's impl.
			parameters.setConfig(cfg != null ? cfg.wrappedConfig : wrappedType.parseConfig(null));
			return wrappedType;
		}

		FoundsetLinkedConfig config = ((FoundsetLinkedConfig)parameters.getConfig());
		config.setWrappedPropertyDescription(
			new PropertyDescription(propertyName, wrappedType, ((FoundsetLinkedConfig)parameters.getConfig()).wrappedConfig, parameters.defaultValue,
				parameters.initialValue, parameters.defaultValue != null, parameters.values, parameters.pushToServer, parameters.tags, parameters.optional));
		return this;
	}

	@Override
	public IPropertyType<YT> getPossibleYieldType()
	{
		return wrappedType;
	}

	@Override
	public FoundsetLinkedConfig parseConfig(JSONObject config)
	{
		return config == null ? null : new FoundsetLinkedConfig(config.optString(FOR_FOUNDSET_PROPERTY_NAME, null), wrappedType.parseConfig(config));
	}

	@Override
	public FoundsetLinkedTypeSabloValue<YF, YT> defaultValue(PropertyDescription pd)
	{
		YT wrappedDefault = wrappedType.defaultValue(getConfig(pd).wrappedPropertyDescription);
		return wrappedDefault == null ? null : new FoundsetLinkedTypeSabloValue<YF, YT>(wrappedDefault, getConfig(pd).forFoundset);
	}

	@Override
	public boolean isProtecting()
	{
		return wrappedType.isProtecting();
	}

	@Override
	public TargetDataLinks getDataLinks(YF formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		if (wrappedType instanceof IDataLinkedType)
		{
			PropertyDescription wrappedPd = getConfig(pd).wrappedPropertyDescription;
			return ((IDataLinkedType<YF, YT>)wrappedType).getDataLinks(formElementValue, wrappedPd, flattenedSolution, formElement);
		}

		return TargetDataLinks.NOT_LINKED_TO_DATA;
	}

	@Override
	public boolean valueInTemplate(YF formElementValue, PropertyDescription pd, FormElementContext formElementContext)
	{
		return true; // even if wrapped value is not in template, we still send the "forFoundset" config value
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, YF formElementValue, PropertyDescription pd, DataConversion browserConversionMarkers,
		FormElementContext formElementContext) throws JSONException
	{
		browserConversionMarkers.convert(CONVERSION_NAME);
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();

		writer.key(FoundsetLinkedPropertyType.FOR_FOUNDSET_PROPERTY_NAME).value(getConfig(pd).forFoundset);

		if (wrappedType instanceof ISupportTemplateValue &&
			((ISupportTemplateValue<YF>)wrappedType).valueInTemplate(formElementValue, getConfig(pd).wrappedPropertyDescription, formElementContext))
		{
			DataConversion dataConversions = new DataConversion();
			dataConversions.pushNode(SINGLE_VALUE);
			NGConversions.INSTANCE.convertFormElementToTemplateJSONValue(writer, SINGLE_VALUE, formElementValue, getConfig(pd).wrappedPropertyDescription,
				browserConversionMarkers, formElementContext);
			JSONUtils.writeClientConversions(writer, dataConversions);
		}

		writer.endObject();
		return writer;
	}

	protected FoundsetLinkedConfig getConfig(PropertyDescription pd)
	{
		return ((FoundsetLinkedConfig)pd.getConfig());
	}

	@Override
	public FoundsetLinkedTypeSabloValue<YF, YT> toSabloComponentValue(YF formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return new FoundsetLinkedTypeSabloValue<YF, YT>(getConfig(pd).forFoundset, formElementValue, getConfig(pd).wrappedPropertyDescription, formElement,
			component);
	}

	@Override
	public FoundsetLinkedTypeSabloValue<YF, YT> fromJSON(Object newJSONValue, FoundsetLinkedTypeSabloValue<YF, YT> previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (previousSabloValue != null)
		{
			previousSabloValue.browserUpdatesReceived(newJSONValue, getConfig(pd).wrappedPropertyDescription, pd, dataConverterContext,
				returnValueAdjustedIncommingValue);
		}
		// else there's nothing to do here / this type can't receive browser updates when server has no value for it

		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, FoundsetLinkedTypeSabloValue<YF, YT> sabloValue, PropertyDescription pd,
		DataConversion clientConversion, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return sabloValue.fullToJSON(writer, key, clientConversion, getConfig(pd).wrappedPropertyDescription, dataConverterContext);
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter writer, String key, FoundsetLinkedTypeSabloValue<YF, YT> sabloValue, PropertyDescription pd,
		DataConversion clientConversion, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return sabloValue.changesToJSON(writer, key, clientConversion, getConfig(pd).wrappedPropertyDescription, dataConverterContext);
	}

	@Override
	public boolean isValueAvailableInRhino(FoundsetLinkedTypeSabloValue<YF, YT> webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		if (wrappedType instanceof ISabloComponentToRhino)
			return ((ISabloComponentToRhino<YT>)wrappedType).isValueAvailableInRhino(webComponentValue.getWrappedValue(),
				getConfig(pd).wrappedPropertyDescription, componentOrService);

		return true;
	}

	@Override
	public Object toRhinoValue(FoundsetLinkedTypeSabloValue<YF, YT> webComponentValue, PropertyDescription pd, BaseWebObject componentOrService,
		Scriptable startScriptable)
	{
		return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(webComponentValue.getWrappedValue(), getConfig(pd).wrappedPropertyDescription,
			componentOrService, startScriptable);
	}

	@Override
	public FoundsetLinkedTypeSabloValue<YF, YT> toSabloComponentValue(Object rhinoValue, FoundsetLinkedTypeSabloValue<YF, YT> previousComponentValue,
		PropertyDescription pd, BaseWebObject componentOrService)
	{
		if (previousComponentValue == null)
		{
			FoundsetLinkedTypeSabloValue<YF, YT> flts = new FoundsetLinkedTypeSabloValue<YF, YT>(getConfig(pd).forFoundset, (YF)rhinoValue,
				getConfig(pd).wrappedPropertyDescription, ((WebFormComponent)componentOrService).getFormElement(), (WebFormComponent)componentOrService);
			return flts;
		}
		else
		{
			previousComponentValue.rhinoToSablo(rhinoValue, getConfig(pd).wrappedPropertyDescription, componentOrService);
			return previousComponentValue;
		}
	}

	@Override
	public boolean isFindModeAware(YF formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		if (wrappedType instanceof IFindModeAwareType)
		{
			PropertyDescription wrappedPd = getConfig(pd).wrappedPropertyDescription;
			return ((IFindModeAwareType<YF, YT>)wrappedType).isFindModeAware(formElementValue, wrappedPd, flattenedSolution, formElement);
		}

		return false;
	}

	@Override
	public Pair<IDataLinkedPropertyValue, PropertyDescription> getWrappedDataLinkedValue(FoundsetLinkedTypeSabloValue<YF, YT> propertyValue,
		PropertyDescription pd)
	{
		if (propertyValue != null && propertyValue.wrappedSabloValue instanceof IDataLinkedPropertyValue)
			return new Pair(propertyValue.wrappedSabloValue, getConfig(pd).wrappedPropertyDescription);
		return null;
	}

}