package src.dao;

import java.util.List;

public interface GenericDAO<T> {
    T findById(String id);
    List<T> findAll();
    void save(T t);
    void update(T t);
    void deleteById(String id);
}