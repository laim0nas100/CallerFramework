package lt.lb.caller;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import lt.lb.caller.util.CastList;
import lt.lb.caller.util.CheckedFunction;
import lt.lb.fastid.FastID;
import lt.lb.fastid.FastIDGen;

/**
 * Recursion avoiding function modeling. Main purpose: write a recursive
 * function. If likely to get stack overflown, use this framework to replace
 * every recursive call with Caller equivalent, without needing to design an
 * iterative solution.
 *
 * Performance and memory penalties are self-evident. Is not likely to be faster
 * than well-made iterative solution.
 *
 * This class is immutable.
 *
 * @author laim0nas100
 * @param <T> Most general type of return result (and arguments) that this
 * caller is used to model.
 */
public class Caller<T> {

    private static List emptyList = Arrays.asList();

    /**
     * What to put if stack limit is disabled
     */
    public static final int DISABLED_STACK_LIMIT = -1;
    /**
     * What to put if call limit is disabled
     */
    public static final long DISABLED_CALL_LIMIT = -1L;
    /**
     * DEFAULT fork count
     */
    public static final int DEFAULT_FORK_COUNT = 10;

    private static final FastIDGen idgen = new FastIDGen();
    private static final Caller<?> emptyResultCaller = new Caller<>(CallerType.RESULT, null, null, emptyList);

    public static enum CallerType {
        RESULT, FUNCTION, SHARED
    }

    public final CallerType type;
    public final FastID id;
    protected final T value;
    protected final Function<CastList<T>, Caller<T>> call;
    protected final List<Caller<T>> dependencies;

    /**
     * Shared things. This actually stores a value and a caller object, but the
     * value is stored as a result of a future.
     */
    protected final CompletableFuture<T> compl;
    protected final AtomicBoolean started;

    public boolean isSharedDone() {
        return type == CallerType.SHARED && compl.isDone();
    }

    public boolean isSharedNotDone() {
        return type == CallerType.SHARED && !compl.isDone();
    }

    public static <T> CallerBuilder<T> builder() {
        return new CallerBuilder<>();
    }

    public static <T> CallerBuilder<T> builder(int deps) {
        return new CallerBuilder<>(deps);
    }

    public static <T> SharedCallerBuilder<T> builderShared() {
        return new SharedCallerBuilder<>();
    }

    public static <T> SharedCallerBuilder<T> builderShared(int deps) {
        return new SharedCallerBuilder<>(deps);
    }

    public static <R, T> CallerForBuilder<R, T> builderFor() {
        return new CallerForBuilder<>();
    }

    public static <R, T> CallerForBuilder<R, T> builderForBulk() {
        return new CallerForBuilderBulk<>();
    }

    public static <T> CallerWhileBuilder<T> builderWhile() {
        return new CallerWhileBuilder<>();
    }
    
    public static <T> CallerDoWhileBuilder<T> builderDoWhile() {
        return new CallerDoWhileBuilder<>();
    }

    /**
     * Signify {@code for} loop end inside {@code Caller for} loop. Equivalent
     * of using {@code return} with recursive function call.
     *
     * @param <T>
     * @param next next Caller object
     * @return
     */
    public static <T> CallerFlowControl<T> flowReturn(Caller<T> next) {
        return new CallerFlowControl<>(next, CallerFlowControl.CallerForType.RETURN);
    }

    /**
     * Signify {@code for} loop end inside {@code Caller for} loop. Equivalent
     * of using {@code return} with value.
     *
     * @param <T>
     * @param next next Caller object
     * @return
     */
    public static <T> CallerFlowControl<T> flowReturn(T result) {
        return flowReturn(Caller.ofResult(result));
    }

    /**
     * Signify {@code for} loop end inside {@code Caller for} loop. Equivalent
     * of using {@code break}.
     *
     * @param <T>
     * @return
     */
    public static <T> CallerFlowControl<T> flowBreak() {
        return new CallerFlowControl<>(null, CallerFlowControl.CallerForType.BREAK);
    }

    /**
     * Signify {@code for} loop continue inside {@code Caller for} loop.
     *
     * @param <T>
     * @return
     */
    public static <T> CallerFlowControl<T> flowContinue() {
        return new CallerFlowControl<>(null, CallerFlowControl.CallerForType.CONTINUE);
    }

    /**
     * Create a Caller that has a result (terminating)
     *
     * @param <T>
     * @param result
     * @return Caller, that has a result
     */
    public static <T> Caller<T> ofResult(T result) {
        if (result == null) {
            return (Caller<T>) emptyResultCaller;
        }
        return new Caller<>(CallerType.RESULT, result, null, emptyList);
    }

    /**
     * @param <T>
     * @return Caller, that has a result null
     */
    public static <T> Caller<T> ofNull() {
        return (Caller<T>) emptyResultCaller;
    }

    /**
     * Caller modeling a recursive tail-call
     *
     * @param <T>
     * @param call
     * @return Caller, with recursive tail call
     */
    public static <T> Caller<T> ofFunction(CheckedFunction<CastList<T>, Caller<T>> call) {
        Objects.requireNonNull(call);
        return new Caller<>(CallerType.FUNCTION, null, call, emptyList);
    }

    /**
     * Caller modeling a recursive tail-call wrapping in supplier
     *
     * @param <T>
     * @param call
     * @return Caller, with recursive tail call
     */
    public static <T> Caller<T> ofCallable(Callable<Caller<T>> call) {
        Objects.requireNonNull(call);
        return ofFunction(args -> call.call());
    }

    /**
     * Caller that has a result (terminating) wrapping in supplier
     *
     * @param <T>
     * @param call
     * @return Caller that ends up as a result
     */
    public static <T> Caller<T> ofCallableResult(Callable<T> call) {
        Objects.requireNonNull(call);
        return ofFunction(args -> ofResult(call.call()));
    }

    /**
     * Caller that has a result (terminating) after calling a function once
     *
     * @param <T>
     * @param call
     * @return Caller that ends up as a result
     */
    public static <T> Caller<T> ofResultCall(CheckedFunction<CastList<T>, T> call) {
        Objects.requireNonNull(call);
        return ofFunction(args -> ofResult(call.apply(args)));
    }

    /**
     * Main constructor
     *
     * @param nextCall
     */
    Caller(CallerType type, T result, CheckedFunction<CastList<T>, Caller<T>> nextCall, List<Caller<T>> dependencies) {
        this.type = type;
        this.value = result;
        this.call = nextCall;
        this.dependencies = dependencies;
        if (type == CallerType.SHARED) {
            this.compl = new CompletableFuture<>();
            this.started = new AtomicBoolean(false);
        } else {
            this.compl = null;
            this.started = null;
        }
        this.id = idgen.getAndIncrement();
    }

    /**
     * Construct Caller loop end with {@code return} from this caller
     *
     * @return
     */
    public CallerFlowControl<T> toFlowReturn() {
        return Caller.flowReturn(this);
    }

    /**
     * Construct CallerBuilder with this caller as first dependency
     *
     * @return
     */
    public CallerBuilder<T> toCallerBuilderAsDep() {
        return new CallerBuilder<T>(1).with(this);
    }

    /**
     * Construct SharedCallerBuilder with this caller as first dependency
     *
     * @return
     */
    public CallerBuilder<T> toSharedCallerBuilderAsDep() {
        return new SharedCallerBuilder<T>(1).with(this);
    }

    /**
     * Resolve value without limits
     *
     * @return
     */
    public T resolve() {
        return Caller.resolve(this);
    }

    /**
     * Resolve value without limits with {@link DEFAULT_FORK_COUNT} forks using
     * ForkJoinPool.commonPool as executor
     *
     * @return
     */
    public T resolveThreaded() {
        return Caller.resolveThreaded(this, DISABLED_STACK_LIMIT, DISABLED_CALL_LIMIT, DEFAULT_FORK_COUNT, ForkJoinPool.commonPool());
    }

    /**
     * Resolve given caller without limits
     *
     * @param <T>
     * @param caller root call point
     * @return
     */
    public static <T> T resolve(Caller<T> caller) {
        return resolve(caller, DISABLED_STACK_LIMIT, DISABLED_CALL_LIMIT);
    }

    /**
     * Resolve function call chain with optional limits
     *
     * @param <T>
     * @param caller root call point
     * @param stackLimit limit of a stack size (each nested dependency expands
     * stack by 1). Use non-positive to disable limit.
     * @param callLimit limit of how many calls can be made (useful for endless
     * recursion detection). Use non-positive to disable limit.
     * @return
     */
    public static <T> T resolve(Caller<T> caller, int stackLimit, long callLimit) {
        return CallerImpl.resolveThreaded(caller, stackLimit, callLimit, -1, Runnable::run); // should never throw exceptions related to threading

    }

    /**
     * Resolve function call chain with optional limits
     *
     * @param <T>
     * @param caller root call point
     * @param stackLimit limit of a stack size (each nested dependency expands
     * stack by 1). Use Optional.empty to disable limit.
     * @param callLimit limit of how many calls can be made (useful for endless
     * recursion detection). Use non-positive to disable limit.
     * @param branch how many branch levels to allow (uses recursion) amount of
     * forks is determined by {@code Caller} dependencies
     * @param exe executor
     * @return
     */
    public static <T> T resolveThreaded(Caller<T> caller, int stackLimit, long callLimit, int branch, Executor exe) {
        return CallerImpl.resolveThreaded(caller, stackLimit, callLimit, branch, exe);
    }

}
