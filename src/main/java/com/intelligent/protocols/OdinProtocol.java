package com.intelligent.protocols;

import com.intelligent.service.DeviceManager;
import com.intelligent.core.UsbBridge.DeviceStatus;
import com.intelligent.odin.pit.PitFile;
import com.intelligent.odin.core.FirmwarePacketizer;

import java.io.File;

public class OdinProtocol {

    // Logic: Handshake with Download Mode
    public boolean performHandshake(DeviceManager manager) {
        System.out.println("[OdinProtocol] Handshake: Attempting to synchronize...");

        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.println("   [Attempt " + attempt + "] Sending 'ODIN'...");

            DeviceStatus status = new DeviceStatus();
            if (!manager.sendPacket(SamsungDefs.CMD_ODIN_HANDSHAKE, status)) {
                System.err.println("   -> Write Failed.");
                continue;
            }

            // Wait a bit
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }

            byte[] resp = manager.readPacket(1024, status);
            if (resp != null && resp.length > 0) {
                String reply = new String(resp).trim();
                System.out.print("   -> Received (" + resp.length + " bytes): ");
                for (byte b : resp)
                    System.out.print(String.format("%02X ", b));
                System.out.println(" | ASCII: " + reply);

                if (reply.contains("LOKE")) {
                    System.out.println("[OdinProtocol] >> Handshake Success! Device is Ready.");
                    return true;
                }
            } else {
                System.out.println("   -> No Response (Timeout)");
            }
        }

        System.out.println("[OdinProtocol] Handshake failed after 3 attempts. Assuming Session Active?");
        return true; // Proceed anyway in case session was already active
    }

    public PitFile getPit(DeviceManager manager) {
        System.out.println("[OdinProtocol] Requesting PIT Table (Strategy A: ASCII)...");
        DeviceStatus status = new DeviceStatus();

        // Strategy A: "PIT"
        manager.sendPacket(SamsungDefs.CMD_PIT_ASCII, status);
        byte[] rawPit = manager.readPacket(8192, status);

        if (rawPit == null || rawPit.length == 0) {
            System.out.println("[OdinProtocol] ASCII PIT Failed or Time-out. Trying Strategy B: HEX (0x80)...");
            manager.sendPacket(SamsungDefs.CMD_PIT_HEX, status);
            rawPit = manager.readPacket(8192, status);
        }

        if (rawPit != null && rawPit.length > 0) {
            System.out.println("[OdinProtocol] PIT Received (" + rawPit.length + " bytes). Parsing...");
            return PitFile.fromBytes(rawPit);
        } else {
            System.out.println("[OdinProtocol] PIT Read Failed/Timeout (Both Strategies).");
            return null;
        }
    }

    public boolean flashFile(DeviceManager manager, File file, int partitionId) {
        System.out.println("[OdinProtocol] Flashing " + file.getName() + " to Partition " + partitionId + "...");
        DeviceStatus status = new DeviceStatus();

        try {
            // Standard Odin Chunk Size is often 1MB
            FirmwarePacketizer packetizer = new FirmwarePacketizer(file, 1024 * 1024);

            // 1. Send Start Command (Dummy for now)
            // manager.sendPacket(cmd_flash_init, status);

            while (packetizer.hasNext()) {
                byte[] chunk = packetizer.nextChunk();
                if (!manager.sendPacket(chunk, status)) {
                    System.err.println("[OdinProtocol] Flash Write Error: " + status.getMessageString());
                    packetizer.close();
                    return false;
                }

                // Read ACK (Odin often ACKs every chunk)
                // manager.readPacket(.., status);

                // System.out.print(".");
            }

            packetizer.close();
            System.out.println("\n[OdinProtocol] Flash Complete.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
