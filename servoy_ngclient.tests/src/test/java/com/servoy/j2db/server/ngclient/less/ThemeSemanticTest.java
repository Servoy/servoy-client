package com.servoy.j2db.server.ngclient.less;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;

/**
 * SVY-21639: the opt-in semantic-core theme (servoy_theme.less?version=semantic).
 * The colour roles are CSS custom properties computed with color-mix() at runtime; this test evaluates them the way a browser
 * does (sRGB mix) to check the WCAG contrast of every text/background pair, for the light and the dark variant.
 */
@SuppressWarnings("nls")
public class ThemeSemanticTest
{
	private static final Pattern BLOCK = Pattern.compile("([^{}]+)\\{([^{}]*)\\}");
	private static final Pattern COLOUR_LITERAL = Pattern.compile("#[0-9a-fA-F]{3,8}\\b|\\brgba?\\(");

	static String compile(String solutionLess)
	{
		return JLessCompiler.compileLess("@import 'servoy_theme.less?version=semantic';\n" + solutionLess, null, "solution.less");
	}

	/** declarations of all blocks whose selector is exactly one of the given selectors, later blocks overriding earlier ones */
	static Map<String, String> declarations(String css, String... selectors)
	{
		List<String> wanted = Arrays.asList(selectors);
		Map<String, String> result = new LinkedHashMap<>();
		Matcher m = BLOCK.matcher(css.replaceAll("(?s)/\\*.*?\\*/", ""));
		while (m.find())
		{
			boolean match = false;
			for (String part : m.group(1).split(","))
				if (wanted.contains(part.trim())) match = true;
			if (!match) continue;
			for (String decl : m.group(2).split(";"))
			{
				int colon = decl.indexOf(':');
				if (colon > 0) result.put(decl.substring(0, colon).trim(), decl.substring(colon + 1).trim());
			}
		}
		return result;
	}

	/** evaluates a CSS colour value made of hex literals, var() and color-mix(in srgb, ...) to sRGB 0..255 */
	static double[] colour(String value, Map<String, String> props)
	{
		String v = value.trim();
		if (v.startsWith("var("))
		{
			String inner = v.substring(4, v.length() - 1).trim();
			String name = splitTopLevel(inner).get(0).trim();
			String resolved = props.get(name);
			if (resolved == null) fail("undefined custom property " + name);
			return colour(resolved, props);
		}
		if (v.startsWith("color-mix("))
		{
			List<String> args = splitTopLevel(v.substring("color-mix(".length(), v.length() - 1));
			assertEquals("in srgb", args.get(0).trim(), v);
			String[] a = colourAndPercent(args.get(1));
			String[] b = colourAndPercent(args.get(2));
			double pa = a[1] != null ? Double.parseDouble(a[1]) : (b[1] != null ? 100 - Double.parseDouble(b[1]) : 50);
			double[] ca = colour(a[0], props), cb = colour(b[0], props);
			double[] r = new double[3];
			for (int i = 0; i < 3; i++)
				r[i] = ca[i] * pa / 100 + cb[i] * (100 - pa) / 100;
			return r;
		}
		if (v.startsWith("#"))
		{
			String hex = v.substring(1);
			if (hex.length() == 3) hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
			return new double[] { Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(
				hex.substring(4, 6), 16) };
		}
		fail("cannot evaluate colour " + v);
		return null;
	}

	private static String[] colourAndPercent(String arg)
	{
		String a = arg.trim();
		Matcher m = Pattern.compile("^(.*\\S)\\s+([0-9.]+)%$").matcher(a);
		return m.matches() ? new String[] { m.group(1), m.group(2) } : new String[] { a, null };
	}

	private static List<String> splitTopLevel(String s)
	{
		List<String> parts = new ArrayList<>();
		int depth = 0, start = 0;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '(') depth++;
			else if (c == ')') depth--;
			else if (c == ',' && depth == 0)
			{
				parts.add(s.substring(start, i));
				start = i + 1;
			}
		}
		parts.add(s.substring(start));
		return parts;
	}

	static double luminance(double[] c)
	{
		double[] lin = new double[3];
		for (int i = 0; i < 3; i++)
		{
			double s = Math.round(c[i]) / 255.0;
			lin[i] = s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2];
	}

	static double contrast(String fg, String bg, Map<String, String> props)
	{
		double l1 = luminance(colour("var(" + fg + ")", props)), l2 = luminance(colour("var(" + bg + ")", props));
		return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
	}

	static void assertContrastPairs(Map<String, String> props, String variant)
	{
		Object[][] pairs = { //
			{ "--svy-text", "--svy-surface", 4.5 }, //
			{ "--svy-text", "--svy-surface-raised", 4.5 }, //
			{ "--svy-text-muted", "--svy-surface", 4.5 }, //
			{ "--svy-text-muted", "--svy-surface-sunken", 4.5 }, //
			{ "--svy-border-strong", "--svy-surface", 3.0 }, //
			{ "--svy-focus-ring", "--svy-surface", 3.0 }, //
			{ "--svy-accent", "--svy-surface", 4.5 }, //
			{ "--svy-accent-contrast", "--svy-accent", 4.5 }, //
			{ "--svy-accent-contrast", "--svy-accent-hover", 4.5 }, //
			{ "--svy-accent-contrast", "--svy-accent-pressed", 4.5 }, //
			{ "--svy-text", "--svy-accent-subtle", 4.5 }, //
			{ "--svy-chrome-text", "--svy-chrome", 4.5 }, //
			{ "--svy-chrome-text", "--svy-chrome-hover", 4.5 }, //
			{ "--svy-chrome-text", "--svy-chrome-active", 4.5 }, //
			{ "--svy-chrome-text-muted", "--svy-chrome", 4.5 }, //
			{ "--svy-chrome-text-muted", "--svy-chrome-hover", 4.5 } };
		List<String> failures = new ArrayList<>();
		for (Object[] pair : pairs)
		{
			double ratio = contrast((String)pair[0], (String)pair[1], props);
			if (ratio < (Double)pair[2]) failures.add(pair[0] + " on " + pair[1] + " = " + String.format("%.2f", ratio));
		}
		for (String tone : new String[] { "danger", "warning", "success", "info", "attention", "neutral" })
		{
			double solid = contrast("--svy-" + tone + "-on-solid", "--svy-" + tone + "-solid", props);
			if (solid < 4.5) failures.add(tone + " on-solid = " + String.format("%.2f", solid));
			double soft = contrast("--svy-" + tone + "-soft-fg", "--svy-" + tone + "-soft-bg", props);
			if (soft < 4.5) failures.add(tone + " soft = " + String.format("%.2f", soft));
		}
		assertTrue(failures.isEmpty(), variant + ": " + failures);
	}

	@Nested
	class Versioning
	{
		@Test
		public void semanticKeyResolvesToTheSemanticFiles()
		{
			assertTrue(ThemeResourceLoader.getTheme(ThemeResourceLoader.SEMANTIC).startsWith("/* VERSION: semantic */"));
			assertTrue(ThemeResourceLoader.getThemeProperties(ThemeResourceLoader.SEMANTIC).startsWith("/* VERSION: semantic */"));
			assertEquals(ThemeResourceLoader.getTheme(ThemeResourceLoader.SEMANTIC), ThemeResourceLoader.getTheme("semantic_ng2"),
				"the theme editor appends _ng2 when the Titanium flag is set");
		}

		@Test
		public void latestAndUnversionedImportsAreUnchanged()
		{
			assertEquals("2023.3.0", ThemeResourceLoader.getLatestVersion());
			assertEquals("2023.3.0_ng2", ThemeResourceLoader.getLatestNG2Version());
			String ng2 = ThemeResourceLoader.getTheme("2023.3.0_ng2");
			assertEquals(ng2, ThemeResourceLoader.getLatestTheme());
			assertEquals(ng2, ThemeResourceLoader.getTheme("2026.12.0"));
			assertEquals(ThemeResourceLoader.getThemeProperties("2023.3.0_ng2"), ThemeResourceLoader.getLatestThemeProperties());
		}

		@Test
		public void semanticIsOfferedButNotLatest()
		{
			assertTrue(Arrays.asList(ThemeResourceLoader.VERSIONS).contains(ThemeResourceLoader.SEMANTIC));
			assertEquals("latest", ThemeResourceLoader.VERSIONS[0]);
		}
	}

	@Nested
	class Compilation
	{
		@Test
		public void compilesWithTheDefaultCompiler()
		{
			String css = compile("");
			assertTrue(css.contains("--svy-accent: #2563EB"), css);
			assertTrue(css.contains("--svy-accent-subtle: color-mix(in srgb, var(--svy-accent) 12%, var(--svy-surface))"), css);
			assertTrue(css.contains(".svy-theme-dark"), css);
			assertTrue(css.contains(".svy-tone-attention"), css);
		}

		@Test
		public void propertiesFileEmitsNoCss()
		{
			String css = JLessCompiler.compileLess("@import 'servoy_theme_properties.less?version=semantic';", null, "form.less");
			assertFalse(css.replaceAll("(?s)/\\*.*?\\*/", "").contains("{"), css);
		}

		@Test
		public void rulesContainNoColourLiterals()
		{
			Matcher m = BLOCK.matcher(compile("").replaceAll("(?s)/\\*.*?\\*/", ""));
			List<String> offending = new ArrayList<>();
			while (m.find())
			{
				String selector = m.group(1).trim();
				// role blocks: the seed values are the only literals, emitted wherever a variant scope is applied
				if (selector.equals(":root") || selector.equals(".svy-theme-light") || selector.equals(".svy-theme-dark") ||
					selector.equals(".bts-extra-navbar.navbar-dark")) continue;
				if (COLOUR_LITERAL.matcher(m.group(2)).find()) offending.add(selector);
			}
			assertTrue(offending.isEmpty(), "colour literals outside the role blocks: " + offending);
		}

		@Test
		public void everyReferencedRoleIsDefined()
		{
			String css = compile("");
			List<String> defined = new ArrayList<>();
			Matcher d = Pattern.compile("(--svy-[\\w-]+)\\s*:").matcher(css);
			while (d.find())
				defined.add(d.group(1));
			// var(--svy-x, fallback) is an optional hook (only set when a solution overrides a legacy variable); a bare var(--svy-x) must be defined
			Matcher m = Pattern.compile("var\\((--svy-[\\w-]+)\\)").matcher(css);
			List<String> missing = new ArrayList<>();
			while (m.find())
				if (!defined.contains(m.group(1))) missing.add(m.group(1));
			assertTrue(missing.isEmpty(), "undefined roles: " + missing);
		}
	}

	@Nested
	class Contrast
	{
		@Test
		public void lightDefaultsMeetWcag()
		{
			assertContrastPairs(declarations(compile(""), ":root"), "light");
		}

		@Test
		public void darkDefaultsMeetWcag()
		{
			assertContrastPairs(declarations(compile(""), ":root", ".svy-theme-dark"), "dark (scoped class)");
			assertContrastPairs(declarations(compile("@variant: dark;"), ":root"), "dark (variant seed)");
		}

		@Test
		public void lightDefaultsMatchTheProposal()
		{
			Map<String, String> root = declarations(compile(""), ":root");
			assertEquals(hex("#E5ECFD"), hex(root, "--svy-accent-subtle"));
			assertEquals(hex("#5E656F"), hex(root, "--svy-text-muted"));
			assertEquals(hex("#848991"), hex(root, "--svy-border-strong"));
			assertEquals("#FFFFFF", root.get("--svy-accent-contrast"));
			assertEquals("#1F2937", root.get("--svy-chrome-text"));
		}

		private String hex(String h)
		{
			return h.toUpperCase();
		}

		private String hex(Map<String, String> props, String name)
		{
			double[] c = colour("var(" + name + ")", props);
			return String.format("#%02X%02X%02X", Math.round(c[0]), Math.round(c[1]), Math.round(c[2]));
		}
	}

	@Nested
	class LegacyCompatibility
	{
		@Test
		public void everyLegacyVariableIsDeclared()
		{
			Matcher m = Pattern.compile("(?m)^@([\\w-]+)\\s*:").matcher(ThemeResourceLoader.getThemeProperties("2023.3.0_ng2"));
			String semantic = ThemeResourceLoader.getThemeProperties(ThemeResourceLoader.SEMANTIC);
			List<String> missing = new ArrayList<>();
			while (m.find())
				if (!Pattern.compile("(?m)^@" + Pattern.quote(m.group(1)) + "\\s*:").matcher(semantic).find()) missing.add(m.group(1));
			assertTrue(missing.isEmpty(), "legacy variables missing from the semantic theme: " + missing);
		}

		@Test
		public void formLessUsingLegacyVariablesCompiles()
		{
			String css = JLessCompiler.compileLess("@import 'servoy_theme_properties.less?version=semantic';\n" +
				".my-form .title { color: @main-color-dark; background: @body-bg-inverse; border: @border-width @border-style @border-color; " +
				"padding: @box-size-10; font-size: @font-size-h3; }\n.my-form .x { color: fade(@brand-danger, 50%); }", null, "form.less");
			assertTrue(css.contains(".my-form .title"), css);
			assertFalse(css.contains("undefined"), css);
		}

		@Test
		public void legacyColourOverridesReBrandTheNewTheme()
		{
			Map<String, String> root = declarations(compile("@main-color: #CC0000; @secondary-color: #18222C; @text-color: #333333; @brand-danger: #990000;"),
				":root");
			assertEquals("#CC0000", root.get("--svy-accent"));
			assertEquals("#18222C", root.get("--svy-chrome"));
			assertEquals("#333333", root.get("--svy-text"));
			assertEquals("#990000", root.get("--svy-danger"));
			assertTrue(contrast("--svy-chrome-text", "--svy-chrome", root) >= 4.5, "dark legacy chrome must get light text");
		}

		/** 2023 draws the selected sidenav border only through @sidenav-selected-border-width; a colour override alone shows nothing */
		@Test
		public void legacySidenavIndicatorFollowsTheOldBorderWidth()
		{
			Map<String, String> colourOnly = declarations(compile("@sidenav-selected-border-color: #F64F07;"), ".svy-sidenav");
			assertEquals("0px", colourOnly.get("--svy-sidenav-indicator-width"), "colour alone must not draw an indicator");
			Map<String, String> withWidth = declarations(compile("@sidenav-selected-border-width: 0px 0px 0px 4px;"), ".svy-sidenav");
			assertEquals("4px", withWidth.get("--svy-sidenav-indicator-width"));
			Map<String, String> untouched = declarations(compile(""), ".svy-sidenav");
			assertEquals("0px", untouched.get("--svy-sidenav-indicator-width"), "no selected indicator by default, in light and dark");
		}

		/** solution .less written for 2023 uses @main-color, @text-color, @body-bg...; in the dark variant they must be the dark seeds */
		@Test
		public void legacyNamesFollowTheDarkVariant()
		{
			String css = compile("@variant: dark; .probe { a: @main-color; t: @text-color; b: @body-bg; c: @secondary-color; }");
			Map<String, String> probe = declarations(css, ".probe");
			assertEquals("#60A5FA", probe.get("a").toUpperCase());
			assertEquals("#E5E7EB", probe.get("t").toUpperCase());
			assertEquals("#111827", probe.get("b").toUpperCase());
			assertEquals("#0B1220", probe.get("c").toUpperCase());
			Map<String, String> root = declarations(css, ":root");
			assertEquals("#111827", root.get("--svy-surface").toUpperCase());
			Map<String, String> branded = declarations(compile("@variant: dark; @main-color: #FFBA3F;"), ":root");
			assertEquals("#FFBA3F", branded.get("--svy-accent").toUpperCase(), "a legacy override re-brands the active (dark) variant");
		}

		/**
		 * a solution may set any legacy variable to a runtime value (sqwad: @body-bg: var(--bs-body-bg)); jlessc replaces the whole
		 * stylesheet with the error message when a colour function gets one, so every variable must compile with var() and no colour
		 * function may be left in the CSS with a var() argument
		 */
		@Test
		public void everyLegacyVariableAcceptsARuntimeValue()
		{
			Matcher m = Pattern.compile("(?m)^@([\\w-]+)\\s*:").matcher(ThemeResourceLoader.getThemeProperties(ThemeResourceLoader.SEMANTIC));
			Pattern leftover = Pattern.compile("\\b(lighten|darken|mix|contrast|luma|fade|fadein|fadeout|spin|saturate|desaturate|tint|shade)\\(var\\(");
			List<String> failures = new java.util.ArrayList<>();
			int checked = 0;
			while (m.find())
			{
				String name = m.group(1);
				if (name.startsWith("svy-") || name.equals("variant")) continue;
				checked++;
				String css = compile("@" + name + ": var(--probe);");
				if (css == null || !css.contains("{")) failures.add(name + " does not compile: " + css);
				else if (leftover.matcher(css).find()) failures.add(name + " leaves a colour function on var() in the CSS");
			}
			assertTrue(failures.isEmpty(), String.join("\n", failures));
			assertTrue(checked > 150, "checked " + checked + " variables");
		}

		@Test
		public void runtimeBodyBgDoesNotFormABootstrapCycle()
		{
			String css = compile(":root { --bs-body-bg: #F6F6F6; } @body-bg: var(--bs-body-bg);");
			Map<String, String> root = declarations(css, ":root");
			assertEquals("var(--bs-body-bg)", root.get("--svy-surface"));
			assertEquals("#F6F6F6", root.get("--bs-body-bg"), "the solution's --bs-body-bg must win, not var(--svy-surface)");
		}

		/** svyCloud styles tabs as an underline through @tab-*; Bootstrap's boxed tab must not show around it */
		@Test
		public void legacyTabOverridesBringBackThe2023TabGeometry()
		{
			String sel = ".bts-tabpanel > .nav-tabs > li > a";
			Map<String, String> plain = declarations(compile(""), sel);
			assertTrue(plain.isEmpty(), "no 2023 tab geometry without legacy tab overrides: " + plain);
			String css = compile("@tab-active-border-color: #FFBA3F; @tab-active-border-width: 0 0 3px 0;");
			Map<String, String> link = declarations(css, sel);
			assertEquals("0", link.get("border-width"));
			assertEquals("0", link.get("border-radius"));
			Map<String, String> active = declarations(css, ".bts-tabpanel > .nav-tabs > li > .nav-link.active");
			assertEquals("0 0 3px 0", active.get("border-width"));
			assertEquals("#FFBA3F", active.get("border-color"));
			assertEquals("0px", declarations(css, ".bts-tabpanel > .nav-tabs").get("--svy-tab-indicator-width"));
		}

		@Test
		public void legacyFontAndRadiusOverridesReachTheRoles()
		{
			Map<String, String> root = declarations(compile("@font-family: Inter, sans-serif; @text-font-size: 13px; @border-radius: 2px;"), ":root");
			assertEquals("Inter, sans-serif", root.get("--svy-font-family"));
			assertEquals("13px", root.get("--svy-font-size"));
			assertEquals("2px", root.get("--svy-radius"));
		}

		@Test
		public void legacyComponentOverridesApplyOnlyWhenSet()
		{
			Map<String, String> plain = declarations(compile(""), ".svy-sidenav");
			assertFalse(plain.containsKey("--svy-sidenav-bg") && plain.get("--svy-sidenav-bg").startsWith("#"), "no legacy value by default");
			Map<String, String> sidenav = declarations(compile("@sidenav-color: #18222C; @sidenav-selected-color: #464E56;"), ".svy-sidenav");
			assertEquals("#18222C", sidenav.get("--svy-sidenav-bg"));
			assertEquals("#464E56", sidenav.get("--svy-sidenav-selected-bg"));
			assertEquals("#464E56", sidenav.get("--svy-sidenav-hover-bg"), "legacy used one colour for selected and hover");
			Map<String, String> grid = declarations(compile("@table-row-selected-bg-color: #FFE0B2;"), ":root");
			assertEquals("#FFE0B2", grid.get("--svy-table-row-selected-bg"));
		}

		/** solutions call old theme classes as LESS mixins; jlessc aborts the whole stylesheet on the first undefined one */
		@Test
		public void everyStandaloneClassOfTheOldThemeCanStillBeUsedAsMixin()
		{
			String old = ThemeResourceLoader.getTheme("2023.3.0_ng2").replaceAll("(?s)/\\*.*?\\*/", "");
			Matcher m = Pattern.compile("([^{}]+)\\{").matcher(old);
			java.util.Set<String> classes = new java.util.TreeSet<>();
			while (m.find())
				for (String part : m.group(1).split(","))
				{
					Matcher c = Pattern.compile("^\\s*\\.([a-zA-Z][\\w-]*)\\s*(\\([^)]*\\))?\\s*(when.*)?$").matcher(part);
					if (c.matches()) classes.add(c.group(1));
				}
			StringBuilder less = new StringBuilder(".probe {");
			for (String cls : classes)
				less.append(" .").append(cls).append("();");
			less.append(" }");
			String css = compile(less.toString());
			assertFalse(css.contains("Undefined mixin"), css.length() > 300 ? css.substring(0, 300) : css);
			assertTrue(css.contains(".probe"), "probe rule must compile");
			assertTrue(classes.size() > 150, "parsed " + classes.size() + " old classes");
		}

		@Test
		public void legacyUtilityClassesExist()
		{
			String css = compile("");
			for (String cls : new String[] { ".margin-10", ".padding-top-20", ".margin-left-15", ".no-margin", ".full-size", ".clickable", ".btn-round",
				".btn-tertiary", ".text-tertiary", ".bg-main", ".border-default", ".form-primary", ".label_header_0", ".header-bar", ".box",
				".simple-collapsible", ".check-spacing-10", ".inactive" })
			{
				assertTrue(css.contains(cls), cls + " missing");
			}
			assertTrue(declarations(css, ".margin-10").get("margin").equals("10px"));
			assertTrue(declarations(compile("@box-size-10: 8px;"), ".margin-10").get("margin").equals("8px"), "box sizes stay overridable");
		}

		@Test
		public void legacyWindowDialogNavbarOverridesApplyOnlyWhenSet()
		{
			String plain = compile("");
			assertFalse(declarations(plain, ":root").containsKey("--svy-window-header-bg"));
			assertFalse(declarations(plain, ".bts-extra-navbar.bts-navbar-default").containsKey("--svy-navbar-bg") &&
				declarations(plain, ".bts-extra-navbar.bts-navbar-default").get("--svy-navbar-bg").startsWith("#"));

			Map<String, String> root = declarations(compile("@window-header-bg: #428bca; @dialog-info-header-bg: #5bc0de;"), ":root");
			assertEquals("#428bca", root.get("--svy-window-header-bg"));
			assertEquals("#1F2937", root.get("--svy-window-header-fg").toUpperCase(), "the text is chosen by contrast for the legacy background");
			assertEquals("#5bc0de", root.get("--svy-dialog-info-bg"));

			Map<String, String> navbar = declarations(compile("@navbar-bg: #E9720B;"), ".bts-extra-navbar.bts-navbar-default");
			assertEquals("#E9720B", navbar.get("--svy-navbar-bg"));
			assertEquals("#1F2937", navbar.get("--svy-navbar-fg").toUpperCase());
		}

		@Test
		public void deadLegacySectionsAreNotCarriedOver()
		{
			String css = compile("").replaceAll("(?s)/\\*.*?\\*/", "");
			for (String dead : new String[] { "ui-grid", "bootbox", "yuimenu", "bootstrap-datetimepicker", "ui-select", "containerform-" })
				assertFalse(css.contains(dead), dead);
		}
	}

	@Nested
	class Seeds
	{
		@Test
		public void overridingASeedInTheSolutionReachesTheRoles()
		{
			String css = compile("@accent: #FACC15;");
			Map<String, String> root = declarations(css, ":root");
			assertEquals("#FACC15", root.get("--svy-accent"));
			assertEquals("#1F2937", root.get("--svy-accent-contrast"), "a light accent must get dark text");
			assertTrue(css.contains("--bs-primary-rgb: 250, 204, 21"), css);
		}

		@Test
		public void variantSeedSwitchesTheRootToTheDarkSet()
		{
			Map<String, String> root = declarations(compile("@variant: dark;"), ":root");
			assertEquals("#111827", root.get("--svy-surface"));
			assertEquals("dark", root.get("color-scheme"));
		}

		@Test
		public void inverseNavbarTakesTheOppositeSet()
		{
			assertEquals("#111827", declarations(compile(""), ".bts-extra-navbar.navbar-dark").get("--svy-surface"), "dark in a light app");
			assertEquals("#FFFFFF", declarations(compile("@variant: dark;"), ".bts-extra-navbar.navbar-dark").get("--svy-surface"),
				"light in a dark app");
		}

		@Test
		public void darkChromeGetsLightText()
		{
			Map<String, String> root = declarations(compile("@chrome: #0F172A;"), ":root");
			assertNotEquals("#1F2937", root.get("--svy-chrome-text"));
			assertTrue(contrast("--svy-chrome-text", "--svy-chrome", root) >= 4.5);
		}
	}
}
