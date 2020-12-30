package lt.lb.caller;

/**
 *
 * @author laim0nas100
 */
public class CallerException extends IllegalStateException {

    public CallerException() {
    }

    public CallerException(String s) {
        super(s);
    }

    public CallerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CallerException(Throwable cause) {
        super(cause);
    }

}
