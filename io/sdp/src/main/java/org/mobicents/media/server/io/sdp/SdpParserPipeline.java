package org.mobicents.media.server.io.sdp;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.media.server.io.sdp.fields.AttributeField;
import org.mobicents.media.server.io.sdp.fields.parser.MediaDescriptionFieldParser;
import org.mobicents.media.server.io.sdp.fields.parser.OriginFieldParser;
import org.mobicents.media.server.io.sdp.fields.parser.SessionNameFieldParser;
import org.mobicents.media.server.io.sdp.fields.parser.TimingFieldParser;
import org.mobicents.media.server.io.sdp.fields.parser.VersionFieldParser;

/**
 * Creates a pipeline composed of {@link SdpParser} objects that can be used to
 * parse SDP dynamically.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SdpParserPipeline {

	private static final int DEFAULT_PARSERS_SIZE = 10;

	private final Map<FieldType, SdpParser<? extends SdpField>> fieldParsers;
	private final Map<String, SdpParser<? extends AttributeField>> attributeParsers;

	public SdpParserPipeline() {
		this.fieldParsers = new HashMap<FieldType, SdpParser<?>>(DEFAULT_PARSERS_SIZE);
		this.fieldParsers.put(FieldType.VERSION, new VersionFieldParser());
		this.fieldParsers.put(FieldType.ORIGIN, new OriginFieldParser());
		this.fieldParsers.put(FieldType.SESSION_NAME, new SessionNameFieldParser());
		this.fieldParsers.put(FieldType.TIMING, new TimingFieldParser());
		this.fieldParsers.put(FieldType.MEDIA, new MediaDescriptionFieldParser());
		
		this.attributeParsers = new HashMap<String, SdpParser<? extends AttributeField>>(DEFAULT_PARSERS_SIZE);
	}

	/**
	 * Adds a parser to the pipeline.
	 * 
	 * @param parser
	 *            The parser to be registered
	 */
	public void addFieldParser(FieldType type, SdpParser<?> parser) {
		synchronized (this.fieldParsers) {
			this.fieldParsers.put(type, parser);
		}
	}

	/**
	 * Removes an existing parser from the pipeline.
	 * 
	 * @param parser
	 *            The parser to be removed from the pipeline
	 */
	public void removeFieldParser(FieldType type) {
		synchronized (this.fieldParsers) {
			this.fieldParsers.remove(type);
		}
	}

	/**
	 * Removes all registered parsers from the pipeline.
	 */
	public void removeAllFieldParsers() {
		synchronized (fieldParsers) {
			this.fieldParsers.clear();
		}
	}

	public SdpParser<? extends SdpField> getFieldParser(FieldType type) {
		return this.fieldParsers.get(type);
	}

	public SdpParser<? extends SdpField> getParser(char type) {
		FieldType fieldType = FieldType.fromType(type);
		return fieldType == null ? null : getFieldParser(fieldType);
	}

}
