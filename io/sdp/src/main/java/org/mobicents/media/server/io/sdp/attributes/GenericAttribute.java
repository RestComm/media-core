package org.mobicents.media.server.io.sdp.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * Generic {@link AttributeField} that freely allows overriding of its key and
 * value fields.<br>
 * Should be used when parsing unknown types of attributes.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class GenericAttribute extends AttributeField {

	public GenericAttribute(String key) {
		super();
		if(key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key cannot be empty");
		}
		super.key = key;
	}
	
	public GenericAttribute(String key, String value) {
		super();
		if(key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key cannot be empty");
		}
		super.key = key;
		super.value = value;
	}
	
	public void setKey(String key) {
		if(key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key cannot be empty");
		}
		super.key = key;
	}

	public void setValue(String value) {
		super.value = value;
	}

}
