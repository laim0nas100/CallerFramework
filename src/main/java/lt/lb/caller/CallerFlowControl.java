package lt.lb.caller;

/**
 *
 * @author laim0nas100
 */
public class CallerFlowControl<T> {

    static enum CallerForType {
        RETURN, BREAK, CONTINUE
    }

    final Caller<T> caller;
    final CallerForType flowControl;

    CallerFlowControl(Caller<T> caller, CallerForType endCycle) {
        if (endCycle == CallerForType.RETURN && caller == null) {
            throw new IllegalArgumentException("Caller must not be null when Control type is " + CallerForType.RETURN);
        }
        this.caller = caller;
        this.flowControl = endCycle;

    }
}
