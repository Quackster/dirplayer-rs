package com.dirplayer.player.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lexer for Lingo expressions.
 * Converts a string into a sequence of tokens.
 * Port of Pest-based tokenization from Rust LingoParser.
 */
public class LingoLexer {

    private static final Map<String, LingoToken.TokenType> KEYWORDS;
    static {
        KEYWORDS = new HashMap<>();
        KEYWORDS.put("true", LingoToken.TokenType.TRUE);
        KEYWORDS.put("false", LingoToken.TokenType.FALSE);
        KEYWORDS.put("void", LingoToken.TokenType.VOID);
        KEYWORDS.put("not", LingoToken.TokenType.NOT);
        KEYWORDS.put("and", LingoToken.TokenType.AND);
        KEYWORDS.put("or", LingoToken.TokenType.OR);
        KEYWORDS.put("mod", LingoToken.TokenType.MOD);
        KEYWORDS.put("the", LingoToken.TokenType.THE);
        KEYWORDS.put("of", LingoToken.TokenType.OF);
        KEYWORDS.put("put", LingoToken.TokenType.PUT);
        KEYWORDS.put("into", LingoToken.TokenType.INTO);
        KEYWORDS.put("before", LingoToken.TokenType.BEFORE);
        KEYWORDS.put("after", LingoToken.TokenType.AFTER);
        KEYWORDS.put("member", LingoToken.TokenType.MEMBER);
        KEYWORDS.put("sprite", LingoToken.TokenType.SPRITE);
        KEYWORDS.put("castlib", LingoToken.TokenType.CAST_LIB);
        KEYWORDS.put("castLib", LingoToken.TokenType.CAST_LIB);
        KEYWORDS.put("char", LingoToken.TokenType.CHAR);
        KEYWORDS.put("word", LingoToken.TokenType.WORD);
        KEYWORDS.put("item", LingoToken.TokenType.ITEM);
        KEYWORDS.put("line", LingoToken.TokenType.LINE);
        KEYWORDS.put("to", LingoToken.TokenType.TO);
        KEYWORDS.put("down", LingoToken.TokenType.DOWN);
        KEYWORDS.put("rect", LingoToken.TokenType.RECT);
        KEYWORDS.put("point", LingoToken.TokenType.POINT);
        KEYWORDS.put("rgb", LingoToken.TokenType.RGB);
        KEYWORDS.put("color", LingoToken.TokenType.COLOR);
        KEYWORDS.put("return", LingoToken.TokenType.RETURN);
    }

    private final String source;
    private int position;
    private int line;
    private int column;
    private final List<LingoToken> tokens;

    public LingoLexer(String source) {
        this.source = source != null ? source : "";
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<>();
    }

    /**
     * Tokenize the entire source string.
     */
    public List<LingoToken> tokenize() {
        tokens.clear();

        while (!isAtEnd()) {
            skipWhitespace();
            if (isAtEnd()) break;

            LingoToken token = scanToken();
            if (token != null) {
                tokens.add(token);
            }
        }

        tokens.add(new LingoToken(LingoToken.TokenType.EOF, "", position, line, column));
        return tokens;
    }

    private boolean isAtEnd() {
        return position >= source.length();
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(position);
    }

    private char peekNext() {
        if (position + 1 >= source.length()) return '\0';
        return source.charAt(position + 1);
    }

    private char advance() {
        char c = source.charAt(position);
        position++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void skipWhitespace() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '-' && peekNext() == '-') {
                // Line comment
                skipLineComment();
            } else {
                break;
            }
        }
    }

    private void skipLineComment() {
        // Skip to end of line
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private LingoToken scanToken() {
        int startPos = position;
        int startLine = line;
        int startCol = column;

        char c = advance();

        // Single-character tokens
        switch (c) {
            case '\n':
                return new LingoToken(LingoToken.TokenType.NEWLINE, "\n", startPos, startLine, startCol);
            case '(':
                return new LingoToken(LingoToken.TokenType.LPAREN, "(", startPos, startLine, startCol);
            case ')':
                return new LingoToken(LingoToken.TokenType.RPAREN, ")", startPos, startLine, startCol);
            case '[':
                return new LingoToken(LingoToken.TokenType.LBRACKET, "[", startPos, startLine, startCol);
            case ']':
                return new LingoToken(LingoToken.TokenType.RBRACKET, "]", startPos, startLine, startCol);
            case ',':
                return new LingoToken(LingoToken.TokenType.COMMA, ",", startPos, startLine, startCol);
            case ':':
                return new LingoToken(LingoToken.TokenType.COLON, ":", startPos, startLine, startCol);
            case '.':
                return new LingoToken(LingoToken.TokenType.DOT, ".", startPos, startLine, startCol);
            case '+':
                return new LingoToken(LingoToken.TokenType.PLUS, "+", startPos, startLine, startCol);
            case '-':
                return new LingoToken(LingoToken.TokenType.MINUS, "-", startPos, startLine, startCol);
            case '*':
                return new LingoToken(LingoToken.TokenType.MULTIPLY, "*", startPos, startLine, startCol);
            case '/':
                return new LingoToken(LingoToken.TokenType.DIVIDE, "/", startPos, startLine, startCol);
            case '#':
                return scanSymbol(startPos, startLine, startCol);
            case '"':
                return scanString(startPos, startLine, startCol);
            case '&':
                if (peek() == '&') {
                    advance();
                    return new LingoToken(LingoToken.TokenType.DOUBLE_AMP, "&&", startPos, startLine, startCol);
                }
                return new LingoToken(LingoToken.TokenType.AMPERSAND, "&", startPos, startLine, startCol);
            case '=':
                return new LingoToken(LingoToken.TokenType.EQUALS, "=", startPos, startLine, startCol);
            case '<':
                if (peek() == '=') {
                    advance();
                    return new LingoToken(LingoToken.TokenType.LESS_EQ, "<=", startPos, startLine, startCol);
                }
                if (peek() == '>') {
                    advance();
                    return new LingoToken(LingoToken.TokenType.NOT_EQUALS, "<>", startPos, startLine, startCol);
                }
                return new LingoToken(LingoToken.TokenType.LESS_THAN, "<", startPos, startLine, startCol);
            case '>':
                if (peek() == '=') {
                    advance();
                    return new LingoToken(LingoToken.TokenType.GREATER_EQ, ">=", startPos, startLine, startCol);
                }
                return new LingoToken(LingoToken.TokenType.GREATER_THAN, ">", startPos, startLine, startCol);
        }

        // Numbers
        if (Character.isDigit(c) || (c == '-' && Character.isDigit(peek()))) {
            return scanNumber(startPos, startLine, startCol, c);
        }

        // Identifiers and keywords
        if (isIdentifierStart(c)) {
            return scanIdentifier(startPos, startLine, startCol, c);
        }

        // Unknown character
        return new LingoToken(LingoToken.TokenType.ERROR, String.valueOf(c), startPos, startLine, startCol);
    }

    private LingoToken scanSymbol(int startPos, int startLine, int startCol) {
        // Symbol literal: #symbolName
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && isIdentifierPart(peek())) {
            sb.append(advance());
        }
        return new LingoToken(LingoToken.TokenType.SYMBOL, sb.toString(), startPos, startLine, startCol);
    }

    private LingoToken scanString(int startPos, int startLine, int startCol) {
        // String literal: "..."
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\\' && !isAtEnd()) {
                // Handle escape sequences
                char escaped = advance();
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: sb.append(escaped); break;
                }
            } else {
                sb.append(c);
            }
        }

        if (!isAtEnd()) {
            advance();  // Consume closing quote
        }

        return new LingoToken(LingoToken.TokenType.STRING, sb.toString(), startPos, startLine, startCol);
    }

    private LingoToken scanNumber(int startPos, int startLine, int startCol, char firstChar) {
        StringBuilder sb = new StringBuilder();
        sb.append(firstChar);

        boolean isFloat = false;

        while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
            if (peek() == '.') {
                if (isFloat) break;  // Second dot ends the number
                // Check if it's a decimal point or property access
                if (position + 1 < source.length() && !Character.isDigit(source.charAt(position + 1))) {
                    break;  // It's property access (e.g., "5.count")
                }
                isFloat = true;
            }
            sb.append(advance());
        }

        // Handle scientific notation
        if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
            sb.append(advance());
            if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                sb.append(advance());
            }
            while (!isAtEnd() && Character.isDigit(peek())) {
                sb.append(advance());
            }
            isFloat = true;
        }

        LingoToken.TokenType type = isFloat ? LingoToken.TokenType.FLOAT : LingoToken.TokenType.INTEGER;
        return new LingoToken(type, sb.toString(), startPos, startLine, startCol);
    }

    private LingoToken scanIdentifier(int startPos, int startLine, int startCol, char firstChar) {
        StringBuilder sb = new StringBuilder();
        sb.append(firstChar);

        while (!isAtEnd() && isIdentifierPart(peek())) {
            sb.append(advance());
        }

        String text = sb.toString();
        String lowerText = text.toLowerCase();

        // Check for keywords (case-insensitive)
        LingoToken.TokenType keywordType = KEYWORDS.get(lowerText);
        if (keywordType != null) {
            return new LingoToken(keywordType, text, startPos, startLine, startCol);
        }

        return new LingoToken(LingoToken.TokenType.IDENTIFIER, text, startPos, startLine, startCol);
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Get the original source string.
     */
    public String getSource() {
        return source;
    }
}
