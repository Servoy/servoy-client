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


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.Document;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ISupportAsyncLoading;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.ui.IScriptMediaInputFieldMethods;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.FileChooserUtils;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.SnapShot;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.FileNameSuggestionFileChooser;
import com.servoy.j2db.util.gui.MyImageIcon;

/**
 * Field for serving blobs (mainly images)
 * 
 * @author jblok
 */
public class DataImgMediaField extends EnableScrollPanel implements IDisplayData, IFieldComponent, IScrollPane, DropTargetListener, ISupportAsyncLoading,
	IScriptMediaInputFieldMethods, ISupportCachedLocationAndSize
{
	private final static Icon NOT_EMPTY_IMAGE;

	static
	{
		byte[] notEmptyImage = new byte[0];
		try
		{
			InputStream is = IApplication.class.getResourceAsStream("images/notemptymedia.gif"); //$NON-NLS-1$
			notEmptyImage = new byte[is.available()];
			is.read(notEmptyImage);
			is.close();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		NOT_EMPTY_IMAGE = ImageLoader.getIcon(notEmptyImage, -1, -1, true);
	}

	private final AbstractScriptLabel enclosedComponent;
	private String dataProviderID;
	private Object value;
	private final IApplication application;
	private final EventExecutor eventExecutor;
	private MouseAdapter rightclickMouseAdapter = null;

	public DataImgMediaField(IApplication app)
	{
		application = app;
		eventExecutor = new EventExecutor(this);//not setting the enclosed comp, not needed here

		getViewport().setView(new AbstractScriptLabel(app));
		enclosedComponent = (AbstractScriptLabel)getViewport().getView();
		enclosedComponent.setOpaque(true);
//		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		enclosedComponent.setMediaOption(1);

		MouseAdapter mouseListener = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger() && popup != null)
				{
					popup.show(DataImgMediaField.this, e.getX() - DataImgMediaField.this.getHorizontalScrollBar().getValue(), e.getY() -
						DataImgMediaField.this.getVerticalScrollBar().getValue());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger() && popup != null)
				{
					// the popup invoker should be displayed in order to show popup; otherwise error is thrown
					// it is for readonly listview where action should work and popup shouldn't
					if (isDisplayable()) popup.show(DataImgMediaField.this, e.getX() - DataImgMediaField.this.getHorizontalScrollBar().getValue(), e.getY() -
						DataImgMediaField.this.getVerticalScrollBar().getValue());
				}
				else if (popup == null || !popup.isVisible())
				{
					eventExecutor.fireActionCommand(false, DataImgMediaField.this, e.getModifiers(), e.getPoint());
				}
			}

//			public void mouseClicked(MouseEvent e)
//			{
//				if (popup == null || !popup.isVisible())
//				{
//					eventExecutor.fireActionCommand(false, DataImgMediaField.this);
//				}
//			}
		};
		addMouseListener(mouseListener);
		enclosedComponent.addMouseListener(mouseListener);
		enclosedComponent.addKeyListener(eventExecutor);

		try
		{
			DropTarget dt = getDropTarget();
			if (dt == null)
			{
				dt = new DropTarget(this, this);
			}
			dt.addDropTargetListener(editProvider);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}


	@Override
	public void setHorizontalScrollBarPolicy(int policy)
	{
		setMediaOptions(getVerticalScrollBarPolicy(), policy);
		super.setHorizontalScrollBarPolicy(policy);
	}

	@Override
	public void setVerticalScrollBarPolicy(int policy)
	{
		setMediaOptions(policy, getHorizontalScrollBarPolicy());
		super.setVerticalScrollBarPolicy(policy);
	}


	private void setMediaOptions(int vertical, int horizontal)
	{
		if (enclosedComponent != null)
		{
			if (horizontal == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER || vertical == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
			{
				enclosedComponent.setMediaOption(10);
			}
			else
			{
				enclosedComponent.setMediaOption(1);
			}
		}
	}

	public Object getValueObject()
	{
		return value;
	}

	public Document getDocument()
	{
		return null;
	}

	public boolean needEditListner()
	{
		return true;
	}

	private EditProvider editProvider = null;

	public void addEditListener(IEditListener l)
	{
		if (editProvider == null)
		{
			editProvider = new EditProvider(this);
			editProvider.addEditListener(l);
		}
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	public boolean needEntireState()
	{
		return true; // always true so that we get have a tagresolver to use when loading/saving for use with dataprovider_filename and _mimetype TODO refactor this so as not to use tag resolver for this
	}

	private boolean needsToProcessTags;

	public void setNeedEntireState(boolean b)
	{
		needsToProcessTags = b;
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getValueObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private boolean useAsync = true;

	public void setAsyncLoadingEnabled(boolean useAsync)
	{
		this.useAsync = useAsync;
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	private Object previousValue;

	public void setValueObject(Object obj)
	{
		if ("".equals(obj)) obj = null; //$NON-NLS-1$
		try
		{
			if (editProvider != null) editProvider.setAdjusting(true);

			if (needsToProcessTags)
			{
				if (resolver != null)
				{
					if (tooltip != null)
					{
						String txt = Text.processTags(tooltip, resolver);
						setToolTipTextImpl(txt);
					}
				}
				else
				{
					if (tooltip != null)
					{
						setToolTipTextImpl(null);
					}
				}
			}

			if (previousValue != null && previousValue.equals(obj)) return;
			previousValue = obj;

			if (obj instanceof byte[] || (obj instanceof String && !editState))
			{
				if (useAsync && application.getModeManager().getMode() == IModeManager.EDIT_MODE)
				{
					enclosedComponent.setIconDirect((Icon)null, enclosedComponent.getNextSeq());//clear previous image
					enclosedComponent.setText(application.getI18NMessage("servoy.imageMedia.loadingImage"));//show loading text //$NON-NLS-1$
				}

				final Object tmp = obj;
				final int seq = enclosedComponent.getNextSeq();

				Runnable action = new Runnable()
				{
					public void run()
					{
						byte[] array;
						if (tmp instanceof String)
						{
							array = Utils.getURLContent((String)tmp);
							if (array == null)
							{
								Debug.error("Cannot get media for field with dataprovider '" + getDataProviderID() + "' on form " + //$NON-NLS-1$ //$NON-NLS-2$
									(application.getFormManager().getCurrentForm() == null ? null : application.getFormManager().getCurrentForm().getName()));
							}
						}
						else
						{
							array = (byte[])tmp;
						}

						Icon icon = ImageLoader.getIcon(array, -1, -1, true);
						if (icon == null)
						{
							if (array != null && array.length > 0)
							{
								icon = NOT_EMPTY_IMAGE;
							}
							else if (application.getI18NMessage("servoy.imageMedia.loadingImage").equals(enclosedComponent.getText())) //$NON-NLS-1$
							{
								enclosedComponent.setText(null);
							}
						}
						if (!useAsync || (application.getModeManager().getMode() != IModeManager.EDIT_MODE) || Utils.equalObjects(tmp, getValueObject()))
						{
							enclosedComponent.setIconDirect(icon, seq);
						}
						else if (application.getI18NMessage("servoy.imageMedia.loadingImage").equals(enclosedComponent.getText())) //$NON-NLS-1$
						{
							enclosedComponent.setText(null);
						}
					}
				};

				if (useAsync && application.getModeManager().getMode() == IModeManager.EDIT_MODE)
				{
					application.getScheduledExecutor().execute(action);
				}
				else
				{
					action.run();
				}
			}
			else
			{
				setIcon(null);
			}
			value = obj;
		}
		finally
		{
			if (editProvider != null) editProvider.setAdjusting(false);
		}
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String id)
	{
		dataProviderID = id;
	}

	/*
	 * _____________________________________________________________ Methods for event handling
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public void setEnterCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setEnterCmds(ids, args);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setLeaveCmds(ids, args);
	}

	public boolean isValueValid()
	{
		return isValueValid;
	}

	private boolean isValueValid = true;
	private Object previousValidValue;

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		isValueValid = valid;
		if (!isValueValid)
		{
			previousValidValue = oldVal;
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					requestFocus();
				}
			});
		}
		else
		{
			previousValidValue = null;
		}
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		if (previousValidValue != null)
		{
			oldVal = previousValidValue;
		}

		previousValue = newVal;

		eventExecutor.fireChangeCommand(oldVal, newVal, false, this);

		// if change cmd is not succeeded also don't call action cmd?
		if (isValueValid)
		{
			eventExecutor.fireActionCommand(false, this);
		}
	}

	public void setChangeCmd(String id, Object[] args)
	{
		eventExecutor.setChangeCmd(id, args);
	}

	public void setActionCmd(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
		if (rightClickCmd != null && rightclickMouseAdapter == null)
		{
			rightclickMouseAdapter = new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (e.isPopupTrigger()) handle(e);
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (e.isPopupTrigger()) handle(e);
				}

				private void handle(MouseEvent e)
				{
					if (isEnabled())
					{
						eventExecutor.fireRightclickCommand(true, DataImgMediaField.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			enclosedComponent.addMouseListener(rightclickMouseAdapter);
		}
	}

	public void setValidationEnabled(boolean b)
	{
		eventExecutor.setValidationEnabled(b);
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________


	public boolean stopUIEditing(boolean looseFocus)
	{
		if (editProvider != null) editProvider.commitData();
		if (!isValueValid)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					requestFocus();
				}
			});
			return false;
		}
		return true;
	}

	private JPopupMenu popup;

	public void setEditable(boolean b)
	{
		editState = b;
		if (b)
		{
			ActionListener listener = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					String cmd = e.getActionCommand();
					if (cmd.equals("load")) //$NON-NLS-1$
					{
						loadFromFile();
					}
					else if (cmd.equals("save")) //$NON-NLS-1$
					{
						saveToFile();
					}
					else if (cmd.equals("copy")) //$NON-NLS-1$
					{
						copyToClipboard();
					}
					else if (cmd.equals("paste")) //$NON-NLS-1$
					{
						pasteFromClipboard();
					}
					else if (cmd.equals("remove")) //$NON-NLS-1$
					{
						removeImage();
					}
				}
			};
			popup = new JPopupMenu();
			JMenuItem loadMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.load")); //$NON-NLS-1$
			loadMenuItem.setActionCommand("load"); //$NON-NLS-1$
			loadMenuItem.addActionListener(listener);
			popup.add(loadMenuItem);
			JMenuItem saveMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.save")); //$NON-NLS-1$
			saveMenuItem.setActionCommand("save"); //$NON-NLS-1$
			saveMenuItem.addActionListener(listener);
			popup.add(saveMenuItem);
			JMenuItem copyMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.copy")); //$NON-NLS-1$
			copyMenuItem.setActionCommand("copy"); //$NON-NLS-1$
			copyMenuItem.addActionListener(listener);
			popup.add(copyMenuItem);
			JMenuItem pasteMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.paste")); //$NON-NLS-1$
			pasteMenuItem.setActionCommand("paste"); //$NON-NLS-1$
			pasteMenuItem.addActionListener(listener);
			popup.add(pasteMenuItem);
			JMenuItem removeMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.remove")); //$NON-NLS-1$
			removeMenuItem.setActionCommand("remove"); //$NON-NLS-1$
			removeMenuItem.addActionListener(listener);
			popup.add(removeMenuItem);
		}
		else
		{
			popup = null;
		}
	}

	protected void pasteFromClipboard()
	{
		try
		{
			Transferable tr = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			Image img = (Image)tr.getTransferData(DataFlavor.imageFlavor);
			byte[] data = SnapShot.createJPGImage(application.getMainApplicationFrame(), img, -1, -1);
			if (data != null && data.length != 0)
			{
				if (editProvider != null) editProvider.startEdit();
				setValueObject(data);
				if (editProvider != null) editProvider.commitData();
				// dataprovider_filename and dataprovider_mimetype fields will be set like in the web TODO maybe refactor to avoid cast below and existence of method setValueObject in DataAdapterList 
				if (resolver instanceof DataAdapterList)
				{
					((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.FILENAME, null);
					((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.MIMETYPE, null);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	protected void removeImage()
	{
		try
		{
			if (editProvider != null) editProvider.startEdit();
			setValueObject(null);
			if (editProvider != null) editProvider.commitData();
			// dataprovider_filename and dataprovider_mimetype fields will be set like in the web TODO maybe refactor to avoid cast below and existence of method setValueObject in DataAdapterList 
			if (resolver instanceof DataAdapterList)
			{
				((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.FILENAME, null);
				((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.MIMETYPE, null);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	protected void copyToClipboard()
	{
		try
		{
			final Icon icon = enclosedComponent.getIcon();
			if (icon instanceof ImageIcon || icon instanceof MyImageIcon)
			{
				Transferable tr = new Transferable()
				{
					public DataFlavor[] getTransferDataFlavors()
					{
						return new DataFlavor[] { DataFlavor.imageFlavor };
					}

					public boolean isDataFlavorSupported(DataFlavor flavor)
					{
						return DataFlavor.imageFlavor.equals(flavor);
					}

					public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
					{
						if (!isDataFlavorSupported(flavor))
						{
							throw new UnsupportedFlavorException(flavor);
						}

						if (icon instanceof ImageIcon)
						{
							return ((ImageIcon)icon).getImage();
						}
						else if (icon instanceof MyImageIcon)
						{
							return ((MyImageIcon)icon).getOriginal().getImage();
						}
						return null;
					}
				};

				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(tr, null);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	void loadFromFile()
	{
		JFileChooser fc = new JFileChooser();
		int returnVal = fc.showOpenDialog(application.getMainApplicationFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				File f = fc.getSelectedFile();
				if (editProvider != null) editProvider.startEdit();
				byte[] content = null;
				if (application.isEventDispatchThread())
				{
					setValueObject(content = FileChooserUtils.paintingReadFile(application.getScheduledExecutor(), application, f));
				}
				else
				{
					setValueObject(content = FileChooserUtils.readFile(f));
				}

				if (editProvider != null) editProvider.commitData();
				// dataprovider_filename and dataprovider_mimetype fields will be set like in the web TODO maybe refactor to avoid cast below and existence of method setValueObject in DataAdapterList 
				if (resolver instanceof DataAdapterList)
				{
					((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.FILENAME, f.getName());
					((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.MIMETYPE, ImageLoader.getContentType(content, f.getName()));
				}
			}
			catch (Exception e)
			{
				application.reportError(this, application.getI18NMessage("servoy.imageMedia.error.loading"), e); //$NON-NLS-1$
			}
		}
	}

	void saveToFile()
	{
		if (value instanceof byte[])
		{
			byte[] array = (byte[])value;
			FileNameSuggestionFileChooser fc = new FileNameSuggestionFileChooser();

			String fileName = null;
			// dataprovider_filename and dataprovider_mimetype fields will be used like in the web TODO maybe refactor to avoid cast below 
			if (resolver instanceof DataAdapterList)
			{
				Object val = ((DataAdapterList)resolver).getValueObject(((DataAdapterList)resolver).getState(), dataProviderID + IMediaFieldConstants.FILENAME);
				fileName = (val instanceof String) ? (String)val : null;
			}

			if (fileName != null)
			{
				fc.suggestFileName(fileName);
			}
			else if (array.length > 3 && array[0] == -1 && array[1] == -40 && array[2] == -1)//jpeg
			{
				fc.suggestFileName("image.jpg"); //$NON-NLS-1$
			}
			else if (array.length > 3 && array[0] == 0x47 && array[1] == 0x49 && array[2] == 0x46)//gif
			{
				fc.suggestFileName("image.gif"); //$NON-NLS-1$
			}
			else
			{
				fc.suggestFileName("filename.unknown"); //$NON-NLS-1$
			}

			int returnVal = fc.showSaveDialog(application.getMainApplicationFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					File f = fc.getSelectedFile();
					OutputStream os = new FileOutputStream(f);
					BufferedOutputStream bos = new BufferedOutputStream(os);
					bos.write(array);
					bos.close();
				}
				catch (Exception e)
				{
					application.reportError(this, application.getI18NMessage("servoy.imageMedia.error.loading"), e); //$NON-NLS-1$
				}
			}
		}
	}

	public int getDataType()
	{
		return dataType;
	}

	private int dataType;
	private String format;

	public void setFormat(int dataType, String format)
	{
		this.dataType = dataType;
		this.format = format;
	}

	public String getFormat()
	{
		return format;
	}

	public void setMaxLength(int i)
	{
		//ignore
	}

	public void setHorizontalAlignment(int a)
	{
		enclosedComponent.setHorizontalAlignment(a);
	}

	public void setMargin(Insets i)
	{
		// TODO why no call to enclosedComponent (disappeared in version 1.46)
	}

	/**
	 * processed on background thread
	 */
	public void setIcon(Icon icon)
	{
		enclosedComponent.setIcon(icon);
	}

	public synchronized void drop(DropTargetDropEvent dropTargetDropEvent)
	{
		try
		{
			Transferable tr = dropTargetDropEvent.getTransferable();
			if (popup != null && tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			{
				dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				List fileList = (List)tr.getTransferData(DataFlavor.javaFileListFlavor);
				Iterator iterator = fileList.iterator();
				if (iterator.hasNext())
				{
					try
					{
						File file = (File)iterator.next();
						if (editProvider != null) editProvider.startEdit();
						byte content[] = FileChooserUtils.paintingReadFile(application.getScheduledExecutor(), application, file);
						setValueObject(content);
						if (editProvider != null) editProvider.commitData();
						// dataprovider_filename and dataprovider_mimetype fields will be set like in the web TODO maybe refactor to avoid cast below and existence of method setValueObject in DataAdapterList 
						if (resolver instanceof DataAdapterList)
						{
							((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.FILENAME, file.getName());
							((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.MIMETYPE, ImageLoader.getContentType(content,
								file.getName()));
						}
					}
					catch (Exception e)
					{
						application.reportError(this, application.getI18NMessage("servoy.imageMedia.error.loading"), e); //$NON-NLS-1$
					}
				}
				dropTargetDropEvent.getDropTargetContext().dropComplete(true);
			}
			else
			{
				Debug.trace("Rejected"); //$NON-NLS-1$
				dropTargetDropEvent.rejectDrop();
			}
		}
		catch (Exception io)
		{
			Debug.error(io);
			dropTargetDropEvent.rejectDrop();
		}
	}

	public void dragEnter(DropTargetDragEvent dropTargetDragEvent)
	{
		if (isEnabled() && popup != null)
		{
			dropTargetDragEvent.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		}
		else
		{
			dropTargetDragEvent.rejectDrag();
		}
	}

	public void dragExit(DropTargetEvent dropTargetEvent)
	{
	}

	public void dragOver(DropTargetDragEvent dropTargetDragEvent)
	{
	}

	public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent)
	{
	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public String js_getDataProviderID()
	{
		return getDataProviderID();
	}

	public String js_getElementType()
	{
		return "IMAGE_MEDIA"; //$NON-NLS-1$
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	@Override
	public void setBackground(Color bg)
	{
		if (enclosedComponent != null) enclosedComponent.setBackground(bg);
		super.setBackground(bg);
	}

	@Override
	public Color getBackground()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getBackground();
		}
		return super.getBackground();
	}

	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}

	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(getBackground());
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	@Override
	public Color getForeground()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getForeground();
		}
		return super.getForeground();
	}

	@Override
	public void setForeground(Color fg)
	{
		if (enclosedComponent != null) enclosedComponent.setForeground(fg);
		super.setForeground(fg);
	}

	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		setForeground(PersistHelper.createColor(clr));
	}

	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	private Point cachedLocation;

	/*
	 * location---------------------------------------------------
	 */
	public void js_setLocation(int x, int y)
	{
		cachedLocation = new Point(x, y);
		setLocation(x, y);
		validate();
	}

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	public int js_getHeight()
	{
		return getSize().height;
	}

	public int js_getWidth()
	{
		return getSize().width;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		putClientProperty(key, value);
	}

	public Object js_getClientProperty(Object key)
	{
		return getClientProperty(key);
	}


	private Dimension cachedSize;

	/*
	 * size---------------------------------------------------
	 */
	public void js_setSize(int x, int y)
	{
		cachedSize = new Dimension(x, y);
		setSize(x, y);
		validate();
	}

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		Container parent = getParent();
		while ((parent != null) && !(parent instanceof IDataRenderer))
		{
			parent = parent.getParent();
		}
		if (parent != null)
		{
			return ((IDataRenderer)parent).getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	/*
	 * titleText---------------------------------------------------
	 */

	private String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

	public String js_getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	/*
	 * tooltip---------------------------------------------------
	 */
	public String js_getToolTipText()
	{
		return getToolTipText();
	}

	public void js_setToolTipText(String txt)
	{
		setToolTipText(txt);
	}

	private String tooltip;

	@Override
	public void setToolTipText(String tip)
	{
		if (tip != null && tip.indexOf("%%") != -1) //$NON-NLS-1$
		{
			tooltip = tip;
		}
		else if (!Utils.stringIsEmpty(tip))
		{
			if (!Utils.stringContainsIgnoreCase(tip, "<html") || HtmlUtils.hasUsefulHtmlContent(tip)) //$NON-NLS-1$
			{
				setToolTipTextImpl(tip);
			}
		}
		else
		{
			setToolTipTextImpl(null);
		}
	}


	/**
	 * @param tip
	 */
	private void setToolTipTextImpl(String tip)
	{
		enclosedComponent.setToolTipText(tip);
		getViewport().setToolTipText(tip);
		super.setToolTipText(tip);
	}


	/*
	 * readonly---------------------------------------------------
	 */
	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	private boolean editState;

	public void js_setReadOnly(boolean b)
	{
		if (b && popup == null) return;
		if (b)
		{
			setEditable(false);
			editState = true;
		}
		else
		{
			setEditable(editState);
		}
	}

	public boolean isReadOnly()
	{
		return (popup == null);
	}


	public boolean js_isEditable()
	{
		return isEnabled();
	}

	/*
	 * enabled---------------------------------------------------
	 */
	public void js_setEnabled(final boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = (ILabel)labels.get(i);
					label.setComponentEnabled(b);
				}
			}
		}
	}

	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	private boolean accessible = true;
	private ArrayList labels;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public boolean js_isTransparent()
	{
		return !isOpaque();
	}

	public void js_setTransparent(boolean b)
	{
		setOpaque(!b);
	}

	@Override
	public void setOpaque(boolean b)
	{
		if (enclosedComponent != null) enclosedComponent.setOpaque(b);
		getViewport().setOpaque(b);
		super.setOpaque(b);
	}


	/*
	 * scroll---------------------------------------------------
	 */
	public void js_setScroll(int x, int y)
	{
		enclosedComponent.scrollRectToVisible(new Rectangle(x, y, getWidth(), getHeight()));
	}

	public int js_getScrollX()
	{
		return enclosedComponent.getVisibleRect().x;
	}

	public int js_getScrollY()
	{
		return enclosedComponent.getVisibleRect().y;
	}


	/*
	 * visible---------------------------------------------------
	 */
	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean b)
	{
		setVisible(b);
	}

	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}


	@Override
	public void setVisible(boolean flag)
	{
		super.setVisible(flag);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = (ILabel)labels.get(i);
				label.setComponentVisible(flag);
			}
		}
	}

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList(3);
		labels.add(label);
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}
}
