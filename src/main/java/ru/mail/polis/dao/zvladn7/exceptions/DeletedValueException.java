package ru.mail.polis.dao.zvladn7.exceptions;
/**
 * Thrown by value accessor methods to indicate that the element being deleted.
 * That means that value contains only timestamp which represented the moment
 * when this value was removed.
 */
public class DeletedValueException extends RuntimeException {

    /**
     * Constructs a {@code DeletedValueException} with {@code null}
     * as its error message string.
     */
    public DeletedValueException() {
        super();
    }

    /**
     * Constructs a {@code DeletedValueException}, saving a reference
     * to the error message string {@code msg} for later retrieval by the
     * {@code getMessage} method.
     *
     * @param msg - the detail message.
     */
    public DeletedValueException(final String msg) {
        super(msg);
    }
}
