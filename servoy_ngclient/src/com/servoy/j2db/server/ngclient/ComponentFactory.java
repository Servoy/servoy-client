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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.sablo.Container;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ISupportTemplateValue;
import com.servoy.j2db.server.ngclient.property.types.NGEnabledSabloValue;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;


/**
 * @author lvostinar
 *
 */
public class ComponentFactory
{

	@SuppressWarnings("nls")
	public static WebFormComponent createComponent(IApplication application, IDataAdapterList dataAdapterList, FormElement fe, Container parentToAddTo,
		Form form)
	{
		// TODO anything to do here for custom special types?
		WebFormComponent webComponent = new WebFormComponent(fe.getName(), fe, dataAdapterList);
		if (parentToAddTo != null) parentToAddTo.add(webComponent);

		String formElementName = fe.getName();
		IPersist persist = fe.getPersistIfAvailable();
		int elementSecurity = 0;
		if (persist != null)
		{
			boolean getItDirectlyBasedOnPersistAndForm = true;
			if (fe.isFormComponentChild())
			{
				Form frm = persist.getAncestor(Form.class);
				String elementName = ((AbstractBase)persist).getRuntimeProperty(FormElementHelper.FORM_COMPONENT_ELEMENT_NAME);
				if (frm != null && elementName != null)
				{
					for (IPersist p : frm.getFlattenedFormElementsAndLayoutContainers())
					{
						if (p instanceof IFormElement && Utils.equalObjects(((IFormElement)p).getName(), elementName))
						{
							elementSecurity = application.getFlattenedSolution().getSecurityAccess(p.getUUID(),
								form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
							getItDirectlyBasedOnPersistAndForm = false;
							break;
						}
					}
				}
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

			if (!((elementSecurity & IRepository.VIEWABLE) != 0))
			{
				webComponent.setVisible(false);
			}
		}

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
				webComponent.setProperty(WebFormUI.ENABLED, false);
				Object enableValue = webComponent.getRawPropertyValue(WebFormUI.ENABLED);
				if (enableValue instanceof NGEnabledSabloValue)
				{
					((NGEnabledSabloValue)enableValue).setAccessible(false);
				}
			}
			else
			{
				int formSecurity = application.getFlattenedSolution().getSecurityAccess(form.getUUID(),
					form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
				if (!((formSecurity & IRepository.ACCESSIBLE) != 0)) // form not accessible
				{
					webComponent.setProperty(WebFormUI.ENABLED, false);
					Object enableValue = webComponent.getRawPropertyValue(WebFormUI.ENABLED);
					if (enableValue instanceof NGEnabledSabloValue)
					{
						((NGEnabledSabloValue)enableValue).setAccessible(false);
					}
				}
			}
		}

		boolean foundOnDataChangeInDPConfigFromSpec[] = new boolean[] { false };

		Collection<PropertyDescription> properties = new ArrayList<PropertyDescription>();
		properties.addAll(componentSpec.getProperties(DataproviderPropertyType.INSTANCE, true));
		Map<String, ICustomType< ? >> declaredCustomObjectTypes = componentSpec.getDeclaredCustomObjectTypes();
		for (IPropertyType< ? > pt : declaredCustomObjectTypes.values())
		{
			if (pt instanceof CustomJSONPropertyType< ? >)
			{
				PropertyDescription customJSONSpec = ((CustomJSONPropertyType< ? >)pt).getCustomJSONTypeDefinition();
				properties.addAll(customJSONSpec.getProperties(DataproviderPropertyType.INSTANCE, true));
			}
		}

		properties.forEach((propertyFromSpec) -> {
			// the property type found here is for a 'dataprovider' property from the spec file of this component
			Object configOfDPOrFoundsetLinkedDP = propertyFromSpec.getConfig();
			DataproviderConfig dpConfig;

			if (configOfDPOrFoundsetLinkedDP instanceof FoundsetLinkedConfig)
				dpConfig = (DataproviderConfig)((FoundsetLinkedConfig)configOfDPOrFoundsetLinkedDP).getWrappedConfig();
			else dpConfig = (DataproviderConfig)configOfDPOrFoundsetLinkedDP;

			if (dpConfig.getOnDataChange() != null)
			{
				foundOnDataChangeInDPConfigFromSpec[0] = true;
				webComponent.add(dpConfig.getOnDataChange(), form.getOnElementDataChangeMethodID());
			}
		});

		// TODO should this be a part of type conversions for handlers instead?
		for (String eventName : componentSpec.getHandlers().keySet())
		{
			Object eventValue = fe.getPropertyValue(eventName);
			if (eventValue instanceof String eventValueString)
			{
				webComponent.add(eventName, eventValueString);
			}
			else if (eventValue instanceof Number && ((Number)eventValue).intValue() > 0)
			{
				Debug.warn("Event handler for " + eventName + " with value '" + eventValue + "' is a number (form " + form + ", form element " +
					formElementName + ")");
			}
			else if (Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName()))
			{
				webComponent.add(eventName, form.getOnElementFocusGainedMethodID());
			}
			else if (Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName()))
			{
				webComponent.add(eventName, form.getOnElementFocusLostMethodID());
			}
			else if (!foundOnDataChangeInDPConfigFromSpec[0] &&
				Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName()))
			{
				// legacy behavior - based on hard-coded handler name (of component)
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
		return 's' + Utils.calculateMD5HashBase16(formName + '.' + elementName);
	}

}
