package org.van.rowaccumulate.example;

/**
 * Created by vly on 9/4/2015.
 */
public class Dish {
    public String name;

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("{name:%s}", name);
    }
}
