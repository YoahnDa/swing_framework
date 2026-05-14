package com.framework.repository;

import com.framework.dao.GenericDAO;
import com.framework.utils.Filtre;
import com.framework.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

public abstract class AbstractRepository<T> {

    protected GenericDAO dao = new GenericDAO();
    protected Class<T> entityClass;

    public AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T save(T entity) throws Exception {
        Field idField = ReflectionUtils.getIdField(entityClass);
        Object idValue = ReflectionUtils.getFieldValue(entity, idField);
        if (idValue == null || (idValue instanceof Number && ((Number) idValue).intValue() == 0)) {
            dao.insert(entity);
        } else {
            dao.update(entity);
        }
        return entity;
    }

    public T findById(Object id) throws Exception {
        return dao.findById(entityClass, id);
    }

    public List<T> findAll() throws Exception {
        return dao.findAll(entityClass);
    }

    public void delete(T entity) throws Exception {
        dao.delete(entity);
    }

    public void deleteById(Object id) throws Exception {
        T entity = findById(id);
        if (entity != null) {
            dao.delete(entity);
        }
    }
    
    public List<T> find(List<Filtre> filtres) throws Exception {
        return dao.find(entityClass, filtres);
    }
}