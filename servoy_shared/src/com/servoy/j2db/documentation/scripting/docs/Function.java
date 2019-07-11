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
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Function", scriptingName = "Function")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Function
{
	/**
	 * Specifies the function that creates an object's prototype.
	 * 
	 * @sample
	 * function Tree(name) {
	 * 	this.name = name;
	 * }
	 * theTree = new Tree("Redwood");
	 * console.log("theTree.constructor is " + theTree.constructor);
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/constructor
	 */
	public Function js_getConstructor()
	{
		return null;
	}

	public void js_setConstructor(Function constructor)
	{

	}

	/**
	 * Specifies the number of arguments expected by the function.
	 * 
	 * @sample
	 * function addNumbers(x, y){
	 * 	if (addNumbers.length == 2) {
	 * 		return (x + y);
	 * 	} else
	 * 		return 0;
	 * }
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/length
	 */
	public Number js_getLength()
	{
		return null;
	}

	public void js_setLength(Number len)
	{

	}

	/**
	 * Applies the method of another object in the context of a different object (the calling object); arguments can be passed as an Array object.
	 * 
	 * @sample
	 * function book(name, author) {
	 * 	this.name = name;
	 * 	this.author = author;
	 * }
	 * 
	 * function book_with_topic(name, author, topic) {
	 * 	this.topic = topic;
	 * 	book.apply(this, arguments);
	 * }
	 * book_with_topic.prototype = new book();
	 *
	 * var aBook = new book_with_topic("name","author","topic");
	 * 
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/apply
	 */
	public void js_apply()
	{

	}

	/**
	 * Creates a new function which, when called, itself calls this function in the context of the provided value, with a given sequence of arguments preceding any provided when the new function was called.
	 * 
	 * @sample
	 * var x = 9, 
	 * 	module = {
	 * 		getX: function() { 
	 * 		     return this.x;
	 * 		},
	 * 		x: 81
	 * 	};
	 * //  "module.getX()" called, "module" is "this", "module.x" is returned
	 * module.getX(); // > 81
	 * //  "getX()" called, "this" is global, "x" is returned
	 * getX(); // > 9
	 * //  store a reference with "module" bound as "this"
	 * var boundGetX = getX.bind(module);
	 * //  "boundGetX()" called, "module" is "this" again, "module.x" is returned
	 * boundGetX(); // > 81
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/bind
	 */
	public void js_bind()
	{

	}

	/**
	 * Calls (executes) a method of another object in the context of a different object (the calling object); arguments can be passed as they are.
	 * 
	 * @sample
	 * function book(name) {
	 * 	this.name = name;
	 * }
	 * 
	 * function book_with_author(name, author) {
	 * 	this.author = author;
	 * 	book.call(this, name);
	 * }
	 * book_with_author.prototype = new book();
	 *
	 * var aBook = new book_with_author("name","author");
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/call
	 */
	public void js_call()
	{

	}

	/**
	 * Returns a string representing the source code of the function. Overrides the Object.toString method.
	 * 
	 * @sample
	 * function printHello() {
	 * 	return "Hello";
	 * }
	 * application.output(printHello.toString()); 
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/toString
	 */
	public String js_toString()
	{
		return null;
	}
}
