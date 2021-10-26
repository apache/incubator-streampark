package com.streamxhub.streamx.console.base.util;

import lombok.SneakyThrows;
import org.apache.flink.util.FlinkUserCodeClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A variant of the URLClassLoader that first loads from the URLs and only after that from the
 * parent.
 *
 * <p>{@link #getResourceAsStream(String)} uses {@link #getResource(String)} internally so we don't
 * override that.
 */
public final class FlinkShimsChildFirstClassLoader extends FlinkUserCodeClassLoader {

    /**
     * {@link org.apache.flink.configuration.CoreOptions.ALWAYS_PARENT_FIRST_LOADER_PATTERNS}
     */
    private static final String[] ALWAYS_PARENT_FIRST_LOADER_PATTERNS = new String[]{
        "sun.reflect.",
        "sun.misc.",
        "javax.net.",
        "javax.security.",

        "java.",
        "scala.",
        // use flink shims
        // "org.apache.flink.",
        "com.esotericsoftware.kryo",
        "org.apache.hadoop.",
        "javax.annotation.",
        "org.slf4j;org.apache.log4j",
        "org.apache.logging",
        "org.apache.commons.logging",
        // Only certain classes can access this method.
        // "ch.qos.logback",
        "org.xml",
        "javax.xml",
        "org.apache.xerces",
        "org.w3c"
    };

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final Pattern flinkShimsResourcePattern;
    /**
     * The classes that should always go through the parent ClassLoader. This is relevant for Flink
     * classes, for example, to avoid loading Flink classes that cross the user-code/system-code
     * barrier in the user-code ClassLoader.
     */
    private final String[] alwaysParentFirstPatterns;

    public FlinkShimsChildFirstClassLoader(URL[] urls, Pattern flinkShimsResourcePattern) {
        this(urls, null, flinkShimsResourcePattern);
    }

    public FlinkShimsChildFirstClassLoader(URL[] urls, ClassLoader parent, Pattern flinkShimsResourcePattern) {
        this(urls, parent, flinkShimsResourcePattern, ALWAYS_PARENT_FIRST_LOADER_PATTERNS);
    }

    public FlinkShimsChildFirstClassLoader(
        URL[] urls,
        ClassLoader parent,
        Pattern flinkShimsResourcePattern,
        String[] alwaysParentFirstPatterns) {
        this(urls, parent, flinkShimsResourcePattern, alwaysParentFirstPatterns, NOOP_EXCEPTION_HANDLER);
    }

    public FlinkShimsChildFirstClassLoader(
        URL[] urls,
        ClassLoader parent,
        Pattern flinkShimsResourcePattern,
        String[] alwaysParentFirstPatterns,
        Consumer<Throwable> classLoadingExceptionHandler) {
        super(urls, parent, classLoadingExceptionHandler);
        this.flinkShimsResourcePattern = flinkShimsResourcePattern;
        this.alwaysParentFirstPatterns = alwaysParentFirstPatterns;
    }

    @SneakyThrows
    private URL filterFlinkShimsResource(URL urlClassLoaderResource) {
        if (urlClassLoaderResource != null && "jar".equals(urlClassLoaderResource.getProtocol())) {
            /**
             * {@link java.net.JarURLConnection#parseSpecs}
             */
            String spec = urlClassLoaderResource.getFile();
            String filename = new File(spec.substring(0, spec.indexOf("!/"))).getName();

            if (FlinkShimsUtils.FLINK_PATTERN.matcher(filename).matches() && !flinkShimsResourcePattern.matcher(filename).matches()) {
                return null;
            }
        }

        return urlClassLoaderResource;
    }

    private List<URL> addResources(List<URL> result, Enumeration<URL> resources) {
        while (resources.hasMoreElements()) {
            URL urlClassLoaderResource = filterFlinkShimsResource(resources.nextElement());

            if (urlClassLoaderResource != null) {
                result.add(urlClassLoaderResource);
            }
        }

        return result;
    }

    @Override
    protected Class<?> loadClassWithoutExceptionHandling(String name, boolean resolve)
        throws ClassNotFoundException {

        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);

        if (c == null) {
            // check whether the class should go parent-first
            for (String alwaysParentFirstPattern : alwaysParentFirstPatterns) {
                if (name.startsWith(alwaysParentFirstPattern)) {
                    return super.loadClassWithoutExceptionHandling(name, resolve);
                }
            }

            try {
                // check the URLs
                c = findClass(name);
            } catch (ClassNotFoundException e) {
                // let URLClassLoader do it, which will eventually call the parent
                c = super.loadClassWithoutExceptionHandling(name, resolve);
            }
        } else if (resolve) {
            resolveClass(c);
        }

        return c;
    }

    @Override
    public URL getResource(String name) {
        // first, try and find it via the URLClassloader
        URL urlClassLoaderResource = findResource(name);

        if (urlClassLoaderResource == null) {
            // delegate to super
            urlClassLoaderResource = super.getResource(name);
        }

        return filterFlinkShimsResource(urlClassLoaderResource);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // first get resources from URLClassloader
        final List<URL> result = addResources(new ArrayList<>(), findResources(name));

        ClassLoader parent = getParent();

        if (parent != null) {
            // get parent urls
            addResources(result, parent.getResources(name));
        }

        return new Enumeration<URL>() {
            final Iterator<URL> iter = result.iterator();

            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            @Override
            public URL nextElement() {
                return iter.next();
            }
        };
    }
}
