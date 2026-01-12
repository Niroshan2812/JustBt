package com.intelligent.odin.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FirmwarePacketizer {

    private final File firmwareFile;
    private final int chunkSize;
    private FileInputStream fis;
    private long totalBytesRead = 0;

    public FirmwarePacketizer(File firmwareFile, int chunkSize) throws IOException {
        this.firmwareFile = firmwareFile;
        this.chunkSize = chunkSize;
        this.fis = new FileInputStream(firmwareFile);
    }

    public boolean hasNext() throws IOException {
        return fis.available() > 0;
    }

    public byte[] nextChunk() throws IOException {
        byte[] buffer = new byte[chunkSize];
        int read = fis.read(buffer);

        if (read == -1)
            return null;

        totalBytesRead += read;

        // Resize if last chunk is smaller
        if (read < chunkSize) {
            return Arrays.copyOf(buffer, read);
        }

        return buffer;
    }

    public void close() {
        try {
            if (fis != null)
                fis.close();
        } catch (Exception e) {
        }
    }

    public long getTotalBytesRead() {
        return totalBytesRead;
    }

    public long getFileSize() {
        return firmwareFile.length();
    }
}
