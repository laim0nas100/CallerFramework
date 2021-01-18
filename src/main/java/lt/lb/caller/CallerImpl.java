package lt.lb.caller;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import lt.lb.caller.Caller.CallerType;
import lt.lb.caller.CallerFlowControl.CallerForType;
import lt.lb.caller.util.CastList;
import lt.lb.caller.util.CheckedBiFunction;
import lt.lb.caller.util.CheckedException;
import lt.lb.caller.util.CheckedFunction;
import lt.lb.caller.util.IndexedIterator;
import lt.lb.caller.util.IndexedIterator.IndexedValue;
import lt.lb.caller.util.sync.CompletablePromise;
import lt.lb.caller.util.sync.Promise;
import lt.lb.caller.util.sync.ValuePromise;

/**
 *
 * @author laim0nas100
 */
public class CallerImpl {

    static class ThreadStack {

        ThreadStack parent;
        Thread thread;
        AtomicBoolean interrupted = new AtomicBoolean(false);

        public ThreadStack() {
            thread = Thread.currentThread();
        }

        public ThreadStack(ThreadStack parent, Thread thread) {
            this.parent = parent;
            this.thread = thread;
        }

        public static ThreadStack createOrReuse(ThreadStack parent) {
            if (parent == null) { // disabled
                return null;
            }
            if (parent.thread.equals(Thread.currentThread())) {
                return parent;
            } else {
                //minimize depth to 2
                if (parent.parent == null) {
                    return new ThreadStack(parent);
                } else {
                    return new ThreadStack(parent.parent);
                }

            }
        }

        public ThreadStack(ThreadStack parent) {
            this.parent = parent;
            this.thread = Thread.currentThread();
        }

        protected void interruptedAndProliferateUp() {
            if (interrupted.compareAndSet(false, true)) {
                if (parent != null && parent.interrupted.compareAndSet(false, true)) {
                    parent.thread.interrupt();
                }
            }

        }

        public boolean wasInterrupted() {
            if (interrupted.get()) {
                return true;
            }
            if (this.thread.isInterrupted()) {
                interruptedAndProliferateUp();
                return true;
            } else { // not interrupted, but maybe parent was?
                if (parent != null && parent.interrupted.get()) {//found interrupted parent
                    interruptedAndProliferateUp();
                    return true;
                }
            }

            return interrupted.get();
        }
    }

    static class StackFrame<T> implements Serializable {

        Caller<T> call;
        ArrayList<T> args;
        int index;
        Collection<Caller<T>> memoizingStack;

        public StackFrame(Caller<T> call) {
            continueWith(call);
        }

        public final void continueWith(Caller<T> call) {
            this.args = call.dependencies == null ? null : new ArrayList<>(call.dependencies.size());
            this.call = call;
            this.index = 0;
            if (call.type == CallerType.MEMOIZING) {
                if (memoizingStack == null) {
                    memoizingStack = new ArrayDeque<>();
                }
                memoizingStack.add(call);
            }
        }

        public boolean readyArgs(Caller<T> c) {
            return args == null || c.dependencies == null || this.args.size() == c.dependencies.size();
        }

        @Override
        public String toString() {
            return "StackFrame{" + "call=" + call + ", args=" + args + ", index=" + index + ", memoizingStack=" + memoizingStack + '}';
        }

    }

    private static void assertCallLimit(long callLimit, AtomicLong current) {
        if (callLimit > 0) {
            long lim = current.getAndIncrement();
            if (lim >= callLimit) {
                throw new CallerException("Call limit reached " + lim);
            }
        }
    }

    /**
     * Resolve Caller with optional limits
     *
     * @param <T>
     * @param caller
     * @param interruptible check if interrupted before each call
     * @param stackLimit limit of a stack size (each nested dependency expands
     * stack by 1). Use non-positive to disable limit.
     * @param callLimit limit of how many calls can be made (useful for endless
     * recursion detection). Use non-positive to disable limit.
     * @param forkCount how many fork levels to allow (uses recursion) amount of
     * forks is determined by {@code Caller} dependencies
     * @param exe executor
     * @return
     */
    public static <T> T resolveThreaded(Caller<T> caller, boolean interruptible, int stackLimit, long callLimit, int forkCount, Executor exe) throws CheckedException {
        try {
            ThreadStack threadStack = interruptible ? new ThreadStack() : null;
            return resolveThreadedInner(caller, threadStack, stackLimit, callLimit, forkCount, 0, new AtomicLong(0), exe);
        } catch (TimeoutException | InterruptedException | CancellationException | CompletionException | ExecutionException ex) {
            throw new CheckedException(ex);
        }

    }

    /**
     * Resolve Caller with optional limits as a FutureTask
     *
     * @param <T>
     * @param caller
     * @param interruptible check if interrupted before each call
     * @param stackLimit limit of a stack size (each nested dependency expands
     * stack by 1). Use non-positive to disable limit.
     * @param callLimit limit of how many calls can be made (useful for endless
     * recursion detection). Use non-positive to disable limit.
     * @param forkCount how many fork levels to allow (uses recursion) amount of
     * forks is determined by {@code Caller} dependencies
     * @param exe executor
     * @return
     */
    public static <T> FutureTask<T> resolveFuture(Caller<T> caller, boolean interruptible, int stackLimit, long callLimit, int forkCount, Executor exe) {
        return new FutureTask<>(() -> {
            ThreadStack threadStack = interruptible ? new ThreadStack() : null;
            return resolveThreadedInner(caller, threadStack, stackLimit, callLimit, forkCount, 0, new AtomicLong(0), exe);
        });
    }

    /**
     * Retrieves items one by one, each time creating new call. Just constructs
     * appropriate functions for {@link ofWhileLoop#ofWhileLoopSwitch}.
     *
     * Recommended to not use directly for readability. Use
     * {@link CallerForBuilder}.
     *
     * @param <T> the main type of Caller product
     * @param <R> type that iteration happens
     * @param emptyCase Caller when iterator is empty or not terminated anywhere
     * @param iterator ReadOnlyIterator that has items
     * @param func BiFunction that provides Caller that eventually results in T
     * type result. Used to make recursive calls from all items.
     * @param contFunc BiFunction that checks wether to end iteration in the
     * middle of it and how
     * @return
     */
    public static <T, R> Caller<T> ofIteratorLazy(Caller<T> emptyCase, IndexedIterator<R> iterator, CheckedBiFunction<Integer, R, Caller<T>> func, CheckedBiFunction<Integer, T, CallerFlowControl<T>> contFunc) {

        return ofWhileLoop(
                emptyCase,
                iterator::hasNext,
                () -> {
                    IndexedValue<R> nextIndexed = iterator.nextIndexed();
                    return func.apply(nextIndexed.index, nextIndexed.value);
                },
                item -> contFunc.apply(iterator.getCurrentIndex(), item)
        );

    }

    /**
     * Retrieves items all at once and creates dependency calls for each item,
     * which then can be executed in parallel if need be. After all items are
     * retrieved, executed a regular {@code for} loop with those items.
     * Recommended to use only if every item need to evaluated anyway and order
     * of evaluation does not matter.
     *
     * Recommended to not use directly for readability. Use
     * {@link CallerForBuilderBulk}.
     *
     * @param <T> the main type of Caller product
     * @param <R> type that iteration happens
     * @param emptyCase Caller when iterator is empty or not terminated anywhere
     * @param iterator ReadOnlyIterator that has items
     * @param func BiFunction that provides Caller that eventually results in T
     * type result. Used to make recursive calls from all items.
     * @param contFunc BiFunction that checks wether to end iteration in the
     * middle of it and how
     * @return
     */
    public static <T, R> Caller<T> ofIteratorLazyBulk(Caller<T> emptyCase, IndexedIterator<R> iterator, BiFunction<Integer, R, Caller<T>> func, CheckedBiFunction<Integer, T, CallerFlowControl<T>> contFunc) {

        CallerBuilder<T> b = new CallerBuilder<>();

        while (iterator.hasNext()) {
            IndexedValue<R> n = iterator.nextIndexed();
            b.with(args -> func.apply(n.index, n.value));
        }

        return b.toCall(args -> {
            for (int i = 0; i < args.parameterCount; i++) {
                T arg = args.get(i);
                CallerFlowControl<T> apply = contFunc.apply(i, arg);
                if (apply.flowControl == CallerForType.CONTINUE) { // assume this to be the must common response
                    continue;
                }
                if (apply.flowControl == CallerForType.RETURN) {
                    return apply.caller;
                }
                if (apply.flowControl == CallerForType.BREAK) {
                    break;
                }
                throw new IllegalStateException("Unregocnized flow control statement " + apply.flowControl);

            }
            return emptyCase;
        });

    }

    /**
     * Models do while loop.
     *
     * Recommended to not use directly for readability. Use
     * {@link CallerDoWhileBuilder}.
     *
     * @param <T> the main type of Caller product
     * @param emptyCase Caller when iterator is empty of not terminated anywhere
     * @param condition whether to continue the loop
     * @param func main recursive call of the loop
     * @param contFunc how to continue with recursive call result
     * @return
     */
    public static <T> Caller<T> ofDoWhileLoop(Caller<T> emptyCase, Callable<Boolean> condition, Callable<Caller<T>> func, CheckedFunction<T, CallerFlowControl<T>> contFunc) {
        return new CallerBuilder<T>(1)
                .with(func)
                .toCall(args -> flowControlSwitch(contFunc.apply(args._0), emptyCase, condition, func, contFunc));

    }

    private static <T> Caller<T> flowControlSwitch(CallerFlowControl<T> apply, Caller<T> emptyCase, Callable<Boolean> condition, Callable<Caller<T>> func, CheckedFunction<T, CallerFlowControl<T>> contFunc) throws Exception {
        switch (apply.flowControl) {
            case CONTINUE: // this should be the most common
                return ofWhileLoopSwitch(emptyCase, condition, func, contFunc);
            case RETURN:
                return apply.caller;
            case BREAK:
                return emptyCase;
            default:
                throw new IllegalStateException("Unregocnized flow control statement " + apply.flowControl);
        }
    }

    /**
     * Models while loop.
     *
     * @param <T> the main type of Caller product
     * @param emptyCase Caller when iterator is empty of not terminated anywhere
     * @param condition whether to continue the loop
     * @param func main recursive call of the loop
     * @param contFunc how to continue with recursive call result
     * @return
     */
    public static <T> Caller<T> ofWhileLoop(Caller<T> emptyCase, Callable<Boolean> condition, Callable<Caller<T>> func, CheckedFunction<T, CallerFlowControl<T>> contFunc) {
        return Caller.ofCallable(() -> ofWhileLoopSwitch(emptyCase, condition, func, contFunc));

    }

    /**
     * Models while loop iteration.
     *
     * @param <T> the main type of Caller product
     * @param emptyCase Caller when iterator is empty of not terminated anywhere
     * @param condition whether to continue the loop
     * @param func main recursive call of the loop
     * @param contFunc how to continue with recursive call result
     * @return
     */
    private static <T> Caller<T> ofWhileLoopSwitch(Caller<T> emptyCase, Callable<Boolean> condition, Callable<Caller<T>> func, CheckedFunction<T, CallerFlowControl<T>> contFunc) throws Exception {

        if (!condition.call()) {
            return emptyCase;
        }

        return new CallerBuilder<T>(1)
                .with(func)
                .toCall(args -> flowControlSwitch(contFunc.apply(args._0), emptyCase, condition, func, contFunc));

    }

    private static <T> T complete(Collection<Caller<T>> s, T value) {
        if (s == null) {
            return value;
        }
        for (Caller<T> call : s) {
            call.compl.complete(value);
        }
        s.clear();
        return value;
    }

    private static <T> boolean runnerCAS(Caller<T> caller) {
        return caller.isMemoizedNotDone() && caller.started.compareAndSet(false, true);
    }

    private static final CastList emptyArgs = new CastList<>(null);

    private static <T> T resolveThreadedInner(Caller<T> caller, ThreadStack threadStack, final long stackLimit, final long callLimit, final int fork, final int prevStackSize, AtomicLong callNumber, Executor exe) throws InterruptedException, CancellationException, TimeoutException, ExecutionException {

        Deque<StackFrame<T>> stack = new ArrayDeque<>();

        Deque<Caller<T>> firstMemoizedStack = new ArrayDeque<>();

        while (true) {
            if (threadStack != null && threadStack.wasInterrupted()) {
                throw new InterruptedException("Caller has been interrupted");
            }
            if (stack.isEmpty()) {
                switch (caller.type) {
                    case RESULT:
                        return complete(firstMemoizedStack, caller.value);
                    case MEMOIZING:
                        if (runnerCAS(caller)) {
                            if (caller.dependencies == null) {
                                assertCallLimit(callLimit, callNumber);
                                firstMemoizedStack.add(caller);
                                caller = caller.call.apply(emptyArgs);
                            } else {
                                stack.addLast(new StackFrame<>(caller));
                            }
                            break;
                        } else {
                            return complete(firstMemoizedStack, caller.compl.get());
                        }
                    case FUNCTION:
                        if (caller.dependencies == null) {
                            assertCallLimit(callLimit, callNumber);
                            caller = caller.call.apply(emptyArgs);
                        } else {
                            stack.addLast(new StackFrame(caller));
                        }
                        break;

                    default:
                        throw new IllegalStateException("No value or call"); // should never happen
                }
                continue;
            }
            // in stack
            if (stackLimit > 0 && (prevStackSize + stackLimit) <= stack.size()) {
                throw new CallerException("Stack limit overrun " + stack.size() + prevStackSize);
            }
            StackFrame<T> frame = stack.getLast();
            caller = frame.call;
            if (frame.readyArgs(caller)) { //demolish stack, because got all dependecies
                assertCallLimit(callLimit, callNumber);
                caller = caller.call.apply(frame.args == null ? emptyArgs : new CastList(frame.args)); // last call with dependants
                switch (caller.type) {
                    case MEMOIZING:

                        if (runnerCAS(caller)) {
                            stack.getLast().continueWith(caller);
                        } else {// done or executing on other thread
                            T v = caller.compl.get();
                            complete(stack.pollLast().memoizingStack, v);
                            if (stack.isEmpty()) {
                                return complete(firstMemoizedStack, v);
                            } else {
                                stack.getLast().args.add(v);
                            }
                        }
                        break;
                    case FUNCTION:
                        stack.getLast().continueWith(caller);
                        break;

                    case RESULT:
                        complete(stack.pollLast().memoizingStack, caller.value);
                        if (stack.isEmpty()) {
                            return complete(firstMemoizedStack, caller.value);
                        } else {
                            stack.getLast().args.add(caller.value);
                        }
                        break;

                    default:
                        throw new IllegalStateException("No value or call"); // should never happen
                    }
                continue;
            }
            // not demolish stack
            if (caller.type == CallerType.RESULT) {
                frame.args.add(caller.value);
                continue;
            }
            if (caller.isMemoizedDone()) {
                frame.args.add(caller.compl.get());
                continue;
            }
            if (caller.type == CallerType.FUNCTION || caller.isMemoizedNotDone()) {
                if (caller.dependencies == null) { // dependencies empty
                    // just call, assume we have expanded stack before
                    if (caller.type == CallerType.FUNCTION || runnerCAS(caller)) {
                        assertCallLimit(callLimit, callNumber);
                        frame.continueWith(caller.call.apply(emptyArgs)); // replace current frame, because of simple tail recursion
                    } else {//in another thread
                        frame.args.add(caller.compl.get());
                    }
                    continue;
                }
                // dep not empty and no threading
                if (fork <= 0 || caller.dependencies.size() <= 1) {
                    Caller<T> get = caller.dependencies.get(frame.index);
                    frame.index++;
                    switch (get.type) {
                        case RESULT:
                            frame.args.add(get.value);
                            break;
                        case FUNCTION:
                            stack.addLast(new StackFrame<>(get));
                            break;
                        case MEMOIZING:
                            if (runnerCAS(get)) {
                                stack.addLast(new StackFrame<>(get));
                            } else {//in another thread so just wait
                                frame.args.add(get.compl.get());
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unknown caller state" + get);
                    }
                    continue;
                }

                // use threading with dependencies 
                ArrayList<RunnableFuture<T>> array = new ArrayList<>(caller.dependencies.size());
                int stackSize = stack.size() + prevStackSize;
                for (Caller<T> c : caller.dependencies) {
                    switch (c.type) {
                        case RESULT:
                            array.add(new ValuePromise<>(c.value));
                            break;
                        case FUNCTION:
                            new Promise<>(() -> { // actually use recursion, because localizing is hard, and has to be fast, so just limit branching size
                                return resolveThreadedInner(c, ThreadStack.createOrReuse(threadStack), stackLimit, callLimit, fork - 1, stackSize, callNumber, exe);
                            }).execute(exe).collect(array);
                            break;
                        case MEMOIZING:
                            if (c.isMemoizedDone()) {
                                array.add(new CompletablePromise<>(c.compl));
                            } else {
                                new Promise(() -> { // actually use recursion, because localizing is hard, and has to be fast, so just limit branching size
                                    return resolveThreadedInner(c, ThreadStack.createOrReuse(threadStack), stackLimit, callLimit, fork - 1, stackSize, callNumber, exe);
                                }).execute(exe).collect(array);
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unknown caller state" + c);
                    }
                }
                Promise waiterAndRunner = new Promise(array);

                try {
                    waiterAndRunner.run(); // help with progress
                    waiterAndRunner.get(); // wait for execution
                } catch (ExecutionException err) {
                    //execution failed at some point, so just cancel everything
                    for (Future pro : array) {
                        pro.cancel(true);
                    }
                    while (err.getCause() instanceof ExecutionException) {
                        err = (ExecutionException) err.getCause();
                    }
                    throw err;

                }
                for (Future pro : array) {
                    frame.args.add((T) pro.get());
                }
                frame.index += array.size();
                continue;
            }
            throw new IllegalStateException("Reached illegal caller state " + caller + " exiting");

        }
    }
}
