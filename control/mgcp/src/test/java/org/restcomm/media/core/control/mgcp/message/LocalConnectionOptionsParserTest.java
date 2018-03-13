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

package org.restcomm.media.core.control.mgcp.message;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.media.core.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.core.control.mgcp.message.LocalConnectionOptionType;
import org.restcomm.media.core.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.core.control.mgcp.message.LocalConnectionOptionsParser;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class LocalConnectionOptionsParserTest {

    @Test
    public void testParsingSuccessful() throws MgcpParseException {
        // given
        String text = "p:20, a:PCMU;PCMA;G726-32,  e:on,  s:off, t:00, webrtc:true";
        LocalConnectionOptionsParser parser = new LocalConnectionOptionsParser();

        // when
        LocalConnectionOptions options = parser.parse(text);

        // then
        Assert.assertEquals("20", options.get(LocalConnectionOptionType.PACKETIZATION_PERIOD));
        Assert.assertEquals("PCMU;PCMA;G726-32", options.get(LocalConnectionOptionType.CODECS));
        Assert.assertEquals("on", options.get(LocalConnectionOptionType.ECHO_CANCELATION));
        Assert.assertEquals("off", options.get(LocalConnectionOptionType.SILENCE_SUPPRESSION));
        Assert.assertEquals("00", options.get(LocalConnectionOptionType.TYPE_OF_SERVICE));
        Assert.assertEquals("true", options.get(LocalConnectionOptionType.WEBRTC));
        Assert.assertNull(options.get(LocalConnectionOptionType.BANDWIDTH));
        Assert.assertNull(options.get(LocalConnectionOptionType.TYPE_OF_NETWORK));
        Assert.assertNull(options.get(LocalConnectionOptionType.GAIN_CONTROL));
        Assert.assertNull(options.get(LocalConnectionOptionType.RESOURCE_RESERVATION));
        Assert.assertNull(options.get(LocalConnectionOptionType.ENCRYPTION_KEY));
        Assert.assertNull(options.get(LocalConnectionOptionType.DTMF_CLAMP));
        Assert.assertNull(options.get(LocalConnectionOptionType.BANDWIDTH));
        Assert.assertNull(options.get(LocalConnectionOptionType.BANDWIDTH));
    }

    @Test
    public void testParsingSkipUnknownOption() throws MgcpParseException {
        // given
        String text = "p:20, a:PCMU;PCMA;G726-32,  e:on,  s:off, t:00, webrtc:true, XYZ:???";
        LocalConnectionOptionsParser parser = new LocalConnectionOptionsParser();

        // when
        LocalConnectionOptions options = parser.parse(text);

        // then
        Assert.assertEquals("20", options.get(LocalConnectionOptionType.PACKETIZATION_PERIOD));
        Assert.assertEquals("PCMU;PCMA;G726-32", options.get(LocalConnectionOptionType.CODECS));
        Assert.assertEquals("on", options.get(LocalConnectionOptionType.ECHO_CANCELATION));
        Assert.assertEquals("off", options.get(LocalConnectionOptionType.SILENCE_SUPPRESSION));
        Assert.assertEquals("00", options.get(LocalConnectionOptionType.TYPE_OF_SERVICE));
        Assert.assertEquals("true", options.get(LocalConnectionOptionType.WEBRTC));
        Assert.assertNull(options.get(LocalConnectionOptionType.BANDWIDTH));
        Assert.assertNull(options.get(LocalConnectionOptionType.TYPE_OF_NETWORK));
        Assert.assertNull(options.get(LocalConnectionOptionType.GAIN_CONTROL));
        Assert.assertNull(options.get(LocalConnectionOptionType.RESOURCE_RESERVATION));
        Assert.assertNull(options.get(LocalConnectionOptionType.ENCRYPTION_KEY));
        Assert.assertNull(options.get(LocalConnectionOptionType.DTMF_CLAMP));
        Assert.assertNull(options.get(LocalConnectionOptionType.BANDWIDTH));
        Assert.assertNull(options.get(LocalConnectionOptionType.BANDWIDTH));
    }

}
