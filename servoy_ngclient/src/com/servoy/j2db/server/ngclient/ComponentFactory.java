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
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.property.types.ISupportTemplateValue;
import com.servoy.j2db.util.Debug;
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
		int elementSecurity = 0;
		if (persist != null)
		{
			boolean getItDirectlyBasedOnPersistAndForm = true;
			// FormComponent's child security is the security of the FormComponent
			if (fe.isFormComponentChild())
			{
				String feName = fe.getName();
				// form component children security access is currently dictated by the root form component component security settings; currently one only has the Security tab in form editors not in form component editors;
				// for example if you have a form that contains a form component component A pointing to form component X that has in it a form component component B that points to form component Y
				// then the children of both X and Y in this case have the same security settings as 'root' form component component which is A;

				// so find the 'root' form component component persist and get it's access rights; this should always be found!
				String formComponentName = feName.substring(0, feName.indexOf('$'));
				for (IPersist p : form.getAllObjectsAsList())
				{
					if (p instanceof IFormElement && formComponentName.equals(((IFormElement)p).getName()))
					{
						elementSecurity = application.getFlattenedSolution().getSecurityAccess(p.getUUID(),
							form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
						getItDirectlyBasedOnPersistAndForm = false;
						break;
					}
				}
				if (getItDirectlyBasedOnPersistAndForm) Debug.warn("'Root' form component including component on form " + form.getName() +
					" was not found when trying to determine access rights for a child of a form component: " + name);
			}
			else if (persist.getParent() instanceof Portal)
			{
				elementSecurity = application.getFlattenedSolution().getSecurityAccess(((Portal)persist.getParent()).getUUID(),
					form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
				getItDirectlyBasedOnPersistAndForm = false;
			}

			if (getItDirectlyBasedOnPersistAndForm)
			{
				elementSecurity = application.getFlattenedSolution().getSecurityAccess(persist.getUUID(),
					form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
			}

			// don't add the component to the form ui if component is not visible due to security settings
			if (!((elementSecurity & IRepository.VIEWABLE) != 0)) return null;
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
			if (!((elementSecurity & IRepository.ACCESSIBLE) != 0)) // element not accessible
			{
				webComponent.setProperty("enabled", false);
			}
			else
			{
				int formSecurity = application.getFlattenedSolution().getSecurityAccess(form.getUUID(),
					form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
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
				IPersist function = application.getFlattenedSolution().getScriptMethod((String)eventValue);
				if (function == null)
				{
					function = application.getFlattenedSolution().searchPersist((String)eventValue);
				}
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
		return Utils.calculateMD5HashBase16(formName + '.' + elementName);
	}

}
