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
package com.servoy.j2db.ui;

import java.util.Iterator;
import java.util.Map;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;

/**
 * @author jblok
 */
public class PropertyCopy
{
	public static void copyExistingPrintableProperties(IApplication application, FormController fc, Map copy_part_panels)
	{
//		FormController fc = ((FormManager)application.getFormManager()).getCachedFormController(formBeingPrint);
		if (fc != null)
		{
			IDataRenderer[] org_drs = fc.getDataRenderers();
			for (IDataRenderer org : org_drs)
			{
				if (org != null)
				{
					IDataRenderer copy = null;
					String org_part_id = org.getId();
					Iterator it = copy_part_panels.values().iterator();
					while (it.hasNext())
					{
						IDataRenderer panel = (IDataRenderer)it.next();
						if (org_part_id != null && org_part_id.equals(panel.getId()))
						{
							copy = panel;
							break;
						}
					}

					if (copy != null)
					{
						Iterator org_comps = org.getComponentIterator();
						while (org_comps != null && org_comps.hasNext())
						{
							Object org_comp = org_comps.next();
							if (org_comp instanceof IComponent)
							{
								String org_id = ((IComponent)org_comp).getId();
								if (org_id != null)
								{
									Iterator copy_comps = copy.getComponentIterator();
									while (copy_comps != null && copy_comps.hasNext())
									{
										Object copy_comp = copy_comps.next();
										if (copy_comp instanceof IComponent)
										{
											String copy_id = ((IComponent)copy_comp).getId();
											if (org_id.equals(copy_id))
											{
												if (org_comp instanceof IComponent && copy_comp instanceof IComponent)
												{
													copyElementProps((IComponent)org_comp, (IComponent)copy_comp);
												}
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public static void copyElementProps(IComponent org, IComponent copy)
	{
		if (org == null || copy == null) return;
//		if (org instanceof IDelegate && copy instanceof IDelegate)
//		{
//			org = (JComponent)((IDelegate)org).getDelegate();
//			copy = (JComponent) ((IDelegate)copy).getDelegate();
//		}
		copy.setLocation(org.getLocation());
		copy.setSize(org.getSize());
		copy.setBackground(org.getBackground());
		copy.setForeground(org.getForeground());
		copy.setComponentVisible(org.isVisible());
		copy.setComponentEnabled(org.isEnabled());
		copy.setOpaque(org.isOpaque());
		copy.setFont(org.getFont());

		if (org instanceof IScriptableProvider && copy instanceof IScriptableProvider)
		{
			IScriptable source = ((IScriptableProvider)org).getScriptObject();
			IScriptable destination = ((IScriptableProvider)copy).getScriptObject();
			//should we use another interface here for readonly set/get?
			if (source instanceof IScriptReadOnlyMethods && destination instanceof IScriptReadOnlyMethods)
			{
				((IScriptReadOnlyMethods)destination).js_setReadOnly(((IScriptReadOnlyMethods)source).js_isReadOnly());
			}

			if (source instanceof IScriptBaseMethods && destination instanceof IScriptBaseMethods)
			{
				((IScriptBaseMethods)destination).js_setVisible(((IScriptBaseMethods)source).js_isVisible());
			}

			if (source instanceof IScriptLabelMethods && destination instanceof IScriptLabelMethods)
			{
				String imageURL = ((IScriptLabelMethods)source).getImageURL();
				if (imageURL != null)
				{
					//only copy if explicitly set with a url
					((IScriptLabelMethods)destination).js_setImageURL(imageURL);
				}
				String rolloverImageURL = ((IScriptLabelMethods)source).getRolloverImageURL();
				if (rolloverImageURL != null)
				{
					//only copy if explicitly set with a url
					((IScriptLabelMethods)destination).js_setRolloverImageURL(rolloverImageURL);
				}
			}

			if (source instanceof IScriptTabPanelMethods && destination instanceof IScriptTabPanelMethods)
			{
				// keep active tab when printing
				((IScriptTabPanelMethods)destination).js_setTabIndex(((IScriptTabPanelMethods)source).js_getTabIndex());
			}
		}


		if (org instanceof IFieldComponent && copy instanceof IFieldComponent)
		{
			((IFieldComponent)copy).setFormat((((IFieldComponent)org).getDataType()), (((IFieldComponent)org).getFormat()));
		}
		if (org instanceof ILabel && copy instanceof ILabel)
		{
			((ILabel)copy).setMediaIcon(((ILabel)org).getMediaIcon());
			((ILabel)copy).setText(((ILabel)org).getText());
		}

		if (org instanceof IProviderStylePropertyChanges && copy instanceof IProviderStylePropertyChanges)
		{
			((IProviderStylePropertyChanges)copy).getStylePropertyChanges().setChanges(
				((IProviderStylePropertyChanges)org).getStylePropertyChanges().getChanges());
		}

	}
}
