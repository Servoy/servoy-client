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
package com.servoy.j2db.documentation.scripting.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * Dummy class for listing methods for JavaScript types in a manner that
 * suits our documentation generator.
 *
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Object", scriptingName = "Object")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Object
{
	/**
	 * Determines whether two values are the same value.
	 *
	 * @sample
	 * Object.is('foo', 'foo');
	 *
	 * @param value1 The first value to compare.
	 * @param value2 The second value to compare.
	 * @return a Boolean indicating whether or not the two arguments are the same value.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/is
	 */
	public static boolean js_is(Object value1, Object value2)
	{
		return false;
	}

	/**
	 * Copy the values of all enumerable own properties from one or more source objects to a target object.
	 *
	 * @sample
	 * var object1 = { a: 1, b: 2, c: 3};
	 * var object2 = Object.assign({c: 4, d: 5}, object1);
	 * application.output(object2.c, object2.d);
	 *
	 * @param target The target object.
	 * @param sources The source object(s).
	 * @return The target object.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/assign
	 */
	public static Object js_assign(Object target, Object... sources)
	{
		return null;
	}

	/**
	 * Creates a new object, using an existing object to provide the newly created object's prototype.
	 * @param object The object which should be the prototype of the newly-created object.
	 * @return A new object with the specified prototype object.
	 *
	 * @sample
	 * const person = {
	 *	isHuman: false,
	 * 	printIntroduction: function () {
	 * 		application.output("My name is " + this.name + ". Am I human? " + this.isHuman);
	 * 	}
	 * };
	 * var me = Object.create(person);
	 * me.name = "Matthew"; // "name" is a property set on "me", but not on "person"
	 * me.isHuman = true; // inherited properties can be overwritten
	 * me.printIntroduction(); // expected output: "My name is Matthew. Am I human? true"
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/create
	 */
	public static Object js_create(Object object)
	{
		return null;
	}

	/**
	 * Creates a new object, using an existing object to provide the newly created object's prototype and properties.
	 * @param object The object which should be the prototype of the newly-created object.
	 * @param properties
	 * @return A new object with the specified prototype object.
	 *
	 * @sample
	 * var o = Object.create({}, { p: { value: 42 } });
	 * application.output(o.p);
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/create
	 */
	public static Object js_create(Object object, Object properties)
	{
		return null;
	}

	/**
	 * Defines new or modifies existing properties directly on an object, returning the object.
	 * @param object The object on which to define or modify properties.
	 * @param properties An object whose own enumerable properties constitute descriptors for the properties to be defined or modified.
	 * 			Descriptors have the following keys:
	 * 			configurable - true if and only if the type of this property descriptor may be changed and if the property may be deleted
	 * 							from the corresponding object. Defaults to false.
	 * 			enumerable - true if and only if this property shows up during enumeration of the properties on the corresponding object.
	 *							Defaults to false.
	 *			value - The value associated with the property. Can be any valid JavaScript value (number, object, function, etc).
	 *							Defaults to undefined.
	 *			writable - true if and only if the value associated with the property may be changed with an assignment operator.
	 *							Defaults to false.
	 *			get - A function which serves as a getter for the property, or undefined if there is no getter. The function return will
	 *				  be used as the value of property. Defaults to undefined.
	 *			set - A function which serves as a setter for the property, or undefined if there is no setter. The function will receive
	 *				  as only argument the new value being assigned to the property. Defaults to undefined.
	 *
	 * @return The object that was passed to the function.
	 *
	 * @sample
	 * const object1 = {};
	 * Object.defineProperties(object1, {property1: {value: 42, writable: true},   property2: {}});
	 * application.output(object1.property1);
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/defineProperties
	 */
	public static Object js_defineProperties(Object object, Object properties)
	{
		return null;
	}

	/**
	 * Allows a precise addition to or modification of a property on an object.
	 * @param object The object on which to define or modify properties.
	 * @param property The name of the property to be defined or modified.
	 * @param descriptor The descriptor for the property being defined or modified.
	 *
	 * @return The object that was passed to the function.
	 *
	 * @sample
	 * const object1 = {};
	 * Object.defineProperty(object1, 'property1', {value: 42, writable: false});
	 * application.output(object1.property1);
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/defineProperty
	 */
	public static Object js_defineProperty(Object object, Object property, Object descriptor)
	{
		return null;
	}

	/**
	 * Freezes an object: that is, prevents new properties from being added to it; prevents existing properties from being removed; and prevents
	 * existing properties, or their enumerability, configurability, or writability, from being changed. In essence the object is made effectively immutable.
	 * @param object The object to freeze.
	 *
	 * @return The object that was passed to the function.
	 *
	 * @sample
	 * const object1 = { property1: 42 };
	 * const object2 = Object.freeze(object1);
	 * object2.property1 = 33;
	 * application.output(object2.property1); //expected result is 42
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze
	 */
	public static Object js_freeze(Object object)
	{
		return null;
	}

	/**
	 * Permits examination of the precise description of a property.
	 * @param object The object in which to look for the property.
	 * @param property The name of the property whose description is to be retrieved.
	 * @return A property descriptor of the given property if it exists on the object, undefined otherwise.
	 *
	 * @sample
	 * const object1 = { property1: 42 };
	 * const descriptor1 = Object.getOwnPropertyDescriptor(object1, 'property1');
	 * application.output(descriptor1.configurable); // expected output: true
	 * application.output(descriptor1.value); //expected result is 42
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/getOwnPropertyDescriptor
	 */
	public static Object js_getOwnPropertyDescriptor(Object object, Object property)
	{
		return null;
	}

	/**
	 * Returns an array of all properties (including non-enumerable properties) found directly upon a given object.
	 * @param object The object whose enumerable and non-enumerable own properties are to be returned.
	 * @return An array of strings that correspond to the properties found directly upon the given object.
	 *
	 * @sample
	 * const object1 = { a: 1, b: 2, c: 3};
	 * application.output(Object.getOwnPropertyNames(object1));
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/getOwnPropertyNames
	 */
	public static Object[] js_getOwnPropertyNames(Object object)
	{
		return null;
	}

	/**
	 * Returns the prototype of the specified object.
	 * @param object The object whose prototype is to be returned.
	 * @return The prototype of the given object. If there are no inherited properties, null is returned.
	 *
	 * @sample
	 * const prototype1 = {};
	 * const object1 = Object.create(prototype1);
	 * application.output(Object.getPrototypeOf(object1) === prototype1); // expected output: true
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/getPrototypeOf
	 */
	public static Object js_getPrototypeOf(Object object)
	{
		return null;
	}

	/**
	 * Determines if an object is extensible (whether it can have new properties added to it).
	 * Objects are extensible by default, can be marked as non-extensible using Object.preventExtensions(), Object.seal(), or Object.freeze().
	 * @param object The object which should be checked.
	 * @return A Boolean indicating whether or not the given object is extensible.
	 *
	 * @sample
	 * var empty = {};
	 * Object.isExtensible(empty); // === true
	 *
	 * Object.preventExtensions(empty);
	 * Object.isExtensible(empty); // === false
	 *
	 * var sealed = Object.seal({});
	 * Object.isExtensible(sealed); // === false
	 *
	 * var frozen = Object.freeze({});
	 * Object.isExtensible(frozen); // === false
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/isExtensible
	 */
	public static boolean js_isExtensible(Object object)
	{
		return true;
	}

	/**
	 * Determines if an object is frozen. An object is frozen if and only if it is not extensible, all its properties are non-configurable, and all its data properties
	 * (that is, properties which are not accessor properties with getter or setter components) are non-writable.
	 * @param object The object which should be checked.
	 * @return A Boolean indicating whether or not the given object is frozen.
	 *
	 * @sample
	 * const object1 = { property1: 42 };
	 * application.output(Object.isFrozen(object1)); // expected output: false
	 * Object.freeze(object1);
	 * application.output(Object.isFrozen(object1)); // expected output: true
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/isFrozen
	 */
	public static boolean js_isFrozen(Object object)
	{
		return false;
	}


	/**
	 * Determines if an object is sealed. An object is sealed if it is not extensible and if all its properties are non-configurable and therefore not removable (but
	 * not necessarily non-writable).
	 * @param object The object which should be checked.
	 * @return A Boolean indicating whether or not the given object is sealed.
	 *
	 * @sample
	 * const object1 = { property1: 42 };
	 * application.output(Object.isSealed(object1)); // expected output: false
	 * Object.seal(object1);
	 * application.output(Object.isSealed(object1)); // expected output: true
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/isSealed
	 */
	public static boolean js_isSealed(Object object)
	{
		return false;
	}

	/**
	 * Returns an array of all own enumerable properties found upon a given object, in the same order as that provided by a for-in loop (the difference
	 * being that a for-in loop enumerates properties in the prototype chain as well).
	 * @param object An array of strings that represent all the enumerable properties of the given object.
	 * @return The object of which the enumerable's own properties are to be returned.
	 *
	 * @sample
	 * const object1 = {a: 'somestring', b: 42, c: false };
	 * application.outout(Object.keys(object1)); // expected output: Array ["a", "b", "c"]
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
	 */
	public static String[] js_keys(Object object)
	{
		return null;
	}

	/**
	 * Prevents new properties from ever being added to an object (i.e. prevents future extensions to the object).
	 * @param object The object which should be made non-extensible.
	 * @return The object being made non-extensible.
	 *
	 * @sample
	 * const object1 = {};
	 * Object.preventExtensions(object1);
	 * try {
	 *   Object.defineProperty(object1, 'property1', {
	 *       value: 42
	 *  });
	 *  } catch (e) {
	 *    application.output(e);
	 * }
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/preventExtensions
	 */
	public static Object js_preventExtensions(Object object)
	{
		return null;
	}

	/**
	 * Determine whether the object has the specified property as its own property (as opposed to inheriting it).
	 * @param prop The name of the property to test.
	 * @return A Boolean indicating whether or not the object has the specified property as own property.
	 *
	 * @sample
	 * 	const object1 = new Object();
	 *  object1.property1 = 42;
	 *  application.output(object1.hasOwnProperty('property1')); // expected output: true
	 *  application.output(object1.hasOwnProperty('toString')); // expected output: false
	 *  application.output(object1.hasOwnProperty('hasOwnProperty')); // expected output: false
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/hasOwnProperty
	 */
	public boolean js_hasOwnProperty(String prop)
	{
		return false;
	}

	/**
	 * Checks if an object exists in another object's prototype chain.
	 * @param object The object whose prototype chain will be searched.
	 * @return A Boolean indicating whether the calling object lies in the prototype chain of the specified object.
	 *
	 * @sample
	 * function object1() {}
	 * function object2() {}
	 * object1.prototype = Object.create(object2.prototype);
	 * const object3 = new object1();
	 * application.output(object1.prototype.isPrototypeOf(object3)); // expected output: true
	 * application.output(object2.prototype.isPrototypeOf(object3)); // expected output: true
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/isPrototypeOf
	 */
	public boolean js_isPrototypeOf(Object object)
	{
		return false;
	}

	/**
	 * Indicates whether the specified property is enumerable.
	 * @param prop The name of the property to test.
	 * @return A Boolean indicating whether the specified property is enumerable.
	 *
	 * @sample
	 * const array1 = [];
	 * object1.property1 = 42;
	 * array1[0] = 42;
	 * application.output(object1.propertyIsEnumerable('property1')); // expected output: true
	 * application.output(array1.propertyIsEnumerable(0)); // expected output: true
	 * application.output(array1.propertyIsEnumerable('length')); // expected output: false
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/propertyIsEnumerable
	 */
	public boolean js_propertyIsEnumerable(String prop)
	{
		return false;
	}

	/**
	 * Returns a string representing the object. This method is meant to be overriden by derived objects for locale-specific purposes.
	 * @return A string representing the object.
	 *
	 * @sample
	 * const number1 = 123456.789;
	 * application.output(number1.toLocaleString('de-DE')); // expected output: "123.456,789"
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/toLocaleString
	 */
	public String js_toLocaleString()
	{
		return null;
	}

	/**
	 * Returns a string representing the specified object.
	 * @return A string representing the object.
	 *
	 * @sample
	 * function Dog(name) {
	 *   this.name = name;
	 * }
	 * dog1 = new Dog('Spike');
	 * Dog.prototype.toString = function dogToString() { return this.name; }
	 *
	 * application.output(dog1.toString());
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/toString
	 */
	public String js_toString()
	{
		return null;
	}

	/**
	 * Returns the primitive value of the specified object. By default, the valueOf method is inherited by every object descended from Object.
	 * Every built-in core object overrides this method to return an appropriate value.
	 * If an object has no primitive value, valueOf returns the object itself.
	 * @return The primitive value of the specified object.
	 *
	 * @sample
	 * function MyNumberType(n) {
	 *  this.number = n;
	 * }
	 * MyNumberType.prototype.valueOf = function() { return this.number; };
	 *
	 * const object1 = new MyNumberType(4);
	 * application.output(object1 + 3); // expected output: 7
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/valueOf
	 */
	public Object js_valueOf()
	{
		return null;
	}

	/**
	 * Seals an object, preventing new properties from being added to it and marking all existing properties as non-configurable.
	 * Values of present properties can still be changed as long as they are writable.
	 * @param object The object which should be sealed.
	 * @return The object being sealed.
	 *
	 * @sample
	 * const object1 = { property1: 42 };
	 * Object.seal(object1);
	 * object1.property1 = 33;
	 * application.output(object1.property1); // expected output: 33
	 *
	 * delete object1.property1; // cannot delete when sealed
	 * application.output(object1.property1); // expected output: 33
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/seal
	 */
	public static Object js_seal(Object object)
	{
		return null;
	}
}
