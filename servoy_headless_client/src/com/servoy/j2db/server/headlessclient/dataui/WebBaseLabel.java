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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.apache.wicket.Component;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IAnchoredComponent;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.JpegEncoder;

/**
 * Base class for simples labels.
 *
 * @author jcompagner,jblok
 */
public class WebBaseLabel extends Component implements ILabel, IProviderStylePropertyChanges,
	ISupportWebBounds, IImageDisplay, IAnchoredComponent, ISupportSimulateBoundsProvider, ISupportOnRender
{
	private static final long serialVersionUID = 1L;

//	private int rotation;
//	private boolean showFocus;
	private int halign;
	private int valign;
	private Cursor cursor;

	protected MediaResource icon;
	private int mediaOptions;
	private String iconUrl;
	private Media media;
//	private Dimension mediaSize;
	private Media rolloverMedia;
	private int anchors = 0;
	protected final IApplication application;
	private String text_url;
	private String rolloverUrl;

	protected IFieldComponent labelForComponent;
	protected final WebEventExecutor eventExecutor;
	private final AbstractRuntimeBaseComponent< ? extends IComponent> scriptable;

	public WebBaseLabel(IApplication application, AbstractRuntimeBaseComponent< ? extends IComponent> scriptable, String id)
	{
		super(id);
		this.application = application;
		this.scriptable = scriptable;

		halign = ISupportTextSetup.LEFT; // default horizontal align
		valign = ISupportTextSetup.CENTER; // default vertical align

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
	}

	/**
	 * @param id
	 * @param label
	 */
	public WebBaseLabel(IApplication application, AbstractRuntimeBaseComponent< ? extends IComponent> scriptable, String id, String label)
	{
		this(application, scriptable, id);
	}

	public final AbstractRuntimeBaseComponent< ? extends IComponent> getScriptObject()
	{
		return scriptable;
	}

	protected CharSequence getBodyText()
	{
		return getText();
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setMediaOption(int)
	 */
	public void setMediaOption(int mediaOptions)
	{
		this.mediaOptions = mediaOptions;
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#addScriptExecuter(com.servoy.j2db.IScriptExecuter)
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return scriptable.getChangesRecorder();
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setActionCommand(java.lang.String, Object[])
	 */
	public void setActionCommand(String actionCommand, Object[] args)
	{
		eventExecutor.setActionCmd(actionCommand, args);
	}

	public void setDoubleClickCommand(String doubleClickCommand, Object[] args)
	{
		eventExecutor.setDoubleClickCmd(doubleClickCommand, args);
	}

	public void setRightClickCommand(String rightClickCommand, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCommand, args);
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setRolloverIcon(byte[])
	 */
	public void setRolloverIcon(String rolloverUUID)
	{
	}

	public void setIcon(final byte[] bs)
	{
		media = null;
//		mediaSize = null;
		if (bs != null && bs.length != 0)
		{
			icon = new MediaResource(bs, mediaOptions);
			if (size != null)
			{
				(icon).checkResize(size);
			}
		}
		else
		{
			icon = new MediaResource(WebDataImgMediaField.emptyImage, mediaOptions);
		}
	}

	public String getMediaIcon()
	{
		return media != null ? media.getUUID().toString() : null;
	}

	public void setMediaIcon(String iconUUID)
	{
		this.icon = null;
		this.iconUrl = null;
		if ((media = application.getFlattenedSolution().getMedia(iconUUID)) != null)
		{
			text_url = MediaURLStreamHandler.MEDIA_URL_DEF + media.getName();
		}

	}

	private int rotation = 0;

	private String txt;

	/**
	 * @see com.servoy.j2db.ui.ILabel#setRotation(int)
	 */
	public void setRotation(int rotation)
	{
		this.rotation = rotation;
	}

	public void setFocusPainted(boolean showFocus)
	{
//		this.showFocus = showFocus;
	}

	public void setHorizontalAlignment(int c)
	{
		this.halign = c;
	}

	public void setVerticalAlignment(int t)
	{
		this.valign = t;
	}

	public int getVerticalAlignment()
	{
		return valign;
	}

	public void setText(String txt)
	{
		this.txt = txt;
	}

	public String getText()
	{
		return this.txt;
	}

	@Override
	public void setSize(Dimension size)
	{
		this.size = size;
		if (size != null && icon != null)
		{
			icon.checkResize(size);
		}
	}

	@Override
	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	public void setTextTransform(int mode)
	{
		// ignore should be done through css
	}

	public void setImageURL(String textUrl)
	{
		this.text_url = textUrl;
		if (textUrl == null)
		{
			icon = null;
			iconUrl = null;
		}
		else
		{
			int index = textUrl.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (index == -1)
			{
				icon = null;
				iconUrl = textUrl;
			}
			else
			{
				String mediaName = textUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				try
				{
					Media med = application.getFlattenedSolution().getMedia(mediaName);
					if (med != null)
					{
						setMediaIcon(med.getUUID().toString());
					}
					else if (mediaName.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
					{
						// clear previous images
						icon = null;
						iconUrl = null;
					}
				}
				catch (Exception ex)
				{
					Debug.error("Error loading media for url: " + textUrl, ex); //$NON-NLS-1$
				}
			}
		}
	}

	public String getImageURL()
	{
		return text_url;
	}

	public String getRolloverImageURL()
	{
		return rolloverUrl;
	}

	public void setRolloverImageURL(String imageUrl)
	{
		this.rolloverUrl = imageUrl;
		if (rolloverUrl != null)
		{
			int index = imageUrl.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (index != -1)
			{
				String nm = rolloverUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				Media m = application.getFlattenedSolution().getMedia(nm);
				if (m != null)
				{
					setRolloverIcon(m.getUUID().toString());
				}
			}
		}
	}

	public byte[] getThumbnailJPGImage(int width, int height)
	{
		return getThumbnailJPGImage(width, height, icon, text_url, media != null ? media.getUUID().toString() : null, (mediaOptions & 8) == 8, application);
	}

	public static byte[] getThumbnailJPGImage(int width, int height, MediaResource icon, String text_url, String iconUUID, boolean keepAspectRatio,
		IApplication application)
	{
		Image sourceImage = null;
		byte[] sourceRawData = null;

		if (icon != null)
		{
			sourceRawData = icon.getRawData();
		}
		else if (text_url != null)
		{
			int index = text_url.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			// If it is a media URL.
			if (index != -1)
			{
				String name = text_url.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				try
				{
					Media media = application.getFlattenedSolution().getMedia(name);
					if (media != null) sourceRawData = media.getMediaData();
					else if (name.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
						sourceRawData = MediaURLStreamHandler.getBlobLoaderMedia(application, text_url);
				}
				catch (Exception ex)
				{
					Debug.error("Error loading media for URL: " + text_url, ex); //$NON-NLS-1$
				}
			}
			// If it is a regular, non-media, URL.
			else
			{
				try
				{
					URL url = new URL(text_url);
					ImageIcon iicon = new ImageIcon(url);
					iicon = ImageLoader.resizeImageIcon(iicon, width, height, keepAspectRatio);
					sourceImage = iicon.getImage();
				}
				catch (Exception ex)
				{
					Debug.error("Error loading icon from URL: " + text_url, ex); //$NON-NLS-1$
				}
			}
		}
		else if (iconUUID != null)
		{
			Media media = application.getFlattenedSolution().getMedia(iconUUID);
			if (media != null) sourceRawData = media.getMediaData();
		}

		if ((sourceImage == null) && (sourceRawData != null))
		{
			// don't get the directly scaled buffered image, it is not precise.
			sourceImage = ImageLoader.getBufferedImage(sourceRawData, -1, -1, true);
			ImageIcon iicon = new ImageIcon(sourceImage);
			iicon = ImageLoader.resizeImageIcon(iicon, width, height, keepAspectRatio);
			sourceImage = iicon.getImage();
		}

		byte[] jpegedRawData = null;
		if (sourceImage != null)
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JpegEncoder encoder = new JpegEncoder(sourceImage, 100, baos);
			encoder.compress();
			jpegedRawData = baos.toByteArray();
		}
		return jpegedRawData;
	}

	@Deprecated
	public String getParameterValue(String param)
	{
		return null;
	}

	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEnabled();
	}


	/*
	 * name---------------------------------------------------
	 */

	@Override
	public void setName(String n)
	{
		name = n;
	}

	private String name;

	@Override
	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	protected Border border;

	@Override
	public void setBorder(Border border)
	{
		this.border = border;
	}

	@Override
	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	@Override
	public boolean isOpaque()
	{
		return opaque;
	}

	/*
	 * titleText---------------------------------------------------
	 */

	protected String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

	private String tooltip;

	@Override
	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			this.tooltip = null;
		}
		else
		{
			this.tooltip = tooltip;
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		return tooltip;
	}

	/*
	 * font---------------------------------------------------
	 */
	@Override
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	@Override
	public Font getFont()
	{
		return font;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	private Color background;

	@Override
	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	@Override
	public Color getBackground()
	{
		return background;
	}


	/*
	 * fgcolor---------------------------------------------------
	 */

	private Color foreground;

	@Override
	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	@Override
	public Color getForeground()
	{
		return foreground;
	}


	/*
	 * visible---------------------------------------------------
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		if (viewable)
		{
			setVisible(visible);
		}
	}

	/*
	 * enabled---------------------------------------------------
	 */
	@Override
	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			scriptable.getChangesRecorder().setChanged();
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
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	@Override
	public void setLocation(Point location)
	{
		this.location = location;
	}

	@Override
	public Point getLocation()
	{
		return location;
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	private char mnemonic;


	@Override
	public Dimension getSize()
	{
		return size;
	}

	public int getFontSize()
	{
		int fontSize = 11;
		Font fnt = getFont();
		if (fnt != null)
		{
			fontSize = fnt.getSize();
			fontSize = (int)(fontSize * (4 / (double)3));
		}
		return fontSize;
	}

	public Rectangle getWebBounds()
	{
		Border b = border;
		Insets m = null;
		// empty border gets handled as margin
		if (b instanceof EmptyBorder)
		{
			m = ComponentFactoryHelper.getBorderInsetsForNoComponent(b);
			b = null;
		}
		Dimension d = ((ChangesRecorder)scriptable.getChangesRecorder()).calculateWebSize(size.width, size.height, b, m, getFontSize(), null, false, valign);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		Border b = border;
		Insets m = null;
		// empty border gets handled as margin
		if (b instanceof EmptyBorder)
		{
			m = ComponentFactoryHelper.getBorderInsetsForNoComponent(b);
			b = null;
		}
		return ((ChangesRecorder)scriptable.getChangesRecorder()).getPaddingAndBorder(size.height, b, m, getFontSize(), null, false, valign);
	}


	public IFieldComponent getLabelFor()
	{
		return labelForComponent;
	}

	/**
	 * @see com.servoy.j2db.ui.IStandardLabel#setDisplayedMnemonic(char)
	 */
	public void setDisplayedMnemonic(char mnemonic)
	{
		this.mnemonic = mnemonic;
	}

	public int getDisplayedMnemonic()
	{
		return mnemonic;
	}

	public void setLabelFor(IFieldComponent component)
	{
		this.labelForComponent = component;
	}

	@Override
	public String toString()
	{
		return scriptable.toString();
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	protected boolean hasHtmlOrImage()
	{
		if (!HtmlUtils.startsWithHtml(getText()))
		{
			return false;
		}
		return true;
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable instanceof ISupportOnRenderCallback)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			if (force) ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor().setRenderStateChanged();
			((ISupportOnRenderCallback)scriptable).getRenderEventExecutor().fireOnRender(isFocused);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getIcon()
	 */
	public MediaResource getIcon()
	{
		return icon;
	}

	public Media getMedia()
	{
		return media;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getMediaOptions()
	 */
	public int getMediaOptions()
	{
		return mediaOptions;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getIconUrl()
	 */
	public String getIconUrl()
	{
		return iconUrl;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getTextUrl()
	 */
	public String getTextUrl()
	{
		return text_url;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getRolloverMedia()
	 */
	public Media getRolloverMedia()
	{
		return rolloverMedia;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.IImageDisplay#getRolloverUrl()
	 */
	public String getRolloverUrl()
	{
		return rolloverUrl;
	}

	public void setAnchors(int arg)
	{
		this.anchors = arg;
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}
}
