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

package com.servoy.j2db.server.ngclient.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.sablo.WebComponent;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.server.ngclient.DefaultNavigator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.Utils;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Wrap some objects for form file generation using freemarker templates.
 *
 * @author rgansevles
 *
 */
public class FormTemplateObjectWrapper extends DefaultObjectWrapper
{
	private final IServoyDataConverterContext context;
	private final boolean useControllerProvider;
	private Form flattenedForm;
	private final boolean design;
	private final WebFormUI formUI;
	private final JSONObject runtimePropertiesForNG1;
	private final Map<Object, TemplateModel> wrapperCache = new HashMap<>();

	public FormTemplateObjectWrapper(IServoyDataConverterContext context, boolean useControllerProvider, boolean design, boolean ng1WithFTLTemplatePreparation)
	{
		this.context = context;
		this.useControllerProvider = useControllerProvider;
		this.design = design;
		formUI = (context.getForm() != null && context.getForm().getFormUI() instanceof WebFormUI) ? (WebFormUI)context.getForm().getFormUI() : null;
		if (formUI != null && ng1WithFTLTemplatePreparation)
		{
			String componentProps = NGUtils.formComponentPropertiesToString(formUI, FullValueToJSONConverter.INSTANCE);
			runtimePropertiesForNG1 = new JSONObject(componentProps);
		}
		else runtimePropertiesForNG1 = null;
	}

	@Override
	public TemplateModel wrap(Object obj) throws TemplateModelException
	{
		TemplateModel model = wrapperCache.get(obj);
		if (model != null) return model;
		Object wrapped;
		if (obj instanceof Form)
		{
			wrapped = getFormWrapper((Form)obj);
		}
		else if (obj instanceof Object[])
		{
			this.flattenedForm = context.getSolution().getFlattenedForm((Form)((Object[])obj)[0]);
			wrapped = new FormWrapper(flattenedForm, (String)((Object[])obj)[1], useControllerProvider, context, design,
				runtimePropertiesForNG1 != null ? runtimePropertiesForNG1.getJSONObject("") : null);
		}
		else if (obj == DefaultNavigator.INSTANCE)
		{
			wrapped = new FormElement(DefaultNavigator.INSTANCE, context.getSolution(), new PropertyPath(), design);
			JSONObject object = runtimePropertiesForNG1 != null ? runtimePropertiesForNG1.optJSONObject(((FormElement)wrapped).getName()) : null;
			if (object != null)
			{
				wrapped = new FormElementContext((FormElement)wrapped, context, object);
			}
		}
		else if (obj instanceof Part)
		{
			wrapped = new PartWrapper((Part)obj, flattenedForm, context, design);
		}
		else if (obj instanceof IFormElement)
		{
			FormElement fe = null;
			if (formUI != null)
			{
				List<FormElement> cachedFormElements = formUI.getFormElements();
				for (FormElement cachedFE : cachedFormElements)
				{
					if (Utils.equalObjects(cachedFE.getPersistIfAvailable(), obj))
					{
						fe = cachedFE;
						break;
					}
				}
				if (fe == null)
				{
					Form parentForm = (Form)((IFormElement)obj).getAncestor(IRepository.FORMS);
					if (parentForm != null && parentForm.isFormComponent())
					{
						for (WebComponent webComponent : formUI.getAllComponents())
						{
							if (webComponent instanceof WebFormComponent)
							{
								FormElement cachedFE = ((WebFormComponent)webComponent).getFormElement();
								if (Utils.equalObjects(cachedFE.getPersistIfAvailable(), obj))
								{
									fe = cachedFE;
									break;
								}
							}
						}
					}
				}
			}
			FormElement formElement = fe != null ? fe : FormElementHelper.INSTANCE.getFormElement((IFormElement)obj, context.getSolution(), null, false);
			JSONObject object = runtimePropertiesForNG1 != null ? runtimePropertiesForNG1.optJSONObject(formElement.getName()) : null;
			wrapped = new FormElementContext(formElement, context, object);
		}
		else
		{
			wrapped = obj;
		}
		TemplateModel wrap = super.wrap(wrapped);
		wrapperCache.put(obj, wrap);
		return wrap;
	}

	public FormWrapper getFormWrapper(Form frm)
	{
		this.flattenedForm = context.getSolution().getFlattenedForm(frm);
		return new FormWrapper(flattenedForm, null, useControllerProvider, context, design,
			runtimePropertiesForNG1 != null ? runtimePropertiesForNG1.getJSONObject("") : null);
	}
}
