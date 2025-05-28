package org.example.core;

import java.util.Scanner;

public class CommandDispatcher implements Runnable {
    private final MinecraftServerProcess mc;

    public CommandDispatcher(MinecraftServerProcess mc) {
        this.mc = mc;
    }

    @Override public void run() {
        try (Scanner sc = new Scanner(System.in)) {
            while (sc.hasNextLine()) {
                String cmd = sc.nextLine();
                if ("stop".equalsIgnoreCase(cmd) || "restart".equalsIgnoreCase(cmd)) {
                    mc.sendCommand(cmd);
                } else {
                    mc.sendCommand(cmd);
                }
            }
        }
    }
}
