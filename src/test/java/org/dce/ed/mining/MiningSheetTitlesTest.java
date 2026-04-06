package org.dce.ed.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MiningSheetTitlesTest {

    @Test
    void sheetTitleForCommander_usesCmdrPrefix() {
        assertEquals("CMDR -", MiningSheetTitles.sheetTitleForCommander(null));
        assertEquals("CMDR -", MiningSheetTitles.sheetTitleForCommander("   "));
        assertEquals("CMDR Villunus", MiningSheetTitles.sheetTitleForCommander("Villunus"));
    }

    @Test
    void isCmdrMiningWorksheet_recognizesPrefixCaseInsensitive() {
        assertTrue(MiningSheetTitles.isCmdrMiningWorksheet("CMDR Villunus"));
        assertTrue(MiningSheetTitles.isCmdrMiningWorksheet("  cmdr UkeBard  "));
        assertFalse(MiningSheetTitles.isCmdrMiningWorksheet("Sheet1"));
        assertFalse(MiningSheetTitles.isCmdrMiningWorksheet("Prospector log"));
        assertFalse(MiningSheetTitles.isCmdrMiningWorksheet("Villunus"));
    }

    @Test
    void commanderNameFromCmdrWorksheetTitle_stripsPrefix() {
        assertEquals("Villunus", MiningSheetTitles.commanderNameFromCmdrWorksheetTitle("CMDR Villunus"));
        assertEquals("Uke Bard", MiningSheetTitles.commanderNameFromCmdrWorksheetTitle("cmdr Uke Bard"));
        assertEquals("", MiningSheetTitles.commanderNameFromCmdrWorksheetTitle("Sheet1"));
    }

    @Test
    void sanitizeTitle_replacesInvalidCharacters() {
        assertEquals("a_b_c", MiningSheetTitles.sanitizeTitle("a:b*c"));
        assertEquals("x_y", MiningSheetTitles.sanitizeTitle("x\\y"));
        assertEquals("q_r", MiningSheetTitles.sanitizeTitle("q?r"));
    }

    @Test
    void sanitizeTitle_collapsesWhitespace() {
        assertEquals("Cmdr Name", MiningSheetTitles.sanitizeTitle("Cmdr   Name"));
    }

    @Test
    void uniqueTitle_addsNumericSuffixOnCollision() {
        Set<String> used = new LinkedHashSet<>();
        used.add("CMDR Villunus");
        assertEquals("CMDR Villunus (2)", MiningSheetTitles.uniqueTitle("CMDR Villunus", used));
    }

    @Test
    void quoteSheetNameForRange_escapesSingleQuotes() {
        assertEquals("'It''s fine'!A:O", MiningSheetTitles.rangeA1O("It's fine"));
    }

    @Test
    void rangeA1O_prefixesQuotedName() {
        assertTrue(MiningSheetTitles.rangeA1O("Sheet1").startsWith("'"));
        assertTrue(MiningSheetTitles.rangeA1O("Sheet1").endsWith("!A:O"));
    }
}
