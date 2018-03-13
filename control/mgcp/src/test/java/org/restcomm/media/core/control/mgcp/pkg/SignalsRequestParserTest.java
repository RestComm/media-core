/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.core.control.mgcp.pkg;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.core.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.core.control.mgcp.pkg.SignalRequest;
import org.restcomm.media.core.control.mgcp.pkg.SignalsRequestParser;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SignalsRequestParserTest {

    @Test
    public void testParseSingleRequest() throws MgcpParseException {
        // given
        final String request = "AU/pa(an=https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26d12996.wav it=1)";

        // when
        SignalRequest[] requests = SignalsRequestParser.parse(request);

        // then
        assertEquals(1, requests.length);
        assertEquals("AU", requests[0].getPackageName());
        assertEquals("pa", requests[0].getSignalType());
        assertEquals("https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26d12996.wav", requests[0].getParameter("an"));
        assertEquals("1", requests[0].getParameter("it"));
    }

    @Test
    public void testParseMultipleRequests() throws MgcpParseException {
        // given
        final String request = "AU/pa(an=https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26d12996.wav it=1), AU/pr(an=https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26xxx111.wav)";
        
        // when
        SignalRequest[] requests = SignalsRequestParser.parse(request);
        
        // then
        assertEquals(2, requests.length);
        assertEquals("AU", requests[0].getPackageName());
        assertEquals("pa", requests[0].getSignalType());
        assertEquals("https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26d12996.wav", requests[0].getParameter("an"));
        assertEquals("1", requests[0].getParameter("it"));
        assertEquals("AU", requests[1].getPackageName());
        assertEquals("pr", requests[1].getSignalType());
        assertEquals("https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26xxx111.wav", requests[1].getParameter("an"));
        assertNull(requests[1].getParameter("it"));
    }

    @Test(expected = MgcpParseException.class)
    public void testParseWhenMissingPackageNameSeparator() throws MgcpParseException {
        // given
        final String request = "AUpa(an=https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26d12996.wav it=1)";

        // when
        SignalsRequestParser.parse(request);
    }

    @Test(expected = MgcpParseException.class)
    public void testParseWhenMissingParameterStartSeparator() throws MgcpParseException {
        // given
        final String request = "AU/pa an=https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26d12996.wav it=1)";

        // when
        SignalsRequestParser.parse(request);
    }

    @Test(expected = MgcpParseException.class)
    public void testParseWhenMissingParameterValueSeparator() throws MgcpParseException {
        // given
        final String request = "AU/pa(an=https://127.0.0.1:8080/restcomm/cache/ACae6e420f4/5a26d12996.wav it 1)";

        // when
        SignalsRequestParser.parse(request);
    }

    @Test(expected = MgcpParseException.class)
    public void testParseWhenMissingParameters() throws MgcpParseException {
        // given
        final String request = "AU/pa()";

        // when
        SignalsRequestParser.parse(request);
    }

}
