package com.intelligent.protocols;

import com.intelligent.service.DeviceManager;
import com.intelligent.core.UsbBridge.DeviceStatus;

public class OdinProtocol {

    // Logic: Handshake with Download Mode
    public boolean performHandshake(DeviceManager manager) {
        System.out.println("[OdinProtocol] Sending ODIN Handshake...");

        DeviceStatus status = new DeviceStatus();

        // 1. Send "ODIN"
        if (!manager.sendPacket(SamsungDefs.CMD_ODIN_HANDSHAKE, status)) {
            System.err.println("[OdinProtocol] Write Failed: " + status.getMessageString());
            // Even if write fails (maybe), we try to read? No.
            return false;
        }

        // 2. Read Reply (Expect "LOKE")
        byte[] resp = manager.readPacket(1024, status);
        if (resp != null && resp.length > 0) {
            String reply = new String(resp).trim();
            System.out.println("[OdinProtocol] Raw Reply: " + reply);

            if (reply.contains("LOKE")) {
                System.out.println("[OdinProtocol] >> Handshake Success! Device is Ready.");
                return true;
            }
        }

        System.out.println("[OdinProtocol] Handshake Reply unclear or empty. Proceeding to PIT experiment anyway.");
        return true; // Use with caution
    }

    public void readPit(DeviceManager manager) {
        System.out.println("[OdinProtocol] Attempting to Read PIT...");
        DeviceStatus status = new DeviceStatus();

        // Try ASCII Request ("PIT")
        manager.sendPacket(SamsungDefs.CMD_PIT_ASCII, status);

        // Give it time
        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }

        byte[] pit1 = manager.readPacket(4096, status);
        if (pit1 != null) {
            System.out.println("[OdinProtocol] PIT READ SUCCESS (ASCII) bytes: " + pit1.length);
            System.out.println(new String(pit1));
        } else {
            System.out.println("[OdinProtocol] ASCII PIT Read Timeout.");
        }

        // Try HEX Request (0x80)
        manager.sendPacket(SamsungDefs.CMD_PIT_HEX, status);

        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }

        byte[] pit2 = manager.readPacket(4096, status);
        if (pit2 != null) {
            System.out.println("[OdinProtocol] PIT READ SUCCESS (HEX) bytes: " + pit2.length);
        } else {
            System.out.println("[OdinProtocol] HEX PIT Read Timeout.");
        }
    }
}
