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
package com.servoy.j2db.util.gui;


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.List;

/**
 * Wanted extend Cardlayout, but to much is protected...
 * 
 * @author jblok
 */
public class FixedCardLayout implements LayoutManager2
{
	/*
	 * This creates an ArrayList to store associated pairs of components and their names.
	 */
	List<Card> componentList = new ArrayList<Card>();

	/*
	 * A pair of Component and String that represents its name.
	 */
	class Card
	{
		public String name;
		public Component comp;

		public Card(String cardName, Component cardComponent)
		{
			name = cardName;
			comp = cardComponent;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/*
	 * Index of Component currently displayed by CardLayout.
	 */
	int currentCard = 0;

	/*
	 * A cards horizontal Layout gap (inset). It specifies the space between the left and right edges of a container and the current component. This should be a
	 * non negative Integer.
	 * 
	 * @see getHgap()
	 * 
	 * @see setHgap()
	 */
	int hgap;

	/*
	 * A cards vertical Layout gap (inset). It specifies the space between the top and bottom edges of a container and the current component. This should be a
	 * non negative Integer.
	 * 
	 * @see getVgap()
	 * 
	 * @see setVgap()
	 */
	int vgap;

	/**
	 * Creates a new card layout with gaps of size zero.
	 */
	public FixedCardLayout()
	{
		this(0, 0);
	}

	/**
	 * Creates a new card layout with the specified horizontal and vertical gaps. The horizontal gaps are placed at the left and right edges. The vertical gaps
	 * are placed at the top and bottom edges.
	 * 
	 * @param hgap the horizontal gap.
	 * @param vgap the vertical gap.
	 */
	public FixedCardLayout(int hgap, int vgap)
	{
		this.hgap = hgap;
		this.vgap = vgap;
	}

	/**
	 * Gets the horizontal gap between components.
	 * 
	 * @return the horizontal gap between components.
	 * @see java.awt.CardLayout#setHgap(int)
	 * @see java.awt.CardLayout#getVgap()
	 * @since JDK1.1
	 */
	public int getHgap()
	{
		return hgap;
	}

	/**
	 * Sets the horizontal gap between components.
	 * 
	 * @param hgap the horizontal gap between components.
	 * @see java.awt.CardLayout#getHgap()
	 * @see java.awt.CardLayout#setVgap(int)
	 * @since JDK1.1
	 */
	public void setHgap(int hgap)
	{
		this.hgap = hgap;
	}

	/**
	 * Gets the vertical gap between components.
	 * 
	 * @return the vertical gap between components.
	 * @see java.awt.CardLayout#setVgap(int)
	 * @see java.awt.CardLayout#getHgap()
	 */
	public int getVgap()
	{
		return vgap;
	}

	/**
	 * Sets the vertical gap between components.
	 * 
	 * @param vgap the vertical gap between components.
	 * @see java.awt.CardLayout#getVgap()
	 * @see java.awt.CardLayout#setHgap(int)
	 * @since JDK1.1
	 */
	public void setVgap(int vgap)
	{
		this.vgap = vgap;
	}

	/**
	 * Adds the specified component to this card layout's internal table of names. The object specified by <code>constraints</code> must be a string. The card
	 * layout stores this string as a key-value pair that can be used for random access to a particular card. By calling the <code>show</code> method, an
	 * application can display the component with the specified name.
	 * 
	 * @param comp the component to be added.
	 * @param constraints a tag that identifies a particular card in the layout.
	 * @see java.awt.CardLayout#show(java.awt.Container, java.lang.String)
	 * @exception IllegalArgumentException if the constraint is not a string.
	 */
	public void addLayoutComponent(Component comp, Object constraints)
	{
		synchronized (comp.getTreeLock())
		{
			if (constraints instanceof String)
			{
				addLayoutComponent((String)constraints, comp);
			}
			else
			{
				throw new IllegalArgumentException("cannot add to layout: constraint must be a string"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @deprecated replaced by <code>addLayoutComponent(Component, Object)</code>.
	 */
	@Deprecated
	public void addLayoutComponent(String name, Component comp)
	{
		synchronized (comp.getTreeLock())
		{
			if (!componentList.isEmpty())
			{
				comp.setVisible(false);
			}
			for (int i = 0; i < componentList.size(); i++)
			{
				if (componentList.get(i).name.equals(name))
				{
					componentList.remove(i);
					break;
				}
			}
			componentList.add(new Card(name, comp));
		}
	}

	/**
	 * Removes the specified component from the layout.
	 * 
	 * @param comp the component to be removed.
	 * @see java.awt.Container#remove(java.awt.Component)
	 * @see java.awt.Container#removeAll()
	 */
//################################################################################ this method is fixed...! (could not override method, so copy class change this)
	public void removeLayoutComponent(Component comp)
	{
		synchronized (comp.getTreeLock())
		{
			int i = 0;
			for (; i < componentList.size(); i++)
			{
				if (componentList.get(i).comp == comp)
				{
					componentList.remove(i);
					//## second fix,place panels in same condition as added
					//addendum: alsways as invisible...becouse they can still have a parent (setting back visible is expensive and generates bugs on mac).
					comp.setVisible(false);
					break;
				}
			}

			if (componentList.isEmpty())
			{
				currentCard = 0;
			}
			else
			{
				if (currentCard > 0 && currentCard >= i)
				{
					currentCard--;//%= vector.size(); //## first fix,keep index correct
					layoutContainer(componentList.get(currentCard).comp.getParent());
				}
			}
		}
	}

	/**
	 * Determines the preferred size of the container argument using this card layout.
	 * 
	 * @param parent the name of the parent container.
	 * @return the preferred dimensions to lay out the subcomponents of the specified container.
	 * @see java.awt.Container#getPreferredSize
	 * @see java.awt.CardLayout#minimumLayoutSize
	 */
	public Dimension preferredLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			Insets insets = parent.getInsets();
			int ncomponents = componentList.size();
			int w = 0;
			int h = 0;

			for (int i = 0; i < ncomponents; i++)
			{
				Component comp = componentList.get(i).comp;
				if (comp.isVisible())
				{
					Dimension d = comp.getPreferredSize();
					if (d.width > w)
					{
						w = d.width;
					}
					if (d.height > h)
					{
						h = d.height;
					}
				}
			}
			return new Dimension(insets.left + insets.right + w + hgap * 2, insets.top + insets.bottom + h + vgap * 2);
		}
	}

	/**
	 * Calculates the minimum size for the specified panel.
	 * 
	 * @param parent the name of the parent container in which to do the layout.
	 * @return the minimum dimensions required to lay out the subcomponents of the specified container.
	 * @see java.awt.Container#doLayout
	 * @see java.awt.CardLayout#preferredLayoutSize
	 */
	public Dimension minimumLayoutSize(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			Insets insets = parent.getInsets();
			int ncomponents = componentList.size();
			int w = 0;
			int h = 0;

			for (int i = 0; i < ncomponents; i++)
			{
				Component comp = componentList.get(i).comp;
				Dimension d = comp.getMinimumSize();
				if (d.width > w)
				{
					w = d.width;
				}
				if (d.height > h)
				{
					h = d.height;
				}
			}
			return new Dimension(insets.left + insets.right + w + hgap * 2, insets.top + insets.bottom + h + vgap * 2);
		}
	}

	/**
	 * Returns the maximum dimensions for this layout given the components in the specified target container.
	 * 
	 * @param target the component which needs to be laid out
	 * @see Container
	 * @see #minimumLayoutSize
	 * @see #preferredLayoutSize
	 */
	public Dimension maximumLayoutSize(Container target)
	{
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Returns the alignment along the x axis. This specifies how the component would like to be aligned relative to other components. The value should be a
	 * number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned the furthest away from the origin, 0.5 is centered, etc.
	 */
	public float getLayoutAlignmentX(Container parent)
	{
		return 0.5f;
	}

	/**
	 * Returns the alignment along the y axis. This specifies how the component would like to be aligned relative to other components. The value should be a
	 * number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned the furthest away from the origin, 0.5 is centered, etc.
	 */
	public float getLayoutAlignmentY(Container parent)
	{
		return 0.5f;
	}

	/**
	 * Invalidates the layout, indicating that if the layout manager has cached information it should be discarded.
	 */
	public void invalidateLayout(Container target)
	{
	}

	/**
	 * Lays out the specified container using this card layout.
	 * <p>
	 * Each component in the <code>parent</code> container is reshaped to be the size of the container, minus space for surrounding insets, horizontal gaps,
	 * and vertical gaps.
	 * 
	 * @param parent the name of the parent container in which to do the layout.
	 * @see java.awt.Container#doLayout
	 */
	public void layoutContainer(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			Insets insets = parent.getInsets();

			if (!componentList.isEmpty())
			{
				final Component comp = componentList.get(currentCard).comp;
				comp.setBounds(hgap + insets.left, vgap + insets.top, parent.getWidth() - (hgap * 2 + insets.left + insets.right), parent.getHeight() -
					(vgap * 2 + insets.top + insets.bottom));
				if (!comp.isVisible())
				{
					comp.setVisible(true);
				}
			}
		}
	}

	/**
	 * Make sure that the Container really has a CardLayout installed. Otherwise havoc can ensue!
	 */
	void checkLayout(Container parent)
	{
		if (parent.getLayout() != this)
		{
			throw new IllegalArgumentException("wrong parent for CardLayout"); //$NON-NLS-1$
		}
	}

	/**
	 * Flips to the first card of the container.
	 * 
	 * @param parent the name of the parent container in which to do the layout.
	 * @see java.awt.CardLayout#last
	 */
	public void first(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			checkLayout(parent);
			show(parent, 0, true);
		}
	}

	/**
	 * Flips to the next card of the specified container. If the currently visible card is the last one, this method flips to the first card in the layout.
	 * 
	 * @param parent the name of the parent container in which to do the layout.
	 * @see java.awt.CardLayout#previous
	 */
	public void next(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			checkLayout(parent);
			show(parent, (currentCard + 1) % componentList.size(), true);
		}
	}

	/**
	 * Flips to the previous card of the specified container. If the currently visible card is the first one, this method flips to the last card in the layout.
	 * 
	 * @param parent the name of the parent container in which to do the layout.
	 * @see java.awt.CardLayout#next
	 */
	public void previous(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			checkLayout(parent);
			int newIndex = (currentCard + componentList.size() - 1) % componentList.size();
			show(parent, newIndex, true);
		}
	}

	/**
	 * Flips to the last card of the container.
	 * 
	 * @param parent the name of the parent container in which to do the layout.
	 * @see java.awt.CardLayout#first
	 */
	public void last(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			checkLayout(parent);
			show(parent, componentList.size() - 1, true);
		}
	}

	/**
	 * Flips to the component that was added to this layout with the specified <code>name</code>, using <code>addLayoutComponent</code>. If no such
	 * component exists, then nothing happens.
	 * 
	 * @param parent the name of the parent container in which to do the layout.
	 * @param name the component name.
	 * @see java.awt.CardLayout#addLayoutComponent(java.awt.Component, java.lang.Object)
	 */
	public void show(Container parent, String name)
	{
		synchronized (parent.getTreeLock())
		{
			checkLayout(parent);

			if (componentList.get(currentCard).name.equals(name)) return;

			int ncomponents = componentList.size();
			for (int i = 0; i < ncomponents; i++)
			{
				Card card = componentList.get(i);
				if (card.name.equals(name))
				{
					show(parent, i, true);
					break;
				}
			}
		}
	}

	public void show(Container parent, int newIndex, boolean checkIndex)
	{
		if (!componentList.isEmpty() && (!checkIndex || (currentCard != newIndex)))
		{
			componentList.get(currentCard).comp.setVisible(false);
			currentCard = newIndex;
			parent.validate();
		}
	}

	/**
	 * Returns a string representation of the state of this card layout.
	 * 
	 * @return a string representation of this card layout.
	 */
	@Override
	public String toString()
	{
		return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
