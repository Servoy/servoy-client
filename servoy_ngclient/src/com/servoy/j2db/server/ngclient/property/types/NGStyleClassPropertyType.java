/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.types.StyleClassPropertyType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 2022.12
 *
 */
public class NGStyleClassPropertyType extends StyleClassPropertyType implements IDesignToFormElement<String, String, String>, IDesignValueConverter<String>
{
	public static final NGStyleClassPropertyType NG_INSTANCE = new NGStyleClassPropertyType();

	private NGStyleClassPropertyType()
	{
	}

	@Override
	public String toFormElementValue(String designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		String returnValue = designValue;
		// this needs to test if there is a variants property and that is set, if so the default of the property should not be send.
		String defaultValue = (String)pd.getDefaultValue();
		if (defaultValue != null && formElement.getPersistIfAvailable() instanceof WebComponent)
		{
			WebComponent webComponent = (WebComponent)formElement.getPersistIfAvailable();
			Collection<PropertyDescription> variants = formElement.getProperties(VariantPropertyType.INSTANCE);
			returnValue = filterDefaultValues(returnValue, defaultValue, webComponent, variants);
		}
		return returnValue;
	}

	/**
	 * @param returnValue
	 * @param defaultValue
	 * @param webComponent
	 * @param variants
	 * @return
	 */
	private String filterDefaultValues(String value, String defaultValue, WebComponent webComponent, Collection<PropertyDescription> variants)
	{
		String returnValue = value;
		if (returnValue != null)
		{
			for (PropertyDescription variantPd : variants)
			{
				String variantValue = (String)webComponent.getProperty(variantPd.getName());
				if (!Utils.stringIsEmpty(variantValue))
				{
					List<String> defaultValues = Arrays.asList(defaultValue.split(" ")); //$NON-NLS-1$
					List<String> currentValues = new ArrayList<String>(Arrays.asList(returnValue.split(" "))); //$NON-NLS-1$

					currentValues.removeAll(defaultValues);
					returnValue = StringUtils.join(currentValues, ' ');
				}
			}
		}
		return returnValue;
	}

	@Override
	public String fromDesignValue(Object designValue, PropertyDescription propertyDescription, IPersist persist)
	{
		if (designValue instanceof String)
		{
			String returnValue = (String)designValue;
			if (persist instanceof WebComponent && returnValue != null)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
					((WebComponent)persist).getTypeName());
				returnValue = filterDefaultValues(returnValue, (String)propertyDescription.getDefaultValue(), (WebComponent)persist,
					spec.getProperties(VariantPropertyType.INSTANCE));
			}
			return Utils.stringIsEmpty(returnValue) ? null : returnValue;
		}
		return null;
	}

	@Override
	public Object toDesignValue(Object javaValue, PropertyDescription pd)
	{
		return javaValue;
	}

}
