/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.nongwt.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.servoy.base.nongwt.test.ILineMapper.LineMapping;

/**
 * Class that holds mappings from mobile generated solution JS to lines in the developer scope/form js files.
 * Useful for better jsunit test stack traces.
 * 
 * The line numbers in the map are 1 - based.
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class LineMapper implements ILineMapper
{

	// currently we don't remember the script file path, as the test client can already locate that based on generated function name
	private final Map<Long, LineMapping> mobileLineToDeveloperLine = new HashMap<Long, LineMapping>(128);
	protected final static Pattern HYBUGGER_LINE_EXTRACT_REGEX = Pattern.compile("JsHybugger\\.track\\(.*,\\D*(\\d*),.*\\);");

	private final static String PROP_PREFIX = "m";

	/**
	 * In this case the function's code is actually no longer the same as in developer; we need to map based on hybugger injected code.
	 * @param startMobileJSLine the line where this code will start in the exported mobile JS file.
	 * @param code hybugger injected code; parse and use the line numbers in there.
	 * @param filePath workspace relative file path to JS file.
	 */
	public void mapFunctionDebugMode(long startMobileJSLine, String code, String filePath)
	{
		// hybugger lines are 0 - based (so editor line 18 is actually hybugger line 17)
		// example:
		//    JsHybugger.track('http://127.0.0.1:8080/servoy_sample_mobile/forms/companies.js', 30, false);
		//    application.output("b");
		String[] lines = code.split("\r\n|\n|\r");
		Matcher m;
		for (int i = 0; i < lines.length; i++)
		{
			m = HYBUGGER_LINE_EXTRACT_REGEX.matcher(lines[i].trim());
			if (m.matches())
			{
				i++;
				mobileLineToDeveloperLine.put(Long.valueOf(startMobileJSLine + i), new LineMapping(Long.valueOf(m.group(1)).longValue() + 1, filePath));
			}
		}
	}

	/**
	 * The function code is identical as in developer. We only need to map all lines to their mobile counterparts.
	 * @param startMobileJSLine the line where this code will start in the exported mobile JS file.
	 * @param endMobileJSLine the line where this code will end in the exported mobile JS file.
	 * @param filePath workspace relative file path to JS file.
	 * @param startDeveloperJSLine start 1 - based line in developer js editor.
	 */
	public void mapFunction(long startMobileJSLine, long endMobileJSLine, String filePath, int startDeveloperJSLine)
	{
		for (long l = startMobileJSLine; l < endMobileJSLine; l++)
		{
			mobileLineToDeveloperLine.put(Long.valueOf(l), new LineMapping(startDeveloperJSLine + l - startMobileJSLine, filePath));
		}
	}

	public LineMapping mapToDeveloperScript(long mobileJSlineNumber)
	{
		return mobileLineToDeveloperLine.get(Long.valueOf(mobileJSlineNumber));
	}

	/**
	 * Writes the mappings as a property file:
	 * m[mobile_line_number]=[dev_line_number],[dev_path]
	 */
	public InputStream toProperties() throws IOException
	{
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		try
		{
			out = new ByteArrayOutputStream(mobileLineToDeveloperLine.size() * 30);

			Properties prop = new Properties();

			for (Map.Entry<Long, LineMapping> e : mobileLineToDeveloperLine.entrySet())
			{
				prop.put(PROP_PREFIX + e.getKey(), e.getValue().lineNumber + "," + e.getValue().file);
			}

			prop.store(out, null);

			in = new ByteArrayInputStream(out.toByteArray());
		}
		finally
		{
			if (out != null) out.close();
		}
		return in;
	}

	/**
	 * Writes the mappings as a property file:
	 * m[mobile_line_number]=[dev_line_number],[dev_path]
	 */
	public static LineMapper fromProperties(InputStream propertiesStream) throws IOException
	{
		LineMapper mapper = new LineMapper();

		try
		{
			Properties prop = new Properties();
			prop.load(propertiesStream);

			for (Map.Entry<Object, Object> e : prop.entrySet())
			{
				String value = e.getValue().toString();
				mapper.mobileLineToDeveloperLine.put(Long.valueOf(e.getKey().toString().substring(PROP_PREFIX.length())),
					new LineMapping(Long.valueOf(value.substring(0, value.indexOf(','))).longValue(), value.substring(value.indexOf(',') + 1)));
			}
		}
		finally
		{
			if (propertiesStream != null) propertiesStream.close();
		}
		return mapper;
	}

}
