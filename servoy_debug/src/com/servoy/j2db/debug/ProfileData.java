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
	private final Object[] args;
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
		if (functionName.endsWith("_") && sourceName.endsWith("_calculations.js")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			this.functionName = functionName.substring(0, functionName.length() - 1);
			this.isCalculation = true;
		}
		else
		{
			this.functionName = functionName;
			this.isCalculation = false;
		}
		this.time = time;
		this.args = args;
		this.sourceName = sourceName;


		for (int i = 0; i < args.length; i++)
		{
			if (args[i] instanceof Wrapper)
			{
				args[i] = ((Wrapper)args[i]).unwrap();
			}
			if (args[i] instanceof JSEvent)
			{
				JSEvent event = (JSEvent)args[i];
				StringBuilder sb = new StringBuilder();
				sb.append("JSEvent["); //$NON-NLS-1$
				boolean added = false;
				if (event.js_getType() != null)
				{
					sb.append("type="); //$NON-NLS-1$
					sb.append(event.js_getType());
					added = true;
				}
				if (event.js_getFormName() != null)
				{
					if (added) sb.append(","); //$NON-NLS-1$
					sb.append("form="); //$NON-NLS-1$
					sb.append(event.js_getFormName());
					added = true;
				}
				if (event.js_getElementName() != null)
				{
					if (added) sb.append(","); //$NON-NLS-1$
					sb.append("element="); //$NON-NLS-1$
					sb.append(event.js_getElementName());
					added = true;
				}
				if (event.js_getModifiers() != 0)
				{
					if (added) sb.append(","); //$NON-NLS-1$
					sb.append("modifiers="); //$NON-NLS-1$
					sb.append(event.js_getModifiers());
					added = true;
				}
				if (event.js_getX() != 0)
				{
					if (added) sb.append(","); //$NON-NLS-1$
					sb.append("x="); //$NON-NLS-1$
					sb.append(event.js_getX());
					added = true;
				}
				if (event.js_getY() != 0)
				{
					if (added) sb.append(","); //$NON-NLS-1$
					sb.append("y="); //$NON-NLS-1$
					sb.append(event.js_getY());
					added = true;
				}
				sb.append("]"); //$NON-NLS-1$
				args[i] = sb.toString();
			}
		}
	}

	/**
	 * @return the dataCallProfileDatas
	 */
	public List<DataCallProfileData> getDataCallProfileDatas()
	{
		return dataCallProfileDatas;
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
		String childPrefix = "\t"; //$NON-NLS-1$

		int endLine = sb.lastIndexOf("\n"); //$NON-NLS-1$
		if (endLine != -1)
		{
			childPrefix = sb.substring(endLine + 1) + '\t';
		}

		sb.append("<profiledata "); //$NON-NLS-1$
		sb.append("methodname=\""); //$NON-NLS-1$
		sb.append(functionName);
		sb.append("\" innerfuction=\""); //$NON-NLS-1$
		sb.append(innerFunction);
		sb.append("\" owntime=\""); //$NON-NLS-1$
		sb.append(getOwnTime());
		sb.append("\" totaltime=\""); //$NON-NLS-1$
		sb.append(time);
		sb.append("\" args=\""); //$NON-NLS-1$
		sb.append(getArgs());
		sb.append("\" source=\""); //$NON-NLS-1$
		sb.append(sourceName);
		if (parentSourceCall != null)
		{
			sb.append("\" callposition=\""); //$NON-NLS-1$
			sb.append(parentSourceCall);
		}
		sb.append("\">"); //$NON-NLS-1$
		for (ProfileData child : childs)
		{
			sb.append('\n');
			sb.append(childPrefix);
			child.toXML(sb);
		}
		sb.append("</profiledata>"); //$NON-NLS-1$
	}
}
