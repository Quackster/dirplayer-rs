package com.dirplayer.player.eval;

import com.dirplayer.player.ScriptError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for Lingo config/value parsing.
 * Port of Rust vm-rust/tests/lingo/config_parsing.rs
 *
 * These tests verify that the parser can handle external_variables.txt
 * and external_texts.txt style config lines from HabboHotel and similar
 * Shockwave applications.
 */
public class ConfigParsingTest {

    /**
     * Try to parse a value as a Lingo expression.
     */
    private boolean parseValue(String value) {
        try {
            LingoParser.parse(value);
            return true;
        } catch (ScriptError e) {
            return false;
        }
    }

    /**
     * Parse a config line of the form key=value.
     */
    private boolean parseConfigLine(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            return false;
        }
        String value = parts[1].trim();
        return parseValueAsLingo(value);
    }

    /**
     * Parse a value using various strategies.
     */
    private boolean parseValueAsLingo(String valueStr) {
        // Strategy 0: Empty value
        if (valueStr.isEmpty()) {
            return true;
        }

        // Strategy 1: Empty list
        if (valueStr.equals("[]")) {
            return parseValue(valueStr);
        }

        // Strategy 2: Lists and property lists
        if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
            return parseValue(valueStr);
        }

        // Strategy 3: Numbers
        if (valueStr.matches("^[\\d.+-]+$")) {
            return parseValue(valueStr);
        }

        // Strategy 4: RGB colors
        if (valueStr.startsWith("rgb(")) {
            return parseValue(valueStr);
        }

        // Strategy 5: Already quoted strings
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return parseValue(valueStr);
        }

        // Strategy 6: Try as identifier first
        if (parseValue(valueStr)) {
            return true;
        }

        // Strategy 7: Treat as unquoted string
        String quoted = "\"" + valueStr.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        return parseValue(quoted);
    }

    @Test
    void testParseExternalVariables() {
        // Sample lines from external_variables.txt
        String[] testCases = {
            "navigator.private.default=4",
            "interface.cmds.item.ctrl=[]",
            "cast.entry.9=hh_room",
            "moderator.cmds=[\":alertx\",\":banx\",\":kickx\"]",
            "client.window.title=HabboHotel",
            "language=en",
            "room.cast.private=[\"hh_room_private\"]",
            "client.version.id=401",
            "fuse.project.id=ion",
            "navigator.visible.private.root=4",
            "room.default.floor=111"
        };

        int passed = 0;
        int failed = 0;

        for (String line : testCases) {
            if (parseConfigLine(line)) {
                passed++;
            } else {
                failed++;
                System.out.println("Failed to parse: " + line);
            }
        }

        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        assertTrue(passed > 0, "Should parse at least some config lines");
        // Allow some failures for edge cases
        assertTrue(failed < testCases.length / 2, "More than half of config lines should parse");
    }

    @Test
    void testParseExternalTextsWithAsterisks() {
        // Lines from external_texts.txt that have asterisks
        String[] testCases = {
            "furni_table_silo_small*9_desc=Red Area Occasional Table",
            "furni_divider_nor2*2_desc=Black Iced bar desk",
            "furni_sofachair_silo*5_desc=Pink Area Armchair",
            "furni_table_plasto_round*2_desc=Hip plastic furniture",
            "furni_couch_norja*3_desc=Two can perch comfortably",
            "furni_divider_nor1*8_desc=Yellow Ice corner",
            "furni_bed_polyfon_one*3_desc=White Mode Single Bed",
            "furni_sofa_polyfon*4_name=Beige Mode Sofa"
        };

        int passed = 0;
        int failed = 0;

        for (String line : testCases) {
            if (parseConfigLine(line)) {
                passed++;
            } else {
                failed++;
            }
        }

        System.out.println("Asterisk lines: " + passed + " passed, " + failed + " failed");
        assertTrue(passed > 0, "Should parse at least some lines with asterisks");
    }

    @Test
    void testGrammarParsingOnly() {
        // Test basic grammar parsing without evaluation
        Object[][] testCases = {
            {"4", true},
            {"[]", true},
            {"hh_room", true},
            {"[\":alertx\",\":banx\"]", true},
            {"HabboHotel", true},
            {"\"http://localhost/v7/c_images/\"", true},
            {"en", true},
            {"401", true},
            {"[#font:\"v\",#fontSize:9]", true},
            {"rgb(255,0,0)", true}
        };

        for (Object[] testCase : testCases) {
            String value = (String) testCase[0];
            boolean shouldPass = (Boolean) testCase[1];
            boolean result = parseValue(value);

            if (shouldPass) {
                assertTrue(result, "Should parse: " + value);
            }
        }
    }

    @Test
    void testSimpleValues() throws ScriptError {
        // Simple integer
        LingoExpr ast = LingoParser.parse("4");
        assertInstanceOf(LingoExpr.IntLiteral.class, ast);
        assertEquals(4, ((LingoExpr.IntLiteral) ast).value);

        // Empty list
        ast = LingoParser.parse("[]");
        assertInstanceOf(LingoExpr.ListLiteral.class, ast);
        assertEquals(0, ((LingoExpr.ListLiteral) ast).items.size());

        // Simple identifier
        ast = LingoParser.parse("hh_room");
        assertInstanceOf(LingoExpr.Identifier.class, ast);
        assertEquals("hh_room", ((LingoExpr.Identifier) ast).name);
    }

    @Test
    void testStringListParsing() throws ScriptError {
        LingoExpr ast = LingoParser.parse("[\":alertx\",\":banx\",\":kickx\"]");
        assertInstanceOf(LingoExpr.ListLiteral.class, ast);
        LingoExpr.ListLiteral list = (LingoExpr.ListLiteral) ast;
        assertEquals(3, list.items.size());

        assertInstanceOf(LingoExpr.StringLiteral.class, list.items.get(0));
        assertEquals(":alertx", ((LingoExpr.StringLiteral) list.items.get(0)).value);
    }

    @Test
    void testPropListParsing() throws ScriptError {
        LingoExpr ast = LingoParser.parse("[#font:\"v\",#fontSize:9]");
        assertInstanceOf(LingoExpr.PropListLiteral.class, ast);
        LingoExpr.PropListLiteral propList = (LingoExpr.PropListLiteral) ast;
        assertEquals(2, propList.entries.size());

        assertEquals("font", ((LingoExpr.SymbolLiteral) propList.entries.get(0).key).value);
        assertEquals("v", ((LingoExpr.StringLiteral) propList.entries.get(0).value).value);
        assertEquals("fontSize", ((LingoExpr.SymbolLiteral) propList.entries.get(1).key).value);
        assertEquals(9, ((LingoExpr.IntLiteral) propList.entries.get(1).value).value);
    }

    @Test
    void testRgbColorParsing() throws ScriptError {
        LingoExpr ast = LingoParser.parse("rgb(255,0,0)");
        // May be parsed as ColorLiteral or HandlerCall depending on implementation
        if (ast instanceof LingoExpr.ColorLiteral) {
            LingoExpr.ColorLiteral color = (LingoExpr.ColorLiteral) ast;
            assertEquals(255, color.color.red);
            assertEquals(0, color.color.green);
            assertEquals(0, color.color.blue);
        } else if (ast instanceof LingoExpr.HandlerCall) {
            LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) ast;
            assertEquals("rgb", call.handlerName);
            assertEquals(3, call.args.size());
        } else {
            fail("Expected ColorLiteral or HandlerCall, got " + ast.getClass().getSimpleName());
        }
    }

    @Test
    void testQuotedStringWithUrl() throws ScriptError {
        LingoExpr ast = LingoParser.parse("\"http://localhost/v7/c_images/\"");
        assertInstanceOf(LingoExpr.StringLiteral.class, ast);
        assertEquals("http://localhost/v7/c_images/", ((LingoExpr.StringLiteral) ast).value);
    }

    @Test
    @Disabled("Requires external file - enable when file is present in tests/ directory")
    void testParseFullExternalVariablesFile() {
        // This test would read an actual external_variables.txt file
        // Implementation omitted since it requires file access
    }

    @Test
    @Disabled("Requires external file - enable when file is present in tests/ directory")
    void testParseFullExternalTextsFile() {
        // This test would read an actual external_texts.txt file
        // Implementation omitted since it requires file access
    }
}
