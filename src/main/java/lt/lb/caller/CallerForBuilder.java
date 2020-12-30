package lt.lb.caller;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import lt.lb.caller.util.CheckedBiFunction;
import lt.lb.caller.util.CheckedFunction;
import lt.lb.caller.util.IndexedIterator;

/**
 * @param <R> type that iteration happens
 * @param <T> the main type of Caller product
 * By default every evaluation call ends with {@code continue} and loop return {@code null};
 * @author laim0nas100
 */
public class CallerForBuilder<R, T> {

    protected boolean bulk = false;
    protected IndexedIterator<R> iter;
    protected BiFunction<Integer, R, Caller<T>> contFunction;
    protected BiFunction<Integer, T, CallerFlowControl<T>> thenFunction;
    protected Caller<T> afterwards;

    public CallerForBuilder() {
        thenFunction = (i, c) -> Caller.flowContinue();
        afterwards = Caller.ofNull();
    }

    /**
     *
     * @param stream items to iterate
     * @return builder
     */
    public CallerForBuilder<R, T> with(Stream<R> stream) {
        return with(stream.iterator());
    }

    /**
     *
     * @param iterator items to iterate
     * @return builder
     */
    public CallerForBuilder<R, T> with(Iterator<R> iterator) {
        this.iter = new IndexedIterator<>(iterator);
        return this;
    }

    /**
     *
     * @param iterable items to iterate
     * @return builder
     */
    public CallerForBuilder<R, T> with(Iterable<R> iterable) {
        return with(iterable.iterator());
    }

    /**
     *
     * @param array items to iterate
     * @return builder
     */
    public CallerForBuilder<R, T> with(R... array) {
        return with(Stream.of(array).iterator());
    }

    /**
     * @param afterwards Caller when iterator runs out of items (or never had
     * them to begin with) so {@code for} loop never exited inside or exited
     * with break condition.
     * @return builder
     */
    public CallerForBuilder<R, T> afterwards(Caller<T> afterwards) {
        this.afterwards = afterwards;
        return this;
    }

    /**
     *
     * @return created Caller that models such loop
     */
    public Caller<T> build() {
        Objects.requireNonNull(afterwards);
        Objects.requireNonNull(iter);
        Objects.requireNonNull(contFunction);
        Objects.requireNonNull(thenFunction);
        if (bulk) {
            return CallerImpl.ofIteratorLazyBulk(afterwards, iter, contFunction, thenFunction);
        } else {
            return CallerImpl.ofIteratorLazy(afterwards, iter, contFunction, thenFunction);
        }

    }

    /**
     * How to evaluate each item ignoring indices
     *
     * @param thenFunction evaluation function that gets how to proceed in the
     * middle of a {@code for} loop
     * @return builder
     */
    public CallerForBuilder<R, T> evaluate(Function<T, CallerFlowControl<T>> thenFunction) {
        Objects.requireNonNull(thenFunction);
        return evaluate((i, item) -> thenFunction.apply(item));
    }

    /**
     * How to evaluate each item
     *
     * @param thenFunction evaluation function that gets how to proceed in the
     * middle of a {@code for} loop
     * @return builder
     */
    public CallerForBuilder<R, T> evaluate(BiFunction<Integer, T, CallerFlowControl<T>> thenFunction) {
        this.thenFunction = thenFunction;
        return this;
    }

    /**
     * Create recursive calls for each item in iterator.
     *
     * @param contFunction
     * @return
     */
    public CallerForBuilder<R, T> forEachCall(Function<R, Caller<T>> contFunction) {
        Objects.requireNonNull(contFunction);
        return this.forEachCall((i, item) -> contFunction.apply(item));
    }

    /**
     * Create recursive calls for each item in iterator with unsafe function
     * masking any exceptions.
     *
     * @param contFunction
     * @return
     */
    public CallerForBuilder<R, T> forEachCall(CheckedFunction<R, Caller<T>> contFunction) {
        Objects.requireNonNull(contFunction);
        return this.forEachCall((i, item) -> contFunction.apply(item));
    }

    /**
     * Create recursive calls for each (index,item) pair in iterator.
     *
     * @param contFunction
     * @return
     */
    public CallerForBuilder<R, T> forEachCall(BiFunction<Integer, R, Caller<T>> contFunction) {
        this.contFunction = contFunction;
        return this;
    }

    /**
     * Create recursive calls for each (index,item) pair in iterator with unsafe
     * function masking any exceptions.
     *
     * @param contFunction
     * @return
     */
    public CallerForBuilder<R, T> forEachCall(CheckedBiFunction<Integer, R, Caller<T>> contFunction) {
        this.contFunction = contFunction;
        return this;
    }

}
