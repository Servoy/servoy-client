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
package com.servoy.j2db.dnd;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.TransferHandler;

import com.servoy.j2db.FormController;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.util.Debug;

/**
 * Class used for handling drag and drop operations in smart client
 * 
 * @author gboros
 */
public class FormDataTransferHandler extends TransferHandler implements DropTargetListener
{
	private static FormDataTransferHandler instance;

	DropTargetDragEvent dropTargetDragEvent;
	DropTargetDropEvent dropTargetDropEvent;
	InputEvent inputEvent;

	private boolean canImport;

	private Object eventData;
	private JSDNDEvent onDragEvent;


	public static TransferHandler getInstance()
	{
		if (instance == null) instance = new FormDataTransferHandler();

		return instance;
	}

	@Override
	public int getSourceActions(JComponent c)
	{
		// when dnd from portal, getSourceActions is called also before, exportAsDrag,
		// so, let ignore it, and wait for the inputEvent to be set
		if (inputEvent == null) return TransferHandler.COPY_OR_MOVE;
		ISupportDragNDrop ddComp = (ISupportDragNDrop)c;

		onDragEvent = createScriptEvent(EventType.onDrag, (ISupportDragNDrop)c, inputEvent);
		int dragReturn = ddComp.onDrag(onDragEvent);
		eventData = onDragEvent.getData();

		return dragReturn == DRAGNDROP.NONE ? TransferHandler.NONE : dragReturn == DRAGNDROP.COPY ? TransferHandler.COPY : dragReturn == DRAGNDROP.MOVE
			? TransferHandler.MOVE : dragReturn == DRAGNDROP.COPY + DRAGNDROP.MOVE ? TransferHandler.COPY_OR_MOVE : TransferHandler.NONE;
	}

	/**
	 * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
	 */
	@Override
	protected Transferable createTransferable(JComponent c)
	{
		Transferable formDataTransferable = new FormDataTransferable(eventData);
		return formDataTransferable;
	}

	@Override
	public void exportAsDrag(JComponent comp, InputEvent e, int action)
	{
		inputEvent = e;
		eventData = null;
		super.exportAsDrag(comp, e, action);
		inputEvent = null;
	}

	/**
	 * @see javax.swing.TransferHandler#exportDone(javax.swing.JComponent, java.awt.datatransfer.Transferable, int)
	 */
	@Override
	protected void exportDone(JComponent source, Transferable data, int action)
	{
		super.exportDone(source, data, action);
		eventData = null;
		onDragEvent.setType(EventType.onDragEnd);
		onDragEvent.setTimestamp(new Date());
		int dragResult = action == COPY ? DRAGNDROP.COPY : action == MOVE ? DRAGNDROP.MOVE : DRAGNDROP.NONE;
		onDragEvent.setDragResult(dragResult);
		((ISupportDragNDrop)source).onDragEnd(onDragEvent);
	}

	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)
	{
		JComponent cmp = getDragComponent(comp);
		if (isDataFlavorSupported(transferFlavors) && cmp instanceof ISupportDragNDrop)
		{
			ISupportDragNDrop ddComp = (ISupportDragNDrop)cmp;

			JSDNDEvent event = createScriptEvent(EventType.onDragOver, ddComp, dropTargetDragEvent);
//			ddComp = testTarget(ddComp, event);
			event.setData(eventData);
			return ddComp.onDragOver(event);
		}
		return false;
	}

	public JComponent getDragComponent(JComponent comp)
	{
		if (comp instanceof JViewport)
		{
			return (JComponent)((JViewport)comp).getView();
		}
		return comp;
	}

	/**
	 * @param ddComp
	 * @param event
	 * @return
	 */
//	private ISupportDragNDrop testTarget(ISupportDragNDrop ddComp, JSEvent event)
//	{
//		if (event.js_getSource() instanceof SpecialTabPanel)
//		{
//			SpecialTabPanel tabPanel = (SpecialTabPanel)event.js_getSource();
//			Component selectedComponent = tabPanel.getEnclosingComponent().getSelectedComponent();
//			if (selectedComponent instanceof FormLookupPanel)
//			{
//				FormController formControler = ((FormLookupPanel)selectedComponent).getFormPanel();
//				event.setSource(null);
//				event.setFormName(formControler.getName());
//				event.setElementName(null);
//				return (ISupportDragNDrop)formControler.getViewComponent();
//			}
//		}
//		return ddComp;
//	}

	@Override
	public boolean importData(JComponent comp, Transferable t)
	{
		JComponent cmp = getDragComponent(comp);
		if (cmp instanceof ISupportDragNDrop)
		{
			ISupportDragNDrop ddComp = (ISupportDragNDrop)cmp;
			try
			{
				JSDNDEvent event = createScriptEvent(EventType.onDrop, ddComp, dropTargetDropEvent);
//				ddComp = testTarget(ddComp, event);
				event.setData(eventData);
				eventData = null;
				return ddComp.onDrop(event);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		eventData = null;
		return false;
	}

	private boolean isDataFlavorSupported(DataFlavor[] dataFlavors)
	{
		for (DataFlavor flavor : dataFlavors)
		{
			if (flavor.equals(FormDataTransferable.formDataFlavor)) return true;
		}

		return false;
	}

	private JSDNDEvent createScriptEvent(EventType type, ISupportDragNDrop ddComponent, Object event)
	{
		JSDNDEvent jsEvent = new JSDNDEvent();
		jsEvent.setType(type);
		jsEvent.setFormName(ddComponent.getDragFormName());
		Point location = getEventXY(event);
		if (location != null)
		{
			IComponent dragSource = ddComponent.getDragSource(location);
			if (dragSource instanceof IDataRenderer)
			{
				IDataRenderer dr = (IDataRenderer)dragSource;
				FormController fc = dr.getDataAdapterList().getFormController();
				jsEvent.setSource(fc.getFormScope());
				jsEvent.setElementName(fc.getName());
			}
			else
			{
				jsEvent.setSource(dragSource);
				if (dragSource != null)
				{
					String name = dragSource.getName();
					if (name != null && name.startsWith(ComponentFactory.WEB_ID_PREFIX))
					{
						name = null;
					}
					jsEvent.setElementName(name);
				}
			}
			jsEvent.setLocation(location);
			IRecordInternal dragRecord = ddComponent.getDragRecord(location);
			if (dragRecord instanceof Record) jsEvent.setRecord((Record)dragRecord);
		}
		jsEvent.setModifiers(getModifiersOrDropAction(event));
		return jsEvent;
	}

	private int getModifiersOrDropAction(Object eventObject)
	{
		if (eventObject instanceof MouseEvent)
		{
			MouseEvent mouseEvent = (MouseEvent)eventObject;
			return mouseEvent.getModifiers();
		}
		else if (eventObject instanceof DropTargetDragEvent)
		{
			DropTargetDragEvent event = (DropTargetDragEvent)eventObject;
			return event.getDropAction();
		}
		else if (eventObject instanceof DropTargetDropEvent)
		{
			DropTargetDropEvent event = (DropTargetDropEvent)eventObject;
			return event.getDropAction();
		}
		return 0;
	}

	private Point getEventXY(Object eventObject)
	{
		if (eventObject instanceof MouseEvent)
		{
			MouseEvent mouseEvent = (MouseEvent)eventObject;
			return mouseEvent.getPoint();
		}
		else if (eventObject instanceof DropTargetDragEvent)
		{
			DropTargetDragEvent event = (DropTargetDragEvent)eventObject;
			return event.getLocation();
		}
		else if (eventObject instanceof DropTargetDropEvent)
		{
			DropTargetDropEvent event = (DropTargetDropEvent)eventObject;
			return event.getLocation();
		}
		return null;
	}

	private boolean actionSupported(int action)
	{
		return (action & (COPY_OR_MOVE)) != NONE;
	}

	// --- DropTargetListener methods -----------------------------------

	private IComponent lastDragSource = null;
	private IRecordInternal lastDragRecord = null;

	public void dragEnter(DropTargetDragEvent e)
	{
		dropTargetDragEvent = e;
		DataFlavor[] flavors = e.getCurrentDataFlavors();

		JComponent c = getDragComponent((JComponent)e.getDropTargetContext().getComponent());
		if (c instanceof ISupportDragNDrop)
		{
			ISupportDragNDrop dropComponent = (ISupportDragNDrop)c;
			lastDragSource = dropComponent.getDragSource(getEventXY(e));
			lastDragRecord = dropComponent.getDragRecord(getEventXY(e));
		}
		TransferHandler importer = c.getTransferHandler();

		if (importer != null && importer.canImport(c, flavors))
		{
			canImport = true;
		}
		else
		{
			canImport = false;
		}

		int dropAction = e.getDropAction();

		if (canImport && actionSupported(dropAction))
		{
			e.acceptDrag(dropAction);
		}
		else
		{
			e.rejectDrag();
		}
		dropTargetDragEvent = null;
	}

	public void dragOver(DropTargetDragEvent e)
	{
		dropTargetDragEvent = e;
		int dropAction = e.getDropAction();

		Component component = getDragComponent((JComponent)e.getDropTargetContext().getComponent());
		if (actionSupported(dropAction) && component instanceof ISupportDragNDrop)
		{
			ISupportDragNDrop dropComponent = (ISupportDragNDrop)component;
			IComponent dragSource = dropComponent.getDragSource(getEventXY(e));
			IRecordInternal dragRecord = dropComponent.getDragRecord(getEventXY(e));
			if (dragSource != lastDragSource || dragRecord != lastDragRecord)
			{
				// simulate a drag enter now
				dragEnter(e);
			}
			else
			{
				if (canImport)
				{
					e.acceptDrag(dropAction);
				}
				else
				{
					e.rejectDrag();
				}
			}
		}
		else
		{
			e.rejectDrag();
		}
		dropTargetDragEvent = null;
	}

	public void dragExit(DropTargetEvent e)
	{
		dropTargetDragEvent = null;
	}

	public void drop(DropTargetDropEvent e)
	{
		dropTargetDropEvent = e;
		int dropAction = e.getDropAction();
		JComponent c = getDragComponent((JComponent)e.getDropTargetContext().getComponent());
		TransferHandler importer = c.getTransferHandler();

		if (canImport && importer != null && actionSupported(dropAction))
		{
			e.acceptDrop(dropAction);

			try
			{
				Transferable t = e.getTransferable();
				e.dropComplete(importer.importData(c, t));
			}
			catch (RuntimeException re)
			{
				e.dropComplete(false);
			}
		}
		else
		{
			e.rejectDrop();
		}
		dropTargetDropEvent = null;
	}

	public void dropActionChanged(DropTargetDragEvent e)
	{
		dropTargetDragEvent = e;
		int dropAction = e.getDropAction();

		if (canImport && actionSupported(dropAction))
		{
			e.acceptDrag(dropAction);
		}
		else
		{
			e.rejectDrag();
		}
		dropTargetDragEvent = null;
	}
}
