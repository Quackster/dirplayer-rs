package com.dirplayer.director.lingo.decompiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lingo syntax highlighting tokenizer.
 * Produces spans with token types for syntax highlighting.
 * Port of Rust tokenizer.rs.
 */
public class Tokenizer {

    /**
     * Token types for syntax highlighting.
     */
    public enum TokenType {
        Keyword("keyword"),
        Identifier("identifier"),
        Number("number"),
        String("string"),
        Symbol("symbol"),
        Operator("operator"),
        Comment("comment"),
        Builtin("builtin"),
        Punctuation("punctuation"),
        Whitespace("whitespace");

        private final String name;

        TokenType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * A span of text with a token type.
     */
    public static class Span {
        public String text;
        public TokenType tokenType;

        public Span(String text, TokenType tokenType) {
            this.text = text;
            this.tokenType = tokenType;
        }
    }

    /**
     * Keywords in Lingo (case-insensitive).
     */
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "if", "then", "else", "end", "repeat", "while", "with", "in", "to", "down",
        "exit", "return", "next", "put", "into", "before", "after", "set",
        "global", "property", "on", "me", "new", "case", "of", "otherwise",
        "tell", "and", "or", "not", "mod", "true", "false", "void",
        "sprite", "member", "castlib", "field", "the",
        "char", "word", "line", "item"
    ));

    /**
     * Check if a word is a keyword (case-insensitive).
     */
    private static boolean isKeyword(String word) {
        return KEYWORDS.contains(word.toLowerCase());
    }

    /**
     * Tokenize a line of Lingo code into spans for syntax highlighting.
     */
    public static List<Span> tokenizeLine(String line) {
        List<Span> spans = new ArrayList<>();
        char[] chars = line.toCharArray();
        int pos = 0;

        while (pos < chars.length) {
            char ch = chars[pos];

            // Comment (-- to end of line)
            if (ch == '-' && pos + 1 < chars.length && chars[pos + 1] == '-') {
                String text = line.substring(pos);
                spans.add(new Span(text, TokenType.Comment));
                break;
            }

            // Whitespace
            if (Character.isWhitespace(ch)) {
                int start = pos;
                while (pos < chars.length && Character.isWhitespace(chars[pos])) {
                    pos++;
                }
                spans.add(new Span(line.substring(start, pos), TokenType.Whitespace));
                continue;
            }

            // String literal
            if (ch == '"') {
                int start = pos;
                pos++;
                while (pos < chars.length && chars[pos] != '"') {
                    pos++;
                }
                if (pos < chars.length) {
                    pos++; // include closing quote
                }
                spans.add(new Span(line.substring(start, pos), TokenType.String));
                continue;
            }

            // Symbol (#identifier)
            if (ch == '#') {
                int start = pos;
                pos++;
                while (pos < chars.length && (Character.isLetterOrDigit(chars[pos]) || chars[pos] == '_')) {
                    pos++;
                }
                spans.add(new Span(line.substring(start, pos), TokenType.Symbol));
                continue;
            }

            // Number (including negative numbers and floats)
            if (Character.isDigit(ch) || (ch == '-' && pos + 1 < chars.length && Character.isDigit(chars[pos + 1]))) {
                int start = pos;
                if (ch == '-') {
                    pos++;
                }
                while (pos < chars.length && Character.isDigit(chars[pos])) {
                    pos++;
                }
                // Check for decimal point
                if (pos < chars.length && chars[pos] == '.' && pos + 1 < chars.length && Character.isDigit(chars[pos + 1])) {
                    pos++;
                    while (pos < chars.length && Character.isDigit(chars[pos])) {
                        pos++;
                    }
                }
                // Check for exponent
                if (pos < chars.length && (chars[pos] == 'e' || chars[pos] == 'E')) {
                    int expStart = pos;
                    pos++;
                    if (pos < chars.length && (chars[pos] == '+' || chars[pos] == '-')) {
                        pos++;
                    }
                    if (pos < chars.length && Character.isDigit(chars[pos])) {
                        while (pos < chars.length && Character.isDigit(chars[pos])) {
                            pos++;
                        }
                    } else {
                        pos = expStart; // Not a valid exponent, backtrack
                    }
                }
                spans.add(new Span(line.substring(start, pos), TokenType.Number));
                continue;
            }

            // Identifier or keyword
            if (Character.isLetter(ch) || ch == '_') {
                int start = pos;
                while (pos < chars.length && (Character.isLetterOrDigit(chars[pos]) || chars[pos] == '_')) {
                    pos++;
                }
                String word = line.substring(start, pos);
                TokenType tokenType = isKeyword(word) ? TokenType.Keyword : TokenType.Identifier;
                spans.add(new Span(word, tokenType));
                continue;
            }

            // Multi-character operators
            if (pos + 1 < chars.length) {
                String twoChar = line.substring(pos, pos + 2);
                if (twoChar.equals("<>") || twoChar.equals("<=") || twoChar.equals(">=") || twoChar.equals("&&")) {
                    spans.add(new Span(twoChar, TokenType.Operator));
                    pos += 2;
                    continue;
                }
            }

            // Single-character operators
            if (ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '&' || ch == '=' || ch == '<' || ch == '>' || ch == '.') {
                spans.add(new Span(String.valueOf(ch), TokenType.Operator));
                pos++;
                continue;
            }

            // Punctuation
            if (ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == ',' || ch == ':') {
                spans.add(new Span(String.valueOf(ch), TokenType.Punctuation));
                pos++;
                continue;
            }

            // Unknown character - treat as identifier
            spans.add(new Span(String.valueOf(ch), TokenType.Identifier));
            pos++;
        }

        return spans;
    }
}
