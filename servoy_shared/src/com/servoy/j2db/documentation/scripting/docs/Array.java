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

import org.mozilla.javascript.Function;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * The javascript Array implementation.<br/>
 * It is a collection of items kept by index, providing typical array API. The index is a integer (>= 0) or a string representing such an integer.<br/><br/>
 *
 * For more information see: <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array">Array (MDN)</a>.
 *
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Array", scriptingName = "Array")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Array
{
	/**
	 * Get an element by index.
	 *
	 * @sample array[0]
	 */
	public Object js_getArray__indexedby_index()
	{
		return null;
	}

	public void js_setArray__indexedby_index(Object indexIndex)
	{
	}

	/**
	 * Get the length of the array.
	 *
	 * @sample array.length
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/length
	 */
	public Number js_getLength()
	{
		return null;
	}

	public void js_setLength(Number length)
	{
	}

	/**
	 * Constructs a new default array
	 *
	 * @sample var array = new Array();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array
	 */
	public void jsConstructor_Array()
	{
	}

	/**
	 * Constructs a new array with specified size.
	 *
	 * @sample var array = new Array(number);
	 *
	 * @param number
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array
	 */
	public void jsConstructor_Array(Number number)
	{
	}

	/**
	 * Constructs a new array that contains the given values.
	 *
	 * @sample var array = new Array(value1,value2);
	 *
	 * @param value1
	 * @param value2
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array
	 */
	public void jsConstructor_Array(Object value1, Object value2)
	{
	}

	/**
	 * @clonedesc jsConstructor_Array(Object, Object)
	 * @sampleas jsConstructor_Array(Object, Object)
	 *
	 * @param value1
	 * @param value2
	 * @param valueN
	 *
	 */
	public void jsConstructor_Array(Object value1, Object value2, Object valueN)
	{
	}

	/**
	 * Returns a new array comprised of this array joined with other array(s) and/or value(s).
	 *
	 * @sample array.concat();
	 *
	 * @param value1
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/concat
	 */
	public Array js_concat(Object value1)
	{
		return null;
	}

	/**
	 * @clonedesc js_concat(Object)
	 * @sampleas js_concat(Object)
	 *
	 * @param value1
	 * @param value2
	 *
	 */
	public Array js_concat(Object value1, Object value2)
	{
		return null;
	}

	/**
	 * @clonedesc js_concat(Object)
	 * @sampleas js_concat(Object)
	 *
	 * @param value1
	 * @param value2
	 * @param valueN
	 *
	 */
	public Array js_concat(Object value1, Object value2, Object valueN)
	{
		return null;
	}

	/**
	 *  Shallow copies part of an array to another location in the same array and returns it without modifying its length
	 *
	 * @sample array.copyWithin(2);
	 *
	 * @param target Zero-based index at which to copy the sequence to. If negative, target will be counted from the end.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/copyWithin
	 */
	public Array js_copyWithin(int target)
	{
		return null;
	}

	/**
	 * @clonedesc js_copyWithin(int)
	 * @sampleas js_copyWithin(int)
	 *
	 * @param target Zero-based index at which to copy the sequence to. If negative, target will be counted from the end.
	 * @param start Zero-based index at which to start copying elements from. If negative, start will be counted from the end. If start is omitted, copyWithin will copy from index 0.
	 *
	 */
	public Array js_copyWithin(int target, int start)
	{
		return null;
	}

	/**
	 * @clonedesc js_copyWithin(int)
	 * @sampleas js_copyWithin(int)
	 *
	 * @param target Zero-based index at which to copy the sequence to. If negative, target will be counted from the end.
	 * @param start Zero-based index at which to start copying elements from. If negative, start will be counted from the end. If start is omitted, copyWithin will copy from index 0.
	 * @param end Zero-based index at which to end copying elements from. copyWithin copies up to but not including end. If negative, end will be counted from the end.
	 *
	 */
	public Array js_copyWithin(int target, int start, int end)
	{
		return null;
	}

	/**
	 * Runs a function on items in the array while that function is returning true. It returns true if the function returns true for every item it could visit.
	 * The callback function is invoked with three arguments: the element value, the element index, the array being traversed.
	 *
	 * @sample
	 * function isNumber(value) { return typeof value == 'number'; }
	 * var a1 = [1, 2, 3];
	 * application.output(a1.every(isNumber));
	 * var a2 = [1, '2', 3];
	 * application.output(a2.every(isNumber));
	 *
	 * @param callback
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/every
	 */
	public Boolean js_every(Function callback)
	{
		return null;
	}

	/**
	 * @clonedesc js_every(Function)
	 * @sampleas js_every(Function)
	 *
	 * @param callback
	 * @param thisObject
	 *
	 */
	public Boolean js_every(Function callback, Array thisObject)
	{
		return null;
	}

	/**
	 *  Changes all elements in an array to a static value, from a start index (default 0) to an end index (default array.length). It returns the modified array.
	 *
	 * @sample array.fill('test');
	 *
	 * @param value Value to fill the array with.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/fill
	 */
	public Array js_fill(Object value)
	{
		return null;
	}

	/**
	 * @clonedesc js_fill(Object)
	 * @sampleas js_fill(Object)
	 *
	 * @param value Value to fill the array with.
	 * @param start Zero-based index at which to start filling.
	 *
	 */
	public Array js_fill(Object value, int start)
	{
		return null;
	}

	/**
	 * @clonedesc js_fill(Object)
	 * @sampleas js_fill(Object)
	 *
	 * @param value Value to fill the array with.
	 * @param start Zero-based index at which to start filling.
	 * @param end Zero-based index at which to end filling.
	 *
	 */
	public Array js_fill(Object value, int start, int end)
	{
		return null;
	}

	/**
	 * Runs a function on every item in the array and returns an array of all items for which the function returns true.
	 * The callback function is invoked with three arguments: the element value, the element index, the array being traversed.
	 *
	 * @sample
	 * var a1 = ['a', 10, 'b', 20, 'c', 30];
	 * var a2 = a1.filter(function(item) { return typeof item == 'number'; });
	 * application.output(a2);
	 *
	 * @param callback
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/filter
	 */
	public Array js_filter(Function callback)
	{
		return null;
	}

	/**
	 * @clonedesc js_filter(Function)
	 * @sampleas js_filter(Function)
	 *
	 * @param callback
	 * @param thisObject
	 *
	 */
	public Array js_filter(Function callback, Array thisObject)
	{
		return null;
	}

	/**
	 * Runs a function (callback) on every item in the array. The callback function is invoked only for indexes of the array which have assigned values.
	 * The callback function is invoked with three arguments: the element value, the element index, the array being traversed.
	 *
	 * @sample
	 * function printThemOut(element, index, array) {
	 * 		application.output("a[" + index + "] = " + element);
	 * }
	 * var a = ['a', 'b', 'c'];
	 * a.forEach(printThemOut);
	 *
	 * @param callback
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/forEach
	 */
	public void js_forEach(Function callback)
	{
	}

	/**
	 * @clonedesc js_forEach(Function)
	 * @sampleas js_forEach(Function)
	 *
	 * @param callback
	 * @param thisObject
	 *
	 */
	public void js_forEach(Function callback, Object thisObject)
	{
	}

	/**
	 * Creates a new, shallow-copied Array instance from an iterable or array-like object.
	 *
	 * @sample
	 * var a = Array.from([1, 2, 3]);
	 *
	 * @param value An iterable or array-like object to convert to an array.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/from
	 */
	public Array js_from(Object value)
	{
		return null;
	}

	/**
	 * @clonedesc js_from(Object)
	 * @sampleas js_from(Object)
	 *
	 * @param value An iterable or array-like object to convert to an array.
	 * @param mapFunction Map function to call on every element of the array. If provided, every value to be added to the array is first passed through this function, and mapFunction's return value is added to the array instead.
	 * @param thisObject Value to use as this when executing mapFunction.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/of
	 */
	public Array js_from(Object value, Function mapFunction, Object thisObject)
	{
		return null;
	}

	/**
	 * Determines whether an array includes a certain value among its entries, returning true or false as appropriate.
	 *
	 * @sample array.includes('test');
	 *
	 * @param searchElement The value to search for.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/includes
	 */
	public Boolean js_includes(Object searchElement)
	{
		return null;
	}

	/**
	 * @clonedesc js_includes(Object)
	 * @sampleas js_includes(Object)
	 *
	 * @param searchElement The value to search for.
	 * @param start Zero-based index at which to start searching.
	 *
	 */
	public Boolean js_includes(Object searchElement, int start)
	{
		return null;
	}

	/**
	 * Returns the first index at which a given element can be found in the array, or -1 if it is not present.
	 *
	 * @sample
	 * var a = ['a', 'b', 'a', 'b', 'a'];
	 * application.output(a.indexOf('b'));
	 * application.output(a.indexOf('b', 2));
	 * application.output(a.indexOf('z'));
	 *
	 * @param searchElement
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/indexOf
	 */
	public Number js_indexOf(Object searchElement)
	{
		return null;
	}

	/**
	 * @clonedesc js_indexOf(Object)
	 * @sampleas js_indexOf(Object)
	 *
	 * @param searchElement
	 * @param fromIndex
	 *
	 */
	public Number js_indexOf(Object searchElement, Number fromIndex)
	{
		return null;
	}

	/**
	 * Puts all elements in the array into a string, separating each element with the specified delimiter
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete","In","Out");
	 * var jwords = words.join(";");
	 *
	 * @param delimiter
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/join
	 */
	public String js_join(String delimiter)
	{
		return null;
	}

	/**
	 * Returns the last index at which a given element can be found in the array, or -1 if it is not present. The array is searched backwards, starting at fromIndex.
	 *
	 * @sample
	 * var a = ['a', 'b', 'c', 'd', 'a', 'b'];
	 * application.output(a.lastIndexOf('b'));
	 * application.output(a.lastIndexOf('b', 4));
	 * application.output(a.lastIndexOf('z'));
	 *
	 * @param searchElement
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/lastIndexOf
	 */
	public Number js_lastIndexOf(Object searchElement)
	{
		return null;
	}

	/**
	 * @clonedesc js_lastIndexOf(Object)
	 * @sampleas js_lastIndexOf(Object)
	 *
	 * @param searchElement
	 * @param fromIndex
	 *
	 */
	public Number js_lastIndexOf(Object searchElement, Number fromIndex)
	{
		return null;
	}

	/**
	 * Runs a function on every item in the array and returns the results in an array.
	 * The callback function is invoked with three arguments: the element value, the element index, the array being traversed.
	 *
	 * @sample
	 * var a = ['a', 'b', 'c'];
	 * var a2 = a.map(function(item) { return item.toUpperCase(); });
	 * application.output(a2);
	 *
	 * @param callback
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/map
	 */
	public Array js_map(Object callback)
	{
		return null;
	}

	/**
	 * @clonedesc js_map(Object)
	 * @sampleas js_map(Object)
	 *
	 * @param callback
	 * @param thisObject
	 *
	 */
	public Array js_map(Object callback, Array thisObject)
	{
		return null;
	}

	/**
	 * Ccreates a new Array instance from a variable number of arguments.
	 *
	 * @sample
	 * var a = Array.of(1, 2, 3);
	 *
	 * @param value
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/of
	 */
	public Array js_of(Object... value)
	{
		return null;
	}

	/**
	 * Pops the last string off the array and returns it.
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete","In","Out");
	 * var lastword = words.pop();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/pop
	 */
	public Object js_pop()
	{
		return null;
	}

	/**
	 * Mutates an array by appending the given elements and returning the new length of the array.
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete");
	 * words.push("In","Out");
	 *
	 * @param value1
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/push
	 */
	public Number js_push(Object value1)
	{
		return null;
	}

	/**
	 * @clonedesc js_push(Object)
	 * @sampleas js_push(Object)
	 *
	 * @param value1
	 * @param value2
	 *
	 */
	public Number js_push(Object value1, Object value2)
	{
		return null;
	}

	/**
	 * @clonedesc js_push(Object)
	 * @sampleas js_push(Object)
	 *
	 * @param value1
	 * @param value2
	 * @param valueN
	 *
	 */
	public Number js_push(Object value1, Object value2, Object valueN)
	{
		return null;
	}

	/**
	 * Puts array elements in reverse order.
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete","In","Out");
	 * words.reverse();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/reverse
	 */
	public Array js_reverse()
	{
		return null;
	}

	/**
	 * Decreases array element size by one by shifting the first element off the array and returning it.
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete","In","Out");
	 * words.shift();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/shift
	 */
	public Object js_shift()
	{
		return null;
	}

	/**
	 * Reduces the array to a single value by executing a provided function for each value of the array (from left-to-right).
	 *
	 * @sample
	 * var euros = [29.76, 41.85, 46.5];
	 * var sum = euros.reduce( function(total, amount) {
	 *   return total + amount
	 * });
	 *
	 * @param f Function to execute on each element in the array, taking four arguments:
	 *  		-accumulator: accumulates the callback's return values; it is the accumulated value previously returned
	 * 			              in the last invocation of the callback, or initialValue, if supplied (see below).
	 * 			-currentValue: the current element being processed in the array.
	 * 			-currentIndex (Optional): the index of the current element being processed in the array (starts at index 0,
	 * 						  if an initialValue is provided, and at index 1 otherwise)
	 * 			-array (Optional): the array reduce() was called upon.
	 * @param initialValue Value to use as the first argument to the first call of the callback. If no initial value is supplied,
	 * 					   the first element in the array will be used.
	 * @return Object
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/reduce
	 */
	public Object js_reduce(Function f, Object initialValue)
	{
		return null;
	}

	/**
	 * The slice method creates a new array from a selected section of an array.
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete","In","Out");
	 * var nwords1 = words.slice(3, 5);
	 *
	 * @param begin
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/slice
	 */
	public Array js_slice(Object begin)
	{
		return null;
	}

	/**
	 * @clonedesc js_slice(Object)
	 * @sampleas js_slice(Object)
	 *
	 * @param begin
	 * @param end
	 *
	 */
	public Array js_slice(Object begin, Object end)
	{
		return null;
	}

	/**
	 * Runs a function on items in the array while that function returns false. It returns true if the function returns true for any item it could visit.
	 * The callback function is invoked with three arguments: the element value, the element index, the array being traversed.
	 *
	 * @sample
	 * function isNumber(value) { return typeof value == 'number'; }
	 * var a1 = [1, 2, 3];
	 * application.output(a1.some(isNumber));
	 * var a2 = [1, '2', 3];
	 * application.output(a2.some(isNumber));
	 *
	 * @param callback
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/some
	 */
	public Boolean js_some(Function callback)
	{
		return null;
	}

	/**
	 * @clonedesc js_some(Function)
	 * @sampleas js_some(Function)
	 *
	 * @param callback
	 * @param thisObject
	 *
	 */
	public Boolean js_some(Function callback, Array thisObject)
	{
		return null;
	}

	/**
	 * Sorts the array elements in dictionary order or using a compare function passed to the method.
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete","In","Out");
	 * words.sort();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/sort
	 */
	public Array js_sort()
	{
		return null;
	}

	/**
	 * @clonedesc js_sort()
	 * @sampleas js_sort()
	 *
	 * @param function
	 *
	 */
	public Array js_sort(Function function)
	{
		return null;
	}

	/**
	 * It is used to take elements out of an array and replace them with those specified.
	 *
	 * @sample
	 * var words = new Array("limit","lines","finish","complete","In","Out");
	 * var nwords1 = words.splice(3, 2, "done", "On");
	 *
	 * @param arrayIndex
	 * @param length
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/splice
	 */
	public Array js_splice(Object arrayIndex, Object length)
	{
		return null;
	}

	/**
	 * @clonedesc js_splice(Object, Object)
	 * @sampleas js_splice(Object, Object)
	 *
	 * @param arrayIndex
	 * @param length
	 * @param value1
	 *
	 */
	public Array js_splice(Object arrayIndex, Object length, Object value1)
	{
		return null;
	}

	/**
	 * @clonedesc js_splice(Object, Object)
	 * @sampleas js_splice(Object, Object)
	 *
	 * @param arrayIndex
	 * @param length
	 * @param value1
	 * @param value2
	 *
	 */
	public Array js_splice(Object arrayIndex, Object length, Object value1, Object value2)
	{
		return null;
	}

	/**
	 * @clonedesc js_splice(Object, Object)
	 * @sampleas js_splice(Object, Object)
	 *
	 * @param arrayIndex
	 * @param length
	 * @param value1
	 * @param value2
	 * @param valueN
	 *
	 */
	public Array js_splice(Object arrayIndex, Object length, Object value1, Object value2, Object valueN)
	{
		return null;
	}

	/**
	 * Places element data at the start of an array.
	 *
	 * @sample
	 * var words = new Array("finish","complete","In","Out");
	 * words.unshift("limit","lines");
	 *
	 * @param value1
	 * @param value2
	 * @param valueN
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Array/unshift
	 */
	public Number js_unshift(Object value1, Object value2, Object valueN)
	{
		return null;
	}

	/**
	 * Checks whether an object is an array or not.
	 *
	 * @sample
	 * var a = [1, 2, 3];
	 * application.output(Array.isArray(a)); //prints true
	 * application.output(Array.isArray(23)); //prints false
	 *
	 * @param obj
	 *
	 * @link https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Global_Objects/Array/isArray
	 */
	public Boolean js_isArray(Object obj)
	{
		return null;
	}

	/**
	 *  Returns the value of the first element in the provided array that satisfies the provided testing function.
	 *  If no values satisfy the testing function, undefined is returned.
	 *  The callback function can invoked with three arguments: the element value, the element index(optional), the array being traversed (optional).
	 *
	 *  @sample
	 *  var array1 = [5, 12, 8, 130, 44];
	 *  var found = array1.find(function(element) { return element > 10});
	 *  application.output(found); // prints 12
	 *
	 * @param callback a testing function
	 * @return the element which satisfies the function or undefined
	 */
	public Object js_find(Function callback)
	{
		return null;
	}

	/**
	 *  Returns the index of the first element in the provided array which satisfies the provided testing function.
	 *  If no values satisfy the testing function, -1 is returned.
	 *  The callback function can invoked with three arguments: the element value, the element index (optional), the array being traversed (optional).
	 *
	 *  @sample
	 *  var array1 = [5, 12, 8, 130, 44];
	 *  var found = array1.findIndex(function(element) { return element > 10});
	 *  application.output(found); // prints 1
	 *
	 * @param callback a testing function
	 * @return the index of the first element which satisfies the function or -1
	 */
	public Number js_findIndex(Function callback)
	{
		return null;
	}
}
