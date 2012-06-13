/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.solutionmodel;


/**
 * Solution model css style.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMStyle extends ISMHasUUID
{

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
	public String getName();

	/**
	 * The textual content of the style.
	 * 
	 * @sampleas getName()
	 */
	public String getText();

	/**
	 * Sets the css text of this style. Forms have to be recreated to show this change!
	 *  
	 * @param text the style text
	 */
	public void setText(String text);

}