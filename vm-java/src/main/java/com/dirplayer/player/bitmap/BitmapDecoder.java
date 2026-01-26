package com.dirplayer.player.bitmap;

import com.dirplayer.director.BitmapInfo;
import com.dirplayer.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bitmap decompression and decoding utilities.
 * Port of Rust bitmap decompression functions.
 */
public class BitmapDecoder {
    private static final Logger logger = LoggerFactory.getLogger(BitmapDecoder.class);

    /**
     * Decompress and decode a Director bitmap.
     */
    public static Bitmap decompressBitmap(byte[] data, BitmapInfo info, int castLib, int version) {
        int numChannels = getNumChannels(info.bitDepth);
        int alignmentWidth = getAlignmentWidth(info.bitDepth);

        BinaryReader reader = BinaryReader.fromBytes(data);

        int scanHeight = info.height;
        int scanWidth;
        if (info.width % alignmentWidth == 0) {
            scanWidth = info.width;
        } else {
            scanWidth = alignmentWidth * ((info.width + alignmentWidth - 1) / alignmentWidth);
        }

        int expectedLen = calculateExpectedLength(info, scanWidth, scanHeight, numChannels, version);
        boolean skipCompression = data.length >= expectedLen;

        byte[] result;
        if (skipCompression) {
            result = new byte[expectedLen];
            System.arraycopy(data, 0, result, 0, Math.min(data.length, expectedLen));
        } else {
            result = decompressRle(reader, expectedLen);
        }

        // Recalculate scanWidth based on result size
        if (result.length == info.width * info.height * numChannels) {
            scanWidth = info.width;
        } else if (info.bitDepth == 32 && version >= 400) {
            scanWidth = info.width;
        }

        Bitmap bitmap;
        switch (info.bitDepth) {
            case 1:
                bitmap = decodeBitmap1bit(info.width, info.height, scanWidth, scanHeight,
                    PaletteRef.from(info.paletteId, castLib), result);
                break;
            case 2:
                bitmap = decodeBitmap2bit(info.width, info.height, scanWidth, scanHeight,
                    PaletteRef.from(info.paletteId, castLib), result);
                break;
            case 4:
                bitmap = decodeBitmap4bit(info.width, info.height, scanWidth, scanHeight,
                    PaletteRef.from(info.paletteId, castLib), result);
                break;
            case 8:
                bitmap = decodeGenericBitmap(info.width, info.height, 8, 1, scanWidth, scanHeight,
                    PaletteRef.from(info.paletteId, castLib), result);
                break;
            case 16:
                bitmap = decodeBitmap16bit(info.width, info.height, scanWidth, scanHeight,
                    PaletteRef.from(info.paletteId, castLib), result, skipCompression);
                break;
            case 32:
                bitmap = decodeBitmap32bit(info, scanWidth, result, castLib, version);
                break;
            default:
                logger.warn("Unsupported bit depth: {}", info.bitDepth);
                bitmap = new Bitmap();
                bitmap.width = info.width;
                bitmap.height = info.height;
                bitmap.bitDepth = 8;
                bitmap.data = new byte[info.width * info.height];
                bitmap.paletteRef = PaletteRef.from(info.paletteId, castLib);
        }

        bitmap.useAlpha = info.useAlpha;
        bitmap.trimWhiteSpace = info.trimWhiteSpace;
        return bitmap;
    }

    private static int calculateExpectedLength(BitmapInfo info, int scanWidth, int scanHeight, int numChannels, int version) {
        if (info.bitDepth == 32 && version >= 400) {
            return info.width * scanHeight * numChannels;
        } else if (info.bitDepth == 1) {
            return (scanWidth / 8) * scanHeight;
        } else if (info.bitDepth == 2) {
            return (scanWidth / 4) * scanHeight;
        } else if (info.bitDepth == 4) {
            return (scanWidth / 2) * scanHeight;
        } else {
            return scanWidth * scanHeight * numChannels;
        }
    }

    private static byte[] decompressRle(BinaryReader reader, int expectedLen) {
        byte[] result = new byte[expectedLen];
        int pos = 0;

        while (pos < expectedLen && !reader.eof()) {
            int control = reader.readU8();

            if (control < 0x80) {
                int count = control + 1;
                for (int i = 0; i < count && pos < expectedLen && !reader.eof(); i++) {
                    result[pos++] = (byte) reader.readU8();
                }
            } else {
                int count = 257 - control;
                if (reader.eof()) break;
                byte val = (byte) reader.readU8();
                for (int i = 0; i < count && pos < expectedLen; i++) {
                    result[pos++] = val;
                }
            }
        }

        return result;
    }

    private static int getNumChannels(int bitDepth) {
        switch (bitDepth) {
            case 1:
            case 2:
            case 4:
            case 8:
                return 1;
            case 16:
                return 2;
            case 32:
                return 4;
            default:
                return 1;
        }
    }

    private static int getAlignmentWidth(int bitDepth) {
        switch (bitDepth) {
            case 1:
                return 16;
            case 4:
            case 32:
                return 4;
            case 2:
            case 8:
                return 2;
            case 16:
                return 1;
            default:
                return 1;
        }
    }

    private static Bitmap decodeBitmap1bit(int width, int height, int scanWidth, int scanHeight,
                                           PaletteRef paletteRef, byte[] data) {
        // Decode 1-bit to 8-bit indexed
        byte[] scanData = new byte[data.length * 8];
        int p = 0;
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            for (int j = 1; j <= 8; j++) {
                int bit = (b & (0x1 << (8 - j))) >> (8 - j);
                scanData[p++] = (bit == 1) ? (byte) 0xFF : (byte) 0x00;
            }
        }

        byte[] result = new byte[width * height];
        for (int y = 0; y < scanHeight && y < height; y++) {
            for (int x = 0; x < scanWidth && x < width; x++) {
                int scanIndex = y * scanWidth + x;
                if (scanIndex < scanData.length && x < width) {
                    int pixelIndex = y * width + x;
                    result[pixelIndex] = scanData[scanIndex];
                }
            }
        }

        Bitmap bitmap = new Bitmap();
        bitmap.width = width;
        bitmap.height = height;
        bitmap.bitDepth = 8;
        bitmap.originalBitDepth = 1;
        bitmap.data = result;
        bitmap.paletteRef = paletteRef;
        return bitmap;
    }

    private static Bitmap decodeBitmap2bit(int width, int height, int scanWidth, int scanHeight,
                                           PaletteRef paletteRef, byte[] data) {
        byte[] decodedData = new byte[data.length * 4];
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            decodedData[i * 4] = (byte) (((b & 0xC0) >> 6) * 85);
            decodedData[i * 4 + 1] = (byte) (((b & 0x30) >> 4) * 85);
            decodedData[i * 4 + 2] = (byte) (((b & 0x0C) >> 2) * 85);
            decodedData[i * 4 + 3] = (byte) ((b & 0x03) * 85);
        }

        byte[] result = new byte[width * height];
        for (int y = 0; y < scanHeight && y < height; y++) {
            for (int x = 0; x < scanWidth && x < width; x++) {
                int srcIndex = y * scanWidth + x;
                if (srcIndex < decodedData.length && x < width) {
                    result[y * width + x] = decodedData[srcIndex];
                }
            }
        }

        Bitmap bitmap = new Bitmap();
        bitmap.width = width;
        bitmap.height = height;
        bitmap.bitDepth = 8;
        bitmap.originalBitDepth = 2;
        bitmap.data = result;
        bitmap.paletteRef = paletteRef;
        return bitmap;
    }

    private static Bitmap decodeBitmap4bit(int width, int height, int scanWidth, int scanHeight,
                                           PaletteRef paletteRef, byte[] data) {
        byte[] decodedData = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            decodedData[i * 2] = (byte) ((b & 0xF0) >> 4);
            decodedData[i * 2 + 1] = (byte) (b & 0x0F);
        }

        byte[] result = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int scanIndex = y * scanWidth + x;
                if (scanIndex < decodedData.length) {
                    result[y * width + x] = decodedData[scanIndex];
                }
            }
        }

        Bitmap bitmap = new Bitmap();
        bitmap.width = width;
        bitmap.height = height;
        bitmap.bitDepth = 8;
        bitmap.originalBitDepth = 4;
        bitmap.data = result;
        bitmap.paletteRef = paletteRef;
        return bitmap;
    }

    private static Bitmap decodeBitmap16bit(int width, int height, int scanWidth, int scanHeight,
                                            PaletteRef paletteRef, byte[] data, boolean skipCompression) {
        byte[] result = new byte[width * height * 4];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel16;
                if (skipCompression) {
                    int offset = (y * scanWidth + x) * 2;
                    if (offset + 1 < data.length) {
                        int high = data[offset] & 0xFF;
                        int low = data[offset + 1] & 0xFF;
                        pixel16 = (high << 8) | low;
                    } else {
                        pixel16 = 0;
                    }
                } else {
                    int rowOffset = y * scanWidth * 2;
                    if (rowOffset + x < data.length && rowOffset + scanWidth + x < data.length) {
                        int high = data[rowOffset + x] & 0xFF;
                        int low = data[rowOffset + scanWidth + x] & 0xFF;
                        pixel16 = (high << 8) | low;
                    } else {
                        pixel16 = 0;
                    }
                }

                // RGB555
                int r5 = (pixel16 >> 10) & 0x1F;
                int g5 = (pixel16 >> 5) & 0x1F;
                int b5 = pixel16 & 0x1F;

                int dst = (y * width + x) * 4;
                result[dst] = (byte) ((r5 << 3) | (r5 >> 2));
                result[dst + 1] = (byte) ((g5 << 3) | (g5 >> 2));
                result[dst + 2] = (byte) ((b5 << 3) | (b5 >> 2));
                result[dst + 3] = (byte) 255;
            }
        }

        Bitmap bitmap = new Bitmap();
        bitmap.width = width;
        bitmap.height = height;
        bitmap.bitDepth = 32;
        bitmap.originalBitDepth = 16;
        bitmap.data = result;
        bitmap.paletteRef = paletteRef;
        return bitmap;
    }

    private static Bitmap decodeBitmap32bit(BitmapInfo info, int scanWidth, byte[] data, int castLib, int version) {
        boolean skipCompression = version < 300
            ? data.length >= (info.width * info.height * 4)
            : (version < 400 && data.length == (info.width * info.height * 4));

        byte[] finalData = new byte[info.width * info.height * 4];

        if (skipCompression) {
            // Direct ARGB format
            for (int y = 0; y < info.height; y++) {
                for (int x = 0; x < info.width; x++) {
                    int srcIdx = (y * scanWidth + x) * 4;
                    int dstIdx = (y * info.width + x) * 4;
                    if (srcIdx + 3 < data.length) {
                        finalData[dstIdx] = data[srcIdx + 1];     // R
                        finalData[dstIdx + 1] = data[srcIdx + 2]; // G
                        finalData[dstIdx + 2] = data[srcIdx + 3]; // B
                        finalData[dstIdx + 3] = data[srcIdx];     // A
                    }
                }
            }
        } else {
            // D4+ format: planar ARGB per scanline
            for (int y = 0; y < info.height; y++) {
                for (int x = 0; x < info.width; x++) {
                    int lineOffset = y * scanWidth * 4;
                    int pixelIdx = (y * info.width + x) * 4;

                    if (lineOffset + x + 3 * scanWidth < data.length) {
                        byte a = data[lineOffset + x];
                        byte r = data[lineOffset + x + scanWidth];
                        byte g = data[lineOffset + x + 2 * scanWidth];
                        byte b = data[lineOffset + x + 3 * scanWidth];

                        finalData[pixelIdx] = r;
                        finalData[pixelIdx + 1] = g;
                        finalData[pixelIdx + 2] = b;
                        finalData[pixelIdx + 3] = a;
                    }
                }
            }
        }

        Bitmap bitmap = new Bitmap();
        bitmap.width = info.width;
        bitmap.height = info.height;
        bitmap.bitDepth = 32;
        bitmap.originalBitDepth = 32;
        bitmap.data = finalData;
        bitmap.paletteRef = PaletteRef.from(info.paletteId, castLib);
        bitmap.useAlpha = info.useAlpha;
        bitmap.trimWhiteSpace = info.trimWhiteSpace;
        return bitmap;
    }

    private static Bitmap decodeGenericBitmap(int width, int height, int bitDepth, int numChannels,
                                              int scanWidth, int scanHeight, PaletteRef paletteRef, byte[] data) {
        int bytesPerPixel = bitDepth / 8;
        byte[] result = new byte[width * height * numChannels * bytesPerPixel];

        for (int y = 0; y < scanHeight && y < height; y++) {
            for (int x = 0; x < scanWidth && x < width; x++) {
                for (int c = 0; c < numChannels; c++) {
                    for (int b = 0; b < bytesPerPixel; b++) {
                        int scanIndex = y * scanWidth * numChannels * bytesPerPixel
                                      + x * numChannels * bytesPerPixel
                                      + c * bytesPerPixel + b;
                        int resultIndex = y * width * numChannels * bytesPerPixel
                                        + x * numChannels * bytesPerPixel
                                        + c * bytesPerPixel + b;

                        if (scanIndex < data.length && resultIndex < result.length) {
                            result[resultIndex] = data[scanIndex];
                        }
                    }
                }
            }
        }

        Bitmap bitmap = new Bitmap();
        bitmap.width = width;
        bitmap.height = height;
        bitmap.bitDepth = bitDepth * numChannels;
        bitmap.originalBitDepth = bitDepth;
        bitmap.data = result;
        bitmap.paletteRef = paletteRef;
        return bitmap;
    }
}
