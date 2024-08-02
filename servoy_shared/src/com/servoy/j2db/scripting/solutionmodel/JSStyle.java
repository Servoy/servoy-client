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
package com.servoy.j2db.scripting.solutionmodel;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.solutionmodel.ISMStyle;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@Deprecated
public class JSStyle implements IJavaScriptType, ISMStyle
{
	private boolean copy;
	private Style style;
	private final IApplication application;

	public JSStyle(IApplication application, Style style, boolean copy)
	{
		this.application = application;
		this.style = style;
		this.copy = copy;
	}

	/**
	 * Gets the name of the style.
	 *
	 * @sample
	 * var st = solutionModel.newStyle('myStyle','form { background-color: yellow; }');
	 * st.text = st.text + 'field { background-color: blue; }';
	 * form.styleName = 'myStyle';
	 * application.output('Style name is: ' + st.getName());
	 *
	 * @return A String holding the name of the style.
	 */
	@JSFunction
	public String getName()
	{
		return style.getName();
	}

	/**
	 * The textual content of the style.
	 *
	 * @sampleas getName()
	 */
	@JSGetter
	public String getText()
	{
		return style.getContent();
	}

	@JSSetter
	public void setText(String text)
	{
		if (!copy)
		{
			style = application.getFlattenedSolution().createStyleCopy(style);
			copy = true;
		}
		style.setContent(text);
		// flag style to make sure its parsed style is not cached globally
		style.setRuntimeProperty(ComponentFactory.MODIFIED_BY_CLIENT, Boolean.TRUE);
		ComponentFactory.flushStyle(application, style);
	}

	/**
	 * Returns the UUID of the style object
	 *
	 * @sample
	 * var st = solutionModel.newStyle('myStyle','form { background-color: yellow; }');
	 * application.output(st.getUUID().toString());
	 */
	@JSFunction
	public UUID getUUID()
	{
		return style.getUUID();
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSStyle[name:" + style.getName() + ']';
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((style == null) ? 0 : style.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSStyle other = (JSStyle)obj;
		if (style == null)
		{
			if (other.style != null) return false;
		}
		else if (!style.getUUID().equals(other.style.getUUID())) return false;
		return true;
	}
}
