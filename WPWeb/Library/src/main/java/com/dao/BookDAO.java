package com.dao;

import com.models.Author;
import com.models.Book;
import jdk.nashorn.api.scripting.JSObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by piotrek on 07.11.15.
 */
@Repository
public class BookDAO extends DatabaseDAO<Book>{


    public void save(Book book) {

        getSession().persist(book);
    }

    public Book get(int key) {

        return getSession().get(com.models.Book.class, key);
    }

    public List<Book> getAll() {

        Query query = getSession().createQuery("from Book");
        List<Book> list = query.list();
        System.out.println(list);
        return list;

    }

    public List<Book> findByColumn(String column, String expression){

        return getSession().createQuery("from Book where " + column + " LIKE lower('%" + expression +"%')").list();

    }


}
