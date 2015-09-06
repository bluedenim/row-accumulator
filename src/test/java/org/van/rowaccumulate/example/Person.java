package org.van.rowaccumulate.example;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by vly on 9/4/2015.
 */
public class Person {
    public long id;
    public String firstName;
    public String lastName;

    public List<Identity> identityList;
    public List<Dish> dishes;

    public Person() {
        identityList = new LinkedList<>();
        dishes = new LinkedList<>();
    }

    public Long getId() {
        return id;
    }

    public static Person fromMap(Map<String,String> map) {
        Person inst = new Person();
        inst.id = Long.parseLong(map.get("id"));
        inst.firstName = map.get("firstName");
        inst.lastName = map.get("lastName");
        return inst;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("{%d:%s, %s; ", id, lastName, firstName));
        sb.append(dump(identityList, "Identities")).append("; ");
        sb.append(dump(dishes, "Dishes"));
        sb.append('}');
        return sb.toString();
    }

    <T> String dump(Collection<T> tCollection, String label) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: [", label));
        for (T t: tCollection) {
            sb.append(" ").append(t.toString());
        }
        sb.append(" ]");
        return sb.toString();
    }
}
