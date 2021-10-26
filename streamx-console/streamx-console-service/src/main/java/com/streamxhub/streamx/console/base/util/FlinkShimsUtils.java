package com.streamxhub.streamx.console.base.util;

import com.streamxhub.streamx.common.util.FlinkUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author benjobs
 * @author zzz
 */
@Slf4j
public class FlinkShimsUtils {

    public static final Pattern FLINK_PATTERN = Pattern.compile(
        "flink-(.*).jar",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern SHIMS_PATTERN = Pattern.compile(
        "streamx-flink-shims_flink-(1.12|1.13|1.14)-(.*).jar",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Map<String, ClassLoader> SHIMS_CLASS_LOADER_CACHE = new ConcurrentHashMap<>();

    private static Pattern getFlinkShimsResourcePattern(String flinkLargeVersion) {
        return Pattern.compile(
            "flink-(.*)-" + flinkLargeVersion + "(.*).jar",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
    }

    @SneakyThrows
    public static synchronized ClassLoader getFlinkShimsClassLoader(String flinkLargeVersion, String flinkHome) {
        log.info("flink shims version: {}", flinkLargeVersion);

        ClassLoader classLoader = SHIMS_CLASS_LOADER_CACHE.get(flinkLargeVersion);

        if (classLoader == null) {
            //1) flink/lib
            List<URL> shimsUrls = new ArrayList<>(FlinkUtils.getFlinkHomeLibWithoutLog4j(flinkHome));

            //2) shims jar
            Arrays.stream(Objects.requireNonNull(new File(WebUtils.getAppDir("lib")).listFiles())).forEach((jar) -> {
                try {
                    Matcher shimsMatcher = SHIMS_PATTERN.matcher(jar.getName());
                    if (shimsMatcher.matches()) {
                        if (flinkLargeVersion != null && flinkLargeVersion.equals(shimsMatcher.group(1))) {
                            shimsUrls.add(jar.toURI().toURL());
                        }
                    } else if (!FLINK_PATTERN.matcher(jar.getName()).matches()) {
                        shimsUrls.add(jar.toURI().toURL());
                    } else {
                        log.info("exclude {}", jar.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            //3) submit-core jar
            Arrays.stream(Objects.requireNonNull(new File(WebUtils.getAppDir("plugins")).listFiles()))
                .filter(x -> x.getName().matches("streamx-flink-submit-core-.*\\.jar"))
                .forEach(x -> {
                    try {
                        shimsUrls.add(x.toURI().toURL());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                });

            URL[] urls = shimsUrls.toArray(new URL[0]);
            classLoader = new FlinkShimsChildFirstClassLoader(urls, getFlinkShimsResourcePattern(flinkLargeVersion));
            SHIMS_CLASS_LOADER_CACHE.put(flinkLargeVersion, classLoader);
        }

        return classLoader;
    }

    public static Object getClassLoaderObject(ClassLoader loader, Object obj) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(obj);

        final InputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ClassLoaderObjectInputStream clois = new ClassLoaderObjectInputStream(loader, bais)) {
            return clois.readObject();
        }
    }
}
