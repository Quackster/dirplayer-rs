# DirPlayer Java Port - Task Documentation

## Overview
This is a Java port of **DirPlayer**, a Shockwave/Director Player emulator originally written in Rust (~60,000+ lines across 141 files). The project emulates the Macromedia/Adobe Director runtime environment, allowing playback of classic Shockwave content in modern browsers.

## Task
Port the entirety of the Rust project (`vm-rust/`) to Java (`vm-java/`) without changing any behavior or skipping any features.

## Original Project Structure (Rust)

### Core Modules
- **`io/`** - Binary readers for Director file formats
- **`director/`** - Director file parsing and data structures
  - `chunks/` - 24+ chunk type parsers (bitmap, cast, script, score, etc.)
  - `lingo/` - Lingo scripting language support
    - `decompiler/` - Bytecode decompiler
    - `datum.rs` - Runtime value types
    - `opcode.rs` - Bytecode opcodes
    - `constants.rs` - Property name mappings
- **`player/`** - Runtime engine (~40,000+ lines)
  - `bitmap/` - Bitmap rendering and management
  - `bytecode/` - Bytecode execution
  - `handlers/` - Built-in function handlers
  - `score.rs` - Animation timeline
  - `events.rs` - Event dispatch system
  - Core state management
- **`rendering.rs`** - Canvas rendering
- **`js_api.rs`** - JavaScript/WASM bridge
- **`lib.rs`** - Main entry point

### Key Dependencies (Rust -> Java equivalents)
- `wasm-bindgen` -> TeaVM (for JS compilation)
- `binary-reader` -> Custom `BinaryReader` class
- `flate2` -> Apache Commons Compress
- `chrono` -> `java.time` / Joda-Time
- `pest` (parser) -> Custom parser or ANTLR
- `image` -> Java ImageIO / imgscalr

## Current Progress

### Completed ✓
1. **Project setup** - Maven project with pom.xml
2. **IO module** - `BinaryReader.java`, `ListReaders.java`
3. **Director enums** - `MemberType`, `ScriptType`, `ShapeType`, `BoxType`, `Alignment`, etc.
4. **Director info classes** - `BitmapInfo`, `ShapeInfo`, `FieldInfo`, `FilmLoopInfo`, `SoundInfo`, `FontInfo`, `TextMemberData`
5. **Lingo core types**:
   - `OpCode.java` - All bytecode opcodes
   - `LingoConstants.java` - Property name mappings
   - `DatumType.java` - Value type enumeration
   - `Datum.java` - Runtime value class
   - `StringChunkType.java`, `StringChunkExpr.java`
   - `ScriptContext.java`
6. **Chunk parsers (24+ types)**:
   - `BasicListChunk.java`, `BitmapChunk.java`
   - `CastChunk.java`, `CastInfoChunk.java`, `CastListChunk.java`
   - `CastMemberChunk.java`, `CastMemberInfoChunk.java`, `CastMemberSpecificData.java`
   - `Chunk.java` (container/factory), `ConfigChunk.java`
   - `EffectChunk.java`, `FrameLabelsChunk.java`
   - `HandlerRecord.java`, `InitialMapChunk.java`, `KeyTableChunk.java`
   - `MediaChunk.java`, `PaletteChunk.java`
   - `ScoreChunk.java`, `ScoreFrameData.java`, `ScoreFrameChannelData.java`
   - `ScriptContextChunk.java`, `ScriptNamesChunk.java`, `ScriptChunk.java`
   - `SordChunk.java`, `SoundChunk.java`, `SoundChannelData.java`
   - `TempoChannelData.java`, `TextChunk.java`, `ThumChunk.java`, `XMediaChunk.java`
7. **Director file parser**:
   - `DirectorFile.java` - Main file parser with Afterburner map support
   - `RIFXReaderContext.java` - Parser state
   - `GuidConstants.java` - Compression GUIDs
   - `CastDef.java` - Cast library definition
   - `MoaID.java` - MOA GUID class
   - `Utils.java` - FOURCC and version utilities
8. **Player core infrastructure**:
   - `DirPlayer.java` - Main player class
   - `Movie.java` - Movie state
   - `IntRect.java` - Rectangle class
   - `CastManager.java`, `CastLib.java`, `CastMember.java`
   - `Score.java` - Timeline/score
   - `Sprite.java` - Sprite state
   - `NetManager.java` - Network requests
   - `TimeoutManager.java` - Timeout handling
   - `FontManager.java` - Font management
   - `KeyboardManager.java` - Input handling
   - `SoundManager.java` - Audio playback
   - `DatumAllocator.java` - Value allocation
   - `BitmapManager.java` - Bitmap/palette management
   - `DateObject.java`, `MathObject.java` - Built-in objects
   - `XmlNode.java`, `XmlDocument.java` - XML support
9. **Supporting classes**:
   - `ScriptError.java`
   - `CastMemberRef.java`
   - `ColorRef.java`
   - `CursorRef.java`
   - `BitmapRef.java`
   - `StaticDatum.java`
   - `Utils.java`

### In Progress
- Player bytecode executor
- Player handlers (built-in functions)

### Remaining Work
1. **Lingo decompiler** (`director/lingo/decompiler/`)
   - AST representation
   - Handler decompilation
   - Code generation

2. **Remaining chunk parsers** (~20 types):
   - Bitmap, Cast, CastInfo, CastList
   - Config, Effect, Imap, KeyTable
   - Lctx, Media, Palette, Score
   - ScoreOrder, ScriptNames, Sound
   - Text, Thum, Xmedia, etc.

3. **Director file reader** (`director/file.rs`)
   - RIFX parsing
   - Afterburner map reading
   - Chunk container management

4. **Player components**:
   - Bytecode executor (`player/bytecode/`)
   - Event system (`player/events.rs`)
   - Built-in handlers (`player/handlers/`)
   - Bitmap operations (`player/bitmap/`)
   - Score keyframes
   - Geometry utilities

5. **Rendering system** (`rendering.rs`)
   - Canvas compositing
   - Ink effects
   - Stage rendering

6. **JavaScript bridge** (`js_api.rs`)
   - TeaVM JSO bindings
   - Event callbacks
   - Data serialization

7. **Testing & verification**
   - Compile all code
   - Unit tests
   - Integration tests with sample DCR files

## Package Structure (Java)
```
com.dirplayer
├── io/
│   ├── BinaryReader.java
│   └── ListReaders.java
├── director/
│   ├── chunks/
│   │   ├── Bytecode.java
│   │   ├── HandlerDef.java
│   │   ├── LiteralStore.java
│   │   ├── LiteralType.java
│   │   ├── ScriptChunk.java
│   │   └── ... (more chunk types)
│   ├── lingo/
│   │   ├── decompiler/
│   │   ├── Datum.java
│   │   ├── DatumType.java
│   │   ├── LingoConstants.java
│   │   ├── OpCode.java
│   │   ├── ScriptContext.java
│   │   ├── StringChunkExpr.java
│   │   └── StringChunkType.java
│   ├── Alignment.java
│   ├── BitmapInfo.java
│   ├── BoxType.java
│   ├── FieldInfo.java
│   ├── FilmLoopInfo.java
│   ├── FontInfo.java
│   ├── MemberType.java
│   ├── ScriptType.java
│   ├── ShapeInfo.java
│   ├── ShapeType.java
│   ├── SoundInfo.java
│   ├── StaticDatum.java
│   └── TextMemberData.java
├── player/
│   ├── bitmap/
│   │   └── BitmapRef.java
│   ├── bytecode/
│   ├── handlers/
│   ├── score/
│   │   └── Score.java
│   ├── CastLib.java
│   ├── CastManager.java
│   ├── CastMember.java
│   ├── CastMemberRef.java
│   ├── ColorRef.java
│   ├── CursorRef.java
│   ├── DatumAllocator.java
│   ├── DirPlayer.java
│   ├── FontManager.java
│   ├── IntRect.java
│   ├── KeyboardManager.java
│   ├── Movie.java
│   ├── NetManager.java
│   ├── ScriptError.java
│   ├── SoundManager.java
│   ├── Sprite.java
│   └── TimeoutManager.java
├── DirPlayer.java (main entry)
└── Utils.java
```

## Build
```bash
cd vm-java
mvn clean compile
mvn package
```

## Notes
- The original Rust code uses `wasm-bindgen` for browser integration; Java uses TeaVM
- Async operations in Rust use `async-std`; Java uses `CompletableFuture` or RxJava
- Rust enums with data are converted to Java classes with type fields
- Rust's `Option<T>` becomes nullable types or `Optional<T>` in Java
- Rust traits become Java interfaces

## Files Count
- Original Rust: 141 .rs files, ~60,000+ lines
- Current Java: ~70 .java files created, ~25,000+ lines

## Last Updated
2026-01-26
