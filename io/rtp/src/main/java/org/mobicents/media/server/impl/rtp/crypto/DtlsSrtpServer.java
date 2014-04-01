package org.mobicents.media.server.impl.rtp.crypto;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.crypto.tls.AlertLevel;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.ClientCertificateType;
import org.bouncycastle.crypto.tls.DefaultTlsServer;
import org.bouncycastle.crypto.tls.ExporterLabel;
import org.bouncycastle.crypto.tls.HashAlgorithm;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.SRTPProtectionProfile;
import org.bouncycastle.crypto.tls.SignatureAlgorithm;
import org.bouncycastle.crypto.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.crypto.tls.TlsCredentials;
import org.bouncycastle.crypto.tls.TlsEncryptionCredentials;
import org.bouncycastle.crypto.tls.TlsSRTPUtils;
import org.bouncycastle.crypto.tls.TlsSignerCredentials;
import org.bouncycastle.crypto.tls.UseSRTPData;

/**
 * 
 * This class represents the DTLS SRTP server connection handler.
 * 
 * The implementation follows the advise from Pierrick Grasland and Tim Panton on this forum thread:
 * http://bouncy-castle.1462172.n4.nabble.com/DTLS-SRTP-with-bouncycastle-1-49-td4656286.html
 * 
 * 
 * @author ivelin.ivanov@telestax.com
 *
 */
public class DtlsSrtpServer extends DefaultTlsServer {
	
    private Logger logger = Logger.getLogger(DtlsSrtpServer.class);

    // Certificate resources
	private static final String[] CERT_RESOURCES = new String[] { "x509-server.pem", "x509-ca.pem" };
	private static final String KEY_RESOURCE = "x509-server-key.pem";
    
    // the server response to the client handshake request
    // http://tools.ietf.org/html/rfc5764#section-4.1.1
	private UseSRTPData serverSrtpData;

	// Asymmetric shared keys derived from the DTLS handshake and used for the SRTP encryption/
	private byte[] srtpMasterClientKey;
	private byte[] srtpMasterServerKey;
	private byte[] srtpMasterClientSalt;
	private byte[] srtpMasterServerSalt;

	private SRTPPolicy srtpPolicy;

	private SRTPPolicy srtcpPolicy;
	
	public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Exception cause)
    {
    	Level logLevel = (alertLevel == AlertLevel.fatal) ? Level.ERROR : Level.WARN; 
        logger.log(logLevel, String.format("DTLS server raised alert (AlertLevel.%d, AlertDescription.%d, message='%s')", alertLevel, alertDescription, message), cause);
    }

    public void notifyAlertReceived(short alertLevel, short alertDescription)
    {
    	Level logLevel = (alertLevel == AlertLevel.fatal) ? Level.ERROR : Level.WARN; 
        logger.log(logLevel, String.format("DTLS server received alert (AlertLevel.%d, AlertDescription.%d)", alertLevel, alertDescription));
    }

    public CertificateRequest getCertificateRequest()
    {
		Vector serverSigAlgs = null;

		if (org.bouncycastle.crypto.tls.TlsUtils
				.isSignatureAlgorithmsExtensionAllowed(serverVersion)) {
			short[] hashAlgorithms = new short[] { HashAlgorithm.sha512,
					HashAlgorithm.sha384, HashAlgorithm.sha256,
					HashAlgorithm.sha224, HashAlgorithm.sha1 };
			short[] signatureAlgorithms = new short[] { SignatureAlgorithm.rsa };

			serverSigAlgs = new Vector();
			for (int i = 0; i < hashAlgorithms.length; ++i) {
				for (int j = 0; j < signatureAlgorithms.length; ++j) {
					serverSigAlgs.addElement(new SignatureAndHashAlgorithm(
							hashAlgorithms[i], signatureAlgorithms[j]));
				}
			}
		}

		return new CertificateRequest(
				new short[] { ClientCertificateType.rsa_sign }, serverSigAlgs,
				null);
    }

    public void notifyClientCertificate(org.bouncycastle.crypto.tls.Certificate clientCertificate)
        throws IOException
    {
        Certificate[] chain = clientCertificate.getCertificateList();
        logger.info(String.format("Received client certificate chain of length %d", chain.length));
        for (int i = 0; i != chain.length; i++)
        {
            Certificate entry = chain[i];
            // TODO Create fingerprint based on certificate signature algorithm digest
            logger.info(String.format("WebRTC Client certificate fingerprint:SHA-256 %s (%s)", TlsUtils.fingerprint(entry), entry.getSubject()));
        }
    }

    protected ProtocolVersion getMaximumVersion()
    {
        return ProtocolVersion.DTLSv12;
    }

    protected ProtocolVersion getMinimumVersion()
    {
        return ProtocolVersion.DTLSv10;
    }

    protected TlsEncryptionCredentials getRSAEncryptionCredentials()
        throws IOException
    {
        return TlsUtils.loadEncryptionCredentials(context, CERT_RESOURCES, KEY_RESOURCE);
    }

    protected TlsSignerCredentials getRSASignerCredentials()
        throws IOException
    {
    	
    	/*
         * TODO Note that this code fails to provide default value for the client supported
         * algorithms if it wasn't sent.
         */
        SignatureAndHashAlgorithm signatureAndHashAlgorithm = null;
        Vector sigAlgs = supportedSignatureAlgorithms;
        if (sigAlgs != null)
        {
            for (int i = 0; i < sigAlgs.size(); ++i)
            {
                SignatureAndHashAlgorithm sigAlg = (SignatureAndHashAlgorithm)
                    sigAlgs.elementAt(i);
                if (sigAlg.getSignature() == SignatureAlgorithm.rsa)
                {
                    signatureAndHashAlgorithm = sigAlg;
                    break;
                }
            }

            if (signatureAndHashAlgorithm == null)
            {
                return null;
            }
        }
        return TlsUtils.loadSignerCredentials(context, new String[]{"x509-server.pem", "x509-ca.pem"},
            "x509-server-key.pem", signatureAndHashAlgorithm);
    }
    
    // Hashtable is (Integer -> byte[])
    @SuppressWarnings("unchecked")
	@Override
    public Hashtable<Integer, byte[]> getServerExtensions()
        throws IOException
    {
    	Hashtable<Integer, byte[]> serverExtensions = (Hashtable<Integer, byte[]>)super.getServerExtensions();
        if (TlsSRTPUtils.getUseSRTPExtension(serverExtensions) == null)
        {

            if (serverExtensions == null)
            {
            	serverExtensions = new Hashtable<Integer, byte[]>();
            }

            TlsSRTPUtils.addUseSRTPExtension(serverExtensions, serverSrtpData );
        }
        return serverExtensions;
    }
    
    @SuppressWarnings("rawtypes")
	@Override
    public void processClientExtensions(Hashtable newClientExtensions) throws IOException {
    	super.processClientExtensions(newClientExtensions);
    	int chosenProfile = SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80; // set to some reasonable default value
    	UseSRTPData clientSrtpData = TlsSRTPUtils.getUseSRTPExtension(newClientExtensions);
    	for (int profile : clientSrtpData.getProtectionProfiles()) {
    		switch (profile) {
    			case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32:
    			case SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80:
    			case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32:
    			case SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80:
    				chosenProfile  = profile;
    				break;
    			default:
    		};
    	};
    	// server chooses a mutually supported SRTP protection profile
    	// http://tools.ietf.org/html/draft-ietf-avt-dtls-srtp-07#section-4.1.2
    	int[] protectionProfiles = {chosenProfile};
    	
    	// server agrees to use the MKI offered by the client
    	serverSrtpData = new UseSRTPData(protectionProfiles, clientSrtpData.getMki());
    }
    
    public byte[] getKeyingMaterial(int length) {
        return context.exportKeyingMaterial(ExporterLabel.dtls_srtp, null, length);
    }

    /**
     * 
     * @return the shared secret key that will be used for the SRTP session
     */
    public void prepareSrtpSharedSecret() {
    	byte[] sharedSecret = null;
		// preparing keys for SRTP. Length of keys is in bits, not bytes. So, we must divide by 8.
    	SRTPParameters srtpParams = SRTPParameters.getSrtpParametersForProfile(serverSrtpData.getProtectionProfiles()[0]);
    	final int keyLen = srtpParams.getCipherKeyLength();
    	final int saltLen = srtpParams.getCipherSaltLength();
    	
    	srtpPolicy = srtpParams.getSrtpPolicy();
    	srtcpPolicy = srtpParams.getSrtcpPolicy();
    	
        srtpMasterClientKey = new byte[keyLen/8];
        srtpMasterServerKey = new byte[keyLen/8];
        srtpMasterClientSalt = new byte[saltLen/8];
        srtpMasterServerSalt = new byte[saltLen/8];
        // 2* (key + salt lenght) / 8. From http://tools.ietf.org/html/rfc5764#section-4-2
        sharedSecret = getKeyingMaterial((keyLen + saltLen)/4);
        
        /*
         * 
         * See: http://tools.ietf.org/html/rfc5764#section-4.2
         * 
         * sharedSecret is an equivalent of :
         * 
         * struct {
         *     client_write_SRTP_master_key[SRTPSecurityParams.master_key_len];
         *     server_write_SRTP_master_key[SRTPSecurityParams.master_key_len];
         *     client_write_SRTP_master_salt[SRTPSecurityParams.master_salt_len];
         *     server_write_SRTP_master_salt[SRTPSecurityParams.master_salt_len];
         *  } ;
         *
         * Here, client = local configuration, server = remote.
         * NOTE [ivelin]: 'local' makes sense if this code is used from a DTLS SRTP client. 
         *                Here we run as a server, so 'local' referring to the client is actually confusing. 
         * 
         * l(k) = KEY length
         * s(k) = salt lenght
         * 
         * So we have the following repartition :
         *                           l(k)                                 2*l(k)+s(k)   
         *                                                   2*l(k)                       2*(l(k)+s(k))
         * +------------------------+------------------------+---------------+-------------------+
         * + local key           |    remote key    | local salt   | remote salt   |
         * +------------------------+------------------------+---------------+-------------------+
         */
        System.arraycopy(sharedSecret, 0, srtpMasterClientKey, 0, keyLen/8); 
        System.arraycopy(sharedSecret, keyLen/8, srtpMasterServerKey, 0, keyLen/8);
        System.arraycopy(sharedSecret, 2*keyLen/8, srtpMasterClientSalt, 0, saltLen/8);
        System.arraycopy(sharedSecret, (2*keyLen/8+saltLen/8), srtpMasterServerSalt, 0, saltLen/8);    	
    }
    
    public SRTPPolicy getSrtpPolicy() {
    	return srtpPolicy;
    }
    
    public SRTPPolicy getSrtcpPolicy() {
    	return srtcpPolicy;
    }
    
    public byte[] getSrtpMasterServerKey() {
    	return srtpMasterServerKey;
    }
    
    public byte[] getSrtpMasterServerSalt() {
    	return srtpMasterServerSalt;
    }
    
    public byte[] getSrtpMasterClientKey() {
    	return srtpMasterClientKey;
    }
    
    public byte[] getSrtpMasterClientSalt() {
    	return srtpMasterClientSalt;
    }
    
	/**
	 * Gets the fingerprint of the Certificate associated to the server.
	 * 
	 * @return The fingerprint of the server certificate. Returns an empty
	 *         String if the server does not contain a certificate.
	 */
	public String getFingerprint() {
		try {
			org.bouncycastle.crypto.tls.Certificate chain = TlsUtils.loadCertificateChain(CERT_RESOURCES);
			Certificate certificate = chain.getCertificateAt(0);
			return TlsUtils.fingerprint(certificate);
		} catch (IOException e1) {
			return "";
		}
	}
    
}
