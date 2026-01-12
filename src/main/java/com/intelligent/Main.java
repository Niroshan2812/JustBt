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
        boolean found = false;

        System.out.println("[Main] Starting Interface Scan (Looking for Modem)...");

        for (int i = 0; i < 4; i++) {
            System.out.println("\n[Main] Trying Interface " + i + "...");

            if (manager.connect(0x04E8, 0x6860, i)) {
                System.out.println("[Main] Interface " + i + " Connected. Testing Protocol...");

                // Execute the Protocol Logic
                if (samsung.performModemHandshake(manager)) {
                    System.out.println("\n[Main] >>> FOUND CORRECT MODEM INTERFACE: " + i + " <<<");
                    found = true;

                    // --- PHASE 8: READ INFO ---
                    samsung.readDeviceInfo(manager);

                    // Keep connection open or do further work here
                    break;
                } else {
                    System.out.println("[Main] Handshake Failed on Interface " + i + ". Disconnecting...");
                    manager.disconnect();
                }
            } else {
                System.out.println("[Main] Failed to claim Interface " + i);
            }
        }

        if (!found) {
            System.out.println("\n[Main] Scan Complete. No responsive Modem interface found.");
        }

        System.out.println("=== End of Operation ===");
    }
}