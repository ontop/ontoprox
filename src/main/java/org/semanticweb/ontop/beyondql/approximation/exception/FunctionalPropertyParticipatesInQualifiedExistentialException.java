package org.semanticweb.ontop.beyondql.approximation.exception;
/**
 * When an object property is functional, its functionality can be added in
 * the Dl Lite ontology when:
 * <ul>
 * <li>It is not an specialized object property (i.e. if it does not appear
 * positively in the right-hand side of an inclusion assertion), and
 * <li>It does not appear in an expression of the form ∃Q.C in T. 
 * <ul>
 * In this exception we cober the second case. 
 * @author Alejandra Lorenzo
 *
 */
public class FunctionalPropertyParticipatesInQualifiedExistentialException
		extends Exception {

	public FunctionalPropertyParticipatesInQualifiedExistentialException() {
		// TODO Auto-generated constructor stub
	}

	public FunctionalPropertyParticipatesInQualifiedExistentialException(
			String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public FunctionalPropertyParticipatesInQualifiedExistentialException(
			Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public FunctionalPropertyParticipatesInQualifiedExistentialException(
			String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

}
