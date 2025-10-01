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
import java.awt.image.BufferedImage;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.scripting.RuntimeScriptButton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.SnapShot;
import com.servoy.j2db.util.gui.SnapShot.IMAGE_TYPE;

/**
 * Shows an image from a bean running in the server
 * @author jcompagner
 */
public class WebImageBeanHolder extends WebBaseButton implements IDelegate
{
	private static final long serialVersionUID = 1L;

	private final JComponent bean;

	// Fields used for caching data sent to web client. The image of the bean is sent
	// only if there is any change since the previously sent image.
	private boolean lastChanged;
	private byte[] lastSnapshot;
	private Date lastIsChangedQuery;

	private final int anchoring;

	public WebImageBeanHolder(IApplication application, RuntimeScriptButton scriptable, String id, JComponent bean, int anchoring)
	{
		super(application, scriptable, id, ""); //$NON-NLS-1$
		((ChangesRecorder)scriptable.getChangesRecorder()).setDefaultBorderAndPadding(null, null);
		this.bean = bean;
		this.anchoring = anchoring;
		setMediaOption(8 + 1);

		icon = new MediaResource();
	}

	public Object getDelegate()
	{
		return bean;
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

		parent.setOpaque(false);
		parent.add(bean, BorderLayout.CENTER);
		parent.setSize(size);
		parent.doLayout();
		parent.addNotify();

		byte[] mostRecent = SnapShot.createImage(null, parent, size.width, size.height, IMAGE_TYPE.PNG);

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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.headlessclient.dataui.WebBaseButton#setComponentVisible(boolean)
	 */
	@Override
	public void setComponentVisible(boolean visible)
	{
		bean.setVisible(visible);
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
}
