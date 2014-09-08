/**
 * 
 * Code derived and adapted from the Jitsi client side SRTP framework.
 * 
 * Distributed under LGPL license.
 * See terms of license at gnu.org.
 */
package org.mobicents.media.server.impl.rtp.crypto;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.mobicents.media.server.impl.rtp.RtpPacket;

/**
 * SRTCPTransformer implements PacketTransformer.
 * It encapsulate the encryption / decryption logic for SRTCP packets
 * 
 * @author Bing SU (nova.su@gmail.com)
 * @author Werner Dittmann &lt;Werner.Dittmann@t-online.de>
 */
public class SRTCPTransformer implements PacketTransformer {
	
	private final RtcpPacket packet;
	
    private SRTPTransformEngine forwardEngine;
    private SRTPTransformEngine reverseEngine;

    /** All the known SSRC's corresponding SRTCPCryptoContexts */
    private Hashtable<Long,SRTCPCryptoContext> contexts;

    /**
     * Constructs a SRTCPTransformer object.
     * 
     * @param engine The associated SRTPTransformEngine object for both
     *            transform directions.
     */
    public SRTCPTransformer(SRTPTransformEngine engine)
    {
        this(engine, engine);
    }

    /**
     * Constructs a SRTCPTransformer object.
     * 
     * @param forwardEngine The associated SRTPTransformEngine object for
     *            forward transformations.
     * @param reverseEngine The associated SRTPTransformEngine object for
     *            reverse transformations.
     */
    public SRTCPTransformer(SRTPTransformEngine forwardEngine, SRTPTransformEngine reverseEngine) {
    	this.packet = new RtcpPacket();
        this.forwardEngine = forwardEngine;
        this.reverseEngine = reverseEngine;
        this.contexts = new Hashtable<Long,SRTCPCryptoContext>();
    }

    /**
     * Encrypts a SRTCP packet
     * 
     * @param pkt plain SRTCP packet to be encrypted
     * @return encrypted SRTCP packet
     */
    public boolean transform(byte[] pkt) {
    	// Decode the packet
    	this.packet.decode(pkt, 0);
    	
    	// Associate the packet with its encryption context
        long ssrc = this.packet.getReport().getSsrc();
        SRTCPCryptoContext context = contexts.get(ssrc);

        if (context == null) {
            context = forwardEngine.getDefaultContextControl().deriveContext(ssrc);
            context.deriveSrtcpKeys();
            contexts.put(ssrc, context);
        }
        context.transformPacket(packet);
        return true;
    }

    /**
     * Decrypts a SRTCP packet
     * 
     * @param pkt encrypted SRTCP packet to be decrypted
     * @return decrypted SRTCP packet
     */
    public boolean reverseTransform(RtpPacket pkt)
    {
        long ssrc = pkt.GetRTCPSyncSource();
        SRTCPCryptoContext context = this.contexts.get(ssrc);

        if (context == null)
        {
            context =
                reverseEngine.getDefaultContextControl().deriveContext(ssrc);
            context.deriveSrtcpKeys();
            contexts.put(new Long(ssrc), context);
        }
        return context.reverseTransformPacket(pkt);
    }

    /**
     * Close the transformer and underlying transform engine.
     * 
     * The close functions closes all stored crypto contexts. This deletes key data 
     * and forces a cleanup of the crypto contexts.
     */
    public void close() 
    {
        forwardEngine.close();
        if (forwardEngine != reverseEngine)
            reverseEngine.close();

        Iterator<Map.Entry<Long, SRTCPCryptoContext>> iter
            = contexts.entrySet().iterator();

        while (iter.hasNext()) 
        {
            Map.Entry<Long, SRTCPCryptoContext> entry = iter.next();
            SRTCPCryptoContext context = entry.getValue();

            iter.remove();
            if (context != null)
                context.close();
        }
    }
}
