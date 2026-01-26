package com.dirplayer.player.eval;

/**
 * Token types for the Lingo lexer.
 * Used by LingoLexer for tokenization.
 */
public class LingoToken {

    public enum TokenType {
        // Literals
        INTEGER,
        FLOAT,
        STRING,
        SYMBOL,

        // Identifiers and keywords
        IDENTIFIER,
        TRUE,
        FALSE,
        VOID,
        NOT,
        AND,
        OR,
        MOD,
        THE,
        OF,
        PUT,
        INTO,
        BEFORE,
        AFTER,
        MEMBER,
        SPRITE,
        CAST_LIB,
        CHAR,
        WORD,
        ITEM,
        LINE,
        TO,
        DOWN,
        RECT,
        POINT,
        RGB,
        COLOR,
        RETURN,

        // Operators
        PLUS,           // +
        MINUS,          // -
        MULTIPLY,       // *
        DIVIDE,         // /
        AMPERSAND,      // &
        DOUBLE_AMP,     // &&
        EQUALS,         // =
        NOT_EQUALS,     // <>
        LESS_THAN,      // <
        GREATER_THAN,   // >
        LESS_EQ,        // <=
        GREATER_EQ,     // >=

        // Delimiters
        LPAREN,         // (
        RPAREN,         // )
        LBRACKET,       // [
        RBRACKET,       // ]
        COMMA,          // ,
        COLON,          // :
        DOT,            // .
        HASH,           // #

        // Special
        EOF,
        NEWLINE,
        ERROR
    }

    public final TokenType type;
    public final String value;
    public final int position;
    public final int line;
    public final int column;

    public LingoToken(TokenType type, String value, int position, int line, int column) {
        this.type = type;
        this.value = value;
        this.position = position;
        this.line = line;
        this.column = column;
    }

    public LingoToken(TokenType type, String value, int position) {
        this(type, value, position, 1, position);
    }

    public boolean is(TokenType t) {
        return type == t;
    }

    public boolean isOneOf(TokenType... types) {
        for (TokenType t : types) {
            if (type == t) return true;
        }
        return false;
    }

    public boolean isComparisonOp() {
        return isOneOf(TokenType.EQUALS, TokenType.NOT_EQUALS,
                       TokenType.LESS_THAN, TokenType.GREATER_THAN,
                       TokenType.LESS_EQ, TokenType.GREATER_EQ);
    }

    public boolean isAdditiveOp() {
        return isOneOf(TokenType.PLUS, TokenType.MINUS);
    }

    public boolean isMultiplicativeOp() {
        return isOneOf(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MOD);
    }

    public boolean isConcatOp() {
        return isOneOf(TokenType.AMPERSAND, TokenType.DOUBLE_AMP);
    }

    public boolean isChunkType() {
        return isOneOf(TokenType.CHAR, TokenType.WORD, TokenType.ITEM, TokenType.LINE);
    }

    @Override
    public String toString() {
        return "Token{" + type + ", \"" + value + "\", pos=" + position + "}";
    }
}
