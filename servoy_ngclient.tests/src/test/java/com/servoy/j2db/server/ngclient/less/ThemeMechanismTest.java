package com.servoy.j2db.server.ngclient.less;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;

/**
 * SVY-21639 Phase 0: the constructs a semantic-core theme relies on must survive the default jlessc compiler.
 * CSS-level functions (var, color-mix, calc, :where, :focus-visible) must pass through verbatim, LESS colour
 * functions must still evaluate on LESS seed variables.
 */
@SuppressWarnings("nls")
public class ThemeMechanismTest
{
	private static String compile(String less)
	{
		return JLessCompiler.compileLess(less, null, "mechanism.less").replaceAll("\\s+", " ");
	}

	@Test
	public void customPropertiesAreEmittedFromLessSeeds()
	{
		String css = compile("@accent: #2563EB; :root { --svy-accent: @accent; }");
		assertTrue(css.contains("--svy-accent: #2563EB;"), css);
	}

	@Test
	public void cssFunctionsPassThroughVerbatim()
	{
		String css = compile(".a { background: color-mix(in srgb, var(--svy-accent) 12%, var(--svy-surface)); padding: calc(var(--svy-space) * 3); " +
			"outline: 2px solid var(--svy-focus-ring, var(--svy-accent)); }");
		assertTrue(css.contains("color-mix(in srgb, var(--svy-accent) 12%, var(--svy-surface))"), css);
		assertTrue(css.contains("calc(var(--svy-space) * 3)"), css);
		assertTrue(css.contains("var(--svy-focus-ring, var(--svy-accent))"), css);
	}

	@Test
	public void modernSelectorsPassThrough()
	{
		String css = compile(":where(.svy-x) .e:focus-visible { color: red; } [data-svy-theme=dark] { --svy-surface: #111827; }");
		assertTrue(css.contains(":where(.svy-x) .e:focus-visible"), css);
		assertTrue(css.contains("[data-svy-theme=dark] { --svy-surface: #111827; }"), css);
	}

	@Test
	public void lessColourFunctionsEvaluateOnSeeds()
	{
		String css = compile("@accent: #2563EB; @surface: #FFFFFF; @text: #1F2937; " +
			".b { color: contrast(@accent, @text, @surface); } .c { color: mix(@accent, @surface, 12%); } " +
			":root { --bs-primary-rgb: red(@accent), green(@accent), blue(@accent); }");
		assertTrue(css.contains(".b { color: #ffffff; }"), css);
		assertTrue(css.contains(".c { color: #e5ecfd; }"), css);
		assertTrue(css.contains("--bs-primary-rgb: 37, 99, 235;"), css);
	}

	@Test
	public void toneMixinWithInterpolatedSelector()
	{
		String css = compile(".svy-tone(@name, @hue) { .svy-tone-@{name} { --svy-tone-solid: @hue; --svy-tone-on-solid: contrast(@hue, #111827, #FFFFFF); } } " +
			".svy-tone(attention, #C2410C);");
		assertTrue(css.contains(".svy-tone-attention { --svy-tone-solid: #C2410C; --svy-tone-on-solid: #ffffff; }"), css);
	}

	@Test
	public void everyCurrentAndFutureVersionResolvesToTheLatestNg2Theme()
	{
		String latest = ThemeResourceLoader.getTheme("2023.3.0_ng2");
		for (String v : new String[] { "latest", "latest_ng2", "2025.3.0", "2026.12.0", "2026.12.0_ng2", "2026.12.0_semantic" })
		{
			assertEquals(latest, ThemeResourceLoader.getTheme(v), v);
		}
		assertEquals("2023.3.0_ng2", ThemeResourceLoader.getLatestNG2Version());
	}
}
