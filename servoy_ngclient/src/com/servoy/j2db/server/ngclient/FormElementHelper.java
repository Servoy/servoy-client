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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.RuntimeProperty;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGTabSeqPropertyType;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ISolutionImportListener;
import com.servoy.j2db.util.xmlxport.SolutionImportNotifier;

/**
 * Class used to cache FormElements that can be cached.
 * Also contains more code that is useful when working with FormElements.
 *
 * @author acostescu
 */
public class FormElementHelper implements IFormElementCache, ISolutionImportListener
{
	public final static RuntimeProperty<ConcurrentMap<UUID, Map<String, FormComponentCache>>> SOLUTION_MODEL_CACHE = new RuntimeProperty<ConcurrentMap<UUID, Map<String, FormComponentCache>>>()
	{
	};
	public final static RuntimeProperty<String> FORM_COMPONENT_FORM_NAME = new RuntimeProperty<String>()
	{
	};
	public final static RuntimeProperty<String> FORM_COMPONENT_UUID = new RuntimeProperty<String>()
	{
	};
	public final static RuntimeProperty<String> FORM_COMPONENT_ElEMENT_NAME = new RuntimeProperty<String>()
	{
	};
	public final static RuntimeProperty<Pair<Long, Map<TabSeqProperty, Integer>>> FORM_TAB_SEQUENCE = new RuntimeProperty<Pair<Long, Map<TabSeqProperty, Integer>>>()
	{
	};
	public final static FormElementHelper INSTANCE = new FormElementHelper();

	// todo identity key? SolutionModel persist shouldn't be cached at all?
	private final ConcurrentMap<String, FlattenedSolution> globalFlattendSolutions = new ConcurrentHashMap<>();
	private final ConcurrentMap<IPersist, FormElement> persistWrappers = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Map<TabSeqProperty, Integer>> formTabSequences = new ConcurrentHashMap<>();

	private final ConcurrentMap<UUID, Map<String, FormComponentCache>> formComponentElements = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Map<String, FormComponentCache>> formComponentElementsForDesign = new ConcurrentHashMap<>();

	private static final Map<UUID, Map<UUID, UUID>> formComponentElementsUUIDS = new WeakHashMap<>();

	private FormElementHelper()
	{
		SolutionImportNotifier.addImportListener(this);
	}

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

	public FormComponentCache getFormComponentCache(INGFormElement formElement, PropertyDescription pd, JSONObject formElementValue, Form form,
		FlattenedSolution fs)
	{
		ConcurrentMap<UUID, Map<String, FormComponentCache>> cache = formElement.getDesignId() != null ? formComponentElementsForDesign : formComponentElements;
		Solution solutionCopy = fs.getSolutionCopy(false);
		FlattenedSolution usedFS = getSharedFlattenedSolution(fs);
		if (solutionCopy != null && solutionCopy.getForm(formElement.getForm().getName()) != null)
		{
			usedFS = fs;
			// if the form is a solution model for we can't use the standard caches.
			cache = solutionCopy.getRuntimeProperty(SOLUTION_MODEL_CACHE);
			if (cache == null)
			{
				cache = new ConcurrentHashMap<UUID, Map<String, FormComponentCache>>();
				solutionCopy.setRuntimeProperty(SOLUTION_MODEL_CACHE, cache);
			}
		}
		return getFormComponentFromCache(formElement, pd, formElementValue, form, usedFS,
			cache);
	}

	private static FormComponentCache getFormComponentFromCache(INGFormElement parentElement, PropertyDescription pd, JSONObject json, Form frm,
		FlattenedSolution fs, ConcurrentMap<UUID, Map<String, FormComponentCache>> cache)
	{
		Map<String, FormComponentCache> map = cache.get(parentElement.getPersistIfAvailable().getUUID());
		if (map == null)
		{
			map = new ConcurrentHashMap<>();
			cache.put(parentElement.getPersistIfAvailable().getUUID(), map);
		}
		FormComponentCache fcCache = map.get(pd.getName());
		if (fcCache == null || fcCache.created < frm.getLastModified() || fcCache.created < parentElement.getForm().getLastModified() ||
			!frm.getUUID().toString().equals(fcCache.frmUUID))
		{
			final List<FormElement> list = generateFormComponentElements(parentElement, pd, json, frm, fs);
			fcCache = generateFormComponentCacheObject(parentElement, pd, frm, fs, list);
			map.put(pd.getName(), fcCache);
		}
		return fcCache;
	}

	private static FormComponentCache generateFormComponentCacheObject(INGFormElement parentElement, PropertyDescription pd, Form frm, FlattenedSolution fs,
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
		return new FormComponentCache(list, template, frm.getUUID().toString());
	}

	private static List<FormElement> generateFormComponentElements(INGFormElement parent, PropertyDescription pd, JSONObject json, Form frm,
		FlattenedSolution fs)
	{
		List<FormElement> elements = new ArrayList<>();
		List<IFormElement> persistElements = generateFormComponentPersists(parent, pd, json, frm, fs);
		for (IFormElement formElement : persistElements)
		{
			elements.add(new FormElement(formElement, fs, new PropertyPath(), parent.getDesignId() != null));
		}
		return elements;
	}

	private static List<IFormElement> generateFormComponentPersists(INGFormElement parent, PropertyDescription pd, JSONObject json, Form frm,
		FlattenedSolution fs)
	{
		List<IFormElement> elements = new ArrayList<>();
		List<IFormElement> formelements = fs.getFlattenedForm(frm).getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		for (IFormElement element : formelements)
		{
			element = (IFormElement)((AbstractBase)element).clonePersist(null);
			// we kind of want to have this element a new uuid, but then it is very hard to get it stable.
			UUID newElementUUID = null;
			synchronized (formComponentElementsUUIDS)
			{
				Map<UUID, UUID> map = formComponentElementsUUIDS.get(parent.getPersistIfAvailable().getUUID());
				if (map == null)
				{
					map = new HashMap<>();
					formComponentElementsUUIDS.put(parent.getPersistIfAvailable().getUUID(), map);
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
			String templateName = getStartElementName(parent, pd) + elementName;
			String formName = parent.getForm().getName();
			if (parent.getForm().isFormComponent() && parent.getPersistIfAvailable() instanceof AbstractBase &&
				((AbstractBase)parent.getPersistIfAvailable()).getRuntimeProperty(FORM_COMPONENT_FORM_NAME) != null)
			{
				formName = ((AbstractBase)parent.getPersistIfAvailable()).getRuntimeProperty(FORM_COMPONENT_FORM_NAME);
			}
			((AbstractBase)element).setRuntimeProperty(FORM_COMPONENT_FORM_NAME, formName);
			((AbstractBase)element).setRuntimeProperty(FORM_COMPONENT_ElEMENT_NAME, elementName);
			((AbstractBase)element).setRuntimeProperty(FORM_COMPONENT_UUID, parent.getPersistIfAvailable().getUUID().toString());
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
								val = ((IDesignValueConverter)property.getType()).fromDesignValue(val, property, element);
							}
							else
							{
								// will not fit, very likely a uuid that should be an int.
								if (val != null)
								{
									IPersist found = fs.searchPersist(val.toString());
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
					else if (val instanceof JSONArray && ((AbstractBase)element).getProperty(key) instanceof IChildWebObject[])
					{
						IChildWebObject[] webObjectChildren = (IChildWebObject[])((AbstractBase)element).getProperty(key);
						JSONArray original = new JSONArray();
						for (IChildWebObject element2 : webObjectChildren)
						{
							original.put(element2.getJson());
						}
						ServoyJSONObject.mergeAndDeepCloneJSON((JSONArray)val, original);
						((AbstractBase)element).setProperty(key, original);
					}
					else((AbstractBase)element).setProperty(key, val);
				}
			}
			element.setName(templateName);
			elements.add(element);
		}
		return elements;
	}

	public static String getStartElementName(INGFormElement parent, PropertyDescription pd)
	{
		String name = parent.getDesignId() != null ? parent.getDesignId() : parent.getName();
		return name != null ? name + '$' + pd.getName() + '$' : null;
	}

	private boolean isSecurityVisible(IPersist persist, FlattenedSolution fs, Form form)
	{
		int access = fs.getSecurityAccess(persist.getUUID(),
			form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
		boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
		return b_visible;
	}

	public FormElement getFormElement(IFormElement formElement, FlattenedSolution fs, PropertyPath propertyPath, final boolean designer)
	{
		// dont cache if solution model is used (media,valuelist,relations can be changed for a none changed element)
		if (designer || (fs.getSolutionCopy(false) != null) || ((AbstractBase)formElement).getRuntimeProperty(FORM_COMPONENT_FORM_NAME) != null)
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
		FlattenedSolution flattenedSolution = globalFlattendSolutions.get(fs.getMainSolutionMetaData().getName());
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
				int scrollbars = form.getScrollbars();
				if (!listViewPortal.isTableview())
				{
					// handle horizontal scrollbar on form level for listview
					int verticalScrollBars = ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED;
					if ((form.getScrollbars() & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) != 0)
					{
						verticalScrollBars = ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS;
					}
					else if ((form.getScrollbars() & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) != 0)
					{
						verticalScrollBars = ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER;
					}
					scrollbars = ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER + verticalScrollBars;
				}
				portal.put("scrollbars", scrollbars);
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
						Point loc = CSSPositionUtils.getLocation((IFormElement)persist);
						if (startPos <= loc.y && endPos > loc.y)
						{
							if (listViewPortal.isTableview() && persist instanceof GraphicalComponent && ((GraphicalComponent)persist).getLabelFor() != null)
								continue;
							propertyPath.add(children.size());
							FormElement fe = getFormElement((IFormElement)persist, fs, propertyPath, isInDesigner);
							if (listViewPortal.isTableview())
							{
								String elementName = ((IFormElement)persist).getName();
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
		formComponentElementsForDesign.clear();
		FormComponentCache.templateCache.clear();
		for (FlattenedSolution fs : globalFlattendSolutions.values())
		{
			fs.close(null);
		}
		globalFlattendSolutions.clear();
	}

	@Override
	public void afterImport()
	{
		reload();
	}

	public void flush(Collection<IPersist> changes)
	{
		if (changes != null)
		{
			for (IPersist persist : changes)
			{
				if (persist instanceof IBasicWebObject)
				{
					formComponentElementsForDesign.remove(persist.getParent().getUUID());
				}
				persistWrappers.remove(persist);
			}
		}
	}

	// this variable prevents infinite cycle for form components that have a tabseq property
	private final ThreadLocal<Boolean> nestedCall = new ThreadLocal<Boolean>();

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
		FlattenedSolution flattenedSolution, boolean design) // TODO more args will be needed here such as the tabSeq property name or description
	{
		if (persistIfAvailable == null || (nestedCall.get() != null && nestedCall.get().booleanValue())) return designValue; // TODO this can be removed when we know we'll always have a persist here; currently don't handle this in any way as it's not supported
		nestedCall.set(Boolean.TRUE);
		final boolean responsiveForm = flattenedForm.isResponsiveLayout();
		try
		{
			if (flattenedForm.isFormComponent() && persistIfAvailable instanceof AbstractBase)
			{
				String mainFormName = ((AbstractBase)persistIfAvailable).getRuntimeProperty(FORM_COMPONENT_FORM_NAME);
				if (mainFormName != null)
				{
					flattenedForm = flattenedSolution.getFlattenedForm(flattenedSolution.getForm(mainFormName));
					if (flattenedForm == null) return Integer.valueOf(-2); // just return the skip value if this is somehow invalid.
				}
			}
			boolean formWasModifiedViaSolutionModel = flattenedSolution.hasCopy(flattenedForm);
			Map<TabSeqProperty, Integer> cachedTabSeq = null;
			if (formWasModifiedViaSolutionModel)
			{
				Pair<Long, Map<TabSeqProperty, Integer>> pair = flattenedForm.getRuntimeProperty(FORM_TAB_SEQUENCE);
				if (pair != null && flattenedForm.getLastModified() == pair.getLeft().longValue())
				{
					cachedTabSeq = pair.getRight();
				}
			}
			else cachedTabSeq = formTabSequences.get(flattenedForm.getUUID());

			if (cachedTabSeq == null)
			{
				cachedTabSeq = new HashMap<TabSeqProperty, Integer>();
				SortedList<TabSeqProperty> selected = new SortedList<TabSeqProperty>(new Comparator<TabSeqProperty>()
				{
					public int compare(TabSeqProperty o1, TabSeqProperty o2)
					{
						int seq1 = o1.getSeqValue();
						int seq2 = o2.getSeqValue();
						if (seq1 == ISupportTabSeq.DEFAULT && seq2 == ISupportTabSeq.DEFAULT)
						{
							if (responsiveForm)
							{
								if (o1.element.getParent() == o2.element.getParent())
								{
									int positionComparator = PositionComparator.comparePoint(false, o1.getLocation(), o2.getLocation());
									if (positionComparator != 0)
									{
										return positionComparator;
									}

								}
								else
								{
									List<ISupportChilds> ancestors = new ArrayList<ISupportChilds>();
									IPersist persist = o1.element;
									while (persist.getParent() instanceof AbstractContainer)
									{
										ancestors.add(persist.getParent());
										persist = persist.getParent();
									}
									persist = o2.element;
									while (persist.getParent() instanceof AbstractContainer)
									{
										if (ancestors.contains(persist.getParent()))
										{
											// we found the common ancestor
											int index = ancestors.indexOf(persist.getParent());
											IPersist comparablePersist = index == 0 ? o1.element : ancestors.get(index - 1);
											int positionComparator = PositionComparator.comparePoint(false, ((ISupportBounds)comparablePersist).getLocation(),
												((ISupportBounds)persist).getLocation());
											if (positionComparator != 0)
											{
												return positionComparator;
											}
										}
										persist = persist.getParent();
									}
								}
							}
							else
							{
								int positionComparator = PositionComparator.comparePoint(false, o1.getLocation(), o2.getLocation());
								if (positionComparator != 0)
								{
									return positionComparator;
								}
							}
						}
						return compareTabSeq(seq1, o1.element, seq2, o2.element, flattenedSolution);
					}
				});
				Map<TabSeqProperty, List<TabSeqProperty>> listFormComponentMap = new HashMap<TabSeqProperty, List<TabSeqProperty>>();
				List<TabSeqProperty> listFormComponentElements = null;
				TabSeqProperty listFormComponentTabSeq = null;
				for (IFormElement formElement : flattenedForm.getFlattenedObjects(null))
				{
					if (FormTemplateGenerator.isWebcomponentBean(formElement))
					{
						String componentType = FormTemplateGenerator.getComponentTypeName(formElement);
						WebObjectSpecification specification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentType);
						if (specification != null)
						{
							Collection<PropertyDescription> properties = specification.getProperties(NGTabSeqPropertyType.NG_INSTANCE);
							Collection<PropertyDescription> formComponentProperties = specification.getProperties(FormComponentPropertyType.INSTANCE);
							boolean isListFormComponent = isListFormComponent(formComponentProperties);
							if (properties != null && properties.size() > 0)
							{
								IBasicWebComponent webComponent = (IBasicWebComponent)formElement;
								for (PropertyDescription tabSeqProperty : properties)
								{
									int tabseq = Utils.getAsInteger(webComponent.getProperty(tabSeqProperty.getName()));
									TabSeqProperty seqProperty = new TabSeqProperty(formElement, tabSeqProperty.getName());
									if (listFormComponentTabSeq == null && isListFormComponent)
									{
										listFormComponentTabSeq = seqProperty;
										listFormComponentElements = new ArrayList<TabSeqProperty>();
										listFormComponentMap.put(listFormComponentTabSeq, listFormComponentElements);
									}
									if (tabseq >= 0)
									{
										selected.add(seqProperty);
									}
									else
									{
										cachedTabSeq.put(seqProperty, Integer.valueOf(-2));
									}
								}
							}
							addFormComponentProperties(formComponentProperties, formElement, flattenedSolution, cachedTabSeq, selected,
								listFormComponentElements, design, new HashSet<String>());
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

				for (TabSeqProperty tabSeq : listFormComponentMap.keySet())
				{
					List<TabSeqProperty> elements = listFormComponentMap.get(tabSeq);
					if (elements != null)
					{
						Integer value = cachedTabSeq.get(tabSeq);
						// all elements inside list form component have same tabindex as the list itself
						for (TabSeqProperty tabSeqElement : elements)
						{
							cachedTabSeq.put(tabSeqElement, value);
						}
					}
				}
				if (!formWasModifiedViaSolutionModel)
				{
					formTabSequences.putIfAbsent(flattenedForm.getUUID(), cachedTabSeq);
				}
				else
				{
					flattenedForm.setRuntimeProperty(FORM_TAB_SEQUENCE, new Pair<>(Long.valueOf(flattenedForm.getLastModified()), cachedTabSeq));
				}
			}
			IPersist realPersist = flattenedForm.findChild(persistIfAvailable.getUUID());
			if (realPersist == null)
			{
				realPersist = persistIfAvailable;
			}
			Integer controlledTabSeq = cachedTabSeq.get(new TabSeqProperty(realPersist, pd.getName()));
			if (controlledTabSeq == null) controlledTabSeq = Integer.valueOf(-2); // if not in tabSeq, use "skip" value

			return controlledTabSeq;
		}
		finally
		{
			nestedCall.set(Boolean.FALSE);
		}
	}

	private void addFormComponentProperties(Collection<PropertyDescription> formComponentProperties, IFormElement formElement,
		FlattenedSolution flattenedSolution, Map<TabSeqProperty, Integer> cachedTabSeq, List<TabSeqProperty> selected,
		List<TabSeqProperty> listFormComponentElements, boolean design, Set<String> recursionCheck)
	{
		if (formComponentProperties != null && formComponentProperties.size() > 0)
		{
			boolean isListFormComponent = isListFormComponent(formComponentProperties);
			// avoid infinite cycle here
			FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement(formElement, flattenedSolution, null, design);
			for (PropertyDescription property : formComponentProperties)
			{
				Object frmValue = formComponentEl.getPropertyValue(property.getName());
				Form frm = FormComponentPropertyType.INSTANCE.getForm(frmValue, flattenedSolution);
				if (frm == null) continue;

				if (!recursionCheck.add(frm.getName()))
				{
					Debug.error("recursive reference found between (List)FormComponents: " + recursionCheck); //$NON-NLS-1$
					continue;
				}

				// do not use the formcomponentcache, we do not want formelement with wrong tabseq to be cached, must caclulate first the sequence
				List<IFormElement> elements = generateFormComponentPersists(formComponentEl, property, (JSONObject)frmValue, frm, flattenedSolution);

				for (IFormElement element : elements)
				{
					if (element instanceof ISupportTabSeq)
					{
						if (isListFormComponent && listFormComponentElements != null)
						{
							listFormComponentElements.add(new TabSeqProperty(element, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName()));
						}
						else if (((ISupportTabSeq)element).getTabSeq() >= 0)
						{
							selected.add(new TabSeqProperty(element, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(),
								CSSPositionUtils.getLocation(formElement)));
						}
						else
						{
							cachedTabSeq.put(new TabSeqProperty(element, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName()), Integer.valueOf(-2));
						}
					}
					else if (FormTemplateGenerator.isWebcomponentBean(element))
					{
						String nestedDomponentType = FormTemplateGenerator.getComponentTypeName(element);
						WebObjectSpecification nestedSpecification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
							nestedDomponentType);
						if (nestedSpecification != null)
						{
							Collection<PropertyDescription> nestedProperties = nestedSpecification.getProperties(NGTabSeqPropertyType.NG_INSTANCE);
							if (nestedProperties != null && nestedProperties.size() > 0)
							{
								IBasicWebComponent webComponent = (IBasicWebComponent)element;
								for (PropertyDescription tabSeqProperty : nestedProperties)
								{
									int tabseq = Utils.getAsInteger(webComponent.getProperty(tabSeqProperty.getName()));
									if (tabseq >= 0 && isListFormComponent && listFormComponentElements != null)
									{
										// all elements will have the tabseq of the list
										listFormComponentElements.add(new TabSeqProperty(element, tabSeqProperty.getName()));
									}
									else if (tabseq >= 0)
									{
										selected.add(new TabSeqProperty(element, tabSeqProperty.getName(), CSSPositionUtils.getLocation(formElement)));
									}
									else
									{
										cachedTabSeq.put(new TabSeqProperty(element, tabSeqProperty.getName()), Integer.valueOf(-2));
									}
								}
							}

							nestedProperties = nestedSpecification.getProperties(FormComponentPropertyType.INSTANCE);
							addFormComponentProperties(nestedProperties, element, flattenedSolution, cachedTabSeq, selected,
								listFormComponentElements, design, recursionCheck);
						}
					}
				}
				recursionCheck.remove(frm.getName());
			}
		}
	}

	public static boolean isListFormComponent(Collection<PropertyDescription> properties)
	{
		boolean isListFormComponent = false;
		if (properties != null)
		{
			for (PropertyDescription property : properties)
			{
				if (property.getConfig() instanceof ComponentTypeConfig && ((ComponentTypeConfig)property.getConfig()).forFoundset != null)
				{
					isListFormComponent = true;
				}
				else
				{
					isListFormComponent = false;
					break;
				}
			}
		}
		return isListFormComponent;
	}

	public static class TabSeqProperty
	{
		public IPersist element;
		public String propertyName;
		public Point locationOffset;

		public TabSeqProperty(IPersist element, String propertyName)
		{
			this(element, propertyName, null);
		}

		public TabSeqProperty(IPersist element, String propertyName, Point locationOffset)
		{
			this.element = element;
			this.propertyName = propertyName;
			this.locationOffset = locationOffset;
		}

		public int getSeqValue()
		{
			if (propertyName != null && element instanceof IBasicWebComponent)
			{
				String componentType = FormTemplateGenerator.getComponentTypeName((IBasicWebComponent)element);
				WebObjectSpecification specification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentType);
				if (specification != null)
				{
					PropertyDescription property = specification.getProperty(propertyName);
					if (property != null)
					{
						return Utils.getAsInteger(((IBasicWebComponent)element).getProperty(propertyName));
					}
				}
			}
			else if (propertyName != null && element instanceof IBasicWebObject)
			{
				return Utils.getAsInteger(((IBasicWebObject)element).getProperty(propertyName));
			}
			else if (element instanceof ISupportTabSeq)
			{
				return ((ISupportTabSeq)element).getTabSeq();
			}
			return -1;
		}

		public Point getLocation()
		{
			Point location = new Point();
			if (element instanceof ISupportBounds)
			{
				Point elementLocation = CSSPositionUtils.getLocation((ISupportBounds)element);
				location.setLocation(elementLocation.x + (locationOffset != null ? locationOffset.x : 0),
					elementLocation.y + (locationOffset != null ? locationOffset.y : 0));
			}
			return location;
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
		private final String frmUUID;

		private final long created = System.currentTimeMillis();

		public FormComponentCache(List<FormElement> list, String template, String frmUUID)
		{
			this.list = list;
			this.frmUUID = frmUUID;
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

		public String getHtmlTemplateUUIDForAngular()
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

	//TODO: try to make this method recursive, for the cases when there are more than 2 nested form components
	public static int compareTabSeq(int seq1, Object o1, int seq2, Object o2, FlattenedSolution flattenedSolution)
	{
		int yxCompare = 0;
		if (seq1 == ISupportTabSeq.DEFAULT && seq2 == ISupportTabSeq.DEFAULT && o1 instanceof IPersist && o2 instanceof IPersist)
		{
			IPersist form = ((IPersist)o1).getAncestor(IRepository.FORMS);
			if (form instanceof Form && ((Form)form).isResponsiveLayout())
			{
				if (((IPersist)o1).getParent().equals(((IPersist)o2).getParent()))
				{
					//delegate to Yx
					yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare((IPersist)o1, (IPersist)o2);
				}
				else
				{
					/*
					 * We must search all the parents of the objects o1 and o2.If the objects have a same parent by searching all the ancestors we must compare
					 * the objects before encountering the same parent.
					 */
					List<IPersist> parentsOfo1 = new ArrayList<IPersist>();
					IPersist parent = ((IPersist)o1).getParent();
					while (!(parent instanceof Form))
					{
						parentsOfo1.add(parent);
						parent = parent.getParent();
					}
					//also add the form to the list of parents
					parentsOfo1.add(parent);

					//the last parent of o1 or o2 is a formComponent, not the main form
					if (parent instanceof Form)
					{
						if (((Form)parent).isFormComponent() && o1 instanceof AbstractBase)
						{
							String uuid = ((AbstractBase)o1).getRuntimeProperty(FORM_COMPONENT_UUID);
							if (uuid != null)
							{
								IPersist persist = flattenedSolution.searchPersist(uuid);
								if (persist != null)
								{
									parent = persist.getParent();
									while (!(parent instanceof Form))
									{
										parentsOfo1.add(parent);
										parent = parent.getParent();
									}
									//also add the form to the list of parents
									parentsOfo1.add(parent);
								}
							}
						}
					}

					IPersist childo2 = (IPersist)o2;
					IPersist parento2 = ((IPersist)o2).getParent();
					while (!(parento2 instanceof Form))
					{
						if (parentsOfo1.contains(parento2))
						{
							int index = parentsOfo1.indexOf(parento2);
							//delegate to Yx
							if (index > 0)
							{
								yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(parentsOfo1.get(index - 1), childo2);
							}
						}
						childo2 = parento2;
						parento2 = parento2.getParent();
					}
					//also check to see if the common parent is the actual form
					if (parentsOfo1.contains(parento2))
					{
						int index = parentsOfo1.indexOf(parento2);
						if (index > 0)
						{
							yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(parentsOfo1.get(index - 1), childo2);
						}
					}

					if (parento2 instanceof Form && !parento2.equals(form))
					{
						if (((Form)parento2).isFormComponent() && o2 instanceof AbstractBase)
						{
							String uuid = ((AbstractBase)o2).getRuntimeProperty(FORM_COMPONENT_UUID);
							if (uuid != null)
							{
								IPersist persist = flattenedSolution.searchPersist(uuid);
								if (persist != null)
								{
									parento2 = persist.getParent();
									while (!(parento2 instanceof Form))
									{
										if (parentsOfo1.contains(parento2))
										{
											int index = parentsOfo1.indexOf(parento2);
											//delegate to Yx
											if (index > 0)
											{
												yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(parentsOfo1.get(index - 1), childo2);
											}
										}
										childo2 = parento2;
										parento2 = parento2.getParent();
									}
								}
							}
						}
					}
					if (yxCompare == 0 && parentsOfo1.contains(parento2) && parentsOfo1.size() > 1)
					{
						yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(parentsOfo1.get(parentsOfo1.size() - 2), childo2);
					}
				}
			}
			else if (form instanceof Form && !((Form)form).isResponsiveLayout())
			{
				yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare((IPersist)o1, (IPersist)o2);
			}
			// if they are at the same position, and are different persist, just use UUID to decide the sequence
			return yxCompare == 0 ? ((IPersist)o1).getUUID().compareTo(((IPersist)o2).getUUID()) : yxCompare;
		}
		else if (seq1 == ISupportTabSeq.DEFAULT)
		{
			return 1;
		}
		else if (seq2 == ISupportTabSeq.DEFAULT)
		{
			return -1;
		}
		return seq1 - seq2;
	}
}
