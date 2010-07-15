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
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.server.headlessclient.ByteArrayResource;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IAccessible;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ILabelForMethods;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptHtmlSubmitLabelsMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Base class from labels with an action event. 
 * 
 * @author jcompagner, jblok
 */
public class WebBaseSubmitLink extends SubmitLink implements ILabel, IScriptHtmlSubmitLabelsMethods, IResourceListener, IProviderStylePropertyChanges,
	IAccessible, IAjaxIndicatorAware, ILabelForMethods, IDoubleClickListener, IRightClickListener, ISupportWebBounds, IButton
{
	private static final long serialVersionUID = 1L;

	private int halign;
	private int valign;
	private int mediaOptions;
	private int rotation;
	private boolean showFocus;
	private Cursor cursor;
	private Insets margin;
	private String inputId;

	protected MediaResource icon;
	private AttributeModifier imageStyle;
	private Media media;
	private Media rolloverMedia;
	private String iconUrl;
	private ResourceReference iconReference;
	private ResourceReference rolloverIconReference;
	protected final IApplication application;
	protected ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, TemplateGenerator.DEFAULT_LABEL_PADDING);
	private String text_url;
	private String rolloverUrl;
	private final WebEventExecutor eventExecutor;
	private ServoyAjaxEventBehavior rolloverBehavior;

	protected IFieldComponent labelForComponent;

	public WebBaseSubmitLink(IApplication application, String id)
	{
		super(id);
		this.application = application;
		setEscapeModelStrings(false);
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);

		add(new StyleAppendingModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				if (!isEnabled())
				{
					return "cursor: default;"; //$NON-NLS-1$
				}
				return null;
			}
		}));

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);
	}

	public WebBaseSubmitLink(IApplication application, String id, String label)
	{
		this(application, id);
		setText(label);
	}


	/**
	 * @see org.apache.wicket.Component#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return application.getLocale();
	}

	@Override
	public String getMarkupId()
	{
		return WebComponentSpecialIdMaker.getSpecialIdIfAppropriate(this);
	}

	/**
	 * @see org.apache.wicket.ajax.IAjaxIndicatorAware#getAjaxIndicatorMarkupId()
	 */
	public String getAjaxIndicatorMarkupId()
	{
		return "indicator"; //$NON-NLS-1$
	}

	/**
	 * @see wicket.markup.html.form.Button#onSubmit()
	 */
	@Override
	public void onSubmit()
	{
		eventExecutor.onEvent(EventType.action, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	/**
	 * @see wicket.MarkupContainer#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
	}

	/**
	 * @see wicket.MarkupContainer#onComponentTagBody(wicket.markup.MarkupStream, wicket.markup.ComponentTag)
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		instrumentAndReplaceBody(markupStream, openTag, getDefaultModelObjectAsString());
	}

	/**
	 * @see wicket.markup.html.form.FormComponent#getInputName()
	 */
	@Override
	public String getInputName()
	{
		if (inputId == null)
		{
			Page page = findPage();
			if (page instanceof MainPage)
			{
				inputId = ((MainPage)page).nextInputNameId();
			}
			else
			{
				return super.getInputName();
			}
		}
		return inputId;
	}

	/**
	 * @see wicket.IResourceListener#onResourceRequested()
	 */
	public void onResourceRequested()
	{
		String mediaName = RequestCycle.get().getRequest().getParameter("media"); //$NON-NLS-1$
		if (mediaName != null)
		{
			Media m;
			try
			{
				m = application.getFlattenedSolution().getMedia(mediaName);
				byte[] bytes = m.getMediaData();
				new ByteArrayResource(ImageLoader.getContentType(bytes), bytes, null).onResourceRequested();
			}
			catch (Exception ex)
			{
				Debug.error("Error serving media: " + mediaName, ex); //$NON-NLS-1$
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

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
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
	public void setRolloverIcon(int rolloverMediaId)
	{
		if ((rolloverMedia = application.getFlattenedSolution().getMedia(rolloverMediaId)) != null)
		{
			addRolloverBehaviors();
			rolloverIconReference = new ResourceReference("media"); //$NON-NLS-1$ 
		}
	}

	/**
	 * @see com.servoy.j2db.ui.ILabel#setIcon(byte[])
	 */
	public void setIcon(byte[] bs)
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
				String solutionName = J2DBGlobals.getServiceProvider().getSolution().getName();
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
							url = RequestCycle.get().urlFor(WebBaseSubmitLink.this, IResourceListener.INTERFACE) +
								"&" + MediaURLStreamHandler.MEDIA_URL_BLOBLOADER + "=true&" + //$NON-NLS-1$ 
								mediaName.substring((MediaURLStreamHandler.MEDIA_URL_BLOBLOADER + "?").length()); //$NON-NLS-1$ 
						}
					}
					else url = rolloverUrl;
				}
				url = "url(" + url + ")";
				return "Servoy.Rollover.onMouseOver('" + WebBaseSubmitLink.this.getMarkupId() + "','" + url + "','" + getBackgroundPosition() + "','" +
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
				return "Servoy.Rollover.onMouseOut('" + WebBaseSubmitLink.this.getMarkupId() + "')";
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
		String text = WebBaseSubmitLink.this.getDefaultModelObjectAsString();
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
					url = Strings.replaceAll(getResponse().encodeURL(url), "&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$ 
					styleAttribute = "background-image: url(" + url + "); background-repeat: " + repeat + "; background-position: " + position; //$NON-NLS-1$ //$NON-NLS-2$ 
				}
				else if (iconReference != null && media != null)
				{
					String solutionName = J2DBGlobals.getServiceProvider().getSolution().getName();
					if (mediaOptions != 0 && mediaOptions != 1)
					{
						styleAttribute = "background-image: url(" + urlFor(iconReference) + "?id=" + media.getName() + "&s=" + solutionName + "&option=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							mediaOptions + "&w=" + getSize().width + "&h=" + getSize().height + "&l=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							(media.getMediaData() != null ? +media.getMediaData().hashCode() : 0) +
							"); background-repeat: " + repeat + "; background-position: " + //$NON-NLS-1$ 
							position;
					}
					else
					{
						styleAttribute = "background-image: url(" + urlFor(iconReference) + "?id=" + media.getName() + "&s=" + solutionName + "&l=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							(media.getMediaData() != null ? +media.getMediaData().hashCode() : 0) +
							"); background-repeat: " + repeat + "; background-position: " + //$NON-NLS-1$ 
							position;
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
						String url = RequestCycle.get().urlFor(WebBaseSubmitLink.this, IResourceListener.INTERFACE) +
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
		this.media = null;
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
		return getDefaultModelObjectAsString();
	}

	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}


	public void setRolloverEnabled(boolean b)
	{
		// TODO Auto-generated method stub
	}

	public void setMargin(Insets margin)
	{
		this.margin = margin;
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
				String nm = textUrl.substring(index + MediaURLStreamHandler.MEDIA_URL_DEF.length());
				try
				{
					Media m = application.getFlattenedSolution().getMedia(nm);
					if (m != null)
					{
						setMediaIcon(m.getID());
					}
					else if (nm.startsWith(MediaURLStreamHandler.MEDIA_URL_BLOBLOADER))
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
		return WebBaseLabel.getThumbnailJPGImage(args, icon, text_url, media != null ? media.getID() : 0, (mediaOptions & 8) == 8, application);
	}


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

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "BUTTON"; //$NON-NLS-1$
	}

	public String js_getDataProviderID()
	{
		//default implementation
		return null;
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

	protected boolean accessible = true;

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

	private int mnemonic;

	public Dimension getSize()
	{
		return size;
	}

	public void js_setSize(int width, int height)
	{
		size = new Dimension(width, height);
		jsChangeRecorder.setSize(width, height, border, margin, getFontSize(), valign);
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
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, margin, getFontSize(), null, valign);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, margin, getFontSize(), null, valign);
	}


	public String js_getLabelForElementName()
	{
		if (labelForComponent != null) return labelForComponent.getName();
		return null;
	}

	public String js_getMnemonic()
	{
		if (mnemonic == 0) return ""; //$NON-NLS-1$ 
		return new Character((char)mnemonic).toString();
	}

	public void js_setMnemonic(String m)
	{
		String mnem = application.getI18NMessageIfPrefixed(m);
		if (mnem != null && mnem.length() > 0)
		{
			setDisplayedMnemonic(mnem.charAt(0));
		}
	}

	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public int js_getWidth()
	{
		return size.width;
	}

	public int js_getHeight()
	{
		return size.height;
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

	@Override
	protected void onComponentTag(final ComponentTag tag)
	{
		super.onComponentTag(tag);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		if (useAJAX)
		{
			Object oe = js_getClientProperty("ajax.enabled"); //$NON-NLS-1$ 
			if (oe != null) useAJAX = Utils.getAsBoolean(oe);
		}

		if (!useAJAX)
		{
			if (eventExecutor.hasDoubleClickCmd())
			{
				CharSequence urld = urlFor(IDoubleClickListener.INTERFACE);
				tag.put("ondblclick", getForm().getJsForInterfaceUrl(urld)); //$NON-NLS-1$
			}

			if (eventExecutor.hasRightClickCmd())
			{
				CharSequence urlr = urlFor(IRightClickListener.INTERFACE);
				// We need a "return false;" so that the context menu is not displayed in the browser.
				tag.put("oncontextmenu", getForm().getJsForInterfaceUrl(urlr) + " return false;"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	public void onDoubleClick()
	{
		// If form validation fails, we don't execute the method.
		if (getForm().process()) eventExecutor.onEvent(EventType.doubleClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
	}

	public void onRightClick()
	{
		// If form validation fails, we don't execute the method.
		if (getForm().process()) eventExecutor.onEvent(EventType.rightClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
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
}
