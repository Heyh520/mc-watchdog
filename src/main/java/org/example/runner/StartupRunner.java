package org.example.runner;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.config.ServerProperties;
import org.example.core.CommandDispatcher;
import org.example.service.WatchdogScheduler;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupRunner implements ApplicationRunner {

    private final WatchdogScheduler scheduler;
    private final ServerProperties props;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 启动 MC
        scheduler.getMc().start(
                props.buildLaunchCmd(), props.getWorkingDir().toFile(),
                (l, t) -> {});

        // 监听控制台输入
        new Thread(new CommandDispatcher(scheduler.getMc()), "Console-Listener").start();
    }
}
