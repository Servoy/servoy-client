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
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ILinkListener;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.ByteArrayResource;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ILabelForMethods;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptHtmlSubmitLabelsMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.JpegEncoder;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Base class for simples labels.
 * 
 * @author jcompagner,jblok
 */
public class WebBaseLabel extends Label implements ILabel, IScriptHtmlSubmitLabelsMethods, IResourceListener, IProviderStylePropertyChanges, ILabelForMethods,
	IDoubleClickListener, IRightClickListener, ISupportWebBounds
{
	private static final long serialVersionUID = 1L;

	private int rotation;
	private boolean showFocus;
	private int halign;
	private int valign;
	private Cursor cursor;

	protected MediaResource icon;
	private int mediaOptions;
	private AttributeModifier imageStyle;
	private ResourceReference iconReference;
	private ResourceReference rolloverIconReference;
	private String iconUrl;
	private Media media;
	private Media rolloverMedia;
	protected final IApplication application;
	protected ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, TemplateGenerator.DEFAULT_LABEL_PADDING);
	private String text_url;
	private String rolloverUrl;
	private ServoyAjaxEventBehavior rolloverBehavior;

	protected IFieldComponent labelForComponent;
	private final WebEventExecutor eventExecutor;

	/**
	 * @param id
	 */
	public WebBaseLabel(IApplication application, String id)
	{
		super(id);
		this.application = application;
		setEscapeModelStrings(false);
		setOutputMarkupPlaceholderTag(true);
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
	}

	/**
	 * @param id
	 * @param label
	 */
	public WebBaseLabel(IApplication application, String id, String label)
	{
		this(application, id);
		setDefaultModel(new Model<String>(label));
	}

	/**
	 * @param id
	 * @param model
	 */
	public WebBaseLabel(IApplication application, String id, IModel model)
	{
		this(application, id);
		setDefaultModel(model);
	}

	/**
	 * @see org.apache.wicket.Component#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return application.getLocale();
	}

	/**
	 * @see wicket.markup.html.WebComponent#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
	}

	/**
	 * @see wicket.IResourceListener#onResourceRequested()
	 */
	public void onResourceRequested()
	{
		String media = RequestCycle.get().getRequest().getParameter("media"); //$NON-NLS-1$
		if (media != null)
		{
			Media m;
			try
			{
				m = application.getFlattenedSolution().getMedia(media);
				byte[] bytes = m.getMediaData();
				new ByteArrayResource(ImageLoader.getContentType(bytes), bytes, null).onResourceRequested();
			}
			catch (Exception ex)
			{
				Debug.error("Error serving media: " + media, ex); //$NON-NLS-1$
			}
		}
		else if (getRequest().getParameter(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER) != null)
		{
			String url = getRequest().getRelativeURL();
			try
			{
				byte[] bytes = MediaURLStreamHandler.getBlobLoaderMedia(application, url);
				if (bytes != null)
				{
					String mime = MediaURLStreamHandler.getBlobLoaderMimeType(url);
					if (mime == null) mime = ImageLoader.getContentType(bytes);
					String filename = MediaURLStreamHandler.getBlobLoaderFileName(url);
					if (size != null)
					{
						MediaResource tempIcon = new MediaResource(bytes, mediaOptions);
						(tempIcon).checkResize(size);
						bytes = tempIcon.resized;
					}
					new ByteArrayResource(mime, bytes, filename).onResourceRequested();

				}
			}
			catch (IOException ex)
			{
				Debug.error("Error serving blobloader url: " + url, ex); //$NON-NLS-1$
			}
		}
		else
		{
			icon.onResourceRequested();
		}
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
		return jsChangeRecorder;
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
	public void setRolloverIcon(int rolloverId)
	{
		if ((rolloverMedia = application.getFlattenedSolution().getMedia(rolloverId)) != null)
		{
			addRolloverBehaviors();
			rolloverIconReference = new ResourceReference("media"); //$NON-NLS-1$ 
		}
	}

	public void setIcon(final byte[] bs)
	{
		media = null;
		iconReference = null;
		if (bs != null && bs.length != 0)
		{
			addImageStyleAttributeModifier();
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

	private void addRolloverBehaviors()
	{
		if (rolloverBehavior != null) return;

		rolloverBehavior = new ServoyAjaxEventBehavior("onmouseover")
		{
			@Override
			protected CharSequence generateCallbackScript(CharSequence partialCall)
			{
				String solutionName = application.getSolution().getName();
				String url = "";
				if (rolloverIconReference != null && rolloverMedia != null)
				{
					if (mediaOptions != 0 && mediaOptions != 1)
					{
						url = urlFor(rolloverIconReference) + "?id=" + rolloverMedia.getName() + "&s=" + solutionName + "&option=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
							mediaOptions + "&w=" + getSize().width + "&h=" + getSize().height + "&l=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							(rolloverMedia.getMediaData() != null ? +rolloverMedia.getMediaData().hashCode() : 0);
					}
					else
					{
						url = urlFor(rolloverIconReference) + "?id=" + rolloverMedia.getName() + "&s=" + solutionName + "&l=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
							(rolloverMedia.getMediaData() != null ? +rolloverMedia.getMediaData().hashCode() : 0);
					}
				}
				else if (rolloverUrl != null)
				{
					if (rolloverUrl.startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
					{
						String mediaName = rolloverUrl.substring(MediaURLStreamHandler.MEDIA_URL_DEF.length());
						if (mediaName.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
						{
							url = RequestCycle.get().urlFor(WebBaseLabel.this, IResourceListener.INTERFACE) +
								"&" + MediaURLStreamHandler.MEDIA_URL_BLOBLOADER + "=true&" + //$NON-NLS-1$ 
								mediaName.substring((MediaURLStreamHandler.MEDIA_URL_BLOBLOADER + "?").length()); //$NON-NLS-1$ 
						}
					}
					else url = rolloverUrl;
				}
				url = "url(" + url + ")";
				return "Servoy.Rollover.onMouseOver('" + WebBaseLabel.this.getMarkupId() + "','" + url + "','" + getBackgroundPosition() + "','" +
					getBackgroundRepeat() + "')";
			}

			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				// not used, client side implementation
			}
		};
		add(rolloverBehavior);
		ServoyAjaxEventBehavior mouseoutBehavior = new ServoyAjaxEventBehavior("onmouseout")
		{
			@Override
			protected CharSequence generateCallbackScript(CharSequence partialCall)
			{
				return "Servoy.Rollover.onMouseOut('" + WebBaseLabel.this.getMarkupId() + "')";
			}

			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				// not used, client side implementation
			}
		};
		add(mouseoutBehavior);
	}

	private String getBackgroundRepeat()
	{
		return "no-repeat";
	}

	private String getBackgroundPosition()
	{
		String position = "center center"; //$NON-NLS-1$ 
		String text = WebBaseLabel.this.getDefaultModelObjectAsString();
		if (text != null && text.length() != 0)
		{
			position = "center left"; //$NON-NLS-1$ 
		}
		else
		{
			if (valign == SwingConstants.TOP)
			{
				position = "top "; //$NON-NLS-1$
			}
			else if (valign == SwingConstants.BOTTOM)
			{
				position = "bottom "; //$NON-NLS-1$ 
			}
			else
			{
				position = "center "; //$NON-NLS-1$ 
			}

			if (halign == SwingConstants.LEFT)
			{
				position += "left"; //$NON-NLS-1$ 
			}
			else if (halign == SwingConstants.RIGHT)
			{
				position += "right"; //$NON-NLS-1$ 
			}
			else
			{
				position += "center"; //$NON-NLS-1$ 
			}
		}
		return position;
	}

	private void addImageStyleAttributeModifier()
	{
		if (imageStyle != null) return;

		imageStyle = new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				String position = getBackgroundPosition();
				String repeat = getBackgroundRepeat();

				String styleAttribute = null;
				if (icon != null)
				{
					CharSequence url = urlFor(IResourceListener.INTERFACE) + "&r=" + Math.random(); //$NON-NLS-1$
					styleAttribute = "background-image: url(" + getResponse().encodeURL(url) + "); background-repeat: no-repeat; background-position: " + position; //$NON-NLS-1$//$NON-NLS-2$
				}
				else if (iconReference != null && media != null)
				{
					String solutionName = application.getSolution().getName();
					if (mediaOptions != 0 && mediaOptions != 1)
					{
						styleAttribute = "background-image: url(" + urlFor(iconReference) + "?id=" + media.getName() + "&s=" + solutionName + "&option=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							mediaOptions +
							"&w=" + getSize().width + "&h=" + getSize().height + "&l=" + (media.getMediaData() != null ? +media.getMediaData().hashCode() : 0) + "); background-repeat: " + repeat + "; background-position: " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							position;
					}
					else
					{
						styleAttribute = "background-image: url(" + urlFor(iconReference) + "?id=" + media.getName() + "&s=" + solutionName + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							"&l=" + (media.getMediaData() != null ? +media.getMediaData().hashCode() : 0) + //$NON-NLS-1$
							"); background-repeat: " + repeat + "; background-position: " + position; //$NON-NLS-1$
					}
				}
				else if (iconUrl != null)
				{
					styleAttribute = "background-image: url(" + iconUrl + "); background-repeat: " + repeat + "; background-position: " + position; //$NON-NLS-1$ //$NON-NLS-2$

				}
				else if (text_url != null)
				{
					String mediaName = text_url.substring(MediaURLStreamHandler.MEDIA_URL_DEF.length());
					if (mediaName.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
					{
						String url = RequestCycle.get().urlFor(WebBaseLabel.this, IResourceListener.INTERFACE) +
							"&" + MediaURLStreamHandler.MEDIA_URL_BLOBLOADER + "=true&" + //$NON-NLS-1$
							mediaName.substring((MediaURLStreamHandler.MEDIA_URL_BLOBLOADER + "?").length()); //$NON-NLS-1$
						styleAttribute = "background-image: url(" + url + "); background-repeat: " + repeat + "; background-position: " + position; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				if (!js_isEnabled())
				{
					styleAttribute += "; filter:alpha(opacity=50);-moz-opacity:.50;opacity:.50"; //$NON-NLS-1$
				}
				return styleAttribute;
			}
		});
		add(imageStyle);
	}

	public int getMediaIcon()
	{
		return media != null ? media.getID() : 0;
	}

	public void setMediaIcon(int iconId)
	{
		this.icon = null;
		this.iconUrl = null;
		this.iconReference = null;
		if ((media = application.getFlattenedSolution().getMedia(iconId)) != null)
		{
			addImageStyleAttributeModifier();
			iconReference = new ResourceReference("media"); //$NON-NLS-1$
		}
		else if (imageStyle != null)
		{
			remove(imageStyle);
			imageStyle = null;
		}
	}

	public void setRotation(int rotation)
	{
		this.rotation = rotation;
	}

	public void setFocusPainted(boolean showFocus)
	{
		this.showFocus = showFocus;
	}

	public void setHorizontalAlignment(int c)
	{
		this.halign = c;
	}

	public void setVerticalAlignment(int t)
	{
		this.valign = t;
	}

	public void setText(String txt)
	{
		setDefaultModel(new Model<String>(txt));
	}

	public String getText()
	{
		Object o = getDefaultModelObject();
		return o == null ? null : o.toString();
	}

	public void setSize(Dimension size)
	{
		this.size = size;
		if (size != null && icon != null)
		{
			icon.checkResize(size);
		}
	}

	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	@Override
	public IConverter getConverter(Class< ? > cls)
	{
		if (cls.isArray() && cls.getComponentType().toString().equals("byte")) return new IConverter() //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			public Object convertToObject(String value, Locale locale)
			{
				return null;
			}

			public String convertToString(Object value, Locale locale)
			{
				return null;
			}
		};
		return StripHTMLTagsConverter.htmlStripper;
	}

	public void setTextTransform(int mode)
	{
		// ignore should be done through css
	}

	/*
	 * jsmethods---------------------------------------------------
	 */
	public void js_setImageURL(String textUrl)
	{
		this.text_url = textUrl;
		if (textUrl == null)
		{
			icon = null;
			iconReference = null;
			iconUrl = null;
		}
		else
		{
			int index = textUrl.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (index == -1)
			{
				icon = null;
				iconReference = null;
				iconUrl = textUrl;
				addImageStyleAttributeModifier();
			}
			else
			{
				String mediaName = textUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				try
				{
					Media med = application.getFlattenedSolution().getMedia(mediaName);
					if (med != null)
					{
						setMediaIcon(med.getID());
					}
					else if (mediaName.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
					{
						// clear previous images
						icon = null;
						iconReference = null;
						iconUrl = null;
						addImageStyleAttributeModifier();
					}
				}
				catch (Exception ex)
				{
					Debug.error("Error loading media for url: " + textUrl, ex); //$NON-NLS-1$
				}
			}
		}
		jsChangeRecorder.setChanged();
	}

	public String getImageURL()
	{
		return text_url;
	}

	public String getRolloverImageURL()
	{
		return rolloverUrl;
	}

	public void js_setRolloverImageURL(String imageUrl)
	{
		this.rolloverUrl = imageUrl;
		rolloverIconReference = null;
		if (rolloverUrl != null)
		{
			int index = imageUrl.indexOf(MediaURLStreamHandler.MEDIA_URL_DEF);
			if (index != -1)
			{
				String nm = rolloverUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				Media m = application.getFlattenedSolution().getMedia(nm);
				if (m != null)
				{
					setRolloverIcon(m.getID());
				}
			}
		}
		addRolloverBehaviors();
		jsChangeRecorder.setChanged();
	}

	public byte[] js_getThumbnailJPGImage(Object[] args)
	{
		return getThumbnailJPGImage(args, icon, text_url, media != null ? media.getID() : 0, (mediaOptions & 8) == 8, application);
	}

	public static byte[] getThumbnailJPGImage(Object[] args, MediaResource icon, String text_url, int iconId, boolean keepAspectRatio, IApplication application)
	{
		Image sourceImage = null;
		byte[] sourceRawData = null;
		int width = -1;
		int height = -1;
		if (args != null && args.length == 2)
		{
			width = Utils.getAsInteger(args[0]);
			height = Utils.getAsInteger(args[1]);
		}

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
					else if (name.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER)) sourceRawData = MediaURLStreamHandler.getBlobLoaderMedia(application,
						text_url);
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
		else if (iconId > 0)
		{
			Media media = application.getFlattenedSolution().getMedia(iconId);
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
	public String js_getParameterValue(String param)
	{
		RequestCycle cycle = RequestCycle.get();
		if (cycle != null)
		{
			Request req = cycle.getRequest();
			if (req != null)
			{
				return req.getParameter(param);
			}
		}
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
	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public void setName(String n)
	{
		name = n;
	}

	private String name;

	public String getName()
	{
		return name;
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	public boolean js_isTransparent()
	{
		return !opaque;
	}

	public void js_setTransparent(boolean b)
	{
		opaque = !b;
		jsChangeRecorder.setTransparent(b);
	}

	public boolean isOpaque()
	{
		return opaque;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return IScriptBaseMethods.LABEL;
	}

	public String js_getDataProviderID()
	{
		//default implementation
		return null;
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
		if (titleText != null && getDefaultModel() instanceof ITagResolver)
		{
			return Text.processTags(titleText, (ITagResolver)getDefaultModel());
		}
		return titleText;
	}

	/*
	 * tooltip---------------------------------------------------
	 */
	public String js_getToolTipText()
	{
		return tooltip;
	}

	private String tooltip;

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

	public void js_setToolTipText(String tip)
	{
		setToolTipText(tip);
		jsChangeRecorder.setChanged();
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		if (tooltip != null && getDefaultModel() instanceof ITagResolver)
		{
			final ITagResolver resolver = (ITagResolver)getDefaultModel();
			return Text.processTags(tooltip, resolver);
		}
		return tooltip;
	}

	/*
	 * font---------------------------------------------------
	 */
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	public void js_setFont(String spec)
	{
		font = PersistHelper.createFont(spec);
		jsChangeRecorder.setFont(spec);
	}

	public String js_getFont()
	{
		return PersistHelper.createFontString(font);
	}

	public Font getFont()
	{
		return font;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(background);
	}

	public void js_setBgcolor(String bgcolor)
	{
		background = PersistHelper.createColor(bgcolor);
		jsChangeRecorder.setBgcolor(bgcolor);
	}

	private Color background;

	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	public Color getBackground()
	{
		return background;
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(foreground);
	}

	public void js_setFgcolor(String fgcolor)
	{
		foreground = PersistHelper.createColor(fgcolor);
		jsChangeRecorder.setFgcolor(fgcolor);
	}

	private Color foreground;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	public Color getForeground()
	{
		return foreground;
	}


	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
		jsChangeRecorder.setBorder(spec);
	}

	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getBorder());
	}

	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean visible)
	{
		setVisible(visible);
		jsChangeRecorder.setVisible(visible);
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
			jsChangeRecorder.setChanged();
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
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

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
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public void js_setLocation(int x, int y)
	{
		location = new Point(x, y);
		jsChangeRecorder.setLocation(x, y);
	}

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public Point getLocation()
	{
		return location;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	private Map<Object, Object> clientProperties;

	public Object js_getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	private char mnemonic;


	public Dimension getSize()
	{
		return size;
	}

	public void js_setSize(int width, int height)
	{
		setSize(new Dimension(width, height));
		jsChangeRecorder.setSize(width, height, border, null, getFontSize(), valign);
	}

	protected int getFontSize()
	{
		int fontSize = TemplateGenerator.DEFAULT_FONT_SIZE;
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
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, getBorder(), null, getFontSize(), null, valign);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, getBorder(), null, getFontSize(), null, valign);
	}


	public int js_getWidth()
	{
		return size.width;
	}

	public int js_getHeight()
	{
		return size.height;
	}

	public String js_getLabelForElementName()
	{
		if (labelForComponent != null) return labelForComponent.getName();
		return null;
	}

	public String js_getMnemonic()
	{
		if (mnemonic == 0) return "";
		return new Character(mnemonic).toString();
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
	 * @see com.servoy.j2db.ui.IStandardLabel#setDisplayedMnemonic(char)
	 */
	public void setDisplayedMnemonic(char mnemonic)
	{
		this.mnemonic = mnemonic;
	}

	public void setLabelFor(IFieldComponent component)
	{
		this.labelForComponent = component;
	}

	// Searches for a parent form, up the hierarchy of controls in the page.
	private Form getForm()
	{
		Component c = this;
		while ((c != null) && !(c instanceof Form))
			c = c.getParent();
		return (Form)c;
	}

	@Override
	protected void onComponentTag(final ComponentTag tag)
	{
		super.onComponentTag(tag);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		if (useAJAX)
		{
			Object oe = js_getClientProperty("ajax.enabled");
			if (oe != null) useAJAX = Utils.getAsBoolean(oe);
		}

		if (!useAJAX)
		{
			Form f = getForm();
			if (f != null)
			{
				if (eventExecutor.hasActionCmd())
				{
					CharSequence url = urlFor(ILinkListener.INTERFACE);
					tag.put("onclick", f.getJsForInterfaceUrl(url)); //$NON-NLS-1$
				}

				if (eventExecutor.hasDoubleClickCmd())
				{
					CharSequence urld = urlFor(IDoubleClickListener.INTERFACE);
					tag.put("ondblclick", f.getJsForInterfaceUrl(urld)); //$NON-NLS-1$
				}

				if (eventExecutor.hasRightClickCmd())
				{
					CharSequence urlr = urlFor(IRightClickListener.INTERFACE);
					// We need a "return false;" so that the context menu is not displayed in the browser.
					tag.put("oncontextmenu", f.getJsForInterfaceUrl(urlr) + " return false;"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	public void onDoubleClick()
	{
		Form f = getForm();
		if (f != null)
		{
			// If form validation fails, we don't execute the method.
			if (f.process()) eventExecutor.onEvent(JSEvent.EventType.doubleClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
	}

	public void onRightClick()
	{
		Form f = getForm();
		if (f != null)
		{
			// If form validation fails, we don't execute the method.
			if (f.process()) eventExecutor.onEvent(JSEvent.EventType.rightClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",label:" + getText() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	protected void instrumentAndReplaceBody(MarkupStream markupStream, ComponentTag openTag, CharSequence bodyText)
	{
		replaceComponentTagBody(markupStream, openTag, WebBaseButton.instrumentBodyText(bodyText, halign, valign));
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		if (eventExecutor != null)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			eventExecutor.fireOnRender(this, isFocused);
		}
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getRenderEventExecutor()
	 */
	public RenderEventExecutor getRenderEventExecutor()
	{
		return eventExecutor;
	}
}
