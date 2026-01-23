/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.ngclient.utils;

import java.util.Collection;
import java.util.function.Function;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Pair;

/**
 * Utility class for working with form components. (form component containers & their FC properties, Form instances that are form components etc.)
 *
 * @author acostescu
 */
public class FormComponentUtils
{

	public static record FCCCHandlerArgs<RPT>(IFormElement childFe, WebObjectSpecification childSpecIfAvailable,
		IFormElement parentComponentThatUsesAFormComponentInIt,
		RPT handlerCallState)
	{
	}

	/**
	 * Goes deep through form component components and gives all the form elements that it finds to the handler function.
	 * If the given "componentThatUsesAFormComponentInIt" is not a form component component, it will do nothing.
	 *
	 * @param handler it will be called once for each child (no matter how deeply nested) that is found; the first argument contains the child and, if it's a web component
	 *           that has a .spec available it's webObjectSpecification; the second argument is the old handlerState; the return value should be the new handler state + a boolean
	 *           that says if searching for children should continue or not.
	 */
	public static <RPT> void addFormComponentComponentChildren(IFormElement componentThatUsesAFormComponentInIt,
		FlattenedSolution fs, boolean includeListFCCs,
		Function<FCCCHandlerArgs<RPT>, Pair<RPT, Boolean>> handler, RPT handlerState, boolean inDesigner)
	{
		addFormComponentComponentChildrenInternal(componentThatUsesAFormComponentInIt, fs, includeListFCCs,
			handler, handlerState, inDesigner);
	}

	private static <RPT> boolean addFormComponentComponentChildrenInternal(IFormElement componentThatUsesAFormComponentInIt,
		FlattenedSolution fs, boolean includeListFCCs,
		Function<FCCCHandlerArgs<RPT>, Pair<RPT, Boolean>> handler, RPT handlerState, boolean inDesigner)
	{
		String componentType = FormTemplateGenerator.getComponentTypeName(componentThatUsesAFormComponentInIt);
		WebObjectSpecification specification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentType);
		if (specification == null) return true; // it's not even a web component; it's probably some legacy form element

		Collection<PropertyDescription> propertiesOfFormComponentType = specification.getProperties(FormComponentPropertyType.INSTANCE);

		if (propertiesOfFormComponentType.size() > 0 && (includeListFCCs || !FormElementHelper.isListFormComponent(propertiesOfFormComponentType)))
		{
			// so it is a form component component that we care about...
			FormElement elementThatUsesAFormComponentInIt = FormElementHelper.INSTANCE.getFormElement(componentThatUsesAFormComponentInIt, fs, null,
				inDesigner);
			for (PropertyDescription pd : propertiesOfFormComponentType)
			{
				Object rawPropertyValueOfFEsFCProperty = elementThatUsesAFormComponentInIt.getPropertyValue(pd.getName());
				Form formComponent = FormComponentPropertyType.INSTANCE.getForm(rawPropertyValueOfFEsFCProperty, fs);
				if (formComponent == null) continue;

				FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(elementThatUsesAFormComponentInIt, pd,
					(JSONObject)rawPropertyValueOfFEsFCProperty, formComponent, fs);
				for (FormElement element : cache.getFormComponentElements())
				{
					IPersist p = element.getPersistIfAvailable();
					if (p instanceof IFormElement pfe)
					{
						// stuff like [7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8, containedForm, formComponent2, containedForm, n1]
						String[] feComponentAndPropertyNamePath = ((AbstractBase)p)
							.getRuntimeProperty(FormElementHelper.FC_COMPONENT_AND_PROPERTY_NAME_PATH);

						if (feComponentAndPropertyNamePath != null && feComponentAndPropertyNamePath.length > 2 &&
							!feComponentAndPropertyNamePath[feComponentAndPropertyNamePath.length - 1].startsWith(FormElement.SVY_NAME_PREFIX))
						{
							if (FormTemplateGenerator.isWebcomponentBean(p))
							{
								IBasicWebComponent innerWebComponent = (IBasicWebComponent)p;
								String innerComponentType = FormTemplateGenerator.getComponentTypeName(innerWebComponent);
								WebObjectSpecification innerSpecification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
									innerComponentType);

								if (innerSpecification != null)
								{
									Pair<RPT, Boolean> newHandlerGivenStateAndContinue = handler
										.apply(new FCCCHandlerArgs<>(pfe, innerSpecification, componentThatUsesAFormComponentInIt, handlerState));
									// stop searching if handler says so
									if (newHandlerGivenStateAndContinue != null && !newHandlerGivenStateAndContinue.getRight().booleanValue()) return false;

									boolean continueSearching = addFormComponentComponentChildrenInternal(innerWebComponent, fs,
										includeListFCCs, handler,
										newHandlerGivenStateAndContinue != null ? newHandlerGivenStateAndContinue.getLeft() : null,
										inDesigner);
									if (!continueSearching) return false;
								}
							}
							else
							{
								// not a web component; could be a legacy button/label... for example
								Pair<RPT, Boolean> newHandlerGivenStateAndContinue = handler
									.apply(new FCCCHandlerArgs<>(pfe, null, componentThatUsesAFormComponentInIt, handlerState));

								// stop searching if handler says so
								if (newHandlerGivenStateAndContinue != null && !newHandlerGivenStateAndContinue.getRight().booleanValue()) return false;
							}
						}
					}
				}
			}
		}

		return true;
	}

}
