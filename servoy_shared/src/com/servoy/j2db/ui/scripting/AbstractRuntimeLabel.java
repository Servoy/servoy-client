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
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.runtime.IRuntimeBaseLabel;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.FormatParser;

/**
 * Abstract scriptable label.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeLabel<C extends ILabel> extends AbstractRuntimeRendersupportComponent<C> implements IFormatScriptComponent,
	IRuntimeBaseLabel
{
	private String i18nTT;

	protected ComponentFormat componentFormat;

	public AbstractRuntimeLabel(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}


	public void setImageURL(String text_url)
	{
		getComponent().setImageURL(text_url);
		getChangesRecorder().setChanged();
	}

	public void setRolloverImageURL(String imageUrl)
	{
		getComponent().setRolloverImageURL(imageUrl);
		getChangesRecorder().setChanged();
	}

	public String getElementType()
	{
		return IRuntimeComponent.LABEL;
	}

	public String getDataProviderID()
	{
		//default implementation
		return null;
	}

	public byte[] getThumbnailJPGImage()
	{
		return getThumbnailJPGImage(-1, -1);
	}

	public byte[] getThumbnailJPGImage(int width, int height)
	{
		return getComponent().getThumbnailJPGImage(width, height);
	}

	public int getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	@Override
	public void setToolTipText(String text)
	{
		String tooltip = text;
		if (tooltip != null && tooltip.startsWith("i18n:")) //$NON-NLS-1$
		{
			i18nTT = tooltip;
			tooltip = application.getI18NMessage(tooltip);
		}
		else
		{
			i18nTT = null;
		}
		super.setToolTipText(tooltip);
	}

	/**
	 * @see com.servoy.j2db.ui.runtime.IRuntimeBaseLabel#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		if (i18nTT != null) return i18nTT;
		return super.getToolTipText();
	}

	public String getMnemonic()
	{
		int i = getComponent().getDisplayedMnemonic();
		if (i == 0) return ""; //$NON-NLS-1$
		return new Character((char)i).toString();
	}

	public void setMnemonic(String m)
	{
		String mnemonic = application.getI18NMessageIfPrefixed(m);
		if (mnemonic != null && mnemonic.length() > 0)
		{
			getComponent().setDisplayedMnemonic(mnemonic.charAt(0));
			getChangesRecorder().setChanged();
		}
	}

	public String getImageURL()
	{
		return getComponent().getImageURL();
	}

	public String getRolloverImageURL()
	{
		return getComponent().getRolloverImageURL();
	}

	public void setFormat(String formatString)
	{
		setComponentFormat(new ComponentFormat(FormatParser.parseFormatString(application.getI18NMessageIfPrefixed(formatString), componentFormat == null
			? null : componentFormat.parsedFormat.getUIConverterName(),
			componentFormat == null ? null : componentFormat.parsedFormat.getUIConverterProperties()), componentFormat == null ? IColumnTypes.TEXT
			: componentFormat.dpType, componentFormat == null ? IColumnTypes.TEXT : componentFormat.uiType));
		getChangesRecorder().setChanged();
		fireOnRender();
	}

	public String getFormat()
	{
		return componentFormat == null ? null : componentFormat.parsedFormat.getFormatString();
	}

	public void setComponentFormat(ComponentFormat componentFormat)
	{
		this.componentFormat = componentFormat;
		if (componentFormat != null && getComponent() instanceof IFormattingComponent)
		{
			((IFormattingComponent)getComponent()).installFormat(componentFormat.uiType, componentFormat.parsedFormat.getFormatString());
		}
	}

	public ComponentFormat getComponentFormat()
	{
		return componentFormat;
	}

}
