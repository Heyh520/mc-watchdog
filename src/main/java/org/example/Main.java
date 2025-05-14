package org.example;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String START_COMMAND = "\"C:\\Program Files\\Java\\jdk-17\\bin\\java.exe\" -server -Xms6G -Xmx8G -XX:+UseG1GC -XX:ParallelGCThreads=8 -XX:MaxDirectMemorySize=8G -XX:+UseCompressedOops -jar forge-1.12.2-14.23.5.2860.jar nogui";
    private static final Logger logger= LogManager.getLogger(Main.class);
    private static final long CHECK_INTERVAL_SECONDS = 10;
    private static int lastRestartMinute = -1;
    private static LocalTime RESTART_TIME = LocalTime.of(0, 0); // 每天定时重启

    private static Process mcProcess;
    private static boolean restarting = false;
    private static volatile boolean intentionalShutdown = false;
    private static final Properties config = new Properties();
    private static String env;
    private static String javaPath;
    private static String jarName;
    private static String workingDir;
    private static String formGmail;
    private static String toGmail;
    private static final ScheduledExecutorService emailScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("Email-Retry-Thread");
                return t;
            });
    static {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
            env = System.getenv().getOrDefault("ENV", "dev"); // 默认 dev
            RESTART_TIME = Objects.equals(env, "dev") ?LocalTime.of(9, 33):RESTART_TIME;
            javaPath = config.getProperty(env + ".java.path");
            jarName = config.getProperty(env + ".jar.name");
            workingDir = config.getProperty(env + ".working.dir");
            formGmail=config.getProperty("formGmail");
            toGmail=config.getProperty("toGmail");
        } catch (IOException e) {
            System.err.println("[Watchdog] 加载配置文件失败：" + e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        System.out.println("[Watchdog] 当前环境: " + env);
        System.out.println("[Watchdog] 启动 Java: " + javaPath);
        System.out.println("[Watchdog] 启动 JAR: " + jarName);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        startMinecraftServer();

        // 日常检查服务器状态 & 定时重启
        scheduler.scheduleAtFixedRate(() -> {
            if (!mcProcess.isAlive() && !restarting) {
                System.out.println("[Watchdog] 检测到服务器已关闭，自动重启...");
                if (!intentionalShutdown) {
                    String msg = "[Watchdog] 检测到服务器崩溃（非预期关闭），正在尝试重启...";
                    System.err.println(msg);
                    logger.warn(msg);
                    sendEmailWithRetry("⚠️ Minecraft服务器崩溃提醒", "服务器于 " + LocalTime.now() + " 意外关闭，系统已自动尝试重启。");
                }
                startMinecraftServer();
            }

            LocalTime now = LocalTime.now().withSecond(0).withNano(0);
            if (now.getHour() == RESTART_TIME.getHour()
                    && now.getMinute() == RESTART_TIME.getMinute()
                    && now.getMinute() != lastRestartMinute
                    && !restarting) {

                restarting = true;
                lastRestartMinute = now.getMinute();
                System.out.println("[Watchdog] 到达定时重启时间，准备安全关闭服务器...");
                sendCommandToServer("say §c[系统公告] §c服务器将在 30 秒后重启，请及时保存并下线！");
                intentionalShutdown = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(30_000); // 等待 30 秒后再发送 stop
                        sendCommandToServer("stop");

                        boolean exited = mcProcess.waitFor(30, TimeUnit.SECONDS);
                        if (!exited) {
                            System.out.println("[Watchdog] 超时未关闭，强制结束服务器进程");
                            mcProcess.destroy();
                            // ❗重启失败时发邮件
                            sendEmail("❌ Minecraft重启失败", "服务器在 0 点未能正常关闭，将尝试强制重启。\n时间：" + LocalTime.now());
                        }
                        startMinecraftServer();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        restarting = false;
                    }
                }).start();
            }
        }, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 监听用户输入的指令
        new Thread(() -> {
            try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
                String input;
                while ((input = consoleReader.readLine()) != null) {
                    if (input.equalsIgnoreCase("stop") || input.equalsIgnoreCase("restart")) {
                        intentionalShutdown = true;
                        System.out.println("[Watchdog] 检测到用户输入了 " + input + "，标记为主动关服");
                    }
                    sendCommandToServer(input);
                }
            } catch (IOException e) {
                System.err.println("[Watchdog] 读取控制台指令失败: " + e.getMessage());
            }
        }).start();
    }

    private static void startMinecraftServer() {
        try {
            System.out.println("[Watchdog] 正在启动 Minecraft 服务器...");

            ProcessBuilder builder;

            if ("neoforge".equalsIgnoreCase(env)) {
                // 自动定位 win_args.txt 文件
                File winArgsFile = findFile(new File(workingDir), "win_args.txt");
                File jvmArgsFile = new File(workingDir, "user_jvm_args.txt");

                if (!jvmArgsFile.exists() || winArgsFile == null) {
                    throw new RuntimeException("NeoForge 参数文件不存在");
                }

                List<String> command = new ArrayList<>();
                command.add(javaPath);
                command.add("@" + jvmArgsFile.getAbsolutePath());
                command.add("@" + winArgsFile.getAbsolutePath());
                command.add("nogui");

                builder = new ProcessBuilder(command);
            } else {
                String[] command = {
                        javaPath,
                        "-server",
                        "-Xms6G",
                        "-Xmx10G",
                        "-XX:+UseG1GC",
                        "-XX:ParallelGCThreads=8",
                        "-XX:MaxDirectMemorySize=8G",
                        "-XX:+UseCompressedOops",
                        Objects.equals(env, "test") ? "-Dfile.encoding=GBK" : "-Dfile.encoding=UTF-8",
                        "-jar",
                        jarName,
                        "nogui"
                };
                builder = new ProcessBuilder(command);
            }
            mcProcess = builder.start();
            intentionalShutdown = false;
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(mcProcess.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Server] " + line);
                        // 监听启动完成标志
                        // 监听服务器启动成功后
                        if (line.contains("Done") && line.contains("For help")) {
                            System.out.println("[Watchdog] 服务器启动完成，准备发送通知邮件...");

                            String cityId = WeatherFetcher.getCityId("沈阳");
                            String weather = WeatherFetcher.getWeatherInfo(cityId);
                            String content = "🟢 服务器环境: " + env +
                                    "\n启动时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                                    "\n\n" + weather;

                            sendEmailWithRetry("Minecraft服务器已重启", content);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[Watchdog] 读取服务器输出失败: " + e.getMessage());
                    logger.error("[Watchdog] 读取服务器输出失败: {}", e.getMessage());
                }
            }).start();
        } catch (IOException e) {
            System.err.println("[Watchdog] 启动服务器失败: " + e.getMessage());
        }
    }

    private static void sendCommandToServer(String command) {
        if (mcProcess != null && mcProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(mcProcess.getOutputStream(), StandardCharsets.UTF_8), true);
                writer.println(command);
                logger.info("[Watchdog] 已通过标准输入发送命令: {}", command);
            } catch (RuntimeException e) {
                System.err.println("[Watchdog] 向服务器写入命令失败: " + e.getMessage());
                logger.error("[Watchdog] 向服务器写入命令失败:{} " , e.getMessage());
            }
        }
    }

    private static void sendEmailWithRetry(String subject, String content) {
        class EmailRetryTask implements Runnable {
            private int attemptCount = 0;
            private final int maxRetries = 3;
            private ScheduledFuture<?> selfFuture;

            public void setSelfFuture(ScheduledFuture<?> future) {
                this.selfFuture = future;
            }

            @Override
            public void run() {
                try {
                    sendEmail(subject, content);
                    logger.info("[Watchdog] 邮件发送成功: {}", subject);
                    if (selfFuture != null) selfFuture.cancel(false); // 成功取消任务
                } catch (Exception e) {
                    attemptCount++;
                    logger.error("[Watchdog] 邮件发送失败（第 {} 次），将在 5 分钟后重试: {}", attemptCount, e.getMessage(), e);
                    if (attemptCount >= maxRetries && selfFuture != null) {
                        logger.error("[Watchdog] 邮件连续 {} 次发送失败，已放弃发送", maxRetries);
                        selfFuture.cancel(false); // 达到最大重试次数，取消任务
                    }
                }
            }
        }

        EmailRetryTask task = new EmailRetryTask();
        ScheduledFuture<?> future = emailScheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.MINUTES);
        task.setSelfFuture(future);
    }
    private static void sendEmail(String subject, String content) {
        final String from = formGmail;
        final String to = toGmail;
        //WJT final String password = "hvcd zoaa cfwi hklo".replace(" ", ""); // 授权码
        final String password = "powt gyuy bxnf gvyj".replace(" ", "");// 授权码

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com"); // or smtp.163.com / smtp.gmail.com
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
            logger.info("[Watchdog] 邮件已发送至 {}",to);
        } catch (MessagingException e) {
            System.err.println("[Watchdog] 发送邮件失败: " + e.getMessage());
        }
    }
    /**
     * 递归查找（NeoForge服务端）
     * @param dir
     * @param name
     * @return
     */
    private static File findFile(File dir, String name) {
        if (!dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFile(file, name);
                if (found != null) return found;
            } else if (file.getName().equals(name)) {
                return file;
            }
        }
        return null;
    }
    /**
     * QQ BOT预留方法
     * @param message
     */
    private static void notifyViaQQBot(String message) {
        // TODO: 接入 mirai-api-http 或 go-cqhttp
        // 示例 POST 请求发送到 QQBot
        System.out.println("[QQBot] 通知: " + message);
    }

    /**
     * Telegram 通知接口（预留）
     * @param message
     */
    private static void notifyViaTelegram(String message) {
        // TODO: 接入 Telegram Bot API（https://api.telegram.org）
        // 需要配置 token + chat_id
        System.out.println("[Telegram] 通知: " + message);
    }

}
