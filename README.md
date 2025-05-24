# 🛡️ MC Watchdog

这是一个用于 Minecraft 服务器的看门狗程序，具备以下核心功能：

- ⏰ **定时自动重启**：每日定时重启服务器，保持服务稳定。
- 📧 **邮件通知**：服务器状态变动或错误事件可通过邮件通知管理员。
- 🧩 **MOD 更新检测**：检测 `mods` 文件夹的变化，支持增删改监控(现在已支持增加提示重启)。
- 🔄 **持续迭代中**：项目持续更新中，欢迎关注和提出建议。

---

## 🚀 功能说明

### ✅ 自动重启

- 支持设置每天指定时间进行服务器自动重启。
- 避免内存泄露、服务假死等问题。

### ✅ 邮件提醒

- 使用 SMTP 邮件服务发送提醒。
- 支持异常日志发送、MOD 变动提示等。

### ✅ MOD 监控

- 启动时记录 `mods` 文件夹快照。
- 检测是否有 MOD 被替换、删除或新增。
- 可结合日志或邮件通知反馈变更。

---

## 🧱 技术栈

- **语言**：Java
- **依赖**：
  - JavaMail
  - 日志组件（如 log4j/slf4j）
  - JDK 1.8+

---

## 🔧 使用方式

1. **克隆仓库**
   ```bash
   git clone https://github.com/Heyh520/mc-watchdog.git
   cd mc-watchdog
2.修改配置文件
  - 修改项目根目录下的 config.properties 文件，配置当前使用的环境与相关参数：
    ```bash
    # 环境选择（dev / prod / test / neoforge）
    env=dev
    
    # 开发环境配置（dev）
    dev.java.path=C:\\Program Files\\Java\\jdk-17\\bin\\java.exe
    dev.jar.name=banner-1.20.1-800-server.jar
    dev.working.dir=C:\\Users\\26658\\Desktop\\cs
    
    # 生产环境配置（prod）
    prod.java.path=C:\\Program Files\\Java\\jdk-1.8\\bin\\java.exe
    prod.jar.name=forge-1.12.2-14.23.5.2860.jar
    prod.working.dir=E:\\mcpo3server
    
    # 测试环境配置（test）
    test.java.path=C:\\Program Files\\Java\\jdk-21\\bin\\java.exe
    test.jar.name=fabric-server-launch.jar
    test.working.dir=E:\\mc1.21.1
    
    # NeoForge 环境配置（neoforge）
    neoforge.java.path=C:\\Program Files\\Java\\jdk-21\\bin\\java.exe
    neoforge.jar.name=fabric-server-launch.jar
    neoforge.working.dir=E:\\1.21.1NeoForge
|📌 注意：你需要根据实际情况调整 Java 路径、JAR 文件名和服务运行目录。
3. 编译与启动
  - 使用 Maven 构建并运行：
    ```bash
    mvn clean package
    java -jar target/mc-watchdog-1.0-SNAPSHOT.jar
## 📝 注意事项
- 保证配置文件中的路径与文件名正确。
- 如启用邮件通知功能，请在 config.properties 中正确配置 SMTP 服务信息。
- 可考虑将程序设置为系统服务，实现开机自启和守护运行。
