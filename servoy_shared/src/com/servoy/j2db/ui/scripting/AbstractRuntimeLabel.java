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

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.RenderableWrapper;

/**
 * Abstract scriptable label.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeLabel<C extends ILabel> extends AbstractRuntimeBaseComponent<C> implements IScriptRenderMethods, ISupportOnRenderCallback
{
	private String i18nTT;
	protected IScriptRenderMethods renderable;
	protected RenderEventExecutor renderEventExecutor;

	public AbstractRuntimeLabel(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
		renderable = new RenderableWrapper(this);
		renderEventExecutor = new RenderEventExecutor(this);
	}

	@Override
	public void setComponent(C component)
	{
		super.setComponent(component);
		if (component instanceof Component)
		{
			((Component)component).addFocusListener(new FocusListener()
			{
				public void focusLost(FocusEvent e)
				{
					getRenderEventExecutor().setRenderStateChanged();
					getRenderEventExecutor().fireOnRender(false);
				}

				public void focusGained(FocusEvent e)
				{
					getRenderEventExecutor().setRenderStateChanged();
					getRenderEventExecutor().fireOnRender(false);
				}
			});
		}
	}

	public void js_setImageURL(String text_url)
	{
		getComponent().setImageURL(text_url);
	}

	public void js_setRolloverImageURL(String imageUrl)
	{
		getComponent().setRolloverImageURL(imageUrl);
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
		return getComponent().getThumbnailJPGImage(args);
	}

	public int js_getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
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
		getComponent().setToolTipText(text);
		getChangesRecorder().setChanged();
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptLabelMethods#js_getToolTipText()
	 */
	public String js_getToolTipText()
	{
		if (i18nTT != null) return i18nTT;
		return getComponent().getToolTipText();
	}

	public String js_getMnemonic()
	{
		int i = getComponent().getDisplayedMnemonic();
		if (i == 0) return "";
		return new Character((char)i).toString();
	}

	public void js_setMnemonic(String mnemonic)
	{
		mnemonic = application.getI18NMessageIfPrefixed(mnemonic);
		if (mnemonic != null && mnemonic.length() > 0)
		{
			getComponent().setDisplayedMnemonic(mnemonic.charAt(0));
		}
	}

	public String js_getImageURL()
	{
		return getComponent().getImageURL();
	}

	public String js_getRolloverImageURL()
	{
		return getComponent().getRolloverImageURL();
	}

	public void js_setFormat(String format)
	{
		getComponent().setFormat(getComponent().getDataType(), application.getI18NMessageIfPrefixed(format));
		getChangesRecorder().setChanged();
	}

	public String js_getFormat()
	{
		return getComponent().getFormat();
	}

	public RenderEventExecutor getRenderEventExecutor()
	{
		return renderEventExecutor;
	}

	public IScriptRenderMethods getRenderable()
	{
		return renderable;
	}

	public void setRenderableStateChanged()
	{
		getChangesRecorder().setChanged();
	}
}
