# DirPlayer Java Port - Task Documentation

## Overview
This is a Java port of **DirPlayer**, a Shockwave/Director Player emulator originally written in Rust (~61,630 lines across 149 files). The project emulates the Macromedia/Adobe Director runtime environment, allowing playback of classic Shockwave content in modern browsers.

## Task
Port the entirety of the Rust project (`vm-rust/`) to Java (`vm-java/`) without changing any behavior or skipping any features.

## Current Status: TODO COMPLETION IN PROGRESS

### Statistics
- **Original Rust**: 149 .rs files, 61,630 lines
- **Java Port**: 263+ .java files
- **Build Status**: Compiles successfully
- **TODO Progress**: ~50 TODOs completed, ~40 remaining

### Completed TODOs (this session)
1. **HandlerManager.java** (31 TODOs) - COMPLETED
   - All network handlers (preloadNetThing, netDone, getNetText, getStreamStatus, netError, netTextResult, postNetText)
   - Script/member handlers (script, member, puppetSprite)
   - External params (externalParamName, externalParamValue)
   - Event handlers (stopEvent, sendSprite, sendAllSprites, updateStage)
   - Misc handlers (getPref, setPref, goToNetPage, puppetSound, cursor, timeout, image, xtra)
   - newInstance and callAncestor

2. **DirPlayer.java** (6 TODOs) - COMPLETED
   - getMemberProp/setMemberProp - now delegates to CastMemberRefHandlers
   - Xtra handler calls - now delegates to XtraHandlers
   - FilmLoop frame advancement
   - Bitmap hex print for debugging

3. **CastManager.java** (2 TODOs) - COMPLETED
   - getFieldValueByIdentifiers - now returns member text
   - palettes() - now extracts palette data from PaletteMember

4. **New Files Created**:
   - `XtraHandlers.java` - Handles Xtra method calls, property get/set

### In Progress
- **Renderer.java** - FilmLoop rendering, text rendering
- **EventDispatcher.java** - FilmLoop sprite collection

### Remaining TODOs by File
1. **Renderer.java** (~4 items): Text/Field rendering, FilmLoop rendering
2. **EventDispatcher.java** (~2 items): FilmLoop sprite collection
3. **Stage.java** (~2 items): Stage image getter
4. **Remaining files** (~15 items): Various edge cases

## Completed Modules

### 1. IO Module
- `BinaryReader.java` - Binary data reader with endian support
- `ListReaders.java` - List deserialization utilities

### 2. Director Module
- **Chunk Parsers (30+ types)**: BitmapChunk, CastChunk, ConfigChunk, ScoreChunk, ScriptChunk, etc.
- **Lingo Core**: OpCode, Datum, DatumType, LingoConstants, ScriptContext
- **Lingo Decompiler**: AstNode, DecompilerHandler, CodeWriter, Tokenizer
- **File Parser**: DirectorFile, RIFXReaderContext, CastDef, Afterburner map support
- **Enums & Info Classes**: MemberType, ScriptType, BitmapInfo, SoundInfo, etc.

### 3. Player Module
- **Core Runtime**: DirPlayer, Movie, Score, Sprite, CastManager, CastLib
- **Bytecode Execution**: BytecodeHandlerManager, GetSetBytecodeHandler, StackBytecodeHandler, FlowControlBytecodeHandler, etc.
- **Event System**: EventDispatcher with full event dispatch support
- **Datum Handlers (25+ types)**: ListHandlers, PropListHandlers, StringDatumHandlers, CastMemberRefHandlers, SpriteHandlers, XtraHandlers, etc.
- **Bitmap Module**: Bitmap, BitmapDrawing, BitmapDecoder, Palettes, PaletteRef
- **Score/Keyframes**: KeyframeUtils, SpritePathKeyframes, SpritePropertyUtils
- **Rendering**: StageRenderer, PlayerCanvasRenderer, FilmLoopRenderer, InkEffect
- **Support Systems**: SoundManager, FontManager, TimeoutManager, NetManager, KeyboardManager
- **Xtra Support**: XtraManager, MultiUserXtra, FileIOXtra, NetLingoXtra

### 4. JavaScript Bridge
- `JsApi.java` - TeaVM JSO bindings for browser integration

## Package Structure
```
com.dirplayer
├── io/                     # Binary readers
├── director/
│   ├── chunks/             # 30+ chunk type parsers
│   └── lingo/
│       └── decompiler/     # Lingo bytecode decompiler
├── player/
│   ├── bitmap/             # Bitmap rendering
│   ├── bytecode/           # Bytecode execution
│   ├── cast/               # Cast member data types
│   ├── commands/           # Command execution
│   ├── events/             # Event dispatch
│   ├── handlers/           # Built-in handlers
│   │   └── datum/          # Type-specific handlers (25+)
│   │       └── castmember/ # Cast member type handlers
│   ├── rendering/          # Canvas rendering
│   ├── score/              # Timeline/score management
│   ├── script/             # Script instances
│   ├── sound/              # Audio playback
│   ├── xml/                # XML parsing
│   └── xtra/               # Xtra support
├── rendering/              # Core rendering types
├── JsApi.java             # JavaScript bridge
└── SimpleLogger.java      # Logging utility
```

## Build

### Gradle (recommended)
```bash
cd vm-java
./gradlew compileJava
./gradlew build
```

### Maven
```bash
cd vm-java
mvn clean compile
mvn package
```

## Technical Notes

### Rust to Java Mapping
- `wasm-bindgen` -> TeaVM JSO bindings
- `async-std` -> Synchronous execution (no async needed for browser)
- Rust enums with data -> Java classes with type fields
- `Option<T>` -> Nullable types
- Rust traits -> Java interfaces
- `DatumRef` (u32) -> `int` reference IDs

### Key Design Decisions
- Datum allocation uses integer reference IDs (like original Rust)
- Bytecode execution is synchronous (simpler for Java/TeaVM)
- Event dispatch follows original Rust patterns
- Property access uses the same name constants as Rust

## Reference Files
When implementing remaining TODOs, refer to these Rust sources:
- `vm-rust/src/rendering.rs` - Full filmloop rendering implementation
- `vm-rust/src/player/handlers/movie.rs` - Movie handlers
- `vm-rust/src/player/handlers/net.rs` - Network handlers
- `vm-rust/src/player/events.rs` - Event dispatch

## Last Updated
2026-01-27 (TODO completion session in progress)
