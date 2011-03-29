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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.SnapShot;

/**
 * Shows an image from a bean running in the server
 * @author jcompagner
 */
public class WebImageBeanHolder extends WebBaseButton implements IComponent, IDelegate
{
	private static final long serialVersionUID = 1L;

	private final JComponent bean;

	// Fields used for caching data sent to web client. The image of the bean is sent
	// only if there is any change since the previously sent image.
	private boolean lastChanged;
	private byte[] lastSnapshot;
	private Date lastIsChangedQuery;

	private final int anchoring;

	public WebImageBeanHolder(IApplication application, String id, JComponent bean, int anchoring)
	{
		super(application, id, ""); //$NON-NLS-1$
		this.bean = bean;
		this.anchoring = anchoring;
		if (bean != null) bean.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				if (!WebImageBeanHolder.this.getSize().equals(WebImageBeanHolder.this.bean.getSize()))
				{
					WebImageBeanHolder.this.js_setSize(WebImageBeanHolder.this.bean.getWidth(), WebImageBeanHolder.this.bean.getHeight());
				}
			}
		});
		setMediaOption(8 + 1);

		add(new AttributeModifier("src", new AbstractReadOnlyModel<String>() //$NON-NLS-1$
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					return urlFor(IResourceListener.INTERFACE) + "&x=" + Math.random(); //$NON-NLS-1$
				}

			}));

		icon = new MediaResource();

		final boolean useAnchors = Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$
		if (useAnchors)
		{
			if ((anchoring & (IAnchorConstants.WEST | IAnchorConstants.EAST)) != 0 || (anchoring & (IAnchorConstants.NORTH | IAnchorConstants.SOUTH)) != 0)
			{
				add(new AbstractServoyDefaultAjaxBehavior()
				{
					@Override
					public void renderHead(IHeaderResponse response)
					{
						super.renderHead(response);

						String beanHolderId = WebImageBeanHolder.this.getMarkupId();

						int width = getSize().width;
						int height = getSize().height;

						StringBuffer sb = new StringBuffer();
						sb.append("if(typeof(beansPreferredSize) != \"undefined\")\n").append("{\n"); //$NON-NLS-1$ //$NON-NLS-2$
						sb.append("beansPreferredSize['").append(beanHolderId).append("'] = new Array();\n"); //$NON-NLS-1$ //$NON-NLS-2$
						sb.append("beansPreferredSize['").append(beanHolderId).append("']['height'] = ").append(height).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						sb.append("beansPreferredSize['").append(beanHolderId).append("']['width'] = ").append(width).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						sb.append("beansPreferredSize['").append(beanHolderId).append("']['callback'] = '").append(getCallbackUrl()).append("';\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						sb.append("}\n"); //$NON-NLS-1$
						response.renderOnLoadJavascript(sb.toString());
					}

					@Override
					protected void respond(AjaxRequestTarget target)
					{
						String sWidthHint = getComponent().getRequest().getParameter("width"); //$NON-NLS-1$ 
						String sHeightHint = getComponent().getRequest().getParameter("height"); //$NON-NLS-1$ 
						int widthHint = Integer.parseInt(sWidthHint);
						int heightHint = Integer.parseInt(sHeightHint);

						setSize(new Dimension(widthHint, heightHint));
						WebEventExecutor.generateResponse(target, getComponent().getPage());
					}
				});
			}
		}
	}

	public Object getDelegate()
	{
		return bean;
	}

	@Override
	public void onSubmit()
	{
		super.onSubmit();
		try
		{
			int x = Utils.getAsInteger(getRequest().getParameter(getInputName() + ".x")); //$NON-NLS-1$
			int y = Utils.getAsInteger(getRequest().getParameter(getInputName() + ".y")); //$NON-NLS-1$
			bean.dispatchEvent(new MouseEvent(bean, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false));
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	@Override
	public void onResourceRequested()
	{
		Dimension size = getSize();
		if (size != null)
		{
			createBeanIcon(size);
			super.onResourceRequested();
		}
	}

	private void createBeanIcon(Dimension size)
	{
		try
		{
			// Try to get a new icon, if there were any changes. If there
			// was no change, then null will be returned.
			byte[] mostRecentIcon = getIconIfChanged(size);
			if (mostRecentIcon != null)
			{
				setIcon(mostRecentIcon);
				if (icon != null)
				{
					icon.setCacheable(false);
				}
			}
		}
		catch (Exception e)
		{
			Debug.log("Error rendering bean in web client: " + bean, e); //$NON-NLS-1$
		}
	}

	private byte[] getIconIfChanged(Dimension size)
	{
		// It seems that the web client generates invocations in pairs: first the "isChanged" method
		// is invoked, then the "onResourceRequested". The following situation may appear: when the 
		// "isChanged" method is called, it detects a change, but by the time we get to "onResourceRequested"
		// there is no new change. In this situation we have to remember the result from the "isChanged".
		// On the other hand, sometimes the "onResourceRequested" method can be invoked independently of 
		// "isChanged", in which case we don't have to rely on the results from previous calls to "isChanged".
		// So, as a compromise, we do the following: we rely on a previous call to "isChanged" only if it
		// was invoked in the recent past (last 2 seconds). Otherwise we assume any cached data may be
		// out dated and we check again for changed in "onResourceRequested".
		Date now = new Date();
		if ((lastIsChangedQuery == null) || (now.getTime() - lastIsChangedQuery.getTime() > 2000)) createSnapshot(size);
		if (lastChanged) return lastSnapshot;
		else return null;
	}

	private void createSnapshot(Dimension size)
	{
		JPanel parent = new JPanel(new BorderLayout())
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Image createImage(int width, int height)
			{
				return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
			}
		};
		parent.add(bean, BorderLayout.CENTER);
		parent.setSize(size);
		parent.doLayout();
		parent.addNotify();

		byte[] mostRecent = SnapShot.createJPGImage(null, parent, size.width, size.height);

		parent.remove(bean);

		if (mostRecent != null)
		{
			if (lastSnapshot != null)
			{
				if (mostRecent.length != lastSnapshot.length)
				{
					lastChanged = true;
				}
				else
				{
					lastChanged = false;
					for (int i = 0; i < mostRecent.length; i++)
					{
						if (mostRecent[i] != lastSnapshot[i])
						{
							lastChanged = true;
							break;
						}
					}
				}
			}
			else
			{
				lastChanged = true;
			}
		}
		else
		{
			lastChanged = false;
		}

		if (lastChanged) lastSnapshot = mostRecent;
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseButton#getStylePropertyChanges()
	 */
	@Override
	public IStylePropertyChanges getStylePropertyChanges()
	{
		IStylePropertyChanges changes = super.getStylePropertyChanges();
		if (!lastChanged)
		{
			testChanged();
			if (!changes.isChanged() && lastChanged)
			{
				changes.setChanged();
			}
		}
		else
		{
			changes.setChanged();
		}
		boolean useAnchors = Utils.getAsBoolean(application.getRuntimeProperties().get("enableAnchors")); //$NON-NLS-1$
		if (useAnchors)
		{
			if ((anchoring & (IAnchorConstants.EAST | IAnchorConstants.WEST)) != 0) changes.getChanges().remove("width"); //$NON-NLS-1$
			if ((anchoring & (IAnchorConstants.NORTH | IAnchorConstants.SOUTH)) != 0) changes.getChanges().remove("height"); //$NON-NLS-1$
		}
		return changes;
	}

	@Override
	public boolean isVisible()
	{
		return bean.isVisible();
	}

	@Override
	public void setSize(Dimension size)
	{
		createBeanIcon(size);
		super.setSize(size);
	}

	private boolean testChanged()
	{
		try
		{
			// Here we check for changes all the time. This is the purpose of this method, to query any
			// new changes, so we cannot cache results.
			Dimension size = getSize();
			if (size != null)
			{
				createSnapshot(getSize());
				lastIsChangedQuery = new Date(); // Remember the time of this query for changes.
				return lastChanged;
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
			Debug.log("Error checking if bean in web client was changed: " + bean, e);
			return false;
		}
	}

	@Override
	protected void addImageStyleAttributeModifier()
	{
		// don't generate the 'background-image' attribute, it will just send twice the requests for the image
	}
}
