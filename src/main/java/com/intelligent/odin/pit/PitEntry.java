package com.intelligent.odin.pit;

public class PitEntry {

    // Standard PIT Entry is usually ~132 bytes
    public int binaryType;
    public int deviceType;
    public int identifier;
    public int attributes;
    public int updateAttributes;
    public int blockSizeOrOffset; // Often 512-byte blocks
    public int blockCount;
    public int fileOffset; // In blocks
    public int fileSystemType;

    public String partitionName; // Max 32 chars
    public String filename; // Max 32 chars
    public String fotaFilename; // Max 32 chars

    @Override
    public String toString() {
        return String.format("Part: %-20s | File: %-20s | Blk: %d", partitionName, filename, blockCount);
    }
}
