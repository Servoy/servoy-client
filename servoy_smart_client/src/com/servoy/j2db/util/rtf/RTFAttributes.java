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
package com.servoy.j2db.util.rtf;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabStop;

class RTFAttributes
{
    static RTFAttribute attributes[];

    static {
        Vector a = new Vector();
	int CHR = RTFAttribute.D_CHARACTER;
	int PGF = RTFAttribute.D_PARAGRAPH;
	int SEC = RTFAttribute.D_SECTION;
	int DOC = RTFAttribute.D_DOCUMENT;
	int PST = RTFAttribute.D_META;
	Boolean True = new Boolean(true);
	Boolean False = new Boolean(false);

	a.addElement(new BooleanAttribute(CHR, StyleConstants.Italic, "i")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, StyleConstants.Bold, "b")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, StyleConstants.Underline, "ul")); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(PGF, StyleConstants.LeftIndent, "li", //$NON-NLS-1$
					0f, 0));
	a.addElement(NumericAttribute.NewTwips(PGF, StyleConstants.RightIndent, "ri", //$NON-NLS-1$
					0f, 0));
	a.addElement(NumericAttribute.NewTwips(PGF, StyleConstants.FirstLineIndent, "fi", //$NON-NLS-1$
					0f, 0));

	a.addElement(new AssertiveAttribute(PGF, StyleConstants.Alignment,
					    "ql", StyleConstants.ALIGN_LEFT)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PGF, StyleConstants.Alignment,
					    "qr", StyleConstants.ALIGN_RIGHT)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PGF, StyleConstants.Alignment,
					    "qc", StyleConstants.ALIGN_CENTER)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PGF, StyleConstants.Alignment,
					    "qj", StyleConstants.ALIGN_JUSTIFIED)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(PGF, StyleConstants.SpaceAbove,
					"sa", 0)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(PGF, StyleConstants.SpaceBelow,
					"sb", 0)); //$NON-NLS-1$

	a.addElement(new AssertiveAttribute(PST, RTFReader.TabAlignmentKey,
					    "tqr", TabStop.ALIGN_RIGHT)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PST, RTFReader.TabAlignmentKey,
					    "tqc", TabStop.ALIGN_CENTER)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PST, RTFReader.TabAlignmentKey,
					    "tqdec", TabStop.ALIGN_DECIMAL)); //$NON-NLS-1$
		     

	a.addElement(new AssertiveAttribute(PST, RTFReader.TabLeaderKey,
					    "tldot", TabStop.LEAD_DOTS)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PST, RTFReader.TabLeaderKey,
					    "tlhyph", TabStop.LEAD_HYPHENS)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PST, RTFReader.TabLeaderKey,
					    "tlul", TabStop.LEAD_UNDERLINE)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PST, RTFReader.TabLeaderKey,
					    "tlth", TabStop.LEAD_THICKLINE)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PST, RTFReader.TabLeaderKey,
					    "tleq", TabStop.LEAD_EQUALS)); //$NON-NLS-1$

	/* The following aren't actually recognized by Swing */
	a.addElement(new BooleanAttribute(CHR, Constants.Caps,      "caps")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, Constants.Outline,   "outl")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, Constants.SmallCaps, "scaps")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, Constants.Shadow,    "shad")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, Constants.Hidden,    "v")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, Constants.Strikethrough,
					       "strike")); //$NON-NLS-1$
	a.addElement(new BooleanAttribute(CHR, Constants.Deleted,
					       "deleted")); //$NON-NLS-1$


	
	a.addElement(new AssertiveAttribute(DOC, "saveformat", "defformat", "RTF")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	a.addElement(new AssertiveAttribute(DOC, "landscape", "landscape")); //$NON-NLS-1$ //$NON-NLS-2$

	a.addElement(NumericAttribute.NewTwips(DOC, Constants.PaperWidth,
					       "paperw", 12240)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(DOC, Constants.PaperHeight,
					       "paperh", 15840)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(DOC, Constants.MarginLeft,
					       "margl",  1800)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(DOC, Constants.MarginRight,
					       "margr",  1800)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(DOC, Constants.MarginTop,
					       "margt",  1440)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(DOC, Constants.MarginBottom,
					       "margb",  1440)); //$NON-NLS-1$
	a.addElement(NumericAttribute.NewTwips(DOC, Constants.GutterWidth,
					       "gutter", 0)); //$NON-NLS-1$

	a.addElement(new AssertiveAttribute(PGF, Constants.WidowControl,
					    "nowidctlpar", False)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(PGF, Constants.WidowControl,
					    "widctlpar", True)); //$NON-NLS-1$
	a.addElement(new AssertiveAttribute(DOC, Constants.WidowControl,
					    "widowctrl", True)); //$NON-NLS-1$
	

	RTFAttribute[] attrs = new RTFAttribute[a.size()];
	a.copyInto(attrs);
	attributes = attrs;
    }	

    static Dictionary attributesByKeyword()
    {
        Dictionary d = new Hashtable(attributes.length);
	int i, m;

	m = attributes.length;
	for(i = 0; i < m; i++)
	    d.put(attributes[i].rtfName(), attributes[i]);

	return d;
    }

    /************************************************************************/
    /************************************************************************/

    static abstract class GenericAttribute 
    {
        int domain;
	Object swingName;
	String rtfName;
	
	protected GenericAttribute(int d,Object s, String r)
	{
	    domain = d;
	    swingName = s;
	    rtfName = r;
	}

	public int domain() { return domain; }
	public Object swingName() { return swingName; }
	public String rtfName() { return rtfName; }
	
	abstract boolean set(MutableAttributeSet target);
	abstract boolean set(MutableAttributeSet target, int parameter);
	abstract boolean setDefault(MutableAttributeSet target);
	
	public boolean write(AttributeSet source,
			     RTFGenerator target,
			     boolean force)
	    throws IOException
	{
	    return writeValue(source.getAttribute(swingName), target, force);
	}

	public boolean writeValue(Object value, RTFGenerator target,
				  boolean force)
	    throws IOException 
	{
	    return false;
	}
    }

    static class BooleanAttribute 
        extends GenericAttribute
	implements RTFAttribute
    {
        boolean rtfDefault;
	boolean swingDefault;

	protected static final Boolean True = new Boolean(true);
	protected static final Boolean False = new Boolean(false);
	
	public BooleanAttribute(int d, Object s,
				String r, boolean ds, boolean dr)
	{
	    super(d, s, r);
	    swingDefault = ds;
	    rtfDefault = dr;
	}

	public BooleanAttribute(int d, Object s, String r)
	{
	    super(d, s, r);
	    
	    swingDefault = false;
	    rtfDefault = false;
	}

	public boolean set(MutableAttributeSet target)
	{
	    /* TODO: There's some ambiguity about whether this should
	       *set* or *toggle* the attribute. */
	    target.addAttribute(swingName, True);

	    return true;  /* true indicates we were successful */
	}

	public boolean set(MutableAttributeSet target, int parameter)
	{
	    /* See above note in the case that parameter==1 */
	    Boolean value = ( parameter != 0 ? True : False );
	    
	    target.addAttribute(swingName, value);
	    
	    return true; /* true indicates we were successful */
	}
	
	public boolean setDefault(MutableAttributeSet target)
	{
	    if (swingDefault != rtfDefault ||
		( target.getAttribute(swingName) != null ) )
	      target.addAttribute(swingName, new Boolean(rtfDefault));
	    return true;
	}

	public boolean writeValue(Object o_value,
				  RTFGenerator target,
				  boolean force)
	    throws IOException
	{
	    Boolean val;

	    if (o_value == null)
	      val = new Boolean(swingDefault);
	    else
	      val = (Boolean)o_value;
	    
	    if (force || (val.booleanValue() != rtfDefault)) {
		if (val.booleanValue()) {
		    target.writeControlWord(rtfName);
		} else {
		    target.writeControlWord(rtfName, 0);
		}
	    }
	    return true;
	}
    }


    static class AssertiveAttribute
        extends GenericAttribute
	implements RTFAttribute
    {
	Object swingValue;

	public AssertiveAttribute(int d, Object s, String r)
	{
	    super(d, s, r);
	    swingValue = new Boolean(true);
	}

	public AssertiveAttribute(int d, Object s, String r, Object v)
	{
	    super(d, s, r);
	    swingValue = v;
	}

	public AssertiveAttribute(int d, Object s, String r, int v)
	{
	    super(d, s, r);
	    swingValue = new Integer(v);
	}
	
	public boolean set(MutableAttributeSet target)
	{
	    if (swingValue == null)
	        target.removeAttribute(swingName);
	    else
	        target.addAttribute(swingName, swingValue);
	    
	    return true;
	}
    
	public boolean set(MutableAttributeSet target, int parameter)
	{
	    return false;
	}
    
	public boolean setDefault(MutableAttributeSet target)
	{
	    target.removeAttribute(swingName);
	    return true;
	}

	public boolean writeValue(Object value,
				  RTFGenerator target,
				  boolean force)
	    throws IOException
        {
	    if (value == null) {
		return ! force;
	    }

	    if (value.equals(swingValue)) {
		target.writeControlWord(rtfName);
		return true;
	    }
	    
	    return ! force;
	}
    }


    static class NumericAttribute 
        extends GenericAttribute
	implements RTFAttribute
    {
	int rtfDefault;
	Number swingDefault;
	float scale;
	
	protected NumericAttribute(int d, Object s, String r)
	{
	    super(d, s, r);
	    rtfDefault = 0;
	    swingDefault = null;
	    scale = 1f;
	}

	public NumericAttribute(int d, Object s,
				String r, int ds, int dr)
	{
	    this(d, s, r, new Integer(ds), dr, 1f);
	}

	public NumericAttribute(int d, Object s,
				String r, Number ds, int dr, float sc)
	{
	    super(d, s, r);
	    swingDefault = ds;
	    rtfDefault = dr;
	    scale = sc;
	}

	public static NumericAttribute NewTwips(int d, Object s, String r,
						float ds, int dr)
	{
	    return new NumericAttribute(d, s, r, new Float(ds), dr, 20f);
	}

	public static NumericAttribute NewTwips(int d, Object s, String r,
						int dr)
	{
	    return new NumericAttribute(d, s, r, null, dr, 20f);
	}

	public boolean set(MutableAttributeSet target)
	{
	    return false;
	}

	public boolean set(MutableAttributeSet target, int parameter)
	{
	    Number swingValue;

	    if (scale == 1f)
	        swingValue = new Integer(parameter);
	    else
	        swingValue = new Float(parameter / scale);
	    target.addAttribute(swingName, swingValue);
	    return true;
	}

	public boolean setDefault(MutableAttributeSet target)
	{
	    Number old = (Number)target.getAttribute(swingName);
	    if (old == null)
	        old = swingDefault;
	    if (old != null && (
		    (scale == 1f && old.intValue() == rtfDefault) ||
		    (Math.round(old.floatValue() * scale) == rtfDefault)
	       ))
	        return true;
	    set(target, rtfDefault);
	    return true;
	}

	public boolean writeValue(Object o_value,
				  RTFGenerator target,
				  boolean force)
	    throws IOException
	{
	    Number value = (Number)o_value;
	    if (value == null)
	        value = swingDefault;
	    if (value == null) {
		/* TODO: What is the proper behavior if the Swing object does
		   not specify a value, and we don't know its default value?
		   Currently we pretend that the RTF default value is
		   equivalent (probably a workable assumption) */
		return true;
	    }
	    int int_value = Math.round(value.floatValue() * scale);
	    if (force || (int_value != rtfDefault))
	        target.writeControlWord(rtfName, int_value);
	    return true;
	}
    }
}
