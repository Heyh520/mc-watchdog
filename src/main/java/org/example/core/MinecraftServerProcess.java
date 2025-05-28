package org.example.core;

import lombok.Getter;
import lombok.extern.log4j.Log4j;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class MinecraftServerProcess {
    private final ExecutorService streamPool =
            Executors.newCachedThreadPool(r -> new Thread(r, "MC-Stream"));
    private final AtomicBoolean starting = new AtomicBoolean(false);
    private Process process;
    @Getter
    private volatile boolean intentionalShutdown;
    public synchronized void start(List<String> cmd, File dir,
                                   ServerLogListener listener) throws IOException {
        if (isRunning() || starting.get()) return;
        starting.set(true);
        try{
            log.info("Starting MC with cmd: {}", String.join(" ", cmd));
            log.info("服务器正在启动...");
            process = new ProcessBuilder(cmd).directory(dir).start();
            intentionalShutdown = false;
            streamPool.submit(() -> pipe(process.getInputStream(), false, listener));
            streamPool.submit(() -> pipe(process.getErrorStream(), true, listener));
        }finally {
            starting.set(false);
        }
    }
    public synchronized void stop(boolean intentional) throws InterruptedException {
        if (!isRunning()) return;
        intentionalShutdown = intentional;
        sendCommand("stop");
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            log.warn("Graceful stop timeout, destroying forcibly");
            process.destroy();
        }
    }

    public void sendCommand(String cmd) {
        if (!isRunning()) return;
        try (PrintWriter w = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream(), Charset.defaultCharset()), true)) {
            w.println(cmd);
        }
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public boolean isStarting() {
        return starting.get();
    }
    private void pipe(InputStream in, boolean err, ServerLogListener lsn) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (err) log.error("[Server] {}", line);
                else     log.info("[Server] {}", line);
                lsn.onLog(line, LocalDateTime.now());
            }
        } catch (IOException ignore) { }
    }

    public void destroy() {
        streamPool.shutdownNow();
        if (isRunning()) process.destroy();
    }

}
