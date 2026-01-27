package com.dirplayer.player.eval;

import com.dirplayer.player.ColorRef;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for Lingo expressions.
 * Replaces Pest-based parsing from Rust LingoParser.
 *
 * Grammar (precedence from lowest to highest):
 *   expression   -> assignment
 *   assignment   -> or_expr ("=" or_expr)?
 *   or_expr      -> and_expr ("or" and_expr)*
 *   and_expr     -> not_expr ("and" not_expr)*
 *   not_expr     -> "not" not_expr | comparison
 *   comparison   -> concat (("=" | "<>" | "<" | ">" | "<=" | ">=") concat)*
 *   concat       -> additive (("&" | "&&") additive)*
 *   additive     -> multiplicative (("+" | "-") multiplicative)*
 *   multiplicative -> unary (("*" | "/" | "mod") unary)*
 *   unary        -> "-" unary | postfix
 *   postfix      -> primary ("." IDENTIFIER | "[" expression "]" | "(" args? ")")*
 *   primary      -> literal | identifier | "(" expression ")" | list | proplist
 */
public class LingoParser {

    private final List<LingoToken> tokens;
    private int current;

    public LingoParser(List<LingoToken> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }

    /**
     * Parse a single expression from source code.
     */
    public static LingoExpr parse(String source) throws ScriptError {
        LingoLexer lexer = new LingoLexer(source);
        List<LingoToken> tokens = lexer.tokenize();
        LingoParser parser = new LingoParser(tokens);
        return parser.parseExpression();
    }

    /**
     * Parse multiple expressions (e.g., command with arguments).
     */
    public static List<LingoExpr> parseAll(String source) throws ScriptError {
        LingoLexer lexer = new LingoLexer(source);
        List<LingoToken> tokens = lexer.tokenize();
        LingoParser parser = new LingoParser(tokens);

        List<LingoExpr> exprs = new ArrayList<>();
        while (!parser.isAtEnd()) {
            if (parser.check(LingoToken.TokenType.NEWLINE)) {
                parser.advance();
                continue;
            }
            exprs.add(parser.parseExpression());
        }
        return exprs;
    }

    /**
     * Parse a command (identifier followed by optional arguments).
     */
    public static LingoExpr parseCommand(String source) throws ScriptError {
        LingoLexer lexer = new LingoLexer(source);
        List<LingoToken> tokens = lexer.tokenize();
        LingoParser parser = new LingoParser(tokens);
        return parser.parseCommandExpr();
    }

    // ============ Parser Entry Points ============

    public LingoExpr parseExpression() throws ScriptError {
        skipNewlines();
        return parseAssignment();
    }

    public LingoExpr parseCommandExpr() throws ScriptError {
        skipNewlines();

        // Special handling for 'put' command
        if (check(LingoToken.TokenType.PUT)) {
            return parsePutStatement();
        }

        // Parse identifier as potential handler call
        LingoExpr expr = parseCommandAssignment();

        // If it's just an identifier, treat as handler call with no args
        if (expr instanceof LingoExpr.Identifier && isAtEnd()) {
            String name = ((LingoExpr.Identifier) expr).name;
            return new LingoExpr.HandlerCall(name, new ArrayList<>());
        }

        return expr;
    }

    /**
     * Parse assignment in command context - treats '=' as assignment at top level.
     * This differs from parseExpression which uses '=' for comparison.
     */
    private LingoExpr parseCommandAssignment() throws ScriptError {
        LingoExpr expr = parsePostfix();

        // In command context, top-level '=' is assignment
        if (match(LingoToken.TokenType.EQUALS)) {
            LingoExpr value = parseOrExpr();
            return new LingoExpr.Assignment(expr, value);
        }

        // If not assignment, continue with full expression parsing from or_expr level
        // But we already have the left side from parsePostfix, so continue from there
        return continueExpressionFrom(expr);
    }

    /**
     * Continue expression parsing from a given left-hand expression.
     * Used when we've already parsed the left side and need to continue.
     */
    private LingoExpr continueExpressionFrom(LingoExpr left) throws ScriptError {
        // Continue with or_expr level
        while (match(LingoToken.TokenType.OR)) {
            LingoExpr right = parseAndExpr();
            left = new LingoExpr.Or(left, right);
        }

        // Check for 'and' (should have been handled but just in case)
        if (check(LingoToken.TokenType.AND)) {
            left = continueAndFrom(left);
        }

        return left;
    }

    private LingoExpr continueAndFrom(LingoExpr left) throws ScriptError {
        while (match(LingoToken.TokenType.AND)) {
            LingoExpr right = parseNotExpr();
            left = new LingoExpr.And(left, right);
        }
        return left;
    }

    // ============ Expression Parsing ============

    private LingoExpr parseAssignment() throws ScriptError {
        LingoExpr expr = parseOrExpr();

        if (match(LingoToken.TokenType.EQUALS)) {
            LingoExpr value = parseOrExpr();
            return new LingoExpr.Assignment(expr, value);
        }

        return expr;
    }

    private LingoExpr parseOrExpr() throws ScriptError {
        LingoExpr left = parseAndExpr();

        while (match(LingoToken.TokenType.OR)) {
            LingoExpr right = parseAndExpr();
            left = new LingoExpr.Or(left, right);
        }

        return left;
    }

    private LingoExpr parseAndExpr() throws ScriptError {
        LingoExpr left = parseNotExpr();

        while (match(LingoToken.TokenType.AND)) {
            LingoExpr right = parseNotExpr();
            left = new LingoExpr.And(left, right);
        }

        return left;
    }

    private LingoExpr parseNotExpr() throws ScriptError {
        if (match(LingoToken.TokenType.NOT)) {
            LingoExpr operand = parseNotExpr();
            return new LingoExpr.Not(operand);
        }

        return parseComparison();
    }

    private LingoExpr parseComparison() throws ScriptError {
        LingoExpr left = parseConcat();

        while (true) {
            if (match(LingoToken.TokenType.EQUALS)) {
                left = new LingoExpr.Eq(left, parseConcat());
            } else if (match(LingoToken.TokenType.NOT_EQUALS)) {
                left = new LingoExpr.Ne(left, parseConcat());
            } else if (match(LingoToken.TokenType.LESS_THAN)) {
                left = new LingoExpr.Lt(left, parseConcat());
            } else if (match(LingoToken.TokenType.GREATER_THAN)) {
                left = new LingoExpr.Gt(left, parseConcat());
            } else if (match(LingoToken.TokenType.LESS_EQ)) {
                left = new LingoExpr.Le(left, parseConcat());
            } else if (match(LingoToken.TokenType.GREATER_EQ)) {
                left = new LingoExpr.Ge(left, parseConcat());
            } else {
                break;
            }
        }

        return left;
    }

    private LingoExpr parseConcat() throws ScriptError {
        LingoExpr left = parseAdditive();

        while (true) {
            if (match(LingoToken.TokenType.AMPERSAND)) {
                left = new LingoExpr.Join(left, parseAdditive());
            } else if (match(LingoToken.TokenType.DOUBLE_AMP)) {
                left = new LingoExpr.JoinPad(left, parseAdditive());
            } else {
                break;
            }
        }

        return left;
    }

    private LingoExpr parseAdditive() throws ScriptError {
        LingoExpr left = parseMultiplicative();

        while (true) {
            if (match(LingoToken.TokenType.PLUS)) {
                left = new LingoExpr.Add(left, parseMultiplicative());
            } else if (match(LingoToken.TokenType.MINUS)) {
                left = new LingoExpr.Subtract(left, parseMultiplicative());
            } else {
                break;
            }
        }

        return left;
    }

    private LingoExpr parseMultiplicative() throws ScriptError {
        LingoExpr left = parseUnary();

        while (true) {
            if (match(LingoToken.TokenType.MULTIPLY)) {
                left = new LingoExpr.Multiply(left, parseUnary());
            } else if (match(LingoToken.TokenType.DIVIDE)) {
                left = new LingoExpr.Divide(left, parseUnary());
            } else if (match(LingoToken.TokenType.MOD)) {
                left = new LingoExpr.Mod(left, parseUnary());
            } else {
                break;
            }
        }

        return left;
    }

    private LingoExpr parseUnary() throws ScriptError {
        if (match(LingoToken.TokenType.MINUS)) {
            LingoExpr operand = parseUnary();
            return new LingoExpr.Negate(operand);
        }

        return parsePostfix();
    }

    private LingoExpr parsePostfix() throws ScriptError {
        LingoExpr expr = parsePrimary();

        while (true) {
            if (match(LingoToken.TokenType.DOT)) {
                // Property access or method call
                LingoToken name = consume(LingoToken.TokenType.IDENTIFIER, "Expected property name after '.'");

                if (match(LingoToken.TokenType.LPAREN)) {
                    // Method call
                    List<LingoExpr> args = parseArguments();
                    consume(LingoToken.TokenType.RPAREN, "Expected ')' after arguments");
                    expr = new LingoExpr.ObjHandlerCall(expr, name.value, args);
                } else {
                    // Property access
                    expr = new LingoExpr.ObjProp(expr, name.value);
                }
            } else if (match(LingoToken.TokenType.LBRACKET)) {
                // List access
                LingoExpr index = parseExpression();
                consume(LingoToken.TokenType.RBRACKET, "Expected ']' after index");
                expr = new LingoExpr.ListAccess(expr, index);
            } else if (match(LingoToken.TokenType.LPAREN) && expr instanceof LingoExpr.Identifier) {
                // Function call (only if expr is identifier)
                String name = ((LingoExpr.Identifier) expr).name;
                List<LingoExpr> args = parseArguments();
                consume(LingoToken.TokenType.RPAREN, "Expected ')' after arguments");
                expr = new LingoExpr.HandlerCall(name, args);
            } else {
                break;
            }
        }

        return expr;
    }

    private LingoExpr parsePrimary() throws ScriptError {
        // Literals
        if (match(LingoToken.TokenType.INTEGER)) {
            return new LingoExpr.IntLiteral(Integer.parseInt(previous().value));
        }
        if (match(LingoToken.TokenType.FLOAT)) {
            return new LingoExpr.FloatLiteral(Double.parseDouble(previous().value));
        }
        if (match(LingoToken.TokenType.STRING)) {
            return new LingoExpr.StringLiteral(previous().value);
        }
        if (match(LingoToken.TokenType.SYMBOL)) {
            return new LingoExpr.SymbolLiteral(previous().value);
        }
        if (match(LingoToken.TokenType.TRUE)) {
            return new LingoExpr.BoolLiteral(true);
        }
        if (match(LingoToken.TokenType.FALSE)) {
            return new LingoExpr.BoolLiteral(false);
        }
        if (match(LingoToken.TokenType.VOID)) {
            return LingoExpr.VoidLiteral.INSTANCE;
        }
        if (match(LingoToken.TokenType.RETURN)) {
            return new LingoExpr.StringLiteral("\r\n");
        }

        // The ... of ... construct
        if (match(LingoToken.TokenType.THE)) {
            return parseTheExpr();
        }

        // Member reference
        if (match(LingoToken.TokenType.MEMBER)) {
            return parseMemberRef();
        }

        // Sprite reference
        if (match(LingoToken.TokenType.SPRITE)) {
            return parseSpriteRef();
        }

        // CastLib reference
        if (match(LingoToken.TokenType.CAST_LIB)) {
            return parseCastLibRef();
        }

        // Chunk expressions
        if (peek().isChunkType()) {
            return parseChunkExpr();
        }

        // Rect literal
        if (match(LingoToken.TokenType.RECT)) {
            return parseRectLiteral();
        }

        // Point literal
        if (match(LingoToken.TokenType.POINT)) {
            return parsePointLiteral();
        }

        // RGB color literal
        if (match(LingoToken.TokenType.RGB) || match(LingoToken.TokenType.COLOR)) {
            return parseColorLiteral();
        }

        // Grouping or list
        if (match(LingoToken.TokenType.LPAREN)) {
            LingoExpr expr = parseExpression();
            consume(LingoToken.TokenType.RPAREN, "Expected ')' after expression");
            return expr;
        }

        // List literal
        if (match(LingoToken.TokenType.LBRACKET)) {
            return parseListOrPropList();
        }

        // Identifier
        if (match(LingoToken.TokenType.IDENTIFIER)) {
            return new LingoExpr.Identifier(previous().value);
        }

        throw error("Unexpected token: " + peek());
    }

    // ============ Special Constructs ============

    private LingoExpr parseTheExpr() throws ScriptError {
        // "the" propertyName ["of" expression]
        if (!peek().canBePropertyName()) {
            throw error("Expected property name after 'the'");
        }

        StringBuilder propName = new StringBuilder();
        // Allow multi-word properties like "the long time"
        while (peek().canBePropertyName()) {
            if (peek().type == LingoToken.TokenType.OF) break;
            if (propName.length() > 0) propName.append(" ");
            propName.append(advance().value);
        }

        if (match(LingoToken.TokenType.OF)) {
            LingoExpr target = parsePostfix();
            return new LingoExpr.ThePropOf(target, propName.toString());
        }

        // Just "the propertyName" - treat as identifier
        return new LingoExpr.Identifier("the " + propName.toString());
    }

    private LingoExpr parseMemberRef() throws ScriptError {
        // member(expr) or member expr of castLib expr
        LingoExpr memberExpr;

        if (match(LingoToken.TokenType.LPAREN)) {
            memberExpr = parseExpression();
            consume(LingoToken.TokenType.RPAREN, "Expected ')' after member expression");
        } else {
            memberExpr = parsePrimary();
        }

        LingoExpr castLibExpr = null;
        if (match(LingoToken.TokenType.OF)) {
            if (match(LingoToken.TokenType.CAST_LIB)) {
                castLibExpr = parsePrimary();
            } else {
                throw error("Expected 'castLib' after 'of'");
            }
        }

        return new LingoExpr.MemberRef(memberExpr, castLibExpr);
    }

    private LingoExpr parseSpriteRef() throws ScriptError {
        // sprite(expr) or sprite expr or sprite the X
        LingoExpr spriteExpr;

        if (match(LingoToken.TokenType.LPAREN)) {
            spriteExpr = parseExpression();
            consume(LingoToken.TokenType.RPAREN, "Expected ')' after sprite expression");
        } else if (check(LingoToken.TokenType.THE)) {
            // Handle "sprite the currentSpriteNum" form
            spriteExpr = parsePrimary(); // This will call parseTheExpr()
        } else {
            spriteExpr = parsePrimary();
        }

        List<LingoExpr> args = new ArrayList<>();
        args.add(spriteExpr);
        return new LingoExpr.HandlerCall("sprite", args);
    }

    private LingoExpr parseCastLibRef() throws ScriptError {
        // castLib(expr) or castLib expr
        LingoExpr castLibExpr;

        if (match(LingoToken.TokenType.LPAREN)) {
            castLibExpr = parseExpression();
            consume(LingoToken.TokenType.RPAREN, "Expected ')' after castLib expression");
        } else {
            castLibExpr = parsePrimary();
        }

        List<LingoExpr> args = new ArrayList<>();
        args.add(castLibExpr);
        return new LingoExpr.HandlerCall("castLib", args);
    }

    private LingoExpr parseChunkExpr() throws ScriptError {
        // char/word/item/line X [to Y] of expression
        LingoToken chunkType = advance();
        String type = chunkType.value.toLowerCase();

        LingoExpr startIndex = parseExpression();
        LingoExpr endIndex = null;

        if (match(LingoToken.TokenType.TO)) {
            endIndex = parseExpression();
        }

        consume(LingoToken.TokenType.OF, "Expected 'of' in chunk expression");
        LingoExpr source = parsePostfix();

        return new LingoExpr.ChunkExpr(type, startIndex, endIndex, source);
    }

    private LingoExpr parseRectLiteral() throws ScriptError {
        consume(LingoToken.TokenType.LPAREN, "Expected '(' after 'rect'");
        LingoExpr left = parseExpression();
        consume(LingoToken.TokenType.COMMA, "Expected ',' in rect");
        LingoExpr top = parseExpression();
        consume(LingoToken.TokenType.COMMA, "Expected ',' in rect");
        LingoExpr right = parseExpression();
        consume(LingoToken.TokenType.COMMA, "Expected ',' in rect");
        LingoExpr bottom = parseExpression();
        consume(LingoToken.TokenType.RPAREN, "Expected ')' after rect");
        return new LingoExpr.RectLiteral(left, top, right, bottom);
    }

    private LingoExpr parsePointLiteral() throws ScriptError {
        consume(LingoToken.TokenType.LPAREN, "Expected '(' after 'point'");
        LingoExpr x = parseExpression();
        consume(LingoToken.TokenType.COMMA, "Expected ',' in point");
        LingoExpr y = parseExpression();
        consume(LingoToken.TokenType.RPAREN, "Expected ')' after point");
        return new LingoExpr.PointLiteral(x, y);
    }

    private LingoExpr parseColorLiteral() throws ScriptError {
        consume(LingoToken.TokenType.LPAREN, "Expected '(' after 'rgb'/'color'");

        LingoExpr rExpr = parseExpression();
        consume(LingoToken.TokenType.COMMA, "Expected ',' in color");
        LingoExpr gExpr = parseExpression();
        consume(LingoToken.TokenType.COMMA, "Expected ',' in color");
        LingoExpr bExpr = parseExpression();
        consume(LingoToken.TokenType.RPAREN, "Expected ')' after color");

        // If all are integer literals, create a direct ColorLiteral
        if (rExpr instanceof LingoExpr.IntLiteral &&
            gExpr instanceof LingoExpr.IntLiteral &&
            bExpr instanceof LingoExpr.IntLiteral) {
            int r = ((LingoExpr.IntLiteral) rExpr).value;
            int g = ((LingoExpr.IntLiteral) gExpr).value;
            int b = ((LingoExpr.IntLiteral) bExpr).value;
            return new LingoExpr.ColorLiteral(ColorRef.fromRgb(r, g, b));
        }

        // Otherwise, return as a handler call to be evaluated at runtime
        List<LingoExpr> args = new ArrayList<>();
        args.add(rExpr);
        args.add(gExpr);
        args.add(bExpr);
        return new LingoExpr.HandlerCall("rgb", args);
    }

    private LingoExpr parseListOrPropList() throws ScriptError {
        // Empty list/proplist
        if (match(LingoToken.TokenType.RBRACKET)) {
            return new LingoExpr.ListLiteral(new ArrayList<>());
        }

        // Empty proplist [:]
        if (match(LingoToken.TokenType.COLON)) {
            consume(LingoToken.TokenType.RBRACKET, "Expected ']' after ':'");
            return new LingoExpr.PropListLiteral(new ArrayList<>());
        }

        // First element
        LingoExpr first = parseExpression();

        // Check if it's a proplist
        if (match(LingoToken.TokenType.COLON)) {
            // Proplist
            List<LingoExpr.PropListEntry> entries = new ArrayList<>();
            LingoExpr value = parseExpression();
            entries.add(new LingoExpr.PropListEntry(first, value));

            while (match(LingoToken.TokenType.COMMA)) {
                LingoExpr key = parseExpression();
                consume(LingoToken.TokenType.COLON, "Expected ':' in proplist entry");
                value = parseExpression();
                entries.add(new LingoExpr.PropListEntry(key, value));
            }

            consume(LingoToken.TokenType.RBRACKET, "Expected ']' after proplist");
            return new LingoExpr.PropListLiteral(entries);
        }

        // Regular list
        List<LingoExpr> items = new ArrayList<>();
        items.add(first);

        while (match(LingoToken.TokenType.COMMA)) {
            items.add(parseExpression());
        }

        consume(LingoToken.TokenType.RBRACKET, "Expected ']' after list");
        return new LingoExpr.ListLiteral(items);
    }

    private LingoExpr parsePutStatement() throws ScriptError {
        consume(LingoToken.TokenType.PUT, "Expected 'put'");

        // Handle "put" alone (no arguments) -> HandlerCall
        if (isAtEnd()) {
            return new LingoExpr.HandlerCall("put", new ArrayList<>());
        }

        // Handle "put()" or "put(...)" with parentheses
        if (check(LingoToken.TokenType.LPAREN)) {
            advance(); // consume '('

            // "put()" with no args is an error
            if (check(LingoToken.TokenType.RPAREN)) {
                throw error("put() requires at least one argument");
            }

            // Parse first argument
            LingoExpr first = parseExpression();

            // Check if there are more arguments
            if (match(LingoToken.TokenType.COMMA)) {
                // Multiple args -> HandlerCall("put", args)
                List<LingoExpr> args = new ArrayList<>();
                args.add(first);
                do {
                    args.add(parseExpression());
                } while (match(LingoToken.TokenType.COMMA));
                consume(LingoToken.TokenType.RPAREN, "Expected ')' after arguments");
                return new LingoExpr.HandlerCall("put", args);
            }

            // Single arg in parens -> PutDisplay
            consume(LingoToken.TokenType.RPAREN, "Expected ')' after expression");

            // Check for into/before/after
            if (match(LingoToken.TokenType.INTO)) {
                LingoExpr target = parsePostfix();
                return new LingoExpr.PutInto(first, target);
            } else if (match(LingoToken.TokenType.BEFORE)) {
                LingoExpr target = parsePostfix();
                return new LingoExpr.PutBefore(first, target);
            } else if (match(LingoToken.TokenType.AFTER)) {
                LingoExpr target = parsePostfix();
                return new LingoExpr.PutAfter(first, target);
            }

            return new LingoExpr.PutDisplay(first);
        }

        // Handle "put expr" without parentheses
        LingoExpr value = parseExpression();

        // Check for into/before/after
        if (match(LingoToken.TokenType.INTO)) {
            LingoExpr target = parsePostfix();
            return new LingoExpr.PutInto(value, target);
        } else if (match(LingoToken.TokenType.BEFORE)) {
            LingoExpr target = parsePostfix();
            return new LingoExpr.PutBefore(value, target);
        } else if (match(LingoToken.TokenType.AFTER)) {
            LingoExpr target = parsePostfix();
            return new LingoExpr.PutAfter(value, target);
        }

        // Check for comma-separated args -> HandlerCall
        if (match(LingoToken.TokenType.COMMA)) {
            List<LingoExpr> args = new ArrayList<>();
            args.add(value);
            do {
                args.add(parseExpression());
            } while (match(LingoToken.TokenType.COMMA));

            // After comma-separated args, we should be at end
            if (!isAtEnd()) {
                throw error("Unexpected token after put arguments");
            }
            return new LingoExpr.HandlerCall("put", args);
        }

        // Check for space-separated args (error case) - "put 1 2 3"
        if (!isAtEnd()) {
            throw error("Unexpected token after put expression - use commas to separate arguments");
        }

        // Just "put value" - display to message window
        return new LingoExpr.PutDisplay(value);
    }

    private List<LingoExpr> parseArguments() throws ScriptError {
        List<LingoExpr> args = new ArrayList<>();

        if (!check(LingoToken.TokenType.RPAREN)) {
            do {
                args.add(parseExpression());
            } while (match(LingoToken.TokenType.COMMA));
        }

        return args;
    }

    // ============ Utility Methods ============

    private boolean check(LingoToken.TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean match(LingoToken.TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private LingoToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == LingoToken.TokenType.EOF;
    }

    private LingoToken peek() {
        return tokens.get(current);
    }

    private LingoToken previous() {
        return tokens.get(current - 1);
    }

    private LingoToken consume(LingoToken.TokenType type, String message) throws ScriptError {
        if (check(type)) return advance();
        throw error(message + ", got " + peek().type);
    }

    private void skipNewlines() {
        while (check(LingoToken.TokenType.NEWLINE)) {
            advance();
        }
    }

    private ScriptError error(String message) {
        LingoToken token = peek();
        return new ScriptError("Parse error at position " + token.position + ": " + message);
    }
}
