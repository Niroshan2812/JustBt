package com.intelligent.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public interface UsbBridge extends Library {
    // Load DLL
    UsbBridge INSTANCE = Native.load("usb_driver.dll", UsbBridge.class);

    // --- Structures ---
    class DeviceStatus extends Structure {
        public int statusCode;
        public byte[] message = new byte[256];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("statusCode", "message");
        }

        public String getMessageString() {
            return new String(message).trim();
        }
    }

    // --- Native Functions ---
    int connect_physical_device(int vid, int pid);

    void send_raw_packet(byte[] data, int length, DeviceStatus status);

    int read_data(byte[] buffer, int maxLength, DeviceStatus status);
}
