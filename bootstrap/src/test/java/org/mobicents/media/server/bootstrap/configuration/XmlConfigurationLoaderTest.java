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

package org.mobicents.media.server.bootstrap.configuration;

import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.core.configuration.MediaConfiguration;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.core.configuration.MgcpControllerConfiguration;
import org.mobicents.media.core.configuration.MgcpEndpointConfiguration;
import org.mobicents.media.core.configuration.NetworkConfiguration;
import org.mobicents.media.core.configuration.ResourcesConfiguration;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class XmlConfigurationLoaderTest {

    @Test
    public void testLoadConfiguration() {
        // given
        String filepath = "mediaserver.xml";
        XmlConfigurationLoader loader = new XmlConfigurationLoader();

        // when
        MediaServerConfiguration config = loader.load(filepath);

        // then
        NetworkConfiguration network = config.getNetworkConfiguration();
        Assert.assertEquals("192.168.1.175", network.getBindAddress());
        Assert.assertEquals("50.54.74.123", network.getExternalAddress());
        Assert.assertEquals("192.168.1.0", network.getNetwork());
        Assert.assertEquals("255.255.255.255", network.getSubnet());
        Assert.assertTrue(network.isSbc());

        MgcpControllerConfiguration controller = config.getControllerConfiguration();
        Assert.assertEquals("198.162.1.175", controller.getAddress());
        Assert.assertEquals(3437, controller.getPort());
        MgcpEndpointConfiguration bridgeEndpoint = controller.getEndpoint("mobicents/bridge");
        Assert.assertNotNull(bridgeEndpoint);
        Assert.assertEquals(51, bridgeEndpoint.getPoolSize());
        MgcpEndpointConfiguration ivrEndpoint = controller.getEndpoint("mobicents/ivr");
        Assert.assertNotNull(ivrEndpoint);
        Assert.assertEquals(52, ivrEndpoint.getPoolSize());
        MgcpEndpointConfiguration cnfEndpoint = controller.getEndpoint("mobicents/cnf");
        Assert.assertNotNull(cnfEndpoint);
        Assert.assertEquals(53, cnfEndpoint.getPoolSize());

        MediaConfiguration media = config.getMediaConfiguration();
        Assert.assertEquals(5, media.getTimeout());
        Assert.assertEquals(54534, media.getLowPort());
        Assert.assertEquals(64534, media.getHighPort());
        Assert.assertEquals(60, media.getJitterBufferSize());
        Assert.assertTrue(media.hasCodec("l16"));
        Assert.assertTrue(media.hasCodec("PCMU"));
        Assert.assertTrue(media.hasCodec("pcma"));
        Assert.assertTrue(media.hasCodec("gSm"));
        Assert.assertTrue(media.hasCodec("g729"));

        ResourcesConfiguration resources = config.getResourcesConfiguration();
        Assert.assertEquals(200, resources.getLocalConnectionCount());
        Assert.assertEquals(100, resources.getRemoteConnectionCount());
        Assert.assertEquals(100, resources.getPlayerCount());
        Assert.assertEquals(100, resources.getRecorderCount());
        Assert.assertEquals(100, resources.getDtmfDetectorCount());
        Assert.assertEquals(-25, resources.getDtmfDetectorDbi());
        Assert.assertEquals(100, resources.getDtmfGeneratorCount());
        Assert.assertEquals(100, resources.getDtmfGeneratorToneDuration());
        Assert.assertEquals(-25, resources.getDtmfGeneratorToneVolume());
        Assert.assertEquals(10, resources.getSignalDetectorCount());
        Assert.assertEquals(10, resources.getSignalGeneratorCount());
    }

}
