/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.smart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;

/**
 * Class responsible for offering UI feedback when things (solutions) are being loaded in Smart Client.
 *
 * @author acostescu
 */
public class LoadingUIEffects
{

	public static final String SERVOY_BRANDING = "servoy.branding"; //$NON-NLS-1$
	public static final String SERVOY_BRANDING_WEBSTART_SPLASH = "servoy.branding.webstart.splash"; //$NON-NLS-1$
	public static final String SERVOY_BRANDING_LOADING_IMAGE = "servoy.branding.loadingimage"; //$NON-NLS-1$
	public static final String SERVOY_BRANDING_LOADING_BACKGROUND = "servoy.branding.loadingbackground"; //$NON-NLS-1$
	public static final String SERVOY_BRANDING_HIDE_FRAME_WHILE_LOADING = "servoy.branding.hideframewhileloading"; //$NON-NLS-1$

	private JLabel loadingLabel = null;
	private final J2DBClient application;
	private final MainPanel mainPanel;

	// before the first solution loaded in this client we should always use "hide frame while loading" behaviour
	// but using the WEBSTART_SPLASH - it is nicer to do this until we actually have something useful to show in main window
	private boolean beforeFirstSolutionLoad = true;

	public LoadingUIEffects(J2DBClient client, MainPanel mainPanel)
	{
		this.application = client;
		this.mainPanel = mainPanel;
	}

	private boolean isBrandingOn()
	{
		return application.getSettings().getProperty(SERVOY_BRANDING, "false").equals("true"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean shouldShowFrameWhileLoading()
	{
		return application.getSettings().getProperty(SERVOY_BRANDING_HIDE_FRAME_WHILE_LOADING, "false").equals("false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void showSolutionLoading(boolean loading)
	{
		if (loading && loadingLabel == null)
		{
			createLoadingLabel();
		}

		if ((!beforeFirstSolutionLoad) && shouldShowFrameWhileLoading())
		{
			// show the "loading" img as part of the main frame/main panel
			JFrame f = application.getMainApplicationFrame();
			if (loading && f != null && !f.isVisible())
			{
				f.setVisible(true);
			}
			if (loading)
			{
				Color loadingBackground = getLoadingBackgroundColor();
				mainPanel.showLoadingUI(loadingLabel, loadingBackground);
			}
			else if (loadingLabel != null)
			{
				mainPanel.hideLoadingUI(loadingLabel);
			}
		}
		else
		{
			// show the "loading" img as a splash undecorated frame

			// hide main frame when showing splash / show the frame when done loading
			JFrame f = application.getMainApplicationFrame();
			if (f != null && f.isVisible() == loading)
			{
				f.setVisible(!loading);
			}

			if (loading)
			{
				getSplashFrame(true).setVisible(true);
			}
			else if (loadingLabel != null)
			{
				JFrame splashFrame = getSplashFrame(false);
				if (splashFrame != null)
				{
					splashFrame.setVisible(false);
					splashFrame.getContentPane().remove(loadingLabel);
				}
			}
		}

		if (!loading && beforeFirstSolutionLoad)
		{
			// we are ready to show a solution; from now on, the loading image will now be different and
			// loading behaviour will actually take into account the value of SERVOY_BRANDING_HIDE_FRAME_WHILE_LOADING
			beforeFirstSolutionLoad = false;
			loadingLabel = null;
		}
	}

	private Color getLoadingBackgroundColor()
	{
		if (!isBrandingOn()) return null;

		String frameBackgroundString = application.getSettings().getProperty(SERVOY_BRANDING_LOADING_BACKGROUND);
		Color loadingBackground = (frameBackgroundString != null ? PersistHelper.createColor(frameBackgroundString) : null);
		return loadingBackground;
	}

	protected JFrame getSplashFrame(boolean createIfNeeded)
	{
		JFrame splashFrame = getFrame(loadingLabel);
		if (splashFrame == null && createIfNeeded)
		{
			JFrame mf = application.getMainApplicationFrame();
			splashFrame = new JFrame(mf.getTitle());
			splashFrame.setIconImage(mf.getIconImage());
			splashFrame.setUndecorated(true);
			UIUtils.setWindowTransparency(splashFrame, splashFrame.getContentPane(), true, true, false);
			splashFrame.getContentPane().add(loadingLabel, BorderLayout.CENTER);
			splashFrame.pack();
			if (mf.isShowing()) splashFrame.setLocationRelativeTo(mf); // this doesn't work when mf is not showing; it will probably never execute this
			else splashFrame.setBounds(UIUtils.getCenteredBoundsOn(mf.getBounds(), splashFrame.getWidth(), splashFrame.getHeight()));
		}
		return splashFrame;
	}

	protected JFrame getFrame(Component src)
	{
		Container c = src.getParent();
		while (c != null && !(c instanceof JFrame))
		{
			c = c.getParent();
		}
		return (JFrame)c;
	}

	protected void createLoadingLabel()
	{
		String loadingImage = beforeFirstSolutionLoad ? application.getSettings().getProperty(SERVOY_BRANDING_WEBSTART_SPLASH, "lib/splashclient.png") //$NON-NLS-1$
			: application.getSettings().getProperty(SERVOY_BRANDING_LOADING_IMAGE);
		if ((isBrandingOn() || beforeFirstSolutionLoad) && loadingImage != null && Utils.isSwingClient(application.getApplicationType()))
		{
			if (loadingImage.equals("")) //$NON-NLS-1$
			{
				loadingLabel = new JLabel();
			}
			else
			{
				loadingLabel = getWebStartURLImageLabel(loadingImage);
			}
		}

		if (loadingLabel == null)
		{
			loadingLabel = getWebStartURLImageLabel("lib/images/solutionloading.gif"); //$NON-NLS-1$
		}
	}

	protected JLabel getWebStartURLImageLabel(String imgPath)
	{
		if (application.getApplicationType() == IApplication.RUNTIME)
		{
			return new JLabel(new ImageIcon(imgPath));
		}
		else
		{
			String loadingImage = imgPath;
			URL webstartUrl = getWebStartURL();
			if (webstartUrl != null)
			{
				try
				{
					if (!loadingImage.startsWith("/")) loadingImage = "/" + loadingImage; //$NON-NLS-1$//$NON-NLS-2$
					String loadingImageFile = null;
					String path = webstartUrl.getPath();
					if (!path.equals("") && path.endsWith("/")) //$NON-NLS-1$//$NON-NLS-2$
					{
						loadingImageFile = path.substring(0, path.length() - 1) + loadingImage;
					}
					else loadingImageFile = loadingImage;
					URL url = new URL(webstartUrl.getProtocol(), webstartUrl.getHost(), webstartUrl.getPort(), loadingImageFile);
					return new JLabel(new ImageIcon(url), SwingConstants.CENTER);
				}
				catch (MalformedURLException ex)
				{
					Debug.error("Error loading the solution loading image", ex); //$NON-NLS-1$
				}
			}
			return null;
		}
	}

	protected URL getWebStartURL()
	{
		return WebStart.getWebStartURL();
	}

}
