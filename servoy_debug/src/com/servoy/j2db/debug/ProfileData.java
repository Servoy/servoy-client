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
package com.servoy.j2db.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.scripting.JSEvent;

/**
 * @author jcompagner
 *
 */
public final class ProfileData
{

	private final String functionName;
	private final long time;
	private final String[] args;
	private final String sourceName;

	private final List<ProfileData> childs = new ArrayList<ProfileData>();
	private ProfileData parent;

	private final boolean isCalculation;
	private final String parentSourceCall;
	private final boolean innerFunction;
	private final int[] lineNumbers;
	private final List<DataCallProfileData> dataCallProfileDatas;

	/**
	 * @param functionName
	 * @param l
	 * @param args
	 * @param sourceName
	 * @param parentSourceCall
	 * @param innerFunction
	 * @param lineNumbers
	 * @param dataCallProfileDatas
	 */
	public ProfileData(String functionName, long time, Object[] args, String sourceName, String parentSourceCall, boolean innerFunction, int[] lineNumbers,
		List<DataCallProfileData> dataCallProfileDatas)
	{
		this.parentSourceCall = parentSourceCall;
		this.innerFunction = innerFunction;
		this.lineNumbers = lineNumbers;
		this.dataCallProfileDatas = dataCallProfileDatas;
		if (this.lineNumbers != null)
		{
			Arrays.sort(this.lineNumbers);
		}
		// calcs always end with _ and are always in a source file that ends with _calculations)
		if (functionName != null && functionName.endsWith("_") && sourceName.endsWith("_calculations.js"))
		{
			this.functionName = functionName.substring(0, functionName.length() - 1);
			this.isCalculation = true;
		}
		else
		{
			this.functionName = functionName == null ? "<eval>" : functionName;
			this.isCalculation = false;
		}
		this.time = time;
		if (args != null)
		{
			this.args = new String[args.length];
			for (int i = 0; i < args.length; i++)
			{
				if (args[i] instanceof Wrapper && !(args[i] instanceof NativeArray))
				{
					args[i] = ((Wrapper)args[i]).unwrap();
				}
				if (args[i] instanceof JSEvent)
				{
					JSEvent event = (JSEvent)args[i];
					StringBuilder sb = new StringBuilder();
					sb.append("JSEvent[");
					boolean added = false;
					if (event.getType() != null)
					{
						sb.append("type=");
						sb.append(event.getType());
						added = true;
					}
					if (event.getFormName() != null)
					{
						if (added) sb.append(",");
						sb.append("form=");
						sb.append(event.getFormName());
						added = true;
					}
					if (event.getElementName() != null)
					{
						if (added) sb.append(",");
						sb.append("element=");
						sb.append(event.getElementName());
						added = true;
					}
					if (event.getModifiers() != 0)
					{
						if (added) sb.append(",");
						sb.append("modifiers=");
						sb.append(event.getModifiers());
						added = true;
					}
					if (event.getX() != 0)
					{
						if (added) sb.append(",");
						sb.append("x=");
						sb.append(event.getX());
						added = true;
					}
					if (event.getY() != 0)
					{
						if (added) sb.append(",");
						sb.append("y=");
						sb.append(event.getY());
						added = true;
					}
					sb.append("]");
					this.args[i] = sb.toString();
				}
				else if (args[i] instanceof Undefined)
				{
					this.args[i] = "undefined";
				}
				else
				{
					this.args[i] = args[i] != null ? args[i].toString() : null;
				}
			}
		}
		else this.args = new String[] { };

		this.sourceName = sourceName;
	}

	/**
	 * @return the dataCallProfileDatas
	 */
	public List<DataCallProfileData> getDataCallProfileDatas()
	{
		return dataCallProfileDatas;
	}

	public long getDataQueriesTime()
	{
		long time = 0;
		if (dataCallProfileDatas != null && dataCallProfileDatas.size() > 0)
		{
			for (DataCallProfileData profile : dataCallProfileDatas)
			{
				time += profile.getTime();
			}
		}
		return time;
	}

	/**
	 * @param profileData
	 */
	public void addChild(ProfileData profileData)
	{
		profileData.setParent(this);
		childs.add(profileData);
	}

	public boolean isInnerFunction()
	{
		return innerFunction;
	}

	public int[] getLineNumbers()
	{
		return lineNumbers;
	}

	/**
	 * @return the isCalculation
	 */
	public boolean isCalculation()
	{
		return isCalculation;
	}

	/**
	 * @param profileData
	 */
	private void setParent(ProfileData parent)
	{
		this.parent = parent;
	}

	/**
	 * @return
	 */
	public ProfileData getParent()
	{
		return parent;
	}

	/**
	 * @return
	 */
	public ProfileData[] getChildren()
	{
		return childs.toArray(new ProfileData[0]);
	}

	/**
	 * @return
	 */
	public String getMethodName()
	{
		return functionName;
	}

	public String getArgs()
	{
		return Arrays.toString(args);
	}

	public long getOwnTime()
	{
		int childtime = 0;
		for (ProfileData pd : childs)
		{
			childtime += pd.time;
		}
		return time - childtime;
	}

	public long getTime()
	{
		return time;
	}

	public String getSourceName()
	{
		return sourceName;
	}

	/**
	 * @return the parentSourceCall
	 */
	public String getParentSourceCall()
	{
		return parentSourceCall;
	}

	/**
	 * @param sb
	 */
	public void toXML(StringBuilder sb)
	{
		String childPrefix = "\t";

		int endLine = sb.lastIndexOf("\n");
		if (endLine != -1)
		{
			childPrefix = sb.substring(endLine + 1) + '\t';
		}

		sb.append("<profiledata ");
		sb.append("methodname=\"");
		sb.append(functionName);
		sb.append("\" innerfuction=\"");
		sb.append(innerFunction);
		sb.append("\" owntime=\"");
		sb.append(getOwnTime());
		sb.append("\" totaltime=\"");
		sb.append(time);
		sb.append("\" args=\"");
		sb.append(getArgs());
		sb.append("\" source=\"");
		sb.append(sourceName);
		if (parentSourceCall != null)
		{
			sb.append("\" callposition=\"");
			sb.append(parentSourceCall);
		}
		sb.append("\">");
		if (dataCallProfileDatas != null)
		{
			for (DataCallProfileData dataCallProfileData : dataCallProfileDatas)
			{
				sb.append('\n');
				sb.append(childPrefix);
				dataCallProfileData.toXML(sb);
			}
		}
		for (ProfileData child : childs)
		{
			sb.append('\n');
			sb.append(childPrefix);
			child.toXML(sb);
		}
		sb.append("\n</profiledata>\n");
	}
}
