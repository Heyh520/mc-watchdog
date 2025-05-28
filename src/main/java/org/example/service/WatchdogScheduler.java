package org.example.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.config.ServerProperties;
import org.example.core.MinecraftServerProcess;
import org.example.core.util.WeatherFetcher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Log4j2
@Service
@RequiredArgsConstructor
public class WatchdogScheduler {

    /* 暴露给其他 Bean（Controller / Runner）使用 */
    @Getter
    private final MinecraftServerProcess mc = new MinecraftServerProcess();
    private final ServerProperties props;
    private final EmailService email;

    /** 每 10 秒做一次健康检查 */
    @Scheduled(fixedDelay = 10_000)
    public void healthCheck() throws Exception {
        if (!mc.isRunning() && !mc.isStarting() && !mc.isIntentionalShutdown()) {
            log.warn("MC crashed, restarting…");
            email.sendWithRetry("⚠️ MC 崩溃", "检测到崩溃，已尝试自动重启", 3);
            mc.start(props.buildLaunchCmd(), props.getWorkingDir().toFile(), this::onLog);
        }
    }

    /** 每天固定时间重启（由 cron 表达式或属性控制） */
    @Scheduled(cron = "0 * * * * *")   // 每分钟检查一次
    public void dailyRestart() throws Exception {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        if (now.equals(props.getRestartTime())) {
            log.info("Scheduled restart…");
            mc.sendCommand("say §c[系统公告] 服务器将在 30 秒后重启！");
            Thread.sleep(30_000);
            mc.stop(true);
            mc.start(props.buildLaunchCmd(), props.getWorkingDir().toFile(), this::onLog);
        }
    }

    /* ----------------------------------- */

    /** 用于 ServerLogListener 回调 */
    private void onLog(String line, LocalDateTime ts) {
        if (line.contains("Done") && line.contains("For help")) {

            String weather = WeatherFetcher.getWeatherInfo(
                    WeatherFetcher.getCityId("沈阳"));
            String body = "🟢 服务器环境: " + props.getEnv() +
                    "\n启动时间: " + ts.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    "\n\n" + weather;
            email.sendWithRetry("Minecraft 服务器已重启", body, 3);
        }
    }

}
