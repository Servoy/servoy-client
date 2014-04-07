package com.servoy.j2db.server.ngclient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.border.Border;

import org.json.JSONException;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.persistence.AbstractPersistFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.component.RuntimeLegacyComponent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.component.WebComponentSpec;
import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.server.ngclient.utils.JSONUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

public class WebFormUI extends WebComponent implements IWebFormUI
{
	protected final Map<String, WebComponent> components = new HashMap<>();
	private final INGApplication application;
	private final IFormController formController;

	private boolean enabled = true;
	private boolean readOnly = false;
	private WebComponent parentContainer;

	public WebFormUI(IFormController formController)
	{
		super(formController.getName(), formController.getForm());
		this.formController = formController;
		this.application = (INGApplication)formController.getApplication();
		init();
	}

	/**
	 * this is a full recreate ui.
	 * 
	 * @param formController
	 * @param dal
	 * @return
	 * @throws RepositoryException
	 */
	public void init()
	{
		components.clear();
		DataAdapterList dal = new DataAdapterList(application, formController);
		Form form = formController.getForm();
		ElementScope elementsScope = initElementScope(formController);
		List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), application.getFlattenedSolution());
		int counter = 0;
		for (FormElement fe : formElements)
		{
			WebComponentSpec componentSpec = fe.getWebComponentSpec();

			WebComponent component = ComponentFactory.createComponent(application, dal, fe, this);
			if (!fe.getName().startsWith("svy_"))
			{
				RuntimeWebComponent runtimeComponent = new RuntimeWebComponent(component, componentSpec);
				elementsScope.put(fe.getName(), formController.getFormScope(), runtimeComponent);
				elementsScope.put(counter++, formController.getFormScope(), runtimeComponent);
				if (fe.isLegacy())
				{
					// add legacy behavior
					runtimeComponent.setPrototype(new RuntimeLegacyComponent(component));
				}
			}
			add(component);

			for (String propName : fe.getProperties().keySet())
			{
				if (fe.getPropertyWithDefault(propName) == null || componentSpec.getProperty(propName) == null) continue; //TODO this if should not be necessary. currently in the case of "printable" hidden property
				fillProperties(fe.getForm(), fe, fe.getPropertyWithDefault(propName), componentSpec.getProperty(propName), dal, component, component, "");
			}

			for (String eventName : componentSpec.getHandlers().keySet())
			{
				Object eventValue = fe.getProperty(eventName);
				if (eventValue instanceof String)
				{
					UUID uuid = UUID.fromString((String)eventValue);
					try
					{
						component.add(eventName, ((AbstractPersistFactory)ApplicationServerRegistry.get().getLocalRepository()).getElementIdForUUID(uuid));
					}
					catch (RepositoryException e)
					{
						Debug.error(e);
					}
				}
				else if (eventValue instanceof Number && ((Number)eventValue).intValue() > 0)
				{
					component.add(eventName, ((Number)eventValue).intValue());

				}
			}
		}

		DefaultNavigatorWebComponent nav = (DefaultNavigatorWebComponent)components.get(DefaultNavigator.NAME_PROP_VALUE);
		if (nav != null)
		{
			nav.newFoundset(null);
		}
		// special support for the default navigator
		if (formController.getForm().getNavigatorID() == Form.NAVIGATOR_DEFAULT)
		{
			add(new DefaultNavigatorWebComponent(dal));
		}

		if (dataAdapterList != null)
		{
			IRecordInternal record = ((DataAdapterList)dataAdapterList).getRecord();
			if (record != null)
			{
				dal.setRecord(record, false);
				dataAdapterList.setRecord(null, false);

				nav = (DefaultNavigatorWebComponent)components.get(DefaultNavigator.NAME_PROP_VALUE);
				if (nav != null) nav.newFoundset(record.getParentFoundSet());
			}
		}
		dataAdapterList = dal;
	}

	/**
	 *  -fe is only needed because of format . It accesses another property value based on the 'for' property (). TODO this FormElement parameter should be analyzed because format accepts a flat property value.
	 *  
	 * -level is only needed because the current implementation 'flattens' the dataproviderid's and tagstrings for DAL  .(level should be removed after next changes)
	 *  
	 *  -component is the whole component for now ,but it should be the current component node in the runtime component tree (instead of flat properties map)
	 *  -component and componentNode should have been just componentNode (but currently WebCoponent is not nested)
	 */
	public void fillProperties(Form formElNodeForm, FormElement fe, Object formElementProperty, PropertyDescription propertySpec, DataAdapterList dal,
		WebComponent component, Object componentNode, String level)
	{
		if (propertySpec.isArray())
		{
			List<Object> processedArray = new ArrayList<>();
			List<Object> fePropertyArray = (List<Object>)formElementProperty;
			for (Object arrayValue : fePropertyArray)
			{
				Object propValue = initFormElementProperty(formElNodeForm, fe, arrayValue, propertySpec, dal, component, componentNode, level, true);
				switch (propertySpec.getType())
				{
					case dataprovider : // array of dataprovider is not supported yet (DAL does not support arrays)  , Should be done in initFormElementProperty()
					{
						Debug.error("Array of dataprovider currently not supported dataprovider");
						Object dataproviderID = propValue;
						if (dataproviderID instanceof String)
						{
							dal.add(component, level + (String)dataproviderID, propertySpec.getName());
						}
						break;
					}
					case tagstring : // array of taggstring is not supported yet (DAL does not support arrays)
					{
						Debug.error("Array of tagstring currently not supported dataprovider");
						//bind tag expressions
						//for each property with tags ('tagstring' type), add it's dependent tags to the DAL 
						if (propValue != null && propValue instanceof String && ((String)propValue).contains("%%"))
						{
							dal.addTaggedProperty(component, level + propertySpec.getName());
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
				putInComponentNode(componentNode, propertySpec.getName(), processedArray);
			}
		}
		else
		{
			Object propValue = initFormElementProperty(formElNodeForm, fe, formElementProperty, propertySpec, dal, component, componentNode, level, false);
			String propName = propertySpec.getName();
			switch (propertySpec.getType())
			{
				case dataprovider : // array of dataprovider is not supported yet (DAL does not support arrays)
				{
					Object dataproviderID = formElementProperty;
					if (dataproviderID instanceof String)
					{
						dal.add(component, (String)dataproviderID, level + propName);
					}
					break;
				}
				case tagstring : // array of taggstring is not supported yet (DAL does not support arrays)
				{
					//bind tag expressions
					//for each property with tags ('tagstring' type), add it's dependent tags to the DAL 
					if (propValue != null && propValue instanceof String && ((String)propValue).contains("%%"))
					{
						dal.addTaggedProperty(component, level + propName);
					}
					break;
				}
				case valuelist : // skip valuelistID , it is handled elsewhere (should be changed to be handled here?)
					break;
			}
			if (propValue != null) putInComponentNode(componentNode, propName, propValue); //TODO
		}
	}

	/**
	 * TEMPORARY FUNCTION until we move to nested web component tree , with each node having semantics (PropertyType)
	 *  Webcomponent will be a tree 
	 */
	private void putInComponentNode(Object componentNode, String propName, Object propValue)
	{
		if (componentNode instanceof WebComponent)
		{
			((WebComponent)componentNode).putProperty(propName, propValue);
		}
		else
		{
			((Map)componentNode).put(propName, propValue);
		}
	}

	/**
	 *  TODO merge component and component node remove isarrayElement parameter
	 * @return
	 */
	private Object initFormElementProperty(Form formElNodeForm, FormElement fe, Object formElementProperty, PropertyDescription propertySpec,
		DataAdapterList dal, WebComponent component, Object componentNode, String level, boolean isArrayElement)
	{
		Object ret = null;
		switch (propertySpec.getType())
		{
//			case dataprovider :
//			{
//				Object dataproviderID = formElementProperty;
//				if (dataproviderID instanceof String)
//				{
//					return dataproviderID;
//				}
//				break;
//			}
//			case tagstring :
//			{
//				Object propValue = formElementProperty;
//				//bind tag expressions
//				//for each property with tags ('tagstring' type), add it's dependent tags to the DAL 
//				if (propValue != null && propValue instanceof String && ((String)propValue).contains("%%"))
//				{
//					dal.addTaggedProperty(component, propName); 
//				}
//				break;
//			}
			case format :
			{
				Object propValue = formElementProperty;
				if (propValue instanceof String)
				{
					// get dataproviderId
					String dataproviderId = (String)fe.getProperties().get(propertySpec.getConfig());
					ComponentFormat format = ComponentFormat.getComponentFormat((String)propValue, dataproviderId,
						application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), formElNodeForm), application);
					ret = format;
				}
				break;
			}
			case bean :
			{
				Object propValue = formElementProperty;
				if (propValue instanceof String)
				{
					ret = ComponentFactory.getMarkupId(fe.getName(), (String)propValue);
				}
				break;
			}
			case valuelist : // skip valuelistID , it is handled elsewhere (should be changed to be handled here?)
				break;
			case custom :
			{
				Map<String, PropertyDescription> props = ((Map<String, PropertyDescription>)formElementProperty);
				Map<String, Object> newComponentNode = new HashMap<>();
				//putInComponentNode(componentNode, propertySpec.getName(), newComponentNode); // TODO
				for (String prop : props.keySet())
				{
					PropertyDescription localPropertyDescription = propertySpec.getProperty(prop);
					String innerLevelpropName = level + propertySpec.getName();
					fillProperties(formElNodeForm, fe, props.get(prop), localPropertyDescription, dal, component, newComponentNode, isArrayElement ? ""
						: innerLevelpropName + ".");
				}
				ret = newComponentNode;
				break;
			}
			default :
			{
				ret = formElementProperty;
			}
		}
		return ret;
	}


	public IDataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

	public void add(WebComponent component)
	{
		components.put(component.getName(), component);
	}

	public WebComponent getWebComponent(String name)
	{
		return components.get(name);
	}

	public Map<String, WebComponent> getWebComponents()
	{
		return components;
	}

	public Map<String, Map<String, Object>> getAllProperties()
	{
		Map<String, Map<String, Object>> props = new HashMap<String, Map<String, Object>>();

		ArrayList<WebComponent> allComponents = new ArrayList<WebComponent>();
		allComponents.add(this); // add the form itself
		allComponents.addAll(components.values());

		for (WebComponent wc : allComponents)
		{
			props.put(wc == this ? "" : wc.getName(), wc.getProperties()); //$NON-NLS-1$
		}

		return props;
	}

	public Map<String, Map<String, Object>> getAllChanges()
	{
		Map<String, Map<String, Object>> props = null;

		ArrayList<WebComponent> allComponents = new ArrayList<WebComponent>();
		allComponents.add(this); // add the form itself
		allComponents.addAll(components.values());

		for (WebComponent wc : allComponents)
		{
			Map<String, Object> changes = wc.getChanges();
			if (changes.size() > 0)
			{
				if (props == null) props = new HashMap<String, Map<String, Object>>(8);
				props.put(wc == this ? "" : wc.getName(), changes); //$NON-NLS-1$
			}
		}
		if (props == null) return Collections.emptyMap();
		return props;
	}

	private ElementScope initElementScope(IFormController controller)
	{
		FormScope formScope = controller.getFormScope();
		ElementScope elementsScope = new ElementScope(formScope);
		formScope.putWithoutFireChange("elements", elementsScope); //$NON-NLS-1$
		return elementsScope;
	}

	@Override
	public void putBrowserProperty(String propertyName, Object propertyValue) throws JSONException
	{
		if ("size".equals(propertyName))
		{
			properties.put(propertyName,
				JSONUtils.toJavaObject(propertyValue, new PropertyDescription("size", PropertyType.dimension), application.getFlattenedSolution()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IFormUI#getController()
	 */
	@Override
	public IForm getController()
	{
		return formController;
	}

	@Override
	public boolean isDisplayingMoreThanOneRecord()
	{
		return false;
	}

	@Override
	public void destroy()
	{
		components.clear();
	}

	@Override
	public void setModel(IFoundSetInternal fs)
	{
		DefaultNavigatorWebComponent nav = (DefaultNavigatorWebComponent)components.get(DefaultNavigator.NAME_PROP_VALUE);
		if (nav != null)
		{
			nav.newFoundset(fs);
		}
	}

	@Override
	public void setComponentEnabled(boolean enabled)
	{
		this.enabled = enabled;
		propagatePropertyToAllComponents("enabled", enabled);
	}

	@Override
	public boolean isEnabled()
	{
		return enabled;
	}

	@Override
	public boolean isReadOnly()
	{
		return readOnly;
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		propagatePropertyToAllComponents("readOnly", readOnly);
	}

	private void propagatePropertyToAllComponents(String property, Object value)
	{
		FormScope formScope = formController.getFormScope();
		if (formScope != null)
		{
			ElementScope elementScope = (ElementScope)formScope.get("elements", null); //$NON-NLS-1$
			if (elementScope != null)
			{
				Object[] components = elementScope.getValues();
				if (components != null)
				{
					for (Object component : components)
					{
						if (component instanceof RuntimeWebComponent)
						{
							((RuntimeWebComponent)component).put(property, null, value);
						}
					}
				}
			}
		}
	}

	@Override
	public int recalculateTabIndex(int startIndex, TabSequencePropertyWithComponent startComponent)
	{
		int currentIndex = startIndex;
		List<TabSequencePropertyWithComponent> tabSeqComponents = getTabSeqComponents();
		int startIndexInList = 0;
		if (startComponent != null)
		{
			startIndexInList = tabSeqComponents.indexOf(startComponent);
		}
		for (int i = Math.max(0, startIndexInList); i < tabSeqComponents.size(); i++)
		{
			tabSeqComponents.get(i).setCalculatedTabSequence(currentIndex++);
		}
		if (startComponent != null && parentContainer != null)
		{
			// upwards traversal
			parentContainer.recalculateTabSequence(currentIndex);
		}
		nextAvailableTabSequence = currentIndex;
		return currentIndex;
	}

	protected List<TabSequencePropertyWithComponent> getTabSeqComponents()
	{
		SortedList<TabSequencePropertyWithComponent> tabSeqComponents = new SortedList<TabSequencePropertyWithComponent>(
			new Comparator<TabSequencePropertyWithComponent>()
			{
				@Override
				public int compare(TabSequencePropertyWithComponent o1, TabSequencePropertyWithComponent o2)
				{
					int seq1 = o1.getTabSequence();
					int seq2 = o2.getTabSequence();

					if (seq1 == ISupportTabSeq.DEFAULT && seq2 == ISupportTabSeq.DEFAULT)
					{
						//delegate to Yx
						int yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(o1.getPersist(), o2.getPersist());
						// if they are at the same position, and are different persist, just use UUID to decide the sequence
						return yxCompare == 0 ? o1.getPersist().getUUID().compareTo(o2.getPersist().getUUID()) : yxCompare;
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

			});
		for (WebComponent comp : components.values())
		{
			Map<String, PropertyDescription> tabSeqProps = comp.getFormElement().getWebComponentSpec().getProperties(PropertyType.tabseq);
			for (PropertyDescription pd : tabSeqProps.values())
			{
				if (Utils.getAsInteger(comp.getInitialProperty(pd.getName())) >= 0)
				{
					tabSeqComponents.add(new TabSequencePropertyWithComponent(comp, pd.getName()));
				}
			}
		}
		return tabSeqComponents;
	}

	public void setParentContainer(WebComponent parentContainer)
	{
		this.parentContainer = parentContainer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setComponentVisible(boolean)
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#isVisible()
	 */
	@Override
	public boolean isVisible()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setLocation(java.awt.Point)
	 */
	@Override
	public void setLocation(Point location)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getLocation()
	 */
	@Override
	public Point getLocation()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setSize(java.awt.Dimension)
	 */
	@Override
	public void setSize(Dimension size)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getSize()
	 */
	@Override
	public Dimension getSize()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setForeground(java.awt.Color)
	 */
	@Override
	public void setForeground(Color foreground)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getForeground()
	 */
	@Override
	public Color getForeground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setBackground(java.awt.Color)
	 */
	@Override
	public void setBackground(Color background)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getBackground()
	 */
	@Override
	public Color getBackground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setFont(java.awt.Font)
	 */
	@Override
	public void setFont(Font font)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getFont()
	 */
	@Override
	public Font getFont()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setBorder(javax.swing.border.Border)
	 */
	@Override
	public void setBorder(Border border)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getBorder()
	 */
	@Override
	public Border getBorder()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setOpaque(boolean)
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#isOpaque()
	 */
	@Override
	public boolean isOpaque()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setCursor(java.awt.Cursor)
	 */
	@Override
	public void setCursor(Cursor cursor)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setToolTipText(java.lang.String)
	 */
	@Override
	public void setToolTipText(String tooltip)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#getId()
	 */
	@Override
	public String getId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#showYesNoQuestionDialog(com.servoy.j2db.IApplication, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean showYesNoQuestionDialog(IApplication application, String dlgMessage, String string)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#getContainerName()
	 */
	@Override
	public String getContainerName()
	{
		return (String)application.getActiveWebSocketClientEndpoint().executeDirectServiceCall("$windowService", "getContainerName",
			new Object[] { formController.getForm() });
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#printPreview(boolean, boolean, int, java.awt.print.PrinterJob)
	 */
	@Override
	public void printPreview(boolean showDialogs, boolean printCurrentRecordOnly, int zoomFactor, PrinterJob printerJob)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#print(boolean, boolean, boolean, java.awt.print.PrinterJob)
	 */
	@Override
	public void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#printXML(boolean)
	 */
	@Override
	public String printXML(boolean printCurrentRecordOnly)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#showSortDialog(com.servoy.j2db.IApplication, java.lang.String)
	 */
	@Override
	public void showSortDialog(IApplication application, String options)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#getFormWidth()
	 */
	@Override
	public int getFormWidth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#getPartHeight(int)
	 */
	@Override
	public int getPartHeight(int partType)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#getFormContext()
	 */
	@Override
	public JSDataSet getFormContext()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#changeFocusIfInvalid(java.util.List)
	 */
	@Override
	public void changeFocusIfInvalid(List<Runnable> invokeLaterRunnables)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IBasicFormUI#prepareForSave(boolean)
	 */
	@Override
	public void prepareForSave(boolean looseFocus)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#start(com.servoy.j2db.IApplication)
	 */
	@Override
	public void start(IApplication app)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#stop()
	 */
	@Override
	public void stop()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#editCellAt(int)
	 */
	@Override
	public boolean editCellAt(int row)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#stopUIEditing(boolean)
	 */
	@Override
	public boolean stopUIEditing(boolean looseFocus)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#isEditing()
	 */
	@Override
	public boolean isEditing()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#requestFocus()
	 */
	@Override
	public void requestFocus()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#ensureIndexIsVisible(int)
	 */
	@Override
	public void ensureIndexIsVisible(int index)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#setEditable(boolean)
	 */
	@Override
	public void setEditable(boolean findMode)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#getVisibleRect()
	 */
	@Override
	public Rectangle getVisibleRect()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IView#setVisibleRect(java.awt.Rectangle)
	 */
	@Override
	public void setVisibleRect(Rectangle scrollPosition)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.ISupportRowBGColorScript#setRowBGColorScript(java.lang.String, java.util.List)
	 */
	@Override
	public void setRowBGColorScript(String bgColorScript, List<Object> args)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.ISupportRowBGColorScript#getRowBGColorScript()
	 */
	@Override
	public String getRowBGColorScript()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.ISupportRowBGColorScript#getRowBGColorArgs()
	 */
	@Override
	public List<Object> getRowBGColorArgs()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
