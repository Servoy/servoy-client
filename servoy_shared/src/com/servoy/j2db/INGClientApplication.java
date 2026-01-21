/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db;

import java.util.function.Function;

import org.json.JSONObject;

import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author jcompagner
 *
 * @since 8.0.3
 */
public interface INGClientApplication extends IApplication
{

	void overrideStyleSheet(String oldStyleSheet, String newStyleSheet);

	void setClipboardContent(String content);

	String getClipboardContent();

	String getMediaURL(String mediaName);

	JSONObject getBounds(String webComponentID, String subselector);

	/**
	 * @param weekday
	 */
	void setFirstDayOfTheWeek(int weekday);

	/**
	 * @return
	 */
	public JSONObject getUserAgentAndPlatform();

	/**
	 * If the persist is a form component component, it goes though it's child FC components (recursively) and calls addFCCChildConsumer
	 * for each one that has it's name set.
	 * @param addFCCChildConsumer a consumer that handles the children and returns true if it wants to consume more and false if it wants
	 * to stop consuming children. A null return value is equivalent to returning Boolean.TRUE.
	 */
	void addFormComponentComponentChildrenWithNames(IPersist elem, Function<IFormElement, Boolean> addFCCChildConsumer);

}
