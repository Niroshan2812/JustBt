package com.intelligent.protocols;

public class SamsungDefs {
    // 1. Modem Mode Commands (ASCII)
    // Used when phone is ON (Normal Mode)
    public static final byte[] CMD_AT_HELLO = "AT\r".getBytes();
    public static final byte[] CMD_READ_IMEI = "AT+CGSN\r".getBytes();
    public static final byte[] CMD_READ_MODEL = "AT+GMM\r".getBytes();
    public static final byte[] CMD_READ_BATTERY = "AT+CBC\r".getBytes();
    public static final byte[] CMD_READ_VERSION = "AT+CGMR\r".getBytes();

    // 2. Download Mode Commands (Hex)
    // Used when phone is in "Odin Mode" (Blue Screen)
    public static final byte[] CMD_ODIN_HANDSHAKE = new byte[] { (byte) 0x3A, (byte) 0x01, (byte) 0x00 };
}
