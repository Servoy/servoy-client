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
package com.servoy.j2db.smart.dataui;


import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.plaf.ColorUIResource;

import org.mozilla.javascript.Undefined;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dnd.DRAGNDROP;
import com.servoy.j2db.dnd.FormDataTransferHandler;
import com.servoy.j2db.dnd.ISupportDragNDropTextTransfer;
import com.servoy.j2db.dnd.JSDNDEvent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.ui.DataRendererOnRenderWrapper;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportRowBGColorScript;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Panel which corresponds to a part at design-time
 * @author jblok
 */
public class DataRenderer extends StyledEnablePanel implements ListCellRenderer, IDataRenderer
{
/*
 * _____________________________________________________________ Declaration of attributes
 */

	private HashMap<IPersist, IDisplay> fieldComponents;
	private boolean usesSliding = false;

	private DataAdapterList dataAdapterList;

	private boolean isRenderer = false;
	private boolean showSelection = false;
	private FormController dragNdropController;
	private final ISupportOnRenderCallback dataRendererOnRenderWrapper;


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	DataRenderer(IApplication app, boolean showSelection)
	{
		super(app);
		this.showSelection = showSelection;
		fieldComponents = new HashMap<IPersist, IDisplay>();
//		setInsets(insets);
		setOpaque(false);
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				getApplication().getFoundSetManager().getEditRecordList().stopEditing(false);
			}

			//request focus for the form panel
			@Override
			public void mouseClicked(MouseEvent e)
			{
				DataRenderer.this.requestFocus();
			}
		});
		dataRendererOnRenderWrapper = new DataRendererOnRenderWrapper(this);
	}

	public int getYOffset()
	{
		return yOffset;
	}

	private int yOffset;


	/**
	 * @param e
	 */
	private void exportDrag(MouseEvent e)
	{
		// controller is set when dragNdrop is enabled.
		if (dragNdropController != null)
		{
			boolean isCTRLDown = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
			TransferHandler handler = getTransferHandler();
			handler.exportAsDrag(this, e, isCTRLDown ? TransferHandler.COPY : TransferHandler.MOVE);
		}
	}

	public void initDragNDrop(FormController formController, int clientDesignYOffset)
	{
		if (!GraphicsEnvironment.isHeadless())
		{
			this.yOffset = clientDesignYOffset;
			Form form = formController.getForm();
			if (form.getOnDragMethodID() > 0 || form.getOnDragEndMethodID() > 0 || form.getOnDragOverMethodID() > 0 || form.getOnDropMethodID() > 0)
			{
				this.dragNdropController = formController;
				// remove drag&drop from children as it is handled by the data renderer

				final DragStartTester dragTester = new DragStartTester();
				addMouseMotionListener(dragTester);
				addMouseListener(dragTester);
				addContainerListener(new ContainerListener()
				{
					public void componentAdded(ContainerEvent e)
					{
						Component child = e.getChild();
						if (child instanceof JComponent)
						{
							child.addMouseMotionListener(dragTester);
							child.addMouseListener(dragTester);
							if (child instanceof ISupportDragNDropTextTransfer) ((ISupportDragNDropTextTransfer)child).clearTransferHandler();
							else if (child instanceof DataComboBox) ((DataComboBox)child).disableEditorTransferHandler();
							else((JComponent)child).setTransferHandler(null);
						}
					}

					public void componentRemoved(ContainerEvent e)
					{
						// ignore
					}

				});
				// Again, needs to negotiate with the draggable object
				setTransferHandler(FormDataTransferHandler.getInstance());

				new DropTarget(this, (DropTargetListener)FormDataTransferHandler.getInstance());
			}
		}
	}

	public void setShowSelection(boolean b)
	{
		showSelection = b;
//		noFocusBorder = BorderFactory.createEmptyBorder(insets.top,insets.left,insets.bottom,insets.right);
//		if (insets.top == 0 && insets.left == 0 && insets.bottom == 0 && insets.right == 0)
//		{
//			focusBorder = noFocusBorder;
//		}
//		else
//		{
//			focusBorder = BorderFactory.createMatteBorder(insets.top,insets.left,insets.bottom,insets.right, Color.black);
//		}
//		setBorder(noFocusBorder);
	}

	public void addDisplayComponent(IPersist obj, IDisplay comp)
	{
		fieldComponents.put(obj, comp);
	}

	@Override
	public void setBackground(Color bgColor)
	{
		if (bgColor != null)
		{
			super.setBackground(bgColor);
		}
		setOpaque(!(bgColor == null || bgColor instanceof ColorUIResource));
	}

	public void destroy()
	{
		if (dataAdapterList != null)
		{
			dataAdapterList.destroy();
			dataAdapterList = null;
		}
		Component[] comps = getComponents();
		for (Component element : comps)
		{
			if (element instanceof Applet)
			{
				((Applet)element).destroy();
			}
		}
		removeAll();
	}

	/*
	 * There seesm to be a bug in Swing that cell/list Editor remove couses focus move to fields inside this dataRenderer So we simple block moves during editor
	 * remove
	 */
	private boolean traversalPolicyEnabled = true;

	@Override
	public void removeNotify()
	{
		try
		{
			traversalPolicyEnabled = false;
			super.removeNotify();
		}
		finally
		{
			traversalPolicyEnabled = true;
		}
	}

	public boolean isTraversalPolicyEnabled()
	{
		return traversalPolicyEnabled;
	}


	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		if (dataAdapterList != null)
		{
			//we just forward the call
			dataAdapterList.notifyVisible(b, invokeLaterRunnables);
		}
		Component[] comps = getComponents();
		for (Component element : comps)
		{
			if (element instanceof Applet)
			{
				if (b)
				{
					((Applet)element).setVisible(true);
					((Applet)element).start();
				}
				else
				{
					((Applet)element).stop();
					((Applet)element).setVisible(false);
				}
			}
		}
	}

	private final HashSet<IDisplay> globalFields = new HashSet<IDisplay>();

	void createDataAdapter(IApplication app, IDataProviderLookup dataProviderLookup, IScriptExecuter el, ControllerUndoManager undoManager) throws Exception
	{
		// IScriptExecutor can be null for a design component
		FormController formController = el == null ? null : el.getFormController();
		dataAdapterList = new DataAdapterList(app, dataProviderLookup, fieldComponents, formController, null, undoManager);

		//make it really fields only
		HashMap<IPersist, IDisplay> f = new HashMap<IPersist, IDisplay>();
		Iterator<Map.Entry<IPersist, IDisplay>> it = fieldComponents.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<IPersist, IDisplay> element = it.next();
			if (element.getValue() instanceof IDisplayData)
			{
				String id = ((IDisplayData)element.getValue()).getDataProviderID();
				if (dataProviderLookup.getDataProvider(id) instanceof ScriptVariable)
				{
					globalFields.add(element.getValue());
				}
				f.put(element.getKey(), element.getValue());
			}
		}
		fieldComponents = f;
	}

	public Map<IPersist, IDisplay> getFieldComponents()//used in printing
	{
		return fieldComponents;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (!isRenderer && dataAdapterList != null) return dataAdapterList.stopUIEditing(looseFocus);
		return true;
	}

	public void setAllNonFieldsEnabled(boolean b)
	{
		Component[] comp = getComponents();
		for (Component c : comp)
		{
			if (!fieldComponents.containsValue(c))
			{
//				if (c instanceof IComponent)
//				{
//					((IComponent)c).setComponentEnabled(b);
//				}
//				else
//				{
//					c.setEnabled(b);
//				}
			}
		}
	}

	public void setAllNonRowFieldsEnabled(boolean b)
	{
		Component[] comp = getComponents();
		for (Component c : comp)
		{
			if (!globalFields.contains(c))
			{
				if (c instanceof IComponent)
				{
					((IComponent)c).setComponentEnabled(b);
				}
				else
				{
					c.setEnabled(b);
				}
			}
		}
	}

	public DataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

/*
 * public Dimension getPreferredSize() { return new Dimension(form.getSize().width, body_end - body_start); //form.getSize(); }
 */

	/*
	 * @see javax.swing.JComponent#paintChildren(java.awt.Graphics)
	 */
	@Override
	protected void paintChildren(Graphics g)
	{
		if (showSelection && selected)
		{
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 3, getHeight());
		}
		try
		{
			super.paintChildren(g);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private String strRowBGColorProvider = null;
	private List<Object> rowBGColorArgs = null;
	private boolean isRowBGColorCalculation;

// do not use this

	public void setRowBGColorProvider(String str, List<Object> args)
	{
		strRowBGColorProvider = str;
		rowBGColorArgs = args;
	}

	private Color defaultColor = null;
	private boolean selected;

	public Component getListCellRendererComponent(JList donotusecanbenullifinrecondview, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		return getListCellRendererComponent((JComponent)donotusecanbenullifinrecondview, value, index, isSelected, cellHasFocus);
	}

	public Component getListCellRendererComponent(JComponent rendererParentCanBeNull, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		this.selected = isSelected;
//		if (isSelected)
//		{
//			setBorder(focusBorder);
//		}
//		else
//		{
//			setBorder(noFocusBorder);
//		}
		boolean bgRowColorSet = false;
		// if in Renderer mode for a the List then a renderer can't stop editing.
		if (value instanceof IRecordInternal)
		{
			IRecordInternal val = (IRecordInternal)value;
			// if in list view, we need to set the render state for each list item here
			if (rendererParentCanBeNull != null)
			{
				DataAdapterList.setDataRendererComponentsRenderState(this, val);
			}
			dataAdapterList.setRecord(val, !isRenderer);
//			setOpaque(true);
			if (index != -1)
			{
				boolean specialStateCase = (val instanceof PrototypeState || val instanceof FindState || val.getRawData() == null);
				if (strRowBGColorProvider == null && !specialStateCase)
				{
					if (rendererParentCanBeNull instanceof ISupportRowBGColorScript)
					{
						strRowBGColorProvider = ((ISupportRowBGColorScript)rendererParentCanBeNull).getRowBGColorScript();
						rowBGColorArgs = ((ISupportRowBGColorScript)rendererParentCanBeNull).getRowBGColorArgs();
					}

					if (strRowBGColorProvider == null) strRowBGColorProvider = "servoy_row_bgcolor"; //$NON-NLS-1$

					isRowBGColorCalculation = val.getRawData().containsCalculation(strRowBGColorProvider);
					if (!isRowBGColorCalculation && strRowBGColorProvider.equals("servoy_row_bgcolor"))
					{
						strRowBGColorProvider = ""; //$NON-NLS-1$
					}

					defaultColor = getBackground();
				}


				if (strRowBGColorProvider != null && !"".equals(strRowBGColorProvider)) //$NON-NLS-1$
				{
					IFoundSetInternal parent = val.getParentFoundSet();
					if (parent != null && !specialStateCase)
					{
						Object bg_color = null;

						if (isRowBGColorCalculation)
						{
							bg_color = parent.getCalculationValue(
								val,
								strRowBGColorProvider,
								Utils.arrayMerge((new Object[] { new Integer(index), new Boolean(isSelected), null, null, Boolean.FALSE }),
									Utils.parseJSExpressions(rowBGColorArgs)), null);
						}
						else
						{
							try
							{
								FormController currentForm = dataAdapterList.getFormController();
								bg_color = currentForm.executeFunction(strRowBGColorProvider, Utils.arrayMerge((new Object[] { new Integer(index), new Boolean(
									isSelected), null, null, currentForm.getName(), val, Boolean.FALSE }), Utils.parseJSExpressions(rowBGColorArgs)), false,
									null, true, null);
							}
							catch (Exception ex)
							{
								Debug.error(ex);
							}
						}
						if (bg_color != null && !(bg_color.toString().trim().length() == 0) && !(bg_color instanceof Undefined))
						{
							bgRowColorSet = true;
							setBackground(PersistHelper.createColor(bg_color.toString()));
						}
						else
						{
							setBackground(defaultColor);
						}
					}
					else
					{
						setBackground(defaultColor);
					}
				}

				if (rendererParentCanBeNull instanceof ISupportRowStyling && !specialStateCase && !bgRowColorSet)
				{
					ISupportRowStyling oddEvenStyling = (ISupportRowStyling)rendererParentCanBeNull;
					IStyleSheet ss = oddEvenStyling.getRowStyleSheet();
					IStyleRule style = isSelected ? oddEvenStyling.getRowSelectedStyle() : null;
					if (style != null && style.getAttributeCount() == 0) style = null;
					if (style == null)
					{
						style = (index % 2 == 0) ? oddEvenStyling.getRowOddStyle() : oddEvenStyling.getRowEvenStyle(); // because index = 0 means record = 1
					}
					if (ss != null && style != null)
					{
						Color bgColor = ss.getBackground(style);
						if (bgColor != null)
						{
							showSelection = false;
							bgRowColorSet = true;
							setBackground(bgColor);
						}
					}
				}
			}
		}

		if (rendererParentCanBeNull != null)
		{
			if (rendererParentCanBeNull.isEnabled() != isEnabled())
			{
//Debug.trace(donotusecanbenullifinrecondview.getName()+" "+donotusecanbenullifinrecondview.isEnabled());
				setEnabled(rendererParentCanBeNull.isEnabled()); //needed for portals
			}
			if (bgRowColorSet && !isOpaque())
			{
				setOpaque(true);
			}
			dataRendererOnRenderWrapper.getRenderEventExecutor().fireOnRender(hasFocus());
		}

//		setFont(list.getFont());

		//System.out.println(this);

		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.JComponent#isOpaque()
	 */
	@Override
	public boolean isOpaque()
	{
		if (getBackground() != null && getBackground().getAlpha() < 255)
		{
			return false;
		}
		return super.isOpaque();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.JComponent#setOpaque(boolean)
	 */
	@Override
	public void setOpaque(boolean isOpaque)
	{
		if (getBackground() != null && getBackground().getAlpha() < 255)
		{
			super.setOpaque(false);
		}
		else super.setOpaque(isOpaque);
	}

	/*
	 * @see JComponent#getToolTipText(MouseEvent)
	 */
	@Override
	public String getToolTipText(MouseEvent event)
	{
		Component comp = SwingUtilities.getDeepestComponentAt(this, event.getX(), event.getY());
		if (comp == null || !(comp instanceof JComponent) || comp == this)
		{
			return super.getToolTipText(event);
		}
		Point p = SwingUtilities.convertPoint(this, event.getPoint(), comp);
		MouseEvent newEvent = new MouseEvent(comp, event.getID(), event.getWhen(), event.getModifiers(), p.x, p.y, event.getClickCount(),
			event.isPopupTrigger());

		String tip = ((JComponent)comp).getToolTipText(newEvent);
		if (tip != null) return tip;
		return getToolTipText();
	}

	/**
	 * @return
	 */
	public boolean isRenderer()
	{
		return isRenderer;
	}

	/**
	 * @param b
	 */
	public void setRenderer(boolean b)
	{
		isRenderer = b;
	}

	/**
	 * @return
	 */
//	public Border getFocusBorder()
//	{
//		return focusBorder;
//	}
	/**
	 * @return
	 */
//	public Border getNoFocusBorder()
//	{
//		return noFocusBorder;
//	}
	public boolean isUsingSliding()
	{
		return usesSliding;
	}

	public void setUsingSliding(boolean usesSliding)
	{
		this.usesSliding = usesSliding;
	}

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}

	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	public void refreshRecord(IRecordInternal record)
	{
		DataAdapterList.setDataRendererComponentsRenderState(this, record);
		if (dataAdapterList != null)
		{
			dataAdapterList.setRecord(record, true);
		}
		fireDataRendererOnRender();
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}

	public Iterator getComponentIterator()
	{
		return Arrays.asList(getComponents()).iterator();
	}

	public void add(IComponent c, String name)
	{
		//ignore
	}

	public void remove(IComponent c)
	{
		//ignore
	}

	private Map componentsUsingSliding;

	public void setComponentsUsingSliding(Map componentsUsingSliding)
	{
		this.componentsUsingSliding = componentsUsingSliding;
	}

	public Map getComponentsUsingSliding()
	{
		return componentsUsingSliding;
	}

	private int spaceToRightMargin;

	/**
	 * Sets the width of the space to keep empty between the right-most component and the right edge of the renderer. This info is only used for growing fields
	 * that can be limited to fit on the page and setting it will not affect the existing spring layout.
	 *
	 * @param spaceToRightMargin the empty space that should be kept by any child to the right margin of the renderer; it is calculated when springs are added
	 *            for printing...
	 */
	public void setSpaceToRightMargin(int spaceToRightMargin)
	{
		this.spaceToRightMargin = spaceToRightMargin;
	}

	/**
	 * Returns the empty space that should be kept by any child to the right margin of the renderer; it is set when springs are added for printing...
	 *
	 * @return the empty space that should be kept by any child to the right margin of the renderer; it is set when springs are added for printing...
	 */
	public int getSpaceToRightMargin()
	{
		return spaceToRightMargin;
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
		Component dragedComp = getComponentAt(xy);
		if (!this.equals(dragedComp) && dragedComp instanceof IComponent && dragedComp.isEnabled() && !(dragedComp instanceof SpecialTabPanel) &&
			!(dragedComp instanceof SpecialSplitPane)) return (IComponent)dragedComp;

		return this;
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

	private class DragStartTester extends MouseAdapter implements MouseMotionListener
	{
		boolean startDrag = false;

		@Override
		public void mouseMoved(MouseEvent e)
		{
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			if (startDrag) exportDrag(SwingUtilities.convertMouseEvent((Component)e.getSource(), e, DataRenderer.this));
			startDrag = false;
		}

		/**
		 * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
		 */
		@Override
		public void mousePressed(MouseEvent e)
		{
			startDrag = true;
		}
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

	private void fireDataRendererOnRender()
	{
		dataRendererOnRenderWrapper.getRenderEventExecutor().fireOnRender(hasFocus());

		@SuppressWarnings("rawtypes")
		Iterator compIte = getComponentIterator();
		Object comp;
		while (compIte.hasNext())
		{
			comp = compIte.next();
			if (comp instanceof IScriptableProvider && !(comp instanceof IDisplayData) && !(comp instanceof IDisplayRelatedData))
			{
				IScriptable scriptable = ((IScriptableProvider)comp).getScriptObject();
				if (scriptable instanceof ISupportOnRenderCallback)
				{
					RenderEventExecutor rendererEventExecutor = ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor();
					boolean hasFocus = (comp instanceof Component) ? ((Component)comp).hasFocus() : false;
					rendererEventExecutor.fireOnRender(hasFocus);
				}
			}
		}
	}

	/*
	 * @see com.servoy.j2db.dnd.IFormDataDragNDrop#getDragNDropController()
	 */
	public FormController getDragNDropController()
	{
		return dragNdropController;
	}
}
