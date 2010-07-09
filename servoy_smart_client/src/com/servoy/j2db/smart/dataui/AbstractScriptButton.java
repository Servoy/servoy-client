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


import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.IconUIResource;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.JpegEncoder;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.MyImageIcon;

/**
 * @author jcompagner
 */
public class AbstractScriptButton extends JButton implements ISkinnable, IButton, ISupportCachedLocationAndSize
{
	private static final long serialVersionUID = 1L;

	private int rotation;
	private int mediaOption;

	private boolean specialPaint = false;
	protected IApplication application;

	protected EventExecutor eventExecutor;

	private MouseAdapter doubleclickMouseAdapter;
	private MouseAdapter rightclickMouseAdapter;


	public AbstractScriptButton(IApplication app)
	{
		application = app;
//		setContentAreaFilled(false);
		eventExecutor = new EventExecutor(this);
	}

	/**
	 * Fix for bad font rendering (bad kerning == strange spacing) in java 1.5 see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097047
	 */
	@Override
	public FontMetrics getFontMetrics(Font font)
	{
		if (application != null)//getFontMetrics can be called in the constructor super call before application is assigned
		{
			boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting"));
			if (isPrinting)
			{
				Graphics g = (Graphics)application.getRuntimeProperties().get("printGraphics");
				if (g != null)
				{
					String text = getText();
					// only return print graphics font metrics if text does not start with 'W',
					// because of left side bearing issue
					if (!(text != null && text.length() > 0 && text.charAt(0) == 'W')) return g.getFontMetrics(font);
				}
			}
		}
		return super.getFontMetrics(font);
	}

	@Override
	public void setToolTipText(String tip)
	{
		if (!Utils.stringIsEmpty(tip))
		{
			if (!Utils.stringContainsIgnoreCase(tip, "<html")) //$NON-NLS-1$
			{
				super.setToolTipText(tip);
			}
			else if (HtmlUtils.hasUsefulHtmlContent(tip))
			{
				super.setToolTipText(tip);
			}
		}
		else
		{
			super.setToolTipText(null);
		}
	}

/*
 * does not work, when onAction is fired the repaint event is not yet posted public AbstractScriptButton() { addActionListener(new ActionListener() { public
 * void actionPerformed(ActionEvent e) { Utils.dispatchEvents(700); } }); }
 * 
 * //We override this method becouse we want first added listners to be notified first! protected void fireActionPerformed(ActionEvent event) { // Guaranteed to
 * return a non-null array Object[] listeners = listenerList.getListenerList(); ActionEvent e = null; // Process the listeners first to last, notifying // those
 * that are interested in this event for (int i = 0 ; i < listeners.length ; i+=2) { if (listeners[i]==ActionListener.class) { // Lazily create the event: if (e
 * == null) { String actionCommand = event.getActionCommand(); if(actionCommand == null) { actionCommand = getActionCommand(); } e = new ActionEvent(this,
 * ActionEvent.ACTION_PERFORMED, actionCommand, event.getWhen(), event.getModifiers()); } ((ActionListener)listeners[i+1]).actionPerformed(e); } } }
 */

	@Override
	public void setIcon(Icon icon)
	{
		Icon prevIcon = getIcon();
		if (prevIcon instanceof MyImageIcon)
		{
			Icon rollOver = getRolloverIcon();
			if (rollOver instanceof MyImageIcon)
			{
				((MyImageIcon)prevIcon).removeImageIcon((MyImageIcon)rollOver);
			}
		}
		ComponentFactory.deregisterIcon(prevIcon);

		if (mediaOption != 1 && icon instanceof ImageIcon)
		{
			//do scaling cropping if needed
			icon = new MyImageIcon(application, this, (ImageIcon)icon, mediaOption);

			Icon rollOver = getRolloverIcon();
			if (rollOver instanceof MyImageIcon)
			{
				((MyImageIcon)icon).addImageIcon((MyImageIcon)rollOver);
			}
		}
		ComponentFactory.registerIcon(icon);
		super.setIcon(icon);
	}

	public int getMediaIcon()
	{
		return mediaId;
	}

	private int mediaId;

	public void setMediaIcon(int mediaId)
	{
		this.mediaId = mediaId;
		try
		{
			setIcon(ComponentFactory.loadIcon(application.getFlattenedSolution(), new Integer(mediaId)));
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	private IconUIResource disabledIcon;

	@Override
	public Icon getDisabledIcon()
	{
		if (disabledIcon != null) return disabledIcon;
		if (getIcon() instanceof MyImageIcon)
		{
			MyImageIcon icon = (MyImageIcon)getIcon();
			icon.getIconWidth();
			ImageIcon scaledIcon = icon.getScaledIcon(0, 0);
			if (scaledIcon != null)
			{
				disabledIcon = new IconUIResource(new ImageIcon(GrayFilter.createDisabledImage((scaledIcon).getImage())));
				return disabledIcon;
			}
		}
		return super.getDisabledIcon();
	}

	public void setIcon(byte[] data)
	{
		Icon prevIcon = getIcon();
		if (prevIcon instanceof MyImageIcon)
		{
			Icon rollOver = getRolloverIcon();
			if (rollOver instanceof MyImageIcon)
			{
				((MyImageIcon)prevIcon).removeImageIcon((MyImageIcon)rollOver);
			}
		}
		ComponentFactory.deregisterIcon(prevIcon);

		Icon icon = null;
		// media option 1 == crop so no scaling
		if (mediaOption != 1 && data != null)
		{
			//do scaling cropping if needed
			icon = new MyImageIcon(application, this, data, mediaOption);

			Icon rollOver = getRolloverIcon();
			if (rollOver instanceof MyImageIcon)
			{
				((MyImageIcon)icon).addImageIcon((MyImageIcon)rollOver);
			}
		}
		else
		{
			icon = ImageLoader.getIcon(data, -1, -1, true);
		}
		ComponentFactory.registerIcon(icon);
		super.setIcon(icon);
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setRolloverIcon(int)
	 */
	public void setRolloverIcon(int rollOverMediaId)
	{
		try
		{
			setRolloverIcon(ComponentFactory.loadIcon(application.getFlattenedSolution(), new Integer(rollOverMediaId)));
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public void setRolloverIcon(byte[] data)
	{
		ComponentFactory.deregisterIcon(getRolloverIcon());

		Icon rolloverIcon = null;
		// media option 1 == crop so no scaling
		if (mediaOption != 1 && data != null)
		{
			//do scaling cropping if needed
			rolloverIcon = new MyImageIcon(application, this, data, mediaOption);
			Icon icon = getIcon();
			if (icon instanceof MyImageIcon)
			{
				((MyImageIcon)icon).addImageIcon((MyImageIcon)rolloverIcon);
			}
		}
		else
		{
			rolloverIcon = ImageLoader.getIcon(data, -1, -1, true);
		}
		super.setRolloverIcon(rolloverIcon);
	}

	@Override
	public void paint(Graphics g)
	{
		if (rotation == 0)
		{
			super.paint(g);
		}
		else
		{
			AffineTransform at = ((Graphics2D)g).getTransform();
			AffineTransform save = (AffineTransform)at.clone();
			try
			{
				int w = getWidth();
				int h = getHeight();

				if (rotation >= 45 && rotation <= 135)
				{
					specialPaint = true;
					at.rotate(Math.toRadians(rotation), w / 2, w / 2);
				}
				else if (rotation >= 135 && rotation <= 225)
				{
					at.rotate(Math.toRadians(rotation), w / 2, h / 2);
				}
				else
				{
					specialPaint = true;
					at.rotate(Math.toRadians(rotation), h / 2, h / 2);
				}
				((Graphics2D)g).setTransform(at);
				super.paint(g);
			}
			finally
			{
				specialPaint = false;
				((Graphics2D)g).setTransform(save);

				// now paint the border
				doPaintBorder = true;
				paintBorder(g);
				doPaintBorder = false;
			}
		}
	}

	private boolean doPaintBorder;

	@Override
	public boolean isBorderPainted()
	{
		return (doPaintBorder ? true : super.isBorderPainted());
	}

	@Override
	public int getWidth()
	{
		if (specialPaint)
		{
			return super.getHeight();
		}
		else
		{
			return super.getWidth();
		}
	}

	@Override
	public int getHeight()
	{
		if (specialPaint)
		{
			return super.getWidth();
		}
		else
		{
			return super.getHeight();
		}
	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}


	/**
	 * @implem see com.servoy.j2db.ui.IScriptLabelMethods#js_getBgcolor()
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(getBackground());
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setBgcolor(java.lang.String)
	 */
	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getFgcolor()
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(getForeground());
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setFgcolor(java.lang.String)
	 */
	public void js_setFgcolor(String clr)
	{
		setForeground(PersistHelper.createColor(clr));
	}

	public void js_setBorder(String spec)
	{
		Border border = ComponentFactoryHelper.createBorder(spec);
		Border oldBorder = getBorder();
		if (oldBorder instanceof CompoundBorder && ((CompoundBorder)oldBorder).getInsideBorder() != null)
		{
			Insets insets = ((CompoundBorder)oldBorder).getInsideBorder().getBorderInsets(this);
			setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)));
		}
		else
		{
			setBorder(border);
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_isVisible()
	 */
	public boolean js_isVisible()
	{
		return isVisible();
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setVisible(boolean)
	 */
	public void js_setVisible(boolean b)
	{
		setVisible(b);
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_isTransparent()
	 */
	public boolean js_isTransparent()
	{
		return !isOpaque();
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setTransparent(boolean)
	 */
	public void js_setTransparent(boolean b)
	{
		setOpaque(!b);
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setImageURL(java.lang.String)
	 */
	public void js_setImageURL(String text_url)
	{
		this.text_url = text_url;
		try
		{
			if (text_url == null)
			{
				setIcon((Icon)null);
			}
			else
			{
				URL url = new URL(text_url);
				setIcon(new ImageIcon(url));
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private String text_url;

	public String getImageURL()
	{
		return text_url;
	}

	public void js_setEnabled(final boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
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
			requestFocus();
		}
	}

	public void js_requestFocus(Object[] vargs)
	{
//		if (!hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (isDisplayable())
			{
				// Must do it in a runnable or else others after a script can get focus first again..
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
				wantFocus = true;
			}
		}
	}

	private String i18nTT;

	private int textTransformMode;

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setToolTipText(java.lang.String)
	 */
	public void js_setToolTipText(String text)
	{
		if (text != null && text.startsWith("i18n:")) //$NON-NLS-1$
		{
			i18nTT = text;
			text = application.getI18NMessage(text);
		}
		else
		{
			i18nTT = null;
		}
		setToolTipText(text);
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getToolTipText()
	 */
	public String js_getToolTipText()
	{
		if (i18nTT != null) return i18nTT;
		return getToolTipText();
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "BUTTON";
	}

	public String js_getDataProviderID()
	{
		//default implementation
		return null;
	}

	public String js_getMnemonic()
	{
		int i = getMnemonic();
		if (i == 0) return "";
		return new Character((char)i).toString();
	}

	public void js_setMnemonic(String mnemonic)
	{
		mnemonic = application.getI18NMessageIfPrefixed(mnemonic);
		if (mnemonic != null && mnemonic.length() > 0)
		{
			setDisplayedMnemonic(mnemonic.charAt(0));
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getLocationX()
	 */
	public int js_getLocationX()
	{
		return getLocation().x;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getLocationY()
	 */
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

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setLocation(int, int)
	 */
	public void js_setLocation(int x, int y)
	{
		cachedLocation = new Point(x, y);
		setLocation(x, y);
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
		if ("contentAreaFilled".equals(key) && value instanceof Boolean)
		{
			setContentAreaFilled(((Boolean)value).booleanValue());
		}
		else
		{
			putClientProperty(key, value);
		}
	}

	public Object js_getClientProperty(Object key)
	{
		return getClientProperty(key);
	}


	private Dimension cachedSize;

	private ActionListener actionAdapter;

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setSize(int, int)
	 */
	public void js_setSize(int x, int y)
	{
		cachedSize = new Dimension(x, y);
		setSize(x, y);
	}

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_setFont(java.lang.String)
	 */
	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getWidth()
	 */
	public int js_getWidth()
	{
		return getSize().width;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getHeight()
	 */
	public int js_getHeight()
	{
		return getSize().height;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getThumbnailJPGImage(java.lang.Object[])
	 */
	public byte[] js_getThumbnailJPGImage(Object[] args)
	{
		int width = -1;
		int height = -1;
		if (args != null && args.length == 2)
		{
			width = Utils.getAsInteger(args[0]);
			height = Utils.getAsInteger(args[1]);
		}
		Icon icon = getIcon();
		Image image = null;
		if (icon instanceof MyImageIcon)
		{
			ImageIcon myIcon = ((MyImageIcon)icon).getScaledIcon(width, height);
			image = myIcon.getImage();
		}
		else if (icon instanceof ImageIcon)
		{
			image = ((ImageIcon)icon).getImage();
		}
		if (image != null)
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JpegEncoder encoder = new JpegEncoder(this, image, 100, baos);
			encoder.compress();
			return baos.toByteArray();
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getName()
	 */
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
			",height:" + js_getHeight() + ",label:" + getText() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	/**
	 * @return
	 */
	public int getRotation()
	{
		return rotation;
	}

	/**
	 * @param i
	 */
	public void setRotation(int i)
	{
		rotation = i;
		if (i != 0)
		{
			//fix for bad painting
			setContentAreaFilled(false);
			setBorderPainted(false);
//			setFocusPainted(false);
		}
	}

	/**
	 * @return Returns the mediaOption.
	 */
	public int getMediaOption()
	{
		return mediaOption;
	}

	/**
	 * @param mediaOption The mediaOption to set.
	 */
	public void setMediaOption(int mediaOption)
	{
		this.mediaOption = mediaOption;
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setActionCommand(java.lang.String, Object[])
	 */
	public void setActionCommand(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
		if (id != null && actionAdapter == null)
		{
			actionAdapter = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					eventExecutor.fireActionCommand(true, AbstractScriptButton.this, e.getModifiers());

				}
			};
			addActionListener(actionAdapter);
		}
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#addScriptExecuter(com.servoy.j2db.IScriptExecuter)
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}

	public boolean isReadOnly()
	{
		return true;
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setTextTransform(int)
	 */
	public void setTextTransform(int mode)
	{
		this.textTransformMode = mode;
	}

	/**
	 * @see javax.swing.JLabel#setText(java.lang.String)
	 */
	@Override
	public void setText(String text)
	{
		if (text != null && text.length() > 0)
		{
			if (textTransformMode == ILabel.CAPITALIZE)
			{
				StringBuffer sb = new StringBuffer(text.length());
				StringTokenizer st = new StringTokenizer(text);
				while (st.hasMoreTokens())
				{
					String word = st.nextToken();
					sb.append(Character.toUpperCase(word.charAt(0)));
					sb.append(word.substring(1));
					sb.append(" ");
				}
				text = sb.substring(0, sb.length() - 1);
			}
			else if (textTransformMode == ILabel.LOWERCASE)
			{
				text = text.toLowerCase();
			}
			else if (textTransformMode == ILabel.UPPERCASE)
			{
				text = text.toUpperCase();
			}
		}
		super.setText(text);
	}

	public String getId()
	{
		return (String)getClientProperty("Id");
	}

	/**
	 * @see com.servoy.j2db.ui.IStandardLabel#setDisplayedMnemonic(char)
	 */
	public void setDisplayedMnemonic(char mnemonic)
	{
		setMnemonic(mnemonic);
	}

	public void setDoubleClickCommand(String id, Object[] args)
	{
		eventExecutor.setDoubleClickCmd(id, args);
		if (id != null && doubleclickMouseAdapter == null)
		{
			doubleclickMouseAdapter = new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					// Don't allow double click with other buttons except left button.
					if ((e.getClickCount() == 2) && SwingUtilities.isLeftMouseButton(e) && isEnabled())
					{
						eventExecutor.fireDoubleclickCommand(true, AbstractScriptButton.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(doubleclickMouseAdapter);
		}
	}

	public void setRightClickCommand(String id, Object[] args)
	{
		eventExecutor.setRightClickCmd(id, args);
		if (id != null && rightclickMouseAdapter == null)
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
						eventExecutor.fireRightclickCommand(true, AbstractScriptButton.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(rightclickMouseAdapter);
		}
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}
}