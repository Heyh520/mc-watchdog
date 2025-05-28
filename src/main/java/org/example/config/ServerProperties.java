package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.example.core.util.FileFinder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "watchdog")
@Getter @Setter
public class ServerProperties {
    private String env = "dev";
    private Path workingDir;
    private Path javaPath;
    private String jarName;
    private LocalTime restartTime = LocalTime.MIDNIGHT;

    private Mail mail = new Mail();
    @Getter @Setter public static class Mail {
        private String from;
        private String to;
        private String appPassword;
    }
    public List<String> buildLaunchCmd() {
        if ("neoforge".equalsIgnoreCase(env)) {
            return buildNeoForgeCmd();
        }
        // 其他核心（vanilla/forge/fabric 等）
        return buildGenericCmd();
    }

    /* --------- private helpers --------- */

    private List<String> buildNeoForgeCmd() {
        // 1) 找 win_args.txt 和 user_jvm_args.txt
        File winArgs = FileFinder.find(workingDir.toFile(), "win_args.txt");
        File jvmArgs = new File(workingDir.toFile(), "user_jvm_args.txt");

        if (winArgs == null || !jvmArgs.exists()) {
            throw new IllegalStateException("NeoForge 参数文件不存在");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(javaPath.toString());
        cmd.add("@" + jvmArgs.getAbsolutePath());
        cmd.add("@" + winArgs.getAbsolutePath());
        cmd.add("nogui");
        return cmd;
    }

    private List<String> buildGenericCmd() {
        return List.of(
                javaPath.toString(),
                "-server",
                "-Xms6G",
                "-Xmx10G",
                "-XX:+UseG1GC",
                "-XX:ParallelGCThreads=8",
                "-XX:MaxDirectMemorySize=8G",
                "-XX:+UseCompressedOops",
                "test".equals(env) ? "-Dfile.encoding=GBK" : "-Dfile.encoding=UTF-8",
                "-jar",
                jarName,
                "nogui"
        );
    }
}
