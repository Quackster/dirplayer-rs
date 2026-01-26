package com.dirplayer.director;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text member data structure.
 * Port of Rust TextMemberData struct.
 */
public class TextMemberData {
    private static final Logger logger = LoggerFactory.getLogger(TextMemberData.class);

    public long width;
    public long height;
    public TexSection texSection;

    public TextMemberData() {
        this.width = 0;
        this.height = 0;
        this.texSection = null;
    }

    public static TextMemberData fromRawBytes(byte[] bytes) {
        if (bytes.length < 8) {
            logger.debug("TextMemberData: too short ({} bytes)", bytes.length);
            return null;
        }

        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        // Read outer structure
        long length = reader.readU32();
        String fourcc = reader.readFourCC();

        if (!"text".equals(fourcc)) {
            return null;
        }

        long dataLength = reader.readU32();
        logger.debug("Text member data length: {}", dataLength);

        // Skip zeros to find actual data (36 bytes of padding)
        reader.skip(36);

        TextMemberData data = new TextMemberData();

        // Read dimensions and counts
        data.width = reader.readU32();
        data.height = reader.readU32();
        logger.debug("Dimensions: {}x{}", data.width, data.height);

        long count1 = reader.readU32();
        long size1 = reader.readU32();
        logger.debug("Format count: {}, Size: {}", count1, size1);

        long count2 = reader.readU32();
        long size2 = reader.readU32();
        logger.debug("Run count: {}, Size: {}", count2, size2);

        long charCount = reader.readU32();
        long size3 = reader.readU32();
        logger.debug("Character count: {}, Size: {}", charCount, size3);

        // Read values before 3TEX
        reader.readU32();
        reader.readU32();
        String val3Str = reader.readFourCC();

        if (!"3TEX".equals(val3Str)) {
            logger.debug("Expected '3TEX', got '{}'", val3Str);
            return null;
        }

        logger.debug("Found 3TEX section");

        // Parse the 3TEX section
        data.texSection = parseTexSection(reader, charCount);

        return data;
    }

    private static TexSection parseTexSection(BinaryReader reader, long expectedCharCount) {
        TexSection tex = new TexSection();

        long texLength = reader.readU32();
        logger.debug("3TEX section length: {}", texLength);

        // Parse header
        tex.colorId = reader.readI32();
        tex.bgColorId = reader.readI32();
        tex.unknown1 = reader.readU32();
        tex.unknown2 = reader.readU32();
        tex.charCount = reader.readU32();
        tex.unknown3 = reader.readU32();
        tex.lineCount = reader.readU32();
        tex.unknown4 = reader.readU32();
        tex.unknown5 = reader.readU32();
        tex.unknown6 = reader.readU32();
        tex.unknown7 = reader.readU32();
        tex.textOffset = reader.readU32();

        // Read RGB colors
        tex.color1R = reader.readU8();
        tex.color1G = reader.readU8();
        tex.color1B = reader.readU8();
        tex.color2R = reader.readU8();
        tex.color2G = reader.readU8();
        tex.color2B = reader.readU8();
        tex.color3R = reader.readU8();
        tex.color3G = reader.readU8();
        tex.color3B = reader.readU8();

        // Padding and floats
        tex.padding1 = reader.readU32();
        tex.padding2 = reader.readU32();
        tex.padding3 = reader.readU32();
        tex.float1 = Float.intBitsToFloat((int) reader.readU32());
        tex.padding4 = reader.readU32();
        tex.padding5 = reader.readU32();
        tex.padding6 = reader.readU32();
        tex.float2 = Float.intBitsToFloat((int) reader.readU32());

        // Texture flag (usually 'NoTexture\0')
        byte[] noTextureBytes = reader.readBytes(9);
        String noTexture = new String(noTextureBytes).replace("\0", "");
        logger.debug("Texture flag: '{}'", noTexture);

        // Read actual text string
        tex.text = "";

        while (reader.bytesLeft() >= 8) {
            long chunkLen = reader.readU32();
            String chunkType = reader.readFourCC();

            if ("TEXT".equals(chunkType)) {
                byte[] textBytes = reader.readBytes((int) chunkLen);
                tex.text = new String(textBytes);
                logger.debug("Found TEXT chunk: '{}'", tex.text);
                break;
            } else {
                // Skip unknown chunk
                reader.skip((int) chunkLen);
            }
        }

        return tex;
    }

    public void logSummary() {
        logger.info("═══════════════════════════════════");
        logger.info("TEXT MEMBER DATA SUMMARY");
        logger.info("═══════════════════════════════════");
        logger.info("Dimensions:    {}x{}", width, height);

        if (texSection != null) {
            logger.info("───────────────────────────────────");
            logger.info("3TEX Section:");
            logger.info("  Color ID:      {} {}", texSection.colorId,
                texSection.colorId == -1 ? "(white FFFFFF)" : "");
            logger.info("  BG Color ID:   {}", texSection.bgColorId);
            logger.info("  Char Count:    {}", texSection.charCount);
            logger.info("  Line Count:    {}", texSection.lineCount);
            logger.info("  Text Offset:   {}", texSection.textOffset);
            logger.info("  Color 1:       RGB({}, {}, {})",
                texSection.color1R, texSection.color1G, texSection.color1B);
            logger.info("  Color 2:       RGB({}, {}, {})",
                texSection.color2R, texSection.color2G, texSection.color2B);
            logger.info("  Color 3:       RGB({}, {}, {})",
                texSection.color3R, texSection.color3G, texSection.color3B);
            logger.info("  Float 1:       {}", texSection.float1);
            logger.info("  Float 2:       {}", texSection.float2);

            if (texSection.text != null && !texSection.text.isEmpty()) {
                logger.info("  Text:          '{}'", texSection.text);
            } else {
                logger.info("  Text:          (in child Text chunk)");
            }
        }

        logger.info("═══════════════════════════════════");
    }

    public static class TexSection {
        public int colorId;
        public int bgColorId;
        public long unknown1;
        public long unknown2;
        public long charCount;
        public long unknown3;
        public long lineCount;
        public long unknown4;
        public long unknown5;
        public long unknown6;
        public long unknown7;
        public long textOffset;

        public int color1R, color1G, color1B;
        public int color2R, color2G, color2B;
        public int color3R, color3G, color3B;

        public long padding1, padding2, padding3;
        public float float1;
        public long padding4, padding5, padding6;
        public float float2;

        public String text;
    }
}
