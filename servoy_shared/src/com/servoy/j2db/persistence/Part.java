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
package com.servoy.j2db.persistence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.base.persistence.IBasePart;
import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;


/**
 * A part is a section from a Form, which can be used in reporting to aggregate data
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.PARTS)
@ServoyClientSupport(mc = false, wc = true, sc = true)
public class Part extends AbstractBase implements ISupportSize, IPersistCloneable, ICloneable, ISupportExtendsID, IBasePart
{

	private static final long serialVersionUID = 1L;

	public static final int TITLE_HEADER = IPartConstants.TITLE_HEADER;
	public static final int HEADER = IPartConstants.HEADER;
	public static final int LEADING_GRAND_SUMMARY = IPartConstants.LEADING_GRAND_SUMMARY;
	public static final int LEADING_SUBSUMMARY = IPartConstants.LEADING_SUBSUMMARY;
	public static final int BODY = IPartConstants.BODY;
	public static final int TRAILING_SUBSUMMARY = IPartConstants.TRAILING_SUBSUMMARY;
	public static final int TRAILING_GRAND_SUMMARY = IPartConstants.TRAILING_GRAND_SUMMARY;
	public static final int FOOTER = IPartConstants.FOOTER;
	public static final int TITLE_FOOTER = IPartConstants.TITLE_FOOTER;
	public static final int PART_ARRAY_SIZE = IPartConstants.PART_ARRAY_SIZE;

	public static boolean rendersOnlyInPrint(int partType)
	{
		return partType == Part.TITLE_FOOTER || partType == Part.LEADING_SUBSUMMARY || partType == Part.TRAILING_SUBSUMMARY;
	}

	/**
	 * Constructor I
	 */
	Part(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.PARTS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Property Methods
	 */

	/**
	 * Set the absolute height (from top of Form).
	 *
	 * @param arg the height
	 */
	public void setHeight(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_HEIGHT, arg);
	}

	/**
	 * The height of a selected part; specified in pixels.
	 *
	 * This height property is the lowerbound as its ending Y value (0 == top of the form).
	 */
	public int getHeight()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_HEIGHT).intValue();
	}

	/**
	 * Set the partType
	 *
	 * @param arg the partType
	 */
	public void setPartType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PARTTYPE, arg);
	}

	/**
	 * The type of this part.
	 */
	public int getPartType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PARTTYPE).intValue();
	}

	/**
	 * Set the background
	 *
	 * @param arg the background
	 */
	public void setBackground(java.awt.Color arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND, arg);
	}

	/**
	 * The background color of the form part.
	 *
	 * NOTE: When no background color has been set, the default background
	 * color will be determined by the Look and Feel (LAF) that has been selected
	 * in Application Preferences.
	 */
	public java.awt.Color getBackground()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND);
	}

	/**
	 * Set the sequence
	 *
	 * @param arg the sequence
	 */
	public void setSequence(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SEQUENCE, arg);
	}

	/**
	 * Get the sequence
	 *
	 * @return the sequence
	 */
	public int getSequence()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SEQUENCE).intValue();
	}


	//Size methods only for ISupportSize
	/**
	 * Set the size
	 *
	 * @param arg the size
	 */
	public void setSize(java.awt.Dimension arg)
	{
		setHeight(arg.height);
		width = arg.width;
	}

	private int width;

	/**
	 * Get the size
	 *
	 * @return the size
	 */
	public java.awt.Dimension getSize()
	{
		return new java.awt.Dimension(width, getHeight());
	}

	/**
	 * When set, the remainder of a selected part that does not fit on the page currently
	 * being printed, will not be transported to the next page - it will break where the page
	 * ends and continue on the next page.
	 *
	 * NOTE: Make sure to set this option when you are printing more than one page per record.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getAllowBreakAcrossPageBounds()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWBREAKACROSSPAGEBOUNDS).booleanValue();
	}

	/**
	 * Set the allowBreakAcrossPageBounds
	 *
	 * @param b the allowBreakAcrossPageBounds
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setAllowBreakAcrossPageBounds(boolean b)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ALLOWBREAKACROSSPAGEBOUNDS, b);
	}

	/**
	 * When set, the remainder of a selected part that is broken due to the page
	 * ending will not be printed on the next page - it will be discarded.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getDiscardRemainderAfterBreak()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DISCARDREMAINDERAFTERBREAK).booleanValue();
	}

	/**
	 * Set the discardRemainderAfterBreak
	 *
	 * @param b the discardRemainderAfterBreak
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setDiscardRemainderAfterBreak(boolean b)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DISCARDREMAINDERAFTERBREAK, b);
	}

	/**
	 * For Leading Subsummary or Trailing Subsummary parts, one or more
	 * dataproviders can be added as Break (GroupBy) dataproviders. The
	 * Leading/Trailing Subsummary parts will be displayed once for each
	 * resulted group of data.
	 */
	public String getGroupbyDataProviderIDs()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_GROUPBYDATAPROVIDERIDS);
	}

	/**
	 * Set the groupbyDataProviderIDs
	 *
	 * @param arg the groupbyDataProviderIDs
	 */
	public void setGroupbyDataProviderIDs(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_GROUPBYDATAPROVIDERIDS, arg);
	}

	/**
	 * When set, a page break will be inserted before each occurrence of a selected part.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getPageBreakBefore()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PAGEBREAKBEFORE).booleanValue();
	}

	/**
	 * Set the pageBreakBefore
	 *
	 * @param b the pageBreakBefore
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPageBreakBefore(boolean b)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PAGEBREAKBEFORE, b);
	}

	/**
	 * A page break will be inserted after a specified number of occurences of a selected part.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getPageBreakAfterOccurrence()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PAGEBREAKAFTEROCCURRENCE).intValue();
	}

	/**
	 * Set the pageBreakAfterOccurrence
	 *
	 * @param i the pageBreakAfterOccurrence
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPageBreakAfterOccurrence(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PAGEBREAKAFTEROCCURRENCE, i);
	}

	/**
	 * When set, page numbering will be restarted after each occurrence of a selected part.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getRestartPageNumber()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_RESTARTPAGENUMBER).booleanValue();
	}

	/**
	 * Set the restartPageNumber
	 *
	 * @param b the restartPageNumber
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setRestartPageNumber(boolean b)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_RESTARTPAGENUMBER, b);
	}

	/**
	 * When set, the last part on a page (such as a Trailing Grand Summary part) will
	 * "sink" to the lowest part of the page when there is free space.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getSinkWhenLast()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SINKWHENLAST).booleanValue();
	}

	/**
	 * Set the sinkWhenLast
	 *
	 * @param b the sinkWhenLast
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setSinkWhenLast(boolean b)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SINKWHENLAST, b);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */
	public static String getDisplayName(int type)
	{
		switch (type)
		{
			case TITLE_HEADER :
				return "Title Header"; //$NON-NLS-1$

			case HEADER :
				return "Header"; //$NON-NLS-1$

			case LEADING_GRAND_SUMMARY :
				return "Leading Grand Summary"; //$NON-NLS-1$

			case LEADING_SUBSUMMARY :
				return "Leading Subsummary"; //$NON-NLS-1$

			case BODY :
				return "Body"; //$NON-NLS-1$

			case TRAILING_SUBSUMMARY :
				return "Trailing Subsummary"; //$NON-NLS-1$

			case TRAILING_GRAND_SUMMARY :
				return "Trailing Grand Summary"; //$NON-NLS-1$

			case FOOTER :
				return "Footer"; //$NON-NLS-1$

			case TITLE_FOOTER :
				return "Title Footer"; //$NON-NLS-1$

			default :
				return "<not Init>"; //$NON-NLS-1$
		}
	}

	public static String getCSSSelector(int type)
	{
		switch (type)
		{
			case TITLE_HEADER :
				return "title_header"; //$NON-NLS-1$

			case HEADER :
				return "header"; //$NON-NLS-1$

			case BODY :
				return "body"; //$NON-NLS-1$

			case FOOTER :
				return "footer"; //$NON-NLS-1$

			case TITLE_FOOTER :
				return "title_footer"; //$NON-NLS-1$

			default :
				return null;
		}
	}

	@Override
	public String toString()
	{
		return getDisplayName(getPartType());
	}

	public String getEditorName()
	{
		String s = getDisplayName(getPartType());
		boolean b = (getPartType() == Part.LEADING_SUBSUMMARY || getPartType() == Part.TRAILING_SUBSUMMARY);
		if (b)
		{
			s += " ON " + getGroupbyDataProviderIDs(); //$NON-NLS-1$
		}
		return s;
	}

	public boolean canBeMoved()
	{
		return canBeMoved(getPartType());
	}

	public static boolean canBeMoved(int partType)
	{
		return (partType == Part.LEADING_SUBSUMMARY || partType == Part.TRAILING_SUBSUMMARY);
	}

	public Part getPreviousPart() throws RepositoryException
	{
		Part prevPart = null;
		Iterator<Part> parts = getParent().getObjects(IRepository.PARTS);
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getHeight() < getHeight() && (prevPart == null || part.getHeight() > prevPart.getHeight()))
			{
				prevPart = part;
			}
		}
		return prevPart;
	}

	/**
	 * Helper method to get all form elements inside of the part, note they all have the part Y offset in there location!
	 * @return list of all elements in the part
	 */
	public List<IFormElement> getFormElementsSortedByYX()
	{
		List<IFormElement> retval = new ArrayList<IFormElement>();
		Form f = (Form)getParent();
		int top = f.getPartStartYPos(getID());
		int bottom = f.getPartStartYPos(getID());
		Iterator<IPersist> le = f.getAllObjects(PositionComparator.YX_PERSIST_COMPARATOR);
		while (le.hasNext())
		{
			IPersist p = le.next();
			if (p instanceof IFormElement && ((IFormElement)p).getLocation().y >= top && ((IFormElement)p).getLocation().y < bottom)
			{
				retval.add((IFormElement)p);
			}
		}
		return retval;
	}

	public int getExtendsID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID).intValue();
	}

	public void setExtendsID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID, arg);
	}

	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public String getStyleClass()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS);
	}

	public void setStyleClass(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS, arg);
	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		return PersistHelper.getFlattenedPropertiesMap(this);
	}

	@Override
	public ISupportChilds getRealParent()
	{
		return getParent();
	}
}
