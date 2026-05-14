package com.framework.utils;

import com.framework.annotation.Column;
import com.framework.annotation.Id;
import com.framework.annotation.JoinColumn;
import com.framework.annotation.Table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils {
    public static String getTableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            Table table = clazz.getAnnotation(Table.class);
            return table.name();
        }
        return clazz.getSimpleName().toLowerCase();
    }

    public static String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            return column.name();
        }
        return field.getName().toLowerCase();
    }

    public static Field getIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new RuntimeException("Aucune cle primaire (@Id) trouver dans la classe " + clazz.getName());
    }

    public static List<Field> getMappedFields(Class<?> clazz) {
        List<Field> mappedFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(JoinColumn.class)) {
                mappedFields.add(field);
            }
        }
        return mappedFields;
    }

    public static Object getFieldValue(Object obj, Field field) {
        try {
            field.setAccessible(true); 
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Impossible de lire l'attribut " + field.getName(), e);
        }
    }

    public static void setFieldValue(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            
            // On passe la valeur brute dans notre convertisseur
            Object convertedValue = convertType(field.getType(), value);
            
            // On injecte la valeur convertie
            field.set(obj, convertedValue);
            
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Erreur de typage sur l'attribut '" + field.getName() + 
                                       "'. Type attendu : " + field.getType().getSimpleName() + 
                                       ", Type recu : " + (value != null ? value.getClass().getSimpleName() : "null"), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Impossible d'ecrire dans l'attribut " + field.getName(), e);
        }
    }
    
    public static boolean isIdAutoIncrement(Field idField) {
        if (idField.isAnnotationPresent(Id.class)) {
            return idField.getAnnotation(Id.class).autoIncrement();
        }
        return false;
    }

    public static Object convertType(Class<?> targetType, Object dbValue) {
        if (dbValue == null) {
            return null;
        }

        if (targetType.isAssignableFrom(dbValue.getClass())) {
            return dbValue;
        }

        if (dbValue instanceof Number) {
            Number num = (Number) dbValue;
            if (targetType == int.class || targetType == Integer.class) return num.intValue();
            if (targetType == long.class || targetType == Long.class) return num.longValue();
            if (targetType == double.class || targetType == Double.class) return num.doubleValue();
            if (targetType == float.class || targetType == Float.class) return num.floatValue();
            if (targetType == short.class || targetType == Short.class) return num.shortValue();
        }

        if (dbValue instanceof java.sql.Date) {
            java.sql.Date sqlDate = (java.sql.Date) dbValue;
            if (targetType == java.util.Date.class) {
                return new java.util.Date(sqlDate.getTime());
            } else if (targetType == java.time.LocalDate.class) {
                return sqlDate.toLocalDate();
            }
        }

        if (dbValue instanceof java.sql.Timestamp) {
            java.sql.Timestamp timestamp = (java.sql.Timestamp) dbValue;
            if (targetType == java.util.Date.class) {
                return new java.util.Date(timestamp.getTime());
            } else if (targetType == java.time.LocalDateTime.class) {
                return timestamp.toLocalDateTime();
            }
        }

        if (targetType == boolean.class || targetType == Boolean.class) {
            if (dbValue instanceof Number) {
                return ((Number) dbValue).intValue() != 0;
            }
        }

        return dbValue;
    }

    public static boolean isEntity(Class<?> clazz) {
        return clazz.isAnnotationPresent(Table.class);
    }

    public static Field getIdFieldOrNull(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) return field;
        }
        return null;
    }

    public static Object getJoinColumnValue(Object entity, Field field) {
        Object subObject = getFieldValue(entity, field);
        if (subObject == null) return null;

        Field idField = getIdField(subObject.getClass());
        return getFieldValue(subObject, idField);
    }

    public static boolean isJoinColumn(Field field) {
        return field.isAnnotationPresent(JoinColumn.class);
    }
    
    public static String getColumnNameFromAttribute(Class<?> clazz, String attributeName) {
        try {
            Field field = clazz.getDeclaredField(attributeName);
            if (isJoinColumn(field)) {
                return field.getAnnotation(JoinColumn.class).name();
            } 
            else if (field.isAnnotationPresent(Column.class)) {
                return field.getAnnotation(Column.class).name();
            } 
            return field.getName().toLowerCase();
            
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("L'attribut '" + attributeName + "' n'existe pas dans la classe " + clazz.getSimpleName());
        }
    }

    /**
     * Surcharge : Permet d'injecter une valeur en connaissant juste le nom de l'attribut (String).
     */
    public static void setFieldValue(Object entity, String fieldName, Object value) throws Exception {
        // On récupère le Field à partir de son nom
        Field field = entity.getClass().getDeclaredField(fieldName);
        
        // On appelle la méthode originale que tu as déjà codée
        setFieldValue(entity, field, value);
    }

    /**
     * Surcharge : Permet de récupérer une valeur en connaissant juste le nom de l'attribut (String).
     */
    public static Object getFieldValue(Object entity, String fieldName) throws Exception {
        // On cherche le Field correspondant au nom
        Field field = entity.getClass().getDeclaredField(fieldName);
        
        // On appelle ta méthode originale qui gère déjà l'accessibilité (setAccessible)
        return getFieldValue(entity, field);
    }
}