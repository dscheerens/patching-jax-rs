package net.novazero.example.jaxrspatch.patchsupport;

public class ObjectPatchException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ObjectPatchException() {
	}

	public ObjectPatchException(String message) {
		super(message);
	}

	public ObjectPatchException(Throwable cause) {
		super(cause);
	}

	public ObjectPatchException(String message, Throwable cause) {
		super(message, cause);
	}

	protected ObjectPatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
