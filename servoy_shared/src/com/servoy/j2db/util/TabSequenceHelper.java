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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.component.IDataRendererYPositionComparator;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.TabSeqComparator;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;

/**
 * Helper class for management of tab sequence. Used both for smart and web clients.
 * 
 * @author gerzse
 */
public class TabSequenceHelper<T>
{

	/**
	 * For each data renderer, we hold pairs of (IPersist, IComponent). They are gathered while the data renderers are created, in IDataRendererFactory.
	 */
	private final SortedMap<IDataRenderer, SortedMap<ISupportTabSeq, T>> abstractTabSequence;

	/**
	 * Here we store an ordered list of pairs (String, IComponent). These are the components that are part of the tab sequence. They are not yet unrolled, that
	 * is, the ISupplyFocusChilderen are stored here, and not their children. This map is filled either directly by the user, calling setTabSequence(), either
	 * from the "abstractTabSequence" above. This map is used for retrieving the names in the tab sequence when getTabSequence() is called.
	 */
	private final LinkedHashMap<T, String> componentsToNames;

	/**
	 * This is an equivalent of "componentsToNames", but we have the names as keys. This will make it quicker to search up a component based on its name. Also
	 * this means that some components will not make it into this map, if they don't have a name defined.
	 */
	private final LinkedHashMap<String, T> namesToComponents;

	/**
	 * This is an ordered list of controls that build up the final, runtime tab sequence. ISupplyFocusChilderen are unrolled, that is, here we store their
	 * children. This list is filled from the "namedTabSequence" above.
	 */
	private final List<T> runtimeTabSequence;

	// The runtime container (SwingForm/WebForm) and the data renderer factory. Used for putting the tab sequence into effect.
	private final IFormUIInternal<T> runtimeContainer;
	private final IDataRendererFactory<T> dataRendererFactory;


	public TabSequenceHelper(IFormUIInternal<T> runtimeContainer, IDataRendererFactory<T> dataRendererFactory)
	{
		abstractTabSequence = new TreeMap<IDataRenderer, SortedMap<ISupportTabSeq, T>>(IDataRendererYPositionComparator.INSTANCE);
		runtimeTabSequence = new ArrayList<T>();
		componentsToNames = new LinkedHashMap<T, String>();
		namesToComponents = new LinkedHashMap<String, T>();
		this.runtimeContainer = runtimeContainer;
		this.dataRendererFactory = dataRendererFactory;
	}

	public void add(IDataRenderer panel, ISupportTabSeq persist, T component)
	{
		SortedMap<ISupportTabSeq, T> sequenceForPanel = abstractTabSequence.get(panel);
		if (sequenceForPanel == null)
		{
			sequenceForPanel = new TreeMap<ISupportTabSeq, T>(TabSeqComparator.INSTANCE);
			abstractTabSequence.put(panel, sequenceForPanel);
		}
		sequenceForPanel.put(persist, component);
	}

	public List<T> getRuntimeTabSequence()
	{
		return runtimeTabSequence;
	}

	public String[] getNamesInTabSequence()
	{
		List<String> namesList = new LinkedList<String>();
		for (String name : componentsToNames.values())
			if (name != null) namesList.add(name);
		return namesList.toArray(new String[namesList.size()]);
	}

	public void setRuntimeTabSequence(Object[] arrayOfElements)
	{
		if (arrayOfElements == null)
		{
			return;
		}

		Object[] elements = arrayOfElements;
		if (elements.length == 1)
		{
			if (elements[0] instanceof Object[])
			{
				elements = (Object[])elements[0];
			}
			else if (elements[0] == null)
			{
				elements = null;
			}
		}

		componentsToNames.clear();
		if (elements != null)
		{
			for (Object element : elements)
			{
				String name = null;
				if (element instanceof IComponent) name = ((IComponent)element).getName();
				if (element instanceof ISupplyFocusChildren)
				{
					Object[] children = ((ISupplyFocusChildren)element).getFocusChildren();
					if (children != null && children.length != 0)
					{
						for (Object child : children)
						{
							if (child instanceof IComponent) name = ((IComponent)child).getName();
							else name = null;
							componentsToNames.put((T)child, name);
						}
						continue;
					}
				}
				componentsToNames.put((T)element, name);
			}
		}

		revertComponentsToNames();
		fromNamedToRuntime();
	}

	public void fromAbstractToNamed()
	{
		T tableViewToInsert = null;
		int largestIndexBeforeBody = -1;
		T lastComponentBeforeBody = null;

		LinkedHashMap<T, String> componentGroupsByTabIndex = new LinkedHashMap<T, String>();
		FormController fc = runtimeContainer.getController();
		Form f = fc.getForm();
		Iterator<Part> parts = f.getParts();
		while (parts.hasNext())
		{
			Part p = parts.next();

			IDataRenderer dataRenderer = fc.getDataRenderers()[p.getPartType()];
			if (dataRenderer != null)
			{
				// If we are in table mode and we found the body part, remember it.
				// Later we will insert it in the tab sequence.
				if (((f.getView() == FormController.TABLE_VIEW) || (f.getView() == FormController.LOCKED_TABLE_VIEW)) && (p.getPartType() == Part.BODY))
				{
					tableViewToInsert = (T)dataRenderer;
				}

				else
				{
					SortedMap<ISupportTabSeq, T> dataRendererComponents = abstractTabSequence.get(dataRenderer);
					if (dataRendererComponents != null)
					{
						for (ISupportTabSeq supportTabSeq : dataRendererComponents.keySet())
						{
							if (supportTabSeq.getTabSeq() >= 0)
							{
								T next = dataRendererComponents.get(supportTabSeq);
								String name = null;
								if (supportTabSeq instanceof ISupportName) name = ((ISupportName)supportTabSeq).getName();
								componentGroupsByTabIndex.put(next, name);
								if ((p.getPartType() == Part.HEADER) || (p.getPartType() == Part.TITLE_HEADER) ||
									(p.getPartType() == Part.LEADING_GRAND_SUMMARY))
								{
									if (supportTabSeq.getTabSeq() >= largestIndexBeforeBody)
									{
										lastComponentBeforeBody = next;
										largestIndexBeforeBody = supportTabSeq.getTabSeq();
									}
								}
							}
						}
					}
				}
			}
		}

		componentsToNames.clear();
		for (T o : componentGroupsByTabIndex.keySet())
		{
			componentsToNames.put(o, componentGroupsByTabIndex.get(o));
			if ((tableViewToInsert != null) && (lastComponentBeforeBody != null) && (o.equals(lastComponentBeforeBody))) componentsToNames.put(
				tableViewToInsert, null);
		}
		if ((lastComponentBeforeBody == null) && (tableViewToInsert != null)) componentsToNames.put(tableViewToInsert, null);

		revertComponentsToNames();
		fromNamedToRuntime();
	}

	public T getComponentByName(String name)
	{
		T fce = namesToComponents.get(name);
		if (fce != null) return fce;
		else return null;
	}

	public T getComponentForFocus(String name, boolean skipReadonly)
	{
		T component = null;
		Iterator<T> iter = componentsToNames.keySet().iterator();
		if (name != null)
		{
			boolean found = false;
			while (iter.hasNext())
			{
				component = iter.next();
				String thisName = componentsToNames.get(component);
				if ((thisName != null) && thisName.equals(name))
				{
					found = true;
					break;
				}
			}
			if (found)
			{
				if (skipReadonly && isComponentReadonly(component))
				{
					boolean gotAGoodOne = false;
					while (iter.hasNext())
					{
						component = iter.next();
						if (!isComponentReadonly(component))
						{
							gotAGoodOne = true;
							break;
						}
					}
					if (!gotAGoodOne)
					{
						iter = componentsToNames.keySet().iterator();
						while (iter.hasNext())
						{
							component = iter.next();
							String thisName = componentsToNames.get(component);
							if ((thisName == null) || !thisName.equals(name))
							{
								if (!isComponentReadonly(component))
								{
									gotAGoodOne = true;
									break;
								}
							}
						}
					}
					if (!gotAGoodOne) component = null;
				}
			}
			else
			{
				component = null;
			}
		}
		/* If no name provided, just pick the first component (the first non-readonly, if so requested). */
		else
		{
			while (iter.hasNext())
			{
				component = iter.next();
				if (!skipReadonly || !isComponentReadonly(component)) break;
			}
			if (skipReadonly && isComponentReadonly(component)) component = null;
		}
		/* If has focus children, extract them. */
		if ((component != null) && (component instanceof ISupplyFocusChildren))
		{
			Object[] children = ((ISupplyFocusChildren)component).getFocusChildren();
			if ((children != null) && (children.length > 0)) component = (T)children[0];
		}
		return component;
	}

	private boolean isComponentReadonly(T component)
	{
		boolean isReadonly = false;
		if (component instanceof IDisplay) isReadonly = ((IDisplay)component).isReadOnly();
		return isReadonly;
	}

	private void revertComponentsToNames()
	{
		/* Fill in the "namesToComponents" map. */
		namesToComponents.clear();
		for (T comp : componentsToNames.keySet())
		{
			String name = componentsToNames.get(comp);
			if (name != null) namesToComponents.put(name, comp);
		}
	}

	private void fromNamedToRuntime()
	{
		runtimeTabSequence.clear();
		for (T component : componentsToNames.keySet())
		{
			boolean processed = false;
			if (component instanceof ISupplyFocusChildren)
			{
				T[] children = ((ISupplyFocusChildren<T>)component).getFocusChildren();
				if (children != null && children.length != 0)
				{
					for (T element : children)
						runtimeTabSequence.add(element);
					processed = true;
				}
			}
			if (!processed)
			{
				runtimeTabSequence.add(component);
			}
		}

		dataRendererFactory.extendTabSequence(runtimeTabSequence, runtimeContainer);
		dataRendererFactory.applyTabSequence(runtimeTabSequence, runtimeContainer);
	}

	public void clear()
	{
		abstractTabSequence.clear();
		runtimeTabSequence.clear();
		componentsToNames.clear();
		namesToComponents.clear();
	}
}
