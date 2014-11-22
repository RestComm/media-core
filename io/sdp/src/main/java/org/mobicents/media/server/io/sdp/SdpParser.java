package org.mobicents.media.server.io.sdp;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @param <T>
 *            The type of SDP {@link Field} the parser is related to.
 */
public interface SdpParser<T extends Field> {

	String PARSE_ERROR = "Could not parse SDP: ";

	/**
	 * Checks whether the parse is capable of parsing a specific SDP line.
	 * 
	 * @param sdp
	 *            The SDP line to be parsed
	 * @return Returns <code>true</code> if capable of parsing. Returns
	 *         <code>false</code> otherwise.
	 */
	boolean canParse(String sdp);

	/**
	 * Parses the SDP originating a new {@link Field} object.
	 * <p>
	 * <b>Careful!</b> The parser will blindly attempt to parse the SDP text.<br>
	 * Users should invoke the {@link #canParse(String)} method beforehand to
	 * make sure the parsers is able to parse the SDP text.
	 * </p>
	 * 
	 * @param sdp
	 *            The SDP to be parsed
	 * @return An {@link Field} object based on the SDP line
	 * @throws SdpException
	 *             In case the parser cannot parse the SDP line.
	 */
	T parse(String sdp) throws SdpException;

	/**
	 * Parses the SDP to override the values of an existing {@link Field}
	 * object.
	 * <p>
	 * <b>Careful!</b> The parser will blindly attempt to parse the SDP text.<br>
	 * Users should invoke the {@link #canParse(String)} method beforehand to
	 * make sure the parsers is able to parse the SDP text.
	 * </p>
	 * 
	 * @param field
	 *            The {@link Field} to be overwritten
	 * @param sdp
	 *            The SDP to be parsed
	 * @throws SdpException
	 *             In case the parser cannot parse the SDP line.
	 */
	void parse(T field, String sdp) throws SdpException;

}
