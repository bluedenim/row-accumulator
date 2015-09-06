package org.van.rowaccumulate.example;

/**
 * Created by vly on 9/4/2015.
 */
public class Identity {

    public String type;
    public String value;

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("{%s:%s}", type, value);
    }
}
