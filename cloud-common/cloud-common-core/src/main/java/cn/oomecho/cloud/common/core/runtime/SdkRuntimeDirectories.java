package cn.oomecho.cloud.common.core.runtime;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端 SDK 运行目录配置；在 Spring 和 SDK 初始化前隔离各服务的日志与缓存。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public final class SdkRuntimeDirectories {
    /**
     * SDK 日志根目录属性。
     */
    private static final String LOG_PATH = "JM.LOG.PATH";
    /**
     * SDK 配置与服务缓存根目录属性。
     */
    private static final String SNAPSHOT_PATH = "JM.SNAPSHOT.PATH";

    /**
     * 工具类不允许创建实例。
     */
    private SdkRuntimeDirectories() {
    }

    /**
     * 设置 SDK 默认路径，显式传入的 JVM 路径优先；不修改全局 user.home。
     *
     * @param serviceName          服务名，用于隔离不同服务的输出
     * @param applicationDirectory 启动类或可执行 JAR 所在目录
     */
    public static void configure(String serviceName, Path applicationDirectory) {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path projectRoot = findProjectRoot(applicationDirectory);
        if (projectRoot == null) {
            projectRoot = findProjectRoot(workingDirectory);
        }
        // 开发环境定位到仓库根目录；独立部署时使用应用目录，不依赖用户目录。
        Path runtimeBase = projectRoot == null ? applicationDirectory.toAbsolutePath().normalize() : projectRoot;
        Path serviceDirectory = runtimeBase.resolve(".runtime").resolve(serviceName);
        setDefault(LOG_PATH, serviceDirectory.resolve("logs"));
        setDefault(SNAPSHOT_PATH, serviceDirectory.resolve("cache"));
        // Sentinel 日志同样在核心类初始化前确定，避免使用用户目录的默认路径。
        setDefault("csp.sentinel.log.dir", serviceDirectory.resolve("logs/sentinel"));
    }

    /**
     * 从模块、target/classes 或 JAR 所在目录向上寻找本项目根目录。
     *
     * @param directory 搜索起点
     * @return 项目根目录；独立部署没有源码目录时返回 null
     */
    private static Path findProjectRoot(Path directory) {
        for (Path current = directory.toAbsolutePath().normalize(); current != null; current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("cloud-services"))
                    && Files.isDirectory(current.resolve("cloud-common"))) {
                return current;
            }
        }
        return null;
    }

    /**
     * 只填补未设置的 SDK 路径，保留部署者通过 -D 显式指定的目录。
     *
     * @param key  JVM 属性名
     * @param path 默认绝对路径
     */
    private static void setDefault(String key, Path path) {
        String configured = System.getProperty(key);
        if (configured == null || configured.isBlank()) {
            System.setProperty(key, path.toString());
        }
    }
}
