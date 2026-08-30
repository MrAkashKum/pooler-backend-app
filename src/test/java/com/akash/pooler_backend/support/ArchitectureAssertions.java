package com.akash.pooler_backend.support;

import org.junit.jupiter.api.Assertions;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class ArchitectureAssertions {

    private ArchitectureAssertions() {
    }

    public static void assertRestController(Class<?> controllerType) {
        Assertions.assertNotNull(
                AnnotationUtils.findAnnotation(controllerType, RestController.class),
                controllerType.getSimpleName() + " must be annotated with @RestController");
        Assertions.assertTrue(
                Arrays.stream(controllerType.getDeclaredMethods()).anyMatch(ArchitectureAssertions::isRequestHandler),
                controllerType.getSimpleName() + " must expose at least one request handler method");
    }

    public static void assertServiceImplementation(Class<?> serviceType, Class<?> contractType) {
        Assertions.assertNotNull(
                AnnotationUtils.findAnnotation(serviceType, Service.class),
                serviceType.getSimpleName() + " must be annotated with @Service");
        Assertions.assertTrue(
                contractType.isAssignableFrom(serviceType),
                serviceType.getSimpleName() + " must implement " + contractType.getSimpleName());
        Assertions.assertTrue(
                serviceType.getSimpleName().endsWith("Impl"),
                serviceType.getSimpleName() + " must use the Impl suffix");
    }

    public static void assertUtilityClass(Class<?> utilityType) {
        Assertions.assertTrue(
                Modifier.isFinal(utilityType.getModifiers()),
                utilityType.getSimpleName() + " should be final");
        Constructor<?>[] constructors = utilityType.getDeclaredConstructors();
        Assertions.assertEquals(1, constructors.length, utilityType.getSimpleName() + " should have one constructor");
        Assertions.assertTrue(
                Modifier.isPrivate(constructors[0].getModifiers()),
                utilityType.getSimpleName() + " constructor should be private");
    }

    private static boolean isRequestHandler(Method method) {
        return AnnotationUtils.findAnnotation(method, RequestMapping.class) != null
                || AnnotationUtils.findAnnotation(method, GetMapping.class) != null
                || AnnotationUtils.findAnnotation(method, PostMapping.class) != null
                || AnnotationUtils.findAnnotation(method, PutMapping.class) != null
                || AnnotationUtils.findAnnotation(method, PatchMapping.class) != null
                || AnnotationUtils.findAnnotation(method, DeleteMapping.class) != null;
    }
}
