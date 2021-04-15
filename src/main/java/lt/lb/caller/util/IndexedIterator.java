package lt.lb.caller.util;

import java.util.Iterator;

/**
 *
 * @author laim0nas100
 */
public class IndexedIterator<T> implements Iterator<T> {

    public IndexedIterator(int index, Iterator<T> iter) {
        this.iter = iter;
        this.index = index;
    }

    public IndexedIterator(Iterator<T> iter) {
        this(-1, iter);
    }

    private int index;
    private final Iterator<T> iter;

    public int getCurrentIndex() {
        return index;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public T next() {
        T next = iter.next();
        index++;
        return next;
    }

    public IndexedValue<T> nextIndexed() {
        T next = next();
        return new IndexedValue<>(index, next);
    }

    public static class IndexedValue<T> {

        public final int index;
        public final T value;

        public IndexedValue(int index, T value) {
            this.index = index;
            this.value = value;
        }
    }
}
