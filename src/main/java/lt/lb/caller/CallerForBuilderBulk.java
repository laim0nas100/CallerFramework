package lt.lb.caller;


/**
 * @{inheritDoc}
 * Also executes element evaluations in bulk.
 * @author laim0nas100
 */
public class CallerForBuilderBulk<R,T> extends CallerForBuilder<R, T> {
    public CallerForBuilderBulk(){
        super();
        bulk = true;
    }
}
