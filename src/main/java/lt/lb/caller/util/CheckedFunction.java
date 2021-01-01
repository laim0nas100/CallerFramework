package lt.lb.caller.util;

import java.util.function.Function;

/**
 * Function with exception masking as a CheckedException
 *
 * @author laim0nas100
 */
@FunctionalInterface
public interface CheckedFunction<P, R> extends Function<P, R> {

    /**
     * Applies the function to the given argument. Masks exceptions.
     *
     * @param t the function argument
     * @return the function result
     * @throws Exception
     */
    public R applyUnsafe(P t) throws Exception;

    @Override
    public default R apply(P t) throws CheckedException {
        try {
            return applyUnsafe(t);
        } catch (Exception e) {
            throw new CheckedException(e);
        }
    }
}
