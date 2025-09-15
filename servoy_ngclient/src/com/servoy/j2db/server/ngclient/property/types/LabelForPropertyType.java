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

import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class LabelForPropertyType extends DefaultPropertyType<String>
	implements IPropertyConverterForBrowser<String>, IFormElementToTemplateJSON<String, String>, ISupportTemplateValue<String>
{

	public static final LabelForPropertyType INSTANCE = new LabelForPropertyType();
	public static final String TYPE_NAME = "labelfor";

	protected LabelForPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public String fromJSON(Object newJSONValue, String previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return (String)newJSONValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, String sabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (!Utils.stringIsEmpty(sabloValue) && dataConverterContext != null && dataConverterContext.getWebObject() instanceof WebComponent)
		{
			String name = sabloValue;
			WebComponent wc = (WebComponent)dataConverterContext.getWebObject();
			// first just look directly in the parent of this wc to see if it has there that component
			WebComponent component = wc.getParent().getComponent(name); // this only looks at direct children, not recursively in containers
			IWebFormUI parentForm = wc.findParent(IWebFormUI.class); // this will be the actual root runtime form even if the component is nested in non-foundset-linked form components in form component containers
			if (component == null)
			{
				// if not found, then try to find it in the parent form; maybe this should be already default above (that parent in responsive can really be different then the above one i think)
				component = parentForm.getWebComponent(name); // this looks recursively inside the given container and it's child containers for a component with the given name
				if (component == null && wc instanceof WebFormComponent wfc && wfc.getFormElement().getPersistIfAvailable() instanceof AbstractBase ab)
				{
					// If this web component was from a form component inside a non-foundset-linked form component container (parentFormComponentContainer != null), look at
					// the simple name - in that case we generate something like formComponentContainer1Name$fcPropertyName$elementNameInsideFC
					// for getName()..., so that will not match elementNameInsideFC.
					// When searching it uses the design/simple names from the persists in case of children of form components.
					// It will give prio. to labelFor name matching for 2 components inside the same form component - when available. Otherwise it will match any available one.

					IPersist parentFormComponentContainer = ab.getRuntimeProperty(FormElementHelper.FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER);

					// component was not just found, go over all runtime components of the form
					Collection<WebComponent> components = parentForm.getComponents();
					for (WebComponent comp : components)
					{
						if (comp instanceof WebFormComponent wfcomp && wfcomp.getFormElement().getPersistIfAvailable() instanceof AbstractBase abchild)
						{
							// check the element name of the form component itself. (so this is the plain name you see in the designer)
							String elementName = abchild.getRuntimeProperty(FormElementHelper.FC_CHILD_ELEMENT_NAME_INSIDE_DIRECT_PARENT_FORM_COMPONENT);
							if (name.equals(elementName))
							{
								// if that name equals to what we want, then this could possibly be it.
								component = comp;

								// but check if they both belong to the same form component container parent; if not, do continue searching;
								// if then another is found that has the same name and the same parent then that one is taken - else a
								// previous hit will just be used (so the one from a different parent if that was found)
								if (parentFormComponentContainer == abchild.getRuntimeProperty(FormElementHelper.FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER))
								{
									break;
								}
							}
						}
					}
					if (component != null)
					{
						name = component.getName(); // get the full path-like-separated-with-$ real runtime name again (in case of children of form components)
					}
				}
			}

			writer.value(ComponentFactory.getMarkupId(parentForm.getController().getName(), name));
		}
		else
		{
			writer.value(null);
		}
		return writer;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;

		JSONUtils.addKeyIfPresent(writer, key);
		writer.value(formElementValue);
		return writer;
	}

	@Override
	public boolean valueInTemplate(String object, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false;
	}
}
