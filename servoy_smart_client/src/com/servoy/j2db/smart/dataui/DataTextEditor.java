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


import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.TransferHandler;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.View;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ISupportAsyncLoading;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dnd.FormDataTransferHandler;
import com.servoy.j2db.dnd.ISupportDragNDropTextTransfer;
import com.servoy.j2db.printing.IFixedPreferredWidth;
import com.servoy.j2db.smart.MainPanel;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.smart.SwingRuntimeWindow;
import com.servoy.j2db.smart.TextToolbar;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IDisplayTagText;
import com.servoy.j2db.ui.IEditProvider;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportEditProvider;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.ui.scripting.AbstractRuntimeTextEditor;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.FixedHTMLEditorKit;
import com.servoy.j2db.util.gui.FixedJEditorPane;
import com.servoy.j2db.util.rtf.FixedRTFEditorKit;

/**
 * A rich text editor component
 * 
 * @author jblok
 */
public class DataTextEditor extends EnableScrollPanel implements IDisplayData, IDisplayTagText, IFieldComponent, IScrollPane, ISupportAsyncLoading,
	IFixedPreferredWidth, ISupportCachedLocationAndSize, ISupplyFocusChildren, ISupportDragNDropTextTransfer, ISupportEditProvider, ISupportOnRender
{
	private final FixedJEditorPane enclosedComponent;
	private String dataProviderID;
	private String tagText;
	private EditorKit editorKit;
	private final EditorKit plainEditorKit;

	private Document editorDocument;
	private final Document plainEditorDocument;

	private final IApplication application;
	private final EventExecutor eventExecutor;
	private MouseAdapter rightclickMouseAdapter = null;
	private SwingRuntimeWindow parentWindow;
	private final AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable;

	public DataTextEditor(IApplication app, AbstractRuntimeTextEditor<IFieldComponent, JEditorPane> scriptable, int type)
	{
		super();
		application = app;
		getViewport().setView(new MyEditorPane(app.getScheduledExecutor()));
		enclosedComponent = (FixedJEditorPane)getViewport().getView();
		eventExecutor = new EventExecutor(this, enclosedComponent)
		{
			@Override
			public void fireLeaveCommands(Object display, boolean focusEvent, int modifiers)
			{
				if (hasLeaveCmds())
				{
					editProvider.focusLost(new FocusEvent(DataTextEditor.this, FocusEvent.FOCUS_LOST));
				}

				super.fireLeaveCommands(display, focusEvent, modifiers);
			}
		};
		enclosedComponent.addKeyListener(eventExecutor);
		this.scriptable = scriptable;
		scriptable.setTextComponent(enclosedComponent);
		plainEditorKit = enclosedComponent.getEditorKit();
		plainEditorDocument = getDocument();

//		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//		setContentType("text/rtf" );
		if (type == ComponentFactory.RTF_AREA)
		{
			editorKit = new FixedRTFEditorKit();
		}
		else
		{
			editorKit = new FixedHTMLEditorKit(app);
			enclosedComponent.setDocument(editorKit.createDefaultDocument());
		}
		enclosedComponent.setBorder(BorderFactory.createEmptyBorder());
		enclosedComponent.setEditorKit(editorKit);
		editorDocument = getDocument();

		prepareForTextToolbarHandling();

		enclosedComponent.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !Utils.equalObjects(previousValue, getValueObject()))
				{
					e.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && previousValue != null && !Utils.equalObjects(previousValue, getValueObject()))
				{
					StringReader sr = new StringReader((String)previousValue);
					try
					{
						enclosedComponent.getDocument().remove(0, enclosedComponent.getDocument().getLength());
						editorKit.read(sr, enclosedComponent.getDocument(), 0);
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
					finally
					{
						e.consume();
					}
				}
			}
		});
//		Action[] actions = enclosedComponent.getActions();
//		for (int i = 0; i < actions.length; i++)
//		{
//			Debug.trace("Found action "+actions[i]+" name "+actions[i].getValue(Action.NAME));
//		}
	}

	public final AbstractRuntimeField getScriptObject()
	{
		return scriptable;
	}

	private void prepareForTextToolbarHandling()
	{
		addFocusListener(new FocusListener()
		{
			public void focusLost(FocusEvent e)
			{
				setTextToolBarComponent(null);
			}

			public void focusGained(FocusEvent e)
			{
				if (eventExecutor.getValidationEnabled())
				{
					setTextToolBarComponent(enclosedComponent);
				} // else find mode...
			}
		});

		addHierarchyListener(new HierarchyListener()
		{
			public void hierarchyChanged(HierarchyEvent e)
			{
				if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) == HierarchyEvent.PARENT_CHANGED)
				{
					// get the new text toolbar to be used
					Container parent = getParent();
					while (parent != null && !(parent instanceof MainPanel))
					{
						parent = parent.getParent();
					}
					if (parent instanceof MainPanel)
					{
						parentWindow = (SwingRuntimeWindow)application.getRuntimeWindowManager().getWindow(((MainPanel)parent).getContainerName());
					}
					else
					{
						parentWindow = null;
					}
				}
			}
		});
	}

	private void setTextToolBarComponent(Component comp)
	{
		TextToolbar textToolbar = parentWindow != null ? parentWindow.getTextToolbar() : null;
		if (textToolbar != null)
		{
			if (comp instanceof JEditorPane && comp.isEnabled())
			{
				textToolbar.setTextComponent((JEditorPane)comp);
				textToolbar.setEnabled(true);
				textToolbar.setVisible(true);
			}
			else
			{
				textToolbar.setTextComponent(null);
				textToolbar.setEnabled(false);
				textToolbar.setVisible(true);
			}
		}
	}

	/*
	 * _____________________________________________________________ Methods for event handling
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
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

		previousValue = (newVal == null ? null : newVal.toString());

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
						eventExecutor.fireRightclickCommand(true, DataTextEditor.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			enclosedComponent.addMouseListener(rightclickMouseAdapter);
			addMouseListener(rightclickMouseAdapter);
		}
	}

	private boolean wasEditable;

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;
		if (dataProviderID != null && ScopesUtils.isVariableScope(dataProviderID)) return;

		eventExecutor.setValidationEnabled(b);

		boolean prevEditState = editState;
		if (b)
		{
			setEditable(wasEditable);
			enclosedComponent.setEditorKit(editorKit);
			enclosedComponent.setDocument(editorDocument);
			previousValue = getValueObject();
			try
			{
				plainEditorDocument.remove(0, plainEditorDocument.getLength());
			}
			catch (BadLocationException e)
			{
				Debug.error(e);
			}
		}
		else
		{
			wasEditable = enclosedComponent.isEditable();
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
			enclosedComponent.setEditorKit(plainEditorKit);
			enclosedComponent.setDocument(plainEditorDocument);
		}
		editState = prevEditState;
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	@Override
	public void setFont(Font f)
	{
		if (enclosedComponent != null)
		{
			enclosedComponent.setFont(f);
		}
	}

	@Override
	public Font getFont()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getFont();
		}
		return super.getFont();
	}

	public void setHorizontalAlignment(int a)
	{
		//ignore
	}

	public void setMaxLength(int i)
	{
		//ignore
	}

	@Override
	public void setName(String name)
	{
		super.setName(name);
		enclosedComponent.setName(name);
	}

	public void setMargin(Insets m)
	{
//		enclosedComponent.setMargin(i); seems to have no effect
		enclosedComponent.setBorder(BorderFactory.createCompoundBorder(enclosedComponent.getBorder(),
			BorderFactory.createEmptyBorder(m.top, m.left, m.bottom, m.right)));
	}

	public Insets getMargin()
	{
		return null;
	}

	public Document getDocument()
	{
		return enclosedComponent.getDocument();
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { enclosedComponent };
	}

	private class MyEditorPane extends FixedJEditorPane implements ISkinnable, ISupportDragNDropTextTransfer
	{
		MyEditorPane(Executor exe)
		{
			super(exe);
			setDragEnabledEx(true);
//			getDocument().putProperty("IgnoreCharsetDirective", Boolean.valueOf(true));
			/*
			 * Key for a client property used to indicate whether the default font and foreground color from the component are used if a font or foreground
			 * color is not specified in the styled text
			 */
			putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		}

		private void setDragEnabledEx(boolean b)
		{
			try
			{
				Method m = getClass().getMethod("setDragEnabled", new Class[] { boolean.class }); //$NON-NLS-1$
				m.invoke(this, new Object[] { new Boolean(b) });
			}
			catch (Exception e)
			{
//				Debug.trace(e);//is intenionaly trace, becouse fails before 1.4			
			}
		}

		/**
		 * Fix for bad font rendering (bad kerning == strange spacing) in java 1.5 see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097047
		 */
		@Override
		public FontMetrics getFontMetrics(Font font)
		{
			if (application != null)//getFontMetrics can be called in the constructor super call before application is assigned
			{
				boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
				if (isPrinting)
				{
					Graphics g = (Graphics)application.getRuntimeProperties().get("printGraphics"); //$NON-NLS-1$
					if (g != null)
					{
						return g.getFontMetrics(font);
					}
				}
			}
			return super.getFontMetrics(font);
		}

		// MAC FIX
		@Override
		public Insets getInsets()
		{
			Insets insets = super.getInsets();
			if (insets == null)
			{
				insets = new Insets(0, 0, 0, 0);
			}
			return insets;
		}

		// MAC FIX
		@Override
		public Insets getMargin()
		{
			Insets insets = super.getMargin();
			if (insets == null)
			{
				insets = new Insets(0, 0, 0, 0);
			}
			return insets;
		}

		@Override
		public void transferFocus()
		{
			DataTextEditor.this.transferFocus();
		}

		@Override
		public void setUI(ComponentUI ui)
		{
			super.setUI(ui);
		}

		@Override
		public void setText(String t)
		{
			super.setText(t);
			setCaretPosition(0);
		}

		//method to overcome exception by sun system thrown during printing
		@Override
		public Dimension getPreferredSize()
		{
			try
			{
				return super.getPreferredSize();
			}
			catch (RuntimeException ex)
			{
				Debug.error("Invalid HTML for " + dataProviderID + " html: " + previousValue + "\n" + getText()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				//throw new IllegalArgumentException("Invalid HTML for "+ dataProviderID);
			}
			return getSize();
		}

		//method to overcome exception by sun system thrown during printing
		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			try
			{
				return (application.getModeManager().getMode() == IModeManager.PREVIEW_MODE) ? true : super.getScrollableTracksViewportWidth();
			}
			catch (RuntimeException ex)
			{
				Debug.error("Invalid HTML for " + dataProviderID + " html: " + previousValue + "\n" + getText()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				//throw new IllegalArgumentException("Invalid HTML for "+ dataProviderID);
			}
			return false;
		}

		//method to overcome exception by sun system thrown during printing
		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			try
			{
				return (application.getModeManager().getMode() == IModeManager.PREVIEW_MODE) ? true : super.getScrollableTracksViewportHeight();
			}
			catch (RuntimeException ex)
			{
				Debug.error("Invalid HTML for " + dataProviderID + " html: " + previousValue + "\n" + getText()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				//	throw new IllegalArgumentException("Invalid HTML for "+ dataProviderID);
			}
			return false;
		}

		//fix for incorrect clipping
		@Override
		public void print(Graphics g)
		{
			Shape saveClip = g.getClip();
			try
			{
				int w = DataTextEditor.this.getWidth();
				w -= DataTextEditor.this.getInsets().left + DataTextEditor.this.getInsets().right;
				int h = DataTextEditor.this.getHeight();
				h -= DataTextEditor.this.getInsets().top + DataTextEditor.this.getInsets().bottom;
				if (saveClip != null)
				{
					g.setClip(saveClip.getBounds().intersection(new Rectangle(0, 0, w, h)));
				}
				else
				{
					g.setClip(0, 0, w, h);
				}
				super.print(g);
			}
			finally
			{
				g.setClip(saveClip);
			}
		}

		@Override
		public void paint(Graphics g)
		{
			try
			{
				super.paint(g);
			}
			catch (RuntimeException re)
			{
				Debug.error("Error in painting HTML/RTF Area, check your html: " + getText(), re); //$NON-NLS-1$
			}

			//if you want to see all the view enable the following code DO NOT DELETE		
			if (false)
			{
				View v = this.getUI().getRootView(this);
				walkView(g, v, getBounds());
			}
		}

		// Recursively walks a view hierarchy
		private void walkView(Graphics g, View view, Rectangle allocation)
		{
			// Get number of children views
			int n = view.getViewCount();

			// Visit the children of this view
			for (int i = 0; i < n; i++)
			{
				View kid = view.getView(i);

				java.awt.Shape kidshape = view.getChildAllocation(i, allocation);
				if (kidshape == null) continue;
				Rectangle kidbox = kidshape.getBounds();
				g.drawRect(kidbox.x, kidbox.y, kidbox.width, kidbox.height);

				walkView(g, kid, kidbox);
			}
		}

		@Override
		public void copy()
		{
			if (textTransferHandler != null)
			{
				Action copyAction = FormDataTransferHandler.getCopyFormDataAction();
				copyAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)copyAction.getValue(Action.NAME),
					EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
			}
			else super.copy();
		}

		@Override
		public void cut()
		{
			if (textTransferHandler != null && isEditable() && isEnabled())
			{
				Action cutAction = FormDataTransferHandler.getCutFormDataAction();
				cutAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)cutAction.getValue(Action.NAME),
					EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
			}
			else super.cut();
		}

		@Override
		public void paste()
		{
			if (textTransferHandler != null && isEditable() && isEnabled())
			{
				Action pasteAction = FormDataTransferHandler.getPasteFormDataAction();
				pasteAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)pasteAction.getValue(Action.NAME),
					EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
			}
			else super.paste();
		}

		private int getCurrentEventModifiers()
		{
			int modifiers = 0;
			AWTEvent currentEvent = EventQueue.getCurrentEvent();
			if (currentEvent instanceof InputEvent)
			{
				modifiers = ((InputEvent)currentEvent).getModifiers();
			}
			else if (currentEvent instanceof ActionEvent)
			{
				modifiers = ((ActionEvent)currentEvent).getModifiers();
			}
			return modifiers;
		}

		private TransferHandler textTransferHandler;

		/*
		 * @see com.servoy.j2db.dnd.ISupportDragNDropTextTransfer#clearTransferHandler()
		 */
		public void clearTransferHandler()
		{
			textTransferHandler = getTransferHandler();
			setTransferHandler(null);
		}

		/*
		 * @see com.servoy.j2db.dnd.ISupportDragNDropTextTransfer#getTextTransferHandler()
		 */
		public TransferHandler getTextTransferHandler()
		{
			return textTransferHandler;
		}
	}

	public JEditorPane getRealEditor()
	{
		return enclosedComponent;
	}

	public boolean needEditListener()
	{
		return true;
	}

	private EditProvider editProvider = null;

	public IEditProvider getEditProvider()
	{
		return editProvider;
	}

	public void addEditListener(IEditListener l)
	{
		if (editProvider == null)
		{
			editProvider = new EditProvider(this, true);
			addFocusListener(editProvider);
			editorDocument.addDocumentListener(editProvider);
			plainEditorDocument.addDocumentListener(editProvider);
			enclosedComponent.addPropertyChangeListener("document", new PropertyChangeListener()
			{

				public void propertyChange(PropertyChangeEvent evt)
				{
					if (Utils.stringSafeEquals("document", evt.getPropertyName())) //$NON-NLS-1$
					{
						editorDocument.removeDocumentListener(editProvider);
						editorDocument = (Document)evt.getNewValue();
						editProvider.resetState();
						editorDocument.addDocumentListener(editProvider);
					}
				}
			});
//			addPropertyChangeListener("text", editProvider);
			editProvider.addEditListener(l);
			editProvider.setEditable(enclosedComponent.isEditable());
			try
			{
				DropTarget dt = enclosedComponent.getDropTarget();
				if (dt != null) dt.addDropTargetListener(editProvider);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	public void setEditable(boolean b)
	{
		editState = b;
		enclosedComponent.setEditable(b);
		if (!b && editorKit instanceof FixedHTMLEditorKit && linkListener == null)
		{
			linkListener = new LinkListener();
			enclosedComponent.addHyperlinkListener(linkListener);
		}
		if (editProvider != null) editProvider.setEditable(b);

		enclosedComponent.removeMouseListener(eventExecutor);
		if (!b)
		{
			enclosedComponent.addMouseListener(eventExecutor);//listen when not editable
		}
	}

	public boolean isEditable()
	{
		return enclosedComponent.isEditable();
	}

	private LinkListener linkListener;

	class LinkListener implements HyperlinkListener
	{
		public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent)
		{
			if (!DataTextEditor.this.isEnabled()) return;
			HyperlinkEvent.EventType type = hyperlinkEvent.getEventType();
			if (type == HyperlinkEvent.EventType.ACTIVATED)
			{
				try
				{
					String description = hyperlinkEvent.getDescription();
					if (description != null && description.toLowerCase().startsWith("javascript:")) //$NON-NLS-1$
					{
						String script = description;
						if (script.length() > 13)
						{
							String scriptName = script.substring(11);

							Container parent = getParent();
							while (parent != null && !(parent instanceof SwingForm))
							{
								parent = parent.getParent();
							}
							if (parent instanceof SwingForm)
							{
								((SwingForm)parent).getController().eval(scriptName);
							}
							else
							{
								((FormController)application.getFormManager().getCurrentForm()).eval(scriptName);
							}
						}
					}

					URL url = hyperlinkEvent.getURL();
					if (url == null || "javascript".equals(url.getProtocol())) return;
					if (hyperlinkEvent instanceof HTMLFrameHyperlinkEvent)
					{
						HTMLFrameHyperlinkEvent link = (HTMLFrameHyperlinkEvent)hyperlinkEvent;
						if ("_blank".equalsIgnoreCase(link.getTarget())) //$NON-NLS-1$
						{
							application.showURL(url.toString(), null, null, 0, true);
							return;
						}
					}
					else
					{
						try
						{
							enclosedComponent.setPage(url);
						}
						catch (Exception e)
						{
							Debug.trace("exception setting page as url in DataTextEditor, url: " + url); //$NON-NLS-1$
						}
					}
				}
				catch (Exception e)
				{
					if (e instanceof ExitScriptException)
					{
						// ignore
						return;
					}
					Debug.error(e);
				}
			}
			else if (type == HyperlinkEvent.EventType.ENTERED)
			{
				enclosedComponent.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
//			else if (hyperlinkEvent$Event2 != HyperlinkEvent.EventType.EXITED)
//			{
//			}
			else
			{
				enclosedComponent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

	@Override
	public void addFocusListener(FocusListener fl)
	{
		if (enclosedComponent != null) enclosedComponent.addFocusListener(fl);
	}

	@Override
	public void removeFocusListener(FocusListener fl)
	{
		if (enclosedComponent != null) enclosedComponent.removeFocusListener(fl);
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String id)
	{
		dataProviderID = id;
	}

	private Object previousValue;

	private void setValueThreadSafe(final Object value)
	{
		try
		{
			Runnable runnable = new Runnable()
			{
				public void run()
				{
					try
					{
						setValueEx(value);
					}
					catch (Exception e)
					{
						Debug.error("error setting a vallue in the html editor:  " + value, e); //$NON-NLS-1$
						if (editorKit instanceof FixedHTMLEditorKit)
						{
							Debug.error("creating a new document on the html editor kit and setting the value again"); //$NON-NLS-1$
							enclosedComponent.setDocument(editorKit.createDefaultDocument());
							setValueEx(value);
						}
					}
				}
			};
			if (application.isEventDispatchThread())
			{
				// if on event thread make sure next call to getValueObject() gets this value
				runnable.run();
			}
			else
			{
				// do not block when called from another thread, may cause deadlock when ui thread is busy
				application.invokeLater(runnable);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private void setValueEx(Object value)
	{
		try
		{
			if (value == null)
			{
				String prevValue = (String)getValueObject();
				if (prevValue != null && prevValue.toLowerCase().indexOf("<html") > -1) //$NON-NLS-1$
				{
					value = "<html><body></body></html>"; //$NON-NLS-1$
				}
			}
			if (editProvider != null) editProvider.setAdjusting(true);
			if (!Utils.equalObjects(previousValue, value))
			{
				previousValue = value;
				if (value != null)
				{
					int selStart = enclosedComponent.getSelectionStart();
					int selEnd = enclosedComponent.getSelectionEnd();
					String svalue = value.toString();
					String lowercaseValue = svalue.toLowerCase();
					if (lowercaseValue.indexOf("rtf") != -1 || lowercaseValue.indexOf("<html") != -1) //$NON-NLS-1$ //$NON-NLS-2$
					{
						if (svalue.length() > 10000 && application.getModeManager().getMode() == IModeManager.EDIT_MODE && isAsyncLoading() &&
							lowercaseValue.indexOf("<html") != -1)
						{
							ByteArrayInputStream bais = new ByteArrayInputStream(svalue.getBytes("UTF-8")); //$NON-NLS-1$
							enclosedComponent.read(bais, null);
						}
						else
						{
							cancelLoadAndClearDocument();
							enclosedComponent.putClientProperty(FixedJEditorPane.CHARSET_DIRECTIVE, "UTF-8");
							enclosedComponent.getDocument().putProperty("IgnoreCharsetDirective", new Boolean(true));
							StringReader sr = new StringReader(svalue);
							editorKit.read(sr, enclosedComponent.getDocument(), 0);


						}
						// now go over all the generated components and try to add 
						// action listeners to the buttons (checkbox,radio)
						// so that clicks on that also result in the onaction of this htmlarea
						for (Component c : enclosedComponent.getComponents())
						{
							Container parent = (Container)c;
							if (parent.getComponents().length == 1 && parent.getComponents()[0] instanceof AbstractButton)
							{
								((AbstractButton)parent.getComponents()[0]).addActionListener(new ActionListener()
								{
									public void actionPerformed(ActionEvent e)
									{
										eventExecutor.actionPerformed(e.getModifiers());
									}
								});
							}
						}
					}
					else
					{
						cancelLoadAndClearDocument();
						enclosedComponent.getDocument().insertString(0, svalue, null);
					}
					if (selStart <= enclosedComponent.getDocument().getLength() && selEnd <= enclosedComponent.getDocument().getLength()) enclosedComponent.select(
						selStart, selEnd);
					else enclosedComponent.setCaretPosition(0);
				}
				else
				{
					cancelLoadAndClearDocument();
				}

				if (enclosedComponent.getWidth() == 0)
				{
					enclosedComponent.setSize(getSize());
					doLayout();
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			enclosedComponent.setText("<html><body></body></html>");
		}
		finally
		{
			if (editProvider != null) editProvider.setAdjusting(false);
		}

	}

	private void cancelLoadAndClearDocument()
	{
		// cancel an async load that could have been done the previous time
		enclosedComponent.cancelASyncLoad();
		// always just create a new document, asyn load will also do that.
		enclosedComponent.setDocument(editorKit.createDefaultDocument());
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	private boolean needEntireState;
	private ArrayList<ILabel> labels;

	public void setNeedEntireState(boolean b)
	{
		needEntireState = b;
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValueObject(Object obj)
	{
		if (needEntireState)
		{
			if (resolver != null)
			{
				if (dataProviderID == null && !isEditable())
				{
					setValueThreadSafe(Text.processTags(tagText, resolver));
				}
				else
				{
					if (obj != null)
					{
						String val = Text.processTags(resolver.getStringValue(dataProviderID), resolver);
						setValueThreadSafe(val);
					}
					else
					{
						setValueThreadSafe(null);
					}
				}
				if (tooltip != null)
				{
					enclosedComponent.setToolTipText(Text.processTags(tooltip, resolver));
				}
			}
			else
			{
				setValueThreadSafe(null);
				if (tooltip != null)
				{
					enclosedComponent.setToolTipText(null);
				}
			}
		}
		else
		{
			setValueThreadSafe(obj);
			if (tooltip != null)
			{
				enclosedComponent.setToolTipText(tooltip);
			}
		}
		fireOnRender(false);
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable != null)
		{
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(enclosedComponent.hasFocus());
		}
	}

	public Object getValueObject()
	{
		if (eventExecutor.getValidationEnabled())
		{
			try
			{
				if (editorKit instanceof FixedRTFEditorKit)
				{
					// RTF area contents always use ISO Latin-1; we use the byte array because write(writer,..) throws exception
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					editorKit.write(os, enclosedComponent.getDocument(), 0, enclosedComponent.getDocument().getLength());
					return os.toString("ISO-8859-1");
				}
				else
				{
					// HTML area can use any charset - so we get contents using a writer, to avoid charset problems
					StringWriter sw = new StringWriter();
					editorKit.write(sw, enclosedComponent.getDocument(), 0, enclosedComponent.getDocument().getLength());
					return sw.toString();
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				return ""; //$NON-NLS-1$
			}
		}
		return enclosedComponent.getText();
	}


	@Override
	public void requestFocus()
	{
		if (enclosedComponent != null) enclosedComponent.requestFocus();
	}

	@Override
	public boolean requestFocus(boolean temporary)
	{
		if (enclosedComponent != null) return enclosedComponent.requestFocus(temporary);
		else return super.requestFocus(temporary);
	}


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
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	@Override
	public void setOpaque(boolean b)
	{
		if (enclosedComponent != null) enclosedComponent.setOpaque(b);
		getViewport().setOpaque(b);
		super.setOpaque(b);
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

	/*
	 * readonly---------------------------------------------------
	 */

	public boolean isReadOnly()
	{
		return !enclosedComponent.isEditable();
	}

	private boolean editState;

	public void setReadOnly(boolean b)
	{
		if (b && !enclosedComponent.isEditable()) return;
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

	/*
	 * tooltip---------------------------------------------------
	 */
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
			if (!Utils.stringContainsIgnoreCase(tip, "<html")) //$NON-NLS-1$
			{
				enclosedComponent.setToolTipText(tip);
			}
			else if (HtmlUtils.hasUsefulHtmlContent(tip))
			{
				enclosedComponent.setToolTipText(tip);
			}
		}
		else
		{
			enclosedComponent.setToolTipText(null);
		}
	}

	@Override
	public String getToolTipText(MouseEvent event)
	{
		String txt = super.getToolTipText(event);
		if (txt == null || txt.length() == 0)
		{
			return enclosedComponent.getToolTipText(event);
		}
		return null;
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

	private Point cachedLocation;

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	public void setCachedLocation(Point location)
	{
		this.cachedLocation = location;
	}

	public void setCachedSize(Dimension size)
	{
		this.cachedSize = size;
	}

	private Dimension cachedSize;

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	// If component not shown or not added yet 
	// and request focus is called it should wait for the component
	// to be created.
	boolean wantFocus = false;

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (wantFocus)
		{
			wantFocus = false;
			enclosedComponent.requestFocus();
		}
	}

	public void requestFocusToComponent()
	{
//		if (!enclosedComponent.hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (isDisplayable())
			{
				// Must do it in a runnable or else others after a script can get focus first again..
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						enclosedComponent.requestFocus();
					}
				});
			}
			else
			{
				wantFocus = true;
			}
		}
	}


	@Override
	public String toString()
	{
		return scriptable.toString();
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (editProvider != null) editProvider.forceCommit();

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

		if (looseFocus && eventExecutor.mustFireFocusLostCommand())
		{
			eventExecutor.skipNextFocusLost();
			eventExecutor.fireLeaveCommands(this, false, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
		return true;
	}

	boolean asyncLoading = true;

	public void setAsyncLoadingEnabled(boolean b)
	{
		if (editorKit instanceof ISupportAsyncLoading)
		{
			((ISupportAsyncLoading)editorKit).setAsyncLoadingEnabled(b);
		}
		asyncLoading = b;
	}

	private boolean isAsyncLoading()
	{
		return asyncLoading && !Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}

	private int preferredWidth = -1;

	public void setPreferredWidth(int preferredWidth)
	{
		this.preferredWidth = preferredWidth;
	}

	@Override
	public Dimension getPreferredSize()
	{
		if (preferredWidth < 0)
		{
			return super.getPreferredSize();
		}
		else
		{
			Dimension preferredSize = new Dimension();
			preferredSize.width = preferredWidth;

			// calculate preferred height based on the given width
			Insets insets = getInsets();
			Dimension oldSize = enclosedComponent.getSize();
			enclosedComponent.setSize(preferredWidth - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
			preferredSize.height = super.getPreferredSize().height;
			enclosedComponent.setSize(oldSize);

			return preferredSize;
		}
	}

	/*
	 * @see com.servoy.j2db.dnd.ISupportTextTransfer#clearTransferHandler()
	 */
	public void clearTransferHandler()
	{
		setTransferHandler(null);
		if (enclosedComponent instanceof ISupportDragNDropTextTransfer) ((ISupportDragNDropTextTransfer)enclosedComponent).clearTransferHandler();
	}

	/*
	 * @see com.servoy.j2db.dnd.ISupportTextTransfer#getTextTransferHandler()
	 */
	public TransferHandler getTextTransferHandler()
	{
		return enclosedComponent instanceof ISupportDragNDropTextTransfer ? ((ISupportDragNDropTextTransfer)enclosedComponent).getTextTransferHandler() : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IDisplayTagText#setTagText(java.lang.String)
	 */
	public void setTagText(String tagText)
	{
		this.tagText = tagText;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IDisplayTagText#getTagText()
	 */
	public String getTagText()
	{
		return tagText;
	}
}