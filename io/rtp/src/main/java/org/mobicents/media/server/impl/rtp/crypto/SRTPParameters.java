package org.mobicents.media.server.impl.rtp.crypto;

import org.bouncycastle.crypto.tls.SRTPProtectionProfile;

public enum SRTPParameters {

	// DTLS derived key and salt lengths for SRTP 
	// http://tools.ietf.org/html/rfc5764#section-4.1.2
	
	SRTP_AES128_CM_HMAC_SHA1_80 (SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80, 128, 112),
	SRTP_AES128_CM_HMAC_SHA1_32 (SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32, 128, 112),
	SRTP_NULL_HMAC_SHA1_80 (SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80, 0, 0),
	SRTP_NULL_HMAC_SHA1_32 (SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32, 0, 0);
	
	private int profile;
	private int cipherKeyLength;
	private int cipherSaltLength;
	
	private SRTPParameters(int newProfile, int newCipherKeyLength, int newCipherSaltLength) {
		this.profile = newProfile;
		this.cipherKeyLength = newCipherKeyLength;
		this.cipherSaltLength = newCipherSaltLength;
	}

	public int getProfile() {
		return profile;
	}
	
	public int getCipherKeyLength() {
		return cipherKeyLength;
	}
	
	public int getCipherSaltLength() {
		return cipherSaltLength;
	}
	
	public static SRTPParameters getSrtpParametersForProfile(int profileValue) {
		switch (profileValue) {
			case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80:
				return SRTP_AES128_CM_HMAC_SHA1_80;
			case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32:
				return SRTP_AES128_CM_HMAC_SHA1_32;
			case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80:
				return SRTP_NULL_HMAC_SHA1_80;
			case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32:
				return SRTP_NULL_HMAC_SHA1_32;
			default:
				throw new IllegalArgumentException("SRTP Protection Profile value %d is not allowed for DTLS SRTP. See http://tools.ietf.org/html/rfc5764#section-4.1.2 for valid values.");
		}
	}
	
	short SRTP_AES128_CM_HMAC_SHA1_80_CIPHER_KEY_LENGTH = 128;
	short SRTP_AES128_CM_HMAC_SHA1_80_SALT_KEY_LENGTH =  112;
	short SRTP_AES128_CM_HMAC_SHA1_32_CIPHER_KEY_LENGTH = 128; 
	short SRTP_AES128_CM_HMAC_SHA1_32_SALT_KEY_LENGTH =  112;
	short SRTP_NULL_HMAC_SHA1_80_CIPHER_KEY_LENGTH = 0; 
	short SRTP_NULL_HMAC_SHA1_80_SALT_KEY_LENGTH =  0;
	short SRTP_NULL_HMAC_SHA1_32_CIPHER_KEY_LENGTH = 0; 
	short SRTP_NULL_HMAC_SHA1_32_SALT_KEY_LENGTH =  0;
	
	
}
