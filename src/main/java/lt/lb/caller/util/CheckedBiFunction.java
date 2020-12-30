package lt.lb.caller.util;

import java.util.function.BiFunction;

/**
 *
 * @author laim0nas100
 */
@FunctionalInterface
public interface CheckedBiFunction<O, P, R> extends BiFunction<O, P, R> {

    @Override
    public default R apply(O t, P u) throws CheckedException {
        try {
            return applyUnsafe(t, u);
        } catch (Exception e) {
            throw new CheckedException(e);
        }
    }

    public R applyUnsafe(O t, P u) throws Exception;

}
