/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.servoy.j2db.server.ngclient;

/**
 * Implemented by classes that need to know when a property becomes 'dirty' - for example it needs to be sent to browser.
 *
 * @author acostescu
 */
public interface IDirtyPropertyListener
{

	/**
	 * @param contentsUpdated only matters if dirty is true; this is false if the prop. was changed by ref, true if it's value has changed (the smart value itself called changeMonitor.valueChanged()).
	 */
	void propertyFlaggedAsDirty(String propertyName, boolean dirty, boolean contentsUpdated);

}
