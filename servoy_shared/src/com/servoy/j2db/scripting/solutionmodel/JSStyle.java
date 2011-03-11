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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSStyle implements IJavaScriptType
{

	private boolean copy;
	private Style style;
	private final IApplication application;

	/**
	 * @param style
	 * @param b
	 */
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
	public String js_getName()
	{
		return style.getName();
	}

	/**
	 * The textual content of the style.
	 * 
	 * @sampleas js_getName()
	 */
	public String js_getText()
	{
		return style.getContent();
	}

	/**
	 * Sets the css text of this style. Forms have to be recreated to show this change!
	 *  
	 * @param text
	 */
	public void js_setText(String text)
	{
		if (!copy)
		{
			style = application.getFlattenedSolution().createStyleCopy(style);
			copy = true;
		}
		style.setContent(text);
		ComponentFactory.flushStyle(style);
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSStyle[name:" + style.getName() + ']';
	}

	/**
	 * Returns the UUID of the style object
	 * 
	 * @sample
	 * var st = solutionModel.newStyle('myStyle','form { background-color: yellow; }');
	 * application.output(st.getUUID().toString());
	 */
	public UUID js_getUUID()
	{
		return style.getUUID();
	}
}
