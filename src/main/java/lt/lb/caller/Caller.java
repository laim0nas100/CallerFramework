package lt.lb.caller;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lt.lb.caller.util.CastList;
import lt.lb.caller.util.CheckedFunction;
import lt.lb.caller.util.CheckedRunnable;
import lt.lb.fastid.FastID;
import lt.lb.fastid.FastIDGen;

/**
 * Recursion avoiding function modeling. Main purpose: write a recursive
 * function then, use this framework to replace every recursive call or flow
 * directing keyword with {@link Caller} equivalent, without the need for an
 * iterative solution.
 *
 * Supports function interruption (even in single-threaded mode), function
 * termination when call limit is reacher, same with stack limit. Furthermore,
 * offers built-in threading, when a {@link Caller} has more than 1 dependencies
 * to be resolved before final function call. Has built-in
 * {@code for},{@code while} {@code do,while} loop support.
 *
 * Very small footprint (memory and performance) when using only tail-call type
 * of functions.
 *
 * Otherwise, when simulating a call-stack, the performance and memory penalties
 * are self-evident. Is not likely to be faster than well-made iterative
 * solution.
 *
 * @author laim0nas100
 * @param <T> Most general type of return result (and arguments) that this
 * caller is used to model.
 */
public class Caller<T> {

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
    private static final Caller<?> emptyResultCaller = new Caller<>(CallerType.RESULT, null, null, null);

    public static enum CallerType {

        RESULT, FUNCTION, MEMOIZING
    }

    public final CallerType type;
    public final FastID id;
    protected final T value;
    protected final Function<CastList<T>, Caller<T>> call;
    protected final List<Caller<T>> dependencies;

    /**
     * Memoized things. This actually stores a value and a caller object, but
     * the value is stored as a result of a future.
     */
    protected final CompletableFuture<T> compl;
    protected final AtomicBoolean started;

    public boolean isMemoizedDone() {
        return type == CallerType.MEMOIZING && compl.isDone();
    }

    public boolean isMemoizedNotDone() {
        return type == CallerType.MEMOIZING && !compl.isDone();
    }

    public static <T> CallerBuilder<T> builder() {
        return new CallerBuilder<>();
    }

    public static <T> CallerBuilder<T> builder(int deps) {
        return new CallerBuilder<>(deps);
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
        if (next == emptyResultCaller) {
            return CallerFlowControl.FLOW_RETURN_NULL;
        }
        return new CallerFlowControl<>(next, CallerFlowControl.CallerForType.RETURN);
    }

    /**
     * Signify {@code for} loop end inside {@code Caller for} loop.Equivalent of
     * using {@code return} with value.
     *
     * @param <T>
     * @param result
     * @return
     */
    public static <T> CallerFlowControl<T> flowReturn(T result) {
        if (result == null) {
            return CallerFlowControl.FLOW_RETURN_NULL;
        }
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
        return CallerFlowControl.FLOW_BREAK;
    }

    /**
     * Signify {@code for} loop continue inside {@code Caller for} loop.
     *
     * @param <T>
     * @return
     */
    public static <T> CallerFlowControl<T> flowContinue() {
        return CallerFlowControl.FLOW_CONTINUE;
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
        return new Caller<>(CallerType.RESULT, result, null, null);
    }

    /**
     * @param <T>
     * @return Caller, that has a result null
     */
    public static <T> Caller<T> ofNull() {
        return (Caller<T>) emptyResultCaller;
    }

    /**
     * Caller modeling a recursive call
     *
     * @param <T>
     * @param call
     * @return Caller, with recursive call
     */
    public static <T> Caller<T> ofFunction(CheckedFunction<CastList<T>, Caller<T>> call) {
        Objects.requireNonNull(call);
        return new Caller<>(CallerType.FUNCTION, null, call, null);
    }

    /**
     * Caller modeling a recursive call.
     *
     * Memoized (runs only once and stores result).
     *
     * @param <T>
     * @param call
     * @return Caller, with recursive call
     */
    public static <T> Caller<T> ofFunctionMemo(CheckedFunction<CastList<T>, Caller<T>> call) {
        Objects.requireNonNull(call);
        return new Caller<>(CallerType.MEMOIZING, null, call, null);
    }

    /**
     * Caller modeling a recursive call with no result or arguments.
     *
     * @param <T>
     * @param run
     * @return Caller, with recursive call
     */
    public static <T> Caller<T> ofRunnable(CheckedRunnable run) {
        Objects.requireNonNull(run);
        return ofFunction(args -> {
            run.run();
            return Caller.ofNull();
        });
    }

    /**
     * Caller modeling a recursive call with no result or arguments. Returns
     * {@code null} as a result. Memoized (runs only once and stores result).
     *
     * @param <T>
     * @param run
     * @return Caller, with recursive call
     */
    public static <T> Caller<T> ofRunnableMemo(CheckedRunnable run) {
        Objects.requireNonNull(run);
        return ofFunctionMemo(args -> {
            run.run();
            return Caller.ofNull();
        });
    }

    /**
     * Caller modeling a recursive call wrapping in supplier
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
     * Caller modeling a recursive call wrapping in callable with result
     * computed. Memoized (runs only once and stores result).
     *
     * @param <T>
     * @param call
     * @return Caller, with recursive tail call
     */
    public static <T> Caller<T> ofCallableMemo(Callable<Caller<T>> call) {
        Objects.requireNonNull(call);
        return ofFunctionMemo(args -> call.call());
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
     * Caller that has a result (terminating) wrapping in {@link Callable}.
     * Memoized (runs only once and stores result).
     *
     * @param <T>
     * @param call
     * @return Caller that ends up as a result
     */
    public static <T> Caller<T> ofCallableResultMemo(Callable<T> call) {
        Objects.requireNonNull(call);
        return ofFunctionMemo(args -> ofResult(call.call()));
    }

    /**
     * Caller that has a result (terminating) after calling a function.
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
     * Caller that has a result (terminating) after calling a function. Memoized
     * (runs only once and stores result).
     *
     * @param <T>
     * @param call
     * @return Caller that ends up as a result
     */
    public static <T> Caller<T> ofResultCallMemo(CheckedFunction<CastList<T>, T> call) {
        Objects.requireNonNull(call);
        return ofFunctionMemo(args -> ofResult(call.apply(args)));
    }

    /**
     * Main constructor
     *
     * @param type
     * @param result
     * @param nextCall
     * @param dependencies
     */
    protected Caller(CallerType type, T result, CheckedFunction<CastList<T>, Caller<T>> nextCall, List<Caller<T>> dependencies) {
        this.type = type;
        this.value = result;
        this.call = nextCall;
        this.dependencies = dependencies;
        if (type == CallerType.MEMOIZING) {
            this.compl = new CompletableFuture<>();
            this.started = new AtomicBoolean(false);
        } else {
            this.compl = null;
            this.started = null;
        }
        this.id = idgen.getAndIncrement();
    }

    /**
     * Construct {@link Caller} loop end with {@code return} from this caller.
     *
     * @return
     */
    public CallerFlowControl<T> toFlowReturn() {
        return Caller.flowReturn(this);
    }

    /**
     * Construct {@link CallerBuilder} with this caller as first dependency
     *
     * @return
     */
    public CallerBuilder<T> toCallerBuilderAsDep() {
        return new CallerBuilder<T>(1).with(this);
    }

    public static final CallerResolve singleThreadDefaultResolve = new CallerResolve().setInterruptible(false).setForkCount(0);
    public static final CallerResolve threadedDefaultResolve = new CallerResolve().setInterruptible(true).setExecutorCommonForkPool();

    /**
     * Resolve value without limits.
     *
     * Check {@link CallerResolve} for better customization of arguments.
     *
     * @return
     */
    public T resolve() {
        return resolveUsing(singleThreadDefaultResolve);
    }

    /**
     * Resolve value without limits with {@link DEFAULT_FORK_COUNT} forks using
     * ForkJoinPool.commonPool as executor.
     *
     * Check {@link CallerResolve} for better customization of arguments.
     *
     * @return
     */
    public T resolveThreaded() {
        return resolveUsing(threadedDefaultResolve);
    }

    /**
     * Resolve using given argument provider.
     *
     * @param arguments
     * @return
     */
    public T resolveUsing(CallerResolve arguments) {
        return arguments.resolveValue(this);
    }

    /**
     * Get resolvable and customizable object with default base arguments for
     * single-threaded resolve.
     *
     * @return
     */
    public CallerResolve.WithCaller<T> withArguments() {
        return new CallerResolve.WithCaller<>(this, singleThreadDefaultResolve);
    }

    /**
     * Get resolvable and customizable object with default base arguments
     *
     * @return
     */
    public CallerResolve.WithCaller<T> withThreadedArguments() {
        return new CallerResolve.WithCaller<>(this, threadedDefaultResolve);
    }

    /**
     * Get resolvable and customizable object with default base arguments as
     * from provided arguments
     *
     * @param args provided arguments
     * @return
     */
    public CallerResolve.WithCaller<T> withArguments(CallerResolve args) {
        return new CallerResolve.WithCaller<>(this, args);
    }

}
