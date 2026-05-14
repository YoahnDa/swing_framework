package com.framework.repository;

import java.util.HashMap;
import java.util.Map;

public class RepositoryRegistry {
    private static final Map<Class<?>, AbstractRepository<?>> registries = new HashMap<>();

    public static <T> void register(Class<T> clazz, AbstractRepository<T> repository) {
        registries.put(clazz, repository);
    }

    public static AbstractRepository<?> getRepository(Class<?> clazz) {
        return registries.get(clazz);
    }
}