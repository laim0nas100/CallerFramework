package lt.lb.caller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import lt.lb.caller.util.CastList;
import lt.lb.caller.util.CheckedFunction;
import lt.lb.caller.util.CheckedRunnable;

/**
 *
 * @author laim0nas100
 */
public class CallerBuilder<T> {

    /**
     * Create new caller builder
     *
     * @param size expected dependencies (for better performance)
     */
    public CallerBuilder(int size) {
        dependants = new ArrayList<>(size);
    }

    /**
     * Create new caller builder
     */
    public CallerBuilder() {
    }

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

    public CallerBuilder<T> with(Collection<Caller<T>> deps) {
        if (dependants == null) {
            dependants = new ArrayList<>(deps.size());
        }
        dependants.addAll(deps);
        return this;
    }

    public CallerBuilder<T> with(CheckedFunction<CastList<T>, Caller<T>> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofFunction(call));
    }

    public CallerBuilder<T> withRes(CheckedFunction<CastList<T>, T> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofResultCall(call));
    }

    public CallerBuilder<T> with(Callable<Caller<T>> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofCallable(call));
    }

    public CallerBuilder<T> withRes(Callable<T> call) {
        Objects.requireNonNull(call);
        return with(Caller.ofCallableResult(call));
    }

    public CallerBuilder<T> withRes(T res) {
        return with(Caller.ofResult(res));
    }

    public CallerBuilder<T> with(CheckedRunnable run) {
        Objects.requireNonNull(run);
        return with(Caller.ofRunnable(run));
    }

    public Caller<T> toResultCall(CheckedFunction<CastList<T>, T> call) {
        Objects.requireNonNull(call);
        return toCall(args -> Caller.ofResult(call.apply(args)));
    }

    public Caller<T> toResultCallMemo(CheckedFunction<CastList<T>, T> call) {
        Objects.requireNonNull(call);
        return toCallMemo(args -> Caller.ofResult(call.apply(args)));
    }

    public Caller<T> toResultCall(Callable<T> call) {
        Objects.requireNonNull(call);
        return toCall(args -> Caller.ofResult(call.call()));
    }

    public Caller<T> toResultCallMemo(Callable<T> call) {
        Objects.requireNonNull(call);
        return toCallMemo(args -> Caller.ofResult(call.call()));
    }

    public Caller<T> toCall(CheckedFunction<CastList<T>, Caller<T>> call) {
        Objects.requireNonNull(call);
        return new Caller<>(Caller.CallerType.FUNCTION, null, call, this.dependants);
    }

    public Caller<T> toCallMemo(CheckedFunction<CastList<T>, Caller<T>> call) {
        Objects.requireNonNull(call);
        return new Caller<>(Caller.CallerType.MEMOIZING, null, call, this.dependants);
    }

    public Caller<T> toCall(Callable<Caller<T>> call) {
        Objects.requireNonNull(call);
        return toCall(args -> Caller.ofCallable(call));
    }

    public Caller<T> toCallMemo(Callable<Caller<T>> call) {
        Objects.requireNonNull(call);
        return toCallMemo(args -> Caller.ofCallable(call));
    }

    public Caller<T> toRunnable(CheckedRunnable run) {
        Objects.requireNonNull(run);
        return toCall(args -> Caller.ofRunnable(run));
    }

    public Caller<T> toRunnableMemo(CheckedRunnable run) {
        Objects.requireNonNull(run);
        return toCallMemo(args -> Caller.ofRunnable(run));
    }
}
