package com.dynamic.xsd.service.classloader;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Custom ClassLoader for loading dynamically compiled service classes.
 *
 * Features:
 * - Isolated class loading per service
 * - Parent-last delegation (when configured)
 * - Class caching for performance
 * - Proper cleanup to prevent memory leaks
 */
@Slf4j
public class ServiceClassLoader extends URLClassLoader {

    private final String serviceName;
    private final boolean parentFirst;
    private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    /**
     * Creates a new ServiceClassLoader.
     *
     * @param serviceName Service name for identification
     * @param classPath Path to compiled classes
     * @param parent Parent classloader
     * @param parentFirst Use parent-first delegation
     */
    public ServiceClassLoader(String serviceName, Path classPath,
                             ClassLoader parent, boolean parentFirst) {
        super(createURLs(classPath), parent);
        this.serviceName = serviceName;
        this.parentFirst = parentFirst;
        log.info("Created ServiceClassLoader for service: {} with parent-first: {}",
            serviceName, parentFirst);
    }

    /**
     * Creates URLs for the classloader from the given path.
     */
    private static URL[] createURLs(Path classPath) {
        try {
            File file = classPath.toFile();
            if (!file.exists()) {
                throw new IllegalArgumentException("Class path does not exist: " + classPath);
            }
            return new URL[]{file.toURI().toURL()};
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to create URL from path: " + classPath, e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (closed) {
            throw new IllegalStateException("ClassLoader for service " + serviceName + " has been closed");
        }

        // Check cache first
        Class<?> cachedClass = classCache.get(name);
        if (cachedClass != null) {
            return cachedClass;
        }

        // Synchronize on class name to prevent duplicate loading
        synchronized (getClassLoadingLock(name)) {
            // Double-check cache
            cachedClass = classCache.get(name);
            if (cachedClass != null) {
                return cachedClass;
            }

            Class<?> clazz;

            if (parentFirst) {
                // Standard parent-first delegation
                clazz = super.loadClass(name, resolve);
            } else {
                // Parent-last delegation for isolation
                clazz = loadClassParentLast(name, resolve);
            }

            // Cache the class
            if (clazz != null) {
                classCache.put(name, clazz);
            }

            return clazz;
        }
    }

    /**
     * Loads class with parent-last delegation.
     */
    private Class<?> loadClassParentLast(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        // For system classes, always use parent
        if (isSystemClass(name)) {
            return super.loadClass(name, resolve);
        }

        try {
            // Try to find the class in this classloader first
            c = findClass(name);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        } catch (ClassNotFoundException e) {
            // If not found, delegate to parent
            return super.loadClass(name, resolve);
        }
    }

    /**
     * Checks if a class is a system class that should be loaded by parent.
     */
    private boolean isSystemClass(String name) {
        return name.startsWith("java.") ||
               name.startsWith("javax.") ||
               name.startsWith("jakarta.") ||
               name.startsWith("org.springframework.") ||
               name.startsWith("org.slf4j.") ||
               name.startsWith("org.apache.") ||
               name.startsWith("com.fasterxml.jackson.") ||
               name.startsWith("lombok.");
    }

    /**
     * Gets the service name associated with this classloader.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Gets the number of cached classes.
     */
    public int getCachedClassCount() {
        return classCache.size();
    }

    /**
     * Clears the class cache.
     */
    public void clearCache() {
        classCache.clear();
        log.debug("Cleared class cache for service: {}", serviceName);
    }

    /**
     * Closes this classloader and releases resources.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            clearCache();
            try {
                super.close();
                log.info("Closed ServiceClassLoader for service: {}", serviceName);
            } catch (Exception e) {
                log.error("Error closing ServiceClassLoader for service: {}", serviceName, e);
            }
        }
    }

    /**
     * Checks if this classloader is closed.
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "ServiceClassLoader{serviceName='" + serviceName + "', cachedClasses=" +
               classCache.size() + ", closed=" + closed + "}";
    }
}
