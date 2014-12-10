package org.mobicents.media.server.io.sdp;

import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;

/**
 * Exposes access to session-level fields and attributes necessary for a Media
 * Description
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface SessionLevelAccessor {

	/*
	 *  Common
	 */
	ConnectionField getConnection();
	
	ConnectionModeAttribute getConnectionMode();

	/*
	 *  DTLS
	 */
	FingerprintAttribute getFingerprint();
	
	SetupAttribute getSetup();

	/*
	 *  ICE
	 */
	IceUfragAttribute getIceUfrag();

	IcePwdAttribute getIcePwd();

}
