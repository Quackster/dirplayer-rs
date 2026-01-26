package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import com.dirplayer.SimpleLogger;


/**
 * Sound chunk - contains audio data.
 * Port of Rust SoundChunk struct.
 */
public class SoundChunk {
    private static final SimpleLogger logger = SimpleLogger.getLogger(SoundChunk.class);

    private int channels;
    private int sampleRate;
    private int bitsPerSample;
    private int sampleCount;
    private String codec;
    private byte[] data;
    public int version;

    public SoundChunk() {
        this.channels = 1;
        this.sampleRate = 22050;
        this.bitsPerSample = 16;
        this.sampleCount = 0;
        this.codec = "raw_pcm";
        this.data = new byte[0];
        this.version = 0;
    }

    public SoundChunk(byte[] data) {
        this.channels = 1;
        this.sampleRate = 16000;
        this.bitsPerSample = 16;
        this.sampleCount = 0;
        this.codec = "raw_pcm";
        this.data = data;
        this.version = 0;
    }

    public int getChannels() { return channels; }
    public int getSampleRate() { return sampleRate; }
    public int getBitsPerSample() { return bitsPerSample; }
    public int getSampleCount() { return sampleCount; }
    public String getCodec() { return codec; }
    public byte[] getData() { return data.clone(); }

    public void setMetadata(int sampleRate, int channels, int bitsPerSample) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.sampleCount = data.length / (channels * (bitsPerSample / 8));
        logger.info("Updated metadata: channels={}, sample_rate={}, bits={}",
            channels, sampleRate, bitsPerSample);
    }

    public static SoundChunk fromSndChunk(BinaryReader reader, int version) {
        // Read all data for hex dump
        int rBegin = reader.getPos();
        byte[] dataTest = reader.readBytes(reader.getLength() - reader.getPos());
        reader.setPos(rBegin);

        logger.debug("Parsing Director MX 2004 snd chunk");

        ByteOrder originalEndian = reader.getEndian();
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        int readStart = reader.getPos();

        // Skip header reading for logging, reset position
        reader.setPos(readStart);

        // Read candidate sample rates
        reader.readU32(); // Skip 0x00-0x03

        // Offset 0x04: Sample Rate ENCODED
        int sampleRateEncoded = (int) reader.readU32();
        int rateA = (int) Math.round(sampleRateEncoded / 6.144);
        if (rateA > 15990 && rateA < 16020) {
            rateA = 16000;
        }

        // Skip to 0x16
        for (int i = 0; i < 14; i++) {
            reader.readU8();
        }

        // Offset 0x16: Sample Rate u16
        int rateB = reader.readU16();

        // Skip to 0x2A
        for (int i = 0; i < 0x2A - 0x18; i++) {
            reader.readU8();
        }

        // Offset 0x2A: Sample Rate u16
        int rateC = reader.readU16();

        // Determine final format
        int sampleRate;
        int bitsPerSample;
        int channels;

        if (rateB == 22050) {
            sampleRate = 22050;
            bitsPerSample = 16;
            channels = 1;
        } else if (rateC == 44100) {
            sampleRate = 44100;
            bitsPerSample = 16;
            channels = 1;
        } else {
            sampleRate = rateA;
            bitsPerSample = 16;
            channels = 1;
        }

        logger.debug("Selected Format: {} Hz, {}-bit, {} channels (RateA={}, RateB={}, RateC={})",
            sampleRate, bitsPerSample, channels, rateA, rateB, rateC);

        // Skip remaining header (to 0x40)
        for (int i = 0; i < 0x40 - 0x2C; i++) {
            reader.readU8();
        }

        // Read audio data
        byte[] data = reader.readBytes(reader.getLength() - reader.getPos());

        reader.setEndian(originalEndian);

        if (data.length == 0) {
            logger.warn("snd chunk contains no audio data after header");
        }

        // Detect codec (MP3 vs PCM)
        boolean isMp3 = data.length >= 2 && (data[0] & 0xFF) == 0xFF && ((data[1] & 0xF0) == 0xF0);
        String codec = isMp3 ? "mp3" : "raw_pcm";

        // Calculate sample count
        int sampleCount;
        if (isMp3) {
            sampleCount = 0; // Unknown until decoded
        } else {
            int bytesPerSample = bitsPerSample / 8;
            int bytesPerFrame = channels * bytesPerSample;
            sampleCount = data.length / bytesPerFrame;
        }

        double duration = isMp3 ? 0.0 : (double) sampleCount / sampleRate;

        logger.debug("Final snd: {} Hz, {}-bit, codec={}, {} bytes -> {} samples, {:.3}s",
            sampleRate, bitsPerSample, codec, data.length, sampleCount, duration);

        SoundChunk chunk = new SoundChunk();
        chunk.channels = channels;
        chunk.sampleRate = sampleRate;
        chunk.bitsPerSample = bitsPerSample;
        chunk.sampleCount = sampleCount;
        chunk.codec = codec;
        chunk.data = dataTest;
        chunk.version = version;

        return chunk;
    }

    public byte[] toWav() {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;

        // Calculate WAV size
        int wavSize = 44 + data.length;
        byte[] wav = new byte[wavSize];
        int pos = 0;

        // RIFF header
        System.arraycopy("RIFF".getBytes(), 0, wav, pos, 4); pos += 4;
        writeU32LE(wav, pos, 36 + data.length); pos += 4;
        System.arraycopy("WAVE".getBytes(), 0, wav, pos, 4); pos += 4;

        // fmt subchunk
        System.arraycopy("fmt ".getBytes(), 0, wav, pos, 4); pos += 4;
        writeU32LE(wav, pos, 16); pos += 4;
        writeU16LE(wav, pos, 1); pos += 2; // PCM
        writeU16LE(wav, pos, channels); pos += 2;
        writeU32LE(wav, pos, sampleRate); pos += 4;
        writeU32LE(wav, pos, byteRate); pos += 4;
        writeU16LE(wav, pos, blockAlign); pos += 2;
        writeU16LE(wav, pos, bitsPerSample); pos += 2;

        // data subchunk
        System.arraycopy("data".getBytes(), 0, wav, pos, 4); pos += 4;
        writeU32LE(wav, pos, data.length); pos += 4;

        // Audio data - swap endianness for 16-bit
        if (bitsPerSample == 16) {
            for (int i = 0; i < data.length - 1; i += 2) {
                wav[pos++] = data[i + 1];
                wav[pos++] = data[i];
            }
        } else {
            System.arraycopy(data, 0, wav, pos, data.length);
        }

        return wav;
    }

    private static void writeU32LE(byte[] arr, int pos, int value) {
        arr[pos] = (byte) (value & 0xFF);
        arr[pos + 1] = (byte) ((value >> 8) & 0xFF);
        arr[pos + 2] = (byte) ((value >> 16) & 0xFF);
        arr[pos + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void writeU16LE(byte[] arr, int pos, int value) {
        arr[pos] = (byte) (value & 0xFF);
        arr[pos + 1] = (byte) ((value >> 8) & 0xFF);
    }

    public static SoundChunk fromMedia(MediaChunk media) {
        String codec = media.getCodecName();

        int sampleCount;
        int bitsPerSample;

        if (codec.equals("ima_adpcm")) {
            sampleCount = media.dataSizeField;
            bitsPerSample = 16;
        } else if (codec.equals("mp3")) {
            sampleCount = 0;
            bitsPerSample = 0;
        } else {
            sampleCount = media.audioData.length / 2;
            bitsPerSample = 16;
        }

        logger.debug("from_media: codec={}, data_size_field={}, audio_data.len={}, sample_count={}",
            codec, media.dataSizeField, media.audioData.length, sampleCount);

        SoundChunk chunk = new SoundChunk();
        chunk.channels = 1;
        chunk.sampleRate = media.sampleRate;
        chunk.bitsPerSample = bitsPerSample;
        chunk.sampleCount = sampleCount;
        chunk.codec = codec;
        chunk.data = media.audioData.clone();
        chunk.version = 0;

        return chunk;
    }
}
