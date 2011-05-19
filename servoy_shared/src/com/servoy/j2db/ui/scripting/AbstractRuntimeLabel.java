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

package com.servoy.j2db.ui.scripting;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.IStandardLabel;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;

/**
 * Abstract scriptable label.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeLabel extends AbstractRuntimeBaseComponent implements IScriptRenderMethods
{
	protected ILabel label;
	private String i18nTT;

	public AbstractRuntimeLabel(ILabel label, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(label, jsChangeRecorder, application);
		this.label = label;
	}

	public void js_setImageURL(String text_url)
	{
		label.setImageURL(text_url);
	}

	public void js_setRolloverImageURL(String imageUrl)
	{
		label.setRolloverImageURL(imageUrl);
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.LABEL;
	}

	public String js_getDataProviderID()
	{
		//default implementation
		return null;
	}

	public byte[] js_getThumbnailJPGImage(Object[] args)
	{
		return label.getThumbnailJPGImage(args);
	}

	public int js_getAbsoluteFormLocationY()
	{
		return label.getAbsoluteFormLocationY();
	}

	public void js_setToolTipText(String text)
	{
		if (text != null && text.startsWith("i18n:")) //$NON-NLS-1$
		{
			i18nTT = text;
			text = application.getI18NMessage(text);
		}
		else
		{
			i18nTT = null;
		}
		label.setToolTipText(text);
		jsChangeRecorder.setChanged();
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getToolTipText()
	 */
	public String js_getToolTipText()
	{
		if (i18nTT != null) return i18nTT;
		return label.getToolTipText();
	}

	public String js_getMnemonic()
	{
		int i = ((IStandardLabel)label).getDisplayedMnemonic();
		if (i == 0) return "";
		return new Character((char)i).toString();
	}

	public void js_setMnemonic(String mnemonic)
	{
		mnemonic = application.getI18NMessageIfPrefixed(mnemonic);
		if (mnemonic != null && mnemonic.length() > 0)
		{
			((IStandardLabel)label).setDisplayedMnemonic(mnemonic.charAt(0));
		}
	}

	public String getImageURL()
	{
		return label.getImageURL();
	}

	public String getRolloverImageURL()
	{
		return label.getRolloverImageURL();
	}
}
