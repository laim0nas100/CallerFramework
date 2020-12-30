package lt.lb.caller.util.sync;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 *
 * @author laim0nas100
 */
public class Promise<Type> extends FutureTask<Type> {


    public Promise(Callable<Type> clbl) {
        super(clbl);
    }

    public Promise(Collection<RunnableFuture> before) {
        this(() -> {
            for (RunnableFuture p : before) {// look for a thing to run
                p.run();
            }
            for (RunnableFuture p : before) {// await all
                p.get();
            }
            return null;
        });
    }

    public Promise<Type> execute(Executor e) {
        e.execute(this);
        return this;
    }

    public Promise<Type> collect(Collection<RunnableFuture<Type>> collection) {
        collection.add(this);
        return this;
    }

}
