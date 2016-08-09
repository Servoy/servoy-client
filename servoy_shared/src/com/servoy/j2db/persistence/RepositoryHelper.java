/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.persistence;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.servoy.j2db.documentation.persistence.docs.DocsInsetList;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 */
public class RepositoryHelper
{
	public static final int MAX_SQL_NAME_LENGTH = 50;

	public static final int BASIC_FILTER = 0;
	public static final int ADVANCED_FILTER = 1;
	public static final int WEB_FILTER = 2;

	public static String limitSQLName(String name)
	{
		return name.substring(0, Math.min(name.length(), MAX_SQL_NAME_LENGTH));
	}

	private final IDeveloperRepository developerRepository;

	public RepositoryHelper(IDeveloperRepository dr)
	{
		developerRepository = dr;
	}

	private static final Map<Class< ? >, Map<String, Method>> setterCache = Collections.synchronizedMap(new HashMap<Class< ? >, Map<String, Method>>());

	/**
	 * Get all the setMethods on the specified object via introspection
	 *
	 * @param the object
	 * @return a map with name -> java.lang.reflect.Method
	 */
	public static Map<String, Method> getSetters(Object obj)
	{
		if (obj == null) return Collections.<String, Method> emptyMap();
		try
		{
			return getSettersViaIntrospection(obj.getClass());
		}
		catch (IntrospectionException e)
		{
			Debug.error(e);
		}
		return Collections.<String, Method> emptyMap();
	}

	/**
	 * Get all the setMethods on the specified object via introspection
	 *
	 * @param the object
	 * @return a map with name -> java.lang.reflect.Method
	 */
	static Map<String, Method> getSettersViaIntrospection(Object obj) throws IntrospectionException
	{
		if (obj == null) return Collections.<String, Method> emptyMap();
		return getSettersViaIntrospection(obj.getClass());
	}

	/**
	 * Get all the setMethods on the specified class via introspection
	 *
	 * @param clazz the class
	 * @return a map with name -> java.lang.reflect.Method
	 */
	static Map<String, Method> getSettersViaIntrospection(Class< ? > clazz) throws IntrospectionException
	{
		Map<String, Method> retval = setterCache.get(clazz);
		if (retval == null)
		{
			retval = new HashMap<String, Method>();
			BeanInfo bi = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] pds = bi.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds)
			{
				Method m = pd.getWriteMethod();
				if (m != null)
				{
					try
					{
						m.setAccessible(true);
					}
					catch (Exception e)
					{
					}
					retval.put(pd.getName(), m);
				}
			}
			setterCache.put(clazz, retval);
		}
		return retval;
	}

	private static final Map<Class< ? >, Map<String, Method>> getterCache = Collections.synchronizedMap(new HashMap<Class< ? >, Map<String, Method>>());

	/**
	 * Get all the getMethods on the specified object via introspection
	 *
	 * @param the object
	 * @return a map with name -> java.lang.reflect.Method
	 */
	static Map<String, Method> getGettersViaIntrospection(Object obj) throws IntrospectionException
	{
		if (obj == null) return Collections.<String, Method> emptyMap();

		Class< ? > clazz = obj.getClass();
		Map<String, Method> retval = getterCache.get(clazz);
		if (retval == null)
		{
			retval = new HashMap<String, Method>();
			BeanInfo bi = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] pds = bi.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds)
			{
				Method m = pd.getReadMethod();
				if (m != null)
				{
					try
					{
						m.setAccessible(true);
					}
					catch (Exception e)
					{
					}
					retval.put(pd.getName(), m);
				}
			}
			getterCache.put(clazz, retval);
		}
		return retval;
	}

	public static void initClone(IPersist clone, IPersist original, boolean flattenOverrides)
	{
		if (flattenOverrides && original instanceof ISupportExtendsID && PersistHelper.isOverrideElement((ISupportExtendsID)original) &&
			(!(original instanceof Form)))
		{
			// copy all properties from element hierarchy into copy, make copy non-override
			List<AbstractBase> overrideHierarchy = PersistHelper.getOverrideHierarchy((ISupportExtendsID)original);

			// top-most super-element first
			for (int i = overrideHierarchy.size() - 1; i >= 0; i--)
			{
				((AbstractBase)clone).copyPropertiesMap(overrideHierarchy.get(i).getPropertiesMap(), false);
			}
			// no longer an override
			((AbstractBase)clone).clearTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID);
		}
		else
		{
			((AbstractBase)clone).copyPropertiesMap(((AbstractBase)original).getPropertiesMap(), false);
		}
	}

	public static String getObjectTypeName(int objectTypeId)
	{
		switch (objectTypeId)
		{
			case IRepository.SOLUTIONS :
				return "solution"; //$NON-NLS-1$
			case IRepository.STYLES :
				return "style"; //$NON-NLS-1$
			case IRepository.TEMPLATES :
				return "template"; //$NON-NLS-1$
			case IRepository.FORMS :
				return "form"; //$NON-NLS-1$
			case IRepository.FIELDS :
				return "field"; //$NON-NLS-1$
			case IRepository.GRAPHICALCOMPONENTS :
				return "graphical component"; //$NON-NLS-1$
			case IRepository.TABPANELS :
				return "tab panel"; //$NON-NLS-1$
			case IRepository.PORTALS :
				return "portal"; //$NON-NLS-1$
			case IRepository.RELATIONS :
				return "relation"; //$NON-NLS-1$
			case IRepository.VALUELISTS :
				return "valuelist"; //$NON-NLS-1$
			case IRepository.MEDIA :
				return "media"; //$NON-NLS-1$
			default :
				// TODO: add other known object types if this method ever becomes used
				// for objects which are not root objects.
				return "<unknown object type>"; //$NON-NLS-1$
		}
	}

	public List<RootObjectReference> getActiveSolutionModuleMetaDatas(int solutionId) throws RepositoryException
	{
		Map<UUID, RootObjectReference> referencedModules = new HashMap<UUID, RootObjectReference>();
		// get the main solution;
		try
		{
			Solution sol = (Solution)developerRepository.getActiveRootObject(solutionId);
			if (sol != null)
			{
				referencedModules.put(sol.getUUID(),
					new RootObjectReference(sol.getName(), sol.getUUID(), sol.getRootObjectMetaData(), sol.getReleaseNumber()));
				loadObjectMetaDatas(sol.getModulesNames(), referencedModules, SolutionMetaData.isImportHook(sol.getSolutionMetaData()));
			}
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
		return new ArrayList<RootObjectReference>(referencedModules.values());
	}

	private void loadObjectMetaDatas(String moduleNames, Map<UUID, RootObjectReference> referencedModules, boolean loadImportHooks) throws RepositoryException
	{
		if (moduleNames == null) return;
		StringTokenizer tk = new StringTokenizer(moduleNames, ";,"); //$NON-NLS-1$
		int count = tk.countTokens();
		if (count > 0)
		{
			while (tk.hasMoreTokens())
			{
				try
				{
					String moduleDescriptor = tk.nextToken();
					SolutionMetaData metaData;
					int releaseNumber = 0;
					int i = moduleDescriptor.indexOf(':');
					String name;
					UUID uuid;
					if (i != -1)
					{
						releaseNumber = Integer.parseInt(moduleDescriptor.substring(i + 1));
						moduleDescriptor = moduleDescriptor.substring(0, i);
					}

					if (moduleDescriptor.indexOf('-') != -1)
					{
						// A uuid reference.
						uuid = UUID.fromString(moduleDescriptor);
						metaData = (SolutionMetaData)developerRepository.getRootObjectMetaData(uuid);
						if (metaData == null)
						{
							continue;
						}
						name = metaData.getName();
					}
					else
					{
						// A module name; for backwards compatibility.
						name = moduleDescriptor;
						metaData = (SolutionMetaData)developerRepository.getRootObjectMetaData(name, IRepository.SOLUTIONS);
						if (metaData == null)
						{
							continue;
						}
						uuid = metaData.getRootObjectUuid();
					}
					if (referencedModules.get(uuid) == null && (loadImportHooks || !SolutionMetaData.isImportHook(metaData)))
					{
						referencedModules.put(uuid, new RootObjectReference(name, uuid, metaData, releaseNumber));
						Solution sol = (Solution)developerRepository.getRootObject(metaData.getRootObjectId(), releaseNumber);
						loadObjectMetaDatas(sol.getModulesNames(), referencedModules, loadImportHooks);
					}
				}
				catch (RemoteException e)
				{
					throw new RepositoryException(e);
				}
			}
		}
	}

	//-- STATIC HELPER METHODS ------------------------------------------------------------------------------------


	private static boolean hideForFilter(String name)
	{
		int filter = ADVANCED_FILTER; // TODO application.getFilter();
		boolean retval = false;
		if ((filter & ADVANCED_FILTER) == BASIC_FILTER) //basic
		{
			if (name.equals("useSeparateFoundSet")) //$NON-NLS-1$
			{
				retval = true;
			}
		}
		if ((filter & WEB_FILTER) == WEB_FILTER) //web
		{
			//button
			if (name.equals("rolloverImageMediaID")) retval = true; //$NON-NLS-1$
			else if (name.equals("rolloverCursor")) retval = true; //$NON-NLS-1$
//			else if (name.equals("verticalAlignment")) 								retval = true; //$NON-NLS-1$
			else if (name.equals("rotation")) retval = true; //$NON-NLS-1$
			else if (name.equals("showFocus")) retval = true; //$NON-NLS-1$
//			else if (name.startsWith("onFocus"))	 								retval = true; //$NON-NLS-1$
			else if (name.equals("anchors")) retval = true; //$NON-NLS-1$
			//field
//			else if (name.equals("onAction") && persist instanceof Field)	retval = true; //$NON-NLS-1$
			else if (name.equals("selectOnEnter")) retval = true; //$NON-NLS-1$
			//form
			else if (name.endsWith("CmdMethodID")) retval = true; //$NON-NLS-1$
			//portal
			else if (name.equals("resizeble")) retval = true; //$NON-NLS-1$
			else if (name.equals("reorderable")) retval = true; //$NON-NLS-1$
			else if (name.equals("multiLine")) retval = true; //$NON-NLS-1$
			else if (name.equals("intercellSpacing")) retval = true; //$NON-NLS-1$
			else if (name.equals("showVerticalLines")) retval = true; //$NON-NLS-1$
			else if (name.equals("showHorizontalLines")) retval = true; //$NON-NLS-1$
			//tabpanel
			// HIDE/Show (only top) is supported, else if (name.equals("tabOrientation"))									retval = true;
			else if (name.equals("scrollTabs")) retval = true; //$NON-NLS-1$
		}
		return retval;
	}

	@SuppressWarnings("nls")
	public static boolean forceHideInDocs(String name, Class< ? > persistClass, int displayType)
	{
		if (persistClass.equals(TableNode.class) && name.equals("dataSource"))
		{
			return true;
		}
		if (persistClass.equals(Field.class) && name.equals(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()) &&
			(displayType == Field.TEXT_FIELD || displayType == Field.CALENDAR || displayType == Field.COMBOBOX || displayType == Field.PASSWORD ||
				displayType == Field.SPINNER || displayType == Field.TYPE_AHEAD))
		{
			return true;
		}
		return false;
	}

	@SuppressWarnings("nls")
	public static boolean forceShowInDocs(String name, Class< ? > persistClass)
	{
		if (persistClass.equals(Tab.class) && name.equals("containsFormID")) // handled in combined property table //$NON-NLS-1$
		{
			return true;
		}
		if (persistClass.equals(RelationItem.class))
		{
			return name.equals("foreignColumnName") || name.equals("operator") || name.equals("primaryDataProviderID"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}
		if (persistClass.equals(ScriptMethod.class) || persistClass.equals(AbstractScriptProvider.class))
		{
			return name.equals("name") || name.equals("showInMenu") || name.equals("declaration"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}
		if (persistClass.equals(ISupportDataProviderID.class))
		{
			return name.equals("valuelistID"); //$NON-NLS-1$
		}
		if (persistClass.equals(ValueList.class))
		{
			return name.equals("serverName") || name.equals("tableName") || name.equals("addEmptyValue") || name.equals("customValues") ||
				name.equals("dataSource") || name.equals("relationName") || name.equals("separator") || name.equals("sortOptions") ||
				name.equals("useTableFilter") || name.equals("valueListType");
		}
		if (persistClass.equals(Part.class))
		{
			return name.equals("groupbyDataProviderIDs") || name.equals("partType");
		}
		if (persistClass.equals(ISupportPrinting.class))
		{
			return name.equals("printable");
		}
		if (persistClass.equals(ISupportBounds.class))
		{
			return name.equals("location");
		}
		if (persistClass.equals(ISupportSize.class))
		{
			return name.equals("size");
		}
		if (persistClass.equals(ISupportName.class))
		{
			return name.equals("name");
		}
		if (persistClass.equals(Form.class))
		{
			return name.equals("serverName") || name.equals("tableName");
		}
		if (persistClass.equals(Solution.class))
		{
			return name.equals("loginSolutionName");
		}
		if (persistClass.equals(ColumnInfo.class))
		{
			return name.equals("autoEnterSubType") || name.equals("autoEnterType") || name.equals("converterName") || name.equals("converterProperties") ||
				name.equals("databaseDefaultValue") || name.equals("databaseSequenceName") || name.equals("defaultFormat") || name.equals("defaultValue") ||
				name.equals("description") || name.equals("foreignType") || name.equals("lookupValue") || name.equals("titleText") ||
				name.equals("validatorName") || name.equals("validatorProperties");
		}

		return false;
	}

	// Some properties should be created(for undo/redo) but not visible in the properties view
	public static boolean hideForProperties(String name, Class< ? > persistClass, IPersist persist)
	{
		if (persist instanceof Form && Utils.getAsBoolean(((Form)persist).isFormComponent()) &&
			(name.equals("borderType") || name.equals("defaultPageFormat") || name.equals("initialSort") || name.equals("navigatorID") ||
				name.equals("namedFoundSet") || name.equals("paperPrintScale") || name.equals("scrollbars") || name.equals("selectionMode") ||
				name.equals("styleName") || name.equals("styleClass") || name.equals("titleText") || name.equals("transparent") || name.equals("view") ||
				name.equals("showInMenu") || name.equals("encapsulation")))
		{
			return true;
		}
		if (persist instanceof Part && persist.getParent() instanceof Form && Utils.getAsBoolean(((Form)persist.getParent()).isFormComponent()) &&
			!name.equals("height"))
		{
			return true;
		}
		if (name.equals("groupbyDataProviderIDs") && Part.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return true;
		}
		if (name.equals("containsFormID")) // handled in combined property table //$NON-NLS-1$
		{
			return true;
		}
		if (name.equals("foreground") && Portal.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return true;
		}
		if (name.equals("lineNumberOffset")) //$NON-NLS-1$
		{
			return true;
		}
		if (name.equals("extendsID") && LayoutContainer.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return true;
		}
		if (name.equals("groupID")) //$NON-NLS-1$
		{
			return true;
		}
		if (name.equals("customProperties")) //$NON-NLS-1$
		{
			return true;//not implemented yet
		}
		if (name.equals("location") && Tab.class.isAssignableFrom(persistClass)) // set in ChangeBoundsCommand //$NON-NLS-1$
		{
			return true;
		}
		if (name.equals("rowBGColorCalculation") && persist != null) //$NON-NLS-1$
		{
			String rowBGColorCalculation = null;
			if (persist.getTypeID() == IRepository.FORMS)
			{
				rowBGColorCalculation = ((Form)persist).getRowBGColorCalculation();
			}
			else if (persist.getTypeID() == IRepository.PORTALS)
			{
				rowBGColorCalculation = ((Portal)persist).getRowBGColorCalculation();
			}
			return rowBGColorCalculation == null;
		}
		if (name.equals("referenceForm")) //$NON-NLS-1$
		{
			return true;
		}
		return false;
	}

	public static boolean shouldShow(String name, Element element, Class< ? > persistClass, int displayType)
	{
		if (element == null)
		{
			// no content spec (example: form.width), some properties are set via another property.
			if (Form.class.isAssignableFrom(persistClass) && "width".equals(name)) //$NON-NLS-1$
			{
				return true;
			}
			if (Portal.class.isAssignableFrom(persistClass) && IContentSpecConstants.PROPERTY_NG_READONLY_MODE.equals(name))
			{
				return true;
			}
			if (Form.class.isAssignableFrom(persistClass) && IContentSpecConstants.PROPERTY_FORM_COMPONENT.equals(name))
			{
				return true;
			}
			return false;
		}
		else if (element.isDeprecated())
		{
			return false;
		}
		if (name.equals("locked")) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("beanClassName")) //$NON-NLS-1$
		{
			return false;
		}
		if (hideForFilter(name))
		{
			return false;
		}
		if (name.equals("relationName") && //$NON-NLS-1$
			!(DocsInsetList.class.isAssignableFrom(persistClass) || Portal.class.isAssignableFrom(persistClass) || Tab.class.isAssignableFrom(persistClass)))
		{
			return false;
		}
		if (name.equals("selectedTabColor")) //$NON-NLS-1$
		{
			return false;//not correctly impl by sun //TODO
		}
		if (name.equals("anchors") && Shape.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("verticalAlignment") && Field.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("styleClass") && (RectShape.class.isAssignableFrom(persistClass) || Shape.class.isAssignableFrom(persistClass))) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("size") && (Form.class.isAssignableFrom(persistClass) || Part.class.isAssignableFrom(persistClass))) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("background") && (Form.class.isAssignableFrom(persistClass) || Tab.class.isAssignableFrom(persistClass))) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("size") && Tab.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("sequence") && Part.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("partType") && Part.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("containsFormID") && RectShape.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("fontType") && (Portal.class.isAssignableFrom(persistClass))) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("style")) //$NON-NLS-1$
		{
			return false;
		}
		if (name.equals("aliases")) //$NON-NLS-1$
		{
			return false; //aliases concept is dumped in favor of useNewFormInstance on tab
		}
		if (name.equals("useNewFormInstance")) //$NON-NLS-1$
		{
			return false;//TODO impl
		}
		if (name.equals("closeOnTabs")) //$NON-NLS-1$
		{
			return false;//TODO impl
		}
		if (name.equals("useRTF")) //useRTF is not longer used  //$NON-NLS-1$
		{
			return false;
		}
		if (RectShape.class.isAssignableFrom(persistClass) && name.endsWith("font")) //$NON-NLS-1$
		{
			return false;
		}
		if ((Shape.class.isAssignableFrom(persistClass)) &&
			(name.endsWith("transparent") || name.endsWith("background") || name.endsWith("name") || name.endsWith("border") || name.endsWith("shapeType") || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				name.endsWith("font") || name.endsWith("size") || name.endsWith("location") || name.endsWith("points"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		{
			return false;
		}
		if (Solution.class.isAssignableFrom(persistClass) && (name.equals("repository") || name.equals("serverProxies"))) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return false;
		}
		if (Relation.class.isAssignableFrom(persistClass) &&
			(name.equals("valid") || name.equals("duplicateRelatedRecords") || name.equals("sortOptions") || name.equals("existsInDB"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		{
			return false;
		}
		if (Solution.class.isAssignableFrom(persistClass) && name.equals("onInitMethodID")) //$NON-NLS-1$
		{
			return false;
		}
		if (Bean.class.isAssignableFrom(persistClass) &&
			(name.equals("parameters") || name.equals("usesUI") || name.equals("beanXML") || name.equals("onActionMethodID"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		{
			return false;
		}
		if ("methodCode".equals(name) || "declaration".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return false;
		}
		if ("blobId".equals(name)) //$NON-NLS-1$
		{
			return false;
		}
		if (ValueList.class.isAssignableFrom(persistClass) && !"name".equals(name) && //$NON-NLS-1$
			!StaticContentSpecLoader.PROPERTY_ENCAPSULATION.getPropertyName().equals(name) &&
			!StaticContentSpecLoader.PROPERTY_DEPRECATED.getPropertyName().equals(name))
		{
			return false;
		}
		if (name.equals(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()) && (Portal.class.isAssignableFrom(persistClass) ||
			TabPanel.class.isAssignableFrom(persistClass) || Bean.class.isAssignableFrom(persistClass) || WebComponent.class.isAssignableFrom(persistClass) ||
			Field.class.isAssignableFrom(persistClass) || GraphicalComponent.class.isAssignableFrom(persistClass) || Tab.class.isAssignableFrom(persistClass) ||
			Shape.class.isAssignableFrom(persistClass) || RectShape.class.isAssignableFrom(persistClass) || Part.class.isAssignableFrom(persistClass)))
		{
			return false;
		}

		if (name.equals(StaticContentSpecLoader.PROPERTY_PLACEHOLDERTEXT.getPropertyName()) && displayType != Field.TEXT_FIELD &&
			displayType != Field.PASSWORD && displayType != Field.TYPE_AHEAD && displayType != Field.TEXT_AREA && displayType >= 0)
		{
			return false;
		}

		if (name.equals(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()) && (displayType == Field.TEXT_FIELD || displayType == Field.CALENDAR ||
			displayType == Field.COMBOBOX || displayType == Field.PASSWORD || displayType == Field.SPINNER || displayType == Field.TYPE_AHEAD))
		{
			return false;
		}

		if ("selectOnEnter".equals(name) && (displayType == Field.CHECKS || displayType == Field.RADIOS)) //$NON-NLS-1$
		{
			return false;
		}

		if (StaticContentSpecLoader.PROPERTY_LABELS.getPropertyName().equals(name))
		{
			return false;
		}

		if (StaticContentSpecLoader.PROPERTY_JSON.getPropertyName().equals(name))
		{
			return false;
		}
		if (StaticContentSpecLoader.PROPERTY_TYPENAME.getPropertyName().equals(name))
		{
			return false;
		}
		return true;
	}

	public static boolean hideForMobileProperties(String name, Class< ? > persistClass, int displayType, boolean isButton)
	{
		if (name.equals(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName()))
		{
			return true;
		}

		if (name.equals(StaticContentSpecLoader.PROPERTY_I18NDATASOURCE.getPropertyName()))
		{
			return true;
		}

		if (GraphicalComponent.class.isAssignableFrom(persistClass) && name.equals(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName()) &&
			!isButton)
		{
			return true;
		}

		// there is no style support for labels & text fields on mobile client
		if (name.equals(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName()))
		{
			if (GraphicalComponent.class.isAssignableFrom(persistClass))
			{
				return !isButton;
			}
			if (Field.class.isAssignableFrom(persistClass))
			{
				return displayType != Field.CHECKS && displayType != Field.RADIOS && displayType != Field.COMBOBOX && displayType >= 0;
			}
			if (Part.class.isAssignableFrom(persistClass))
			{
				return false;
			}
			return true;
		}

		if (name.equals(StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName()) && Field.class.isAssignableFrom(persistClass) &&
			(displayType == Field.TEXT_FIELD || displayType == Field.TEXT_AREA || displayType == Field.PASSWORD))
		{
			return true;
		}

		return false;
	}

	public static String getDisplayName(String displayName, Class< ? > persistClass)
	{
		if (displayName.equals("extendsID") && Form.class.isAssignableFrom(persistClass)) //$NON-NLS-1$
		{
			return "extendsForm"; //$NON-NLS-1$
		}
		if (displayName.endsWith("CmdMethodID")) //$NON-NLS-1$
		{
			return displayName.substring(0, displayName.length() - 11);
		}
		if (displayName.endsWith("MethodID")) //$NON-NLS-1$
		{
			return displayName.substring(0, displayName.length() - 8);
		}
		if (displayName.endsWith("ID")) //$NON-NLS-1$
		{
			return displayName.substring(0, displayName.length() - 2);
		}
		if (Field.class.isAssignableFrom(persistClass) && displayName.equals("text")) //$NON-NLS-1$
		{
			return "titleText"; //$NON-NLS-1$
		}
		return displayName;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public static String generateCSSText(AbstractRepository repository, Map properties) throws RepositoryException
	{
		// props == object_type_id -> HashMap(content_id,prop_value)
		StringBuffer retval = new StringBuffer();

		ContentSpec cs = repository.getContentSpec();
		Iterator it = properties.keySet().iterator();
		while (it.hasNext())
		{
			Integer object_type_id = (Integer)it.next();
			int ioid = object_type_id.intValue();
			HashMap n_v = (HashMap)properties.get(object_type_id);

			// map id to a name in cloned map
			HashMap values = (HashMap)n_v.clone();
			Iterator it2 = n_v.keySet().iterator();
			while (it2.hasNext())
			{
				Integer c_id = (Integer)it2.next();
				ContentSpec.Element cse = cs.getElementByContentID(c_id.intValue());
				values.put(cse.getName(), n_v.get(c_id));
			}

			switch (ioid)
			{
				case IRepository.PORTALS :
				{
					retval.append("portal\n"); //$NON-NLS-1$
					retval.append("{\r\n"); //$NON-NLS-1$
					addBGColor(values, retval);
					addFGColor(values, retval);
					addBorder(values, retval);
					retval.append("}\n\n"); //$NON-NLS-1$
					break;
				}
				case IRepository.FIELDS :
				{
					retval.append("field\n"); //$NON-NLS-1$
					retval.append("{\n"); //$NON-NLS-1$
					addBGColor(values, retval);
					addFGColor(values, retval);
					addBorder(values, retval);
					addFont(values, retval);
					addHAlign(values, retval);
					addMargin(values, retval);
					retval.append("}\n\n"); //$NON-NLS-1$
					break;
				}
				case IRepository.GRAPHICALCOMPONENTS :
				{
					retval.append("label\n"); //$NON-NLS-1$
					retval.append("{\n"); //$NON-NLS-1$
					addBGColor(values, retval);
					addFGColor(values, retval);
					addBorder(values, retval);
					addFont(values, retval);
					addHAlign(values, retval);
					addVAlign(values, retval);
					addMargin(values, retval);
					retval.append("}\n\n"); //$NON-NLS-1$

					retval.append("button\n"); //$NON-NLS-1$
					retval.append("{\n"); //$NON-NLS-1$
					addBGColor(values, retval);
					addFGColor(values, retval);
					addBorder(values, retval);
					addFont(values, retval);
					addHAlign(values, retval);
					addVAlign(values, retval);
					addMargin(values, retval);
					retval.append("}\n\n"); //$NON-NLS-1$
					break;
				}
				case IRepository.TABS :
				{
					retval.append("field\n"); //$NON-NLS-1$
					retval.append("{\n"); //$NON-NLS-1$
					addBGColor(values, retval);
					addFGColor(values, retval);
					addBorder(values, retval);
					retval.append("}\n\n"); //$NON-NLS-1$
					break;
				}
			}
		}
		if (Debug.tracing())
		{
			Debug.trace(retval.toString());
		}
		return retval.toString();
	}

	private static void addMargin(Map values, StringBuffer retval)
	{
		String margin = (String)values.get("margin"); //$NON-NLS-1$
		if (margin != null)
		{
			StringTokenizer tk = new StringTokenizer(margin, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens())
			{
				int top = Utils.getAsInteger(tk.nextToken());
				int right = Utils.getAsInteger(tk.nextToken());
				int bottom = Utils.getAsInteger(tk.nextToken());
				int left = Utils.getAsInteger(tk.nextToken());

				retval.append("\t"); //$NON-NLS-1$
				retval.append("margin: "); //$NON-NLS-1$
				retval.append(top);
				retval.append("px "); //$NON-NLS-1$
				retval.append(right);
				retval.append("px "); //$NON-NLS-1$
				retval.append(bottom);
				retval.append("px "); //$NON-NLS-1$
				retval.append(left);
				retval.append("px"); //$NON-NLS-1$
				retval.append(";"); //$NON-NLS-1$
				retval.append("\n"); //$NON-NLS-1$
			}
		}
	}

	private static void addHAlign(Map values, StringBuffer retval)
	{
		String halign = (String)values.get("horizontalAlignment"); //$NON-NLS-1$
		if (halign != null)
		{
			int ihalign = Utils.getAsInteger(halign);
			retval.append("\t"); //$NON-NLS-1$
			retval.append("text-align: "); //$NON-NLS-1$
			if (ihalign == 2)
			{
				retval.append("left"); //$NON-NLS-1$
			}
			else if (ihalign == 4)
			{
				retval.append("right"); //$NON-NLS-1$
			}
			else
			{
				retval.append("center"); //$NON-NLS-1$
			}
			retval.append(";"); //$NON-NLS-1$
			retval.append("\n"); //$NON-NLS-1$
		}
	}

	private static void addVAlign(Map values, StringBuffer retval)
	{
		String valign = (String)values.get("verticalAlignment"); //$NON-NLS-1$
		if (valign != null)
		{
			int ivalign = Utils.getAsInteger(valign);
			retval.append("\t"); //$NON-NLS-1$
			retval.append("vertical-align: "); //$NON-NLS-1$
			if (ivalign == 1)
			{
				retval.append("text-top"); //$NON-NLS-1$
			}
			else if (ivalign == 3)
			{
				retval.append("text-bottom"); //$NON-NLS-1$
			}
			else
			{
				retval.append("middle"); //$NON-NLS-1$
			}
			retval.append(";"); //$NON-NLS-1$
			retval.append("\n"); //$NON-NLS-1$
		}
	}

	private static void addFont(Map values, StringBuffer retval)
	{
		String fontType = (String)values.get("fontType"); //$NON-NLS-1$
		if (fontType != null)
		{
			StringTokenizer tk = new StringTokenizer(fontType, ","); //$NON-NLS-1$
			if (tk.countTokens() >= 3)
			{
				String name = tk.nextToken();
				String style = tk.nextToken();
				String size = tk.nextToken();

				retval.append("\t"); //$NON-NLS-1$
				retval.append("font-family: "); //$NON-NLS-1$
				retval.append("\""); //$NON-NLS-1$
				retval.append(name);
				retval.append("\""); //$NON-NLS-1$
				retval.append(";"); //$NON-NLS-1$
				retval.append("\n"); //$NON-NLS-1$

				retval.append("\t"); //$NON-NLS-1$
				retval.append("font: "); //$NON-NLS-1$
				if ((Utils.getAsInteger(style) & 1) == 1)
				{
					retval.append("bold "); //$NON-NLS-1$
				}
				else if ((Utils.getAsInteger(style) & 2) == 2)
				{
					retval.append("italic "); //$NON-NLS-1$
				}
				retval.append(size);
				retval.append("pt"); //$NON-NLS-1$
				retval.append(";"); //$NON-NLS-1$
				retval.append("\n"); //$NON-NLS-1$
			}
		}
	}

	private static void addBGColor(Map values, StringBuffer retval)
	{
		String bgcolor = (String)values.get("background"); //$NON-NLS-1$
		String transparent = (String)values.get("transparent"); //$NON-NLS-1$
		if (bgcolor != null && !"".equals(bgcolor)) //$NON-NLS-1$
		{
			retval.append("\t"); //$NON-NLS-1$
			retval.append("background-color: "); //$NON-NLS-1$
			retval.append((Utils.getAsBoolean(transparent) ? "transparent" : bgcolor)); //$NON-NLS-1$
			retval.append(";"); //$NON-NLS-1$
			retval.append("\n"); //$NON-NLS-1$
		}
	}

	private static void addFGColor(Map values, StringBuffer retval)
	{
		String fgcolor = (String)values.get("foreground"); //$NON-NLS-1$
		if (fgcolor != null)
		{
			retval.append("\t"); //$NON-NLS-1$
			retval.append("color: "); //$NON-NLS-1$
			retval.append(fgcolor);
			retval.append(";"); //$NON-NLS-1$
			retval.append("\n"); //$NON-NLS-1$
		}
	}

	private static void addBorder(Map values, StringBuffer retval)
	{
		String border = (String)values.get("borderType"); //$NON-NLS-1$
		if (border != null)
		{
			StringTokenizer tk = new StringTokenizer(border, ","); //$NON-NLS-1$
			if (tk.hasMoreTokens())
			{
				try
				{
					String type = tk.nextToken();
					if (type.equals(ComponentFactoryHelper.EMPTY_BORDER))
					{
						int top = Utils.getAsInteger(tk.nextToken());
						int right = Utils.getAsInteger(tk.nextToken());
						int bottom = Utils.getAsInteger(tk.nextToken());
						int left = Utils.getAsInteger(tk.nextToken());

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-color: "); //$NON-NLS-1$
						retval.append("transparent"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-width: "); //$NON-NLS-1$
						retval.append(top);
						retval.append("px "); //$NON-NLS-1$
						retval.append(right);
						retval.append("px "); //$NON-NLS-1$
						retval.append(bottom);
						retval.append("px "); //$NON-NLS-1$
						retval.append(left);
						retval.append("px"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-style: "); //$NON-NLS-1$
						retval.append("solid"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$
					}
					else if (type.equals(ComponentFactoryHelper.BEVEL_BORDER))
					{
						int beveltype = Utils.getAsInteger(tk.nextToken());
						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-style: "); //$NON-NLS-1$
						retval.append((beveltype == 0 ? "outset" : "inset")); //$NON-NLS-1$ //$NON-NLS-2$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$

						if (tk.hasMoreTokens())
						{
							String highlightO = tk.nextToken();
							if ("null".equals(highlightO)) highlightO = null; //$NON-NLS-1$
							String highlightI = tk.nextToken();
							if ("null".equals(highlightI)) highlightI = null; //$NON-NLS-1$
							String shadowO = tk.nextToken();
							if ("null".equals(shadowO)) shadowO = null; //$NON-NLS-1$
							String shadowI = tk.nextToken();
							if ("null".equals(shadowI)) shadowI = null; //$NON-NLS-1$

							if (highlightO != null)
							{
								retval.append("\t"); //$NON-NLS-1$
								retval.append("border-color: "); //$NON-NLS-1$
								retval.append(highlightO);
								retval.append(" "); //$NON-NLS-1$
								retval.append(highlightI);
								retval.append(" "); //$NON-NLS-1$
								retval.append(shadowO);
								retval.append(" "); //$NON-NLS-1$
								retval.append(shadowI);
								retval.append(";"); //$NON-NLS-1$
								retval.append("\n"); //$NON-NLS-1$
							}
						}
					}
					else if (type.equals(ComponentFactoryHelper.ETCHED_BORDER))
					{
						/* int beveltype = */Utils.getAsInteger(tk.nextToken());
						/* String highlight = */tk.nextToken();
						/* String shadow = */tk.nextToken();
						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-style: "); //$NON-NLS-1$
						retval.append("groove"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$
					}
					else if (type.equals(ComponentFactoryHelper.LINE_BORDER))
					{
						int thick = Utils.getAsInteger(tk.nextToken());
						String color = tk.nextToken();
						if ("null".equals(color)) color = null; //$NON-NLS-1$

						if (color != null)
						{
							retval.append("\t"); //$NON-NLS-1$
							retval.append("border-color: "); //$NON-NLS-1$
							retval.append(color);
							retval.append(";"); //$NON-NLS-1$
							retval.append("\n"); //$NON-NLS-1$
						}

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-style: "); //$NON-NLS-1$
						retval.append("solid"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-width: "); //$NON-NLS-1$
						retval.append(thick);
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$
					}
					else if (type.equals(ComponentFactoryHelper.MATTE_BORDER))
					{
						int top = Utils.getAsInteger(tk.nextToken());
						int right = Utils.getAsInteger(tk.nextToken());
						int bottom = Utils.getAsInteger(tk.nextToken());
						int left = Utils.getAsInteger(tk.nextToken());
						String color = tk.nextToken();
						if ("null".equals(color)) color = null; //$NON-NLS-1$
						if (color != null)
						{
							retval.append("\t"); //$NON-NLS-1$
							retval.append("border-color: "); //$NON-NLS-1$
							retval.append(color);
							retval.append(";"); //$NON-NLS-1$
							retval.append("\n"); //$NON-NLS-1$
						}

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-width: "); //$NON-NLS-1$
						retval.append(top);
						retval.append("px "); //$NON-NLS-1$
						retval.append(right);
						retval.append("px "); //$NON-NLS-1$
						retval.append(bottom);
						retval.append("px "); //$NON-NLS-1$
						retval.append(left);
						retval.append("px"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$
					}
					else if (type.equals(ComponentFactoryHelper.SPECIAL_MATTE_BORDER))
					{
						int top = (int)Utils.getAsFloat(tk.nextToken());
						int right = (int)Utils.getAsFloat(tk.nextToken());
						int bottom = (int)Utils.getAsFloat(tk.nextToken());
						int left = (int)Utils.getAsFloat(tk.nextToken());
						String topColor = tk.nextToken();
						if ("null".equals(topColor)) topColor = null; //$NON-NLS-1$
						String rightColor = tk.nextToken();
						if ("null".equals(rightColor)) rightColor = null; //$NON-NLS-1$
						String bottomColor = tk.nextToken();
						if ("null".equals(bottomColor)) bottomColor = null; //$NON-NLS-1$
						String leftColor = tk.nextToken();
						if ("null".equals(leftColor)) leftColor = null; //$NON-NLS-1$

						if (topColor != null)
						{
							retval.append("\t"); //$NON-NLS-1$
							retval.append("border-color: "); //$NON-NLS-1$
							retval.append(topColor);
							retval.append(" "); //$NON-NLS-1$
							retval.append(rightColor);
							retval.append(" "); //$NON-NLS-1$
							retval.append(bottomColor);
							retval.append(" "); //$NON-NLS-1$
							retval.append(leftColor);
							retval.append(";"); //$NON-NLS-1$
							retval.append("\n"); //$NON-NLS-1$
						}

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-width: "); //$NON-NLS-1$
						retval.append(top);
						retval.append("px "); //$NON-NLS-1$
						retval.append(right);
						retval.append("px "); //$NON-NLS-1$
						retval.append(bottom);
						retval.append("px "); //$NON-NLS-1$
						retval.append(left);
						retval.append("px"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$

						retval.append("\t"); //$NON-NLS-1$
						retval.append("border-style: "); //$NON-NLS-1$
						retval.append("solid"); //$NON-NLS-1$
						retval.append(";"); //$NON-NLS-1$
						retval.append("\n"); //$NON-NLS-1$
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
		}
	}

}
