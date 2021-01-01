package lt.lb.caller.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * Supports 5 explicitly defined arguments and much more implicitly defined by
 * lists. By default other parameters are null;
 *
 * @param <T> base type or arguments
 * @author laim0nas100
 */
public class CastList<T> implements Iterable<T> {

    protected final List<T> args;
    /**
     * Amount of parameters passed here
     */
    public final int parameterCount;

    private T assign(int index) {
        return parameterCount > index ? args.get(index) : null;
    }

    public CastList(List<T> list) {
        this.args = list;
        parameterCount = list == null ? 0 : args.size();
        _0 = assign(0);
        _1 = assign(1);
        _2 = assign(2);
        _3 = assign(3);
        _4 = assign(4);
    }

    /**
     * @param i index
     * @return 0-based indexed parameter
     */
    public T get(int i) {
        if (parameterCount > i) {
            return args.get(i);
        }
        throw new IndexOutOfBoundsException(i + " size:" + parameterCount);
    }

    /**
     *
     * @param <R> Type of cast
     * @param i index
     * @return 0-based indexed parameter with performed cast
     */
    public <R> R cget(int i) {
        return (R) get(i);
    }

    /**
     * 1-st parameter
     */
    public final T _0;
    /**
     * 2-nd parameter
     */
    public final T _1;
    /**
     * 3-rd parameter
     */
    public final T _2;
    /**
     * 4-th parameter
     */
    public final T _3;
    /**
     * 5-th parameter
     */
    public final T _4;

    @Override
    public Iterator<T> iterator() {

        if (args != null) {
            Iterator<T> iterator = args.iterator();
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public T next() {
                    return iterator.next();
                }
            };
        } else {
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public T next() {
                    throw new IndexOutOfBoundsException("No elements");
                }
            };
        }

    }

    public ArrayList<T> asList() {
        if (args == null) {
            return new ArrayList<>(0);
        }
        return new ArrayList<>(args);
    }

    @Override
    public String toString() {
        if (args == null) {
            return "[]";
        }
        return args.toString();
    }

}
