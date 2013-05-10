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
package com.servoy.j2db.ui;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.scripting.IScriptableProvider;

/**
 * @author jcompagner,jblok
 */
public interface ILabel extends IStandardLabel, ISupportSecuritySettings, ISupportEventExecutor, IScriptableProvider
{
	public static int NONE = 0;
	public static int CAPITALIZE = 1;
	public static int UPPERCASE = 2;
	public static int LOWERCASE = 3;

	/**
	 * @param mediaOptions
	 */
	public void setMediaOption(int mediaOptions);

	/**
	 * @param el
	 */
	public void addScriptExecuter(IScriptExecuter el);

	/**
	 * @param string
	 * @param args TODO
	 */
	public void setActionCommand(String string, Object[] args);

	public void setDoubleClickCommand(String string, Object[] args);

	public void setRightClickCommand(String string, Object[] args);

	/**
	 * @param bs
	 */
	public void setRolloverIcon(int rollOverMediaId);

	/**
	 * @param rotation
	 */
	public void setRotation(int rotation);

	/**
	 * @param showFocus
	 */
	public void setFocusPainted(boolean showFocus);

	public void setMediaIcon(int mediaId);

	public int getMediaIcon();

	/**
	 * @param mode (capitalize | uppercase | lowercase | none)
	 */
	public void setTextTransform(int mode);

	public void setImageURL(String text_url);

	public void setRolloverImageURL(String image_url);

	public byte[] getThumbnailJPGImage(int width, int height);

	public int getAbsoluteFormLocationY();

	public String getImageURL();

	public String getRolloverImageURL();

	public String getParameterValue(String param);

	public int getFontSize();

	public Object getLabelFor();
}
