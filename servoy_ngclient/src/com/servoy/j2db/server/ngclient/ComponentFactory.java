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

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.Container;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.GlobalMethodValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.RelatedValueList;
import com.servoy.j2db.persistence.AbstractPersistFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ISupportTemplateValue;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * @author lvostinar
 *
 */
public class ComponentFactory
{
	public static WebFormComponent createComponent(IApplication application, IDataAdapterList dataAdapterList, FormElement fe, Container parentToAddTo)
	{
		String name = fe.getName();
		if (name != null)
		{
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

			Map<String, PropertyDescription> valuelistProps = fe.getWebComponentSpec().getProperties(TypesRegistry.getType("valuelist"));
			for (PropertyDescription vlProp : valuelistProps.values())
			{
				int valuelistID = Utils.getAsInteger(fe.getPropertyValue(vlProp.getName()));
				if (valuelistID > 0)
				{
					ValueList val = application.getFlattenedSolution().getValueList(valuelistID);
					if (val != null)
					{
						IValueList valueList;
						switch (val.getValueListType())
						{
							case IValueListConstants.GLOBAL_METHOD_VALUES :
								valueList = new GlobalMethodValueList(application, val);
								break;
							case IValueListConstants.CUSTOM_VALUES :
								String dataproviderID = (String)fe.getPropertyValue((String)vlProp.getConfig());
								String format = null;
								if (dataproviderID != null)
								{
									Map<String, PropertyDescription> properties = fe.getWebComponentSpec().getProperties(TypesRegistry.getType("format"));
									for (PropertyDescription pd : properties.values())
									{
										// compare the config objects for Format and Valuelist properties these are both the "for" dataprovider id property
										if (vlProp.getConfig().equals(pd.getConfig()))
										{
											format = (String)fe.getPropertyValue(pd.getName());
											break;
										}
									}
								}
								ComponentFormat fieldFormat = ComponentFormat.getComponentFormat(format, dataproviderID,
									application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), fe.getForm()), application);
								valueList = new CustomValueList(application, val, val.getCustomValues(),
									(val.getAddEmptyValue() == IValueListConstants.EMPTY_VALUE_ALWAYS), fieldFormat.dpType, fieldFormat.parsedFormat);
								break;
							default :
								valueList = val.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES ? new RelatedValueList(application, val)
									: new DBValueList(application, val);
						}
						webComponent.setProperty(vlProp.getName(), valueList);
					}
				}
				else
				{
					if (fe.getTypeName().equals("svy-typeahead"))
					{
						String dp = (String)fe.getPropertyValue(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName());
						IWebFormUI formUI = (WebFormUI)parentToAddTo;
						if (dp != null && formUI.getController().getTable() != null && formUI.getController().getTable().getColumnType(dp) != 0)
						{
							ColumnBasedValueList vl = new ColumnBasedValueList(application, fe.getForm().getServerName(), fe.getForm().getTableName(),
								(String)fe.getPropertyValue(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName()));
							webComponent.setProperty(vlProp.getName(), vl);
						}
					}
				}
			}

			WebComponentSpecification componentSpec = fe.getWebComponentSpec(false);

			for (String propName : fe.getRawPropertyValues().keySet())
			{
				if (componentSpec.getProperty(propName) == null) continue; //TODO this if should not be necessary. currently in the case of "printable" hidden property
				Object value = fe.getPropertyValueConvertedForWebComponent(propName, webComponent);
				if (value == null) continue;
				fillProperties(fe.getForm(), fe, value, componentSpec.getProperty(propName), (DataAdapterList)dataAdapterList, webComponent, webComponent, "",
					application);
			}

			// overwrite accessible
			if (persist != null && !((access & IRepository.ACCESSIBLE) != 0)) webComponent.setProperty("enabled", false);

			// TODO should this be a part of type conversions for handlers instead?
			for (String eventName : componentSpec.getHandlers().keySet())
			{
				Object eventValue = fe.getPropertyValue(eventName);
				if (eventValue instanceof String)
				{
					UUID uuid = UUID.fromString((String)eventValue);
					try
					{
						webComponent.add(eventName, ((AbstractPersistFactory)ApplicationServerRegistry.get().getLocalRepository()).getElementIdForUUID(uuid));
					}
					catch (RepositoryException e)
					{
						Debug.error(e);
					}
				}
				else if (eventValue instanceof Number && ((Number)eventValue).intValue() > 0)
				{
					webComponent.add(eventName, ((Number)eventValue).intValue());
				}
			}
			// just created, it should have no changes.
			webComponent.clearChanges();
			return webComponent;
		}
		return null;
	}

	/**
	 *  -fe is only needed because of format . It accesses another property value based on the 'for' property (). TODO this FormElement parameter should be analyzed because format accepts a flat property value.
	 *
	 * -level is only needed because the current implementation 'flattens' the dataproviderid's and tagstrings for DAL  .(level should be removed after next changes)
	 *
	 *  -component is the whole component for now ,but it should be the current component node in the runtime component tree (instead of flat properties map)
	 *  -component and componentNode should have been just componentNode (but currently WebCoponent is not nested)
	 */
	public static void fillProperties(Form formElNodeForm, FormElement fe, Object formElementProperty, PropertyDescription propertySpec, DataAdapterList dal,
		WebFormComponent component, Object componentNode, String level, IApplication application)
	{
		// TODO This whole method content I think can be removed when dataprovider, tagstring, ... are implemented as complex types and tree JSON handling is also completely working...
		// except for initial filling of all properties from FormElement into WebComponent
		IPropertyType< ? > type = propertySpec.getType();
		if (propertySpec.getType() instanceof CustomJSONArrayType< ? , ? > && formElementProperty instanceof List &&
			!(formElementProperty instanceof ISmartPropertyValue)) // if it's a special property type that handles directly arrays, it could be a different kind of object
		{
			PropertyDescription arrayElDesc = ((CustomJSONArrayType< ? , ? >)propertySpec.getType()).getCustomJSONTypeDefinition();
			type = arrayElDesc.getType();
			List<Object> processedArray = new ArrayList<>();
			List<Object> fePropertyArray = (List<Object>)formElementProperty;
			for (Object arrayValue : fePropertyArray)
			{
				Object propValue = initFormElementProperty(formElNodeForm, fe, arrayValue, arrayElDesc, dal, component, componentNode, level, application, true);
				switch (type.getName())
				{
					case "dataprovider" : // array of dataprovider is not supported yet (DAL does not support arrays)  , Should be done in initFormElementProperty()
					{
						Debug.error("Array of dataprovider currently not supported dataprovider");
						Object dataproviderID = propValue;
						if (dataproviderID instanceof String)
						{
							dal.add(component, level + (String)dataproviderID, propertySpec.getName());
						}
						break;
					}
					case "tagstring" : // array of taggstring is not supported yet (DAL does not support arrays)
					{
						Debug.error("Array of tagstring currently not supported dataprovider");
						//bind tag expressions
						//for each property with tags ('tagstring' type), add it's dependent tags to the DAL
						if (propValue != null && propValue instanceof String && (((String)propValue).contains("%%")) || ((String)propValue).startsWith("i18n:"))
						{
							dal.addTaggedProperty(component, level + propertySpec.getName(), (String)propValue);
						}
						break;
					}
					default :
					{
						processedArray.add(propValue);
					}

				}
			}
			if (processedArray.size() > 0)
			{
				putInComponentNode(componentNode, propertySpec.getName(), processedArray, propertySpec, component);
			}
		}
		else
		{
			Object propValue = initFormElementProperty(formElNodeForm, fe, formElementProperty, propertySpec, dal, component, componentNode, level,
				application, false);
			String propName = propertySpec.getName();
			switch (type.getName())
			{
				case "dataprovider" : // array of dataprovider is not supported yet (DAL does not support arrays)
				{
					Object dataproviderID = formElementProperty;
					if (dataproviderID instanceof String)
					{
						dal.add(component, (String)dataproviderID, level + propName);
						return;
					}
					break;
				}
				case "tagstring" : // array of taggstring is not supported yet (DAL does not support arrays)
				{
					//bind tag expressions
					//for each property with tags ('tagstring' type), add it's dependent tags to the DAL
					if (propValue != null && propValue instanceof String && ((String)propValue).contains("%%"))
					{
						dal.addTaggedProperty(component, level + propName, (String)propValue);
						return;
					}
					break;
				}
				case "valuelist" : // skip valuelistID , it is handled elsewhere (should be changed to be handled here?)
					return;
				default :
					break;
			}
			if (propValue != null)
			{
				putInComponentNode(componentNode, propName, propValue, propertySpec, component); //TODO
			}
		}
	}

	/**
	 * TEMPORARY FUNCTION until we move to nested web component tree , with each node having semantics (PropertyType)
	 *  Webcomponent will be a tree
	 */
	private static void putInComponentNode(Object componentNode, String propName, Object propValue, PropertyDescription propertySpec, WebFormComponent component)
	{
		// TODO should this just a a property.property.property = value called to WebFormComponent?
		if (componentNode instanceof WebFormComponent)
		{
			boolean templatevalue = true;
			if (propertySpec.getType() instanceof ISupportTemplateValue)
			{
				templatevalue = ((ISupportTemplateValue)propertySpec.getType()).valueInTemplate(propValue);
			}
			if (templatevalue)
			{
				((WebFormComponent)componentNode).setDefaultProperty(propName, propValue);
			}
			else
			{
				((WebFormComponent)componentNode).setProperty(propName, propValue);
			}
		}
		else
		{
			// this is now done (the wrapping) inside CustomJSONArrayType and CustomJSONObject type
//			// now we need to convert it ourselfs. because this map will be internal to the WebFormComponent so has to have wrapper values.
//			if (propertySpec != null && propertySpec.getType() instanceof IWrapperType< ? , ? >)
//			{
//				propValue = ((IWrapperType)propertySpec.getType()).wrap(propValue, null, new DataConverterContext(propertySpec, component));
//			}
			((Map)componentNode).put(propName, propValue);
		}
	}

	/**
	 * This method turns a design-time property into a Runtime Object that will be used as that property.
	 *  TODO merge component and component node remove isarrayElement parameter
	 * @return
	 */
	private static Object initFormElementProperty(Form formElNodeForm, FormElement fe, Object formElementProperty, PropertyDescription propertySpec,
		DataAdapterList dal, WebFormComponent component, Object componentNode, String level, IApplication application, boolean isArrayElement)
	{
		// TODO This whole method I think should be removed when dataprovider, tagstring, ... are implemented as complex types and tree JSON handling is also completely working...
		Object ret = null;
		switch (propertySpec.getType().getName())
		{
			case "format" :
			{
				Object propValue = formElementProperty;
				if (propValue instanceof String)
				{
					// get dataproviderId
					String dataproviderId = (String)fe.getPropertyValue((String)propertySpec.getConfig());
					ComponentFormat format = ComponentFormat.getComponentFormat((String)propValue, dataproviderId,
						application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), formElNodeForm), application);
					ret = format;
				}
				break;
			}
			case "bean" :
			{
				Object propValue = formElementProperty;
				if (propValue instanceof String)
				{
					ret = ComponentFactory.getMarkupId(fe.getName(), (String)propValue);
				}
				break;
			}
			case "valuelist" : // skip valuelistID , it is handled elsewhere (should be changed to be handled here?)
				break;
			default :
			{
				if ((propertySpec.getType() instanceof ICustomType) && ((ICustomType)propertySpec.getType()).getCustomJSONTypeDefinition() != null &&
					formElementProperty instanceof Map && !(formElementProperty instanceof ISmartPropertyValue))
				{
					// TODO Remove this when pure tree-like JSON properties which use complex types in leafs are operational (so no need for flattening them any more)
					String innerLevelpropName = level + propertySpec.getName();
					Map<String, PropertyDescription> props = ((Map<String, PropertyDescription>)formElementProperty);
					Map<String, Object> newComponentNode = new HashMap<>();
					PropertyDescription localPropertyType = ((ICustomType)propertySpec.getType()).getCustomJSONTypeDefinition();

					for (String prop : props.keySet())
					{
						PropertyDescription localPropertyDescription = localPropertyType.getProperty(prop);
						fillProperties(formElNodeForm, fe, props.get(prop), localPropertyDescription, dal, component, newComponentNode, isArrayElement ? ""
							: innerLevelpropName + ".", application);
					}
					ret = newComponentNode;
					break;
				}
				else
				{
					ret = formElementProperty;
				}
			}
		}
		return ret;
	}

	// todo identity key? SolutionModel persist shouldn't be cached at all?
	private static ConcurrentMap<IPersist, FormElement> persistWrappers = new ConcurrentHashMap<IPersist, FormElement>();

	/**
	 * @param iterator
	 */
	public static List<FormElement> getFormElements(Iterator<IPersist> iterator, IServoyDataConverterContext context)
	{
		if (Boolean.valueOf(Settings.getInstance().getProperty("servoy.internal.reloadSpecsAllTheTime", "false")).booleanValue())
		{
			WebComponentSpecProvider.reload();
		}
		List<FormElement> lst = new ArrayList<>();
		while (iterator.hasNext())
		{
			IPersist persist = iterator.next();
			if (persist instanceof IFormElement)
			{
				lst.add(getFormElement((IFormElement)persist, context, null));
			}
		}
		return lst;
	}

	public static FormElement getFormElement(IFormElement formElement, IServoyDataConverterContext context, PropertyPath propertyPath)
	{
		// dont cache if solution model is used (media,valuelist,relations can be changed for a none changed element)
		if (context.getSolution().getSolutionCopy(false) != null)
		{
			if (propertyPath == null)
			{
				propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
			}
			if (formElement instanceof ListViewPortal) return createListViewPortalFormElement((ListViewPortal)formElement, context);
			else return new FormElement(formElement, context, propertyPath);
		}
		FormElement persistWrapper = persistWrappers.get(formElement);
		if (persistWrapper == null)
		{
			if (propertyPath == null)
			{
				propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
			}
			if (formElement instanceof ListViewPortal) persistWrapper = createListViewPortalFormElement((ListViewPortal)formElement, context);
			else persistWrapper = new FormElement(formElement, context, propertyPath);
			FormElement existing = persistWrappers.putIfAbsent(formElement, persistWrapper);
			if (existing != null)
			{
				persistWrapper = existing;
			}
		}
		return persistWrapper;
	}

	private static FormElement createListViewPortalFormElement(ListViewPortal listViewPortal, IServoyDataConverterContext context)
	{
		Form form = listViewPortal.getForm();
		Part bodyPart = null;
		for (Part prt : Utils.iterate(form.getParts()))
		{
			if (prt.getPartType() == Part.BODY)
			{
				bodyPart = prt;
				break;
			}
		}
		if (bodyPart != null)
		{
			try
			{
				String name = "svy_lvp_" + form.getName();

				JSONObject portal = new JSONObject();
				portal.put("name", name);
				portal.put("multiLine", true);
				portal.put("rowHeight", bodyPart.getHeight());
				portal.put("anchors", IAnchorConstants.ALL);
				JSONObject location = new JSONObject();
				location.put("x", 0);
				location.put("y", 0);
				portal.put("location", location);
				JSONObject size = new JSONObject();
				size.put("width", form.getWidth());
				size.put("height", bodyPart.getHeight());
				portal.put("size", size);
				portal.put("visible", listViewPortal.getVisible());
				portal.put("enabled", listViewPortal.getEnabled());
				portal.put("childElements", new JSONArray()); // empty contents; will be updated afterwards directly with form element values for components

				JSONObject relatedFoundset = new JSONObject();
				relatedFoundset.put("foundsetSelector", "");
				portal.put("relatedFoundset", relatedFoundset);

				PropertyPath propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
				FormElement portalFormElement = new FormElement("svy-portal", portal, form, name, context, propertyPath);

				PropertyDescription pd = portalFormElement.getWebComponentSpec().getProperties().get("childElements");
				if (pd != null) pd = ((CustomJSONArrayType< ? , ? >)pd.getType()).getCustomJSONTypeDefinition();
				if (pd == null)
				{
					Debug.error(new RuntimeException("Cannot find component definition special type to use for portal."));
					return null;
				}
				ComponentPropertyType type = ((ComponentPropertyType)pd.getType());

				Map<String, Object> portalFormElementProperties = new HashMap<>(portalFormElement.getRawPropertyValues());
				// now put real child component form element values in "childElements"
				int startPos = form.getPartStartYPos(bodyPart.getID());
				int endPos = bodyPart.getHeight();
				Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				List<Object> children = new ArrayList<>(); // contains actually ComponentTypeFormElementValue objects
				propertyPath.add(portalFormElement.getName());
				propertyPath.add("childElements");
				while (it.hasNext())
				{
					IPersist persist = it.next();
					if (persist instanceof IFormElement)
					{
						Point loc = ((IFormElement)persist).getLocation();
						if (startPos <= loc.y && endPos >= loc.y)
						{
							propertyPath.add(children.size());
							FormElement fe = ComponentFactory.getFormElement((IFormElement)persist, context, propertyPath);
							children.add(type.getFormElementValue(null, pd, propertyPath, fe));
							propertyPath.backOneLevel();
						}
					}
				}
				propertyPath.backOneLevel();
				propertyPath.backOneLevel();
				portalFormElementProperties.put("childElements", children.toArray());
				portalFormElement.updatePropertyValuesDontUse(portalFormElementProperties);

				return portalFormElement;
			}
			catch (JSONException ex)
			{
				Debug.error("Cannot create list view portal component", ex);
			}
		}

		return null;
	}

	public static String getMarkupId(String formName, String elementName)
	{
		return Utils.calculateMD5HashBase16(formName + elementName);
	}

	public static void reload()
	{
		persistWrappers.clear();
		WebComponentSpecProvider.reload();
	}

	/**
	 * @param valuelist
	 * @return
	 */
	public static boolean isSingleValue(ValueList valuelist)
	{
		if (valuelist != null && valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES &&
			valuelist.getAddEmptyValue() != IValueListConstants.EMPTY_VALUE_ALWAYS && valuelist.getCustomValues() != null &&
			!valuelist.getCustomValues().contains("\n") && !valuelist.getCustomValues().contains("\r"))
		{
			return true;
		}
		return false;
	}


}
