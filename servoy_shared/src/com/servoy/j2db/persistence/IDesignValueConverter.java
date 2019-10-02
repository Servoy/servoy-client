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
 * Converts between the .frm stored value and the corresponding java value.<br/><br/>
 *
 * I think this is about converting between what the underlying storage stores for persist
 * properties in the .frm file and the values returned or set through the Persist getters/setters to/from the Java world...
 *
 * @author emera
 */
public interface IDesignValueConverter<JT>
{
	/**
	 * Converts from design .frm value to java value.
	 * @param designValue what is stored in the .frm file as JSON value (obj/array/primitive)
	 *
	 * @return the java-world value that the persist property will have - corresponding to the given designValue.
	 */
	JT fromDesignValue(Object designValue, PropertyDescription propertyDescription);

	/**
	 * Converts from the jave-world persist property value to the design json value that will be stored in the .frm file.</br>
	 *
	 * IMPORTANT: it is possible that the given javaOrDesignJSONValue is already the design JSON value in which case it should be returned as it is. This can happen because
	 * JSNGWebComponent.setJSONProperty(...) applies IRhinoDesignConverter.fromRhinoToDesignValue(...) and then calls WebObjectImpl.setProperty(...) which then applies in turn
	 * this conversion (IDesignValueConverter.toDesignValue(...)). But IRhinoDesignConverter.fromRhinoToDesignValue(...) already returns a .frm design JSON value so applying this
	 * toDesignValue is no longer needed.<br/>
	 *
	 * FIXME: change JSNGWebComponent/WebObjectImpl so that the first argument can only be the java value - it would be cleaner and more predictable.
	 *
	 * @param javaOrDesignJSONValue either the persist propety java-world value or an already converted .frm design JSON value.
	 * @return the JSON design value to write into the .frm file.
	 */
	Object toDesignValue(Object javaValue, PropertyDescription pd);
}
