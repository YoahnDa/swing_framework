package com.framework.dao;

import com.framework.annotation.JoinColumn;
import com.framework.utils.Filtre;
import com.framework.connection.DBConnection;
import com.framework.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GenericDAO {
    public <T> T insert(T entity) throws Exception {
        Class<?> clazz = entity.getClass();
        String tableName = ReflectionUtils.getTableName(clazz);
        Field idField = ReflectionUtils.getIdField(clazz);
        List<Field> mappedFields = ReflectionUtils.getMappedFields(clazz);

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<Object> parameters = new ArrayList<>();

        for (Field field : mappedFields) {
            if (field.equals(idField) && ReflectionUtils.isIdAutoIncrement(idField)) {
                continue;
            }else if (ReflectionUtils.isJoinColumn(field)) {
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                sql.append(join.name()).append(", "); 
                values.append("?, ");
                parameters.add(ReflectionUtils.getJoinColumnValue(entity, field)); 
                continue;
            }
            sql.append(ReflectionUtils.getColumnName(field)).append(", ");
            values.append("?, ");
            parameters.add(ReflectionUtils.getFieldValue(entity, field));
        }

        sql.setLength(sql.length() - 2);
        values.setLength(values.length() - 2);
        sql.append(") ").append(values).append(")");

        Connection conn = DBConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            pstmt.executeUpdate();

            if (ReflectionUtils.isIdAutoIncrement(idField)) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Object idValue = generatedKeys.getObject(1);
                        ReflectionUtils.setFieldValue(entity, idField, idValue);
                    }
                }
            }

            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        }
        return entity;
    }

    public void update(Object entity) throws Exception {
        Class<?> clazz = entity.getClass();
        String tableName = ReflectionUtils.getTableName(clazz);
        Field idField = ReflectionUtils.getIdField(clazz);
        List<Field> mappedFields = ReflectionUtils.getMappedFields(clazz);

        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        List<Object> parameters = new ArrayList<>();

        for (Field field : mappedFields) {
            if (field.equals(idField)) continue; 

            if (ReflectionUtils.isJoinColumn(field)) {
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                sql.append(join.name()).append(" = ?, ");
                
                parameters.add(ReflectionUtils.getJoinColumnValue(entity, field));
            } else {
                sql.append(ReflectionUtils.getColumnName(field)).append(" = ?, ");
                
                parameters.add(ReflectionUtils.getFieldValue(entity, field));
            }
        }

        sql.setLength(sql.length() - 2); 
        
        sql.append(" WHERE ").append(ReflectionUtils.getColumnName(idField)).append(" = ?");
        parameters.add(ReflectionUtils.getFieldValue(entity, idField));

        Connection conn = DBConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            pstmt.executeUpdate();
            
            if (!conn.getAutoCommit()) conn.commit();
        }
    }

    public void delete(Object entity) throws Exception {
        Class<?> clazz = entity.getClass();
        String tableName = ReflectionUtils.getTableName(clazz);
        Field idField = ReflectionUtils.getIdField(clazz);

        String sql = "DELETE FROM " + tableName + " WHERE " + ReflectionUtils.getColumnName(idField) + " = ?";
        Object idValue = ReflectionUtils.getFieldValue(entity, idField);

        Connection conn = DBConnection.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, idValue);
            pstmt.executeUpdate();

            if (!conn.getAutoCommit())
                conn.commit();
        }
    }

    public <T> List<T> findAll(Class<T> clazz) throws Exception {
        List<T> resultList = new ArrayList<>();
        String tableName = ReflectionUtils.getTableName(clazz);
        String sql = "SELECT * FROM " + tableName;
    
        Connection conn = DBConnection.getConnection(true);
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                resultList.add(mapResultSetToObject(rs, clazz));
            }
        }
        return resultList;
    }

    public <T> List<T> insertAll(List<T> entities) throws Exception {
        if (entities == null || entities.isEmpty())
            return entities;

        Class<?> clazz = entities.get(0).getClass();
        String tableName = ReflectionUtils.getTableName(clazz);
        Field idField = ReflectionUtils.getIdField(clazz);
        List<Field> mappedFields = ReflectionUtils.getMappedFields(clazz);

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder("VALUES (");

        List<Field> fieldsToInsert = new ArrayList<>();
        for (Field field : mappedFields) {
            if (field.equals(idField) && ReflectionUtils.isIdAutoIncrement(idField)) {
                continue;
            }
            
            if (ReflectionUtils.isJoinColumn(field)) {
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                sql.append(join.name()).append(", "); 
            } else {
                sql.append(ReflectionUtils.getColumnName(field)).append(", ");
            }
            
            values.append("?, ");
            fieldsToInsert.add(field);
        }

        sql.setLength(sql.length() - 2);
        values.setLength(values.length() - 2);
        sql.append(") ").append(values).append(")");

        Connection conn = DBConnection.getConnection(false);
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {

            for (Object entity : entities) {
                for (int i = 0; i < fieldsToInsert.size(); i++) {
                    Field field = fieldsToInsert.get(i);
                    Object valueToInsert;
                    if (ReflectionUtils.isJoinColumn(field)) {
                        valueToInsert = ReflectionUtils.getJoinColumnValue(entity, field);
                    } else {
                        valueToInsert = ReflectionUtils.getFieldValue(entity, field);
                    }

                    pstmt.setObject(i + 1, valueToInsert);
                }
                pstmt.addBatch();
            }

            pstmt.executeBatch();

            if (ReflectionUtils.isIdAutoIncrement(idField)) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    int i = 0;
                    while (rs.next() && i < entities.size()) {
                        Object idValue = rs.getObject(1);
                        ReflectionUtils.setFieldValue(entities.get(i), idField, idValue);
                        i++;
                    }
                }
            }

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
        return entities;
    }

    public void updateAll(List<?> entities) throws Exception {
        if (entities == null || entities.isEmpty()) return;

        Class<?> clazz = entities.get(0).getClass();
        String tableName = ReflectionUtils.getTableName(clazz);
        Field idField = ReflectionUtils.getIdField(clazz);
        List<Field> mappedFields = ReflectionUtils.getMappedFields(clazz);

        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        List<Field> fieldsToUpdate = new ArrayList<>();

        for (Field field : mappedFields) {
            if (field.equals(idField)) continue;

            if (ReflectionUtils.isJoinColumn(field)) {
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                sql.append(join.name()).append(" = ?, ");
            } else {
                sql.append(ReflectionUtils.getColumnName(field)).append(" = ?, ");
            }
            fieldsToUpdate.add(field);
        }

        sql.setLength(sql.length() - 2);
        sql.append(" WHERE ").append(ReflectionUtils.getColumnName(idField)).append(" = ?");

        Connection conn = DBConnection.getConnection(false);
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (Object entity : entities) {
                int paramIndex = 1;
                for (Field field : fieldsToUpdate) {
                    Object valueToUpdate;

                    if (ReflectionUtils.isJoinColumn(field)) {
                        valueToUpdate = ReflectionUtils.getJoinColumnValue(entity, field);
                    } else {
                        valueToUpdate = ReflectionUtils.getFieldValue(entity, field);
                    }

                    pstmt.setObject(paramIndex++, valueToUpdate);
                }

                pstmt.setObject(paramIndex, ReflectionUtils.getFieldValue(entity, idField));
                
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit();
            
        } catch (Exception e) {
            conn.rollback();
            throw new Exception("Erreur lors de la mise à jour en batch, transaction annulee.", e);
        }
    }

    public void deleteAll(List<?> entities) throws Exception {
        if (entities == null || entities.isEmpty())
            return;

        Class<?> clazz = entities.get(0).getClass();
        String tableName = ReflectionUtils.getTableName(clazz);
        Field idField = ReflectionUtils.getIdField(clazz);

        String sql = "DELETE FROM " + tableName + " WHERE " + ReflectionUtils.getColumnName(idField) + " = ?";

        Connection conn = DBConnection.getConnection(false);
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Object entity : entities) {
                pstmt.setObject(1, ReflectionUtils.getFieldValue(entity, idField));
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();

        } catch (Exception e) {
            conn.rollback();
            throw new Exception("Erreur lors de la suppression en batch, transaction annulee.", e);
        }
    }

    public <T> T findById(Class<T> clazz, Object idValue) throws Exception {
        if (idValue == null) return null;
    
        Field idField = ReflectionUtils.getIdField(clazz);
        String tableName = ReflectionUtils.getTableName(clazz);
        String sql = "SELECT * FROM " + tableName + " WHERE " + ReflectionUtils.getColumnName(idField) + " = ?";
    
        Connection conn = DBConnection.getConnection(true);
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, idValue);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToObject(rs, clazz);
                }
            }
        }
        return null;
    }

    private <T> T mapResultSetToObject(ResultSet rs, Class<T> clazz) throws Exception {
        T obj = clazz.getDeclaredConstructor().newInstance();
        List<Field> mappedFields = ReflectionUtils.getMappedFields(clazz);
    
        for (Field field : mappedFields) {
            if (ReflectionUtils.isJoinColumn(field)) {
                JoinColumn join = field.getAnnotation(JoinColumn.class);
                Object fkValue = rs.getObject(join.name());
    
                if (fkValue != null && join.eager()) {
                    Object parentObject = findById(field.getType(), fkValue);
                    ReflectionUtils.setFieldValue(obj, field, parentObject);
                } 
            } else {
                String columnName = ReflectionUtils.getColumnName(field);
                Object value = rs.getObject(columnName);
                ReflectionUtils.setFieldValue(obj, field, value);
            }
        }
        return obj;
    }

    public <T> List<T> find(Class<T> clazz, List<Filtre> filtres) throws Exception {
        List<T> resultList = new ArrayList<>();
        String tableName = ReflectionUtils.getTableName(clazz);
        
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        List<Object> parameters = new ArrayList<>();

        if (filtres != null && !filtres.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < filtres.size(); i++) {
                com.framework.utils.Filtre filtre = filtres.get(i);
                
                String columnName = ReflectionUtils.getColumnNameFromAttribute(clazz, filtre.getAttributJava());
                
                sql.append(columnName).append(" ").append(filtre.getOperateur()).append(" ?");
                parameters.add(filtre.getValeur());
                
                if (i < filtres.size() - 1) {
                    sql.append(" AND ");
                }
            }
        }

        Connection conn = DBConnection.getConnection(true); 
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultList.add(mapResultSetToObject(rs, clazz));
                }
            }
        }
        return resultList;
    }

    public <T> List<T> findByCustomSql(Class<T> clazz, String sql, Object... parameters) throws Exception {
        List<T> resultList = new ArrayList<>();
        
        Connection conn = DBConnection.getConnection(true);
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    pstmt.setObject(i + 1, parameters[i]);
                }
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultList.add(mapResultSetToObject(rs, clazz));
                }
            }
        }
        return resultList;
    }

    public Object queryForSingleValue(String sql, Object... parameters) throws Exception {
        Connection conn = DBConnection.getConnection(true);
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    pstmt.setObject(i + 1, parameters[i]);
                }
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1); 
                }
            }
        }
        return null;
    }
}
