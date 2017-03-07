package org.restcomm.media.spi.format;

/**
 * Implementation of the application format
 * 
 * @author Henrique Rosa
 * 
 */
public class ApplicationFormat extends Format implements Cloneable {

	protected ApplicationFormat(EncodingName name) {
		super(name);
	}

	protected ApplicationFormat(String name) {
		super(new EncodingName(name));
	}

	@Override
	public ApplicationFormat clone() {
		return new ApplicationFormat(getName().clone());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApplicationFormat[").append(getName().toString()).append("]");
		return builder.toString();
	}
}
