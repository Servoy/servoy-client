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


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.StringTokenizer;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.IconUIResource;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.scripting.AbstractRuntimeButton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.JpegEncoder;
import com.servoy.j2db.util.gui.MyImageIcon;

/**
 * @author jcompagner
 */
public abstract class AbstractScriptButton extends JButton implements ISkinnable, IButton, ISupportCachedLocationAndSize
{
	private static final long serialVersionUID = 1L;

	private int rotation;
	private int mediaOption;

	private boolean specialPaint = false;
	protected IApplication application;
	protected final AbstractRuntimeButton<IButton> scriptable;
	protected EventExecutor eventExecutor;

	private MouseAdapter doubleclickMouseAdapter;
	private MouseAdapter rightclickMouseAdapter;

	public AbstractScriptButton(IApplication app, AbstractRuntimeButton<IButton> scriptable)
	{
		this.scriptable = scriptable;
		application = app;
//		setContentAreaFilled(false);
		eventExecutor = new EventExecutor(this);
	}

	public final AbstractRuntimeButton<IButton> getScriptObject()
	{
		return scriptable;
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
			Media media = application.getFlattenedSolution().getMedia(mediaId);
			if (media != null) text_url = MediaURLStreamHandler.MEDIA_URL_DEF + media.getName();
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
	 * @see com.servoy.j2db.ui.runtime.IRuntimeBaseLabel#setImageURL(java.lang.String)
	 */
	public void setImageURL(String text_url)
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

	public void setRolloverImageURL(String image_url)
	{
		this.rollover_url = image_url;
		try
		{
			if (image_url != null)
			{
				ImageIcon rolloverIcon = new ImageIcon(new URL(image_url));
				if (mediaOption != 1)
				{
					super.setRolloverIcon(new MyImageIcon(application, this, rolloverIcon, mediaOption));
				}
				else
				{
					super.setRolloverIcon(rolloverIcon);
				}
			}
			else setRolloverIcon((Icon)null);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private String rollover_url;

	public String getRolloverImageURL()
	{
		return rollover_url;
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
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

	public void requestFocusToComponent()
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

	private int textTransformMode;


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


	private Dimension cachedSize;

	private ActionListener actionAdapter;


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

	public byte[] getThumbnailJPGImage(int width, int height)
	{
		return getThumbnailJPGImage(this, width, height, getIcon());
	}

	static byte[] getThumbnailJPGImage(Component component, int width, int height, Icon icon)
	{
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
			JpegEncoder encoder = new JpegEncoder(component, image, 100, baos);
			encoder.compress();
			return baos.toByteArray();
		}
		return null;
	}

	@Override
	public String toString()
	{
		return scriptable.toString();
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

	private Timer clickTimer;

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
				public void actionPerformed(final ActionEvent e)
				{
					if (doubleclickMouseAdapter != null)
					{
						clickTimer = new Timer(UIUtils.getClickInterval(), new ActionListener()
						{
							public void actionPerformed(ActionEvent ev)
							{
								eventExecutor.fireActionCommand(true, AbstractScriptButton.this, e.getModifiers());
							}
						});
						clickTimer.setRepeats(false); //after expiring once, stop the timer
						clickTimer.start();
					}
					else
					{
						eventExecutor.fireActionCommand(true, AbstractScriptButton.this, e.getModifiers());
					}
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
		if (viewable)
		{
			setVisible(b_visible);
		}
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
						if (clickTimer != null) clickTimer.stop();
						eventExecutor.fireDoubleclickCommand(true, AbstractScriptButton.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(doubleclickMouseAdapter);
			setMultiClickThreshhold(UIUtils.getClickInterval());
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

	public String getParameterValue(String param)
	{
		return null;
	}

	public int getFontSize()
	{
		return 0;
	}

	public Object getLabelFor()
	{
		return null;
	}

	public int getDisplayedMnemonic()
	{
		return getMnemonic();
	}
}