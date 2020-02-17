package io.github.liweitaos.embed;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.ParallelWebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Tomcat启动器
 *
 * @author liweitao
 * @date 2020-02-17 17:40:54
 */
public class TomcatStarter {

    private static final Set<String> TLD_SKIP_PATTERNS = new LinkedHashSet<>(TldSkipPatterns.DEFAULT);

    private Tomcat tomcat;

    private String contextPath = "";

    private int port = 8080;

    public TomcatStarter() {
        super();
    }

    public TomcatStarter(String contextPath, int port) {
        super();
        this.contextPath = contextPath;
        this.port = port;
    }

    public void start() {
        final String baseDir = createTempDir("tomcat", port).getAbsolutePath();
        final String docBase = createTempDir("tomcat-docbase", port).getAbsolutePath();

        tomcat = new Tomcat();
        tomcat.setBaseDir(baseDir);

        final Connector connector = new Connector();
        connector.setPort(port);
        connector.setThrowOnFailure(true);
        connector.setURIEncoding(StandardCharsets.UTF_8.name());
        connector.setProperty("bindOnInit", "false");
        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);

        tomcat.getHost().setAutoDeploy(false);
        tomcat.getEngine().setBackgroundProcessorDelay(10);

        final Context context = tomcat.addWebapp(contextPath, docBase);

        final URL url;
        try {
            url = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        final WebResourceRoot resources = new MyStandardRoot();
        resources.setContext(context);
        resources.createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, "/WEB-INF/classes", url, "/");
        resources.createWebResourceSet(WebResourceRoot.ResourceSetType.RESOURCE_JAR, "/", url, "/META-INF/resources");

        context.setResources(resources);
        context.addWelcomeFile("index.html");
        context.addLifecycleListener(new Tomcat.FixContextListener());

        context.setParentClassLoader(getDefaultClassLoader());

        context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(), StandardCharsets.UTF_8.displayName());
        context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(), StandardCharsets.UTF_8.displayName());
        context.addLocaleEncodingMappingParameter(Locale.SIMPLIFIED_CHINESE.toString(), StandardCharsets.UTF_8.displayName());

        context.setUseRelativeRedirects(false);
        context.setCreateUploadTargets(true);

        final StandardJarScanFilter filter = new StandardJarScanFilter();
        filter.setTldSkip(String.join(",", TLD_SKIP_PATTERNS));
        context.getJarScanner().setJarScanFilter(filter);

        final WebappLoader loader = new WebappLoader(context.getParentClassLoader());
        loader.setLoaderClass(ParallelWebappClassLoader.class.getName());
        loader.setDelegate(true);
        context.setLoader(loader);

        context.setSessionTimeout(30);
        context.setUseHttpOnly(true);

        // 虚拟机关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "thread-tomcat-shutdown-hook"));

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        tomcat.getServer().await();
    }

    public void stop() {
        if (tomcat == null) {
            return;
        }
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }

    private File createTempDir(String prefix, int port) {
        try {
            final File tempDir = File.createTempFile(prefix + ".", "." + port);
            tempDir.delete();
            tempDir.mkdir();
            tempDir.deleteOnExit();
            return tempDir;
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create tempDir. java.io.tmpdir is set to " + System.getProperty("java.io.tmpdir"), ex);
        }
    }

    private ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = getClass().getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }

}
