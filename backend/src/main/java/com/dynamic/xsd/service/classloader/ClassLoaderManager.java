package com.dynamic.xsd.service.classloader;

import com.dynamic.xsd.config.DynamicServiceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for ServiceClassLoaders.
 *
 * Responsibilities:
 * - Create and manage isolated classloaders per service
 * - Load classes dynamically
 * - Track loaded classloaders
 * - Cleanup and garbage collection
 * - Prevent memory leaks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassLoaderManager {

    private final DynamicServiceProperties properties;
    private final Map<String, ServiceClassLoader> classLoaders = new ConcurrentHashMap<>();

    /**
     * Creates or retrieves a classloader for the given service.
     *
     * @param serviceName Service name
     * @param classOutputDirectory Directory containing compiled classes
     * @return ServiceClassLoader for the service
     */
    public ServiceClassLoader getOrCreateClassLoader(String serviceName, String classOutputDirectory) {
        return classLoaders.computeIfAbsent(serviceName, key -> {
            log.info("Creating new classloader for service: {}", serviceName);

            Path classPath = Paths.get(classOutputDirectory);
            boolean parentFirst = !properties.getClassloader().getIsolationEnabled();

            ServiceClassLoader classLoader = new ServiceClassLoader(
                serviceName,
                classPath,
                Thread.currentThread().getContextClassLoader(),
                parentFirst
            );

            log.info("Created classloader for service: {} with {} classes",
                serviceName, classLoader.getCachedClassCount());

            return classLoader;
        });
    }

    /**
     * Loads a class using the service's classloader.
     *
     * @param serviceName Service name
     * @param className Fully qualified class name
     * @return Loaded class
     * @throws ClassNotFoundException if class not found
     */
    public Class<?> loadClass(String serviceName, String className) throws ClassNotFoundException {
        ServiceClassLoader classLoader = classLoaders.get(serviceName);
        if (classLoader == null) {
            throw new IllegalStateException("No classloader found for service: " + serviceName);
        }

        if (classLoader.isClosed()) {
            throw new IllegalStateException("ClassLoader for service " + serviceName + " has been closed");
        }

        log.debug("Loading class {} for service {}", className, serviceName);
        return classLoader.loadClass(className);
    }

    /**
     * Loads multiple classes for a service.
     *
     * @param serviceName Service name
     * @param classNames List of class names to load
     * @return List of loaded classes
     */
    public List<Class<?>> loadClasses(String serviceName, List<String> classNames) {
        List<Class<?>> loadedClasses = new ArrayList<>();

        for (String className : classNames) {
            try {
                Class<?> clazz = loadClass(serviceName, className);
                loadedClasses.add(clazz);
                log.debug("Loaded class: {} for service: {}", className, serviceName);
            } catch (ClassNotFoundException e) {
                log.error("Failed to load class: {} for service: {}", className, serviceName, e);
            }
        }

        log.info("Loaded {}/{} classes for service: {}",
            loadedClasses.size(), classNames.size(), serviceName);

        return loadedClasses;
    }

    /**
     * Creates an instance of a class.
     *
     * @param serviceName Service name
     * @param className Class name
     * @return New instance of the class
     * @throws Exception if instantiation fails
     */
    public Object createInstance(String serviceName, String className) throws Exception {
        Class<?> clazz = loadClass(serviceName, className);
        return clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * Removes and closes the classloader for a service.
     *
     * @param serviceName Service name
     */
    public void removeClassLoader(String serviceName) {
        ServiceClassLoader classLoader = classLoaders.remove(serviceName);
        if (classLoader != null) {
            classLoader.close();
            log.info("Removed and closed classloader for service: {}", serviceName);

            // Suggest garbage collection
            System.gc();
        } else {
            log.warn("No classloader found to remove for service: {}", serviceName);
        }
    }

    /**
     * Clears the class cache for a service.
     *
     * @param serviceName Service name
     */
    public void clearCache(String serviceName) {
        ServiceClassLoader classLoader = classLoaders.get(serviceName);
        if (classLoader != null) {
            classLoader.clearCache();
            log.info("Cleared cache for service: {}", serviceName);
        }
    }

    /**
     * Gets the classloader for a service.
     *
     * @param serviceName Service name
     * @return ServiceClassLoader or null if not found
     */
    public ServiceClassLoader getClassLoader(String serviceName) {
        return classLoaders.get(serviceName);
    }

    /**
     * Checks if a classloader exists for a service.
     *
     * @param serviceName Service name
     * @return true if classloader exists
     */
    public boolean hasClassLoader(String serviceName) {
        return classLoaders.containsKey(serviceName);
    }

    /**
     * Gets all active service names.
     *
     * @return List of service names
     */
    public List<String> getAllServiceNames() {
        return new ArrayList<>(classLoaders.keySet());
    }

    /**
     * Gets the number of active classloaders.
     *
     * @return Count of classloaders
     */
    public int getClassLoaderCount() {
        return classLoaders.size();
    }

    /**
     * Gets statistics about classloaders.
     *
     * @return ClassLoader statistics
     */
    public ClassLoaderStats getStats() {
        int totalClasses = classLoaders.values().stream()
            .mapToInt(ServiceClassLoader::getCachedClassCount)
            .sum();

        return new ClassLoaderStats(
            classLoaders.size(),
            totalClasses,
            getAllServiceNames()
        );
    }

    /**
     * Closes all classloaders and clears the registry.
     */
    public void closeAll() {
        log.info("Closing all classloaders ({} total)", classLoaders.size());

        classLoaders.forEach((serviceName, classLoader) -> {
            try {
                classLoader.close();
                log.debug("Closed classloader for service: {}", serviceName);
            } catch (Exception e) {
                log.error("Error closing classloader for service: {}", serviceName, e);
            }
        });

        classLoaders.clear();
        System.gc();

        log.info("All classloaders closed");
    }

    /**
     * ClassLoader statistics.
     */
    public record ClassLoaderStats(
        int classLoaderCount,
        int totalCachedClasses,
        List<String> serviceNames
    ) {}
}
