package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.config.ServerProperties;
import org.example.service.WatchdogScheduler;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ServerController {

    private final WatchdogScheduler scheduler;
    private final ServerProperties props;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "running", scheduler.getMc().isRunning(),
                "env", props.getEnv(),
                "time", LocalDateTime.now());
    }

    @PostMapping("/start")
    public void start() throws Exception {
        scheduler.getMc().start(
                props.buildLaunchCmd(), props.getWorkingDir().toFile(),
                (l, t) -> {});
    }

    @PostMapping("/stop")
    public void stop() throws Exception {
        scheduler.getMc().stop(true);
    }

    @PostMapping("/command")
    public void command(@RequestBody Map<String,String> body) {
        scheduler.getMc().sendCommand(body.get("cmd"));
    }
}
