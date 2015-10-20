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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.util.StringTokenizer;

import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.scripting.AbstractHTMLSubmitRuntimeLabel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.MyImageIcon;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jcompagner
 */
public abstract class AbstractScriptLabel extends JLabel implements ISkinnable, ILabel, ISupportCachedLocationAndSize
{
	private static final long serialVersionUID = 1L;

	private int rotation;
	private int mediaOption;
	private int textTransformMode;
	private boolean specialPaint = false;
	protected IApplication application;
	private boolean borderPainted = true;
	protected EventExecutor eventExecutor;
	protected final AbstractHTMLSubmitRuntimeLabel<ILabel> scriptable;

	private MouseAdapter actionMouseAdapter = null;
	private MouseAdapter doubleClickMouseAdapter = null;
	private MouseAdapter rightclickMouseAdapter = null;
	private MouseAdapter rolloverMouseAdapter = null;

	public AbstractScriptLabel(IApplication app, AbstractHTMLSubmitRuntimeLabel<ILabel> scriptable)
	{
		this.scriptable = scriptable;
		application = app;
		eventExecutor = new EventExecutor(this);
	}

	public final AbstractHTMLSubmitRuntimeLabel<ILabel> getScriptObject()
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
			boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
			if (isPrinting)
			{
				Graphics g = (Graphics)application.getRuntimeProperties().get("printGraphics"); //$NON-NLS-1$
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

	/**
	 * Fix for bad font rendering (bad kerning == strange spacing) in java 1.5 see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097047
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
		if (isPrinting && getText() != null && !HtmlUtils.startsWithHtml(getText()) && isEnabled() && getIcon() == null && !isOpaque() &&
			isEmptyBorder(getBorder()))
		{
			FontMetrics fm = getFontMetrics(getFont());
			Insets insets = getInsets(new Insets(0, 0, 0, 0));

			Rectangle paintTextR = new Rectangle();
			Rectangle paintViewR = new Rectangle();
			Rectangle paintIconR = new Rectangle();

			paintViewR.x = insets.left;
			paintViewR.y = insets.top;
			paintViewR.width = getWidth() - (insets.left + insets.right);
			paintViewR.height = getHeight() - (insets.top + insets.bottom);

			paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;

			SwingUtilities.layoutCompoundLabel(this, fm, getText(), null, this.getVerticalAlignment(), this.getHorizontalAlignment(),
				this.getVerticalTextPosition(), this.getHorizontalTextPosition(), paintViewR, paintIconR, paintTextR, this.getIconTextGap());

			int textX = paintTextR.x;
			int textY = paintTextR.y + fm.getAscent();

			g.drawString(getText(), textX, textY);
		}
		else
		{
			// metal and windows laf do not sets the font, they rely on the setting of the font
			// by the container, when rendering the child, but that is not working in case of
			// table views where the label is not a child, so lets just set the component font
			// on the graphics
			g.setFont(getFont());
			super.paintComponent(g);
		}
	}

	private boolean isEmptyBorder(Border border)
	{
		if (border == null) return true;
		if (border instanceof EmptyBorder)
		{
			Insets i = ((EmptyBorder)border).getBorderInsets();
			if (i != null)
			{
				return (i.bottom == 0 && i.top == 0 && i.left == 0 && i.right == 0);
			}
		}
		return false;
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

	//fix for incorrect clipping
	@Override
	public void print(Graphics g)
	{
		Shape saveClip = g.getClip();
		try
		{
			int w = getWidth();
			int h = getHeight();
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

	/**
	 * Created this method in order to not remove this piece of code, but make it take into account rotation.
	 */
	private void superModifiedPaint(Graphics g)
	{
		if (application.getModeManager().getMode() == IModeManager.PREVIEW_MODE)
		{
			// don't remember what this code is suppose to fix (why is condition based on PREVIEW_MODE and not on application.getRuntimeProperty("isprinting"));
			// also it seems that super.paint(g) would call this "htmlView.setSize(r.width, r.height)" anyway if needed so this entire "if" statement could be unnecessary;
			// however, if there was a problem on some OS/java version/L&F, it might be that the condition needs to be changed to "isprinting"
			View htmlView = getViewThatIsAbleToWrap();
			if (htmlView != null)
			{
				Rectangle r = getTextBoundsForSize(new Dimension(getWidth(), getHeight())); // these will take into account rotation because of the getHeight/getWidth methods
				htmlView.setSize(r.width, r.height);
			}
		}
		super.paint(g);
	}

	@Override
	public void paint(Graphics g)
	{
		if (rotation == 0)
		{
			superModifiedPaint(g);
		}
		else
		{
			borderPainted = false;
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
				superModifiedPaint(g);
			}
			finally
			{
				specialPaint = false;
				((Graphics2D)g).setTransform(save);

				// now paint the border
				borderPainted = true;
				paintBorder(g);
				borderPainted = false;
			}
		}
	}

	@Override
	protected void paintBorder(Graphics g)
	{
		if (borderPainted)
		{
			super.paintBorder(g);
		}
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

	private int preferredWidth = -1;

	public void setPreferredWidth(int preferredWidth)
	{
		this.preferredWidth = preferredWidth;
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension size = getSize();
		Insets insets = getInsets();
		Dimension preferred;
		if (preferredWidth < 0)
		{
			preferred = super.getPreferredSize(); // use the preferred width, change the height
		}
		else
		{
			preferred = new Dimension();
			preferred.width = preferredWidth;
			size.width = preferredWidth;
		}

		View htmlView = getViewThatIsAbleToWrap();
		if (htmlView == null)
		{
			return super.getPreferredSize();
		}

		Rectangle r = getTextBoundsForSize(size);

		htmlView.setSize(r.width, r.height);
		preferred.height = (int)htmlView.getPreferredSpan(View.Y_AXIS) + insets.top + insets.bottom;
		return preferred;
	}

	private View getViewThatIsAbleToWrap()
	{
		View htmlView = (View)getClientProperty(BasicHTML.propertyKey);
		if (htmlView != null)
		{
			htmlView = htmlView.getView(0);
		}
		return htmlView;
	}

	private Rectangle getTextBoundsForSize(Dimension size)
	{
		Insets insets = getInsets();
		Rectangle vr = new Rectangle(insets.left, insets.top, size.width - insets.right - insets.left, size.height - insets.bottom - insets.top);
		Rectangle r = new Rectangle();
		SwingUtilities.layoutCompoundLabel(this, getFontMetrics(getFont()), getText(), getIcon(), getVerticalAlignment(), getHorizontalAlignment(),
			getVerticalTextPosition(), getHorizontalTextPosition(), vr, new Rectangle(), r, getIconTextGap());
		return r;
	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}

	public void setFocusPainted(boolean showFocus)
	{
		if (showFocus)
		{
			final SpecialMatteBorder border = new SpecialMatteBorder(1, 1, 1, 1, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
			border.setDashPattern(new float[] { 1 });
			addFocusListener(new FocusListener()
			{
				public void focusGained(FocusEvent e)
				{
					if (getBorder() == null) setBorder(border);
				}

				public void focusLost(FocusEvent e)
				{
					if (getBorder() == border) setBorder(null);
				}
			});
		}
	}

	/*
	 * _____________________________________________________________ Methods for javascript
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
			if (image_url != null) setRolloverIcon(new ImageIcon(new URL(image_url)));
			else setRolloverIcon((ImageIcon)null);
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

	public String getParameterValue(String param)
	{
		// TODO catch the submit of a html that is displayed. 
		return null;
	}

	public byte[] getThumbnailJPGImage(int width, int height)
	{
		return AbstractScriptButton.getThumbnailJPGImage(this, width, height, getIcon());
	}

	public void setComponentVisible(boolean b_visible)
	{
		if (viewable)
		{
			setVisible(b_visible);
		}
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
				disabledIcon = new IconUIResource(new ImageIcon(GrayFilter.createDisabledImage(scaledIcon.getImage())));
				return disabledIcon;
			}
		}
		return super.getDisabledIcon();
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

	/**
	 * processed on background thread and makes it scale if to-big/small
	 */
	@Override
	public void setIcon(Icon icon)
	{
		if (normalIcon == null && icon == null)
		{
			// no change or not initialized yet
			super.setIcon(icon);
		}
		else
		{
			setIconDirect(icon, getNextSeq());
		}
	}

	/**
	 * processed on background thread and makes it scale if to-big/small
	 */
	public void setIcon(byte[] data)
	{
		setIconDirect(data, getNextSeq());
	}

	public void setIconDirect(byte[] data, int seq)
	{
		if (imageSeq == seq)
		{
			ComponentFactory.deregisterIcon(normalIcon);
			if (data != null)
			{
				disabledIcon = null;
				// media option 1 == crop so no scaling
				if (mediaOption != 1)
				{
					normalIcon = new MyImageIcon(application, this, data, mediaOption);
				}
				else
				{
					normalIcon = ImageLoader.getIcon(data, -1, -1, true);
				}
				ComponentFactory.registerIcon(normalIcon);
			}
			else
			{
				normalIcon = null;
			}
			if (application.getI18NMessage("servoy.imageMedia.loadingImage").equals(getText())) //$NON-NLS-1$
			{
				setText(null);
			}

			try
			{
				application.invokeAndWait(new Runnable()
				{
					public void run()
					{
						AbstractScriptLabel.super.setIcon(normalIcon);
					}
				});
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	/**
	 * show processed on background thread, is put on event thread
	 */
	public void setIconDirect(Icon icon, int seq)
	{
		if (imageSeq == seq)
		{
			ComponentFactory.deregisterIcon(normalIcon);

			disabledIcon = null;
			// media option 1 == crop so no scaling
			if (mediaOption != 1 && icon instanceof ImageIcon)
			{
				normalIcon = new MyImageIcon(application, this, (ImageIcon)icon, mediaOption);
			}
			else
			{
				normalIcon = icon;
			}
			ComponentFactory.registerIcon(normalIcon);
			if (application.getI18NMessage("servoy.imageMedia.loadingImage").equals(getText())) //$NON-NLS-1$
			{
				setText(null);
			}
			try
			{
				application.invokeAndWait(new Runnable()
				{
					public void run()
					{
						AbstractScriptLabel.super.setIcon(normalIcon);
					}
				});
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	private int imageSeq = 1;

	int getNextSeq()
	{
		imageSeq++;
		return imageSeq;
	}

	private Icon normalIcon;
	private Icon rolloverIcon;

	public void setRolloverIcon(byte[] data)
	{
		ComponentFactory.deregisterIcon(rolloverIcon);

		if (mediaOption != 1 && data != null)
		{
			rolloverIcon = new MyImageIcon(application, this, data, mediaOption);
		}
		else
		{
			rolloverIcon = ImageLoader.getIcon(data, -1, -1, true);
		}

		if (rolloverIcon != null)
		{
			ComponentFactory.registerIcon(rolloverIcon);
			addRolloverListener();
		}
	}

	public void setRolloverIcon(ImageIcon icon)
	{
		ComponentFactory.deregisterIcon(rolloverIcon);

		if (mediaOption != 1 && icon != null)
		{
			rolloverIcon = new MyImageIcon(application, this, icon, mediaOption);
		}
		else
		{
			rolloverIcon = icon;
		}
		if (rolloverIcon != null) ComponentFactory.registerIcon(rolloverIcon);
		addRolloverListener();
	}

	private void addRolloverListener()
	{
		if (rolloverMouseAdapter == null)
		{
			rolloverMouseAdapter = new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					AbstractScriptLabel.super.setIcon(rolloverIcon);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					AbstractScriptLabel.super.setIcon(normalIcon);
				}
			};
			addMouseListener(rolloverMouseAdapter);
		}
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

	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public boolean hasActionCommand()
	{
		return eventExecutor.hasActionCmd();
	}

	private Timer clickTimer;

	/**
	 * @see com.servoy.j2db.ui.ILabel#setActionCommand(java.lang.String, Object[])
	 */
	public void setActionCommand(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
		if (id != null && actionMouseAdapter == null)
		{
			setFocusable(true);
			setRequestFocusEnabled(true);
			actionMouseAdapter = new MouseAdapter()
			{
				private boolean armed = false;
				// simulates multiClickThreshhold of button
				private long lastPressedTimestamp = 0;
				private boolean shouldDiscardRelease = false;

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.awt.event.MouseAdapter#mouseEntered(java.awt.event.MouseEvent)
				 */
				@Override
				public void mouseEntered(MouseEvent e)
				{
					armed = true;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.awt.event.MouseAdapter#mouseExited(java.awt.event.MouseEvent)
				 */
				@Override
				public void mouseExited(MouseEvent e)
				{
					armed = false;
				}

				/*
				 * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
				 */
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e)) requestFocus();
					armed = true;
					if (doubleClickMouseAdapter != null)
					{
						long lastTime = lastPressedTimestamp;
						long currentTime = lastPressedTimestamp = e.getWhen();
						if (currentTime - lastTime < UIUtils.getClickInterval())
						{
							shouldDiscardRelease = true;
							return;
						}
					}
				}

				@Override
				public void mouseReleased(final MouseEvent e)
				{
					if (shouldDiscardRelease)
					{
						shouldDiscardRelease = false;
						return;
					}
					if (SwingUtilities.isLeftMouseButton(e) && isEnabled() && armed)
					{
						if (doubleClickMouseAdapter != null)
						{
							clickTimer = new Timer(UIUtils.getClickInterval(), new ActionListener()
							{
								public void actionPerformed(ActionEvent ev)
								{
									eventExecutor.fireActionCommand(true, AbstractScriptLabel.this, e.getModifiers(), e.getPoint());
								}
							});
							clickTimer.setRepeats(false); //after expiring once, stop the timer
							clickTimer.start();
						}
						else
						{
							eventExecutor.fireActionCommand(true, AbstractScriptLabel.this, e.getModifiers(), e.getPoint());
						}
					}
				}
			};
			addMouseListener(actionMouseAdapter);
		}
	}

	public void setDoubleClickCommand(String id, Object[] args)
	{
		eventExecutor.setDoubleClickCmd(id, args);
		if (id != null && doubleClickMouseAdapter == null)
		{
			doubleClickMouseAdapter = new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					// Don't allow double click with other buttons except left button.
					if ((e.getClickCount() == 2) && SwingUtilities.isLeftMouseButton(e) && isEnabled())
					{
						if (clickTimer != null) clickTimer.stop();
						eventExecutor.fireDoubleclickCommand(true, AbstractScriptLabel.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(doubleClickMouseAdapter);
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
						eventExecutor.fireRightclickCommand(true, AbstractScriptLabel.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(rightclickMouseAdapter);
		}
	}

	public void setValidationEnabled(boolean b)
	{
		eventExecutor.setValidationEnabled(b);
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
					sb.append(' ');
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
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	public int getFontSize()
	{
		return 0;
	}
}
