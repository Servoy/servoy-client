package com.servoy.j2db.server.ngclient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.border.Border;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.VisiblePropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.component.RuntimeLegacyComponent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebGroup;
import com.servoy.j2db.server.ngclient.property.types.NGEnabledPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGEnabledSabloValue;
import com.servoy.j2db.server.ngclient.property.types.ReadonlyPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ReadonlySabloValue;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class WebFormUI extends Container implements IWebFormUI, IContextProvider
{
	public static final String ENABLED = "enabled";
	public static final String READONLY = "readOnly";

	/**
	 * @author jcomp
	 *
	 */
	private final class ReadOnlyPropertyChangeListener implements PropertyChangeListener
	{
		private boolean parentForcedReadOnly;

		public ReadOnlyPropertyChangeListener(boolean parentForcedReadOnly)
		{
			this.parentForcedReadOnly = parentForcedReadOnly;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			// this is a parent forced readonly (if it is true), remember this so that we can force the readonly back to false
			parentForcedReadOnly = (boolean)evt.getNewValue();
			((BasicFormController)getController()).setReadOnly((boolean)evt.getNewValue());
		}
	}

	private static final class FormSpecification extends WebObjectSpecification
	{
		private FormSpecification()
		{
			super("form_spec", "", IPackageReader.WEB_COMPONENT, "", null, null, null, "", null);
			putProperty("size", new PropertyDescription("size", DimensionPropertyType.INSTANCE, PushToServerEnum.allow));
			putProperty("visible", new PropertyDescription("visible", VisiblePropertyType.INSTANCE, PushToServerEnum.allow));
			putProperty(WebFormUI.ENABLED, new PropertyDescription(WebFormUI.ENABLED, NGEnabledPropertyType.NG_INSTANCE, PushToServerEnum.allow));
			putProperty("findmode", new PropertyDescription("findmode", BooleanPropertyType.INSTANCE, PushToServerEnum.allow));
		}
	}

	private static final WebObjectSpecification FORM_SPEC = new FormSpecification();

	private final Map<String, Integer> events = new HashMap<>(); //event name mapping to persist id
	private final IWebFormController formController;

	private Object parentContainerOrWindowName;

	protected DataAdapterList dataAdapterList;

	private ReadOnlyPropertyChangeListener parentReadOnlyListener;

	protected List<FormElement> cachedElements = new ArrayList<FormElement>();
	private final Map<String, RuntimeWebGroup> groups = new HashMap<String, RuntimeWebGroup>();

	public WebFormUI(IWebFormController formController)
	{
		super(formController.getName(), FORM_SPEC);
		this.formController = formController;
		setVisible(false);
		setEnabled(true);
		setProperty("findmode", Boolean.FALSE);
		init();
	}

	public final INGApplication getApplication()
	{
		return formController.getApplication();
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
		clearComponents();
		cachedElements.clear();
		groups.clear();
		IDataAdapterList previousDataAdapterList = dataAdapterList;
		dataAdapterList = new DataAdapterList(formController);

		ElementScope elementsScope = initElementScope(formController);
		List<FormElement> formElements = getFormElements();
		for (FormElement fe : formElements)
		{
			// TODO do something similar for child elements (so properties of type 'components' which contain componentSpecs in them)

			WebObjectSpecification componentSpec = fe.getWebComponentSpec(false);
			if (componentSpec == null)
			{
				getApplication().reportError("Didn't find a spec file for component " + fe + " when creating form: " + formController.getName(), null);
				continue;
			}

			WebFormComponent component = ComponentFactory.createComponent(getApplication(), dataAdapterList, fe, this, getController().getForm());

			if (component != null)
			{
				contributeComponentToElementsScope(elementsScope, fe, componentSpec, component);
			}
		}

		DefaultNavigatorWebComponent nav = (DefaultNavigatorWebComponent)getComponent(DefaultNavigator.NAME_PROP_VALUE);
		if (nav != null)
		{
			nav.newFoundset(null);
		}
		// special support for the default navigator
		if (formController.getForm().getNavigatorID() == Form.NAVIGATOR_DEFAULT)
		{
			add(new DefaultNavigatorWebComponent(dataAdapterList));
		}

		if (previousDataAdapterList != null)
		{
			IRecordInternal record = ((DataAdapterList)previousDataAdapterList).getRecord();
			if (record != null)
			{
				dataAdapterList.setRecord(record, false);
				previousDataAdapterList.setRecord(null, false);

				nav = (DefaultNavigatorWebComponent)getComponent(DefaultNavigator.NAME_PROP_VALUE);
				if (nav != null) nav.newFoundset(record.getParentFoundSet());
			}
			previousDataAdapterList.destroy();
		}
	}

	// this should be the opposite of formUI.contributeComponentToElementsScope(...)
	public void removeComponentFromElementsScope(FormElement fe, WebObjectSpecification componentSpec, WebFormComponent component)
	{
		ElementScope elementsScope = getElementsScope();
		if (elementsScope != null)
		{
			if (!FormElement.ERROR_BEAN.equals(componentSpec.getName()) && (!fe.getName().startsWith("svy_") ||
				((fe.getPersistIfAvailable() instanceof IFormElement) && ((IFormElement)fe.getPersistIfAvailable()).getGroupID() != null)))
			{
				if (!fe.getName().startsWith("svy_"))
				{
					RuntimeWebComponent runtimeComponent = (RuntimeWebComponent)elementsScope.remove(fe.getRawName());
					elementsScope.removeIndexByValue(runtimeComponent);
				}

				String groupID = fe.getPersistIfAvailable() instanceof IFormElement ? ((IFormElement)fe.getPersistIfAvailable()).getGroupID() : null;
				if (groupID != null)
				{
					RuntimeWebGroup group = groups.get(groupID);
					if (group != null)
					{
						String groupName = FormElementGroup.getName(groupID);
						group.remove(component);
						if (group.getComponentCount() == 0) elementsScope.remove(groupName);
						groups.remove(groupID);
					}
				}
			}
		}
		else
		{
			Debug.error(new RuntimeException("Trying to remove component from a non-existent elements scope for form: " + getName()));
		}
	}

	public void contributeComponentToElementsScope(FormElement fe, WebObjectSpecification componentSpec, WebFormComponent component)
	{
		ElementScope elementsScope = getElementsScope();
		if (elementsScope != null)
		{
			contributeComponentToElementsScope(elementsScope, fe, componentSpec, component);
		}
		else
		{
			Debug.error(new RuntimeException("Trying to contribute a component to a non-existent elements scope for form: " + getName()));
		}
	}

	private void contributeComponentToElementsScope(ElementScope elementsScope, FormElement fe, WebObjectSpecification componentSpec,
		WebFormComponent component)
	{
		Object lengthOfEl = elementsScope.get("length", elementsScope);
		int lastElementByIndex = (lengthOfEl instanceof Integer ? ((Integer)lengthOfEl).intValue() : 0);

		if (!FormElement.ERROR_BEAN.equals(componentSpec.getName()) && (!fe.getName().startsWith("svy_") ||
			((fe.getPersistIfAvailable() instanceof IFormElement) && ((IFormElement)fe.getPersistIfAvailable()).getGroupID() != null)))
		{
			RuntimeWebComponent runtimeComponent = new RuntimeWebComponent(component, componentSpec);
			if (fe.isLegacy() || ((fe.getForm().getView() == IForm.LIST_VIEW || fe.getForm().getView() == FormController.LOCKED_LIST_VIEW ||
				fe.getForm().getView() == FormController.TABLE_VIEW || fe.getForm().getView() == FormController.LOCKED_TABLE_VIEW) &&
				fe.getTypeName().startsWith("servoydefault-")))

			{
				// add legacy behavior
				runtimeComponent.setPrototype(new RuntimeLegacyComponent(component));
			}
			if (!fe.getName().startsWith("svy_"))
			{
				elementsScope.put(fe.getRawName(), formController.getFormScope(), runtimeComponent);
				elementsScope.put(lastElementByIndex, formController.getFormScope(), runtimeComponent);
			}

			String groupID = fe.getPersistIfAvailable() instanceof IFormElement ? ((IFormElement)fe.getPersistIfAvailable()).getGroupID() : null;
			if (groupID != null)
			{
				RuntimeWebGroup group = groups.get(groupID);
				if (group == null)
				{
					String groupName = FormElementGroup.getName(groupID);
					group = new RuntimeWebGroup(groupName);
					group.setParentScope(component.getDataConverterContext().getApplication().getScriptEngine().getSolutionScope());
					elementsScope.put(groupName, formController.getFormScope(), group);
					groups.put(groupID, group);
				}
				group.add(runtimeComponent);
			}
		}
	}

	public IServoyDataConverterContext getDataConverterContext()
	{
		return new ServoyDataConverterContext(formController);
	}

	public IDataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

	public void add(String eventType, int functionID)
	{
		events.put(eventType, Integer.valueOf(functionID));
	}

	@Override
	public boolean hasEvent(String eventType)
	{
		return events.containsKey(eventType);
	}

	@Override
	public Object doExecuteEvent(String eventType, Object[] args)
	{
		Integer eventId = events.get(eventType);
		if (eventId != null)
		{
			return dataAdapterList.executeEvent(this, eventType, eventId.intValue(), args);
		}
		throw new IllegalArgumentException("Unknown event '" + eventType + "' for component " + this);
	}

	@Override
	public WebFormComponent getWebComponent(String compname)
	{
		WebComponent wc = findChildComponent(compname, this);

		return wc instanceof WebFormComponent ? (WebFormComponent)wc : null;
	}

	private static WebComponent findChildComponent(String compname, WebComponent rootComponent)
	{
		if (compname.equals(rootComponent.getName())) return rootComponent;
		if (rootComponent instanceof Container)
		{
			Collection<WebComponent> childComponents = ((Container)rootComponent).getComponents();
			if (childComponents.size() > 0)
			{
				for (WebComponent wc : childComponents)
				{
					WebComponent foundChildComponent = findChildComponent(compname, wc);
					if (foundChildComponent != null) return foundChildComponent;
				}
			}
		}
		return null;
	}

	public Collection<WebComponent> getScriptableComponents()
	{
		List<WebComponent> components = new ArrayList<WebComponent>();
		Object[] names = getElementsScope().getIds();
		if (names != null)
		{
			for (Object componentName : names)
			{
				Object component = getElementsScope().get((String)componentName, null);
				if (component instanceof RuntimeWebComponent)
				{
					components.add(((RuntimeWebComponent)component).getComponent());
				}
			}
		}
		return components;
	}

	public Collection<WebComponent> getAllComponents()
	{
		Collection<WebComponent> allComponents = new ArrayList<WebComponent>();
		addAllChildren(this, allComponents);
		return allComponents;
	}

	private void addAllChildren(Container parent, Collection<WebComponent> allComponents)
	{
		Collection<WebComponent> directChildren = parent.getComponents();
		allComponents.addAll(directChildren);
		for (WebComponent component : directChildren)
		{
			if (component instanceof Container)
			{
				addAllChildren((Container)component, allComponents);
			}
		}
	}

	@Override
	public boolean writeAllComponentsProperties(JSONWriter w, IToJSONConverter<IBrowserConverterContext> converter) throws JSONException
	{
		try
		{
			getController().setRendering(true);
			return super.writeAllComponentsProperties(w, converter);
		}
		finally
		{
			getController().setRendering(false);
		}
	}

	@Override
	public boolean writeAllComponentsChanges(JSONWriter w, String keyInParent, IToJSONConverter<IBrowserConverterContext> converter,
		DataConversion clientDataConversions) throws JSONException
	{
		try
		{
			getController().setRendering(true);
			// converter here is always ChangesToJSONConverter except for some unit tests
			return super.writeAllComponentsChanges(w, keyInParent, converter, clientDataConversions);
		}
		finally
		{
			getController().setRendering(false);
		}
	}

	private ElementScope initElementScope(IFormController controller)
	{
		FormScope formScope = controller.getFormScope();
		ElementScope elementsScope = new ElementScope(formScope);
		formScope.putWithoutFireChange("elements", elementsScope); //$NON-NLS-1$
		return elementsScope;
	}

	@Override
	public void doPutBrowserProperty(String propertyName, Object propertyValue) throws JSONException
	{
		// TODO: convert this to property change listener
		if ("size".equals(propertyName))
		{
			Dimension prev = (Dimension)properties.get("size");
			super.doPutBrowserProperty(propertyName, propertyValue);
			Dimension newSize = (Dimension)properties.get("size");
			if (!Utils.equalObjects(prev, newSize))
			{
				formController.notifyResized();
			}
		}
		else
		{
			super.doPutBrowserProperty(propertyName, propertyValue);
		}
	}

	@Override
	public IWebFormController getController()
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
		if (dataAdapterList != null) dataAdapterList.destroy();
		Collection<WebComponent> componentsList = new ArrayList<WebComponent>(getComponents());
		for (WebComponent c : componentsList)
		{
			c.dispose();
		}
		clearComponents();
		cleanupListeners();
	}

	@Override
	public void setModel(IFoundSetInternal fs)
	{
		DefaultNavigatorWebComponent nav = (DefaultNavigatorWebComponent)getComponent(DefaultNavigator.NAME_PROP_VALUE);
		if (nav != null)
		{
			nav.newFoundset(fs);
		}
		if (fs == null)
		{
			// no data should be shown anymore, (form is made invisible)
			dataAdapterList.setRecord(null, false);
		}
	}

	@Override
	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		propagatePropertyToAllComponents(READONLY, readOnly);
	}

	private void propagatePropertyToAllComponents(String property, boolean value)
	{
		for (WebComponent component : getComponents())
		{
			Object newValue = Boolean.valueOf(value);
			if (READONLY.equals(property))
			{
				Object readonlyproperty = component.getProperty(READONLY);
				if (readonlyproperty instanceof ReadonlySabloValue)
				{
					ReadonlySabloValue oldValue = (ReadonlySabloValue)readonlyproperty;
					//use the rhino conversion to convert from boolean to ReadOnlySabloValue
					PropertyDescription pd = ((WebFormComponent)component).getFormElement().getWebComponentSpec().getProperty(READONLY);
					if (pd != null) newValue = ReadonlyPropertyType.INSTANCE.toSabloComponentValue(Boolean.valueOf(value), oldValue, pd, component);
				}
			}
			component.setProperty(property, newValue);
		}
	}

	private ElementScope getElementsScope()
	{
		FormScope formScope = formController.getFormScope();
		if (formScope != null)
		{
			return (ElementScope)formScope.get("elements", null);
		}
		return null;
	}

	@Override
	public RuntimeWebComponent getRuntimeWebComponent(String elementName)
	{
		return (RuntimeWebComponent)getElementsScope().get(elementName, getElementsScope());
	}

	public void setParentContainer(WebFormComponent parentContainer)
	{
		if (this.parentContainerOrWindowName == parentContainer) return;
		cleanupListeners();
		this.parentContainerOrWindowName = parentContainer;
		if (parentContainer != null)
		{
			boolean formReadOnly = formController.isReadOnly();
			boolean parentReadOnly = parentContainer.dataAdapterList.getForm().isReadOnly();
			// remember that this is a readonly set, dictated by the parent and not the form itself.
			// so that we can set it back when the form is removed from this parent.
			parentReadOnlyListener = new ReadOnlyPropertyChangeListener(formReadOnly == false && parentReadOnly == true);
			parentContainer.addPropertyChangeListener(READONLY, parentReadOnlyListener);
			// set readonly state from form manager, just like in wc/sc, but take into account here the parent because
			// unlike wc/sc we can't walk over all the tabs when the parent gets a setReadOnly(true)
			((BasicFormController)getController()).setReadOnly(formReadOnly || parentReadOnly);
		}
		NGEnabledSabloValue ngSabloValue = (NGEnabledSabloValue)getRawPropertyValue(ENABLED);
		ngSabloValue.flagChanged(this, ENABLED);
	}

	/**
	 * Clean up editable and readonly listners that we added to the parent container before changing it.
	 */
	private void cleanupListeners()
	{
		if (parentContainerOrWindowName instanceof WebFormComponent && parentReadOnlyListener != null)
		{
			// if it was a pure parent force readonly set then set the readonly back to false.
			if (parentReadOnlyListener.parentForcedReadOnly) ((BasicFormController)getController()).setReadOnly(false);
			WebFormComponent parent = (WebFormComponent)parentContainerOrWindowName;
			parent.removePropertyChangeListener(READONLY, parentReadOnlyListener);
		}
	}

	public Object getParentContainer()
	{
		return parentContainerOrWindowName;
	}

	@Override
	public String getParentWindowName()
	{
		if (parentContainerOrWindowName instanceof String)
		{
			return (String)parentContainerOrWindowName;
		}
		else if (parentContainerOrWindowName instanceof WebFormComponent)
		{
			return ((WebFormComponent)parentContainerOrWindowName).findParent(IWebFormUI.class).getParentWindowName();
		}
		return null;
	}

	public void setParentWindowName(String parentWindowName)
	{
		cleanupListeners();
		this.parentContainerOrWindowName = parentWindowName;
		NGEnabledSabloValue ngSabloValue = (NGEnabledSabloValue)getRawPropertyValue(ENABLED);
		ngSabloValue.flagChanged(this, ENABLED);
	}

	@Override
	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	@Override
	public boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		// TODO if there are multiply forms visible and only 1 is reporting that it can't be made invisible
		// what to do with that state? Should it be rollbacked? Should everything be made visible again?
		// See also WebFormComponent
		boolean retValue = true;
		Set<IWebFormController> childFormsThatWereNotified = new HashSet<>();
		for (WebComponent component : getComponents())
		{
			// childFormsThatWereNotified will be populated with forms that are notified below
			retValue = retValue && ((WebFormComponent)component).notifyVisible(visible, invokeLaterRunnables, childFormsThatWereNotified);
		}
		if (retValue) setVisible(visible);

		// childFormsThatWereNotified is given here to avoid double calling for example onHide on the same form if the form's onHide returns false the first time
		dataAdapterList.notifyVisible(visible, invokeLaterRunnables, childFormsThatWereNotified);
		return retValue;
	}

	@Override
	public void setLocation(Point location)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Point getLocation()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSize(Dimension size)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Dimension getSize()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setForeground(Color foreground)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Color getForeground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBackground(Color background)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Color getBackground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFont(Font font)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Font getFont()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBorder(Border border)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Border getBorder()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setOpaque(boolean opaque)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isOpaque()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCursor(Cursor cursor)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setToolTipText(String tooltip)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getToolTipText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean showYesNoQuestionDialog(IApplication application, String dlgMessage, String string)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getContainerName()
	{
		if (parentContainerOrWindowName instanceof String)
		{
			return (String)parentContainerOrWindowName;
		}
		if (parentContainerOrWindowName instanceof WebFormComponent && ((WebFormComponent)parentContainerOrWindowName).getParent() != null)
		{
			return ((WebFormComponent)parentContainerOrWindowName).findParent(IWebFormUI.class).getContainerName();
		}
		return null;
	}

	@Override
	public void printPreview(boolean showDialogs, boolean printCurrentRecordOnly, int zoomFactor, PrinterJob printerJob)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String printXML(boolean printCurrentRecordOnly)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void showSortDialog(IApplication application, String options)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int getFormWidth()
	{
		Dimension size = (Dimension)properties.get("size");
		if (size != null) return size.width;
		return formController.getForm().getWidth();
	}

	@Override
	public int getPartHeight(int partType)
	{
		int totalHeight = 0;
		int bodyHeight = 0;
		for (Part part : Utils.iterate(formController.getForm().getParts()))
		{
			if (partType != Part.BODY)
			{
				if (part.getPartType() == partType)
				{
					return part.getHeight() - totalHeight;
				}
			}
			if (part.getPartType() == Part.BODY)
			{
				bodyHeight = part.getHeight() - totalHeight;
			}
			totalHeight = part.getHeight();

		}
		if (partType == Part.BODY)
		{
			Dimension size = (Dimension)properties.get("size");
			if (size != null) return size.height - totalHeight + bodyHeight;
			return bodyHeight;
		}
		return 0;
	}

	@Override
	public JSDataSet getFormContext()
	{
		IDataSet set = new BufferedDataSet(new String[] { "windowname", "formname", "containername", "tabname", "tabindex", "tabindex1based" }, //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$//$NON-NLS-6$
			new ArrayList<Object[]>());
		set.addRow(new Object[] { null, formController.getName(), null, null, null, null });
		Object currentContainer = parentContainerOrWindowName;
		WebFormUI currentForm = this;
		while (currentContainer instanceof WebFormComponent)
		{
			WebFormComponent currentComponent = (WebFormComponent)currentContainer;
			int index = currentComponent.getFormIndex(currentForm);
			currentForm = currentComponent.findParent(WebFormUI.class);
			set.addRow(0,
				new Object[] { null, currentForm.formController.getName(), currentComponent.getName(), null, new Integer(index), new Integer(index + 1) });
			currentContainer = currentForm.getParentContainer();
		}
		if (currentContainer instanceof String)
		{
			// fill in window name
			for (int i = 0; i < set.getRowCount(); i++)
			{
				set.getRow(i)[0] = currentContainer;
			}
		}
		return new JSDataSet(formController.getApplication(), set);
	}

	@Override
	public void changeFocusIfInvalid(List<Runnable> invokeLaterRunnables)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void prepareForSave(boolean looseFocus)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void start(IApplication app)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void stop()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean editCellAt(int row)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stopUIEditing(boolean looseFocus)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEditing()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void requestFocus()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void ensureIndexIsVisible(int index)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setEditable(boolean findMode)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Rectangle getVisibleRect()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVisibleRect(Rectangle scrollPosition)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setRowBGColorScript(String bgColorScript, List<Object> args)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getRowBGColorScript()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> getRowBGColorArgs()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final void valueChanged()
	{
		getApplication().getChangeListener().valueChanged();
	}

	public void clearCachedFormElements()
	{
		cachedElements.clear();
	}

	public List<FormElement> getFormElements()
	{
		if (cachedElements.size() == 0)
		{
			cachedElements = FormElementHelper.INSTANCE.getFormElements(
				new ArrayList<IPersist>(formController.getForm().getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR)).iterator(),
				getDataConverterContext());
		}
		return cachedElements;
	}

	@Override
	public String toString()
	{
		return "FormUI for " + (formController != null ? formController.toString() : "");
	}

	@Override
	public PropertyDescription getPropertyDescription(String propertyName)
	{
		return FORM_SPEC.getProperty(propertyName);
	}

	@Override
	public Collection<PropertyDescription> getProperties(IPropertyType< ? > type)
	{
		return FORM_SPEC.getProperties(type);
	}

}
