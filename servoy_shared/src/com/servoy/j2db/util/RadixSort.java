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
package com.servoy.j2db.util;

// RadixSort.java version 1.0
// Java Source code for RadixSort

// Last updated by Roedy Green 1997 December 18
// Copyright 1996 Canadian Mind Products
// May be freely distributed for any purpose.

// RadixSort beat HeapSort which beat QuickSort.
// RadixSort works in linear time.  It is a
// little harder to use than ordinary sorts,
// but it can handle big sorts many times faster.
// Its main weakness is small sorts with long
// keys. The sort is stable.  It does not disturb
// pre-existing order of equal keys.
// author:Roedy Green <roedy@bix.com>

public class RadixSort
{

	// callback object we are passed that has
	// a getKeyByteAt(Object a, int offset) method.
	private RadixCompare comparer;

	// pointer to the array of user's objects we are sorting
	private Object [] userArray;

	// pointer to source work array
	private Object [] sourceArray;

	// pointer to target work array
	private Object [] targetArray;

	// how many bytes long the sorting key is.
	private int keyLength;

	// used to tally how many of each key byte there were.
	private int [] counts;

	public static void sortAsString(Object[] array, int maxStringLength )
	{
		sort(array,new UnicodeCompare(),maxStringLength);
	}
	// create a RadixSort object and sort the user's array
	public static void sort (Object [] userArray, RadixCompare comparer, int keyLength )
	{
		RadixSort h = new RadixSort();
		h.comparer = comparer;
		h.userArray = userArray;
		h.keyLength = keyLength;
		// We don't bother to test if isAlreadySorted
		// because it usually takes longer than sorting.
		h.radixSort();
		return;
	} // end sort

	// radixSort that works like a mechanical card
	// sorter, sorting least significant byte of the
	// key first. This works in linear time
	// proportional to key length and number of items
	// sorted.
	private void radixSort()
	{
		counts = new int[256];
		sourceArray = userArray;
		targetArray = new Object[userArray.length];
		// sort least significant column first,
		// working back to the most significant.
		for (int col=keyLength-1; col>0; col--)
		{
			sortCol(col);
			// swap source and target
			Object [] temp = sourceArray;
			sourceArray = targetArray;
			targetArray = temp;
		} // end for

		// copy results back to userArray, if necessary
		if (sourceArray != userArray)
		{
			System.arraycopy(sourceArray,0, userArray,0, sourceArray.length);
		}
	} // end radixSort

	// sort sourceArray by given column.  Put results in targetArray
	private void sortCol (int col)
	{
		// pass 1 count how many of each key there are:
		for (int i=0; i<counts.length; i++)
		{
			counts[i]=0;
		}
		for (int i=0; i<sourceArray.length; i++)
		{
			counts[comparer.getKeyByteAt(sourceArray[i],col)]++;
		} // end for

		// calculate slot number where each item will go.
		{
			int soFar = 0;
			for (int i=0; i<counts.length; i++)
			{
				int temp = counts[i];
				counts[i] = soFar;
				soFar += temp;
			} // end for
		} // end block
		// pass 2 move each object to its new slot
		for (int from=0; from<sourceArray.length; from++)
		{
			int keyByte = comparer.getKeyByteAt(sourceArray[from],col);
			int to = counts[keyByte]++;
			targetArray[to] = sourceArray[from];
		} // end for
	} // end sortCol

} // end class RadixSort
/*
public interface RadixCompare
{

    // Treat key as if it were a string of bytes
    // number returned must lie in the range 0..255!
    public abstract int getKeyByteAt(Object a, int offset);

    // to sort fixed length Latin1 strings you might write:
    // return ((String)a).charAt(offset)&0xff;

    // to sort fixed length Unicode strings you might write:
    //  if (offset%2 == 0 )
    //     return (((String)a).charAt(offset/2)>>>8)&0xff; // high byte
    //   else
    //     return ((String)a).charAt(offset/2)&0xff; // low byte

    // To sort binary integers you have to split them up into 4
    // unsigned bytes treating the most significant byte as offset 0.
    // If you were trying to sort signed ints, you would have
    // to add a bias to them to make them all appear positive
    // before breaking them up.
} // end class RadixCompare
*/
class UnicodeCompare implements RadixCompare
{

	public final int getKeyByteAt ( Object a, int offset)
	{
		if (a == null) return 0;
		String theString = a.toString().toLowerCase();
		if (offset >= theString.length()) return 0;
		if (offset%2 == 0 /* e.g. even */)
		{
		     return (theString.charAt(offset/2)>>>8)&0xff; // high byte
		}
		else
		{
		     return theString.charAt(offset/2)&0xff; // low byte
		}
	}
} // end class UnicodeCompare
