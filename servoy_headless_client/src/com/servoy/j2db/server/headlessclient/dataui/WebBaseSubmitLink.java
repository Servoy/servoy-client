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
import java.util.Locale;

import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

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
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.server.headlessclient.ByteArrayResource;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.scripting.AbstractRuntimeBaseComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Base class from labels with an action event. 
 * 
 * @author jcompagner, jblok
 */
public class WebBaseSubmitLink extends SubmitLink implements ILabel, IResourceListener, IProviderStylePropertyChanges, ISupportSecuritySettings,
	IAjaxIndicatorAware, IDoubleClickListener, IRightClickListener, ISupportWebBounds, IButton
{
	private static final long serialVersionUID = 1L;

	private int halign = -1;
	private int valign = -1;
	private int mediaOptions;
	private int rotation;
	private boolean showFocus;
	private Cursor cursor;
	private Insets margin;
	private String inputId;

	protected MediaResource icon;
	private AttributeModifier imageStyle;
	private Media media;
	private Dimension mediaSize;
	private Media rolloverMedia;
	private String iconUrl;
	private ResourceReference iconReference;
	private ResourceReference rolloverIconReference;
	protected final IApplication application;
	private String text_url;
	private String rolloverUrl;
	private final WebEventExecutor eventExecutor;
	private ServoyAjaxEventBehavior rolloverBehavior;

	protected IFieldComponent labelForComponent;
	private final AbstractRuntimeBaseComponent< ? extends ILabel> scriptable;

	public WebBaseSubmitLink(IApplication application, AbstractRuntimeBaseComponent< ? extends ILabel> scriptable, String id)
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
		this.scriptable = scriptable;
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(null, TemplateGenerator.DEFAULT_LABEL_PADDING);
	}

	public final AbstractRuntimeBaseComponent< ? extends ILabel> getScriptObject()
	{
		return scriptable;
	}

	/**
	 * @see org.apache.wicket.Component#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return application.getLocale();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@SuppressWarnings("nls")
	@Override
	public void renderHead(HtmlHeaderContainer container)
	{
		super.renderHead(container);
		if (valign == ISupportTextSetup.CENTER)
		{
			container.getHeaderResponse().renderOnDomReadyJavascript("Servoy.Utils.setLabelChildHeight('" + getMarkupId() + "')");
		}
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
		scriptable.getChangesRecorder().setRendered();
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
		return scriptable.getChangesRecorder();
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
		mediaSize = null;
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
				if (!scriptable.js_isEnabled())
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
		mediaSize = null;
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

	public int getVerticalAlignment()
	{
		return valign;
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

	public Insets getMargin()
	{
		return margin;
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

	public void setImageURL(String textUrl)
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
		scriptable.getChangesRecorder().setChanged();
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
		scriptable.getChangesRecorder().setChanged();
	}

	public byte[] getThumbnailJPGImage(Object[] args)
	{
		return WebBaseLabel.getThumbnailJPGImage(args, icon, text_url, media != null ? media.getID() : 0, (mediaOptions & 8) == 8, application);
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

	public Font getFont()
	{
		return font;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */

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

	private Color foreground;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	public Color getForeground()
	{
		return foreground;
	}

	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		if (viewable)
		{
			setVisible(visible);
		}
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			super.setEnabled(b);
			scriptable.getChangesRecorder().setChanged();
		}
	}

	protected boolean accessible = true;

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

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	private int mnemonic;

	public Dimension getSize()
	{
		return size;
	}

	public int getFontSize()
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
		Dimension d = ((ChangesRecorder)scriptable.getChangesRecorder()).calculateWebSize(size.width, size.height, border, border != null ? null : margin,
			getFontSize(), null, false, valign); // border already have the margin
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)scriptable.getChangesRecorder()).getPaddingAndBorder(size.height, border, border != null ? null : margin, getFontSize(), null,
			false, valign); // border already have the margin
	}


	public void setSize(Dimension size)
	{
		this.size = size;
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
			Object oe = scriptable.js_getClientProperty("ajax.enabled"); //$NON-NLS-1$ 
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
		return scriptable.toString();
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	protected void instrumentAndReplaceBody(MarkupStream markupStream, ComponentTag openTag, CharSequence bodyText)
	{
		Insets padding = null;
		if (border == null)
		{
			padding = margin;
		}
		else if (border instanceof CompoundBorder)
		{
			padding = ((CompoundBorder)border).getInsideBorder().getBorderInsets(null);
		}

		Insets iconMargin = null;
		if (media != null && ((mediaOptions & 1) == 1) && (halign == ISupportTextSetup.LEFT))
		{
			if (mediaSize == null) mediaSize = ImageLoader.getSize(media.getMediaData());
			iconMargin = new Insets(0, mediaSize.width + 4, 0, 0);
		}

		String instrumentedBodyText = WebBaseButton.instrumentBodyText(bodyText, halign, valign, fillAllSpace(), fillAllSpace(), padding, iconMargin,
			getMarkupId() + "_lb");
		// for vertical centering we need a table wrapper to have the possible <img> in the content centered
		if (valign == ISupportTextSetup.CENTER && instrumentedBodyText.toLowerCase().indexOf("<img ") != -1) //$NON-NLS-1$
		{
			instrumentedBodyText = (new StringBuffer(
				"<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" height=\"100%\"><tr><td style=\"vertical-align:middle;\">").append(instrumentedBodyText).append("</td></tr></table>")).toString(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		replaceComponentTagBody(markupStream, openTag, instrumentedBodyText);
	}

	protected boolean fillAllSpace()
	{
		return false;
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

	public void requestFocus(Object[] vargs)
	{

	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getRenderEventExecutor()
	 */
	public RenderEventExecutor getRenderEventExecutor()
	{
		return eventExecutor;
	}

	public int getDisplayedMnemonic()
	{
		return mnemonic;
	}

	public String getParameterValue(String param)
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

	public Object getLabelFor()
	{
		return labelForComponent;
	}
}
