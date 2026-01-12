package com.intelligent.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class GuiMain {

    public static void main(String[] args) {
        // Setup Theme
        try {
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }
}

class MainWindow extends JFrame {

    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton btnScan;
    private JButton btnInfo;
    private JButton btnReboot;
    private JProgressBar progressBar;

    // --- Logic State ---
    private com.intelligent.service.DeviceManager activeManager;
    private com.intelligent.protocols.SamsungProtocol samsungProtocol;
    private com.intelligent.protocols.OdinProtocol odinProtocol;
    private boolean isModem = false;

    public MainWindow() {
        setTitle("Intelligent Mobile Tool v1.0");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // --- Header ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        headerPanel.setBackground(new Color(30, 30, 35));

        JLabel title = new JLabel("INTELLIGENT MOBILE TOOL");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(100, 180, 255));

        statusLabel = new JLabel("Waiting for Action...");
        statusLabel.setForeground(Color.GRAY);

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(statusLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // --- Center (Logs) ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(25, 25, 25));
        logArea.setForeground(new Color(200, 200, 200));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Redirect StdOut
        redirectSystemStreams();

        // --- Bottom (Controls) ---
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        controlsPanel.setBackground(new Color(35, 35, 40));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setPreferredSize(new Dimension(150, 5));
        progressBar.setVisible(false);

        btnScan = new JButton("Scan Device");
        btnScan.setBackground(new Color(0, 120, 215));
        btnScan.setForeground(Color.WHITE);
        btnScan.setFocusPainted(false);

        btnInfo = new JButton("Read Info");
        btnInfo.setEnabled(false);

        btnReboot = new JButton("Reboot DL");
        btnReboot.setBackground(new Color(200, 50, 50));
        btnReboot.setForeground(Color.WHITE);
        btnReboot.setEnabled(false);

        controlsPanel.add(progressBar);
        controlsPanel.add(btnScan);
        controlsPanel.add(btnInfo);
        controlsPanel.add(btnReboot);

        add(controlsPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnScan.addActionListener(e -> startScan());
        btnInfo.addActionListener(e -> readInfo());
        btnReboot.addActionListener(e -> reboot());
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                updateLog(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                updateLog(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void updateLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // --- Logic Hub ---
    private void startScan() {
        statusLabel.setText("Scanning...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        btnScan.setEnabled(false);
        btnInfo.setEnabled(false);
        btnReboot.setEnabled(false);

        // Close existing
        if (activeManager != null) {
            activeManager.disconnect();
            activeManager = null;
        }

        SwingWorker<String, String> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                activeManager = new com.intelligent.service.DeviceManager();
                samsungProtocol = new com.intelligent.protocols.SamsungProtocol();
                odinProtocol = new com.intelligent.protocols.OdinProtocol();

                // Known PIDs for Samsung (Modem, Download, MTP, etc)
                int[] KNOWN_PIDS = {
                        0x6860, // Normal (Modem)
                        0x685D, // Download (Gadget Serial)
                        0x685B, // Download (Variation)
                        0x6862, // Normal (MTP+ADB)
                        0x685E // Normal (Variation)
                };

                publish("Scanning common Samsung PIDs...");

                for (int pid : KNOWN_PIDS) {
                    for (int i = 0; i < 4; i++) {
                        // publish("Checking PID: " + Integer.toHexString(pid) + " Intf: " + i);
                        if (activeManager.connect(0x04E8, pid, i)) {
                            String pidStr = Integer.toHexString(pid);

                            // Determine Mode based on PID (Heuristic)
                            if (pid == 0x6860 || pid == 0x6862 || pid == 0x685E) {
                                // Attempt Handshake to confirm Modem
                                if (samsungProtocol.performModemHandshake(activeManager)) {
                                    isModem = true;
                                    return "Connected: Normal Mode (" + pidStr + ")";
                                }
                            } else {
                                // 685D, 685B are usually Download
                                isModem = false;
                                return "Connected: Download Mode (" + pidStr + ")";
                            }

                            activeManager.disconnect();
                        }
                    }
                }

                return "No Device Found (Check Zadig)";
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks)
                    System.out.println("[Scan] " + s);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    statusLabel.setText(result);
                    System.out.println("[UI] Scan Result: " + result);

                    progressBar.setVisible(false);
                    btnScan.setEnabled(true);

                    if (result.contains("Connected")) {
                        btnInfo.setEnabled(true);

                        if (isModem) {
                            btnInfo.setText("Read Info");
                            btnReboot.setText("Reboot DL");
                            btnReboot.setEnabled(true);
                        } else {
                            btnInfo.setText("Read PIT");
                            btnReboot.setText("Flash (Wait)");
                            btnReboot.setEnabled(false);

                            System.out.println("\n[UI] O.D.I.N Mode Detected!");
                            System.out.println("   -> Click 'Read PIT' to test Handshake & Partition Table.");
                        }
                    } else {
                        // Cleanup
                        if (activeManager != null) {
                            activeManager.disconnect();
                            activeManager = null;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void readInfo() {
        if (activeManager == null)
            return;

        new Thread(() -> {
            if (isModem) {
                System.out.println("\n=== Reading Info (AT Mode) ===");
                samsungProtocol.readDeviceInfo(activeManager);
            } else {
                System.out.println("\n=== Reading PIT (Odin Mode) ===");
                // 1. Handshake
                if (odinProtocol.performHandshake(activeManager)) {
                    // 2. Read PIT
                    com.intelligent.odin.pit.PitFile pit = odinProtocol.getPit(activeManager);
                    if (pit != null) {
                        System.out.println(">> PIT Parsed Successfully: " + pit.getEntries().size() + " entries.");
                        for (com.intelligent.odin.pit.PitEntry e : pit.getEntries()) {
                            System.out.println(e.toString());
                        }
                    }
                }
            }
            System.out.println("=== Done ===");
        }).start();
    }

    private void reboot() {
        if (activeManager == null)
            return;
        new Thread(() -> {
            System.out.println("\n=== Rebooting ===");
            samsungProtocol.rebootToDownload(activeManager);
            statusLabel.setText("Rebooting...");
            // Device will vanish, so disconnect
            activeManager.disconnect();
            activeManager = null;

            // Update UI on Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                btnInfo.setEnabled(false);
                btnReboot.setEnabled(false);
            });

        }).start();
    }
}
