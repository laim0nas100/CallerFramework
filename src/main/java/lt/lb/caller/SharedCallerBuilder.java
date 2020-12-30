package lt.lb.caller;

/**
 * @param <T> the main type of Caller product
 * @author laim0nas100
 */
public class SharedCallerBuilder<T> extends CallerBuilder<T> {

    public SharedCallerBuilder(int size) {
        super(size);
        this.sharedMutable = true;
    }

    public SharedCallerBuilder() {
        this.sharedMutable = true;
    }

}
