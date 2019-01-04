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
import java.awt.Container;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.UIManager;

import com.servoy.base.scripting.api.IJSEvent.EventType;
import com.servoy.j2db.FormController;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.util.Debug;


/**
 * Class used for handling drag and drop operations in smart client on forms
 *
 * @author gboros
 */
public class FormDataTransferHandler extends CompositeTransferHandler
{
	private static CompositeTransferHandler instance;

	public static TransferHandler getInstance()
	{
		if (instance == null) instance = new FormDataTransferHandler();

		return instance;
	}

	@Override
	protected JSDNDEvent createScriptEvent(EventType type, ICompositeDragNDrop ddComponent, Object event)
	{
		JSDNDEvent jsEvent = super.createScriptEvent(type, ddComponent, event);

		if (ddComponent instanceof IFormDataDragNDrop)
		{
			IFormDataDragNDrop formDataDDComponent = (IFormDataDragNDrop)ddComponent;
			jsEvent.setFormName(formDataDDComponent.getDragFormName());
			Point location = getEventXY(event);
			if (location != null)
			{
				Object dragSource = ddComponent.getDragSource(location);
				if (dragSource instanceof IDataRenderer)
				{
					IDataRenderer dr = (IDataRenderer)dragSource;
					FormController fc = dr.getDataAdapterList().getFormController();
					jsEvent.setSource(fc.getFormScope());
				}
				else if (dragSource instanceof IComponent)
				{
					jsEvent.setSource(dragSource);
					if (dragSource != null)
					{
						String name = ((IComponent)dragSource).getName();
						if (name != null && name.startsWith(ComponentFactory.WEB_ID_PREFIX))
						{
							name = null;
						}
						jsEvent.setElementName(name);
					}
				}
				IRecordInternal dragRecord = formDataDDComponent.getDragRecord(location);
				if (dragRecord instanceof Record) jsEvent.setRecord((Record)dragRecord);
			}
		}

		return jsEvent;
	}

	@Override
	protected boolean canReplaceDragSource(Object currentDragSource, Object newDragSource, DropTargetDragEvent e)
	{
		if (currentDragSource instanceof Component && newDragSource instanceof Component)
		{
			if (currentDragSource != newDragSource)
			{
				// check if the oldLastDragSource is hidding the newDragSource
				Component cCurrentDragSource = (Component)currentDragSource;
				Component cNewDragSource = (Component)newDragSource;

				Point xy = getEventXY(e);
				Container currentDsParent = cCurrentDragSource.getParent();
				Container newDsParent = cNewDragSource.getParent();
				boolean isCompositeParentFound = cNewDragSource instanceof ICompositeDragNDrop;

				while (newDsParent != null && newDsParent != currentDsParent)
				{
					if (isCompositeParentFound) xy.translate(cNewDragSource.getX(), cNewDragSource.getY());
					else isCompositeParentFound = newDsParent instanceof ICompositeDragNDrop;
					cNewDragSource = newDsParent;
					newDsParent = cNewDragSource.getParent();
				}
				if (isCompositeParentFound) xy.translate(cNewDragSource.getX(), cNewDragSource.getY());

				if (newDsParent != null && newDsParent == currentDsParent)
				{
					Component visibleComponent = newDsParent.findComponentAt(xy);
					return visibleComponent == newDragSource;
				}
			}
			else
			{
				return true;
			}
		}

		return super.canReplaceDragSource(currentDragSource, newDragSource, e);
	}

	static final Action cutFormDataAction = new TransferAction("cut");
	static final Action copyFormDataAction = new TransferAction("copy");
	static final Action pasteFormDataAction = new TransferAction("paste");

	public static Action getCopyFormDataAction()
	{
		return copyFormDataAction;
	}

	public static Action getCutFormDataAction()
	{
		return cutFormDataAction;
	}

	public static Action getPasteFormDataAction()
	{
		return pasteFormDataAction;
	}

	static class TransferAction extends AbstractAction
	{
		TransferAction(String name)
		{
			super(name);
		}

		public void actionPerformed(ActionEvent e)
		{
			Object src = e.getSource();
			if (src instanceof JComponent)
			{
				JComponent c = (JComponent)src;
				TransferHandler th = (src instanceof ISupportDragNDropTextTransfer) ? ((ISupportDragNDropTextTransfer)src).getTextTransferHandler()
					: c.getTransferHandler();
				Clipboard clipboard = getClipboard(c);
				String name = (String)getValue(Action.NAME);
				Transferable trans = null;

				// any of these calls may throw IllegalStateException
				try
				{
					if ((clipboard != null) && (th != null) && (name != null))
					{
						if ("cut".equals(name))
						{
							th.exportToClipboard(c, clipboard, MOVE);
						}
						else if ("copy".equals(name))
						{
							th.exportToClipboard(c, clipboard, COPY);
						}
						else if ("paste".equals(name))
						{
							trans = clipboard.getContents(null);
						}
					}
				}
				catch (IllegalStateException ise)
				{
					// clipboard was unavailable
					UIManager.getLookAndFeel().provideErrorFeedback(c);
					return;
				}

				// this is a paste action, import data into the component
				if (trans != null)
				{
					th.importData(c, trans);
				}
			}
		}

		/**
		 * Returns the clipboard to use for cut/copy/paste.
		 */
		private Clipboard getClipboard(JComponent c)
		{
			Clipboard clipboard = null;

			try
			{
				clipboard = c.getToolkit().getSystemClipboard();
			}
			catch (Exception ex)
			{
				Debug.error("Error getting system clipboard", ex);
			}

			return clipboard;
		}
	}
}
