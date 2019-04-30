import java.lang.Exception;
import java.lang.RuntimeException;

public class TypeCheckingException extends RuntimeException { 
    private static final long serialVersionUID = 1L;

    public TypeCheckingException(String errorMessage) {
        super(errorMessage);
    }

    public TypeCheckingException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
