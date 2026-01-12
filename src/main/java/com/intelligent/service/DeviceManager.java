package com.intelligent.service;

import com.intelligent.core.UsbBridge;
import com.intelligent.core.UsbBridge.DeviceStatus;

public class DeviceManager {

    private final UsbBridge driver;

    public DeviceManager() {
        this.driver = UsbBridge.INSTANCE;
    }

    public boolean connect(int vid, int pid) {
        System.out.println("[Manager] Connecting to Device (VID: " + Integer.toHexString(vid) + ")...");
        int result = driver.connect_physical_device(vid, pid);
        return result == 1;
    }

    public boolean sendPacket(byte[] data, DeviceStatus status) {
        // System.out.println("[Manager] Sending " + data.length + " bytes...");
        driver.send_raw_packet(data, data.length, status);
        return status.statusCode == 200;
    }

    public byte[] readPacket(int maxLength, DeviceStatus status) {
        byte[] buffer = new byte[maxLength];
        // System.out.println("[Manager] Listening...");
        int bytesRead = driver.read_data(buffer, maxLength, status);

        if (bytesRead > 0) {
            // Trim buffer to actual size
            byte[] actualData = new byte[bytesRead];
            System.arraycopy(buffer, 0, actualData, 0, bytesRead);
            return actualData;
        } else {
            return null;
        }
    }
}
