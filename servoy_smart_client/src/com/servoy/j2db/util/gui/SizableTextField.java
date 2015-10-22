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
package com.servoy.j2db.util.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import com.servoy.j2db.util.Debug;


/**
 * <class description> creation <date> changes <date1>, <description1> ... <daten>, <descriptionn>
 * 
 * @author <author name(s)>
 * @see <string>
 */

public class SizableTextField extends JLabel implements MouseListener
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
	private final SpecialTextArea textArea;
	private boolean select = false;
	public JRootPane rootPane;
	private Document doc = new PlainDocument();
	private List _focusListeners;
	private List _cellEditorListeners;
	private boolean havingFocus = false; //can't use isManaging() focus becouse that may influence the focus manager

	private Character eventChar;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public SizableTextField()
	{
		this.setBorder(BorderFactory.createEtchedBorder());
		setBackground(Color.white);
		setOpaque(true);
		addMouseListener(this);
		textArea = new SpecialTextArea();
		textArea.setFont(getFont());
		textArea.setLineWrap(true);
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */
	public Object getValue()
	{
		return this.getText();
	}

	public void setValue(Object o)
	{
		if (o != null)
		{
			this.setText(o.toString());
		}
		else
		{
			this.setText(""); //$NON-NLS-1$
		}
	}

	public void setDocument(Document doc)
	{
		this.doc = doc;
	}

	public Document getDocument()
	{
		return doc;
	}

	@Override
	public void setText(String s)
	{
		if (s == null) s = ""; //$NON-NLS-1$
//		Debug.trace("renderer.setText "+s);
//		if (havingFocus) 
		super.setText(s);
		if (textArea != null && havingFocus) textArea.setText(s);
	}

	@Override
	public void setForeground(Color c)
	{
//		if (havingFocus) textArea.setForeground(c);
		super.setForeground(c);
		if (textArea != null) textArea.setForeground(c);
	}

	@Override
	public void setBackground(Color c)
	{
//		if (havingFocus) textArea.setBackground(c);
		super.setBackground(c);
		if (textArea != null) textArea.setBackground(c);
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		if (isEnabled()) requestFocus();
	}

	@Override
	public void grabFocus()
	{
		super.grabFocus();
		requestFocus();
	}

	public void selectAll()
	{
		select = true;
	}

	@Override
	public void requestFocus()
	{
		if (rootPane == null)
		{
			rootPane = SwingUtilities.getRootPane(this);
			if (rootPane == null)
			{
				throw new IllegalStateException("SizableTextField works only with a parent wich is a JRootPane"); //$NON-NLS-1$
			}
			JLayeredPane lay = rootPane.getLayeredPane();
			rootPane.getGlassPane().addMouseListener(textArea);
			rootPane.getGlassPane().addMouseMotionListener(textArea);
			lay.add(textArea, JLayeredPane.MODAL_LAYER, 5);
		}
		textArea.setDocument(doc);
		textArea.setFont(this.getFont());
		textArea.setBackground(this.getBackground());
		textArea.setForeground(this.getForeground());
		textArea.setVisible(true);
		if (/* getBorder() == null && */getParent() instanceof JComponent)
		{
			Border b = ((JComponent)getParent()).getBorder();
			if (b != null)
			{
				textArea.setBorder(b);
			}
			else
			{
				textArea.setBorder(BorderFactory.createLineBorder(Color.black));
			}
		}
		else
		{
			textArea.setBorder(this.getBorder());
		}
//		textArea.setMargin(new Insets(0,2,0,0));
		textArea.setLocation(computeLocation(this));
		textArea.setSize(getSize());
		if (eventChar != null)
		{
			textArea.setText(getText() + eventChar.toString());
			eventChar = null;
		}
		else
		{
			textArea.setText(getText());
		}
		textArea.doSize();
		fireFocusGained();
		rootPane.getGlassPane().setVisible(true);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				textArea.requestFocus();
				if (select) textArea.selectAll();
			}
		});

	}

	@Override
	public boolean isFocusTraversable()
	{
		return true;
	}

	//new focus listener becouse focusin is changed for this componenet	
	public void addCellEditorListener(CellEditorListener l)
	{
		if (_cellEditorListeners == null) _cellEditorListeners = new ArrayList();
		_cellEditorListeners.add(l);
	}

	public void removeCellEditorListener(CellEditorListener l)
	{
		_cellEditorListeners.remove(l);
	}

	//new focus listener becouse focusin is changed for this componenet	
	@Override
	public void addFocusListener(FocusListener l)
	{
		if (_focusListeners == null) _focusListeners = new ArrayList();
		_focusListeners.add(l);
	}

	@Override
	public void removeFocusListener(FocusListener l)
	{
		_focusListeners.remove(l);
	}

	void fireFocusGained()
	{
		havingFocus = true;
		if (_focusListeners != null)
		{
			for (int i = 0; i < _focusListeners.size(); i++)
			{
				((FocusListener)_focusListeners.get(i)).focusGained(new FocusEvent(this, 0, false));
			}
		}
	}

	void fireFocusLost()
	{
		havingFocus = false;
		if (_focusListeners != null) for (int i = 0; i < _focusListeners.size(); i++)
		{
			((FocusListener)_focusListeners.get(i)).focusLost(new FocusEvent(this, 0, false));
		}
		if (_cellEditorListeners != null) for (int i = 0; i < _cellEditorListeners.size(); i++)
		{
			((CellEditorListener)_cellEditorListeners.get(i)).editingStopped(new ChangeEvent(this));
		}
	}

	private Point computeLocation(Component w)
	{
		Point retval = new Point();
		while ((w != null) && !(w instanceof JRootPane))
		{
			retval.translate(w.getLocation().x, w.getLocation().y);
//			Debug.trace(w);
			w = w.getParent();
		}
		return retval;
	}

	/**
	 * @see java.awt.Component#setFont(Font)
	 */
	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		if (textArea != null) textArea.setFont(f);
	}

	/**
	 * @see javax.swing.JComponent#processKeyBinding(KeyStroke, KeyEvent, int, boolean)
	 */
	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		if (Character.isLetterOrDigit(e.getKeyChar()))
		{
			eventChar = new Character(e.getKeyChar());
		}
		return super.processKeyBinding(ks, e, condition, pressed);
	}

	class SpecialTextArea extends JTextArea implements CaretListener, FocusListener, MouseListener, MouseMotionListener
	{
		SpecialTextArea()
		{
			super();
			this.addCaretListener(this);
			this.addFocusListener(this);
			//		this.addMouseListener(this);
			//		this.addMouseMotionListener(this);
			//		this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
			this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape"); //$NON-NLS-1$
			/*
			 * this.getActionMap().put("enter", new AbstractAction() { public void actionPerformed(ActionEvent event) { System.out.println(event); } });
			 */this.getActionMap().put("escape", new AbstractAction() //$NON-NLS-1$
				{
					public void actionPerformed(ActionEvent event)
					{
						//				System.out.println(event);
					}
				});

		}

		@Override
		protected void processKeyEvent(KeyEvent e)
		{
			super.processKeyEvent(e);
		}

		@Override
		protected void processComponentKeyEvent(KeyEvent e)
		{
			//		Debug.trace("processComponentKeyEvent "+e.getID());
			if ((e.getKeyCode() == KeyEvent.VK_TAB || e.getKeyCode() == KeyEvent.VK_ESCAPE))// || (e.getKeyCode() == e.VK_ENTER && e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD))) 
			{
				if (e.getID() == KeyEvent.KEY_PRESSED)
				{
					this.setVisible(false);
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					{
						super.setText(SizableTextField.this.getText());
					}
					else
					{
						SizableTextField.this.setText(getText());
					}
					SizableTextField.this.fireFocusLost();
					if ((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK)
					{
						FocusManager.getCurrentManager().focusPreviousComponent(SizableTextField.this);
					}
					else
					{
						FocusManager.getCurrentManager().focusNextComponent(SizableTextField.this);
					}
				}
				e.consume();
			}
			else
			{
				super.processComponentKeyEvent(e);
			}
		}

		public void caretUpdate(CaretEvent e)
		{
			doSize();
		}

		public void doSize()
		{
			FontMetrics fm = this.getFontMetrics(this.getFont());
			StringTokenizer st = new StringTokenizer(getText(), "\n", true); //$NON-NLS-1$
			int rows = getText().endsWith("\n") ? 0 : 1; //$NON-NLS-1$
			while (st.hasMoreTokens())
			{
				//			rows++;
				String sToken = st.nextToken();
				if (sToken.equals("\n")) //$NON-NLS-1$
				{
					rows++;
				}
				else
				{
					rows += fm.stringWidth(sToken) / SizableTextField.this.getWidth();
				}
			}
			if (getText().endsWith("\n")) rows++; //$NON-NLS-1$
			int height = rows * fm.getHeight();
			if (height > SizableTextField.this.getHeight())
			{
				Border b = this.getBorder();
				Insets i;
				if (b == null)
				{
					i = new Insets(0, 0, 0, 0);
				}
				else
				{
					i = b.getBorderInsets(this);
				}
				setSize(getWidth(), rows * fm.getHeight() + i.top + i.bottom);
			}
			else
			{
				setSize(SizableTextField.this.getSize());
			}
		}

		public void focusGained(FocusEvent e)
		{
			doSize();
			setCaretPosition(getText().length());
		}

		public void focusLost(FocusEvent e)
		{
			SizableTextField.this.setText(getText());
			SizableTextField.this.fireFocusLost();
			this.setVisible(false);
		}

		// -------------------- code for correct hiding of glasspane
		public void mouseMoved(MouseEvent e)
		{
			//		    redispatchMouseEvent(e);
		}

		private boolean dragMode = false;

		public void mouseDragged(MouseEvent e)
		{
			dragMode = true;
			redispatchMouseEvent(e);
		}

		public void mouseClicked(MouseEvent e)
		{
			redispatchMouseEvent(e);
			dragMode = false;
		}

		public void mouseEntered(MouseEvent e)
		{
			//		    redispatchMouseEvent(e);
		}

		public void mouseExited(MouseEvent e)
		{
			//		    redispatchMouseEvent(e);
		}

		public void mousePressed(MouseEvent e)
		{
			redispatchMouseEvent(e);
		}

		public void mouseReleased(MouseEvent e)
		{
			redispatchMouseEvent(e);
			dragMode = false;
		}

		private void redispatchMouseEvent(MouseEvent e)
		{
			Component glassPane = rootPane.getGlassPane();

			if (e.getSource() != glassPane) return;

			JLayeredPane container = rootPane.getLayeredPane();

			Point glassPanePoint = e.getPoint();
			//Container container = rootPane.getContentPane();
			Point containerPoint = SwingUtilities.convertPoint(glassPane, glassPanePoint, container);
			Component component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);
			if (component == null || component == e.getSource()) return;

			Point componentPoint = SwingUtilities.convertPoint(glassPane, glassPanePoint, component);
			MouseEvent ne = new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(), componentPoint.x, componentPoint.y, e.getClickCount(),
				e.isPopupTrigger());

			if (component == this)
			{
				try
				{
					this.dispatchEvent(ne);
				}
				catch (Exception ex)
				{
					// ignore
					Debug.trace(ex);
				}
			}
			else if (!dragMode)
			{
				rootPane.getGlassPane().setVisible(false);
				SizableTextField.this.setText(getText());
				SizableTextField.this.fireFocusLost();
				this.setVisible(false);
				component.dispatchEvent(ne);
			}
		}
	}

}
