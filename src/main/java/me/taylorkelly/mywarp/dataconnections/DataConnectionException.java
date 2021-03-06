package me.taylorkelly.mywarp.dataconnections;

public class DataConnectionException extends Exception {

    private static final long serialVersionUID = -2033822282111044971L;

    public DataConnectionException() {
        super();
    }

    public DataConnectionException(String message) {
        super(message);
    }

    public DataConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataConnectionException(Throwable cause) {
        super(cause);
    }
}
