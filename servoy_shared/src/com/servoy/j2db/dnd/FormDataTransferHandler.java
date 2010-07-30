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
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;

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
import com.servoy.j2db.util.Utils;

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
	private FormDataTransferable formDataTransferable;

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

		JSDNDEvent onDragEvent = createScriptEvent(EventType.onDrag, (ISupportDragNDrop)c, inputEvent);
		int dragReturn = ddComp.onDrag(onDragEvent);
		try
		{
			formDataTransferable = new FormDataTransferable(onDragEvent.getData(), onDragEvent.getDataMimeType());
		}
		catch (ClassNotFoundException ex)
		{
			Debug.error(ex);
			return TransferHandler.NONE;
		}

		return dragReturn == DRAGNDROP.NONE ? TransferHandler.NONE : dragReturn == DRAGNDROP.COPY ? TransferHandler.COPY : dragReturn == DRAGNDROP.MOVE
			? TransferHandler.MOVE : dragReturn == DRAGNDROP.COPY + DRAGNDROP.MOVE ? TransferHandler.COPY_OR_MOVE : TransferHandler.NONE;
	}

	/**
	 * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
	 */
	@Override
	protected Transferable createTransferable(JComponent c)
	{
		return formDataTransferable;
	}

	@Override
	public void exportAsDrag(JComponent comp, InputEvent e, int action)
	{
		inputEvent = e;
		super.exportAsDrag(comp, e, action);
	}

	/**
	 * @see javax.swing.TransferHandler#exportDone(javax.swing.JComponent, java.awt.datatransfer.Transferable, int)
	 */
	@Override
	protected void exportDone(JComponent source, Transferable data, int action)
	{
		super.exportDone(source, data, action);
		JComponent cmp = getDragComponent(source);
		if (cmp instanceof ISupportDragNDrop)
		{
			ISupportDragNDrop ddComp = (ISupportDragNDrop)cmp;

			JSDNDEvent onDragEndEvent = createScriptEvent(EventType.onDragEnd, ddComp, inputEvent);
			int dragResult = action == COPY ? DRAGNDROP.COPY : action == MOVE ? DRAGNDROP.MOVE : DRAGNDROP.NONE;
			onDragEndEvent.setDragResult(dragResult);
			try
			{
				DataFlavor[] transferableFlavors = data.getTransferDataFlavors();
				if (transferableFlavors.length > 0)
				{
					onDragEndEvent.setDataMimeType(transferableFlavors[0].getMimeType());
					if (transferableFlavors[0].isRepresentationClassInputStream() || transferableFlavors[0].isRepresentationClassReader()) onDragEndEvent.setData(null);
					else onDragEndEvent.setData(data.getTransferData(transferableFlavors[0]));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			((ISupportDragNDrop)source).onDragEnd(onDragEndEvent);
		}
		formDataTransferable = null;
		inputEvent = null;
	}

	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors, Transferable transferable)
	{
		JComponent cmp = getDragComponent(comp);
		if (cmp instanceof ISupportDragNDrop)
		{
			ISupportDragNDrop ddComp = (ISupportDragNDrop)cmp;

			JSDNDEvent event = createScriptEvent(EventType.onDragOver, ddComp, dropTargetDragEvent);
//			ddComp = testTarget(ddComp, event);
			try
			{
				DataFlavor[] transferableFlavors = transferable.getTransferDataFlavors();
				if (transferableFlavors.length > 0)
				{
					event.setDataMimeType(transferableFlavors[0].getMimeType());
					if (transferableFlavors[0].isRepresentationClassInputStream() || transferableFlavors[0].isRepresentationClassReader()) event.setData(null);
					else event.setData(transferable.getTransferData(transferableFlavors[0]));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
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
			JSDNDEvent event = createScriptEvent(EventType.onDrop, ddComp, dropTargetDropEvent);
//				ddComp = testTarget(ddComp, event);

			try
			{
				DataFlavor[] transferableFlavors = t.getTransferDataFlavors();
				if (transferableFlavors.length > 0)
				{
					event.setDataMimeType(transferableFlavors[0].getMimeType());
					if (transferableFlavors[0].isRepresentationClassInputStream())
					{
						BufferedInputStream contents = null;
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						byte[] buffer = new byte[4096];
						int len;
						try
						{
							contents = new BufferedInputStream((InputStream)t.getTransferData(transferableFlavors[0]));
							while ((len = contents.read(buffer)) != -1)
								bos.write(buffer, 0, len);
							event.setData(bos.toByteArray());
						}
						finally
						{
							Utils.closeOutputStream(bos);
							Utils.closeInputStream(contents);
						}
					}
					else if (transferableFlavors[0].isRepresentationClassReader())
					{
						Reader contents = (Reader)t.getTransferData(transferableFlavors[0]);
						StringWriter stringWriter = new StringWriter();
						try
						{
							Utils.readerWriterCopy(contents, stringWriter);
							event.setData(stringWriter.toString());
						}
						finally
						{
							Utils.closeWriter(stringWriter);
							Utils.closeReader(contents);
						}
					}
					else event.setData(t.getTransferData(transferableFlavors[0]));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			return ddComp.onDrop(event);

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

		if (importer instanceof FormDataTransferHandler && ((FormDataTransferHandler)importer).canImport(c, flavors, e.getTransferable()))
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
