/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.impl.ClientService;

import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignDefaultToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.util.Debug;

/**
 * Client service that provides our {@link IServoyDataConverterContext} to the service object for data conversion.
 *
 * @author jcompagner
 *
 */
public class ServoyClientService extends ClientService implements IContextProvider
{
	private final INGClientWebsocketSession session;

	public ServoyClientService(String serviceName, WebObjectSpecification spec, INGClientWebsocketSession session)
	{
		super(serviceName, spec, true);
		this.session = session;

		initDefaults(spec);
		propertiesInitialized();
	}

	protected void initDefaults(WebObjectSpecification spec)
	{
		PropertyPath propertyPath = new PropertyPath();
		IServoyDataConverterContext dataConverterContext = getDataConverterContext();

		// we have the service; now set default values as needed;
		// as services don't have a 'design' value, all their properties have the default values initially;
		// default values could either come from the .spec "default" or from the property type itself;
		// here we simulate design-to-formelement and formelement-to-sablo conversions right away
		// as services don't have a "form element"; so we will give null as form element and if any exception
		// happens we ignore it and log - it might be that a property type that was intended for component-only use
		// was used inside a service
		for (String propName : spec.getAllPropertiesNames())
		{
			PropertyDescription pd = spec.getProperty(propName);
			try
			{
				Object formElementEquivalentValue = null;

				// IMPORTANT NOTE: if you change anything in following if-else please update FormElement.initTemplateProperties as well
				if (pd.hasDefault())
				{

					propertyPath.add(pd.getName());
					formElementEquivalentValue = NGConversions.INSTANCE.convertDesignToFormElementValue(pd.getDefaultValue(), pd,
						dataConverterContext.getSolution(), null, propertyPath);
					propertyPath.backOneLevel();
				}
				else if (pd.getType() instanceof IDesignDefaultToFormElement< ? , ? , ? >)
				{
					propertyPath.add(pd.getName());
					formElementEquivalentValue = ((IDesignDefaultToFormElement< ? , ? , ? >)pd.getType()).toDefaultFormElementValue(pd,
						dataConverterContext.getSolution(), null, propertyPath);
					propertyPath.backOneLevel();
				}
				else if (pd.getType().defaultValue(pd) != null || pd.getType() instanceof IFormElementDefaultValueToSabloComponent)
				{
					// remember that we can use type specified default value when this gets transformed to JSON
					formElementEquivalentValue = NGConversions.IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER;
				}

				// simulate form element -> web component conversion
				if (formElementEquivalentValue != null)
				{
					// here form element, web component and dal params are null as this is a service - it will only work for property types that do not use those in this conversion
					setProperty(propName, NGConversions.INSTANCE.convertFormElementToSabloComponentValue(formElementEquivalentValue, pd, null, null, null));
					// we do not use .setDefaultProperty(...) as that is meant actually as 'template' value for components which is not the case for services
				}
			}
			catch (Exception e)
			{
				Debug.log("Default value could not be determined for property '" + propName + "' of service '" + name + "'. Type: '" + pd.getType().getName() +
					"'. See stack-trace. It is possible that the service .spec is using a property type that is only meant to work with components, not services. A null default will be assumed instead.",
					e);
			}
		}
	}

	@Override
	public IServoyDataConverterContext getDataConverterContext()
	{
		return new ServoyDataConverterContext(session.getClient());
	}

	@Override
	public void executeAsyncServiceCall(String functionName, Object[] arguments)
	{
		super.executeAsyncServiceCall(functionName, arguments);
		session.valueChanged();
	}

	@Override
	public Object getDefaultFromPD(PropertyDescription propertyDesc)
	{
		return WebFormComponent.getDefaultFromPropertyDescription(propertyDesc);
	}

}
