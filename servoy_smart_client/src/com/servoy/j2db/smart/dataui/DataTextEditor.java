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
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.RootPaneContainer;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.text.html.StyleSheet;

import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormManagerInternal;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ISupportAsyncLoading;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.printing.IFixedPreferredWidth;
import com.servoy.j2db.smart.MainPanel;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.smart.TextToolbar;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptTextEditorMethods;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.FixedHTMLEditorKit;
import com.servoy.j2db.util.FixedJEditorPane;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.rtf.FixedRTFEditorKit;

/**
 * A rich text editor component
 * 
 * @author jblok
 */
public class DataTextEditor extends EnableScrollPanel implements IDisplayData, IFieldComponent, IScrollPane, ISupportAsyncLoading, IScriptTextEditorMethods,
	IFixedPreferredWidth, ISupportCachedLocationAndSize, ISupplyFocusChildren
{
	private static final String APP_PANEL_NAME = "main_AppliCaTiON__Frame";

	private final FixedJEditorPane enclosedComponent;
	private String dataProviderID;
	private EditorKit editorKit;
	private final EditorKit plainEditorKit;

	private Document editorDocument;
	private final Document plainEditorDocument;

	private final IApplication application;
	private final EventExecutor eventExecutor;
	private MouseAdapter rightclickMouseAdapter = null;
	private String mainPanelName;

	public DataTextEditor(IApplication app, int type)
	{
		super();
		application = app;
		getViewport().setView(new MyEditorPane(app.getScheduledExecutor()));
		enclosedComponent = (FixedJEditorPane)getViewport().getView();
		eventExecutor = new EventExecutor(this, enclosedComponent);
		enclosedComponent.addKeyListener(eventExecutor);

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
//		Action[] actions = enclosedComponent.getActions();
//		for (int i = 0; i < actions.length; i++)
//		{
//			Debug.trace("Found action "+actions[i]+" name "+actions[i].getValue(Action.NAME));
//		}
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
						mainPanelName = ((MainPanel)parent).getContainerName();
						if (mainPanelName == null) mainPanelName = APP_PANEL_NAME;
					}
					else
					{
						mainPanelName = null;
					}
				}
			}
		});
	}

	private void setTextToolBarComponent(Component comp)
	{
		TextToolbar textToolbar = getTextToolbar();
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

	private TextToolbar getTextToolbar()
	{
		TextToolbar t = null;
		if (mainPanelName != null)
		{
			if (APP_PANEL_NAME.equals(mainPanelName))
			{
				t = (TextToolbar)application.getToolbarPanel().getToolBar("text"); //$NON-NLS-1$
			}
			else
			{
				Window w = application.getWindow(IFormManagerInternal.USER_WINDOW_PREFIX + mainPanelName);
				if (w instanceof RootPaneContainer)
				{
					t = findTextToolbar((RootPaneContainer)w);
				}
			}
		}
		return t;
	}

	private TextToolbar findTextToolbar(RootPaneContainer w)
	{
		for (Component c : w.getContentPane().getComponents())
		{
			if (c instanceof TextToolbar) return (TextToolbar)c;
		}
		return null;
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
		if (dataProviderID != null && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;

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
			setEditable(true);//allow search
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

	@Override
	public void setFont(Font f)
	{
		if (enclosedComponent != null)
		{
			enclosedComponent.setFont(f);
			if (f != null)
			{
				Style s = null;
				DefaultStyledDocument doc = (DefaultStyledDocument)enclosedComponent.getDocument();
				if (doc instanceof HTMLDocument)
				{
					StyleSheet[] sheets = ((HTMLDocument)doc).getStyleSheet().getStyleSheets();
					for (StyleSheet element : sheets)
					{
						s = element.getStyle("body"); //$NON-NLS-1$
						if (s != null) break;
					}
				}
				else
				{
					s = doc.getStyle("default"); //$NON-NLS-1$
				}
				if (s != null)
				{
					int style = f.getStyle();
					StyleConstants.setBold(s, ((style & Font.BOLD) == Font.BOLD));
					StyleConstants.setItalic(s, ((style & Font.ITALIC) == Font.ITALIC));
					StyleConstants.setFontFamily(s, f.getFamily());
					StyleConstants.setFontSize(s, f.getSize());
				}
			}
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

	public Document getDocument()
	{
		return enclosedComponent.getDocument();
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { enclosedComponent };
	}

	private class MyEditorPane extends FixedJEditorPane implements ISkinnable
	{
		MyEditorPane(Executor exe)
		{
			super(exe);
			setDragEnabledEx(true);
//			getDocument().putProperty("IgnoreCharsetDirective", Boolean.valueOf(true));			
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
	}

	public JEditorPane getRealEditor()
	{
		return enclosedComponent;
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
							application.showURL(url.toString(), null, null, 0);
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
			application.invokeAndWait(new Runnable()
			{
				public void run()
				{
					setValueEx(value);
				}
			});
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
						enclosedComponent.getDocument().remove(0, enclosedComponent.getDocument().getLength());
						if (svalue.length() > 10000 && application.getModeManager().getMode() == IModeManager.EDIT_MODE && isAsyncLoading() &&
							lowercaseValue.indexOf("<html") != -1)
						{
							ByteArrayInputStream bais = new ByteArrayInputStream(svalue.getBytes("UTF-8")); //$NON-NLS-1$
							enclosedComponent.read(bais, null);
						}
						else
						{
							enclosedComponent.putClientProperty(FixedJEditorPane.CHARSET_DIRECTIVE, "UTF-8");
							enclosedComponent.getDocument().putProperty("IgnoreCharsetDirective", new Boolean(true));
							StringReader sr = new StringReader(svalue);
							editorKit.read(sr, enclosedComponent.getDocument(), 0);
						}
					}
					else
					{
						enclosedComponent.getDocument().remove(0, enclosedComponent.getDocument().getLength());
						enclosedComponent.getDocument().insertString(0, svalue, null);
					}
					if (selStart <= enclosedComponent.getDocument().getLength() && selEnd <= enclosedComponent.getDocument().getLength()) enclosedComponent.select(
						selStart, selEnd);
					else enclosedComponent.setCaretPosition(0);
				}
				else
				{
					enclosedComponent.setText("");
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
				if (obj != null)
				{
					String val = Text.processTags(resolver.getStringValue(dataProviderID), resolver);
					setValueThreadSafe(val);
				}
				else
				{
					setValueThreadSafe(null);
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


	/*
	 * caret---------------------------------------------------
	 */
	public int js_getCaretPosition()
	{
		if (enclosedComponent == null) return 0;
		return enclosedComponent.getCaretPosition();
	}

	public void js_setCaretPosition(int pos)
	{
		if (enclosedComponent == null) return;
		if (pos < 0) pos = 0;
		if (pos > getDocument().getLength()) pos = getDocument().getLength();
		enclosedComponent.setCaretPosition(pos);
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}

	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(enclosedComponent.getBackground());
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


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
	}

	/*
	 * url---------------------------------------------------
	 */
	public void js_setURL(String url)
	{
		if (editorKit instanceof FixedHTMLEditorKit)
		{
			try
			{
				enclosedComponent.setPage(url);
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
	}

	public String js_getURL()
	{
		if (editorKit instanceof FixedHTMLEditorKit)
		{
			URL url = enclosedComponent.getPage();
			if (url != null)
			{
				return url.toString();
			}
		}
		return null;
	}

	public void js_setBaseURL(String url)
	{
		Document document = enclosedComponent.getDocument();
		if (document instanceof HTMLDocument)
		{
			try
			{
				((HTMLDocument)document).setBase(new URL(url));
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
	}

	public String js_getBaseURL()
	{
		Document document = enclosedComponent.getDocument();
		if (document instanceof HTMLDocument)
		{
			URL url = ((HTMLDocument)document).getBase();
			if (url != null)
			{
				return url.toString();
			}
		}
		return null;
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		setForeground(PersistHelper.createColor(clr));
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


	/*
	 * scroll---------------------------------------------------
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

	public String[] js_getLabelForElementNames()
	{
		if (labels != null)
		{
			ArrayList<String> al = new ArrayList<String>(labels.size());
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX)) //$NON-NLS-1$
				{
					al.add(label.getName());
				}
			}
			return al.toArray(new String[al.size()]);
		}
		return new String[0];
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
					ILabel label = labels.get(i);
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

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}


	/*
	 * editable---------------------------------------------------
	 */
	@Deprecated
	public boolean js_isEditable()
	{
		return enclosedComponent.isEditable();
	}

	@Deprecated
	public void js_setEditable(boolean b)
	{
		setEditable(b);
	}


	/*
	 * readonly---------------------------------------------------
	 */
	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	public boolean isReadOnly()
	{
		return !enclosedComponent.isEditable();
	}

	private boolean editState;

	public void js_setReadOnly(boolean b)
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

	public String js_getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}


	/*
	 * tooltip---------------------------------------------------
	 */
	public void js_setToolTipText(String txt)
	{
		setToolTipText(txt);
	}

	public String js_getToolTipText()
	{
		return getToolTipText();
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

	/*
	 * location---------------------------------------------------
	 */
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

	private Point cachedLocation;

	public void js_setLocation(int x, int y)
	{
		cachedLocation = new Point(x, y);
		setLocation(x, y);
		this.validate();
	}

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		putClientProperty(key, value);
		enclosedComponent.putClientProperty(key, value);
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
		this.validate();
	}

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	public int js_getWidth()
	{
		return getSize().width;
	}

	public int js_getHeight()
	{
		return getSize().height;
	}


	/*
	 * textselect---------------------------------------------------
	 */
	public void js_selectAll()
	{
		enclosedComponent.selectAll();
	}

	public String js_getSelectedText()
	{
		return enclosedComponent.getSelectedText();
	}

	public void js_replaceSelectedText(String s)
	{
		if (editProvider != null) editProvider.startEdit();
		enclosedComponent.replaceSelection(s);
		if (editProvider != null) editProvider.commitData();
	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public String js_getAsPlainText()
	{
		Document doc = enclosedComponent.getDocument();
		if (doc != null)
		{
			try
			{
				return doc.getText(0, doc.getLength());
			}
			catch (BadLocationException e)
			{
				Debug.error(e);
			}
		}
		return null;
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

	public void js_requestFocus(Object[] vargs)
	{
//		if (!enclosedComponent.hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (vargs != null && vargs.length >= 1 && !Utils.getAsBoolean(vargs[0]))
			{
				eventExecutor.skipNextFocusGain();
			}
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


	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
	}

	public String js_getDataProviderID()
	{
		return getDataProviderID();
	}

	public String js_getElementType()
	{
		if (editorKit instanceof FixedRTFEditorKit)
		{
			return "RTF_AREA"; //$NON-NLS-1$
		}
		else
		{
			return "HTML_AREA"; //$NON-NLS-1$
		}
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getValueObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		if (eventExecutor.mustFireFocusLostCommand())
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

	@Override
	protected void printChildren(Graphics g)
	{
		super.printChildren(g);
		super.printBorder(g); // print border after children print, to prevent child backgrounds ontop of border
	}

	@Override
	protected void printBorder(Graphics g)
	{
		//intentionally empty to have the border drawn after the content
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
}