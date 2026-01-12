package com.intelligent.protocols;

import com.intelligent.service.DeviceManager;
import com.intelligent.core.UsbBridge.DeviceStatus;

public class SamsungProtocol {

    // The Logic: Attempt to talk to the Modem
    public boolean performModemHandshake(DeviceManager manager) {
        System.out.println("[SamsungProtocol] Sending AT Handshake...");

        DeviceStatus status = new DeviceStatus();

        // 1. Send "AT"
        boolean sent = manager.sendPacket(SamsungDefs.CMD_AT_HELLO, status);
        if (!sent) {
            System.err.println("[SamsungProtocol] Write Failed: " + status.getMessageString());
            return false;
        }

        // 2. Listen for Reply
        System.out.println("[SamsungProtocol] Waiting for device response...");
        byte[] response = manager.readPacket(1024, status);

        // 3. Process the Result
        if (response != null && response.length > 0) {
            String reply = new String(response).trim();
            System.out.println("[SamsungProtocol] SUCCESS! Device Replied: " + reply);

            // Analyze the reply
            if (reply.contains("OK")) {
                System.out.println("[SamsungProtocol] >> Modem is Active and Ready.");
                return true;
            }
        } else {
            System.err.println("[SamsungProtocol] Read Logic Failed: " + status.getMessageString());
        }
        return false;
    }

    // Helper to send command and get string response
    private String sendCommand(DeviceManager manager, byte[] cmd) {
        DeviceStatus status = new DeviceStatus();
        if (!manager.sendPacket(cmd, status))
            return null;

        byte[] resp = manager.readPacket(1024, status);
        if (resp != null && resp.length > 0) {
            return new String(resp).trim();
        }
        return null;
    }

    private String cleanResponse(String raw) {
        if (raw == null)
            return "Failed";
        // Remove known AT noise: "AT+COMMAND", "OK", "ERROR" and newlines
        // Example: "AT+GMM\r\r\nSM-M205F\r\n\r\nOK\r\n" -> "SM-M205F"
        String clean = raw.replaceAll("AT\\+[A-Z]+\r\r\n", "") // Remove Echo
                .replaceAll("AT\\+[A-Z]+\r", "") // Remove Echo variant
                .replaceAll("\r\nOK", "") // Remove OK suffix
                .replaceAll("\r\nERROR", "") // Remove ERROR suffix
                .trim();
        return clean;
    }

    public void readDeviceInfo(DeviceManager manager) {
        System.out.println("\n[SamsungProtocol] Reading Device Info...");

        System.out.println("   -> IMEI:      " + cleanResponse(sendCommand(manager, SamsungDefs.CMD_READ_IMEI)));
        System.out.println("   -> Model:     " + cleanResponse(sendCommand(manager, SamsungDefs.CMD_READ_MODEL)));
        System.out.println("   -> Version:   " + cleanResponse(sendCommand(manager, SamsungDefs.CMD_READ_VERSION)));
        System.out.println("   -> Battery:   " + cleanResponse(sendCommand(manager, SamsungDefs.CMD_READ_BATTERY)));
    }
}
