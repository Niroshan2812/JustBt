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

    public void readDeviceInfo(DeviceManager manager) {
        System.out.println("\n[SamsungProtocol] Reading Device Info...");

        String imei = sendCommand(manager, SamsungDefs.CMD_READ_IMEI);
        System.out.println(
                "   -> IMEI Response: " + (imei != null ? imei.replace("\r", " ").replace("\n", " ") : "Failed"));

        String model = sendCommand(manager, SamsungDefs.CMD_READ_MODEL);
        System.out.println(
                "   -> Model Response: " + (model != null ? model.replace("\r", " ").replace("\n", " ") : "Failed"));
    }
}
