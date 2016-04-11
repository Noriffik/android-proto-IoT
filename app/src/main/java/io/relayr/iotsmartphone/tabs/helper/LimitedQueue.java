package io.relayr.iotsmartphone.tabs.helper;

import java.util.LinkedList;

public class LimitedQueue<E> extends LinkedList<E> {

    private int limit;

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        while (size() > limit) {
            remove();
        }
        return true;
    }
}
