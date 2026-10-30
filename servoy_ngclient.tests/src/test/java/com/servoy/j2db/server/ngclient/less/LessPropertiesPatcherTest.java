package com.servoy.j2db.server.ngclient.less;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.servoy.j2db.server.ngclient.less.resources.LessPropertiesPatcher;
import com.servoy.j2db.server.ngclient.less.resources.LessPropertiesPatcher.Edit;

/**
 * SVY-21639: the theme editor saves in place, so variables it does not know, comments and rules survive a save
 */
@SuppressWarnings("nls")
public class LessPropertiesPatcherTest
{
	/** svyCloud's custom_servoy_theme_properties.less shape: the old 2023 overrides are unknown to the semantic editor */
	static final String SVYCLOUD = "// this file is for over writing the theme properties of the default servoy properties file.\n" +
		"@import 'servoy_theme.less?version=semantic';\n" +
		"\n" +
		"\n" +
		"/* START General */\n" +
		"\n" +
		"@main-color: #122D40; // default:#E9720B\n" +
		"\n" +
		"@secondary-color: #FFBA3F; // default:#18222C\n" +
		"\n" +
		"@font-family: 'Outfit', sans-serif; // default:\"Helvetica Neue\", Helvetica, Arial, sans-serif\n" +
		"\n" +
		"/* START Sidenav */\n" +
		"\n" +
		"@sidenav-color: @main-color; // default:@secondary-color\n" +
		"\n" +
		"@sidenav-text-color: #FFF; // default:@secondary-color-inverse\n" +
		"\n" +
		"/* START Tabs */\n" +
		"\n" +
		"@tab-active-border-width: 3; // default:0\n";

	@Test
	public void editingOneSeedKeepsEveryOtherLineIdentical()
	{
		String out = LessPropertiesPatcher.apply(SVYCLOUD, List.of(new Edit("accent", "#FF0000", "#2563EB", "Seeds")));
		assertTrue(out.startsWith(SVYCLOUD.substring(0, SVYCLOUD.length() - 1)), "existing text must be kept as is:\n" + out);
		assertEquals(SVYCLOUD + "\n/* START Seeds */\n\n@accent: #FF0000; // default:#2563EB\n", out);
		String back = LessPropertiesPatcher.apply(out, List.of(new Edit("accent", null, "#2563EB", "Seeds")));
		assertTrue(back.startsWith(SVYCLOUD), "resetting the seed keeps all old lines:\n" + back);
		assertTrue(!back.contains("@accent"), back);
	}

	@Test
	public void changingAnExistingValueOnlyReplacesTheValue()
	{
		String out = LessPropertiesPatcher.apply(SVYCLOUD, List.of(new Edit("main-color", "#000000", "#E9720B", "General")));
		assertEquals(SVYCLOUD.replace("@main-color: #122D40; // default:#E9720B", "@main-color: #000000; // default:#E9720B"), out);
	}

	@Test
	public void resettingToTheDefaultRemovesTheDeclarationOnly()
	{
		String out = LessPropertiesPatcher.apply(SVYCLOUD, List.of(new Edit("sidenav-text-color", null, null, "Sidenav")));
		assertEquals(SVYCLOUD.replace("@sidenav-text-color: #FFF; // default:@secondary-color-inverse\n\n", ""), out);
	}

	@Test
	public void newPropertyGoesIntoItsExistingCategory()
	{
		String out = LessPropertiesPatcher.apply(SVYCLOUD, List.of(new Edit("sidenav-selected-color", "#123456", "@secondary-color-light", "Sidenav")));
		assertTrue(out.contains("@sidenav-text-color: #FFF; // default:@secondary-color-inverse\n\n@sidenav-selected-color: #123456; // default:@secondary-color-light\n"),
			out);
		assertTrue(out.endsWith("@tab-active-border-width: 3; // default:0\n"), out);
	}

	@Test
	public void newPropertyGoesBeforeTheEndMarkerOfANewStyleFile()
	{
		String props = "@import 'servoy_theme_properties.less?version=semantic';\n\n// START own custom properties (keep this line in the file)\n" +
			"@my-brand: #abcdef;\n// END own custom properties\n.mixin() { @accent: red; }\n";
		String out = LessPropertiesPatcher.apply(props, List.of(new Edit("accent", "#FF0000", "#2563EB", "Seeds")));
		assertTrue(out.contains("@my-brand: #abcdef;\n/* START Seeds */\n\n@accent: #FF0000; // default:#2563EB\n\n// END own custom properties\n.mixin() { @accent: red; }\n"),
			out);
	}

	@Test
	public void declarationsInsideRulesAreNotTouched()
	{
		String less = "@import 'servoy_theme_properties.less?version=semantic';\n.x {\n@accent: red;\n}\n@accent: #111111;\n";
		assertEquals(less.replace("@accent: #111111;", "@accent: #222222;"),
			LessPropertiesPatcher.apply(less, List.of(new Edit("accent", "#222222", "#2563EB", "Seeds"))));
	}

	@Test
	public void theLastTopLevelDeclarationIsPatched()
	{
		String less = "@accent: #111111;\n// later\n@accent: #333333;\n";
		assertEquals("@accent: #111111;\n// later\n@accent: #444444;\n", LessPropertiesPatcher.apply(less, List.of(new Edit("accent", "#444444", null, "Seeds"))));
	}

	@Test
	public void windowsLineEndingsArePreserved()
	{
		String crlf = SVYCLOUD.replace("\n", "\r\n");
		String out = LessPropertiesPatcher.apply(crlf, List.of(new Edit("accent", "#FF0000", "#2563EB", "Seeds"),
			new Edit("main-color", "#000000", "#E9720B", "General")));
		assertTrue(!out.replace("\r\n", "").contains("\n"), "only CRLF line ends");
		assertTrue(out.contains("@main-color: #000000; // default:#E9720B\r\n"), out);
		assertTrue(out.contains("/* START Seeds */\r\n\r\n@accent: #FF0000; // default:#2563EB"), out);
	}
}
