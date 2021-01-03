package lt.lb.caller.util;

import java.util.Iterator;
import java.util.function.Function;

/**
 *
 * @author laim0nas100
 */
public class TailRecursionInterator<T> implements Iterator<T>, Iterable<T> {

    Function<T, T> nextValFunc;
    T lastReturn;
    T next;

    public TailRecursionInterator(T first, Function<T, T> nextVal) {
        lastReturn = null;
        next = first;
        this.nextValFunc = nextVal;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public T next() {
        lastReturn = next;
        next = nextValFunc.apply(next);
        return lastReturn;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

}
