package com.intelligent.service;

import com.intelligent.core.UsbBridge;
import com.intelligent.core.UsbBridge.DeviceStatus;

public class DeviceManager {

    private final UsbBridge driver;

    public DeviceManager() {
        this.driver = UsbBridge.INSTANCE;
    }

    public boolean connect(int vid, int pid, int interfaceIdx) {
        System.out.println("[Manager] Connecting to Device (VID: " + Integer.toHexString(vid) + " Interface: "
                + interfaceIdx + ")...");
        int result = driver.connect_physical_device(vid, pid, interfaceIdx);
        return result == 1;
    }

    public void disconnect() {
        driver.close_device();
    }

    public boolean sendPacket(byte[] data, DeviceStatus status) {
        // System.out.println("[Manager] Sending " + data.length + " bytes...");
        driver.send_raw_packet(data, data.length, status);
        // C++ returns 0 on success
        return status.last_error_code == 0;
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

    public String listDevices() {
        byte[] buffer = new byte[4096];
        int len = driver.list_connected_devices(buffer, 4096);
        if (len > 0) {
            return new String(buffer, 0, len);
        }
        return "No devices found or Driver Error";
    }
}
