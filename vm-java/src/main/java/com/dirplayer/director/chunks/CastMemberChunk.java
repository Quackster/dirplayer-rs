package com.dirplayer.director.chunks;

import com.dirplayer.director.BitmapInfo;
import com.dirplayer.director.FieldInfo;
import com.dirplayer.director.FilmLoopInfo;
import com.dirplayer.director.MemberType;
import com.dirplayer.director.ScriptType;
import com.dirplayer.director.ShapeInfo;
import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import com.dirplayer.SimpleLogger;


/**
 * Cast member chunk - contains cast member type and specific data.
 * Port of Rust CastMemberChunk struct.
 */
public class CastMemberChunk {
    private static final SimpleLogger logger = SimpleLogger.getLogger(CastMemberChunk.class);

    public MemberType memberType;
    public CastMemberSpecificData specificData;
    public byte[] specificDataRaw;
    public CastMemberInfoChunk memberInfo;

    public CastMemberChunk() {
        this.memberType = MemberType.NULL;
        this.specificData = CastMemberSpecificData.none();
        this.specificDataRaw = new byte[0];
        this.memberInfo = null;
    }

    public static CastMemberChunk fromReader(BinaryReader reader, int dirVersion) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        CastMemberChunk chunk = new CastMemberChunk();

        // Debug: read full chunk for logging
        int rBegin = reader.getPos();
        byte[] dataTest = reader.readBytes(reader.getLength() - reader.getPos());
        if (logger.isDebugEnabled()) {
            StringBuilder hexDump = new StringBuilder();
            for (byte b : dataTest) {
                hexDump.append(String.format("%02X ", b & 0xFF));
            }
            logger.debug("CASt (Full Chunk, {} bytes):\n{}", dataTest.length, hexDump);
        }
        reader.setPos(rBegin);

        int infoLen;
        byte[] specificData;
        int specificDataLen;
        MemberType memberType;
        boolean hasFlags1 = false;

        if (dirVersion >= 500) {
            memberType = MemberType.fromValue((int) reader.readU32());
            infoLen = (int) reader.readU32();
            specificDataLen = (int) reader.readU32();

            // info
            if (infoLen != 0) {
                byte[] infoData = reader.readBytes(infoLen);
                BinaryReader infoReader = new BinaryReader(infoData);
                infoReader.setEndian(reader.getEndian());
                chunk.memberInfo = CastMemberInfoChunk.read(infoReader, dirVersion);
            }

            // specific data
            specificData = reader.readBytes(specificDataLen);
        } else {
            specificDataLen = reader.readU16();
            infoLen = (int) reader.readU32();

            // these bytes are common but stored in the specific data
            int specificDataLeft = specificDataLen;
            memberType = MemberType.fromValue(reader.readU8());
            specificDataLeft -= 1;

            if (specificDataLeft > 0) {
                hasFlags1 = true;
                int flags1 = reader.readU8();
                specificDataLeft -= 1;
            }

            // specific data
            specificData = reader.readBytes(specificDataLeft);

            // info
            if (infoLen != 0) {
                byte[] infoData = reader.readBytes(infoLen);
                BinaryReader infoReader = new BinaryReader(infoData);
                infoReader.setEndian(reader.getEndian());
                chunk.memberInfo = CastMemberInfoChunk.read(infoReader, dirVersion);
            }
        }

        chunk.memberType = memberType;
        chunk.specificDataRaw = specificData;

        // Parse specific data based on member type
        BinaryReader specificReader = new BinaryReader(specificData);
        specificReader.setEndian(reader.getEndian());

        switch (memberType) {
            case Script:
                int scriptTypeVal = specificReader.readU16();
                chunk.specificData = CastMemberSpecificData.script(ScriptType.fromValue(scriptTypeVal));
                break;
            case Bitmap:
                chunk.specificData = CastMemberSpecificData.bitmap(BitmapInfo.from(specificData));
                break;
            case Shape:
                chunk.specificData = CastMemberSpecificData.shape(ShapeInfo.from(specificData));
                break;
            case FilmLoop:
                chunk.specificData = CastMemberSpecificData.filmLoop(FilmLoopInfo.from(specificData));
                break;
            case Sound:
                chunk.specificData = CastMemberSpecificData.none();
                break;
            case Text:
                chunk.specificData = CastMemberSpecificData.field(FieldInfo.from(specificData));
                break;
            default:
                chunk.specificData = CastMemberSpecificData.none();
                break;
        }

        return chunk;
    }
}
