package com.servoy.j2db.server.ngclient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.border.Border;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.specification.property.types.VisiblePropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.component.RuntimeLegacyComponent;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.types.ValueListTypeSabloValue;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class WebFormUI extends Container implements IWebFormUI
{
	private static final class FormSpecification extends WebComponentSpecification
	{
		private FormSpecification()
		{
			super("form_spec", "", "", null, null, "", null);
			putProperty("size", new PropertyDescription("size", DimensionPropertyType.INSTANCE));
			putProperty("visible", new PropertyDescription("visible", VisiblePropertyType.INSTANCE));
		}
	}

	private static final WebComponentSpecification FORM_SPEC = new FormSpecification();

	private final Map<String, Integer> events = new HashMap<>(); //event name mapping to persist id
	private final IWebFormController formController;

	private boolean enabled = true;
	private boolean readOnly = false;
	private Object parentContainerOrWindowName;

	protected IDataAdapterList dataAdapterList;

	public WebFormUI(IWebFormController formController)
	{
		super(formController.getName(), FORM_SPEC);
		this.formController = formController;
		setVisible(false);
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
	public final void init()
	{
		components.clear();
		IDataAdapterList previousDataAdapterList = dataAdapterList;
		dataAdapterList = new DataAdapterList(formController);

		ElementScope elementsScope = initElementScope(formController);
		List<FormElement> formElements = getFormElements();
		int counter = 0;
		for (FormElement fe : formElements)
		{
			// TODO do something similar for child elements (so properties of type 'components' which contain componentSpecs in them)

			WebComponentSpecification componentSpec = fe.getWebComponentSpec(false);
			if (componentSpec == null)
			{
				getApplication().reportError("Didn't find a spec file for component " + fe + " when creating form: " + formController.getName(), null);
				continue;
			}

			WebFormComponent component = ComponentFactory.createComponent(getApplication(), dataAdapterList, fe, this);

			if (component != null)
			{
				counter = contributeComponentToElementsScope(elementsScope, counter, fe, componentSpec, component);
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
			add(new DefaultNavigatorWebComponent(dataAdapterList));
		}

		if (previousDataAdapterList != null)
		{
			IRecordInternal record = ((DataAdapterList)previousDataAdapterList).getRecord();
			if (record != null)
			{
				dataAdapterList.setRecord(record, false);
				previousDataAdapterList.setRecord(null, false);

				nav = (DefaultNavigatorWebComponent)components.get(DefaultNavigator.NAME_PROP_VALUE);
				if (nav != null) nav.newFoundset(record.getParentFoundSet());
			}
		}
	}

	public void contributeComponentToElementsScope(FormElement fe, WebComponentSpecification componentSpec, WebFormComponent component)
	{
		ElementScope elementsScope = getElementsScope();
		if (elementsScope != null)
		{
			Object tmp = elementsScope.get("length", elementsScope);
			int counter = tmp instanceof Integer ? ((Integer)tmp).intValue() : 0;
			contributeComponentToElementsScope(elementsScope, counter, fe, componentSpec, component);
		}
		else
		{
			Debug.error(new RuntimeException("Trying to contribute to a non-existent elements scope for form: " + getName()));
		}
	}

	private int contributeComponentToElementsScope(ElementScope elementsScope, int counterStart, FormElement fe, WebComponentSpecification componentSpec,
		WebFormComponent component)
	{
		int counter = counterStart;
		if (!fe.getName().startsWith("svy_") && !FormElement.ERROR_BEAN.equals(componentSpec.getName()))
		{
			RuntimeWebComponent runtimeComponent = new RuntimeWebComponent(component, componentSpec);
			elementsScope.put(fe.getName(), formController.getFormScope(), runtimeComponent);
			elementsScope.put(counter++, formController.getFormScope(), runtimeComponent);
			if (fe.isLegacy() ||
				((fe.getForm().getView() == IForm.LIST_VIEW || fe.getForm().getView() == FormController.LOCKED_LIST_VIEW ||
					fe.getForm().getView() == FormController.TABLE_VIEW || fe.getForm().getView() == FormController.LOCKED_TABLE_VIEW) && fe.getTypeName().startsWith(
					"servoydefault-")))
			{
				// add legacy behavior
				runtimeComponent.setPrototype(new RuntimeLegacyComponent(component));
			}
		}
		return counter;
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
		return (WebFormComponent)super.getComponent(compname);
	}

	@Override
	public boolean writeAllComponentsProperties(JSONWriter w, IToJSONConverter converter) throws JSONException
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
	public boolean writeAllComponentsChanges(JSONWriter w, String keyInParent, IToJSONConverter converter, DataConversion clientDataConversions)
		throws JSONException
	{
		try
		{
			getController().setRendering(true);
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
		Collection<WebComponent> componentsList = new ArrayList<WebComponent>(components.values());
		for (WebComponent c : componentsList)
		{
			c.dispose();
		}
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
		// TODO see https://support.servoy.com/browse/SVY-8024 readonly should become a type, so no need to have here a "readonly" property based on name
		propagatePropertyToAllComponents("readOnly", readOnly);
	}

	private void propagatePropertyToAllComponents(String property, boolean value)
	{
		if ("readOnly".equals(property))
		{
			// special case, we have to set editable property for legacy components
			List<FormElement> formElements = getFormElements();
			for (FormElement fe : formElements)
			{
				WebComponent component = components.get(fe.getName());
				if (component != null)
				{
					if (fe.isLegacy())
					{
						component.setProperty("editable", Boolean.valueOf(!value));
					}
					else
					{
						component.setProperty(property, Boolean.valueOf(value));
					}
				}
			}
		}
		else
		{
			for (WebComponent component : components.values())
			{
				component.setProperty(property, Boolean.valueOf(value));
			}
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

	public void setParentContainer(WebFormComponent parentContainer)
	{
		this.parentContainerOrWindowName = parentContainer;
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
		this.parentContainerOrWindowName = parentWindowName;
	}

	@Override
	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
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
		return 0;
	}

	@Override
	public int getPartHeight(int partType)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public JSDataSet getFormContext()
	{
		IDataSet set = new BufferedDataSet(
			new String[] { "windowname", "formname", "containername", "tabname", "tabindex", "tabindex1based" }, new ArrayList<Object[]>()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		set.addRow(new Object[] { null, formController.getName(), null, null, null, null });
		Object currentContainer = parentContainerOrWindowName;
		WebFormUI currentForm = this;
		while (currentContainer instanceof WebFormComponent)
		{
			WebFormComponent currentComponent = (WebFormComponent)currentContainer;
			int index = currentComponent.getFormIndex(currentForm);
			currentForm = currentComponent.findParent(WebFormUI.class);
			set.addRow(0, new Object[] { null, currentForm.formController.getName(), currentComponent.getName(), null, new Integer(index), new Integer(
				index + 1) });
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

	@Override
	public void refreshValueList(IValueList valuelist)
	{
		for (WebComponent component : components.values())
		{
			WebFormComponent comp = (WebFormComponent)component;
			Collection<PropertyDescription> valuelistProps = comp.getFormElement().getWebComponentSpec().getProperties(TypesRegistry.getType("valuelist"));
			for (PropertyDescription vlProp : valuelistProps)
			{
				ValueListTypeSabloValue propertyValue = (ValueListTypeSabloValue)comp.getProperty(vlProp.getName());
				if (propertyValue != null)
				{
					IValueList vl = propertyValue.getValueList();
					if (vl.getValueList() == valuelist.getValueList())
					{
						propertyValue.setValueList(valuelist);
					}
				}
			}
		}
	}

	protected List<FormElement> getFormElements()
	{
		return FormElementHelper.INSTANCE.getFormElements(
			new ArrayList<IPersist>(formController.getForm().getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR)).iterator(),
			getDataConverterContext());
	}

	@Override
	public String toString()
	{
		return "FormUI for " + (formController != null ? formController.toString() : "");
	}

}
