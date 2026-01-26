/**
 * Lingo expression evaluation module.
 *
 * <p>This package provides runtime evaluation of Lingo expressions for the Director player.
 * It is a port of the Rust vm-rust/src/player/eval.rs module.
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link com.dirplayer.player.eval.LingoExpr} - AST node types for parsed expressions</li>
 *   <li>{@link com.dirplayer.player.eval.LingoToken} - Token types produced by the lexer</li>
 *   <li>{@link com.dirplayer.player.eval.LingoLexer} - Lexer that tokenizes Lingo source code</li>
 *   <li>{@link com.dirplayer.player.eval.LingoParser} - Recursive descent parser for Lingo expressions</li>
 *   <li>{@link com.dirplayer.player.eval.ExpressionEvaluator} - Evaluates AST nodes at runtime</li>
 *   <li>{@link com.dirplayer.player.eval.DatumOperations} - Arithmetic and comparison operations</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Parse and evaluate a simple expression
 * ExpressionEvaluator evaluator = new ExpressionEvaluator(player);
 * int resultRef = evaluator.evalExpression("1 + 2 * 3");
 * Datum result = player.getDatum(resultRef);
 *
 * // Parse expression to AST (for inspection or deferred evaluation)
 * LingoExpr ast = LingoParser.parse("myVar.count + 1");
 *
 * // Evaluate command (identifier treated as handler call)
 * int cmdResult = evaluator.evalCommand("go to frame 10");
 * </pre>
 *
 * <h2>Supported Expression Types</h2>
 * <ul>
 *   <li>Literals: integers, floats, strings, symbols, booleans, void</li>
 *   <li>Lists: [1, 2, 3] and property lists [:key: value]</li>
 *   <li>Arithmetic: +, -, *, /, mod</li>
 *   <li>Comparison: =, <>, <, >, <=, >=</li>
 *   <li>Logical: and, or, not</li>
 *   <li>String: & (concat), && (concat with space)</li>
 *   <li>Access: obj.prop, list[index], the prop of obj</li>
 *   <li>Calls: handler(args), obj.method(args)</li>
 *   <li>Assignment: var = value, put value into var</li>
 *   <li>Chunk expressions: char 1 of str, word 2 to 4 of str</li>
 *   <li>References: member(num), sprite(num), castLib(num)</li>
 *   <li>Colors: rgb(r, g, b), color(r, g, b)</li>
 *   <li>Geometry: point(x, y), rect(l, t, r, b)</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <p>The original Rust implementation uses the Pest parser generator. Since Java
 * does not have Pest, this port uses a hand-written recursive descent parser
 * which provides equivalent functionality with proper operator precedence.
 *
 * <p>The parser handles Lingo's case-insensitivity and its various syntactic forms
 * (e.g., "the X of Y" property access, command-style handler calls).
 *
 * @see com.dirplayer.player.bytecode Package for bytecode-based execution
 */
package com.dirplayer.player.eval;
