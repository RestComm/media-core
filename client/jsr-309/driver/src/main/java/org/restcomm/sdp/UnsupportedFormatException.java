package org.restcomm.sdp;

import javax.media.mscontrol.MediaException;



/**
 * Standard JMF class -- see <a
 * href="http://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/format/UnsupportedFormatException.html"
 * target="_blank">this class in the JMF Javadoc</a>. Complete.
 * 
 * @author Ken Larson
 * 
 */
public class UnsupportedFormatException extends MediaException {
	private final Format unsupportedFormat;

	public UnsupportedFormatException(Format unsupportedFormat) {
		super("Unsupported Format "+unsupportedFormat);
		this.unsupportedFormat = unsupportedFormat;
	}

	public UnsupportedFormatException(String message, Format unsupportedFormat) {
		super(message);
		this.unsupportedFormat = unsupportedFormat;

	}

	public Format getFailedFormat() {
		return unsupportedFormat;
	}
}
