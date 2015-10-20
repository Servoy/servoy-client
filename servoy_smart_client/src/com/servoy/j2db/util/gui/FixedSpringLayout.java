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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SpringLayout;

/**
 * Class to fix the wrong optimise in jdk1.6 SpringLayout (Spring) 
        public final void setValue(int size) {
            if (this.size == size)// <============ if called with UNSET clear() is never reached if size is unused in subclasses and still hold UNSET value  
            {
                return; 
            }
            if (size == UNSET) {
                clear();
            } else {
                setNonClearValue(size);
            }
        }
 */
public class FixedSpringLayout implements LayoutManager2 
{
    private Map componentConstraints = new HashMap();

    private Spring cyclicReference = Spring.constant(Spring.UNSET);
    private Set cyclicSprings;
    private Set acyclicSprings;


    /**
     * Specifies the top edge of a component's bounding rectangle.
     */
    public static final String NORTH  = "North";

    /**
     * Specifies the bottom edge of a component's bounding rectangle.
     */
    public static final String SOUTH  = "South";

    /**
     * Specifies the right edge of a component's bounding rectangle.
     */
    public static final String EAST   = "East";

    /**
     * Specifies the left edge of a component's bounding rectangle.
     */
    public static final String WEST   = "West";


    /**
     * A <code>Constraints</code> object holds the
     * constraints that govern the way a component's size and position
     * change in a container controlled by a <code>SpringLayout</code>.
     * A <code>Constraints</code> object is
     * like a <code>Rectangle</code>, in that it
     * has <code>x</code>, <code>y</code>,
     * <code>width</code>, and <code>height</code> properties.
     * In the <code>Constraints</code> object, however,
     * these properties have
     * <code>Spring</code> values instead of integers.
     * In addition,
     * a <code>Constraints</code> object
     * can be manipulated as four edges
     * -- north, south, east, and west --
     * using the <code>constraint</code> property.
     * 
     * <p>
     * The following formulas are always true
     * for a <code>Constraints</code> object:
     *
     * <pre>
     *       west = x
     *      north = y
     *       east = x + width
     *      south = y + height</pre>
     *
     * <b>Note</b>: In this document,
     * operators represent methods 
     * in the <code>Spring</code> class.
     * For example, "a + b" is equal to
     * <code>Spring.sum(a, b)</code>,
     * and "a - b" is equal to
     * <code>Spring.sum(a, Spring.minus(b))</code>.
     * See the 
     * {@link Spring Spring</code> API documentation<code>}
     * for further details
     * of spring arithmetic.
     *
     * <p>
     * 
     * Because a <code>Constraints</code> object's properties --
     * representing its edges, size, and location -- can all be set
     * independently and yet are interrelated,
     * the object can become <em>over-constrained</em>.
     * For example,
     * if both the <code>x</code> and <code>width</code>
     * properties are set
     * and then the east edge is set,
     * the object is over-constrained horizontally.
     * When this happens, one of the values 
     * (in this case, the <code>x</code> property)
     * automatically changes so
     * that the formulas still hold. 
     *
     * <p>
     * The following table shows which value changes
     * when a <code>Constraints</code> object
     * is over-constrained horizontally.
     *
     * <p>
     *
     * <table border=1 summary="Shows which value changes when a Constraints object is over-constrained horizontally">
     *   <tr>
     *     <th valign=top>Value Being Set<br>(method used)</th>
     *     <th valign=top>Result When Over-Constrained Horizontally<br>
     *      (<code>x</code>, <code>width</code>, and the east edge are all non-<code>null</code>)</th>
     *   </tr>
     *   <tr>
     *     <td><code>x</code> or the west edge <br>(<code>setX</code> or <code>setConstraint</code>)</td>
     *     <td><code>width</code> value is automatically set to <code>east - x</code>.</td>
     *   </tr>
     *   <tr>
     *     <td><code>width</code><br>(<code>setWidth</code>)</td>
     *     <td>east edge's value is automatically set to <code>x + width</code>.</td>
     *   </tr>
     *   <tr>
     *     <td>east edge<br>(<code>setConstraint</code>)</td>
     *     <td><code>x</code> value is automatically set to <code>east - width</code>.</td>
     *   </tr>
     *   </table>
     *
     * <p>
     * The rules for the vertical properties are similar:
     * <p>
     *
     * <table border=1 summary="Shows which value changes when a Constraints object is over-constrained vertically">
     * <tr>
     *  <th valign=top>Value Being Set<br>(method used)</th>
     *  <th valign=top>Result When Over-Constrained Vertically<br>(<code>y</code>, <code>height</code>, and the south edge are all non-<code>null</code>)</th>
     * </tr>
     * <tr>
     *   <td><code>y</code> or the north edge<br>(<code>setY</code> or <code>setConstraint</code>)</td>
     *   <td><code>height</code> value is automatically set to <code>south - y</code>.</td>
     * </tr>
     * <tr>
     *   <td><code>height</code><br>(<code>setHeight</code>)</td>
     *   <td>south edge's value is automatically set to <code>y + height</code>.</td>
     * </tr>
     * <tr>
     *   <td>south edge<br>(<code>setConstraint</code>)</td>
     *   <td><code>y</code> value is automatically set to <code>south - height</code>.</td>
     * </tr>
     * </table>
     *
     */
   public static class Constraints {
       private Spring x;
       private Spring y;
       private Spring width;
       private Spring height;
       private Spring east;
       private Spring south;

       private Spring verticalDerived = null; 
       private Spring horizontalDerived = null; 
       
       /**
        * Creates an empty <code>Constraints</code> object.
        */
       public Constraints() {
           this(null, null, null, null);
       }

       /**
        * Creates a <code>Constraints</code> object with the
	* specified values for its
        * <code>x</code> and <code>y</code> properties.
        * The <code>height</code> and <code>width</code> springs
	* have <code>null</code> values.
        *
        * @param x  the spring controlling the component's <em>x</em> value
        * @param y  the spring controlling the component's <em>y</em> value
        */
       public Constraints(Spring x, Spring y) {
           this(x, y, null, null);
       }

       /**
        * Creates a <code>Constraints</code> object with the 
	* specified values for its
        * <code>x</code>, <code>y</code>, <code>width</code>,
	* and <code>height</code> properties.
        * Note: If the <code>SpringLayout</code> class
	* encounters <code>null</code> values in the
	* <code>Constraints</code> object of a given component,
	* it replaces them with suitable defaults.
        *
        * @param x  the spring value for the <code>x</code> property
        * @param y  the spring value for the <code>y</code> property
        * @param width  the spring value for the <code>width</code> property
        * @param height  the spring value for the <code>height</code> property
        */
       public Constraints(Spring x, Spring y, Spring width, Spring height) {
           this.x = x;
           this.y = y;
           this.width = width;
           this.height = height;
       }

        /**
         * Creates a <code>Constraints</code> object with
         * suitable <code>x</code>, <code>y</code>, <code>width</code> and
         * <code>height</code> springs for component, <code>c</code>.
         * The <code>x</code> and <code>y</code> springs are constant
         * springs  initialised with the component's location at
         * the time this method is called. The <code>width</code> and
         * <code>height</code> springs are special springs, created by
         * the <code>Spring.width()</code> and <code>Spring.height()</code>
         * methods, which track the size characteristics of the component
         * when they change.
         *
         * @param c  the component whose characteristics will be reflected by this Constraints object
         * @throws NullPointerException if <code>c</code> is null.
         * @since 1.5
         */
        public Constraints(Component c) {
            this.x = Spring.constant(c.getX());
            this.y = Spring.constant(c.getY());
            this.width = Spring.width(c);
            this.height = Spring.height(c);
        }

       private boolean overConstrainedHorizontally() { 
           return (x != null) && (width != null) && (east != null); 
       }
       
       private boolean overConstrainedVertically() { 
           return (y != null) && (height != null) && (south != null); 
       }
       
       private Spring sum(Spring s1, Spring s2) { 
           return (s1 == null || s2 == null) ? null : Spring.sum(s1, s2); 
       }
        
       private Spring difference(Spring s1, Spring s2) { 
           return (s1 == null || s2 == null) ? null : Spring.difference(s1, s2); 
       }
        
       /**
        * Sets the <code>x</code> property,
	* which controls the <code>x</code> value
	* of a component's location.
        *
        * @param x the spring controlling the <code>x</code> value
	*          of a component's location
        *
        * @see #getX
        * @see SpringLayout.Constraints
        */
       public void setX(Spring x) {
           this.x = x;
           horizontalDerived = null; 
           if (overConstrainedHorizontally()) { 
               width = null; 
           }
       }

       /**
        * Returns the value of the <code>x</code> property.
        *
        * @return the spring controlling the <code>x</code> value
	*         of a component's location
        *
        * @see #setX
        * @see SpringLayout.Constraints
        */
       public Spring getX() {
           if (x != null) { 
               return x; 
           }
           if (horizontalDerived == null) { 
               horizontalDerived = difference(east, width); 
           } 
           return horizontalDerived; 
       }

       /**
        * Sets the <code>y</code> property,
	* which controls the <code>y</code> value
	* of a component's location.
        *
        * @param y the spring controlling the <code>y</code> value
	*          of a component's location
        *
        * @see #getY
        * @see SpringLayout.Constraints
        */
       public void setY(Spring y) {
           this.y = y;
           verticalDerived = null; 
           if (overConstrainedVertically()) { 
               height = null; 
           }
       }

       /**
        * Returns the value of the <code>y</code> property.
        *
        * @return the spring controlling the <code>y</code> value
	*         of a component's location
        *
        * @see #setY
        * @see SpringLayout.Constraints
        */
       public Spring getY() {
           if (y != null) { 
               return y; 
           }
           if (verticalDerived == null) { 
               verticalDerived = difference(south, height); 
           } 
           return verticalDerived; 
       }

       /**
        * Sets the <code>width</code> property,
	* which controls the width of a component.
        *
        * @param width the spring controlling the width of this
	* <code>Constraints</code> object
        *
        * @see #getWidth
        * @see SpringLayout.Constraints
        */
       public void setWidth(Spring width) {
           this.width = width;
           horizontalDerived = null; 
           if (overConstrainedHorizontally()) { 
               east = null; 
           }
       }

       /**
        * Returns the value of the <code>width</code> property.
        *
        * @return the spring controlling the width of a component
        *
        * @see #setWidth
        * @see SpringLayout.Constraints
        */
       public Spring getWidth() {
           if (width != null) { 
               return width; 
           }
           if (horizontalDerived == null) { 
               horizontalDerived = difference(east, x); 
           } 
           return horizontalDerived; 
       }

       /**
        * Sets the <code>height</code> property,
	* which controls the height of a component.
        *
        * @param height the spring controlling the height of this <code>Constraints</code>
	* object
        *
        * @see #getHeight
        * @see SpringLayout.Constraints
        */
       public void setHeight(Spring height) {
           this.height = height;
           verticalDerived = null; 
           if (overConstrainedVertically()) { 
               south = null; 
           }
       }

       /**
        * Returns the value of the <code>height</code> property.
        *
        * @return the spring controlling the height of a component
        *
        * @see #setHeight
        * @see SpringLayout.Constraints
        */
       public Spring getHeight() {
           if (height != null) { 
               return height; 
           }
           if (verticalDerived == null) { 
               verticalDerived = difference(south, y); 
           } 
           return verticalDerived; 
       }

       private void setEast(Spring east) {
           this.east = east;
           horizontalDerived = null; 
           if (overConstrainedHorizontally()) { 
               x = null; 
           }
       }

       private Spring getEast() {
           if (east != null) { 
               return east; 
           }
           if (horizontalDerived == null) { 
               horizontalDerived = sum(x, width); 
           } 
           return horizontalDerived; 
       }

       private void setSouth(Spring south) {
           this.south = south;
           verticalDerived = null; 
           if (overConstrainedVertically()) { 
               y = null; 
           }
       }

       private Spring getSouth() {
           if (south != null) { 
               return south; 
           }
           if (verticalDerived == null) { 
               verticalDerived = sum(y, height); 
           } 
           return verticalDerived; 
       }

       /**
        * Sets the spring controlling the specified edge.
        * The edge must have one of the following values:
        * <code>SpringLayout.NORTH</code>, <code>SpringLayout.SOUTH</code>,
	* <code>SpringLayout.EAST</code>, <code>SpringLayout.WEST</code>.
        *
        * @param edgeName the edge to be set
        * @param s the spring controlling the specified edge
        *
        * @see #getConstraint
	* @see #NORTH
	* @see #SOUTH
	* @see #EAST
	* @see #WEST
        * @see SpringLayout.Constraints
        */
       public void setConstraint(String edgeName, Spring s) {
           edgeName = edgeName.intern();
           if (edgeName == "West") {
               setX(s);
           }
           else if (edgeName == "North") {
               setY(s);
           }
           else if (edgeName == "East") {
               setEast(s);
           }
           else if (edgeName == "South") {
               setSouth(s);
           }
       }

       /**
        * Returns the value of the specified edge.
        * The edge must have one of the following values:
        * <code>SpringLayout.NORTH</code>, <code>SpringLayout.SOUTH</code>,
	* <code>SpringLayout.EAST</code>, <code>SpringLayout.WEST</code>.
        *
        * @param edgeName the edge whose value
	*                 is to be returned
        *
        * @return the spring controlling the specified edge
        *
        * @see #setConstraint
	* @see #NORTH
	* @see #SOUTH
	* @see #EAST
	* @see #WEST
        * @see SpringLayout.Constraints
        */
       public Spring getConstraint(String edgeName) {
           edgeName = edgeName.intern();
           return (edgeName == "West")  ? getX() :
                  (edgeName == "North") ? getY() :
                  (edgeName == "East")  ? getEast() :
                  (edgeName == "South") ? getSouth() :
                  null;
       }

       /*pp*/ void reset() {
           if (x != null) x.setValue(Spring.UNSET);
           if (y != null) y.setValue(Spring.UNSET);
           if (width != null) width.setValue(Spring.UNSET);
           if (height != null) height.setValue(Spring.UNSET);
           if (east != null) east.setValue(Spring.UNSET);
           if (south != null) south.setValue(Spring.UNSET);
           if (horizontalDerived != null) horizontalDerived.setValue(Spring.UNSET);
           if (verticalDerived != null) verticalDerived.setValue(Spring.UNSET);
       }
   }

   private static class SpringProxy extends Spring {
       private String edgeName;
       private Component c;
       private FixedSpringLayout l;

       public SpringProxy(String edgeName, Component c, FixedSpringLayout l) {
           this.edgeName = edgeName;
           this.c = c;
           this.l = l;
       }

       private Spring getConstraint() { 
           return l.getConstraints(c).getConstraint(edgeName);
       }

       public int getMinimumValue() {
           return getConstraint().getMinimumValue();
       }

       public int getPreferredValue() {
           return getConstraint().getPreferredValue();
       }

       public int getMaximumValue() {
           return getConstraint().getMaximumValue();
       }

       public int getValue() {
           return getConstraint().getValue();
       }

       public void setValue(int size) {
           getConstraint().setValue(size);
       }

       /*pp*/ boolean isCyclic(FixedSpringLayout l) {
           return l.isCyclic(getConstraint()); 
       }

       public String toString() {
           return "SpringProxy for " + edgeName + " edge of " + c.getName() + ".";
       }
    }

    /**
     * Constructs a new <code>SpringLayout</code>.
     */
    public FixedSpringLayout() {}

    private void resetCyclicStatuses() { 
        cyclicSprings = new HashSet();
        acyclicSprings = new HashSet();
    }

    private void setParent(Container p) { 
        resetCyclicStatuses(); 
        Constraints pc = getConstraints(p);
        
        pc.setX(Spring.constant(0));
        pc.setY(Spring.constant(0));
        // The applyDefaults() method automatically adds width and
        // height springs that delegate their calculations to the
        // getMinimumSize(), getPreferredSize() and getMaximumSize()
        // methods of the relevant component. In the case of the
        // parent this will cause an infinite loop since these
        // methods, in turn, delegate their calculations to the
        // layout manager. Check for this case and replace the
        // the springs that would cause this problem with a
        // constant springs that supply default values.
        Spring width = pc.getWidth();
        if (width instanceof Spring.WidthSpring && ((Spring.WidthSpring)width).c == p) {
            pc.setWidth(Spring.constant(0, 0, Integer.MAX_VALUE));
        }
        Spring height = pc.getHeight();
        if (height instanceof Spring.HeightSpring && ((Spring.HeightSpring)height).c == p) {
            pc.setHeight(Spring.constant(0, 0, Integer.MAX_VALUE));
        }
    }

    /*pp*/ boolean isCyclic(Spring s) { 
        if (s == null) {
            return false;
        }
        if (cyclicSprings.contains(s)) {
            return true;
        }
        if (acyclicSprings.contains(s)) {
            return false;
        }
        cyclicSprings.add(s);
        boolean result = s.isCyclic(this);
        if (!result) {
            acyclicSprings.add(s);
            cyclicSprings.remove(s);
        }
        else {
            System.err.println(s + " is cyclic. ");
        }
        return result;
    }

    private Spring abandonCycles(Spring s) { 
        return isCyclic(s) ? cyclicReference : s;
    }

    // LayoutManager methods.

    /**
     * Has no effect,
     * since this layout manager does not
     * use a per-component string.
     */
    public void addLayoutComponent(String name, Component c) {}

    /**
     * Removes the constraints associated with the specified component.
     *
     * @param c the component being removed from the container
     */
    public void removeLayoutComponent(Component c) {
        componentConstraints.remove(c);
    }

    private static Dimension addInsets(int width, int height, Container p) {
        Insets i = p.getInsets();
        return new Dimension(width + i.left + i.right, height + i.top + i.bottom);
    }

    public Dimension minimumLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent); 
        return addInsets(abandonCycles(pc.getWidth()).getMinimumValue(),
                         abandonCycles(pc.getHeight()).getMinimumValue(),
                         parent);
    }

    public Dimension preferredLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent); 
        return addInsets(abandonCycles(pc.getWidth()).getPreferredValue(),
                         abandonCycles(pc.getHeight()).getPreferredValue(),
                         parent);
    }

    // LayoutManager2 methods.

    public Dimension maximumLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent); 
        return addInsets(abandonCycles(pc.getWidth()).getMaximumValue(),
                         abandonCycles(pc.getHeight()).getMaximumValue(),
                         parent);
    }

    /**
     * If <code>constraints</code> is an instance of 
     * <code>SpringLayout.Constraints</code>,
     * associates the constraints with the specified component.
     * <p>
     * @param   component the component being added
     * @param   constraints the component's constraints
     *
     * @see SpringLayout.Constraints
     */
    public void addLayoutComponent(Component component, Object constraints) {
        if (constraints instanceof Constraints) {
            putConstraints(component, (Constraints)constraints);
        }
    }

    /**
     * Returns 0.5f (centered).
     */
    public float getLayoutAlignmentX(Container p) {
        return 0.5f;
    }

    /**
     * Returns 0.5f (centered).
     */
    public float getLayoutAlignmentY(Container p) {
        return 0.5f;
    }

    public void invalidateLayout(Container p) {}

    // End of LayoutManger2 methods

   /**
     * Links edge <code>e1</code> of component <code>c1</code> to
     * edge <code>e2</code> of component <code>c2</code>,
     * with a fixed distance between the edges. This
     * constraint will cause the assignment
     * <pre>
     *     value(e1, c1) = value(e2, c2) + pad</pre>
     * to take place during all subsequent layout operations.
     * <p>
     * @param   e1 the edge of the dependent
     * @param   c1 the component of the dependent
     * @param   pad the fixed distance between dependent and anchor
     * @param   e2 the edge of the anchor
     * @param   c2 the component of the anchor
     *
     * @see #putConstraint(String, Component, Spring, String, Component)
     */
    public void putConstraint(String e1, Component c1, int pad, String e2, Component c2) {
        putConstraint(e1, c1, Spring.constant(pad), e2, c2);
    }

    /**
     * Links edge <code>e1</code> of component <code>c1</code> to
     * edge <code>e2</code> of component <code>c2</code>. As edge
     * <code>(e2, c2)</code> changes value, edge <code>(e1, c1)</code> will
     * be calculated by taking the (spring) sum of <code>(e2, c2)</code>
     * and <code>s</code>. Each edge must have one of the following values:
     * <code>SpringLayout.NORTH</code>, <code>SpringLayout.SOUTH</code>,
     * <code>SpringLayout.EAST</code>, <code>SpringLayout.WEST</code>.
     * <p>
     * @param   e1 the edge of the dependent
     * @param   c1 the component of the dependent
     * @param   s the spring linking dependent and anchor
     * @param   e2 the edge of the anchor
     * @param   c2 the component of the anchor
     *
     * @see #putConstraint(String, Component, int, String, Component)
     * @see #NORTH
     * @see #SOUTH
     * @see #EAST
     * @see #WEST
     */
    public void putConstraint(String e1, Component c1, Spring s, String e2, Component c2) {
        putConstraint(e1, c1, Spring.sum(s, getConstraint(e2, c2)));
    }

    private void putConstraint(String e, Component c, Spring s) {
        if (s != null) {
            getConstraints(c).setConstraint(e, s);
        }
     }

    private Constraints applyDefaults(Component c, Constraints constraints) {
        if (constraints == null) {
           constraints = new Constraints();
        }
        if (constraints.getWidth() == null) {
            constraints.setWidth(new Spring.WidthSpring(c));
        }
        if (constraints.getHeight() == null) {
            constraints.setHeight(new Spring.HeightSpring(c));
        }
        if (constraints.getX() == null) {
            constraints.setX(Spring.constant(0));
        }
        if (constraints.getY() == null) {
            constraints.setY(Spring.constant(0));
        }
        return constraints;
    }

    private void putConstraints(Component component, Constraints constraints) {
        componentConstraints.put(component, applyDefaults(component, constraints));
    }

    /**
     * Returns the constraints for the specified component.
     * Note that,
     * unlike the <code>GridBagLayout</code>
     * <code>getConstraints</code> method,
     * this method does not clone constraints.
     * If no constraints
     * have been associated with this component,
     * this method
     * returns a default constraints object positioned at
     * 0,0 relative to the parent's Insets and its width/height
     * constrained to the minimum, maximum, and preferred sizes of the
     * component. The size characteristics
     * are not frozen at the time this method is called;
     * instead this method returns a constraints object
     * whose characteristics track the characteristics
     * of the component as they change.
     *
     * @param       c the component whose constraints will be returned
     *
     * @return      the constraints for the specified component
     */
    public Constraints getConstraints(Component c) {
       Constraints result = (Constraints)componentConstraints.get(c);
       if (result == null) {
           if (c instanceof javax.swing.JComponent) {
                Object cp = ((javax.swing.JComponent)c).getClientProperty(this.getClass());
                if (cp instanceof Constraints) {
                    return applyDefaults(c, (Constraints)cp);
                }
            }
            result = new Constraints();
            putConstraints(c, result);
       }
       return result;
    }

    /**
     * Returns the spring controlling the distance between 
     * the specified edge of
     * the component and the top or left edge of its parent. This
     * method, instead of returning the current binding for the
     * edge, returns a proxy that tracks the characteristics
     * of the edge even if the edge is subsequently rebound.
     * Proxies are intended to be used in builder envonments
     * where it is useful to allow the user to define the
     * constraints for a layout in any order. Proxies do, however,
     * provide the means to create cyclic dependencies amongst
     * the constraints of a layout. Such cycles are detected
     * internally by <code>SpringLayout</code> so that
     * the layout operation always terminates.
     *
     * @param edgeName must be 
     *                 <code>SpringLayout.NORTH</code>,
     *                 <code>SpringLayout.SOUTH</code>,
     *                 <code>SpringLayout.EAST</code>, or
     *                 <code>SpringLayout.WEST</code>
     * @param c the component whose edge spring is desired
     *
     * @return a proxy for the spring controlling the distance between the
     *         specified edge and the top or left edge of its parent
     * 
     * @see #NORTH
     * @see #SOUTH
     * @see #EAST
     * @see #WEST
     */
    public Spring getConstraint(String edgeName, Component c) {
        // The interning here is unnecessary; it was added for efficiency.
        edgeName = edgeName.intern();
        return new SpringProxy(edgeName, c, this);
    }

    public void layoutContainer(Container parent) {
        setParent(parent);

        int n = parent.getComponentCount();
        getConstraints(parent).reset();
        for (int i = 0 ; i < n ; i++) {
            getConstraints(parent.getComponent(i)).reset();
        }

        Insets insets = parent.getInsets();
        Constraints pc = getConstraints(parent); 
        abandonCycles(pc.getX()).setValue(0);
        abandonCycles(pc.getY()).setValue(0);        
        abandonCycles(pc.getWidth()).setValue(parent.getWidth() -
                                              insets.left - insets.right);
        abandonCycles(pc.getHeight()).setValue(parent.getHeight() -
                                               insets.top - insets.bottom);
        
        for (int i = 0 ; i < n ; i++) {
	    Component c = parent.getComponent(i);
            Constraints cc = getConstraints(c); 
            int x = abandonCycles(cc.getX()).getValue();
            int y = abandonCycles(cc.getY()).getValue();
            int width = abandonCycles(cc.getWidth()).getValue();
            int height = abandonCycles(cc.getHeight()).getValue();
            c.setBounds(insets.left + x, insets.top + y, width, height);
	}
    }
}
