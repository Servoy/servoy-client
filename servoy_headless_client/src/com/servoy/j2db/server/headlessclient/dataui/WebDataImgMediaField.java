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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.Page;
import org.apache.wicket.PageMap;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ILinkListener;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.resource.ByteArrayResource;
import org.apache.wicket.util.string.Strings;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.MediaUploadPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IMediaFieldConstants;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptMediaInputFieldMethods;
import com.servoy.j2db.ui.IScrollPane;
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
 * Component to display media inside the browser has support for up and download of media. Can display the image if it is an image.
 * 
 * @author jcompagner,jblok
 */
public class WebDataImgMediaField extends WebMarkupContainer implements IDisplayData, IFieldComponent, IScrollPane, IScriptMediaInputFieldMethods,
	ILinkListener, IProviderStylePropertyChanges, ISupportWebBounds, IRightClickListener
{
	private static final long serialVersionUID = 1L;

	public static byte[] emptyImage = null;
	private static byte[] notEmptyImage = null;

	static
	{
		try
		{
			InputStream is = IApplication.class.getResourceAsStream("images/empty.gif"); //$NON-NLS-1$
			emptyImage = new byte[is.available()];
			is.read(emptyImage);
			is.close();
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}
		try
		{
			InputStream is = IApplication.class.getResourceAsStream("images/notemptymedia.gif"); //$NON-NLS-1$
			notEmptyImage = new byte[is.available()];
			is.read(notEmptyImage);
			is.close();
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}
	}
	private Cursor cursor;
	private boolean needEntireState;
	private int maxLength;
	private Insets margin;
	private int horizontalAlignment = -1;

	private int horizontalScrollBarPolicy;
	private int verticalScrollBarPolicy;

	private int mediaOption;

	private final WebEventExecutor eventExecutor;
	private byte[] previous;

	private MediaResource resource;
	private final Image upload;
	private final Image download;
	private final Image remove;
	private final ImageDisplay imgd;

	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING);

	private final IApplication application;

	/**
	 * @param id
	 */
	public WebDataImgMediaField(final IApplication application, final String id)
	{
		super(id);
		this.application = application;
		mediaOption = 1;

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);
		setVersioned(false);

		imgd = new ImageDisplay(application, id); //uses the same name
		add(imgd);

		upload = new Image("upload_icon", new ResourceReference(IApplication.class, "images/open_project.gif")) //$NON-NLS-1$//$NON-NLS-2$
		{
			private static final long serialVersionUID = 1l;

			/**
			 * @see wicket.Component#isVisible()
			 */
			@Override
			public boolean isVisible()
			{
				return !js_isReadOnly() && js_isEnabled();
			}
		};
		upload.add(new SimpleAttributeModifier("alt", application.getI18NMessage("servoy.imageMedia.popup.menuitem.load"))); //$NON-NLS-1$//$NON-NLS-2$
		upload.add(new SimpleAttributeModifier("title", application.getI18NMessage("servoy.imageMedia.popup.menuitem.load"))); //$NON-NLS-1$//$NON-NLS-2$
		add(upload);
		if (!useAJAX)
		{
			upload.add(new AttributeModifier("onclick", true, new Model<String>() //$NON-NLS-1$
				{
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject()
					{
						if (editable)
						{
							return "javascript:showMediaUploadPopup('" + urlFor(ILinkListener.INTERFACE) + "', '" + id + "_1')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						return null;
					}
				}));
		}
		else
		{
			upload.add(new ServoyAjaxEventBehavior("onclick") //$NON-NLS-1$
			{
				private static final long serialVersionUID = 1l;

				@Override
				protected void onEvent(AjaxRequestTarget target)
				{
					MainPage page = (MainPage)findPage();
					page.showOpenFileDialog(new IMediaUploadCallback()
					{
						public void uploadComplete(IUploadData[] fu)
						{
							if (fu.length > 0)
							{
								setDefaultModelObject(fu[0].getBytes());
								getStylePropertyChanges().setValueChanged();
								IModel m = WebDataImgMediaField.this.getInnermostModel();
								if (m instanceof RecordItemModel)
								{
									RecordItemModel model = (RecordItemModel)m;
									model.setValue(WebDataImgMediaField.this, WebDataImgMediaField.this.getDataProviderID() + IMediaFieldConstants.FILENAME,
										fu[0].getName());
									model.setValue(WebDataImgMediaField.this, WebDataImgMediaField.this.getDataProviderID() + IMediaFieldConstants.MIMETYPE,
										fu[0].getContentType());
								}
							}
						}

						public void onSubmit()
						{
							// submit without uploaded files 
						}
					}, false, "");
					WebEventExecutor.generateResponse(target, page);
				}
			});
		}
		download = new Image("save_icon", new ResourceReference(IApplication.class, "images/save.gif")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			private static final long serialVersionUID = 1l;

			/**
			 * @see wicket.Component#isVisible()
			 */
			@Override
			public boolean isVisible()
			{
				return !js_isReadOnly() && js_isEnabled();
			}
		};
		download.add(new SimpleAttributeModifier("alt", application.getI18NMessage("servoy.imageMedia.popup.menuitem.save"))); //$NON-NLS-1$ //$NON-NLS-2$
		download.add(new SimpleAttributeModifier("title", application.getI18NMessage("servoy.imageMedia.popup.menuitem.save"))); //$NON-NLS-1$ //$NON-NLS-2$

		add(download);
		download.add(new AttributeModifier("onclick", true, new Model<String>() //$NON-NLS-1$
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					return "javascript:showMediaDownloadPopup('" + imgd.urlFor(IResourceListener.INTERFACE) + "&download=true&t=" + System.currentTimeMillis() +
						"')";
				}
			})
		{
			@Override
			public boolean isEnabled(Component component)
			{
				return getDefaultModelObject() != null;
			}
		});
		remove = new Image("remove_icon", new ResourceReference(IApplication.class, "images/delete.gif")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			private static final long serialVersionUID = 1l;

			/**
			 * @see wicket.Component#isVisible()
			 */
			@Override
			public boolean isVisible()
			{
				return !js_isReadOnly() && js_isEnabled();
			}
		};
		remove.add(new SimpleAttributeModifier("alt", application.getI18NMessage("servoy.imageMedia.popup.menuitem.remove"))); //$NON-NLS-1$ //$NON-NLS-2$
		remove.add(new SimpleAttributeModifier("title", application.getI18NMessage("servoy.imageMedia.popup.menuitem.remove"))); //$NON-NLS-1$ //$NON-NLS-2$
		add(remove);
		remove.add(new ServoyAjaxEventBehavior("onclick") //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1l;

			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				WebDataImgMediaField.this.setDefaultModelObject(null);
				WebDataImgMediaField.this.getStylePropertyChanges().setChanged();

				IModel m = WebDataImgMediaField.this.getInnermostModel();
				if (m instanceof RecordItemModel)
				{
					RecordItemModel model = (RecordItemModel)m;
					try
					{
						model.setValue(WebDataImgMediaField.this, WebDataImgMediaField.this.getDataProviderID() + IMediaFieldConstants.FILENAME, null);
						model.setValue(WebDataImgMediaField.this, WebDataImgMediaField.this.getDataProviderID() + IMediaFieldConstants.MIMETYPE, null);
					}
					catch (Exception e) // this will normally not happen, even if there are non-null constraints as exceptions will be handled by RecordItemModel
					{
						Debug.log("When trying to set filename and mimetype to null because of media remove, an exception happened", e);
					}
				}
				eventExecutor.onEvent(EventType.none, target, WebDataImgMediaField.this, IEventExecutor.MODIFIERS_UNSPECIFIED);
			}
		});
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
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
	 * @see wicket.MarkupContainer#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
		IModel model = getInnermostModel();

		if (model instanceof RecordItemModel)
		{
			((RecordItemModel)model).updateRenderedValue(this);
		}
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
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
				if (eventExecutor.hasRightClickCmd())
				{
					CharSequence urlr = urlFor(IRightClickListener.INTERFACE);
					// We need a "return false;" so that the context menu is not displayed in the browser.
					tag.put("oncontextmenu", f.getJsForInterfaceUrl(urlr) + " return false;"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}

	/**
	 * @see wicket.Component#getMarkupId()
	 */
	@Override
	public String getMarkupId()
	{
		return WebComponentSpecialIdMaker.getSpecialIdIfAppropriate(this);
	}

	/**
	 * @see wicket.markup.html.link.ILinkListener#onLinkClicked()
	 */
	public void onLinkClicked()
	{
		IModel model = getInnermostModel();
		if (model instanceof RecordItemModel)
		{
			setResponsePage(new MediaUploadPage(PageMap.forName("mediaupload"), (RecordItemModel)model, this)); //$NON-NLS-1$
		}
	}

	public void setHorizontalScrollBarPolicy(int policy)
	{
		setMediaOptions(verticalScrollBarPolicy, policy);
		horizontalScrollBarPolicy = policy;
	}

	/**
	 * @see javax.swing.JScrollPane#setVerticalScrollBarPolicy(int)
	 */
	public void setVerticalScrollBarPolicy(int policy)
	{
		setMediaOptions(policy, horizontalScrollBarPolicy);
		verticalScrollBarPolicy = policy;
	}

	private void setMediaOptions(int vertical, int horizontal)
	{
		if (horizontal == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER || vertical == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
		{
			mediaOption = 10;
		}
		else
		{
			mediaOption = 1;
		}
	}

	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
		imgd.addScriptExecuter(el);
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
			requestFocus();
		}
		else
		{
			previousValidValue = null;
		}
	}

	public void requestFocus()
	{
		Page page = findPage();
		if (page instanceof MainPage)
		{
			((MainPage)page).componentToFocus(this);
		}
	}

	public void notifyLastNewValueWasChange(final Object oldVal, final Object newVal)
	{
		if (eventExecutor.hasChangeCmd())
		{
			ServoyForm form = findParent(ServoyForm.class);
			form.addDelayedAction(new ServoyForm.IDelayedAction()
			{
				public void execute()
				{
					Object value = oldVal;
					if (previousValidValue != null) value = oldVal;

					eventExecutor.fireChangeCommand(value, newVal, false, WebDataImgMediaField.this);
				}

				public Component getComponent()
				{
					return WebDataImgMediaField.this;
				}

			});
		}
		else
		{
			setValueValid(true, null);
		}
	}


	public void setChangeCmd(String id, Object[] args)
	{
		eventExecutor.setChangeCmd(id, args);
	}

	public void setActionCmd(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
		imgd.setActionCommand(id, args);
	}

	public void setValidationEnabled(boolean b)
	{
		eventExecutor.setValidationEnabled(b);
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setNeedEntireState(boolean)
	 */
	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;

	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	public void setMaxLength(int maxLength)
	{
		this.maxLength = maxLength;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		//ignore.. pull models
		return null;
	}

	public void setValueObject(Object value)
	{
		jsChangeRecorder.testChanged(this, value);
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public boolean needEditListner()
	{
		return false;
	}

	public void addEditListener(IEditListener editListener)
	{
	}

	public Document getDocument()
	{
		return null;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (!isValueValid)
		{
			requestFocus();
			return false;
		}
		return true;
	}

	private String getImageId()
	{
		return getId() + "_1";
	}

	public class ImageDisplay extends WebBaseButton implements IResourceListener
	{
		private static final long serialVersionUID = 1L;

		public ImageDisplay(IApplication application, String id)
		{
			super(application, id);

			setMediaOption(8 + 1);
			final String elementId = getImageId();
			add(new SimpleAttributeModifier("id", elementId)); //$NON-NLS-1$ 
			add(new StyleAppendingModifier(new Model<String>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					testAndGenerateResource();
					return "top: 0px;left: 0px;position: absolute"; //$NON-NLS-1$
				}
			}));
			add(new AbstractServoyDefaultAjaxBehavior()
			{
				@Override
				protected void respond(AjaxRequestTarget target)
				{
					target.appendJavascript("Servoy.Utils.fixMediaLocation('" + elementId + "'," + horizontalAlignment + ")");
				}

				@Override
				public void renderHead(IHeaderResponse response)
				{
					super.renderHead(response);
					response.renderOnDomReadyJavascript(getCallbackScript().toString());
				}

			});
		}

		/**
		 * @see wicket.IResourceListener#onResourceRequested()
		 */
		@Override
		public void onResourceRequested()
		{
			String param = RequestCycle.get().getRequest().getParameter("download"); //$NON-NLS-1$
			if (param == null)
			{
				testAndGenerateResource();

				if (resource != null)
				{
					resource.onResourceRequested();
				}
			}
			else
			{
				Object data = WebDataImgMediaField.this.getDefaultModelObject();
				if (data instanceof String)
				{
					try
					{
						data = ((String)data).getBytes("UTF8");
					}
					catch (UnsupportedEncodingException e)
					{
						Debug.error(e);
					}
				}
				if (data instanceof byte[])
				{
					IModel m = WebDataImgMediaField.this.getInnermostModel();
					String fileName = null;
					String mimeType = null;
					if (m instanceof RecordItemModel)
					{
						RecordItemModel model = (RecordItemModel)m;
						String mediaDataProviderID = WebDataImgMediaField.this.getDataProviderID();

						Object val = model.getValue(WebDataImgMediaField.this, mediaDataProviderID + IMediaFieldConstants.FILENAME);
						fileName = (val instanceof String) ? (String)val : null;
						val = model.getValue(WebDataImgMediaField.this, mediaDataProviderID + IMediaFieldConstants.MIMETYPE);
						mimeType = (val instanceof String) ? (String)val : null;
					}
					if (mimeType == null) mimeType = ImageLoader.getContentType((byte[])data);
					ByteArrayResource bar = new ByteArrayResource(mimeType, (byte[])data, fileName);
					bar.onResourceRequested();
				}
			}
		}

		private void testAndGenerateResource()
		{
			Object data = WebDataImgMediaField.this.getDefaultModelObject();
			if (data != previous || (previous == null && data == null))
			{
				if (data instanceof String && ((String)data).startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
				{
					String fname = ((String)data).substring(MediaURLStreamHandler.MEDIA_URL_DEF.length());
					if (fname.startsWith("/")) //$NON-NLS-1$
					{
						fname = fname.substring(1);
					}
					final String filename = fname;
					if (application.getSolution() != null) //cannot work without a solution
					{
						try
						{
							Media m = application.getFlattenedSolution().getMedia(filename);
							if (m != null)
							{
								data = m.getMediaData();
							}
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
				}
				if (data != null && data instanceof byte[])
				{
					String contentType = ImageLoader.getContentType((byte[])data);
					if (contentType == null || !contentType.startsWith("image")) //$NON-NLS-1$
					{
						data = notEmptyImage;
					}
				}
				if (data instanceof byte[])
				{
					if (previous != data)
					{
						previous = (byte[])data;
						resource = new MediaResource(previous, mediaOption);
						resource.setCacheable(false);
						resource.checkResize(size);
					}
				}
				else
				{
					if (previous != emptyImage)
					{
						previous = emptyImage;
						if (previous != null)
						{
							resource = new MediaResource(previous, mediaOption);
							resource.setCacheable(false);
						}
						else
						{
							resource = null;
						}
					}
				}
			}
		}

		/**
		 * @see wicket.Component#onComponentTag(wicket.markup.ComponentTag)
		 */
		@Override
		protected void onComponentTag(ComponentTag tag)
		{
			super.onComponentTag(tag);
			CharSequence url = urlFor(IResourceListener.INTERFACE) + "&r=" + Math.random(); //$NON-NLS-1$
			tag.put("src", Strings.replaceAll(getResponse().encodeURL(url), "&", "&amp;")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
	}

	/*
	 * readonly/editable---------------------------------------------------
	 */
	public boolean js_isReadOnly()
	{
		return !editable;
	}

	private boolean editState;

	public void js_setReadOnly(boolean b)
	{
		if (b && !editable) return;
		if (b)
		{
			setEditable(false);
			editState = true;
		}
		else
		{
			setEditable(editState);
		}
		jsChangeRecorder.setChanged();
	}

	public boolean js_isEditable()
	{
		return editable;
	}

	public boolean isReadOnly()
	{
		return !editable;
	}

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
	}

	private boolean editable;


	/*
	 * format---------------------------------------------------
	 */
	public void setFormat(int type, String format)
	{
		dataType = type;
		this.format = format;
	}

	private String format;
	private int dataType;

	public int getDataType()
	{
		return dataType;
	}

	public String getFormat()
	{
		return format;
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	private String dataProviderID;

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public String js_getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "IMAGE_MEDIA"; //$NON-NLS-1$
	}

	/*
	 * scrolling---------------------------------------------------
	 */
	public void js_setScroll(int x, int y)
	{
	}

	public int js_getScrollX()
	{
		return 0;
	}

	public int js_getScrollY()
	{
		return 0;
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
		return Text.processTags(titleText, resolver);
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
			tooltip = null;
		}
		this.tooltip = tooltip;
	}

	public void js_setToolTipText(String tooltip)
	{
		setToolTipText(tooltip);
		jsChangeRecorder.setChanged();
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		if (tooltip != null && getInnermostModel() instanceof RecordItemModel)
		{
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

	private ArrayList labels;

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
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = (ILabel)labels.get(i);
				label.setComponentVisible(visible);
			}
		}
	}

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean visible)
	{
		setVisible(visible);
		jsChangeRecorder.setVisible(visible);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = (ILabel)labels.get(i);
				if (label instanceof IScriptBaseMethods)
				{
					((IScriptBaseMethods)label).js_setVisible(visible);
				}
				else
				{
					label.setComponentVisible(visible);
				}
			}
		}
	}

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList(3);
		labels.add(label);
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
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = (ILabel)labels.get(i);
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

	public Dimension getSize()
	{
		return size;
	}

	public void js_setSize(int width, int height)
	{
		size = new Dimension(width, height);
		jsChangeRecorder.setSize(width, height, border, margin, 0);
	}

	public Rectangle getWebBounds()
	{
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, margin, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, margin, 0, null);
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

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
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

	private Form getForm()
	{
		Component c = this;
		while ((c != null) && !(c instanceof Form))
			c = c.getParent();
		return (Form)c;
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getDefaultModelObjectAsString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
