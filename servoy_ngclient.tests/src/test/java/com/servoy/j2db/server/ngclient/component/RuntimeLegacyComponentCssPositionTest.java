/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;

import com.servoy.j2db.persistence.CSSPosition;

/**
 * Tests for the legacy geometry (width/height/locationX/locationY) fallback that derives values from a
 * component's {@link CSSPosition} in {@link RuntimeLegacyComponent} (SVY-21467).
 *
 * <p>{@code deriveFromCssPosition} and {@code parseCssPixelValue} are private static helpers that use no
 * instance state, so they are invoked reflectively with a {@code null} target.</p>
 */
@SuppressWarnings("nls")
public class RuntimeLegacyComponentCssPositionTest
{
	private static Method parseCssPixelValue;
	private static Method deriveFromCssPosition;

	@BeforeClass
	public static void setUp() throws Exception
	{
		parseCssPixelValue = RuntimeLegacyComponent.class.getDeclaredMethod("parseCssPixelValue", String.class);
		parseCssPixelValue.setAccessible(true);

		deriveFromCssPosition = RuntimeLegacyComponent.class.getDeclaredMethod("deriveFromCssPosition", String.class, CSSPosition.class);
		deriveFromCssPosition.setAccessible(true);
	}

	private static Integer parse(String cssValue) throws Exception
	{
		return (Integer)parseCssPixelValue.invoke(null, cssValue);
	}

	private static Integer derive(String name, CSSPosition cssPosition) throws Exception
	{
		return (Integer)deriveFromCssPosition.invoke(null, name, cssPosition);
	}

	private static CSSPosition css(String top, String right, String bottom, String left, String width, String height)
	{
		return new CSSPosition(top, right, bottom, left, width, height);
	}

	// ---- parseCssPixelValue ----

	@Test
	public void parseNullReturnsNull() throws Exception
	{
		assertNull(parse(null));
	}

	@Test
	public void parseEmptyReturnsNull() throws Exception
	{
		assertNull(parse(""));
	}

	@Test
	public void parseBlankReturnsNull() throws Exception
	{
		assertNull(parse("   "));
	}

	@Test
	public void parsePlainIntReturnsValue() throws Exception
	{
		assertEquals(Integer.valueOf(40), parse("40"));
	}

	@Test
	public void parsePxSuffixReturnsValue() throws Exception
	{
		assertEquals(Integer.valueOf(40), parse("40px"));
	}

	@Test
	public void parsePxSuffixWithSpaceBeforePxReturnsValue() throws Exception
	{
		// trailing "px" is stripped, then the remainder is trimmed -> "40" -> 40
		assertEquals(Integer.valueOf(40), parse("40 px"));
	}

	@Test
	public void parseSurroundingWhitespaceReturnsValue() throws Exception
	{
		assertEquals(Integer.valueOf(40), parse(" 40px "));
	}

	@Test
	public void parseZeroReturnsZero() throws Exception
	{
		assertEquals(Integer.valueOf(0), parse("0"));
	}

	@Test
	public void parseNegativeReturnsNull() throws Exception
	{
		assertNull(parse("-1"));
	}

	@Test
	public void parseNegativeMultiDigitReturnsNull() throws Exception
	{
		assertNull(parse("-10"));
	}

	@Test
	public void parsePercentageReturnsNull() throws Exception
	{
		assertNull(parse("50%"));
	}

	@Test
	public void parseCalcReturnsNull() throws Exception
	{
		assertNull(parse("calc(100% - 10px)"));
	}

	@Test
	public void parseAutoReturnsNull() throws Exception
	{
		assertNull(parse("auto"));
	}

	@Test
	public void parseEmUnitReturnsNull() throws Exception
	{
		assertNull(parse("40em"));
	}

	@Test
	public void parseBarePxReturnsNull() throws Exception
	{
		// "px" stripped to "" -> not parseable
		assertNull(parse("px"));
	}

	// ---- deriveFromCssPosition ----

	@Test
	public void deriveHeightReadsHeightField() throws Exception
	{
		CSSPosition p = css(null, null, null, null, null, "40");
		assertEquals(Integer.valueOf(40), derive("height", p));
	}

	@Test
	public void deriveWidthReadsWidthField() throws Exception
	{
		CSSPosition p = css(null, null, null, null, "120px", null);
		assertEquals(Integer.valueOf(120), derive("width", p));
	}

	@Test
	public void deriveLocationXReadsLeftField() throws Exception
	{
		CSSPosition p = css(null, null, null, "15", null, null);
		assertEquals(Integer.valueOf(15), derive("locationX", p));
	}

	@Test
	public void deriveLocationYReadsTopField() throws Exception
	{
		CSSPosition p = css("25px", null, null, null, null, null);
		assertEquals(Integer.valueOf(25), derive("locationY", p));
	}

	@Test
	public void deriveUnrelatedNameReturnsNull() throws Exception
	{
		CSSPosition p = css("1", "2", "3", "4", "5", "6");
		assertNull(derive("bogus", p));
	}

	@Test
	public void deriveNullSideReturnsNull() throws Exception
	{
		CSSPosition p = css(null, null, null, null, null, null);
		assertNull(derive("height", p));
	}

	@Test
	public void derivePercentageSideReturnsNull() throws Exception
	{
		CSSPosition p = css(null, null, null, null, "50%", null);
		assertNull(derive("width", p));
	}
}
