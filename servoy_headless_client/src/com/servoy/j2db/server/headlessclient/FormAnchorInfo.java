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
package com.servoy.j2db.server.headlessclient;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import org.apache.wicket.Component;

import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.server.headlessclient.dataui.WebDataButton;
import com.servoy.j2db.server.headlessclient.dataui.WebDataHtmlArea;
import com.servoy.j2db.server.headlessclient.dataui.WebDataImgMediaField.ImageDisplay;
import com.servoy.j2db.server.headlessclient.dataui.WebDataLabel;
import com.servoy.j2db.server.headlessclient.dataui.WebDataSubmitLink;
import com.servoy.j2db.server.headlessclient.dataui.WebScriptButton;
import com.servoy.j2db.server.headlessclient.dataui.WebScriptLabel;
import com.servoy.j2db.server.headlessclient.dataui.WebScriptSubmitLink;
import com.servoy.j2db.server.headlessclient.dataui.WebTabPanel;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Holds the anchoring informations for one form. These informations are sent to web clients
 * and then used on client side.
 * 
 * @author acostache, gerzse
 */
public final class FormAnchorInfo implements Comparable<FormAnchorInfo>
{
	public String formName;
	public Dimension formSize;
	public UUID formID;
	public String navigatorWebId;
	public boolean isTableView;
	public boolean isTopLevelForm;
	public boolean isTopLevelNavigator;
	public String bodyContainerId;
	private Map<String, FormPartAnchorInfo> parts;

	private static final String BODY_PART_VAR = "bodyPart"; //$NON-NLS-1$
	private static final String NON_BODY_PARTS_VAR = "nonBodyParts"; //$NON-NLS-1$
	private static final String NON_BODY_PARTS_IDS_VAR = "nonBodyPartsIds"; //$NON-NLS-1$
	private static final String ANCHOR_INFO_VAR = "ai"; //$NON-NLS-1$
	private static final String HEIGHTS_VAR = "heights"; //$NON-NLS-1$
	private static final String FORM_VAR = "f"; //$NON-NLS-1$

	public FormAnchorInfo(String name, Dimension size, UUID id)
	{
		formName = name;
		formSize = size;
		formID = id;
	}

	public FormPartAnchorInfo addPart(String partName, String webID, int height)
	{
		if (parts == null) parts = new HashMap<String, FormPartAnchorInfo>();
		FormPartAnchorInfo newPart = new FormPartAnchorInfo(partName, webID, height);
		parts.put(partName, newPart);
		return newPart;
	}

	public FormPartAnchorInfo getPart(String partName)
	{
		if (parts.containsKey(partName)) return parts.get(partName);
		else return null;
	}

	private void generateAnchoringCode(StringBuffer sb)
	{
		if (parts != null)
		{
			sb.append("\n");
			sb.append("\t").append(FORM_VAR).append(" = {};\n");
			sb.append("\t").append(FORM_VAR).append(".formName = '").append(formName).append("';\n");
			if (isTopLevelForm) sb.append("\t").append(FORM_VAR).append(".isTopLevelForm = ").append(isTopLevelForm).append(";\n");
			if (isTopLevelNavigator) sb.append("\t").append(FORM_VAR).append(".isTopLevelNavigator = ").append(isTopLevelNavigator).append(";\n");
			sb.append("\t").append(FORM_VAR).append(".isTableView = ").append(isTableView).append(";\n");
			sb.append("\t").append(FORM_VAR).append(".width = ").append(formSize.width).append(";\n");
			sb.append("\t").append(FORM_VAR).append(".height = ").append(formSize.height).append(";\n");
			sb.append("\t").append(FORM_VAR).append(".bodyContainerId = '").append(bodyContainerId).append("';\n");
			sb.append("\t").append(FORM_VAR).append(".").append(NON_BODY_PARTS_VAR).append(" = {};\n");
			sb.append("\t").append(FORM_VAR).append(".").append(NON_BODY_PARTS_IDS_VAR).append(" = {};\n");
			sb.append("\t").append(FORM_VAR).append(".").append(BODY_PART_VAR).append(" = undefined;\n");
			sb.append("\t").append(FORM_VAR).append(".").append(HEIGHTS_VAR).append(" = {};\n");
			String bodyPartWebId = null;
			for (String key : parts.keySet())
			{
				FormPartAnchorInfo part = parts.get(key);
				part.generateAnchoringCode(sb);
				if (Part.getDisplayName(Part.BODY).equals(part.partName)) bodyPartWebId = part.webID;
			}
			sb.append("\t").append(FORM_VAR).append(".bodyPartId = '").append(bodyPartWebId).append("';\n");
			sb.append("\t").append(FORM_VAR).append(".navigatorId = ");
			if (navigatorWebId == null) sb.append("undefined");
			else sb.append("'").append(navigatorWebId).append("'");
			sb.append(";\n");
			sb.append("\tdesigninfo['").append(formName).append("']=").append(FORM_VAR).append(";\n");
		}
	}

	public static String generateAnchoringFunctions(SortedSet<FormAnchorInfo> formAnchorInfos, String orientation)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("getOrientation = function()\n");
		sb.append("{\n");
		sb.append("\treturn '").append(orientation.toLowerCase()).append("';\n");
		sb.append("}\n");
		sb.append("\n");
		sb.append("getAnchoredFormsInfo = function()\n");
		sb.append("{\n");
		sb.append("\tvar ").append(ANCHOR_INFO_VAR).append(";\n");
		sb.append("\tvar ").append(FORM_VAR).append(";\n");
		sb.append("\tvar designinfo = {};\n");
		Iterator<FormAnchorInfo> fit = formAnchorInfos.iterator();
		while (fit.hasNext())
		{
			FormAnchorInfo fai = fit.next();
			fai.generateAnchoringCode(sb);
		}
		sb.append("\n\treturn designinfo;\n");
		sb.append("}\n");
		return sb.toString();
	}

	public static String generateAnchoringParams(SortedSet<FormAnchorInfo> formAnchorInfos, Component component)
	{
		Iterator<FormAnchorInfo> it = formAnchorInfos.iterator();
		FormAnchorInfo fai;
		String webId = component.getMarkupId();
		while (it.hasNext())
		{
			fai = it.next();
			if (fai.parts != null)
			{
				for (FormPartAnchorInfo p : fai.parts.values())
				{
					if (p.elementAnchorInfo != null && p.elementAnchorInfo.containsKey(webId))
					{
						// found it
						StringBuffer sb = new StringBuffer();
						p.appendElementAnchoringCode(webId, p.elementAnchorInfo.get(webId), sb);
						return sb.toString();
					}
				}
			}
		}
		return null;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj instanceof FormAnchorInfo)
		{
			FormAnchorInfo fai = (FormAnchorInfo)obj;
			return Utils.equalObjects(fai.formID, formID) && Utils.equalObjects(fai.formName, formName) && Utils.equalObjects(fai.formSize, formSize) &&
				Utils.equalObjects(fai.bodyContainerId, bodyContainerId) && Utils.equalObjects(fai.navigatorWebId, navigatorWebId) &&
				Utils.equalObjects(fai.parts, parts) && fai.isTableView == isTableView && fai.isTopLevelForm == isTopLevelForm &&
				fai.isTopLevelNavigator == isTopLevelNavigator;
		}
		return false;
	}

	public void clear()
	{
		if (parts != null) parts.clear();
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("\n[FormAnchorInfo:\n");
		sb.append("\tform name=").append(formName).append("\n");
		sb.append("\tform size=").append(formSize).append("\n");
		sb.append("\tform id=").append(formID).append("\n");
		if (parts == null) sb.append("\tnull anchor info\n");
		else
		{
			sb.append("\tparts count=").append(parts.size()).append("\n");
			for (String key : parts.keySet())
			{
				FormPartAnchorInfo val = parts.get(key);
				sb.append("\t\t").append(key).append(": ").append(val.toString()).append("\n");
			}
		}
		sb.append("]\n");
		return sb.toString();
	}


	public int compareTo(FormAnchorInfo o)
	{
		int nameCompare = this.formName.compareTo(o.formName);
		if (nameCompare != 0) return nameCompare;

		return this.formID.equals(o.formID) ? nameCompare : this.formID.compareTo(o.formID);
	}

	/**
	 * Holds anchoring info for one element.
	 */
	public final class ElementAnchorInfo
	{
		private final String webID;
		private final int anchors;
		private final Rectangle bounds;
		private final int hAlign;
		private final int vAlign;
		private final String hint;

		public ElementAnchorInfo(String webID, int anchors, Rectangle bounds, int hAlign, int vAlign, String hint)
		{
			this.webID = webID;
			this.anchors = anchors;
			this.bounds = bounds;
			this.hAlign = hAlign;
			this.vAlign = vAlign;
			this.hint = hint;
		}

		public String getWebID()
		{
			return webID;
		}

		public int getAnchors()
		{
			return anchors;
		}

		public Rectangle getBounds()
		{
			return bounds;
		}

		public int getHorizontalAlign()
		{
			return hAlign;
		}

		public int getVerticalAlign()
		{
			return vAlign;
		}

		public String getHint()
		{
			return hint;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof ElementAnchorInfo)
			{
				ElementAnchorInfo eai = (ElementAnchorInfo)other;
				return Utils.equalObjects(this.bounds, eai.bounds) && Utils.equalObjects(this.hint, eai.hint) && Utils.equalObjects(this.webID, eai.webID) &&
					(this.anchors == eai.anchors) && (this.hAlign == eai.hAlign) && (this.vAlign == eai.vAlign);
			}
			return false;
		}
	}

	/**
	 * Holds anchoring info for all elements inside a form part (header/body/footer/etc.).
	 */
	public final class FormPartAnchorInfo
	{
		private final String partName;
		private final String webID;
		private final int height;

		public FormPartAnchorInfo(String partName, String webID, int height)
		{
			this.partName = partName;
			this.webID = webID;
			this.height = height;
		}

		public Map<String, ElementAnchorInfo> elementAnchorInfo;

		public void addAnchoredElement(String webID, int anchors, Rectangle rectangle, int hAlign, int vAlign, Class hintClass)
		{
			if (elementAnchorInfo == null) elementAnchorInfo = new HashMap<String, ElementAnchorInfo>();

			String hint = null;
			if (WebTabPanel.class.equals(hintClass))
			{
				hint = "TabPanel"; //$NON-NLS-1$
			}
			else if (WebDataLabel.class.equals(hintClass) || WebScriptLabel.class.equals(hintClass) || WebDataSubmitLink.class.equals(hintClass) ||
				WebScriptSubmitLink.class.equals(hintClass))
			{
				hint = "Label"; //$NON-NLS-1$
			}
			else if (WebDataButton.class.equals(hintClass) || WebScriptButton.class.equals(hintClass))
			{
				hint = "Button"; //$NON-NLS-1$
			}
			else if (ImageDisplay.class.equals(hintClass))
			{
				hint = "ImgField"; //$NON-NLS-1$
			}
			else if (WebDataHtmlArea.class.equals(hintClass))
			{
				hint = "HTMLArea"; //$NON-NLS-1$
			}

			ElementAnchorInfo elementInfo = new ElementAnchorInfo(webID, anchors, rectangle, hAlign, vAlign, hint);
			elementAnchorInfo.put(webID, elementInfo);
		}

		public void generateAnchoringCode(StringBuffer sb)
		{
			if (elementAnchorInfo != null)
			{
				sb.append("\t").append(ANCHOR_INFO_VAR).append(" = new Array();\n");

				Iterator<Map.Entry<String, ElementAnchorInfo>> eit = elementAnchorInfo.entrySet().iterator();
				int anchorInfoIdx = 0;
				while (eit.hasNext())
				{
					Map.Entry<String, ElementAnchorInfo> entry = eit.next();
					sb.append("\t").append(ANCHOR_INFO_VAR).append(".push(");
					appendElementAnchoringCode(entry.getKey(), entry.getValue(), sb);
					sb.append(");");
					sb.append("\n");
				}

				sb.append("\t").append(FORM_VAR).append(".");
				if (!Part.getDisplayName(Part.BODY).equals(partName))
				{
					sb.append(NON_BODY_PARTS_VAR).append("['").append(webID).append("']");
				}
				else
				{
					sb.append(BODY_PART_VAR);
				}
				sb.append(" = ").append(ANCHOR_INFO_VAR).append(";\n");
				sb.append("\t").append(FORM_VAR).append(".").append(HEIGHTS_VAR).append("['").append(webID).append("'] = ").append(height).append(";\n");
			}
			if (!Part.getDisplayName(Part.BODY).equals(partName))
			{
				sb.append("\t").append(FORM_VAR).append(".");
				sb.append(NON_BODY_PARTS_IDS_VAR).append("['").append(webID).append("']");
				sb.append(" = true;\n");
			}
		}

		private void appendElementAnchoringCode(String webId, ElementAnchorInfo ei, StringBuffer sb)
		{
			sb.append("new Array('").append(webId).append("', ");
			sb.append(ei.getAnchors());
			sb.append(",");
			sb.append(PersistHelper.createRectangleString(ei.getBounds()));
			sb.append(",");
			sb.append("'").append(ei.getHint()).append("'");
			sb.append(",");
			sb.append(ei.getHorizontalAlign());
			sb.append(",");
			sb.append(ei.getVerticalAlign());
			sb.append(")");
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (obj == this) return true;
			if (obj instanceof FormPartAnchorInfo)
			{
				FormPartAnchorInfo fai = (FormPartAnchorInfo)obj;
				return Utils.equalObjects(fai.partName, partName) && Utils.equalObjects(fai.webID, webID) && fai.height == height &&
					Utils.equalObjects(fai.elementAnchorInfo, elementAnchorInfo);
			}
			return false;
		}

		public void clear()
		{
			if (elementAnchorInfo != null) elementAnchorInfo.clear();
		}

		@Override
		public String toString()
		{
			StringBuffer sb = new StringBuffer("\n\t\t\t[FormPartAnchorInfo:\n");
			sb.append("\t\t\t\tpart name=").append(partName).append("\n");
			sb.append("\t\t\t\tweb ID=").append(webID).append("\n");
			if (elementAnchorInfo == null) sb.append("\t\t\t\tnull anchor info\n");
			else
			{
				sb.append("\t\t\t\tanchor info count=").append(elementAnchorInfo.size()).append("\n");
				for (String key : elementAnchorInfo.keySet())
				{
					ElementAnchorInfo val = elementAnchorInfo.get(key);
					sb.append("\t\t\t\t\t").append(key).append(": <").append(val.getAnchors()).append(",").append(val.getBounds()).append(">\n");
				}
			}
			sb.append("\t\t\t]\n");
			return sb.toString();
		}
	}

}
