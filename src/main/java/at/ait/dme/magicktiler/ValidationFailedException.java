package at.ait.dme.magicktiler;

/**
 * Exception for use with validator implementations: Each validator
 * shall throw this exception when validation fails, and provide an
 * end-user-compatible message explaining the reason.
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
 */
public class ValidationFailedException extends Exception {

  private static final long serialVersionUID = 2419151493105137582L;

  public ValidationFailedException(String message) {
    super(message);
  }

}
