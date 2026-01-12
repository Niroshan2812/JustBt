package com.intelligent;

import com.intelligent.service.DeviceManager;
import com.intelligent.protocols.SamsungProtocol;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Intelligent Mobile Tool v1.0 ===");

        DeviceManager manager = new DeviceManager();

        System.out.println("--- USB Diagnostics ---");
        System.out.println(manager.listDevices());
        System.out.println("-----------------------");

        // 1. Try Normal Mode (Modem)
        System.out.println("[Main] Step 1: Scanning for SAMSUNG Modem (Normal Mode)...");
        boolean modemFound = scanForModem(manager);

        // 2. If not found, check if already in Download Mode
        if (!modemFound) {
            System.out.println("[Main] Step 2: Scanning for SAMSUNG Download Mode (Odin Mode)...");
            scanForDownloadMode(manager);
        }

        System.out.println("=== End of Operation ===");
    }

    private static boolean scanForModem(DeviceManager manager) {
        SamsungProtocol samsung = new SamsungProtocol();

        for (int i = 0; i < 4; i++) {
            // Try connecting to Modem PID 0x6860
            if (manager.connect(0x04E8, 0x6860, i)) {
                if (samsung.performModemHandshake(manager)) {
                    System.out.println("\n[Main] >>> DEVICE FOUND: Normal Mode (Interface " + i + ") <<<");

                    // Action 1: Read Info
                    samsung.readDeviceInfo(manager);

                    // Action 2: Reboot
                    samsung.rebootToDownload(manager);

                    manager.disconnect();
                    return true;
                }
                manager.disconnect();
            }
        }
        System.out.println("   -> No Normal Mode device found.");
        return false;
    }

    private static void scanForDownloadMode(DeviceManager manager) {
        // PID 0x685D = Gadget Serial

        for (int i = 0; i < 4; i++) {
            if (manager.connect(0x04E8, 0x685D, i)) {
                System.out.println("\n[Main] >>> DEVICE FOUND: Download Mode (Interface " + i + ") <<<");
                System.out.println("[Main] Ready for Flashing (Low-level connectivity established).");

                // Protocol not fully active yet
                // com.intelligent.protocols.OdinProtocol odin = new
                // com.intelligent.protocols.OdinProtocol();
                // odin.performHandshake(manager);

                manager.disconnect();
                return;
            }
        }
        System.out.println("   -> No Download Mode device found.");
    }
}