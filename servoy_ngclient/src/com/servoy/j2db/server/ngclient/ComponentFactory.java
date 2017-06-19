/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import org.sablo.Container;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.property.types.ISupportTemplateValue;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * @author lvostinar
 *
 */
public class ComponentFactory
{

	public static WebFormComponent createComponent(IApplication application, IDataAdapterList dataAdapterList, FormElement fe, Container parentToAddTo,
		Form form)
	{
		String name = fe.getName();
		IPersist persist = fe.getPersistIfAvailable();
		int access = 0;
		if (persist != null)
		{
			// don't add the component to the form ui if component is not visible due to security settings
			access = application.getFlattenedSolution().getSecurityAccess(persist.getUUID());
			if (!((access & IRepository.VIEWABLE) != 0)) return null;
		}

		// TODO anything to do here for custom special types?
		WebFormComponent webComponent = new WebFormComponent(name, fe, dataAdapterList);
		if (parentToAddTo != null) parentToAddTo.add(webComponent);

		WebObjectSpecification componentSpec = fe.getWebComponentSpec(false);

		// first convert formElement-to-Sablo and store them in the webComponent
		for (String propName : fe.getRawPropertyValues().keySet())
		{
			// TODO this if should not be necessary. currently in the case of "printable" hidden property
			if (componentSpec.getProperty(propName) == null) continue;
			Object value = fe.getPropertyValueConvertedForWebComponent(propName, webComponent, (DataAdapterList)dataAdapterList);
			fillProperty(value, fe.getPropertyValue(propName), componentSpec.getProperty(propName), webComponent);
		}

		// then after all of them are converted above attach them to the webComponent (so that when attach is called on any ISmartPropertyValue at least all the other properties are converted
		// this could help initialize smart properties that depend on each other faster then if we would convert and then attach right away each value)
		webComponent.propertiesInitialized();

		// overwrite accessible
		if (persist != null)
		{
			int elementSecurity;
			if (persist.getParent() instanceof Portal)
			{
				elementSecurity = application.getFlattenedSolution().getSecurityAccess(((Portal)persist.getParent()).getUUID());
			}
			else
			{
				elementSecurity = access;
			}
			if (!((elementSecurity & IRepository.ACCESSIBLE) != 0)) // element not accessible
			{
				webComponent.setProperty("enabled", false);
			}
			else
			{
				int formSecurity = application.getFlattenedSolution().getSecurityAccess(form);
				if (!((formSecurity & IRepository.ACCESSIBLE) != 0)) // form not accessible
				{
					webComponent.setProperty("enabled", false);
				}
			}
		}


		// TODO should this be a part of type conversions for handlers instead?
		for (String eventName : componentSpec.getHandlers().keySet())
		{
			Object eventValue = fe.getPropertyValue(eventName);
			if (eventValue instanceof String)
			{
				UUID uuid = UUID.fromString((String)eventValue);
				IPersist function = application.getFlattenedSolution().searchPersist(uuid);
				if (function != null)
				{
					webComponent.add(eventName, function.getID());
				}
				else
				{
					Debug.warn("Event handler for " + eventName + " not found (form " + form + ", form element " + name + ")");
				}
			}
			else if (eventValue instanceof Number && ((Number)eventValue).intValue() > 0)
			{
				webComponent.add(eventName, ((Number)eventValue).intValue());
			}
			else if (Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName()) &&
				(form.getOnElementFocusGainedMethodID() > 0))
			{
				webComponent.add(eventName, form.getOnElementFocusGainedMethodID());
			}
			else if (Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName()) &&
				(form.getOnElementFocusLostMethodID() > 0))
			{
				webComponent.add(eventName, form.getOnElementFocusLostMethodID());
			}
			else if (Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName()) &&
				(form.getOnElementDataChangeMethodID() > 0))
			{
				webComponent.add(eventName, form.getOnElementDataChangeMethodID());
			}
		}

		// just created, it should have no changes.
		webComponent.clearChanges();
		return webComponent;
	}

	protected static void fillProperty(Object propertyValue, Object formElementValue, PropertyDescription propertySpec, WebFormComponent component)
	{
		String propName = propertySpec.getName();
		if (propertyValue != null)
		{
			boolean templatevalue = true;
			if (propertySpec.getType() instanceof ISupportTemplateValue)
			{
				templatevalue = ((ISupportTemplateValue)propertySpec.getType()).valueInTemplate(formElementValue, propertySpec,
					new FormElementContext(component.getFormElement()));
			}
			if (templatevalue)
			{
				component.setDefaultProperty(propName, propertyValue);
			}
			else
			{
				component.setProperty(propName, propertyValue);
			}
		}
	}

	public static String getMarkupId(String formName, String elementName)
	{
		return Utils.calculateMD5HashBase16(formName + elementName);
	}

}
