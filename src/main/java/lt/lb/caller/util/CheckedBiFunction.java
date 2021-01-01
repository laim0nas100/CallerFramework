package lt.lb.caller.util;

import java.util.function.BiFunction;

/**
 * BiFunction with exception masking as a CheckedException
 *
 * @author laim0nas100
 */
@FunctionalInterface
public interface CheckedBiFunction<O, P, R> extends BiFunction<O, P, R> {

    /**
     * Applies this function to the given arguments. Masks exceptions.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     */
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
