import java.lang.Exception;

public class TypeCheckingException extends Exception { 
    private static final long serialVersionUID = 1L;

    public TypeCheckingException(String errorMessage) {
        super(errorMessage);
    }

    public TypeCheckingException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
