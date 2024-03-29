package lt.lb.caller.util;

import java.util.function.Function;

/**
 * Function with exception masking as a {@link CheckedException}
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
    public R applyUncheked(P t) throws Throwable;

    @Override
    public default R apply(P t) throws CheckedException {
        try {
            return applyUncheked(t);
        } catch (Throwable e) {
            throw new CheckedException(e);
        }
    }
}
