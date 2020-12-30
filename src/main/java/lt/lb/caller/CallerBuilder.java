package lt.lb.caller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import lt.lb.caller.util.CastList;

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
        if(dependants == null){
            dependants = new ArrayList<>(deps.length);
        }
        for (Caller<T> d : deps) {
            this.dependants.add(d);
        }
        return this;
    }

    public CallerBuilder<T> withDependencyCall(Function<CastList<T>, Caller<T>> call) {
        return this.with(Caller.ofFunction(call));
    }

    public CallerBuilder<T> withDependencyResult(Function<CastList<T>, T> call) {
        return this.with(Caller.ofResultCall(call));
    }

    public CallerBuilder<T> withDependencySupp(Supplier<Caller<T>> call) {
        return this.with(Caller.ofSupplier(call));
    }

    public CallerBuilder<T> withDependencySuppResult(Supplier<T> call) {
        return this.with(Caller.ofSupplierResult(call));
    }

    public CallerBuilder<T> withDependencyResult(T res) {
        return this.with(Caller.ofResult(res));
    }

    public Caller<T> toResultCall(Function<CastList<T>, T> call) {
        return toCall(args -> Caller.ofResult(call.apply(args)));
    }

    public Caller<T> toResultCall(Supplier<T> call) {
        return toCall(args -> Caller.ofResult(call.get()));
    }

    public Caller<T> toCall(Function<CastList<T>, Caller<T>> call) {
        if (sharedMutable) {
            return new Caller<>(Caller.CallerType.SHARED, null, call, this.dependants);
        } else {
            return new Caller<>(Caller.CallerType.FUNCTION, null, call, this.dependants);
        }
    }

    public Caller<T> toCall(Supplier<Caller<T>> call) {
        return toCall(args -> Caller.ofSupplier(call));
    }
}
