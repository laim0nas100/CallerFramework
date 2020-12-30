package lt.lb.caller.util;

import java.util.function.Function;

/**
 *
 * @author laim0nas100
 */
@FunctionalInterface
public interface CheckedFunction<P, R> extends Function<P, R> {

    public R applyUnsafe(P t) throws Exception;

    @Override
    public default R apply(P t) throws CheckedException{
        try {
            return applyUnsafe(t);
        } catch (Exception e) {
            throw new CheckedException(e);
        }
    }
}
