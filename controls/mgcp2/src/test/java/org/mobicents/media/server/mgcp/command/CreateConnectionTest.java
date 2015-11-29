/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
        
package org.mobicents.media.server.mgcp.command;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.core.call.Call;
import org.mobicents.media.core.call.CallManager;
import org.mobicents.media.server.mgcp.MgcpActionType;
import org.mobicents.media.server.mgcp.MgcpParameterType;
import org.mobicents.media.server.mgcp.MgcpRequest;
import org.mobicents.media.server.mgcp.exception.MgcpCommandException;
import org.mobicents.media.server.spi.Endpoint;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionTest {
    
    @After
    public void after() {
        CallManager.getInstance().deleteCalls();
    }
    
    @Test
    public void createTwoLocalConnectionsTest() {
        // Given
        MgcpRequest crcxRequest = new MgcpRequest();
        crcxRequest.setActionType(MgcpActionType.CRCX);
        crcxRequest.setEndpointId("mobicents/bridge/$");
        crcxRequest.setTransactionId(147483677);
        crcxRequest.setParameter(MgcpParameterType.CALL_ID, "4");
        crcxRequest.setParameter(MgcpParameterType.MODE, "sendrecv");
        crcxRequest.setParameter(MgcpParameterType.NOTIFIED_ENTITY, "restcomm@127.0.0.1:2727");
        crcxRequest.setParameter(MgcpParameterType.SECOND_ENDPOINT_ID, "mobicents/ivr/$");

        CreateConnection crcx = new CreateConnection(crcxRequest);
        
        // When
        try {
            crcx.execute();
        } catch (MgcpCommandException e) {
            Assert.fail(e.getMessage());
        }
        
        // Then
        Call call = CallManager.getInstance().getCall(Integer.parseInt("4", 16));
        Endpoint primaryEndpoint = call.getEndpoint(crcx.getPrimaryEndpointId());
        Endpoint secondaryEndpoint = call.getEndpoint(crcx.getSecondaryEndpointId());
        
        Assert.assertNotNull(call);
        Assert.assertEquals(2, call.countEndpoints());
        Assert.assertNotNull(primaryEndpoint);
        Assert.assertNotNull(secondaryEndpoint);
    }

}
