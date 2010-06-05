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


/**
 * Implement this interface if wicket component/bean wants to update itself in the browser through ajax requests.
 *  
 * Servoy will call {@link #getStylePropertyChanges()} on every ajax request (user action or ajax poll) to test
 * if this component is changed and wants to update itself.
 * <p>
 * Wicket components that implements this interface should call {@link Component#setOutputMarkupPlaceholderTag(boolean)} with true.
 * So that wicket ajax can replace/update the component.
 * and override the getMarkupId() so that the right servoy markupid is generated:
 * <pre>
 * 	public String getMarkupId()
	{
		return WebComponentSpecialIdMaker.getSpecialIdIfAppropriate(this);
	}
 * </pre>
 * </p>
 * @author jcompagner
 * @since 5.0
 * 
 * @see IStylePropertyChanges
 * @see ChangesRecorder
 */
public interface IProviderStylePropertyChanges
{
	/**
	 * Returns an instanceof {@link IStylePropertyChanges} that monitors the changes of this component
	 * See {@link ChangesRecorder} for an implementation that you can use for this.
	 *  
	 * @return The object that has the changed state for this component.
	 */
	public IStylePropertyChanges getStylePropertyChanges();
}
