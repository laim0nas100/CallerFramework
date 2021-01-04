package lt.lb.caller;

/**
 *
 * @author laim0nas100
 */
public class CallerFlowControl<T> {
    
    public static final CallerFlowControl FLOW_BREAK = new CallerFlowControl(null, CallerForType.BREAK);
    public static final CallerFlowControl FLOW_CONTINUE = new CallerFlowControl(null, CallerForType.CONTINUE);
    public static final CallerFlowControl FLOW_RETURN_NULL = new CallerFlowControl(Caller.ofNull(), CallerForType.RETURN);

    public static enum CallerForType {
        RETURN, BREAK, CONTINUE
    }

    public final Caller<T> caller;
    public final CallerForType flowControl;

    public CallerFlowControl(Caller<T> caller, CallerForType endCycle) {
        if (endCycle == CallerForType.RETURN && caller == null) {
            throw new IllegalArgumentException("Caller must not be null when Control type is " + CallerForType.RETURN);
        }
        this.caller = caller;
        this.flowControl = endCycle;

    }
}
