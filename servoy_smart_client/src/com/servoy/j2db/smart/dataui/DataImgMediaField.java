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
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
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
import javax.swing.TransferHandler;
import javax.swing.text.Document;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.component.ISupportAsyncLoading;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.scripting.RuntimeMediaField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.FileChooserUtils;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.FileNameSuggestionFileChooser;
import com.servoy.j2db.util.gui.MyImageIcon;
import com.servoy.j2db.util.gui.SnapShot;

/**
 * Field for serving blobs (mainly images)
 *
 * @author jblok
 */
@SuppressWarnings("nls")
public class DataImgMediaField extends EnableScrollPanel implements IDisplayData, IFieldComponent, IScrollPane, DropTargetListener, ISupportAsyncLoading,
	ISupportCachedLocationAndSize, ISupportOnRender
{
	private final static Icon NOT_EMPTY_IMAGE;

	static
	{
		byte[] notEmptyImage = new byte[0];
		try
		{
			InputStream is = IApplication.class.getResourceAsStream("images/notemptymedia.gif");
			notEmptyImage = new byte[is.available()];
			is.read(notEmptyImage);
			is.close();
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}
		NOT_EMPTY_IMAGE = ImageLoader.getIcon(notEmptyImage, -1, -1, true);
	}

	private final AbstractScriptLabel enclosedComponent;
	private String dataProviderID;
	private volatile Object value;
	private final IApplication application;
	private final EventExecutor eventExecutor;
	private MouseAdapter rightclickMouseAdapter = null;
	private final RuntimeMediaField scriptable;

	public DataImgMediaField(IApplication app, RuntimeMediaField scriptable)
	{
		application = app;
		eventExecutor = new EventExecutor(this);//not setting the enclosed comp, not needed here

		getViewport().setView(new AbstractScriptLabel(app, null /* no scriptable */)
		{
			@Override
			public String toString() // uses scriptable
			{
				return "Enclosed component for  " + DataImgMediaField.this.toString();
			}
		});
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
				if (e.isPopupTrigger())
				{
					if (popup != null)
					{
						// the popup invoker should be displayed in order to show popup; otherwise error is thrown
						// it is for readonly listview where action should work and popup shouldn't
						if (isDisplayable()) popup.show(DataImgMediaField.this, e.getX() - DataImgMediaField.this.getHorizontalScrollBar().getValue(),
							e.getY() - DataImgMediaField.this.getVerticalScrollBar().getValue());
					}
				}
				else if ((popup == null || !popup.isVisible()) && isEnabled())
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
			if (!GraphicsEnvironment.isHeadless())
			{
				DropTarget dt = getDropTarget();
				if (dt == null)
				{
					dt = new DropTarget(this, this);
				}
				dt.addDropTargetListener(editProvider);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		this.scriptable = scriptable;
		scriptable.setjComponent(enclosedComponent);
	}

	public final RuntimeMediaField getScriptObject()
	{
		return scriptable;
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

	public boolean needEditListener()
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
		return scriptable.toString();
	}

	private boolean useAsync = true;

	public void setAsyncLoadingEnabled(boolean useAsync)
	{
		this.useAsync = useAsync;
	}

	private boolean isAsyncLoading()
	{
		return useAsync && !Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting"));
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	private Object previousValue;

	public void setValueObject(Object obj)
	{
		if ("".equals(obj)) obj = null;
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

			value = obj;

			if (obj instanceof byte[] || (obj instanceof String && !editState))
			{
				if (isAsyncLoading() && application.getModeManager().getMode() == IModeManager.EDIT_MODE)
				{
					enclosedComponent.setIconDirect((Icon)null, enclosedComponent.getNextSeq());//clear previous image
					enclosedComponent.setText(application.getI18NMessage("servoy.imageMedia.loadingImage"));//show loading text
				}

				final Object tmp = obj;
				final int seq = enclosedComponent.getNextSeq();

				Runnable action = new Runnable()
				{
					public void run()
					{
						byte[] array = getByteArrayContents(tmp);

						Icon icon = ImageLoader.getIcon(array, -1, -1, true);
						if (icon == null)
						{
							if (array != null && array.length > 0)
							{
								icon = NOT_EMPTY_IMAGE;
							}
							else if (application.getI18NMessage("servoy.imageMedia.loadingImage").equals(enclosedComponent.getText()))
							{
								enclosedComponent.setText(null);
							}
						}
						if (!isAsyncLoading() || (application.getModeManager().getMode() != IModeManager.EDIT_MODE) ||
							Utils.equalObjects(tmp, getValueObject()))
						{
							enclosedComponent.setIconDirect(icon, seq);
						}
						else if (application.getI18NMessage("servoy.imageMedia.loadingImage").equals(enclosedComponent.getText()))
						{
							enclosedComponent.setText(null);
						}
					}
				};

				if (isAsyncLoading() && application.getModeManager().getMode() == IModeManager.EDIT_MODE)
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
				if (application.getI18NMessage("servoy.imageMedia.loadingImage").equals(enclosedComponent.getText()))
				{
					enclosedComponent.setText(null);
				}

			}
		}
		finally
		{
			if (editProvider != null) editProvider.setAdjusting(false);
		}
		fireOnRender(false);
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable != null)
		{
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(hasFocus());
		}
	}

	private byte[] getByteArrayContents(Object tmp)
	{
		byte[] array = null;
		if (tmp instanceof String)
		{
			array = Utils.getURLContent((String)tmp, application);
			if (array == null)
			{
				Debug.error("Cannot get media for field with dataprovider '" + getDataProviderID() + "' on form " +
					(application.getFormManager().getCurrentForm() == null ? null : application.getFormManager().getCurrentForm().getName()));
			}
		}
		else if (tmp instanceof byte[])
		{
			array = (byte[])tmp;
		}
		return array;
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
					if (cmd.equals("load"))
					{
						loadFromFile();
					}
					else if (cmd.equals("save"))
					{
						saveToFile();
					}
					else if (cmd.equals("copy"))
					{
						copyToClipboard();
					}
					else if (cmd.equals("paste"))
					{
						pasteFromClipboard();
					}
					else if (cmd.equals("remove"))
					{
						removeImage();
					}
				}
			};
			popup = new JPopupMenu();
			JMenuItem loadMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.load"));
			loadMenuItem.setActionCommand("load");
			loadMenuItem.addActionListener(listener);
			popup.add(loadMenuItem);
			JMenuItem saveMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.save"));
			saveMenuItem.setActionCommand("save");
			saveMenuItem.addActionListener(listener);
			popup.add(saveMenuItem);
			JMenuItem copyMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.copy"));
			copyMenuItem.setActionCommand("copy");
			copyMenuItem.addActionListener(listener);
			popup.add(copyMenuItem);
			JMenuItem pasteMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.paste"));
			pasteMenuItem.setActionCommand("paste");
			pasteMenuItem.addActionListener(listener);
			popup.add(pasteMenuItem);
			JMenuItem removeMenuItem = new JMenuItem(application.getI18NMessage("servoy.imageMedia.popup.menuitem.remove"));
			removeMenuItem.setActionCommand("remove");
			removeMenuItem.addActionListener(listener);
			popup.add(removeMenuItem);
		}
		else
		{
			popup = null;
		}
	}

	private static DataFlavor getByteArrayDataFlavor()
	{
		return new DataFlavor(byte[].class, "Byte Array");
	}

	private static DataFlavor getStringArrayDataFlavor()
	{
		return new DataFlavor(String[].class, "String Array");
	}

	protected void copyToClipboard()
	{
		try
		{
			// we copy to clipboard as byte array and filename/mimetype array used when pasting into another media field and as image icon for external pastes or if byte array is not available (and for external types)
			final Icon icon = enclosedComponent.getIcon();
			final byte[] bytes = getByteArrayContents(value);
			final String[] fileNameAndMime = new String[2];
			if (resolver instanceof DataAdapterList)
			{
				DataAdapterList dal = ((DataAdapterList)resolver);
				Object tmp = dal.getValueObject(dal.getState(), dataProviderID + IMediaFieldConstants.FILENAME);
				fileNameAndMime[0] = (tmp instanceof String) ? (String)tmp : null;
				tmp = dal.getValueObject(dal.getState(), dataProviderID + IMediaFieldConstants.MIMETYPE);
				fileNameAndMime[1] = (tmp instanceof String) ? (String)tmp : null;
			}

			if (icon instanceof ImageIcon || icon instanceof MyImageIcon)
			{
				Transferable tr = new Transferable()
				{
					public DataFlavor[] getTransferDataFlavors()
					{
						List<DataFlavor> list = new ArrayList<DataFlavor>(3);
						if (bytes != null) list.add(getByteArrayDataFlavor());
						list.add(DataFlavor.imageFlavor);
						if (fileNameAndMime[0] != null || fileNameAndMime[1] != null)
						{
							list.add(getStringArrayDataFlavor());
							list.add(DataFlavor.stringFlavor);
						}
						return list.toArray(new DataFlavor[list.size()]);
					}

					public boolean isDataFlavorSupported(DataFlavor flavor)
					{
						return DataFlavor.imageFlavor.equals(flavor) ||
							(bytes != null && getByteArrayDataFlavor().equals(flavor)) ||
							((fileNameAndMime[0] != null || fileNameAndMime[1] != null) && (getStringArrayDataFlavor().equals(flavor) || DataFlavor.stringFlavor.equals(flavor)));
					}

					public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
					{
						if (!isDataFlavorSupported(flavor))
						{
							throw new UnsupportedFlavorException(flavor);
						}

						if (DataFlavor.imageFlavor.equals(flavor))
						{
							if (icon instanceof ImageIcon)
							{
								return ((ImageIcon)icon).getImage();
							}
							else if (icon instanceof MyImageIcon)
							{
								return ((MyImageIcon)icon).getOriginal().getImage();
							}
						}
						else if (getByteArrayDataFlavor().equals(flavor))
						{
							return bytes;
						}
						else if (getStringArrayDataFlavor().equals(flavor))
						{
							return fileNameAndMime;
						}
						else if (DataFlavor.stringFlavor.equals(flavor))
						{
							return (fileNameAndMime[0] != null ? "Filename: '" + fileNameAndMime[0] + "' " : "") +
								(fileNameAndMime[1] != null ? "Type: '" + fileNameAndMime[1] + "'" : "");
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

	protected void pasteFromClipboard()
	{
		try
		{
			Transferable tr = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

			byte[] data;
			if (tr.isDataFlavorSupported(getByteArrayDataFlavor()))
			{
				data = (byte[])tr.getTransferData(getByteArrayDataFlavor());
			}
			else
			{
				Image img = (Image)tr.getTransferData(DataFlavor.imageFlavor);
				data = SnapShot.createJPGImage(((ISmartClientApplication)application).getMainApplicationFrame(), img, -1, -1);
			}

			String[] fileNameAndMimeType;
			if (tr.isDataFlavorSupported(getStringArrayDataFlavor()))
			{
				fileNameAndMimeType = (String[])tr.getTransferData(getStringArrayDataFlavor());
				if (fileNameAndMimeType == null || fileNameAndMimeType.length != 2) fileNameAndMimeType = new String[2]; // use null values as this is unknown clipboard data
			}
			else
			{
				fileNameAndMimeType = new String[2]; // null values
			}

			if (data != null && data.length != 0)
			{
				if (editProvider != null) editProvider.startEdit();
				setValueObject(data);
				if (editProvider != null) editProvider.commitData();
				// dataprovider_filename and dataprovider_mimetype fields will be set like in the web TODO maybe refactor to avoid cast below and existence of method setValueObject in DataAdapterList
				if (resolver instanceof DataAdapterList)
				{
					((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.FILENAME, fileNameAndMimeType[0]);
					((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.MIMETYPE, fileNameAndMimeType[1]);
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

	void loadFromFile()
	{
		JFileChooser fc = new JFileChooser();
		int returnVal = fc.showOpenDialog(((ISmartClientApplication)application).getMainApplicationFrame());
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
					((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.MIMETYPE, MimeTypes.getContentType(content, f.getName()));
				}
			}
			catch (Exception e)
			{
				if (application instanceof ISmartClientApplication)
				{
					((ISmartClientApplication)application).reportError(this, application.getI18NMessage("servoy.imageMedia.error.loading"), e);
				}
				else
				{
					application.reportError(application.getI18NMessage("servoy.imageMedia.error.loading"), e);
				}
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
				fc.suggestFileName("image.jpg");
			}
			else if (array.length > 3 && array[0] == 0x47 && array[1] == 0x49 && array[2] == 0x46)//gif
			{
				fc.suggestFileName("image.gif");
			}
			else
			{
				fc.suggestFileName("filename.unknown");
			}

			int returnVal = fc.showSaveDialog(((ISmartClientApplication)application).getMainApplicationFrame());
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
					if (application instanceof ISmartClientApplication)
					{
						((ISmartClientApplication)application).reportError(this, application.getI18NMessage("servoy.imageMedia.error.loading"), e);
					}
					else
					{
						application.reportError(application.getI18NMessage("servoy.imageMedia.error.loading"), e);
					}
				}
			}
		}
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

	public Insets getMargin()
	{
		return null;
	}

	public void requestFocusToComponent()
	{
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
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
							((DataAdapterList)resolver).setValueObject(dataProviderID + IMediaFieldConstants.MIMETYPE,
								MimeTypes.getContentType(content, file.getName()));
						}
					}
					catch (Exception e)
					{
						if (application instanceof ISmartClientApplication)
						{
							((ISmartClientApplication)application).reportError(this, application.getI18NMessage("servoy.imageMedia.error.loading"), e);
						}
						else
						{
							application.reportError(application.getI18NMessage("servoy.imageMedia.error.loading"), e);
						}
					}
				}
				dropTargetDropEvent.getDropTargetContext().dropComplete(true);
			}
			else
			{
				Debug.trace("Rejected");
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

	private Point cachedLocation;

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	private Dimension cachedSize;

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	public void setCachedLocation(Point location)
	{
		this.cachedLocation = location;
	}

	public void setCachedSize(Dimension size)
	{
		this.cachedSize = size;
	}

	public int getAbsoluteFormLocationY()
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

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	private String tooltip;

	@Override
	public void setToolTipText(String tip)
	{
		if (tip != null && tip.indexOf("%%") != -1)
		{
			tooltip = tip;
		}
		else if (!Utils.stringIsEmpty(tip))
		{
			if (!Utils.stringContainsIgnoreCase(tip, "<html") || HtmlUtils.hasUsefulHtmlContent(tip))
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


	private boolean editState;

	public void setReadOnly(boolean b)
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
		return popup == null;
	}


	public boolean isEditable()
	{
		return !isReadOnly();
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = labels.get(i);
					label.setComponentEnabled(b);
				}
			}
		}
	}

	private boolean accessible = true;
	private ArrayList<ILabel> labels;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	private boolean viewable = true;

	public void setViewable(boolean b)
	{
		if (!b) setComponentVisible(b);
		this.viewable = b;
	}

	public boolean isViewable()
	{
		return viewable;
	}

	@Override
	public void setOpaque(boolean b)
	{
		if (enclosedComponent != null) enclosedComponent.setOpaque(b);
		getViewport().setOpaque(b);
		super.setOpaque(b);
	}


	public void setComponentVisible(boolean b_visible)
	{
		if (viewable || !b_visible)
		{
			setVisible(b_visible);
		}
	}


	@Override
	public void setVisible(boolean flag)
	{
		super.setVisible(flag);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
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
		return (String)getClientProperty("Id");
	}

	@Override
	public void setTransferHandler(TransferHandler newHandler)
	{
		super.setTransferHandler(newHandler);
		if (newHandler == null) setDropTarget(null);
	}
}
