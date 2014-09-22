package org.mobicents.media.server.impl.srtp;

/**
 * Listens to DTLS-related events.
 * 
 * @author Henrique Rosa
 * 
 */
public interface DtlsListener {

	void onDtlsHandshakeComplete();

	void onDtlsHandshakeFailed(Throwable e);

}
