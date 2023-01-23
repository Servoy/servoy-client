/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.j2db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author vidmarian
 *
 */
public class PaletteCommonsHandler
{
	private static PaletteCommonsHandler instance;

	private TreeMap<Long, HashMap<String, Integer>> historyCfgMap;
	private TreeMap<Long, HashMap<String, Long>> historyTsMap;
	private HashMap<String, Integer> configMap;
	private HashMap<String, Long> tsMap;

	private final String historyCfgPath;
	private final String historyTsPath;
	private final String configPath;
	private final String tsPath;

	//todo: load from preferences
	private final int historySize = 14;
	private final int saveInterval = 24; //hours
	private final int commonsCategorySize = 5;

	private final JSONObject commonsCategory;
	private final SortedSet<Map.Entry<String, Double>> commonsPercentage;


	private long historyTimestamp;


	public static PaletteCommonsHandler getInstance(String wsPath)
	{
		if (instance == null)
		{
			instance = new PaletteCommonsHandler(wsPath);
		}
		return instance;
	}

	class MyComparator implements Comparator
	{

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Object o1, Object o2)
		{
			// TODO Auto-generated method stub
			return 0;
		}

	}

	@SuppressWarnings("unchecked")
	private PaletteCommonsHandler(String wsPath)
	{
		historyCfgPath = wsPath + File.separator + "resources" + File.separator + "hstCfg.obj"; //$NON-NLS-1$//$NON-NLS-2$
		historyTsPath = wsPath + File.separator + "resources" + File.separator + "hstTs.obj"; //$NON-NLS-1$//$NON-NLS-2$
		configPath = wsPath + File.separator + "resources" + File.separator + "cfg.obj"; //$NON-NLS-1$//$NON-NLS-2$
		tsPath = wsPath + File.separator + "resources" + File.separator + "ts.obj"; //$NON-NLS-1$//$NON-NLS-2$


		historyCfgMap = (TreeMap<Long, HashMap<String, Integer>>)getMapFromFile(historyCfgPath);
		historyTsMap = (TreeMap<Long, HashMap<String, Long>>)getMapFromFile(historyTsPath);
		configMap = (HashMap<String, Integer>)getMapFromFile(configPath);
		tsMap = (HashMap<String, Long>)getMapFromFile(tsPath);

		historyCfgMap = historyCfgMap != null ? historyCfgMap : new TreeMap<Long, HashMap<String, Integer>>();
		historyTsMap = historyTsMap != null ? historyTsMap : new TreeMap<Long, HashMap<String, Long>>();
		configMap = configMap != null ? configMap : new HashMap<String, Integer>();
		tsMap = tsMap != null ? tsMap : new HashMap<String, Long>();

		commonsCategory = new JSONObject();
		commonsCategory.put("packageName", "commons");
		commonsCategory.put("packageDisplayname", "Commonly Used");
		commonsPercentage = new TreeSet<>(new Comparator<Map.Entry<String, Double>>()
		{
			@Override
			public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2)
			{
				int result = e1.getValue().compareTo(e2.getValue());
				if (result == 0)
				{
					//the key which came first in alphabetical order gets higher priority
					result = e1.getKey().compareTo(e2.getKey()) * -1;
				}
				return result;
			}
		});
	}

	private Map< ? , ? > getMapFromFile(String filePath)
	{
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath)))
		{
			return (Map< ? , ? >)ois.readObject();
		}
		catch (IOException | ClassNotFoundException e)
		{
			return null;
		}

	}

	private void saveMapToFile(Map< ? , ? > map, String filePath) throws IOException
	{
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath)))
		{
			oos.writeObject(map);
		}

	}

	public void updateComponentCounter(String componentName) throws IOException
	{
		Integer counter = configMap.get(componentName);
		if (counter == null) counter = 0;
		counter++;
		configMap.put(componentName, counter);
		saveMapToFile(configMap, configPath);

		long utcTimestamp = ZonedDateTime.now().toInstant().toEpochMilli();
		tsMap.put(componentName, utcTimestamp);
		saveMapToFile(tsMap, tsPath);

		if (((utcTimestamp - historyTimestamp) > saveInterval * 60 * 60 * 1000))
		{
			if (historyTsMap.size() > this.historySize)
			{
				Long lastKey = historyTsMap.descendingMap().lastKey();//oldest record
				historyTsMap.remove(lastKey);
			}
			HashMap<String, Long> tsCopy = new HashMap<>();
			tsMap.forEach((k, v) -> tsCopy.put(k, v));
			historyTsMap.put(utcTimestamp, tsCopy);
			saveMapToFile(historyTsMap, historyTsPath);

			if (historyCfgMap.size() > this.historySize)
			{
				Long lastKey = historyCfgMap.descendingMap().lastKey();//oldest record
				historyCfgMap.remove(lastKey);
			}
			HashMap<String, Integer> cfgCopy = new HashMap<>();//clone config
			configMap.forEach((k, v) -> cfgCopy.put(k, v));
			historyCfgMap.put(utcTimestamp, cfgCopy);
			saveMapToFile(historyCfgMap, historyCfgPath);
		}

		//printMaps();
	}

	public JSONArray insertcommonsCategory(JSONArray packages)
	{
		long utcTimestamp = ZonedDateTime.now().toInstant().toEpochMilli();
		commonsPercentage.clear();
		for (int i = 0; i < packages.length(); i++)
		{
			JSONObject packageJSON = packages.getJSONObject(i);
			if (!packageJSON.keySet().contains("components")) continue;
			JSONArray components = packageJSON.getJSONArray("components");
			for (int j = 0; j < components.length(); j++)
			{
				JSONObject component = components.getJSONObject(j);
				double usagePercentage = getUsagePercentage(utcTimestamp, component.getString("name"));
				addComponentKeyToCommonsPercentageList(component.getString("name"), usagePercentage);
			}
		}
		JSONArray commonsComponents = getCommonsComponents(packages);
		commonsCategory.put("components", commonsComponents);
		JSONArray result = new JSONArray();
		result.put(commonsCategory);
		packages.forEach((value) -> {
			result.put(value);
		});

//		printMaps();

		return result;
	}

	private JSONArray getCommonsComponents(JSONArray packages)
	{
		List<String> keysList = new ArrayList<String>();
		commonsPercentage.forEach((value) -> {
			keysList.add(value.getKey());
		});
		JSONArray componentsArray = new JSONArray();
		for (int i = 0; i < packages.length(); i++)
		{
			JSONObject packageJSON = packages.getJSONObject(i);
			if (!packageJSON.keySet().contains("components")) continue;
			JSONArray components = packageJSON.getJSONArray("components");
			for (int j = 0; j < components.length(); j++)
			{
				JSONObject component = components.getJSONObject(j);
				if (keysList.contains(component.getString("name")))
				{
					componentsArray.put(component);
				}
			}
		}
		return sortJSONArray(componentsArray);
	}

	private JSONArray sortJSONArray(JSONArray jsonArray)
	{
		List<JSONObject> list = new ArrayList<JSONObject>();
		for (int i = 0; i < jsonArray.length(); i++)
		{
			list.add(jsonArray.getJSONObject(i));
		}
		Collections.sort(list, new Comparator<JSONObject>()
		{
			@Override
			public int compare(JSONObject a, JSONObject b)
			{
				return a.getString("displayName").compareTo(b.getString("displayName"));
			}
		});
		return new JSONArray(list);
	}

	private double getUsagePercentage(long currentTimestamp, String key)
	{
		Integer counter = configMap.get(key);
		Long timestamp = tsMap.get(key);
		if (counter == null)
		{
			Iterator<Map.Entry<Long, HashMap<String, Integer>>> it = historyCfgMap.descendingMap().entrySet().iterator();//map is in descending order
			while (it.hasNext())
			{
				Map.Entry<Long, HashMap<String, Integer>> entry = it.next(); //palette config
				counter = entry.getValue().get(key);
				if (counter != null)
				{
					timestamp = historyTsMap.get(entry.getKey()).get(key);
					break;
				}
			}
		}
		counter = counter == null ? 0 : counter;
		timestamp = timestamp == null ? 0 : timestamp;

		//timestamp is in miliseconds, converting to hours
		double interval = (((currentTimestamp - timestamp) / 1000.0) / 60.0) / 60.0;

		return counter / interval;
	}

	private void addComponentKeyToCommonsPercentageList(String key, double usagePercentage)
	{
		if (commonsPercentage.size() < commonsCategorySize)
		{
			boolean added = commonsPercentage.add(Map.entry(key, usagePercentage));
		}
		else
		{
			if (usagePercentage > commonsPercentage.first().getValue())
			{
				Map.Entry<String, Double> entry = commonsPercentage.first();
				commonsPercentage.removeIf(value -> value.getKey().equals(entry.getKey()));
				commonsPercentage.add(Map.entry(key, usagePercentage));
			}
		}
	}

	//use only for debug logics in java code
	private void printMaps()
	{
		System.out.println("########################");
		System.out.println("HISTORY");
		Iterator<Map.Entry<Long, HashMap<String, Integer>>> it = historyCfgMap.descendingMap().entrySet().iterator();
		int hsIndex = 0;
		while (it.hasNext())
		{
			Map.Entry<Long, HashMap<String, Integer>> entry = it.next(); //palette config
			Long hashmapTS = entry.getKey();
			System.out.println("   HS[" + hsIndex++ + "]" + hashmapTS);
			Iterator<Map.Entry<String, Integer>> it1 = entry.getValue().entrySet().iterator();
			while (it1.hasNext())
			{
				Map.Entry<String, Integer> entry1 = it1.next();
				System.out.println("        " + historyTsMap.get(entry.getKey()).get(entry1.getKey()) + " - " + entry1.getKey() + " - " + entry1.getValue());

			}
			System.out.println();
		}
		System.out.println("-----------------");
		System.out.println();
		System.out.println("LAST CONFIG");
		Iterator<Map.Entry<String, Integer>> cfgIt = configMap.entrySet().iterator();
		while (cfgIt.hasNext())
		{
			Map.Entry<String, Integer> entry = cfgIt.next();
			System.out.println("        " + tsMap.get(entry.getKey()) + " - " + entry.getKey() + " - " + entry.getValue());
		}
		System.out.println("########################");
		System.out.println();
	}
}
