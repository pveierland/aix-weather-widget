package net.veierland.aix.data;


@SuppressWarnings("serial")
public class AixDataUpdateException extends Exception {

	public enum Reason {
		UNSPECIFIED,
		UNKNOWN,
		PARSE_ERROR,
		RATE_LIMITED,
	}

	public Reason reason;
	
	public AixDataUpdateException()
	{
		super();
		this.reason = Reason.UNSPECIFIED;
	}
	
	public AixDataUpdateException(String message)
	{
		super(message);
		this.reason = Reason.UNSPECIFIED;
	}

	public AixDataUpdateException(String message, Reason reason)
	{
		super(message);
		this.reason = reason;
	}
}
