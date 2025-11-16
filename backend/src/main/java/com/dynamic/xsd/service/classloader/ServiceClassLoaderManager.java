package com.dynamic.xsd.service.classloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ServiceClassLoader instances for different services.
 * Provides centralized lifecycle management and isolation.
 */
@Slf4j
@Component
public class ServiceClassLoaderManager {

    private final Map<String, ServiceClassLoader> classLoaders = new ConcurrentHashMap<>();
    private final boolean parentFirst = false; // Use parent-last delegation for isolation

    /**
     * Gets or creates a classloader for a service.
     */
    public ClassLoader getOrCreateClassLoader(String serviceName, String classPath) {
        return classLoaders.computeIfAbsent(serviceName, key -> {
            log.info("Creating new classloader for service: {}", serviceName);
            Path path = Paths.get(classPath);
            return new ServiceClassLoader(
                serviceName,
                path,
                Thread.currentThread().getContextClassLoader(),
                parentFirst
            );
        });
    }

    /**
     * Gets or creates a classloader for a service with a Path object.
     */
    public ClassLoader getOrCreateClassLoader(String serviceName, Path classPath) {
        return classLoaders.computeIfAbsent(serviceName, key -> {
            log.info("Creating new classloader for service: {}", serviceName);
            return new ServiceClassLoader(
                serviceName,
                classPath,
                Thread.currentThread().getContextClassLoader(),
                parentFirst
            );
        });
    }

    /**
     * Gets an existing classloader for a service.
     */
    public ServiceClassLoader getClassLoader(String serviceName) {
        return classLoaders.get(serviceName);
    }

    /**
     * Checks if a classloader exists for a service.
     */
    public boolean hasClassLoader(String serviceName) {
        return classLoaders.containsKey(serviceName);
    }

    /**
     * Removes a classloader for a service and releases resources.
     */
    public void removeClassLoader(String serviceName) {
        ServiceClassLoader classLoader = classLoaders.remove(serviceName);
        if (classLoader != null) {
            classLoader.close();
            log.info("Removed and closed classloader for service: {}", serviceName);
        } else {
            log.warn("No classloader found for service: {}", serviceName);
        }
    }

    /**
     * Clears the class cache for a service.
     */
    public void clearCache(String serviceName) {
        ServiceClassLoader classLoader = classLoaders.get(serviceName);
        if (classLoader != null) {
            classLoader.clearCache();
            log.debug("Cleared cache for service: {}", serviceName);
        }
    }

    /**
     * Gets statistics about managed classloaders.
     */
    public Map<String, ClassLoaderStats> getStats() {
        Map<String, ClassLoaderStats> stats = new ConcurrentHashMap<>();
        classLoaders.forEach((serviceName, classLoader) -> {
            stats.put(serviceName, new ClassLoaderStats(
                serviceName,
                classLoader.getCachedClassCount(),
                classLoader.isClosed()
            ));
        });
        return stats;
    }

    /**
     * Gets the total number of managed classloaders.
     */
    public int getClassLoaderCount() {
        return classLoaders.size();
    }

    /**
     * Closes all classloaders and releases resources.
     * Should be called on application shutdown.
     */
    public void shutdown() {
        log.info("Shutting down ServiceClassLoaderManager, closing {} classloaders", classLoaders.size());
        classLoaders.forEach((serviceName, classLoader) -> {
            try {
                classLoader.close();
            } catch (Exception e) {
                log.error("Error closing classloader for service: {}", serviceName, e);
            }
        });
        classLoaders.clear();
        log.info("ServiceClassLoaderManager shutdown complete");
    }

    /**
     * Statistics about a classloader.
     */
    public static class ClassLoaderStats {
        public final String serviceName;
        public final int cachedClassCount;
        public final boolean closed;

        public ClassLoaderStats(String serviceName, int cachedClassCount, boolean closed) {
            this.serviceName = serviceName;
            this.cachedClassCount = cachedClassCount;
            this.closed = closed;
        }
    }
}
