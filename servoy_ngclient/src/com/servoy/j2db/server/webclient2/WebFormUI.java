package com.servoy.j2db.server.webclient2;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collections;
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
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.persistence.AbstractPersistFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.webclient2.component.RuntimeLegacyComponent;
import com.servoy.j2db.server.webclient2.component.RuntimeWebComponent;
import com.servoy.j2db.server.webclient2.component.WebComponentSpec;
import com.servoy.j2db.server.webclient2.property.PropertyDescription;
import com.servoy.j2db.server.webclient2.property.PropertyType;
import com.servoy.j2db.server.webclient2.utils.JSONUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

public class WebFormUI extends WebComponent implements IWebFormUI
{
	protected final Map<String, WebComponent> components = new HashMap<>();
	private final IWebSocketApplication application;
	private final IFormController formController;

	public WebFormUI(IFormController formController)
	{
		super(formController.getName(), formController.getForm(), formController.getApplication().getFlattenedSolution());
		this.formController = formController;
		this.application = (IWebSocketApplication)formController.getApplication();
		try
		{
			DataAdapterList dal = new DataAdapterList(application, formController);
			dataAdapterList = dal;

			Form form = formController.getForm();
			ElementScope elementsScope = initElementScope(formController);
			List<FormElement> formElements = ComponentFactory.getFormElements(form.getAllObjects(), application.getFlattenedSolution());
			int counter = 0;
			for (FormElement fe : formElements)
			{
				WebComponentSpec componentSpec = fe.getWebComponentSpec();

				WebComponent component = ComponentFactory.createComponent(application, dal, fe);
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

				Map<String, PropertyDescription> dataproviderProperties = componentSpec.getProperties(PropertyType.dataprovider);
				for (String dataproviderProperty : dataproviderProperties.keySet())
				{
					Object dataproviderID = fe.getProperty(dataproviderProperty);
					if (dataproviderID instanceof String)
					{
						dal.add(component, (String)dataproviderID, dataproviderProperty);
					}
				}

				Map<String, PropertyDescription> tagstringProperties = componentSpec.getProperties(PropertyType.tagstring);
				for (String dataproviderProperty : tagstringProperties.keySet())
				{
					Object propValue = fe.getProperty(dataproviderProperty);
					//bind tag expressions
					//for each property with tags ('tagstring' type), add it's dependent tags to the DAL 
					if (propValue != null && propValue instanceof String && ((String)propValue).contains("%%"))
					{
						dal.addTaggedProperty(component, dataproviderProperty);
					}
				}
				Map<String, PropertyDescription> formatProperties = componentSpec.getProperties(PropertyType.format);
				for (PropertyDescription pd : formatProperties.values())
				{
					Object propValue = fe.getProperty(pd.getName());
					assert (propValue instanceof String);
					// get dataproviderId
					String dataproviderId = (String)fe.getProperty((String)pd.getConfig());
					ComponentFormat format = ComponentFormat.getComponentFormat((String)propValue, dataproviderId,
						application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), fe.getForm()), application);
					component.putProperty(pd.getName(), format);
				}
				Map<String, PropertyDescription> borderProperties = componentSpec.getProperties(PropertyType.border);
				for (PropertyDescription pd : borderProperties.values())
				{
					Object propValue = fe.getProperty(pd.getName());
					assert (propValue instanceof Border);
					component.putProperty(pd.getName(), propValue);

				}
				Map<String, PropertyDescription> colorProperties = componentSpec.getProperties(PropertyType.color);
				for (PropertyDescription pd : colorProperties.values())
				{
					Object propValue = fe.getProperty(pd.getName());
					assert (propValue instanceof Color);
					component.putProperty(pd.getName(), propValue);

				}

				for (String eventName : componentSpec.getEvents().keySet())
				{
					Object eventValue = fe.getProperty(eventName);
					if (eventValue instanceof String)
					{
						UUID uuid = UUID.fromString((String)eventValue);
						component.add(eventName, ((AbstractPersistFactory)ApplicationServerRegistry.get().getLocalRepository()).getElementIdForUUID(uuid));
					}
					else if (eventValue instanceof Number && ((Number)eventValue).intValue() > 0)
					{
						component.add(eventName, ((Number)eventValue).intValue());

					}
				}
			}
			// special support for the default navigator
			if (form.getNavigatorID() == Form.NAVIGATOR_DEFAULT)
			{
				add(new DefaultNavigatorWebComponent(dal));
			}
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
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
			properties.put(propertyName, JSONUtils.toJavaObject(propertyValue, PropertyType.dimension));
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


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#setComponentEnabled(boolean)
	 */
	@Override
	public void setComponentEnabled(boolean enabled)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IComponent#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		// TODO Auto-generated method stub
		return false;
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
		return (String)application.getActiveWebSocketClientEndpoint().executeDirectServiceCall("$dialogService", "getContainerName",
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
