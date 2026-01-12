package com.intelligent;

import com.intelligent.service.DeviceManager;
import com.intelligent.protocols.SamsungProtocol;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Intelligent Mobile Tool v1.0 ===");

        DeviceManager manager = new DeviceManager();
        SamsungProtocol samsung = new SamsungProtocol();

        // PID 6860 is Samsung Normal Mode
        // VID 0x04E8 = Samsung
        if (manager.connect(0x04E8, 0x6860)) {
            System.out.println("[Main] Device Connected!");

            // Execute the Protocol Logic
            samsung.performModemHandshake(manager);

        } else {
            System.out.println("[Main] Device not found. Check Zadig Driver.");
        }

        System.out.println("=== End of Operation ===");
    }
}