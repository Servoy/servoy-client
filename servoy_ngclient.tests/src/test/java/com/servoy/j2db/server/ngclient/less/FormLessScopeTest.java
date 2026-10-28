package com.servoy.j2db.server.ngclient.less;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.servoy.j2db.server.ngclient.AngularFormGenerator;

/**
 * SVY-21592 a form .less is scoped with [svy-formname]; at-rules must keep their condition and the rules inside a
 * conditional at-rule must get the scope as well.
 */
@SuppressWarnings("nls")
public class FormLessScopeTest
{
	private static String parse(String less) throws Exception
	{
		return AngularFormGenerator.parseLess(less, "frm", null);
	}

	@Test
	public void plainRulesAreScoped() throws Exception
	{
		String css = parse(".hero { color: red; } .card .title { color: blue; }");
		assertTrue(css.contains(".hero[svy-frm]{"), css);
		assertTrue(css.contains(".card[svy-frm] .title{"), css);
	}

	@Test
	public void mediaQueryKeepsConditionAndScopesInnerRules() throws Exception
	{
		String css = parse(
			".hero { grid-template-columns: 88px 1fr; } @media (max-width: 640px) { .hero { grid-template-columns: 1fr; } .card .title { color: blue; } }");
		assertFalse(css.contains("@media[svy-"), css);
		assertTrue(css.contains("@media (max-width: 640px){.hero[svy-frm]{grid-template-columns:1fr}.card[svy-frm] .title{"), css);
	}

	@Test
	public void mediaQueryWithMediaTypeKeepsCondition() throws Exception
	{
		String css = parse("@media screen and (max-width: 640px) { .hero { display: block; } }");
		assertTrue(css.contains("@media screen and (max-width: 640px){.hero[svy-frm]{display:block}}"), css);
	}

	@Test
	public void nestedMediaQueryIsMergedAndScoped() throws Exception
	{
		String css = parse("@media screen and (min-width: 1000px) { @media (max-width: 1200px) { .hero { gap: 4px; } } }");
		assertFalse(css.contains("@media[svy-"), css);
		assertTrue(css.contains("@media screen and (min-width: 1000px) and (max-width: 1200px){.hero[svy-frm]{gap:4px}}"), css);
	}

	@Test
	public void supportsKeepsConditionAndScopesInnerRules() throws Exception
	{
		String css = parse("@supports (display: grid) { .hero { display: grid; } }");
		assertFalse(css.contains("@supports[svy-"), css);
		assertTrue(css.contains(".hero[svy-frm]{display:grid}"), css);
	}

	@Test
	public void mediaNestedInRuleIsStillScoped() throws Exception
	{
		String css = parse(".nested { color: green; @media (max-width: 500px) { color: black; } }");
		assertTrue(css.contains("@media (max-width: 500px){.nested[svy-frm]{color:#000}}"), css);
	}

	@Test
	public void keyframesAndFontFaceAreNotScoped() throws Exception
	{
		String css = parse(
			"@keyframes spin { from { opacity: 0; } to { opacity: 1; } } @font-face { font-family: X; src: url(x.woff); } .a { animation: spin 1s; }");
		assertFalse(css.contains("@keyframes[svy-"), css);
		assertFalse(css.contains("@font-face[svy-"), css);
		assertFalse(css.contains("from[svy-"), css);
		assertTrue(css.contains("@keyframes spin{"), css);
		assertTrue(css.contains(".a[svy-frm]{"), css);
	}
}
