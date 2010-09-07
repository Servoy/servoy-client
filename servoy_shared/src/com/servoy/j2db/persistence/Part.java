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

import java.util.Iterator;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.UUID;


/**
 * A part is a section from a Form, which can be used in reporting to aggregate data
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Part extends AbstractBase implements ISupportSize, IPersistCloneable, ICloneable
{
	public static final int TITLE_HEADER = 1;
	public static final int HEADER = 2;
	public static final int LEADING_GRAND_SUMMARY = 3;//no group by
	public static final int LEADING_SUBSUMMARY = 4;//group by (n times!)
	public static final int BODY = 5;
	public static final int TRAILING_SUBSUMMARY = 6;//group by (n times!)
	public static final int TRAILING_GRAND_SUMMARY = 7;//no group by
	public static final int FOOTER = 8;
	public static final int TITLE_FOOTER = 9;
	public static final int PART_ARRAY_SIZE = 10;

	public static boolean rendersOnlyInPrint(int partType)
	{
		return partType == Part.TITLE_FOOTER || partType == Part.LEADING_SUBSUMMARY || partType == Part.TRAILING_SUBSUMMARY;
	}

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private int partType;
	private int height;
	private java.awt.Color background = null;
	private int sequence;

	private boolean allowBreakAcrossPageBounds;
	private boolean discardRemainderAfterBreak;
	private String groupbyDataProviderIDs;
	private boolean pageBreakBefore;
	private int pageBreakAfterOccurrence;
	private boolean restartPageNumber;
	private boolean sinkWhenLast;

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
		checkForChange(height, arg);
		height = arg;
	}

	/**
	 * The height of a selected part; specified in pixels. 
	 * 
	 * This height property is the lowerbound as its ending Y value (0 == top of the form).
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * Set the partType
	 * 
	 * @param arg the partType
	 */
	public void setPartType(int arg)
	{
		checkForChange(partType, arg);
		partType = arg;
	}

	/**
	 * The type of this part.
	 */
	public int getPartType()
	{
		return partType;
	}

	/**
	 * Set the background
	 * 
	 * @param arg the background
	 */
	public void setBackground(java.awt.Color arg)
	{
		checkForChange(background, arg);
		background = arg;
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
		return background;
	}

	/**
	 * Set the sequence
	 * 
	 * @param arg the sequence
	 */
	public void setSequence(int arg)
	{
		checkForChange(sequence, arg);
		sequence = arg;
	}

	/**
	 * Get the sequence
	 * 
	 * @return the sequence
	 */
	public int getSequence()
	{
		return sequence;
	}


	//Size methods only for ISupportSize
	/**
	 * Set the size
	 * 
	 * @param arg the size
	 */
	public void setSize(java.awt.Dimension arg)
	{
		checkForChange(getSize(), arg);
		height = arg.height;
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
		return new java.awt.Dimension(width, height);
	}

	/**
	 * When set, the remainder of a selected part that does not fit on the page currently 
	 * being printed, will not be transported to the next page - it will break where the page 
	 * ends and continue on the next page. 
	 * 
	 * NOTE: Make sure to set this option when you are printing more than one page per record.
	 */
	public boolean getAllowBreakAcrossPageBounds()
	{
		return allowBreakAcrossPageBounds;
	}

	/**
	 * Set the allowBreakAcrossPageBounds
	 * 
	 * @param b the allowBreakAcrossPageBounds
	 */
	public void setAllowBreakAcrossPageBounds(boolean b)
	{
		checkForChange(allowBreakAcrossPageBounds, b);
		allowBreakAcrossPageBounds = b;
	}

	/**
	 * When set, the remainder of a selected part that is broken due to the page 
	 * ending will not be printed on the next page - it will be discarded.
	 */
	public boolean getDiscardRemainderAfterBreak()
	{
		return discardRemainderAfterBreak;
	}

	/**
	 * Set the discardRemainderAfterBreak
	 * 
	 * @param b the discardRemainderAfterBreak
	 */
	public void setDiscardRemainderAfterBreak(boolean b)
	{
		checkForChange(discardRemainderAfterBreak, b);
		discardRemainderAfterBreak = b;
	}

	/**
	 * For Leading Subsummary or Trailing Subsummary parts, one or more
	 * dataproviders can be added as Break (GroupBy) dataproviders. The
	 * Leading/Trailing Subsummary parts will be displayed once for each
	 * resulted group of data.
	 */
	public String getGroupbyDataProviderIDs()
	{
		return groupbyDataProviderIDs;
	}

	/**
	 * Set the groupbyDataProviderIDs
	 * 
	 * @param arg the groupbyDataProviderIDs
	 */
	public void setGroupbyDataProviderIDs(String arg)
	{
		checkForChange(groupbyDataProviderIDs, arg);
		groupbyDataProviderIDs = arg;
	}

	/**
	 * When set, a page break will be inserted before each occurrence of a selected part.
	 */
	public boolean getPageBreakBefore()
	{
		return pageBreakBefore;
	}

	/**
	 * Set the pageBreakBefore
	 * 
	 * @param b the pageBreakBefore
	 */
	public void setPageBreakBefore(boolean b)
	{
		checkForChange(pageBreakBefore, b);
		pageBreakBefore = b;
	}

	/**
	 * A page break will be inserted after a specified number of occurences of a selected part.
	 */
	public int getPageBreakAfterOccurrence()
	{
		return pageBreakAfterOccurrence;
	}

	/**
	 * Set the pageBreakAfterOccurrence
	 * 
	 * @param i the pageBreakAfterOccurrence
	 */
	public void setPageBreakAfterOccurrence(int i)
	{
		checkForChange(pageBreakAfterOccurrence, i);
		pageBreakAfterOccurrence = i;
	}

	/**
	 * When set, page numbering will be restarted after each occurrence of a selected part.
	 */
	public boolean getRestartPageNumber()
	{
		return restartPageNumber;
	}

	/**
	 * Set the restartPageNumber
	 * 
	 * @param b the restartPageNumber
	 */
	public void setRestartPageNumber(boolean b)
	{
		checkForChange(restartPageNumber, b);
		restartPageNumber = b;
	}

	/**
	 * When set, the last part on a page (such as a Trailing Grand Summary part) will 
	 * "sink" to the lowest part of the page when there is free space.
	 */
	public boolean getSinkWhenLast()
	{
		return sinkWhenLast;
	}

	/**
	 * Set the sinkWhenLast
	 * 
	 * @param b the sinkWhenLast
	 */
	public void setSinkWhenLast(boolean b)
	{
		checkForChange(sinkWhenLast, b);
		sinkWhenLast = b;
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

	@Override
	public String toString()
	{
		return getDisplayName(partType);
	}

	public String getEditorName()
	{
		String s = getDisplayName(partType);
		boolean b = (getPartType() == Part.LEADING_SUBSUMMARY || getPartType() == Part.TRAILING_SUBSUMMARY);
		if (b)
		{
			s += " ON " + groupbyDataProviderIDs; //$NON-NLS-1$
		}
		return s;
	}

	public boolean canBeMoved()
	{
		return canBeMoved(partType);
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
			if (part.height < height && (prevPart == null || part.height > prevPart.height))
			{
				prevPart = part;
			}
		}
		return prevPart;
	}
}
