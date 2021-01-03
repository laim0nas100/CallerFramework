package lt.lb.caller;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;

/**
 * Immutable {@link Caller} argument builder.
 *
 * Immutable.
 *
 * @author laim0nas100
 */
public class CallerResolve {

    public static class WithCaller<T> extends CallerResolve {

        public final Caller<T> caller;

        public WithCaller(Caller<T> caller) {
            this.caller = caller;
        }

        public WithCaller(Caller<T> caller, CallerResolve other) {
            super(other);
            this.caller = caller;
        }

        public WithCaller(Caller<T> caller, Executor executor, boolean interruptible, int stackLimit, long callLimit, int forkCount) {
            super(executor, interruptible, stackLimit, callLimit, forkCount);
            this.caller = caller;
        }

        @Override
        public WithCaller<T> setInterruptible(boolean interruptible) {
            return new WithCaller<>(caller, executor, interruptible, stackLimit, callLimit, forkCount);
        }

        @Override
        public WithCaller setStackLimit(int stackLimit) {
            return new WithCaller<>(caller, executor, interruptible, stackLimit, callLimit, forkCount);
        }

        /**
         * @param callLimit limit of how many calls can be made (useful for
         * endless recursion detection). Use non-positive to disable limit.
         * @return
         */
        @Override
        public WithCaller setCallLimit(long callLimit) {
            return new WithCaller<>(caller, executor, interruptible, stackLimit, callLimit, forkCount);
        }

        /**
         * @param forkCount how many branch levels to allow (uses recursion)
         * amount of forks is determined by {@code Caller} dependencies
         * @return
         */
        @Override
        public WithCaller<T> setForkCount(int forkCount) {
            return new WithCaller<>(caller, executor, interruptible, stackLimit, callLimit, forkCount);
        }

        /**
         * Set executor to be used in resolving the value of this
         * {@link Caller}.
         *
         * @param executor
         * @return
         */
        @Override
        public WithCaller<T> setExecutor(Executor executor) {
            return new WithCaller<>(caller, executor, interruptible, stackLimit, callLimit, forkCount);
        }

        /**
         * Set executor to {@link ForkJoinPool#commonPool()
         * } which will be used in resolving the value of this {@link Caller}.
         *
         * @return
         */
        @Override
        public WithCaller<T> setExecutorCommonForkPool() {
            return new WithCaller<>(caller, ForkJoinPool.commonPool(), interruptible, stackLimit, callLimit, forkCount);
        }

        /**
         * Apply arguments and resolve included {@link Caller}.
         *
         * @return
         */
        public T resolveValue() {
            return CallerImpl.resolveThreaded(caller, interruptible, stackLimit, callLimit, forkCount, executor);
        }

        /**
         * Apply arguments and resolve included {@link Caller} as a
         * {@link FutureTask}.
         *
         * @return
         */
        public FutureTask<T> resolveFuture() {
            return CallerImpl.resolveFuture(caller, interruptible, stackLimit, callLimit, forkCount, executor);
        }

        /**
         * Apply arguments and resolve included {@link Caller} as a
         * {@link FutureTask} which is also submitted in provided executor.
         *
         * @return
         */
        public FutureTask<T> resolveFutureAndRun() {
            FutureTask<T> future = resolveFuture(caller);
            executor.execute(future);
            return future;
        }

    }

    public final boolean interruptible;
    public final int stackLimit;
    public final long callLimit;
    public final int forkCount;

    public final Executor executor;

    public CallerResolve() {
        this(Runnable::run, false, Caller.DISABLED_STACK_LIMIT, Caller.DISABLED_CALL_LIMIT, Caller.DEFAULT_FORK_COUNT);
    }

    public CallerResolve(CallerResolve other) {
        this.executor = other.executor;
        this.interruptible = other.interruptible;
        this.stackLimit = other.stackLimit;
        this.callLimit = other.callLimit;
        this.forkCount = other.forkCount;
    }

    public CallerResolve(Executor executor, boolean interruptible, int stackLimit, long callLimit, int forkCount) {
        this.executor = Objects.requireNonNull(executor);
        this.interruptible = interruptible;
        this.stackLimit = stackLimit;
        this.callLimit = callLimit;
        this.forkCount = forkCount;
    }

    /**
     * @param interruptible check if interrupted before each call
     * @return
     */
    public CallerResolve setInterruptible(boolean interruptible) {
        return new CallerResolve(executor, interruptible, stackLimit, callLimit, forkCount);
    }

    /**
     * @param stackLimit limit of a stack size (each nested dependency expands
     * stack by 1). Use non-positive to disable limit.
     * @return
     */
    public CallerResolve setStackLimit(int stackLimit) {
        return new CallerResolve(executor, interruptible, stackLimit, callLimit, forkCount);
    }

    /**
     * @param callLimit limit of how many calls can be made (useful for endless
     * recursion detection). Use non-positive to disable limit.
     * @return
     */
    public CallerResolve setCallLimit(long callLimit) {
        return new CallerResolve(executor, interruptible, stackLimit, callLimit, forkCount);
    }

    /**
     * @param forkCount how many branch levels to allow (uses recursion) amount
     * of forks is determined by {@code Caller} dependencies
     * @return
     */
    public CallerResolve setForkCount(int forkCount) {
        return new CallerResolve(executor, interruptible, stackLimit, callLimit, forkCount);
    }

    /**
     * Set executor to be used in resolving the value of this {@link Caller}.
     *
     * @param executor
     * @return
     */
    public CallerResolve setExecutor(Executor executor) {
        return new CallerResolve(executor, interruptible, stackLimit, callLimit, forkCount);
    }

    /**
     * Set executor to {@link ForkJoinPool#commonPool()
     * } which will be used in resolving the value of this {@link Caller}.
     *
     * @return
     */
    public CallerResolve setExecutorCommonForkPool() {
        return new CallerResolve(ForkJoinPool.commonPool(), interruptible, stackLimit, callLimit, forkCount);
    }

    /**
     * Create a {@link WithCaller} with configurable arguments and given caller.
     *
     * @param <T>
     * @param caller
     * @return
     */
    public <T> WithCaller<T> withCaller(Caller<T> caller) {
        return new WithCaller<>(caller, this);
    }

    /**
     * Apply arguments and resolve provided {@link Caller}.
     *
     * @param <T>
     * @param caller
     * @return
     */
    public <T> T resolveValue(Caller<T> caller) {
        return CallerImpl.resolveThreaded(caller, interruptible, stackLimit, callLimit, forkCount, executor);
    }

    /**
     * Apply arguments and resolve provided {@link Caller} as a
     * {@link FutureTask}.
     *
     * @param <T>
     * @param caller
     * @return
     */
    public <T> FutureTask<T> resolveFuture(Caller<T> caller) {
        return CallerImpl.resolveFuture(caller, interruptible, stackLimit, callLimit, forkCount, executor);
    }

    /**
     * Apply arguments and resolve provided {@link Caller} as a
     * {@link FutureTask} which is also submitted in provided executor.
     *
     * @param <T>
     * @param caller
     * @return
     */
    public <T> FutureTask<T> resolveFutureAndRun(Caller<T> caller) {
        FutureTask<T> future = resolveFuture(caller);
        executor.execute(future);
        return future;
    }

}
