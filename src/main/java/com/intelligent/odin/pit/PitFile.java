package com.intelligent.odin.pit;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PitFile {

    private List<PitEntry> entries = new ArrayList<>();

    public List<PitEntry> getEntries() {
        return entries;
    }

    public static PitFile fromBytes(byte[] data) {
        PitFile pit = new PitFile();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // Header is typically 28 bytes
        // Magic: 0x12349876
        int magic = buf.getInt();
        if (magic != 0x12349876) {
            System.err.println("[PitFile] Invalid Magic: " + Integer.toHexString(magic));
            // Proceed anyway or return null
        }

        int count = buf.getInt(); // Entry count
        // Skip dummy fields in header (usually 5 ints)
        buf.position(28);

        for (int i = 0; i < count; i++) {
            PitEntry entry = new PitEntry();

            entry.binaryType = buf.getInt();
            entry.deviceType = buf.getInt();
            entry.identifier = buf.getInt();
            entry.attributes = buf.getInt();
            entry.updateAttributes = buf.getInt();
            entry.blockSizeOrOffset = buf.getInt();
            entry.blockCount = buf.getInt();
            entry.fileOffset = buf.getInt();
            entry.fileSystemType = buf.getInt();

            // Strings are fixed 32 bytes
            byte[] nameBytes = new byte[32];
            buf.get(nameBytes);
            entry.partitionName = new String(nameBytes, StandardCharsets.US_ASCII).trim();

            byte[] fileBytes = new byte[32];
            buf.get(fileBytes);
            entry.filename = new String(fileBytes, StandardCharsets.US_ASCII).trim();

            byte[] fotaBytes = new byte[32];
            buf.get(fotaBytes);
            entry.fotaFilename = new String(fotaBytes, StandardCharsets.US_ASCII).trim();

            pit.entries.add(entry);
        }

        return pit;
    }
}
