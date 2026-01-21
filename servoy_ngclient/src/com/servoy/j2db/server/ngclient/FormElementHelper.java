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
import java.util.Arrays;
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
import java.util.function.Function;

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
import com.servoy.j2db.util.PersistIdentifier;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
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

	// @formatter:off
	public final static RuntimeProperty<ConcurrentMap<UUID, Map<String, FormComponentCache>>> SOLUTION_MODEL_CACHE = new RuntimeProperty<>() {};

	public final static RuntimeProperty<String> FC_NAME_OF_ROOT_ACTUAL_FORM_EVEN_IN_CASE_OF_NESTED_FORM_COMPONENTS = new RuntimeProperty<>() {};
	public final static RuntimeProperty<IPersist> FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER = new RuntimeProperty<>() {};
	public final static RuntimeProperty<String> FC_CHILD_ELEMENT_NAME_INSIDE_DIRECT_PARENT_FORM_COMPONENT = new RuntimeProperty<>() {};
	public final static RuntimeProperty<String[]> FC_COMPONENT_AND_PROPERTY_NAME_PATH = PersistIdentifier.FC_COMPONENT_AND_PROPERTY_NAME_PATH;

	public final static RuntimeProperty<Pair<Long, Map<TabSeqProperty, Integer>>> FORM_TAB_SEQUENCE = new RuntimeProperty<Pair<Long, Map<TabSeqProperty, Integer>>>() {};
	public final static FormElementHelper INSTANCE = new FormElementHelper();
	// @formatter:on

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

	public FormComponentCache getFormComponentCache(INGFormElement formComponentContainerElement, PropertyDescription pd, JSONObject formElementValue,
		Form formComponent,
		FlattenedSolution fs)
	{
		boolean useGloballySharedFlattenedSolution;
		ConcurrentMap<UUID, Map<String, FormComponentCache>> cache;

		if (formComponentContainerElement.getDesignId() != null)
		{
			cache = formComponentElementsForDesign;
			useGloballySharedFlattenedSolution = false;
		}
		else
		{
			useGloballySharedFlattenedSolution = true;
			cache = formComponentElements;
		}

		Solution solutionCopy = fs.getSolutionCopy(false);
		if (solutionCopy != null && solutionCopy.getForm(formComponentContainerElement.getForm().getName()) != null)
		{
			useGloballySharedFlattenedSolution = false;
			// if the form is a solution model for we can't use the standard caches.
			cache = solutionCopy.getRuntimeProperty(SOLUTION_MODEL_CACHE);
			if (cache == null)
			{
				cache = new ConcurrentHashMap<UUID, Map<String, FormComponentCache>>();
				solutionCopy.setRuntimeProperty(SOLUTION_MODEL_CACHE, cache);
			}
		}
		return getFormComponentFromCache(formComponentContainerElement, pd, formElementValue, formComponent,
			useGloballySharedFlattenedSolution ? getSharedFlattenedSolution(fs) : fs, cache);
	}

	private static FormComponentCache getFormComponentFromCache(INGFormElement formComponentContainerElement, PropertyDescription pd,
		JSONObject formElementValue, Form formComponent,
		FlattenedSolution fs, ConcurrentMap<UUID, Map<String, FormComponentCache>> cache)
	{
		Map<String, FormComponentCache> map = cache.get(formComponentContainerElement.getPersistIfAvailable().getUUID());
		if (map == null)
		{
			map = new ConcurrentHashMap<>();
			cache.put(formComponentContainerElement.getPersistIfAvailable().getUUID(), map);
		}
		FormComponentCache fcCache = map.get(pd.getName());

		// it should generate cache if it's not there
		// it should regenerate it if any form in the form component component nesting hierarchy has a change time-stamp newer then the cache
		boolean[] shouldGenerateCache = new boolean[] { (fcCache == null || fcCache.created < formComponent.getLastModified() ||
			fcCache.created < formComponentContainerElement.getForm().getLastModified() ||
			!formComponent.getUUID().toString().equals(fcCache.frmUUID)) };

		if (!shouldGenerateCache[0])
		{
			FormComponentCache fcCacheFinal = fcCache;
			forEachFormComponentComponentParentOf(formComponentContainerElement, (ancestorFCC) -> {
				shouldGenerateCache[0] = (fcCacheFinal.created < ((Form)ancestorFCC.getAncestor(IRepository.FORMS)).getLastModified());
				return Boolean.valueOf(!shouldGenerateCache[0]); // stop iterating if we know we must regenerate
			});
		}

		if (shouldGenerateCache[0])
		{
			final List<FormElement> list = generateFormComponentElements(formComponentContainerElement, pd, formElementValue, formComponent, fs);
			fcCache = generateFormComponentCacheObject(formComponentContainerElement, pd, formComponent, fs, list);
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
					if (component.getUUID().equals(formElement.getPersistIfAvailable().getUUID()))
					{
						return formElement;
					}
				}
				return FormElementHelper.INSTANCE.getFormElement(component, flattendSol, path, design);
			}
		};
		String template = FormLayoutGenerator.generateFormComponent(frm, fs, cache, null);
		return new FormComponentCache(list, template, frm.getUUID().toString());
	}

	private static List<FormElement> generateFormComponentElements(INGFormElement formComponentContainerElement, PropertyDescription pd,
		JSONObject formElementValue, Form formComponent,
		FlattenedSolution fs)
	{
		List<FormElement> elements = new ArrayList<>();
		List<IFormElement> persistElements = generateFormComponentPersists(formComponentContainerElement, pd, formElementValue, formComponent, fs);
		for (IFormElement formElement : persistElements)
		{
			elements.add(new FormElement(formElement, fs, new PropertyPath(), formComponentContainerElement.getDesignId() != null));
		}
		return elements;
	}

	private static List<IFormElement> generateFormComponentPersists(INGFormElement formComponentContainerElement, PropertyDescription pd,
		JSONObject formElementValue, Form formComponent,
		FlattenedSolution fs)
	{
		List<IFormElement> elements = new ArrayList<>();
		List<IFormElement> formElementsOfFormComponent = fs.getFlattenedForm(formComponent).getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		for (IFormElement childOfFormComponent : formElementsOfFormComponent)
		{
			IFormElement cloneOfChildOfFormComponent = (IFormElement)((AbstractBase)childOfFormComponent).clonePersist(null);
			// we kind of want to have this element a new uuid, but then it is very hard to get it stable.
			UUID clonedChildsUUID = null;
			synchronized (formComponentElementsUUIDS)
			{
				Map<UUID, UUID> map = formComponentElementsUUIDS.get(formComponentContainerElement.getPersistIfAvailable().getUUID());
				if (map == null)
				{
					map = new HashMap<>();
					formComponentElementsUUIDS.put(formComponentContainerElement.getPersistIfAvailable().getUUID(), map);
				}
				clonedChildsUUID = map.get(cloneOfChildOfFormComponent.getUUID());
				if (clonedChildsUUID == null)
				{
					clonedChildsUUID = UUID.randomUUID();
					map.put(cloneOfChildOfFormComponent.getUUID(), clonedChildsUUID);
				}
			}
			((AbstractBase)cloneOfChildOfFormComponent).resetUUID(clonedChildsUUID);
			String childNameInsideFormComponent = cloneOfChildOfFormComponent.getName();
			if (childNameInsideFormComponent == null)
			{
				childNameInsideFormComponent = FormElement.SVY_NAME_PREFIX + cloneOfChildOfFormComponent.getUUID().toString();
			}

			// build names like fcc1$containedForm$fcc2$containedForm$button1
			// & the component-and-property-name-path array which is the same but as a String array (the array
			// is used in code to avoid String parsing); the first array item is the UUID of the root form component, not the name

			// we still do this for the name, because even though our code that needs to be aware of form component nesting should now rely on
			// componentPersist.getRuntimeProperty(FormElementHelper.FC_COMPONENT_AND_PROPERTY_NAME_PATH) instead, we exposed this $ based
			// name in element.getName() to solutions, and we need an unique name when server-client exchange info about components anyway;
			// so names should not conflict with the other form component component children or normal element names inside the same form;
			// this is used also in cypress tests - see the similar comment in ChildrenJSONGenerator where data-cy attribute is added
			// TODO this approach can still be a problem if an element has a $ in it and it ends up being a duplicate with a form component
			// named like it before the $ and a child component named like it after the $; but this is unlikely; if needed, it could be fixed
			// by adding a number or turning the name into a new UUID for example - to make them unique in that case as well (the name should
			// not be used with string $ parsing anymore anyway)... but that would result in solution code element.getName() returning a different
			// value then previously expected
			PersistIdentifier designId = formComponentContainerElement.getDesignId();
			String parentCompAndPropPathAsString = (designId != null && designId.persistUUIDAndFCPropAndComponentPath().length == 1)
				? designId.persistUUIDAndFCPropAndComponentPath()[0] /* so we are inside form designer; this will be an UUID, not a name */
				: formComponentContainerElement.getName();
			// @formatter:off
				// so in designer, if it's not nested inside an FCC, it will be the UUID of the persist; not sure if the UUID is still really needed in the name as now we have the FC_COMPONENT_AND_PROPERTY_NAME_PATH stored as an array that does have the UUID always; we no longer rely on parsing $ merged names
				//                 if it is nested inside an FCC, it will be the name
				// outside designer, it will be the name (if nested inside an FCC it already contains all the path so far concatenated via $)
			// @formatter:on

			String fullChildNameForTemplate = (parentCompAndPropPathAsString != null ? parentCompAndPropPathAsString + '$' + pd.getName() + '$' : "") + //$NON-NLS-1$
				childNameInsideFormComponent;

			String[] parentCompAndPropPath = null;
			String[] compAndPropPath = null;

			// determine the form name of the actual root (real) form (not formComponent), in case of form components nested on multiple levels
			String rootParentFormNameOfFormComponentContainersNesting = formComponentContainerElement.getForm().getName();

			if (formComponentContainerElement.getForm().isFormComponent().booleanValue() &&
				formComponentContainerElement.getPersistIfAvailable() instanceof AbstractBase)
			{
				if (((AbstractBase)formComponentContainerElement.getPersistIfAvailable())
					.getRuntimeProperty(FC_NAME_OF_ROOT_ACTUAL_FORM_EVEN_IN_CASE_OF_NESTED_FORM_COMPONENTS) != null)
				{
					rootParentFormNameOfFormComponentContainersNesting = ((AbstractBase)formComponentContainerElement.getPersistIfAvailable())
						.getRuntimeProperty(FC_NAME_OF_ROOT_ACTUAL_FORM_EVEN_IN_CASE_OF_NESTED_FORM_COMPONENTS);
				}
				parentCompAndPropPath = ((AbstractBase)formComponentContainerElement.getPersistIfAvailable())
					.getRuntimeProperty(FC_COMPONENT_AND_PROPERTY_NAME_PATH);
			}

			if (parentCompAndPropPath != null)
				compAndPropPath = Arrays.copyOf(parentCompAndPropPath, parentCompAndPropPath.length + 2);
			else
			{
				compAndPropPath = new String[3];
				IPersist fccElPersist = formComponentContainerElement.getPersistIfAvailable();
				// fccElPersist should always be non-null here; so compAndPropPath[0] should always end up being an UUID
				compAndPropPath[0] = (fccElPersist != null) ? fccElPersist.getUUID().toString() : parentCompAndPropPathAsString;
			}
			compAndPropPath[compAndPropPath.length - 2] = pd.getName();
			compAndPropPath[compAndPropPath.length - 1] = childNameInsideFormComponent;

			((AbstractBase)cloneOfChildOfFormComponent).setRuntimeProperty(
				FC_NAME_OF_ROOT_ACTUAL_FORM_EVEN_IN_CASE_OF_NESTED_FORM_COMPONENTS,
				rootParentFormNameOfFormComponentContainersNesting);
			((AbstractBase)cloneOfChildOfFormComponent).setRuntimeProperty(
				FC_CHILD_ELEMENT_NAME_INSIDE_DIRECT_PARENT_FORM_COMPONENT,
				childNameInsideFormComponent);
			((AbstractBase)cloneOfChildOfFormComponent).setRuntimeProperty(FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER,
				formComponentContainerElement.getPersistIfAvailable());
			((AbstractBase)cloneOfChildOfFormComponent).setRuntimeProperty(FC_COMPONENT_AND_PROPERTY_NAME_PATH,
				compAndPropPath);

			JSONObject elementJson = formElementValue.optJSONObject(childNameInsideFormComponent);
			if (elementJson != null)
			{
				Map<String, Method> methods = RepositoryHelper.getSetters(cloneOfChildOfFormComponent);
				WebObjectSpecification legacySpec = FormTemplateGenerator.getWebObjectSpecification(cloneOfChildOfFormComponent);
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
								val = ((IDesignValueConverter)property.getType()).fromDesignValue(val, property, cloneOfChildOfFormComponent);
							}
							else
							{
								// will not fit, very likely a uuid that should be an int.
								if (val != null)
								{
									IPersist found = fs.searchPersist(val.toString());
									if (found != null) val = found.getUUID().toString();
								}
							}
						}
					}
					if (val instanceof JSONObject && ((AbstractBase)cloneOfChildOfFormComponent).getProperty(key) instanceof JSONObject)
					{
						// if both are json (like a nested form) then merge it in.
						ServoyJSONObject.mergeAndDeepCloneJSON((JSONObject)val, (JSONObject)((AbstractBase)cloneOfChildOfFormComponent).getProperty(key));
					}
					else if (val instanceof String && StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName().equals(key))
					{
						// custom properties needs to be merged in..
						JSONObject json = ((AbstractBase)cloneOfChildOfFormComponent).getCustomProperties();
						JSONObject original = json != null ? new ServoyJSONObject(json, ServoyJSONObject.getNames(json), false, true)
							: new ServoyJSONObject(false, true);
						ServoyJSONObject.mergeAndDeepCloneJSON(new ServoyJSONObject((String)val, false), original);
						((AbstractBase)cloneOfChildOfFormComponent).setCustomProperties(original);
					}
					else if (val instanceof JSONArray && ((AbstractBase)cloneOfChildOfFormComponent).getProperty(key) instanceof IChildWebObject[])
					{
						IChildWebObject[] webObjectChildren = (IChildWebObject[])((AbstractBase)cloneOfChildOfFormComponent).getProperty(key);
						JSONArray original = new JSONArray();
						for (IChildWebObject element2 : webObjectChildren)
						{
							original.put(element2.getJson());
						}
						ServoyJSONObject.mergeAndDeepCloneJSON((JSONArray)val, original);
						((AbstractBase)cloneOfChildOfFormComponent).setProperty(key, original);
					}
					else((AbstractBase)cloneOfChildOfFormComponent).setProperty(key, val);
				}
			}
			cloneOfChildOfFormComponent.setName(fullChildNameForTemplate);
			elements.add(cloneOfChildOfFormComponent);
		}
		return elements;
	}

	public FormElement getFormElement(IFormElement formElement, FlattenedSolution fs, PropertyPath propertyPath, final boolean designer)
	{
		// dont cache if solution model is used (media,valuelist,relations can be changed for a none changed element)
		if (designer || (fs.getSolutionCopy(false) != null) ||
			((AbstractBase)formElement).getRuntimeProperty(FC_NAME_OF_ROOT_ACTUAL_FORM_EVEN_IN_CASE_OF_NESTED_FORM_COMPONENTS) != null)
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
				int startPos = form.getPartStartYPos(bodyPart.getUUID().toString());
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
					portal.put("sortable", !"-1".equals(form.getOnSortCmdMethodID()));
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
			int startPos = form.getPartStartYPos(part.getUUID().toString());
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
		int startPos = form.getPartStartYPos(part.getUUID().toString());
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
				// this method is called currently only for persist changes in the editing solution (not the real / saved solution)

				// we need to clear all affected form component component caches - even when they are nested multiple times
				// treat any parent FCCs
				forEachFormComponentComponentParentOf(persist, (ancestorFcc) -> {
					formComponentElementsForDesign.remove(ancestorFcc.getUUID());
					return Boolean.TRUE;
				});

				// in case the persist itself is directly an FCC, clear it from the cache as well
				// (this will not do anything for persists that are not FCCs and a simple remove is faster then trying to figure out if the persist is an FCC or not before doing it)
				if (persist.getParent() != null) formComponentElementsForDesign.remove(persist.getParent().getUUID());

				persistWrappers.remove(persist);
			}
		}
	}

	public IPersist findPersist(UUID uuid)
	{
		IPersist persist = findPersist(uuid, formComponentElements);
		if (persist == null) persist = findPersist(uuid, formComponentElementsForDesign);
		return persist;
	}

	private IPersist findPersist(UUID uuid, ConcurrentMap<UUID, Map<String, FormComponentCache>> cache)
	{
		for (Map<String, FormComponentCache> map : cache.values())
		{
			for (FormComponentCache fcc : map.values())
			{
				for (FormElement fe : fcc.getFormComponentElements())
				{
					if (fe.getPersistIfAvailable().getUUID().equals(uuid)) return fe.getPersistIfAvailable();
				}
			}
		}
		return null;
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
				String mainFormName = ((AbstractBase)persistIfAvailable).getRuntimeProperty(FC_NAME_OF_ROOT_ACTUAL_FORM_EVEN_IN_CASE_OF_NESTED_FORM_COMPONENTS);
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
						return compareTabSeq(o1, o2, responsiveForm);
					}
				});
				Map<TabSeqProperty, List<TabSeqProperty>> listFormComponentMap = new HashMap<TabSeqProperty, List<TabSeqProperty>>();
				List<TabSeqProperty> listFormComponentElements = null;
				for (IFormElement formElement : flattenedForm.getFlattenedObjects(null))
				{
					TabSeqProperty listFormComponentTabSeq = null;
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
										// I think we assume list form components don't nest other list form components...; so the addFormComponentProperties(...) call below will only use this array on the first level...
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
								listFormComponentElements, design, new HashSet<String>(), new Point());
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
					List<TabSeqProperty> elementsInThatListFormComponent = listFormComponentMap.get(tabSeq);
					if (elementsInThatListFormComponent != null)
					{
						Integer value = cachedTabSeq.get(tabSeq); // tabSeq (the key) is the tabSeq of the form component itself
						// all elements inside list form component have same tabindex as the list itself
						// TODO why do we make it so?
						for (TabSeqProperty tabSeqElement : elementsInThatListFormComponent)
						{
							cachedTabSeq.put(tabSeqElement, tabSeqElement.getSeqValue());
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

	/**
	 * @param parentFCComputedLocation we care about the location of components nested in FC's relative to the form when comparing
	 * positions in css positioned forms; this param says the starting position of the parent FC (or main form 0,0) of
	 * formComponentFormElement (Note: FCs can be nested multiple times - that is why we need this as a param here)
	 */
	private void addFormComponentProperties(Collection<PropertyDescription> formComponentProperties, IFormElement formComponentFormElement,
		FlattenedSolution flattenedSolution, Map<TabSeqProperty, Integer> cachedTabSeq, List<TabSeqProperty> selected,
		List<TabSeqProperty> listFormComponentElements, boolean design, Set<String> recursionCheck, Point parentFCComputedLocation)
	{
		if (formComponentProperties != null && formComponentProperties.size() > 0)
		{
			// avoid infinite cycle here
			FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement(formComponentFormElement, flattenedSolution, null, design);
			for (PropertyDescription property : formComponentProperties)
			{
				boolean isListFormComponent = isListFormComponent(formComponentProperties);
				Object frmValue = formComponentEl.getPropertyValue(property.getName());
				Form frm = FormComponentPropertyType.INSTANCE.getForm(frmValue, flattenedSolution);
				if (frm == null) continue;

				if (!recursionCheck.add(frm.getName()))
				{
					Debug.error("recursive reference found between (List)FormComponents: " + recursionCheck); //$NON-NLS-1$
					continue;
				}

				// do not use the formcomponentcache, we do not want formelement with wrong tabseq to be cached, must caclulate first the sequence
				List<IFormElement> formComponentPersistsForFCProp = generateFormComponentPersists(formComponentEl, property, (JSONObject)frmValue, frm,
					flattenedSolution);

				for (IFormElement childPersistOfFCProp : formComponentPersistsForFCProp)
				{
					if (childPersistOfFCProp instanceof ISupportTabSeq)
					{
						if (isListFormComponent && listFormComponentElements != null)
						{
							listFormComponentElements.add(new TabSeqProperty(childPersistOfFCProp, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName()));
						}
						else if (((ISupportTabSeq)childPersistOfFCProp).getTabSeq() >= 0)
						{
							Point locationOfFCInParent = CSSPositionUtils.getLocation(formComponentFormElement);
							Point fCComputedLocation = new Point(locationOfFCInParent.x + parentFCComputedLocation.x,
								locationOfFCInParent.y + parentFCComputedLocation.y);
							selected.add(new TabSeqProperty(childPersistOfFCProp, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(),
								fCComputedLocation));
						}
						else
						{
							cachedTabSeq.put(new TabSeqProperty(childPersistOfFCProp, StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName()),
								Integer.valueOf(-2));
						}
					}
					else if (FormTemplateGenerator.isWebcomponentBean(childPersistOfFCProp))
					{
						String childComponentType = FormTemplateGenerator.getComponentTypeName(childPersistOfFCProp);
						WebObjectSpecification childComponentSpecification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
							childComponentType);
						if (childComponentSpecification != null)
						{
							Point locationOfFCInParent = CSSPositionUtils.getLocation(formComponentFormElement);
							Point fCComputedLocation = new Point(locationOfFCInParent.x + parentFCComputedLocation.x,
								locationOfFCInParent.y + parentFCComputedLocation.y);

							Collection<PropertyDescription> childComponentTabSeqProperties = childComponentSpecification
								.getProperties(NGTabSeqPropertyType.NG_INSTANCE);
							if (childComponentTabSeqProperties != null && childComponentTabSeqProperties.size() > 0)
							{
								IBasicWebComponent childWebComponent = (IBasicWebComponent)childPersistOfFCProp;
								for (PropertyDescription childTabSeqProperty : childComponentTabSeqProperties)
								{
									int tabseq = Utils.getAsInteger(childWebComponent.getProperty(childTabSeqProperty.getName()));
									if (tabseq >= 0 && isListFormComponent && listFormComponentElements != null)
									{
										// all elements will have the tabseq of the list
										listFormComponentElements.add(new TabSeqProperty(childPersistOfFCProp, childTabSeqProperty.getName()));
									}
									else if (tabseq >= 0)
									{
										selected.add(new TabSeqProperty(childPersistOfFCProp, childTabSeqProperty.getName(), fCComputedLocation));
									}
									else
									{
										cachedTabSeq.put(new TabSeqProperty(childPersistOfFCProp, childTabSeqProperty.getName()), Integer.valueOf(-2));
									}
								}
							}

							addFormComponentProperties(childComponentSpecification.getProperties(FormComponentPropertyType.INSTANCE),
								childPersistOfFCProp, flattenedSolution, cachedTabSeq, selected,
								listFormComponentElements, design, recursionCheck, fCComputedLocation);
						}
					}
				}
				recursionCheck.remove(frm.getName());
			}
		}
	}

	/**
	 * We currently don't have form component components (list or normal) that contain 2 form component properties in them, only 1 prop;
	 * and even if we did, we don't have a way to link which tabseq. property is for which list form component prop.<br/>
	 *
	 * So we currently decide if it's a list form component or not based on a collection of properties; although logically one property says
	 * if it's a list form component or a component, not multiple (if it's using forFoundset 'formcomponent' typed properties or not).
	 */
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

		public IPersist getElement()
		{
			return element;
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
			result = prime * result + ((element == null) ? 0 : element.getUUID().hashCode());
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

	public static int compareTabSeq(TabSeqProperty tabSeq1, TabSeqProperty tabSeq2, boolean inResponsiveForm)
	{
		int yxCompare = 0;
		int seq1 = tabSeq1.getSeqValue();
		int seq2 = tabSeq2.getSeqValue();

		if (seq1 == ISupportTabSeq.DEFAULT && seq2 == ISupportTabSeq.DEFAULT && tabSeq1.getElement() != null && tabSeq2.getElement() != null)
		{
			if (inResponsiveForm)
			{
				if (tabSeq1.getElement().getParent().equals(tabSeq2.getElement().getParent()))
				{
					// same parent; delegate to Yx (place inside the same layout container or same form probably)
					yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(tabSeq1.getElement(), tabSeq2.getElement());
				}
				else
				{
					// different parent

					// We must search all the parents of persist1 and persist2. If the persists have a common ancestor - by searching all the ancestors
					// (layout containers, including going through parent form component containers) & comparing the position of direct children of
					// that ancestor in order to determine the default tab seq.

					List<ComparabeParentAbstraction> parentsOfPersist1 = findParentsOfPersistThroughFCs(tabSeq1.getElement());
					List<ComparabeParentAbstraction> parentsOfPersist2 = findParentsOfPersistThroughFCs(tabSeq2.getElement());

					// search for the inner most common parent; we start at idx 1 as idx 0 will always be the initial persist itself
					for (int indexOfParentOfP1 = 1; indexOfParentOfP1 < parentsOfPersist1.size(); indexOfParentOfP1++)
					{
						ComparabeParentAbstraction parentOfP1 = parentsOfPersist1.get(indexOfParentOfP1);
						int indexOfParentOfP1InParentsOfP2 = parentsOfPersist2.indexOf(parentOfP1);
						if (indexOfParentOfP1InParentsOfP2 > 0) // not >=, as idx 0 will always be the initial persist itself, not a parent
						{
							// so we found a common parent; could be a form, a layout container or a component container that has multiple FC typed properties;
							// compare the direct children of this common parent on the two paths
							// the children could be:
							//   - Form persists that are actually form components (if the common parent is a form component container that
							//          uses multiple formComponent properties in it...); we compare those using the names of the 2 FC typed
							//          properties (so that the comparison is stable and predictable)
							//   - other persists that implement ISupportBounds (LayoutContainer, IFormElement) that can be compared via x/y
							ComparabeParentAbstraction child1ToUseForComparison = parentsOfPersist1.get(indexOfParentOfP1 - 1); // -1 should not be a prob. because we do check in the beginning if they have a common parent directly - and they don't; so we should be here already in indexes >= 1
							ComparabeParentAbstraction child2ToUseForComparison = parentsOfPersist2.get(indexOfParentOfP1InParentsOfP2 - 1);

							if (child1ToUseForComparison.fcPropName() != null && child2ToUseForComparison.fcPropName() != null)
							{
								// that means both initial persists are children of form components (have those FC_... runtime-properties set)
								// and the first difference is between 2 FromComponentPropertyTypeproperties of the same form component component
								yxCompare = StringComparator.INSTANCE.compare(child1ToUseForComparison.fcPropName(), child2ToUseForComparison.fcPropName());
							}
							else yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(child1ToUseForComparison.persist(),
								child2ToUseForComparison.persist());

							break;
						}
					}
				}
			}
			else
			{
				// we are not in a responsive form
				yxCompare = PositionComparator.comparePoint(false, tabSeq1.getLocation(), tabSeq2.getLocation());
			}

			// if they are at the same position, and are different persist, just use UUID to decide the sequence
			return yxCompare == 0 ? tabSeq1.getElement().getUUID().compareTo(tabSeq2.getElement().getUUID()) : yxCompare;
		}
		// they still are persists at this point I think; just that at least one of them doesn't have a DEFAULT tab seq...
		else if (seq1 == ISupportTabSeq.DEFAULT)
		{
			// Comparator.compare() says -1 or negative if o1 < o2, 0 if equal and 1 or positive if o1 > o2;
			// so we say here that o1 > o2 (o2 has lower tab seq., so it gets focused faster, because o2 has a tabseq set and o1 does not)
			return 1;
		}
		else if (seq2 == ISupportTabSeq.DEFAULT)
		{
			return -1;
		}
		return seq1 - seq2;
	}

	/**
	 * findParentsOfPersistThroughFCs(...) needs to be able to put in the lists IPersist parents that
	 * are unique; but a Form that is a form component and is used as a FormComponentPropertyType property
	 * of a form component component is the same Form instance for all FC clones... so instead of
	 * identifying it by that Form, we identify it based on the form component component + it's FC property name
	 * because the form component component is unique (even if there are nested FCs, the form component components
	 * will be clones - so still unique)<br/><br/>
	 *
	 * So this is the purpose of this record. To keep either simple layout persists/initial persist
	 * (+ null fcPropName) or combinations of form component component persists + their FC property name.
	 */
	private static record ComparabeParentAbstraction(IPersist persist, String fcPropName)
	{
	}

	private static List<ComparabeParentAbstraction> findParentsOfPersistThroughFCs(IPersist persist)
	{
		// when iterating in the while below, in case the persist is inside of nested form components, it will go
		// through form component parent form-component-containers as well; if that happens, innerMostChildPersistInsideCurrentFormComponentContainer
		// will get the value of the latest form-component-containers it went through (because that one should have the FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER
		// runtime property in order to go even further in form-component-container parents)

		List<ComparabeParentAbstraction> parentsOfPersist = new ArrayList<>();

		parentsOfPersist.add(new ComparabeParentAbstraction(persist, null)); // add first persist as well as later, in the calling code we do a idx - 1 on parent arrays (to check positions of children of common parent) and we don't want that to target index -1

		IPersist innerMostChildPersistInsideCurrentFormComponentContainer = persist;
		IPersist parent = innerMostChildPersistInsideCurrentFormComponentContainer.getParent();

		while (!(parent instanceof Form || parent == null))
		{
			parentsOfPersist.add(new ComparabeParentAbstraction(parent, null)); // a layout container, the actual top form or a intermediate Form that is actually a form component
			parent = parent.getParent();

			if (parent instanceof Form && ((Form)parent).isFormComponent().booleanValue() && persist instanceof AbstractBase)
			{
				String[] fcComponentAndPropertyNamePath = ((AbstractBase)innerMostChildPersistInsideCurrentFormComponentContainer)
					.getRuntimeProperty(FC_COMPONENT_AND_PROPERTY_NAME_PATH);
				if (fcComponentAndPropertyNamePath != null)
				{
					// if we reached a form component parent, add the parents in the actual form where this form component is used as well

					// we will need the FormComponentType property's name inside the form component component when checking for equal parent abstractions
					// as well as when comparing between form components used in the same from component component in different properties
					// see javadoc of ComparabeParentAbstraction
					String fcPropName = fcComponentAndPropertyNamePath[fcComponentAndPropertyNamePath.length - 2];

					innerMostChildPersistInsideCurrentFormComponentContainer = ((AbstractBase)innerMostChildPersistInsideCurrentFormComponentContainer)
						.getRuntimeProperty(FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER);
					parent = innerMostChildPersistInsideCurrentFormComponentContainer;

					// so we can have Form instances (that are form components actually) in this parents list; we will have
					// to handle this when using calling code comparison as well (if a form component container uses 2 or more
					// formComponent properties and persist1 is from one such property and persist2 is from another; then we
					// should compare those by property name - for example (so that the comparison is stable and predictable))
					parentsOfPersist.add(new ComparabeParentAbstraction(innerMostChildPersistInsideCurrentFormComponentContainer, fcPropName));
				} // else we reached the root form and it happens to be a form component... (code of form designers open on form components can end up here)
			}
		}
		// add the actual top form as well
		if (parent != null) parentsOfPersist.add(new ComparabeParentAbstraction(parent, null)); // "parent" should never be null here BTW

		return parentsOfPersist;
	}

	/**
	 * Goes through all form component component parents of the given 'elementInsideAFormComponentComponent' from the most nested one to the top one.<br/><br/>
	 *
	 * If the consumer returns FALSE, it stops iterating.<br/><br/>
	 *
	 * If the given 'elementInsideAFormComponentComponent' is itself a form component component, it will NOT be given to the consumer.
	 *
	 * @param consumer it's actually a function that consumes the FCCs that this method produces, and returns TRUE if it needs more values of FALSE if it is no longer interested in getting more values.
	 */
	public static void forEachFormComponentComponentParentOf(INGFormElement elementInsideAFormComponentComponent, Function<IPersist, Boolean> consumer)
	{
		FormElementHelper.forEachFormComponentComponentParentOf(elementInsideAFormComponentComponent.getPersistIfAvailable(), consumer);
	}

	/**
	 * Goes through all form component component parents of the given 'persistInsideAFormComponentComponent' from the most nested one to the top one.<br/><br/>
	 *
	 * If the consumer returns FALSE, it stops iterating.<br/><br/>
	 *
	 * If the given 'persistInsideAFormComponentComponent' is itself a form component component, it will NOT be given to the consumer.
	 *
	 * @param consumer it's actually a function that consumes the FCCs that this method produces, and returns TRUE if it needs more values of FALSE if it is no longer interested in getting more values.
	 */
	public static void forEachFormComponentComponentParentOf(IPersist persistInsideAFormComponentComponent, Function<IPersist, Boolean> consumer)
	{
		IPersist currentFCCPersist = persistInsideAFormComponentComponent;
		Boolean goOn = Boolean.TRUE;
		while (currentFCCPersist != null && goOn.booleanValue())
		{
			currentFCCPersist = (currentFCCPersist instanceof AbstractBase ab)
				? ab.getRuntimeProperty(FormElementHelper.FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER) : null;

			if (currentFCCPersist != null) goOn = consumer.apply(currentFCCPersist);
		}
	}

}