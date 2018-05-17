package gigaherz.guidebook.guidebook;

public class BookParsingException extends RuntimeException
{
    public BookParsingException(String message)
    {
        super(message);
    }

    public BookParsingException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
