package lt.lb.caller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import lt.lb.caller.util.CastList;
import lt.lb.caller.util.CheckedFunction;

/**
 *
 * @author laim0nas100
 */
public class CallerBuilder<T> {

    /**
     * Create new caller
     *
     * @param size expected dependencies (for better performance)
     */
    public CallerBuilder(int size) {
        dependants = new ArrayList<>(size);
    }

    public CallerBuilder() {
    }

    protected boolean sharedMutable = false;
    protected List<Caller<T>> dependants;

    public CallerBuilder<T> with(Caller<T>... deps) {
        if (dependants == null) {
            dependants = new ArrayList<>(deps.length);
        }
        for (Caller<T> d : deps) {
            this.dependants.add(d);
        }
        return this;
    }

    public CallerBuilder<T> withDependencyCall(CheckedFunction<CastList<T>, Caller<T>> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofFunction(call));
    }

    public CallerBuilder<T> withDependencyResult(CheckedFunction<CastList<T>, T> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofResultCall(call));
    }

    public CallerBuilder<T> withDependencySupp(Callable<Caller<T>> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofCallable(call));
    }

    public CallerBuilder<T> withDependencySuppResult(Callable<T> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofCallableResult(call));
    }

    public CallerBuilder<T> withDependencyResult(T res) {
        return with(Caller.ofResult(res));
    }

    public Caller<T> toResultCall(CheckedFunction<CastList<T>, T> call) {
        Objects.requireNonNull(call);
        return toCall(args -> Caller.ofResult(call.apply(args)));
    }

    public Caller<T> toResultCall(Callable<T> call) {
        Objects.requireNonNull(call);
        return toCall(args -> Caller.ofResult(call.call()));
    }

    public Caller<T> toCall(CheckedFunction<CastList<T>, Caller<T>> call) {
        if (sharedMutable) {
            return new Caller<>(Caller.CallerType.SHARED, null, call, this.dependants);
        } else {
            return new Caller<>(Caller.CallerType.FUNCTION, null, call, this.dependants);
        }
    }

    public Caller<T> toCall(Callable<Caller<T>> call) {
        Objects.requireNonNull(call);
        return toCall(args -> Caller.ofCallable(call));
    }
}
