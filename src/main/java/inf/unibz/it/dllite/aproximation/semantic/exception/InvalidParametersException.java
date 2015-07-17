package inf.unibz.it.dllite.aproximation.semantic.exception;

/**
 * To handle invalid arguments in the DLLiteApproximator class. Required:
 * <ul>
 * <li>URI of the OWL ontology
 * <li>URI of the Dl Lite Ontology
 * <li>URI of the working ontology
 * </ul>
 * @author Alejandra Lorenzo
 *
 */
public class InvalidParametersException extends Exception {

	public InvalidParametersException() {
		// TODO Auto-generated constructor stub
	}

	public InvalidParametersException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public InvalidParametersException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public InvalidParametersException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

}
