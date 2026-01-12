package com.intelligent.protocols;

public class SamsungDefs {
    // 1. Modem Mode Commands (ASCII)
    // Used when phone is ON (Normal Mode)
    public static final byte[] CMD_AT_HELLO = "AT\r".getBytes();
    public static final byte[] CMD_READ_IMEI = "AT+CGSN\r".getBytes();
    public static final byte[] CMD_READ_MODEL = "AT+GMM\r".getBytes();
    public static final byte[] CMD_READ_BATTERY = "AT+CBC\r".getBytes();
    public static final byte[] CMD_READ_VERSION = "AT+CGMR\r".getBytes();

    // 3. Reboot Commands
    public static final byte[] CMD_REBOOT_DOWNLOAD = "AT+MODE=2\r".getBytes();
    public static final byte[] CMD_REBOOT_ALT_1 = "AT+REBOOT\r".getBytes();
    public static final byte[] CMD_REBOOT_ALT_2 = "AT+FUN=5\r".getBytes();

    // 4. Odin Mode Commands (Handshake)
    // Host sends "ODIN", Device replies "LOKE"
    public static final byte[] CMD_ODIN_HANDSHAKE = { 0x4F, 0x64, 0x69, 0x6E }; // "ODIN"

    public static final byte[] CMD_PIT_ASCII = "PIT".getBytes();
    public static final byte[] CMD_PIT_HEX = { (byte) 0x80 };
}
