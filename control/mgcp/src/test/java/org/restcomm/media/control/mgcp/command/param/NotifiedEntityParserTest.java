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

package org.restcomm.media.control.mgcp.command.param;

import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.NotifiedEntityParser;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotifiedEntityParserTest {

    @Test
    public void testParseDNS() throws ParseException {
        // given
        final String notifiedEntity = "restcomm@cloud.restcomm.com";

        // when
        NotifiedEntity obj = NotifiedEntityParser.parse(notifiedEntity);

        // then
        Assert.assertEquals("restcomm", obj.getName());
        Assert.assertEquals("cloud.restcomm.com", obj.getDomain());
        Assert.assertEquals(0, obj.getPort());
        Assert.assertEquals(notifiedEntity, obj.toString());
    }

    @Test
    public void testParseIpAddress() throws ParseException {
        // given
        final String notifiedEntity = "restcomm@127.0.0.1:2727";

        // when
        NotifiedEntity obj = NotifiedEntityParser.parse(notifiedEntity);

        // then
        Assert.assertEquals("restcomm", obj.getName());
        Assert.assertEquals("127.0.0.1", obj.getDomain());
        Assert.assertEquals(2727, obj.getPort());
        Assert.assertEquals(notifiedEntity, obj.toString());
    }

    @Test(expected = ParseException.class)
    public void testMissingLocalName() throws ParseException {
        // given
        final String notifiedEntity = "@127.0.0.1:2727";

        // when
        NotifiedEntityParser.parse(notifiedEntity);
    }

    @Test(expected = ParseException.class)
    public void testMissingDomainName() throws ParseException {
        // given
        final String notifiedEntity = "restcomm@";

        // when
        NotifiedEntityParser.parse(notifiedEntity);
    }

    @Test(expected = ParseException.class)
    public void testMissingPort() throws ParseException {
        // given
        final String notifiedEntity = "@127.0.0.1:";

        // when
        NotifiedEntityParser.parse(notifiedEntity);
    }

}
