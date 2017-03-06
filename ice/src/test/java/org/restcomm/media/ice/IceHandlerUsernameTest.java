package org.restcomm.media.ice;

import junit.framework.Assert;
import org.junit.Test;
import org.mobicents.media.io.stun.StunException;
import org.mobicents.media.io.stun.messages.StunMessage;
import org.mobicents.media.io.stun.messages.StunRequest;
import org.mobicents.media.io.stun.messages.StunResponse;
import org.mobicents.media.io.stun.messages.attributes.general.ErrorCodeAttribute;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.ice.IceHandler;
import org.restcomm.media.ice.events.IceEventListener;

import java.net.InetSocketAddress;

import static org.mobicents.media.io.stun.messages.StunMessage.BINDING_REQUEST;

/**
 * Created by farwaakhtar on 2/17/17.
 */
public class IceHandlerUsernameTest {

    @Test
    public void testNullUsername() throws StunException, PacketHandlerException {
        //given
        byte transID[] = new byte[]{(byte) 0x80, 0x08, 0x6a, 0x6c, (byte) 0xc1, (byte) 0xab, 0x74, (byte) 0x8d,
                                    (byte) 0xb2, (byte) 0xe2, (byte) 0x83, 0x69};

        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);

        //when
        StunRequest stunRequest = new StunRequest();
        stunRequest.setMessageType(BINDING_REQUEST);
        stunRequest.setTransactionID(transID);
        byte[] sampleRequest = stunRequest.encode();
        IceEventListener iceEventListener = null;
        IceHandler iceHandler = new IceHandler(IceComponent.RTP_ID, iceEventListener);

        byte[] response = iceHandler.handle(sampleRequest, sampleRequest.length, 0, address, address);
        StunResponse errorResponse = (StunResponse) StunResponse.decode(response, (char)0, (char) response.length);
        ErrorCodeAttribute errorCodeAttribute= (ErrorCodeAttribute) errorResponse.getAttribute(ErrorCodeAttribute.ERROR_CODE);

        //then
        Assert.assertEquals(StunMessage.BINDING_ERROR_RESPONSE, errorResponse.getMessageType());
        Assert.assertEquals(ErrorCodeAttribute.BAD_REQUEST, errorCodeAttribute.getErrorCode());


    }
}