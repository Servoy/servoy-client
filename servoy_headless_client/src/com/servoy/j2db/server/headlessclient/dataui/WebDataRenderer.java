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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.mozilla.javascript.Undefined;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IView;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dnd.DRAGNDROP;
import com.servoy.j2db.dnd.JSDNDEvent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.headlessclient.dnd.DraggableBehavior;
import com.servoy.j2db.ui.DataRendererOnRenderWrapper;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * The web implementation of the {@link IDataRenderer}
 * 
 * @author jcompagner
 */
public class WebDataRenderer extends WebMarkupContainer implements IDataRenderer, IProviderStylePropertyChanges
{
	private static final long serialVersionUID = 1L;

	private Dimension size = new Dimension(0, 0);
	private Point location = new Point(0, 0);
//	private boolean visible;
	private String tooltiptext;
//	private Cursor cursor;
	private boolean opaque;
	private Border border;
	private Font font;
	private Color foreground;
	private Color background;
//	private final IApplication application;
	private DataAdapterList dataAdapterList;
//	private final Set globalFields = new HashSet();
	private HashMap<IPersist, IDisplay> fieldComponents = new HashMap<IPersist, IDisplay>();
	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING);

	private IView parentView;
	private final String formPartName;

	private FormController dragNdropController;
	private final ISupportOnRenderCallback dataRendererOnRenderWrapper;

	/**
	 * @param id
	 */
	public WebDataRenderer(String id, String formPartName, IApplication app)
	{
		super(id);
//		application = app;
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
		setOutputMarkupPlaceholderTag(true);
		this.formPartName = formPartName;
		dataRendererOnRenderWrapper = new DataRendererOnRenderWrapper(this);
		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				WebForm container = findParent(WebForm.class);
				if (container != null && container.getBorder() instanceof TitledBorder)
				{
					int offset = ComponentFactoryHelper.getTitledBorderHeight(container.getBorder());
					return "top: " + offset + "px;";
				}
				return ""; //$NON-NLS-1$
			}
		})
		{
			@Override
			public boolean isEnabled(Component component)
			{
				if (WebDataRenderer.this.getParent().get(0) != WebDataRenderer.this) return false;
				WebForm container = component.findParent(WebForm.class);
				if (container != null && container.getBorder() instanceof TitledBorder)
				{
					return super.isEnabled(component);
				}
				return false;
			}
		});
	}

	public void setParentView(IView parentView)
	{
		this.parentView = parentView;
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#getDataAdapterList()
	 */
	public DataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

	public void addDisplayComponent(IPersist obj, IDisplay comp)
	{
		fieldComponents.put(obj, comp);
	}

	void createDataAdapter(IApplication app, IDataProviderLookup dataProviderLookup, IScriptExecuter el, ControllerUndoManager undoManager) throws Exception
	{
		dataAdapterList = new DataAdapterList(app, dataProviderLookup, fieldComponents, el.getFormController(), null, undoManager);

		//make it really fields only
		HashMap<IPersist, IDisplay> f = new HashMap<IPersist, IDisplay>();
		Iterator<Map.Entry<IPersist, IDisplay>> it = fieldComponents.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<IPersist, IDisplay> element = it.next();
			if (element.getValue() instanceof IDisplayData)
			{
//				String id = ((IDisplayData)element.getValue()).getDataProviderID();
//				if (dataProviderLookup.getDataProvider(id) instanceof ScriptVariable)
//				{
//					globalFields.add(element.getValue());
//				}
				f.put(element.getKey(), element.getValue());
			}
		}
		fieldComponents = f;
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#setAllNonFieldsEnabled(boolean)
	 */
	public void setAllNonFieldsEnabled(boolean b)
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#setAllNonRowFieldsEnabled(boolean)
	 */
	public void setAllNonRowFieldsEnabled(boolean b)
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#stopUIEditing()
	 */
	public boolean stopUIEditing(boolean looseFocus)
	{
		return true;
	}

	/**
	 * @see com.servoy.j2db.ui.IDataRenderer#setName(java.lang.String)
	 */
	public void setName(String name)
	{
		// ignore id can't be set in web
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getName()
	 */
	public String getName()
	{
		return getId();
	}

	public void destroy()
	{
		if (dataAdapterList != null)
		{
			dataAdapterList.destroy();
			dataAdapterList = null;
		}
		if (getRequestCycle() != null)
		{
			removeAll();
			if (getParent() != null)
			{
				remove();
			}
		}
	}

	public void setComponentVisible(boolean visible)
	{
//		this.visible = visible;
	}

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public void setForeground(Color foreground)
	{
		this.foreground = foreground;
	}

	public void setBackground(Color background)
	{
		this.background = background;
	}

	public void setFont(Font font)
	{
		this.font = font;
	}

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	public void setCursor(Cursor cursor)
	{
//		this.cursor = cursor;
	}

	public void setToolTipText(String tooltiptext)
	{
		if (Utils.stringIsEmpty(tooltiptext))
		{
			this.tooltiptext = null;
		}
		else
		{
			this.tooltiptext = tooltiptext;
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		return tooltiptext;
	}


	public Point getLocation()
	{
		return location;
	}

	public Dimension getSize()
	{
		return size;
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		if (dataAdapterList != null)
		{
			//we just forward the call
			dataAdapterList.notifyVisible(b, invokeLaterRunnables);
		}
	}

	public void refreshRecord(IRecordInternal record)
	{
		IModel< ? > model = getDefaultModel();
		if (model != null)
		{
			model.detach();
		}
		if (dataAdapterList != null)
		{
			dataAdapterList.setRecord(record, true);
		}

		DataAdapterList.setDataRendererComponentsRenderState(this, record);
		if (getOnRenderComponent().getRenderEventExecutor().hasRenderCallback())
		{
			jsChangeRecorder.setChanged();
		}
	}

	public Color getBackground()
	{
		return background;
	}

	public Border getBorder()
	{
		return border;
	}

	public Font getFont()
	{
		return font;
	}

	public Color getForeground()
	{
		return foreground;
	}

	public boolean isOpaque()
	{
		return opaque;
	}

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}

	@SuppressWarnings("nls")
	public Iterator< ? extends IComponent> getComponentIterator()
	{
		final Iterator< ? extends Component> wicketIterator = iterator();
		Iterator< ? extends IComponent> iterator = new Iterator<IComponent>()
		{
			private IComponent next;

			public boolean hasNext()
			{
				if (next != null) return true;
				while (wicketIterator.hasNext())
				{
					Object comp = wicketIterator.next();
					Object delegate = comp;
					while (delegate instanceof IDelegate< ? > && !(delegate instanceof IComponent))
					{
						delegate = ((IDelegate< ? >)delegate).getDelegate();
					}
					if (delegate instanceof IComponent)
					{
						next = (IComponent)delegate;
						return true;
					}
					Debug.error("Component found which is not an IComponent " + comp + " for a datarenderer " + this, new RuntimeException(
						comp.getClass().toString()));
				}
				return false;
			}

			public IComponent next()
			{
				if (next == null)
				{
					hasNext();
				}
				if (next == null)
				{
					throw new NoSuchElementException("Component iterator of " + this + " has no more elements");
				}
				IComponent returnValue = next;
				next = null;
				return returnValue;
			}

			public void remove()
			{
				wicketIterator.remove();
			}
		};
		return iterator;
	}

	public void add(IComponent c, String name)
	{
		//ignore
	}

	public void remove(IComponent c)
	{
		//ignore
	}


	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();

		if (getBgcolor() == null)
		{
			String parentViewBGColor = getParentViewBgColor();
			if (parentViewBGColor != null)
			{
				setBgcolor(parentViewBGColor);
			}
		}
		dataRendererOnRenderWrapper.getRenderEventExecutor().fireOnRender(false);
	}

	@Override
	protected void onRender(final MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		String parentViewBackground = getParentViewBgColor();

		if (parentViewBackground != null)
		{
			if (!parentViewBackground.equals(getBgcolor()))
			{
				setBgcolor(parentViewBackground);
			}
		}
		return jsChangeRecorder;
	}

	/*
	 * bgcolor as string---------------------------------------------------
	 */
	private String getBgcolor()
	{
		return PersistHelper.createColorString(background);
	}

	private void setBgcolor(String bgcolor)
	{
		background = PersistHelper.createColor(bgcolor);
		jsChangeRecorder.setBgcolor(bgcolor);
	}

	private String getParentViewBgColor()
	{
		if (parentView != null)
		{
			String rowBGColorCalculation = parentView.getRowBGColorScript();
			IRecordInternal rec = (IRecordInternal)getDefaultModelObject();
			if (rec != null && rec.getRawData() != null)
			{
				IFoundSetInternal parentFoundSet = rec.getParentFoundSet();
				int recIndex = parentFoundSet.getSelectedIndex();
				boolean isSelected = recIndex == parentFoundSet.getRecordIndex(rec);
				if (rowBGColorCalculation != null)
				{
					Object bg_color = null;
					if (rec.getRawData().containsCalculation(rowBGColorCalculation))
					{
						// data renderer is always on the selected index.
						bg_color = parentFoundSet.getCalculationValue(
							rec,
							rowBGColorCalculation,
							Utils.arrayMerge(new Object[] { new Integer(recIndex), new Boolean(isSelected), null, null, Boolean.FALSE },
								Utils.parseJSExpressions(parentView.getRowBGColorArgs())), null);
					}
					else
					{
						try
						{
							FormController currentForm = dataAdapterList.getFormController();
							bg_color = currentForm.executeFunction(
								rowBGColorCalculation,
								Utils.arrayMerge(
									new Object[] { new Integer(parentFoundSet.getSelectedIndex()), new Boolean(isSelected), null, null, currentForm.getName(), rec, Boolean.FALSE },
									Utils.parseJSExpressions(parentView.getRowBGColorArgs())), true, null, true, null);
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
					if (bg_color != null && !(bg_color.toString().trim().length() == 0) && !(bg_color instanceof Undefined))
					{
						return bg_color.toString();
					}
				}

				if (parentView instanceof ISupportRowStyling)
				{
					ISupportRowStyling parentViewWithRowStyling = (ISupportRowStyling)parentView;
					IStyleSheet ss = parentViewWithRowStyling.getRowStyleSheet();
					IStyleRule style = isSelected ? parentViewWithRowStyling.getRowSelectedStyle() : null;
					if (style != null && style.getAttributeCount() == 0) style = null;
					if (style == null)
					{
						style = (recIndex % 2 == 0) ? parentViewWithRowStyling.getRowEvenStyle() : parentViewWithRowStyling.getRowOddStyle();
					}

					if (ss != null && style != null)
					{
						return PersistHelper.createColorString(ss.getBackground(style));
					}
				}
			}
		}

		return null;
	}

	public String getFormPartName()
	{
		return this.formPartName;
	}

	public int onDrag(JSDNDEvent event)
	{
		Form form = dragNdropController.getForm();
		int onDragID = form.getOnDragMethodID();

		if (onDragID > 0)
		{
			Object dragReturn = dragNdropController.executeFunction(Integer.toString(onDragID), new Object[] { event }, false, null, false, "onDragMethodID"); //$NON-NLS-1$
			if (dragReturn instanceof Number) return ((Number)dragReturn).intValue();
		}

		return DRAGNDROP.NONE;
	}

	public boolean onDragOver(JSDNDEvent event)
	{
		Form form = dragNdropController.getForm();
		int onDragOverID = form.getOnDragOverMethodID();

		if (onDragOverID > 0)
		{
			Object dragOverReturn = dragNdropController.executeFunction(Integer.toString(onDragOverID), new Object[] { event }, false, null, false,
				"onDragOverMethodID"); //$NON-NLS-1$
			if (dragOverReturn instanceof Boolean) return ((Boolean)dragOverReturn).booleanValue();
		}

		return form.getOnDropMethodID() > 0;
	}

	public boolean onDrop(JSDNDEvent event)
	{
		Form form = dragNdropController.getForm();
		int onDropID = form.getOnDropMethodID();

		if (onDropID > 0)
		{
			Object dropHappened = dragNdropController.executeFunction(Integer.toString(onDropID), new Object[] { event }, false, null, false, "onDropMethodID"); //$NON-NLS-1$
			if (dropHappened instanceof Boolean) return ((Boolean)dropHappened).booleanValue();
		}
		return false;
	}

	public void onDragEnd(JSDNDEvent event)
	{
		Form form = dragNdropController.getForm();
		int onDragEndID = form.getOnDragEndMethodID();

		if (onDragEndID > 0)
		{
			dragNdropController.executeFunction(Integer.toString(onDragEndID), new Object[] { event }, false, null, false, "onDragEndMethodID"); //$NON-NLS-1$
		}
	}

	public IComponent getDragSource(Point xy)
	{
		// don't need this, ignore
		return null;
	}

	public String getDragFormName()
	{
		return getDataAdapterList().getFormController().getName();
	}

	public boolean isGridView()
	{
		return false;
	}

	public IRecordInternal getDragRecord(Point xy)
	{
		return getDataAdapterList().getState();
	}

	public int getYOffset()
	{
		return yOffset;
	}

	private int yOffset;

	public void initDragNDrop(FormController formController, int clientDesignYOffset)
	{
		this.yOffset = clientDesignYOffset;
		Form form = formController.getForm();
		if (form.getOnDragMethodID() > 0 || form.getOnDragEndMethodID() > 0 || form.getOnDragOverMethodID() > 0 || form.getOnDropMethodID() > 0)
		{
			this.dragNdropController = formController;
			if (dragNdropController != null) addDragNDropBehavior();

		}
	}

	public FormController getDragNDropController()
	{
		return dragNdropController;
	}

	private void addDragNDropBehavior()
	{
		DraggableBehavior dragBehavior = new DraggableBehavior()
		{
			private IComponent hoverComponent;
			private boolean isHoverAcceptDrop;

			@Override
			protected void onDragEnd(String id, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
			{
				if (getCurrentDragOperation() != DRAGNDROP.NONE)
				{
					JSDNDEvent event = WebDataRenderer.this.createScriptEvent(EventType.onDragEnd, getDragComponent(), null, m);
					event.setData(getDragData());
					event.setDataMimeType(getDragDataMimeType());
					event.setDragResult(getDropResult() ? getCurrentDragOperation() : DRAGNDROP.NONE);
					WebDataRenderer.this.onDragEnd(event);
				}

				super.onDragEnd(id, x, y, m, ajaxRequestTarget);
			}

			@Override
			protected boolean onDragStart(final String id, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
			{
				IComponent comp = getBindedComponentChild(id);
				JSDNDEvent event = WebDataRenderer.this.createScriptEvent(EventType.onDrag, comp, new Point(x, y), m);
				setDropResult(false);
				int dragOp = WebDataRenderer.this.onDrag(event);
				if (dragOp == DRAGNDROP.NONE) return false;
				setCurrentDragOperation(dragOp);
				setDragComponent(comp);
				setDragData(event.getData(), event.getDataMimeType());
				hoverComponent = null;
				isHoverAcceptDrop = false;
				return true;
			}

			@Override
			protected void onDrop(String id, final String targetid, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
			{
				if (getCurrentDragOperation() != DRAGNDROP.NONE)
				{
					IComponent comp = getBindedComponentChild(targetid);
					if (hoverComponent == comp && !isHoverAcceptDrop) return;
					WebDataRenderer renderer = WebDataRenderer.this;
					JSDNDEvent event = renderer.createScriptEvent(EventType.onDrop, comp, new Point(x, y), m);
					event.setData(getDragData());
					event.setDataMimeType(getDragDataMimeType());
					setDropResult(renderer.onDrop(event));
				}
			}

//			private ISupportDragNDrop testTarget(ISupportDragNDrop ddComp, JSEvent event)
//			{
//				if (event.js_getSource() instanceof SpecialTabPanel)
//				{
//					SpecialTabPanel tabPanel = (SpecialTabPanel)event.js_getSource();
//					Component selectedComponent = tabPanel.getEnclosingComponent().getSelectedComponent();
//					if (selectedComponent instanceof FormLookupPanel)
//					{
//						FormController formControler = ((FormLookupPanel)selectedComponent).getFormPanel();
//						event.setSource(null);
//						event.setFormName(formControler.getName());
//						event.setElementName(null);
//						return (ISupportDragNDrop)formControler.getViewComponent();
//					}
//				}
//				return ddComp;
//			}

			@Override
			protected void onDropHover(String id, final String targetid, int m, AjaxRequestTarget ajaxRequestTarget)
			{
				if (getCurrentDragOperation() != DRAGNDROP.NONE)
				{
					IComponent comp = getBindedComponentChild(targetid);
					JSDNDEvent event = WebDataRenderer.this.createScriptEvent(EventType.onDragOver, comp, null, m);
					event.setData(getDragData());
					event.setDataMimeType(getDragDataMimeType());
					isHoverAcceptDrop = WebDataRenderer.this.onDragOver(event);
					hoverComponent = comp;
				}
			}

			@Override
			public IComponent getBindedComponentChild(final String childId)
			{
				IComponent comp = super.getBindedComponentChild(childId);
				if (comp == null) comp = WebDataRenderer.this;
				return comp;
			}

		};
		dragBehavior.setUseProxy(true);
		add(dragBehavior);
	}

	private JSDNDEvent createScriptEvent(EventType type, IComponent dragSource, Point xy, int modifiers)
	{
		JSDNDEvent jsEvent = new JSDNDEvent();
		jsEvent.setType(type);
		jsEvent.setFormName(getDragFormName());
		IRecordInternal dragRecord = getDragRecord(xy);
		if (dragRecord instanceof Record) jsEvent.setRecord((Record)dragRecord);
		if (dragSource instanceof IDataRenderer)
		{
			IDataRenderer dr = (IDataRenderer)dragSource;
			FormController fct = dr.getDataAdapterList().getFormController();
			jsEvent.setSource(fct.getFormScope());
		}
		else
		{
			jsEvent.setSource(dragSource);
			if (dragSource != null)
			{
				String dragSourceName = dragSource.getName();
				if (dragSourceName == null) dragSourceName = dragSource.getId();
				jsEvent.setElementName(dragSourceName);
			}
		}
		if (xy != null) jsEvent.setLocation(xy);
		jsEvent.setModifiers(modifiers);

		return jsEvent;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderWrapper#getOnRenderComponent()
	 */
	public ISupportOnRenderCallback getOnRenderComponent()
	{
		return dataRendererOnRenderWrapper;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderWrapper#getOnRenderElementType()
	 */
	public String getOnRenderElementType()
	{
		return IRuntimeComponent.FORM;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderWrapper#getOnRenderToString()
	 */
	public String getOnRenderToString()
	{
		return dataAdapterList != null ? dataAdapterList.getFormController().getForm().toString() : super.toString();
	}
}
