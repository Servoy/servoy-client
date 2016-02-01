/*
 * Copyright (C) 2016 Servoy BV
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

package com.servoy.j2db.persistence;

import org.sablo.specification.PropertyDescription;

/**
 * Converts between the stored value and the corresponding java value.
 * @author emera
 */
public interface IDesignValueConverter<JT>
{
	JT fromDesignValue(Object newValue, PropertyDescription propertyDescription);

	Object toDesignValue(Object value, PropertyDescription pd);
}
