package org.mobicents.media;

import java.io.Serializable;

/**
 * Standard JMF class -- see <a href="http://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/Format.html" target="_blank">this class in the JMF Javadoc</a>.
 * Coding complete.
 * @author Ken Larson
 *
 */
public class Format implements Cloneable, Serializable {

    public static final int NOT_SPECIFIED = -1;
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    
    protected String encoding;	// allowed to be null
    private long encodingCode;	// This is set during equals/matches comparisons via isSameEncoding.  Allows for fast string comparisons.
    private int hash;
    public final static Format ANY = new Format("ANY");
    public final static Format RAW_RTP = new Format("RAW_RTP");
    
    protected int frameRate = NOT_SPECIFIED;

    public static final int FORMAT_HASHMAP_DEFAULT_INITIAL_CAPACITY = 8;
    public static final float FORMAT_HASHMAP_DEFAULT_LOAD_FACTOR = 1f;

    
    public Format(String encoding) {
        this.encoding = encoding;
        hash = encoding.hashCode();
    }
    
    public String getEncoding() {
        return encoding;
    }

    public int getFrameRate() {
        return frameRate;
    }
    
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }
    
    @Override
    public boolean equals(Object format) {
        return format.hashCode() == hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public boolean matches(Format format) {
        if (format == null) {
            return false;
        }

        if (this == ANY) {
            return true;
        }

        if (format == ANY) {
            return true;
        }

//        if (!FormatUtils.isOneAssignableFromTheOther(getClass(), format.getClass())) {
//            return false;
//        }
        return (this.encoding.equals(format.encoding) || this.encoding == null || format.encoding == null || isSameEncoding(format));
    }

    public boolean isSameEncoding(Format other) {
        if (other == null) {
            return false;
        }
        if (other.encoding == null) {
            return false;
        }
        if (this.encoding == null) {
            return false;
        }
        if (other.encoding.equalsIgnoreCase(this.encoding)) {
            return true;
        }
        if (this.encodingCode == 0) {
            this.encodingCode = getEncodingCode(this.encoding);
        }
        if (other.encodingCode == 0) {
            other.encodingCode = getEncodingCode(other.encoding);
        }
        return encodingCode == other.encodingCode;
    }

    public boolean isSameEncoding(String encoding) {
        if (encoding == null) {
            return false;
        }
        if (this.encoding == null) {
            return false;
        }
        
        if (encoding.equals(this.encoding)) {
            return true;
        }
        
        if (this.encodingCode == 0) {
            this.encodingCode = getEncodingCode(this.encoding);
        }
        return this.encodingCode == getEncodingCode(encoding);
    }

    private long getEncodingCode(String enc) {
        if (enc == null) {
            return 0;
        }
        return FormatUtils.stringEncodingCodeVal(enc);
    }

    public Format relax() {
        return (Format) clone();
    }

    @Override
    public Object clone() {
        return new Format(encoding);
    }

    @Override
    public String toString() {
        return getEncoding();
    }
}
