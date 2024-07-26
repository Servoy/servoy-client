package com.servoy.j2db.server.headlessclient.dataui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import com.servoy.j2db.util.Pair;

public class TextualCSS extends TreeMap<String, TextualStyle>
{
	private static final String[] strings = new String[] { "input.button", "span.label", "input.field", ".label", ".field" };

	static
	{
		Arrays.sort(strings);
	}

	private static final long serialVersionUID = 1L;

	private static Comparator<String> css = new Comparator<String>()
	{

		public int compare(String o1, String o2)
		{
			if (o1.equals(o2)) return 0;
			int index1 = Arrays.binarySearch(strings, o1);
			int index2 = Arrays.binarySearch(strings, o2);
			if (index1 >= 0 && index2 >= 0)
			{
				return index1 - index2;
			}
			else if (index1 >= 0) return -1;
			else if (index2 >= 0) return 1;
			return (o1).compareToIgnoreCase(o2);
		}

	};

	private final Stack<ICSSBoundsHandler> handlers = new Stack<ICSSBoundsHandler>();

	public TextualCSS()
	{
		super(css);
		handlers.push(DefaultCSSBoundsHandler.INSTANCE);
	}

	public ArrayList<Pair<String, String>> getAsSelectorValuePairs()
	{
		ArrayList<Pair<String, String>> selectorValuePairs = new ArrayList<Pair<String, String>>();

		for (Map.Entry<String, TextualStyle> selectorTextualStyle : entrySet())
		{
			selectorValuePairs.add(new Pair<String, String>(selectorTextualStyle.getKey(), selectorTextualStyle.getValue().toString("")));
		}

		return selectorValuePairs;
	}

	@Override
	public String toString()
	{
		StringBuffer cssString = new StringBuffer();
		for (Object element_style : values())
		{
			cssString.append(element_style.toString());
		}
		return cssString.toString();
	}

	public ICSSBoundsHandler getCSSBoundsHandler()
	{
		return handlers.peek();
	}

	public void addCSSBoundsHandler(ICSSBoundsHandler b)
	{
		handlers.push(b);
	}

	public void removeCSSBoundsHandler()
	{
		handlers.pop();
	}

	public TextualStyle addStyle(String selector)
	{
		TextualStyle ts = get(selector);
		if (ts != null)
		{
			return ts;
		}
		ts = new TextualStyle(selector, this);
		put(selector, ts);
		return ts;
	}
}