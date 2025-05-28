package org.example.core;

import java.time.LocalDateTime;

@FunctionalInterface
public interface ServerLogListener {
    void onLog(String line, LocalDateTime ts);
}
