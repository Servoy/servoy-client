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

import java.awt.Point;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;

import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.RuntimeProperty;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabSeqComparator;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGTabSeqPropertyType;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Class used to cache FormElements that can be cached.
 * Also contains more code that is useful when working with FormElements.
 *
 * @author acostescu
 */
public class FormElementHelper implements IFormElementCache
{
	public final static RuntimeProperty<String> FORM_COMPONENT_TEMPLATE_NAME = new RuntimeProperty<String>()
	{
	};
	public final static FormElementHelper INSTANCE = new FormElementHelper();

	// todo identity key? SolutionModel persist shouldn't be cached at all?
	private final ConcurrentMap<String, FlattenedSolution> globalFlattendSolutions = new ConcurrentHashMap<>();
	private final ConcurrentMap<IPersist, FormElement> persistWrappers = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Map<TabSeqProperty, Integer>> formTabSequences = new ConcurrentHashMap<>();

	private final ConcurrentMap<UUID, Map<String, FormComponentCache>> formComponentElements = new ConcurrentHashMap<>();

	private final Map<IPersist, Map<UUID, UUID>> formComponentElementsUUIDS = new WeakHashMap<>();

	public List<FormElement> getFormElements(Iterator<IPersist> iterator, IServoyDataConverterContext context)
	{
		List<FormElement> lst = new ArrayList<>();
		while (iterator.hasNext())
		{
			IPersist persist = iterator.next();
			if (persist instanceof IFormElement)
			{
				lst.add(getFormElement((IFormElement)persist, context.getSolution(), null, false));
			}
		}
		return lst;
	}

	/**
	 * @param formElement
	 * @param pd
	 * @param formElementValue
	 * @param form
	 * @param fs
	 * @return
	 */
	public FormComponentCache getFormComponentCache(INGFormElement formElement, PropertyDescription pd, JSONObject formElementValue, Form form,
		FlattenedSolution fs)
	{
		final List<FormElement> noneCacheableElements = testForDesignAndSolutionModel(formElement, pd, formElementValue, form, fs);
		if (noneCacheableElements != null)
		{
			return generateFormComponentCacheObject(formElement, pd, form, fs, noneCacheableElements);
		}

		return getFormComponentFromCache(formElement, pd, formElementValue, form, fs);
	}


	private FormComponentCache getFormComponentFromCache(INGFormElement parentElement, PropertyDescription pd, JSONObject json, Form frm, FlattenedSolution fs)
	{
		Map<String, FormComponentCache> map = formComponentElements.get(parentElement.getPersistIfAvailable().getUUID());
		if (map == null)
		{
			map = new ConcurrentHashMap<>();
			formComponentElements.put(parentElement.getPersistIfAvailable().getUUID(), map);
		}
		FormComponentCache fcCache = map.get(pd.getName());
		if (fcCache == null)
		{
			final List<FormElement> list = generateFormComponentElements(parentElement, pd, json, frm, fs);
			fcCache = generateFormComponentCacheObject(parentElement, pd, frm, fs, list);
			map.put(pd.getName(), fcCache);
		}
		return fcCache;
	}

	/**
	 * @param pd
	 * @param parentElement
	 * @param frm
	 * @param fs
	 * @param list
	 * @return
	 */
	private FormComponentCache generateFormComponentCacheObject(INGFormElement parentElement, PropertyDescription pd, Form frm, FlattenedSolution fs,
		final List<FormElement> list)
	{
		IFormElementCache cache = new IFormElementCache()
		{
			@Override
			public FormElement getFormElement(IFormElement component, FlattenedSolution flattendSol, PropertyPath path, boolean design)
			{
				for (FormElement formElement : list)
				{
					if (component.getID() == formElement.getPersistIfAvailable().getID())
					{
						return formElement;
					}
				}
				return FormElementHelper.INSTANCE.getFormElement(component, flattendSol, path, design);
			}
		};
		String template = FormLayoutGenerator.generateFormComponent(frm, fs, cache);
		return new FormComponentCache(parentElement, pd, list, template);
	}

	/**
	 * @param parentElement
	 * @param pd
	 * @param json
	 * @param frm
	 * @param fs
	 * @param excludedComponents
	 * @return
	 */
	private List<FormElement> testForDesignAndSolutionModel(INGFormElement parentElement, PropertyDescription pd, JSONObject json, Form frm,
		FlattenedSolution fs)
	{
		// for designer always just generate it
		if (parentElement.getDesignId() != null) return generateFormComponentElements(parentElement, pd, json, frm, fs);

		// if the form of the main form component is a solution copy then don't cache.
		Solution solutionCopy = fs.getSolutionCopy(false);
		if (solutionCopy != null && solutionCopy.getForm(parentElement.getForm().getName()) != null)
			return generateFormComponentElements(parentElement, pd, json, frm, fs);
		return null;
	}

	/**
	 * @param parentElement
	 * @param pd
	 * @param json
	 * @param frm
	 * @param fs
	 * @param excludedComponents
	 * @return
	 */
	private List<FormElement> generateFormComponentElements(INGFormElement parent, PropertyDescription pd, JSONObject json, Form frm, FlattenedSolution fs)
	{
		List<FormElement> elements = new ArrayList<>();
		List<IFormElement> formelements = fs.getFlattenedForm(frm).getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		for (IFormElement element : formelements)
		{
			if (isSecurityVisible(element, fs))
			{
				element = (IFormElement)((AbstractBase)element).clonePersist();
				// we kind of want to have this element a new uuid, but then it is very hard to get it stable.
				UUID newElementUUID = null;
				synchronized (formComponentElementsUUIDS)
				{
					Map<UUID, UUID> map = formComponentElementsUUIDS.get(parent.getPersistIfAvailable());
					if (map == null)
					{
						map = new HashMap<>();
						formComponentElementsUUIDS.put(parent.getPersistIfAvailable(), map);
					}
					newElementUUID = map.get(element.getUUID());
					if (newElementUUID == null)
					{
						newElementUUID = UUID.randomUUID();
						map.put(element.getUUID(), newElementUUID);
					}
				}
				((AbstractBase)element).resetUUID(newElementUUID);
				String elementName = element.getName();
				if (elementName == null)
				{
					elementName = FormElement.SVY_NAME_PREFIX + String.valueOf(element.getID());
				}
				((AbstractBase)element).setRuntimeProperty(FORM_COMPONENT_TEMPLATE_NAME, elementName);
				JSONObject elementJson = json.optJSONObject(elementName);
				if (elementJson != null)
				{
					Map<String, Method> methods = RepositoryHelper.getSetters(element);
					WebObjectSpecification legacySpec = FormTemplateGenerator.getWebObjectSpecification(element);
					for (String key : elementJson.keySet())
					{
						Object val = elementJson.get(key);
						if (val != null && methods.get(key) != null)
						{
							Method method = methods.get(key);
							Class< ? > paramType = method.getParameterTypes()[0];
							if (!paramType.isAssignableFrom(val.getClass()) && !(paramType.isPrimitive() && val instanceof Number))
							{
								PropertyDescription property = legacySpec.getProperty(key);
								if (property != null && property.getType() instanceof IDesignValueConverter)
								{
									val = ((IDesignValueConverter)property.getType()).fromDesignValue(val, property);
								}
								else
								{
									// will not fit, very likely a uuid that should be an int.
									UUID uuid = Utils.getAsUUID(val, false);
									if (uuid != null)
									{
										IPersist found = fs.searchPersist(uuid);
										if (found != null) val = Integer.valueOf(found.getID());
									}
								}
							}
						}
						if (val instanceof JSONObject && ((AbstractBase)element).getProperty(key) instanceof JSONObject)
						{
							// if both are json (like a nested form) then merge it in.
							ServoyJSONObject.mergeAndDeepCloneJSON((JSONObject)val, (JSONObject)((AbstractBase)element).getProperty(key));
						}
						else if (val instanceof String && StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName().equals(key) &&
							((AbstractBase)element).getCustomProperties() != null)
						{
							// custom properties needs to be merged in..
							JSONObject original = new ServoyJSONObject(((AbstractBase)element).getCustomProperties(), true);
							ServoyJSONObject.mergeAndDeepCloneJSON(new ServoyJSONObject((String)val, true), original);
							((AbstractBase)element).setCustomProperties(ServoyJSONObject.toString(original, true, true, true));
						}
						else((AbstractBase)element).setProperty(key, val);
					}
				}
				String name = parent.getDesignId() != null ? parent.getDesignId() : parent.getName();
				element.setName(name != null ? (name + '$' + pd.getName() + '$' + elementName) : elementName);
				elements.add(new FormElement(element, fs, new PropertyPath(), parent.getDesignId() != null));
			}
		}
		return elements;
	}

	private boolean isSecurityVisible(IPersist persist, FlattenedSolution fs)
	{
		int access = fs.getSecurityAccess(persist.getUUID());
		boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
		return b_visible;
	}

	public FormElement getFormElement(IFormElement formElement, FlattenedSolution fs, PropertyPath propertyPath, final boolean designer)
	{
		// dont cache if solution model is used (media,valuelist,relations can be changed for a none changed element)
		if (designer || (fs.getSolutionCopy(false) != null) || ((AbstractBase)formElement).getRuntimeProperty(FORM_COMPONENT_TEMPLATE_NAME) != null)
		{
			if (formElement instanceof BodyPortal) return createBodyPortalFormElement((BodyPortal)formElement, fs, designer);
			else return new FormElement(formElement, fs, propertyPath == null ? new PropertyPath().setShouldAddElementName() : propertyPath, designer);
		}
		FormElement persistWrapper = persistWrappers.get(formElement);
		if (persistWrapper == null)
		{
			if (propertyPath == null)
			{
				propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
			}
			if (formElement instanceof BodyPortal)
				persistWrapper = createBodyPortalFormElement((BodyPortal)formElement, getSharedFlattenedSolution(fs), designer);
			else persistWrapper = new FormElement(formElement, getSharedFlattenedSolution(fs), propertyPath, designer);
			FormElement existing = persistWrappers.putIfAbsent(formElement, persistWrapper);
			if (existing != null)
			{
				persistWrapper = existing;
			}
		}
		return persistWrapper;
	}

	private FlattenedSolution getSharedFlattenedSolution(FlattenedSolution fs)
	{
		FlattenedSolution flattenedSolution = globalFlattendSolutions.get(fs.getName());
		if (flattenedSolution == null)
		{
			try
			{
				flattenedSolution = new FlattenedSolution(true);
				flattenedSolution.setSolution(fs.getMainSolutionMetaData(), false, true,
					new AbstractActiveSolutionHandler(ApplicationServerRegistry.getService(IApplicationServer.class))
					{
						@Override
						public IRepository getRepository()
						{
							return ApplicationServerRegistry.get().getLocalRepository();
						}
					});
				FlattenedSolution alreadyCreated = globalFlattendSolutions.putIfAbsent(flattenedSolution.getName(), flattenedSolution);
				if (alreadyCreated != null)
				{
					flattenedSolution.close(null);
					flattenedSolution = alreadyCreated;
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException("Can't create FlattenedSolution for: " + fs, e);
			}
		}
		return flattenedSolution;
	}

	private FormElement createBodyPortalFormElement(BodyPortal listViewPortal, FlattenedSolution fs, final boolean isInDesigner)
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
				int startPos = form.getPartStartYPos(bodyPart.getID());
				int endPos = bodyPart.getHeight();
				int bodyheight = endPos - startPos;

				JSONObject portal = new JSONObject();
				portal.put("name", name);
				portal.put("multiLine", !listViewPortal.isTableview());
				portal.put("rowHeight", !listViewPortal.isTableview() ? bodyheight : getRowHeight(form));
				portal.put("scrollbars", form.getScrollbars());
				if (listViewPortal.isTableview())
				{
					int headerHeight = 30;
					if (form.hasPart(Part.HEADER))
					{
						headerHeight = 0;
					}
					portal.put("headerHeight", headerHeight);
					portal.put("sortable", form.getOnSortCmdMethodID() != -1);
				}
				portal.put("readOnlyMode", form.getNgReadOnlyMode());

				JSONObject location = new JSONObject();
				location.put("x", 0);
				location.put("y", isInDesigner ? startPos : 0);
				portal.put("location", location);
				JSONObject size = new JSONObject();
//				size.put("width", (listViewPortal.isTableview() && !fillsWidth) ? getGridWidth(form) : form.getWidth());
				size.put("width", form.getWidth());
				size.put("height", bodyheight);
				portal.put("size", size);
				portal.put("visible", listViewPortal.getVisible());
				portal.put("enabled", listViewPortal.getEnabled());
				portal.put("childElements", new JSONArray()); // empty contents; will be updated afterwards directly with form element values for components

				JSONObject relatedFoundset = new JSONObject();
				relatedFoundset.put("foundsetSelector", "");
				portal.put("relatedFoundset", relatedFoundset);

				PropertyPath propertyPath = new PropertyPath();
				propertyPath.setShouldAddElementName();
				FormElement portalFormElement = new FormElement("servoycore-portal", portal, form, name, fs, propertyPath, isInDesigner);
				PropertyDescription pd = portalFormElement.getWebComponentSpec().getProperties().get("childElements");
				if (pd != null) pd = ((CustomJSONArrayType< ? , ? >)pd.getType()).getCustomJSONTypeDefinition();
				if (pd == null)
				{
					Debug.error(new RuntimeException("Cannot find component definition special type to use for portal."));
					return null;
				}
				ComponentPropertyType type = ((ComponentPropertyType)pd.getType());

				Map<String, Object> portalFormElementProperties = new HashMap<>(portalFormElement.getRawPropertyValues());
				portalFormElementProperties.put("anchors", IAnchorConstants.ALL);
				portalFormElementProperties.put("offsetY", startPos);
				portalFormElementProperties.put("partHeight", bodyPart.getHeight());
				portalFormElementProperties.put("formview", true);
				// now put real child component form element values in "childElements"
				Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
				List<Object> children = new ArrayList<>(); // contains actually ComponentTypeFormElementValue objects
				List<IPersist> labelFors = new ArrayList<>();
				propertyPath.add(portalFormElement.getName());
				propertyPath.add("childElements");

				// it's a generated table-view form portal (BodyPortal); we just
				// have to set the Portal's tabSeq to the first one of it's children for it to work properly
				int minBodyPortalTabSeq = -2;

				while (it.hasNext())
				{
					IPersist persist = it.next();
					if (persist instanceof IFormElement)
					{
						Point loc = ((IFormElement)persist).getLocation();
						if (startPos <= loc.y && endPos > loc.y)
						{
							if (listViewPortal.isTableview() && persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null)
								continue;
							propertyPath.add(children.size());
							FormElement fe = getFormElement((IFormElement)persist, fs, propertyPath, isInDesigner);
							if (listViewPortal.isTableview())
							{
								String elementName = fe.getName();
								Iterator<GraphicalComponent> graphicalComponents = form.getGraphicalComponents();
								boolean hasLabelFor = false;
								while (graphicalComponents.hasNext())
								{
									GraphicalComponent gc = graphicalComponents.next();
									if (gc.getLabelFor() != null && Utils.equalObjects(elementName, gc.getLabelFor()) && startPos <= gc.getLocation().y &&
										endPos > gc.getLocation().y)
									{
										labelFors.add(gc);
										hasLabelFor = true;
										break;
									}
								}

								Map<String, Object> feRawProperties = new HashMap<>(fe.getRawPropertyValues());
								feRawProperties.put("componentIndex", Integer.valueOf(children.size()));
								if (hasLabelFor) feRawProperties.put("headerIndex", Integer.valueOf(labelFors.size() - 1));
								fe.updatePropertyValuesDontUse(feRawProperties);
							}
							children.add(type.getFormElementValue(null, pd, propertyPath, fe, fs));
							propertyPath.backOneLevel();

							Collection<PropertyDescription> tabSequenceProperties = fe.getWebComponentSpec().getProperties(NGTabSeqPropertyType.NG_INSTANCE);
							for (PropertyDescription tabSeqProperty : tabSequenceProperties)
							{
								String tabSeqPropertyName = tabSeqProperty.getName();
								Integer tabSeqVal = (Integer)fe.getPropertyValue(tabSeqPropertyName);
								if (tabSeqVal == null) tabSeqVal = Integer.valueOf(0); // default is 0 == DEFAULT tab sequence
								if (minBodyPortalTabSeq < 0 || (minBodyPortalTabSeq > tabSeqVal.intValue() && tabSeqVal.intValue() >= 0))
									minBodyPortalTabSeq = tabSeqVal.intValue();
							}
						}

					}
				}
				propertyPath.backOneLevel();
				propertyPath.backOneLevel();
				portalFormElementProperties.put("childElements", children.toArray());
				if (listViewPortal.isTableview())
				{
					propertyPath.add("headers");
					List<Object> headers = new ArrayList<>();
					for (IPersist persist : labelFors)
					{
						if (persist instanceof IFormElement)
						{
							propertyPath.add(headers.size());
							FormElement fe = getFormElement((IFormElement)persist, fs, propertyPath, isInDesigner);
							headers.add(type.getFormElementValue(null, pd, propertyPath, fe, fs));
							propertyPath.backOneLevel();
						}
					}
					propertyPath.backOneLevel();
					propertyPath.backOneLevel();
					portalFormElementProperties.put("headers", headers.toArray());
				}

				portalFormElementProperties.put("tabSeq", Integer.valueOf(minBodyPortalTabSeq)); // table view tab seq. is the minimum of it's children tabSeq'es

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

	private boolean fillsWidth(Form form)
	{
		if ((form.getScrollbars() & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
		{
			Part part = getBodyPart(form);
			int startPos = form.getPartStartYPos(part.getID());
			int endPos = part.getHeight();
			Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			while (it.hasNext())
			{
				IPersist persist = it.next();
				if (persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null) continue;
				if (persist instanceof BaseComponent)
				{
					BaseComponent bc = (BaseComponent)persist;
					if ((bc.getAnchors() & (IAnchorConstants.WEST + IAnchorConstants.EAST)) == (IAnchorConstants.WEST + IAnchorConstants.EAST))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public Part getBodyPart(Form form)
	{
		for (Part prt : Utils.iterate(form.getParts()))
		{
			if (prt.getPartType() == Part.BODY)
			{
				return prt;
			}
		}
		return null;
	}

	public boolean hasExtraParts(Form form)
	{
		int count = 0;
		for (Part prt : Utils.iterate(form.getParts()))
		{
			count++;
			if (count >= 2)
			{
				return true;
			}
		}
		return false;
	}

	private int getRowHeight(Form form)
	{
		int rowHeight = 0;
		Part part = getBodyPart(form);
		int startPos = form.getPartStartYPos(part.getID());
		int endPos = part.getHeight();
		Iterator<IPersist> it = form.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		while (it.hasNext())
		{
			IPersist persist = it.next();
			if (persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null) continue;
			if (persist instanceof BaseComponent)
			{
				BaseComponent bc = (BaseComponent)persist;
				Point location = bc.getLocation();
				if (startPos <= location.y && endPos > location.y)
				{
					if (rowHeight == 0)
					{
						rowHeight = bc.getSize().height;
						break;
					}
				}
			}
		}

		return rowHeight == 0 ? 20 : rowHeight;
	}

	public void reload()
	{
		persistWrappers.clear();
		formTabSequences.clear();
		formComponentElements.clear();
		FormComponentCache.templateCache.clear();
		for (FlattenedSolution fs : globalFlattendSolutions.values())
		{
			fs.close(null);
		}
		globalFlattendSolutions.clear();
	}

	public void flush(Collection<IPersist> changes)
	{
		if (changes != null)
		{
			for (IPersist persist : changes)
			{
				persistWrappers.remove(persist);
			}
		}
	}

	/**
	 * Generates a Servoy controlled tab-sequence-index. We try to avoid sending default (0 or null) tabSeq even
	 * for forms that do use default tab sequence in order to avoid problems with nesting default and non-default tabSeq forms.
	 *
	 * @param designValue the value the persist holds for the tabSeq.
	 * @param form the form containing the persist
	 * @param persistIfAvailable the persist. For now, 'component' type properties might work with non-persist-linked FormElements so it could be null.
	 * When those become persist based as well this will never be null.
	 * @return the requested controlled tabSeq (should make tabSeq be identical to the one shown in developer).
	 */
	public Integer getControlledTabSeqReplacementFor(Integer designValue, PropertyDescription pd, Form flattenedForm, IPersist persistIfAvailable,
		FlattenedSolution flattenedSolution) // TODO more args will be needed here such as the tabSeq property name or description
	{
		if (persistIfAvailable == null) return designValue; // TODO this can be removed when we know we'll always have a persist here; currently don't handle this in any way as it's not supported

		boolean formWasModifiedViaSolutionModel = flattenedSolution.hasCopy(flattenedForm);
		Map<TabSeqProperty, Integer> cachedTabSeq;
		if (formWasModifiedViaSolutionModel) cachedTabSeq = null;
		else cachedTabSeq = formTabSequences.get(flattenedForm.getUUID());

		if (cachedTabSeq == null)
		{
			cachedTabSeq = new HashMap<TabSeqProperty, Integer>();
			SortedList<TabSeqProperty> selected = new SortedList<TabSeqProperty>(new Comparator<TabSeqProperty>()
			{
				public int compare(TabSeqProperty o1, TabSeqProperty o2)
				{
					return TabSeqComparator.compareTabSeq(o1.getSeqValue(), o1.element, o2.getSeqValue(), o2.element);
				}
			});
			Iterator<IFormElement> iterator = flattenedForm.getFlattenedObjects(null).iterator();
			while (iterator.hasNext())
			{
				IFormElement formElement = iterator.next();
				if (FormTemplateGenerator.isWebcomponentBean(formElement))
				{
					String componentType = FormTemplateGenerator.getComponentTypeName(formElement);
					WebObjectSpecification specification = WebComponentSpecProvider.getInstance().getWebComponentSpecification(componentType);
					if (specification != null)
					{
						Collection<PropertyDescription> properties = specification.getProperties(NGTabSeqPropertyType.NG_INSTANCE);
						if (properties != null && properties.size() > 0)
						{
							IBasicWebComponent webComponent = (IBasicWebComponent)formElement;
							for (PropertyDescription tabSeqProperty : properties)
							{
								int tabseq = Utils.getAsInteger(webComponent.getProperty(tabSeqProperty.getName()));
								if (tabseq >= 0)
								{
									selected.add(new TabSeqProperty(formElement, tabSeqProperty.getName()));
								}
								else
								{
									cachedTabSeq.put(new TabSeqProperty(formElement, tabSeqProperty.getName()), Integer.valueOf(-2));
								}
							}
						}
					}
				}
				else if (formElement instanceof ISupportTabSeq)
				{
					if (((ISupportTabSeq)formElement).getTabSeq() >= 0)
					{
						selected.add(new TabSeqProperty(formElement, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName()));
					}
					else
					{
						cachedTabSeq.put(new TabSeqProperty(formElement, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName()), Integer.valueOf(-2));
					}
				}
			}

			int i = 1;
			for (TabSeqProperty tabSeq : selected)
			{
				cachedTabSeq.put(tabSeq, Integer.valueOf(i++));
			}

			if (!formWasModifiedViaSolutionModel)
			{
				formTabSequences.putIfAbsent(flattenedForm.getUUID(), cachedTabSeq);
			}
		}

		Integer controlledTabSeq = cachedTabSeq.get(new TabSeqProperty((IFormElement)flattenedForm.findChild(persistIfAvailable.getUUID()), pd.getName()));
		if (controlledTabSeq == null) controlledTabSeq = Integer.valueOf(-2); // if not in tabSeq, use "skip" value

		return controlledTabSeq;
	}

	public static class TabSeqProperty
	{
		public IFormElement element;
		public String propertyName;

		public TabSeqProperty(IFormElement element, String propertyName)
		{
			this.element = element;
			this.propertyName = propertyName;
		}

		public int getSeqValue()
		{
			if (propertyName != null && element instanceof IBasicWebComponent)
			{
				String componentType = FormTemplateGenerator.getComponentTypeName(element);
				WebObjectSpecification specification = WebComponentSpecProvider.getInstance().getWebComponentSpecification(componentType);
				if (specification != null)
				{
					PropertyDescription property = specification.getProperty(propertyName);
					if (property != null)
					{
						return Utils.getAsInteger(((IBasicWebComponent)element).getProperty(propertyName));
					}
				}
			}
			else if (element instanceof ISupportTabSeq)
			{
				return ((ISupportTabSeq)element).getTabSeq();
			}
			return -1;
		}

		@Override
		public String toString()
		{
			return element.toString() + (propertyName != null ? " [" + propertyName + "]" : "");
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof TabSeqProperty)) return false;
			return Utils.equalObjects(element, ((TabSeqProperty)obj).element) && Utils.equalObjects(propertyName, ((TabSeqProperty)obj).propertyName);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
			result = prime * result + ((element == null) ? 0 : element.getID());
			return result;
		}
	}

	public static class FormComponentCache
	{
		private static final ConcurrentHashMap<String, String> templateCache = new ConcurrentHashMap<>();

		private final List<FormElement> list;
		private final String template;

		private final String uuid;

		/**
		 * @param pd
		 * @param parentElement
		 * @param list
		 * @param template
		 */
		public FormComponentCache(INGFormElement parentElement, PropertyDescription pd, List<FormElement> list, String template)
		{
			this.list = list;
			String templateHit = template.replace("\"", "\\\"");
			String uuidHit = null;
			// first look if the template cache has entries for this
			for (Entry<String, String> entry : templateCache.entrySet())
			{
				if (entry.getValue().equals(templateHit))
				{
					templateHit = entry.getValue();
					uuidHit = entry.getKey();
					break;
				}
			}
			if (uuidHit == null)
			{
				// if it is not hit, just always put this in the template cache,
				// it is very unlikely that the template is constant unique over all clients even with solution model..
				uuidHit = UUID.randomUUID().toString();
				templateCache.put(uuidHit, templateHit);
			}
			this.template = templateHit;
			this.uuid = uuidHit;
		}

		public String getCacheUUID()
		{
			return uuid;
		}

		public String getTemplate()
		{
			return template;
		}

		public List<FormElement> getFormComponentElements()
		{
			return list;
		}
	}

}
