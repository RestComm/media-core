package org.mobicents.sdp;



/**
 * Standard JMF class -- see <a
 * href="http://java.sun.com/products/java-media/jmf/2.1.1/apidocs/javax/media/format/VideoFormat.html"
 * target="_blank">this class in the JMF Javadoc</a>. Coding complete. Frame
 * rate is in frames per second.
 * 
 * @author Ken Larson
 * 
 */
public class VideoFormat extends Format {

	protected int maxDataLength = NOT_SPECIFIED;
	protected float frameRate = NOT_SPECIFIED;

	// encodings:
	public static final String CINEPAK = "cvid";
	public static final String JPEG = "jpeg";
	public static final String JPEG_RTP = "jpeg/rtp";
	public static final String MPEG = "mpeg";
	public static final String MPEG_RTP = "mpeg/rtp";
	public static final String H261 = "h261";
	public static final String H261_RTP = "h261/rtp";
	public static final String H263 = "h263";
	public static final String H263_RTP = "h263/rtp";
	public static final String H263_1998_RTP = "h263-1998/rtp";
	public static final String RGB = "rgb";
	public static final String YUV = "yuv";
	public static final String IRGB = "irgb";
	public static final String SMC = "smc";
	public static final String RLE = "rle";
	public static final String RPZA = "rpza";
	public static final String MJPG = "mjpg";
	public static final String MJPEGA = "mjpa";
	public static final String MJPEGB = "mjpb";
	public static final String INDEO32 = "iv32";
	public static final String INDEO41 = "iv41";
	public static final String INDEO50 = "iv50";

	public VideoFormat(String encoding) {
		super(encoding);
	}
	public VideoFormat(String encoding, float frameRate) {
		super(encoding);
                this.frameRate = frameRate;
	}

	public VideoFormat(String encoding, int maxDataLength, 
			float frameRate) {
		super(encoding);
		this.maxDataLength = maxDataLength;
		this.frameRate = frameRate;
	}

	public int getMaxDataLength() {
		return maxDataLength;
	}



	public float getFrameRate() {
		return frameRate;
	}



	public String toString() {
		final StringBuffer b = new StringBuffer();

		if (encoding == null)
			b.append("N/A");
		else
			b.append(encoding.toUpperCase());

		if (FormatUtils.specified(frameRate)) {
			b.append(", ");
			b.append("FrameRate=");
			b.append(FormatUtils.frameRateToString(frameRate));
		}

		if (FormatUtils.specified(maxDataLength)) {
			b.append(", ");
			b.append("Length=");
			b.append(maxDataLength);
		}

		return b.toString();
	}

	public boolean equals(Object format) {
		if (!super.equals(format))
			return false;

		if (!(format instanceof VideoFormat)) {
			return false;
		}

		final VideoFormat oCast = (VideoFormat) format;
		return oCast.maxDataLength == this.maxDataLength
				&& oCast.frameRate == this.frameRate;

	}

	public boolean matches(Format format) {
		if (!super.matches(format)) {
			// if (getClass() == FormatUtils.videoFormatClass){
			// FormatTraceUtils.traceMatches(this, format, false); // otherwise
			// let subclass trace
			// }
			return false;
		}
		if (!(format instanceof VideoFormat)) {
			final boolean result = true;
			// if (getClass() == FormatUtils.videoFormatClass){
			// FormatTraceUtils.traceMatches(this, format, result);
			// }
			return result;
		}

		final VideoFormat oCast = (VideoFormat) format;

		// do not match against max data length.
		final boolean result = FormatUtils.matches(oCast.getFrameRate(), this
				.getFrameRate());

		// if (getClass() == FormatUtils.videoFormatClass){
		// FormatTraceUtils.traceMatches(this, format, result); // otherwise let
		// subclass trace
		// }

		return result;

	}


}
