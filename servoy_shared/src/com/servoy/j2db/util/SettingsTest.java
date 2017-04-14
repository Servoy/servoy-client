/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author pbakker
 *
 */
public class SettingsTest
{
	private static final Settings s = Settings.getInstance();
	private static File f;

	private static void setTestKeys(Map<String, String> testKeys) throws Exception
	{
		for (Entry<String, String> entry : testKeys.entrySet())
		{
			s.setProperty(entry.getKey(), entry.getValue());
		}
		s.save();
		s.loadFromFile(s.getFile());
	}

	private static void unsetTestKeys(Map<String, String> testKeys) throws Exception
	{
		for (Entry<String, String> entry : testKeys.entrySet())
		{
			s.remove(entry.getKey());
		}
		s.save();
		s.loadFromFile(s.getFile());
	}

	@BeforeClass
	public static void runOnceBeforeClass() throws IOException
	{
		f = s.getFile();
		s.loadFromFile(File.createTempFile("tmp", null));
	}

	@AfterClass
	public static void runOnceAfterClass() throws IOException
	{
		if (f != null)
		{
			s.loadFromFile(f);
		}
	}

	@Test
	public void testNestedKeys() throws Exception
	{
		Map<String, String> testKeys = new HashMap<String, String>()
		{
			{
				put("test.settings.name", "Johan");
				put("test.settings.hello", "Hello ${test.settings.name}");
				put("test.settings.whatsup", "${test.settings.hello}, what's up?");
			}
		};

		setTestKeys(testKeys);

		assertTrue(s.getProperty("test.settings.whatsup").equals("Hello Johan, what's up?"));

		unsetTestKeys(testKeys);
	}

	@Test
	public void testRecursionPrevention() throws Exception
	{
		Map<String, String> testKeys = new HashMap<String, String>()
		{
			{
				put("test.settings.recursion1", "${test.settings.recursion2}");
				put("test.settings.recursion2", "${test.settings.recursion1}");
			}
		};

		setTestKeys(testKeys);

		assertTrue(s.getProperty("test.settings.recursion1").equals("!!RECURSIVELY NESTED KEY!!"));

		unsetTestKeys(testKeys);
	}

	@Test
	public void testMultipleKeys() throws Exception
	{
		Map<String, String> testKeys = new HashMap<String, String>()
		{
			{
				put("test.settings.multiple", "${test.settings.one} - ${test.settings.two}");
				put("test.settings.one", "${test.settings.three}");
				put("test.settings.two", "${test.settings.three}");
				put("test.settings.three", "works");
			}
		};

		setTestKeys(testKeys);

		assertTrue(s.getProperty("test.settings.multiple"), s.getProperty("test.settings.multiple").equals("works - works"));

		unsetTestKeys(testKeys);
	}

	@Test
	public void testNonExistingKey() throws Exception
	{
		Map<String, String> testKeys = new HashMap<String, String>()
		{
			{
				put("test.settings.test", "value: ${test.settings.nonexisting}");
			}
		};

		setTestKeys(testKeys);

		assertTrue(s.getProperty("test.settings.test").equals("value: "));

		unsetTestKeys(testKeys);
	}

	@Test
	public void testSystemProperty() throws Exception
	{
		Map<String, String> testKeys = new HashMap<String, String>()
		{
			{
				put("test.settings.test", "${test.settings.value}");
				put("test.settings.value", "from property");
			}
		};

		System.setProperty("test.settings.value", "from system");
		setTestKeys(testKeys);

		assertTrue(s.getProperty("test.settings.test"), s.getProperty("test.settings.test").equals("from system"));

		unsetTestKeys(testKeys);
		System.clearProperty("test.settings.value");
	}

	public void testEnvironmentVariable() throws Exception
	{
		Map<String, String> testKeys = new HashMap<String, String>()
		{
			{
				put("test.settings.test", "${TEMP}");
			}
		};

		setTestKeys(testKeys);

		assertTrue(s.getProperty("test.settings.test").equals(System.getenv("TEMP")));

		unsetTestKeys(testKeys);
	}
}
