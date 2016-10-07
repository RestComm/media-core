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
import org.mobicents.media.core.configuration.DtlsConfiguration;
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

    /**
     * Test all information from a valid and complete configuration file is loaded properly into memory.
     */
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
        Assert.assertEquals("mgcp-conf-test.xml", controller.getConfiguration());
        Assert.assertEquals(5, controller.getPoolSize());
        MgcpEndpointConfiguration bridgeEndpoint = controller.getEndpoint("mobicents/bridge/");
        Assert.assertNotNull(bridgeEndpoint);
        Assert.assertEquals("org.mobicents.media.server.mgcp.endpoint.BridgeEndpoint", bridgeEndpoint.getClassName());
        Assert.assertEquals(51, bridgeEndpoint.getPoolSize());
        MgcpEndpointConfiguration ivrEndpoint = controller.getEndpoint("mobicents/ivr/");
        Assert.assertNotNull(ivrEndpoint);
        Assert.assertEquals(52, ivrEndpoint.getPoolSize());
        Assert.assertEquals("org.mobicents.media.server.mgcp.endpoint.IvrEndpoint", ivrEndpoint.getClassName());
        MgcpEndpointConfiguration cnfEndpoint = controller.getEndpoint("mobicents/cnf/");
        Assert.assertNotNull(cnfEndpoint);
        Assert.assertEquals("org.mobicents.media.server.mgcp.endpoint.ConferenceEndpoint", cnfEndpoint.getClassName());
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

        DtlsConfiguration dtls = config.getDtlsConfiguration();
        String[] defaultCipherSuiteNames = DtlsConfiguration.CIPHER_SUITES.split(",");
        Assert.assertEquals(defaultCipherSuiteNames.length, dtls.getCipherSuites().length);
        for (int i = 0; i < dtls.getCipherSuites().length; i++) {
            Assert.assertEquals(dtls.getCipherSuites()[i].name(), defaultCipherSuiteNames[i].trim());
        }
    }

    /**
     * Test default configuration is loaded into memory after failing to locate a configuration file.
     */
    @Test
    public void testFileNotFound() {
        // given
        String filepath = "not-found.xml";
        XmlConfigurationLoader loader = new XmlConfigurationLoader();

        // when
        MediaServerConfiguration config = loader.load(filepath);

        // then
        NetworkConfiguration network = config.getNetworkConfiguration();
        Assert.assertEquals(NetworkConfiguration.BIND_ADDRESS, network.getBindAddress());
        Assert.assertEquals(NetworkConfiguration.EXTERNAL_ADDRESS, network.getExternalAddress());
        Assert.assertEquals(NetworkConfiguration.NETWORK, network.getNetwork());
        Assert.assertEquals(NetworkConfiguration.SUBNET, network.getSubnet());
        Assert.assertEquals(NetworkConfiguration.SBC, network.isSbc());

        MgcpControllerConfiguration controller = config.getControllerConfiguration();
        Assert.assertEquals(MgcpControllerConfiguration.ADDRESS, controller.getAddress());
        Assert.assertEquals(MgcpControllerConfiguration.PORT, controller.getPort());
        Assert.assertEquals(MgcpControllerConfiguration.CONFIGURATION, controller.getConfiguration());
        Assert.assertEquals(MgcpControllerConfiguration.POOL_SIZE, controller.getPoolSize());
        Assert.assertFalse(controller.getEndpoints().hasNext());

        MediaConfiguration media = config.getMediaConfiguration();
        Assert.assertEquals(MediaConfiguration.TIMEOUT, media.getTimeout());
        Assert.assertEquals(MediaConfiguration.LOW_PORT, media.getLowPort());
        Assert.assertEquals(MediaConfiguration.HIGH_PORT, media.getHighPort());
        Assert.assertEquals(MediaConfiguration.JITTER_BUFFER_SIZE, media.getJitterBufferSize());
        Assert.assertEquals(0, media.countCodecs());

        ResourcesConfiguration resources = config.getResourcesConfiguration();
        Assert.assertEquals(ResourcesConfiguration.LOCAL_CONNECTION_COUNT, resources.getLocalConnectionCount());
        Assert.assertEquals(ResourcesConfiguration.REMOTE_CONNECTION_COUNT, resources.getRemoteConnectionCount());
        Assert.assertEquals(ResourcesConfiguration.PLAYER_COUNT, resources.getPlayerCount());
        Assert.assertEquals(ResourcesConfiguration.RECORDER_COUNT, resources.getRecorderCount());
        Assert.assertEquals(ResourcesConfiguration.DTMF_DETECTOR_COUNT, resources.getDtmfDetectorCount());
        Assert.assertEquals(ResourcesConfiguration.DTMF_DETECTOR_DBI, resources.getDtmfDetectorDbi());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_COUNT, resources.getDtmfGeneratorCount());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_DURATION, resources.getDtmfGeneratorToneDuration());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_VOLUME, resources.getDtmfGeneratorToneVolume());
        Assert.assertEquals(ResourcesConfiguration.SIGNAL_DETECTOR_COUNT, resources.getSignalDetectorCount());
        Assert.assertEquals(ResourcesConfiguration.SIGNAL_GENERATOR_COUNT, resources.getSignalGeneratorCount());

        DtlsConfiguration dtls = config.getDtlsConfiguration();
        String[] defaultCipherSuiteNames = DtlsConfiguration.CIPHER_SUITES.split(",");
        Assert.assertEquals(defaultCipherSuiteNames.length, dtls.getCipherSuites().length);
        for (int i = 0; i < dtls.getCipherSuites().length; i++) {
            Assert.assertEquals(dtls.getCipherSuites()[i].name(), defaultCipherSuiteNames[i].trim());
        }
    }

    @Test
    public void testFileNetworkMisconfigured() {
        // given
        String filepath = "media-server-network-misconfigured.xml";
        XmlConfigurationLoader loader = new XmlConfigurationLoader();
        
        // when
        MediaServerConfiguration config = loader.load(filepath);
        
        // then
        NetworkConfiguration network = config.getNetworkConfiguration();
        Assert.assertEquals(NetworkConfiguration.BIND_ADDRESS, network.getBindAddress());
        Assert.assertEquals(NetworkConfiguration.EXTERNAL_ADDRESS, network.getExternalAddress());
        Assert.assertEquals(NetworkConfiguration.NETWORK, network.getNetwork());
        Assert.assertEquals(NetworkConfiguration.SUBNET, network.getSubnet());
        Assert.assertEquals(NetworkConfiguration.SBC, network.isSbc());
        
        MgcpControllerConfiguration controller = config.getControllerConfiguration();
        Assert.assertEquals(MgcpControllerConfiguration.ADDRESS, controller.getAddress());
        Assert.assertEquals(MgcpControllerConfiguration.PORT, controller.getPort());
        Assert.assertEquals(MgcpControllerConfiguration.CONFIGURATION, controller.getConfiguration());
        Assert.assertEquals(MgcpControllerConfiguration.POOL_SIZE, controller.getPoolSize());
        Assert.assertFalse(controller.getEndpoints().hasNext());
        
        MediaConfiguration media = config.getMediaConfiguration();
        Assert.assertEquals(MediaConfiguration.TIMEOUT, media.getTimeout());
        Assert.assertEquals(MediaConfiguration.LOW_PORT, media.getLowPort());
        Assert.assertEquals(MediaConfiguration.HIGH_PORT, media.getHighPort());
        Assert.assertEquals(MediaConfiguration.JITTER_BUFFER_SIZE, media.getJitterBufferSize());
        Assert.assertEquals(0, media.countCodecs());
        
        ResourcesConfiguration resources = config.getResourcesConfiguration();
        Assert.assertEquals(ResourcesConfiguration.LOCAL_CONNECTION_COUNT, resources.getLocalConnectionCount());
        Assert.assertEquals(ResourcesConfiguration.REMOTE_CONNECTION_COUNT, resources.getRemoteConnectionCount());
        Assert.assertEquals(ResourcesConfiguration.PLAYER_COUNT, resources.getPlayerCount());
        Assert.assertEquals(ResourcesConfiguration.RECORDER_COUNT, resources.getRecorderCount());
        Assert.assertEquals(ResourcesConfiguration.DTMF_DETECTOR_COUNT, resources.getDtmfDetectorCount());
        Assert.assertEquals(ResourcesConfiguration.DTMF_DETECTOR_DBI, resources.getDtmfDetectorDbi());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_COUNT, resources.getDtmfGeneratorCount());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_DURATION, resources.getDtmfGeneratorToneDuration());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_VOLUME, resources.getDtmfGeneratorToneVolume());
        Assert.assertEquals(ResourcesConfiguration.SIGNAL_DETECTOR_COUNT, resources.getSignalDetectorCount());
        Assert.assertEquals(ResourcesConfiguration.SIGNAL_GENERATOR_COUNT, resources.getSignalGeneratorCount());
        
        DtlsConfiguration dtls = config.getDtlsConfiguration();
        String[] defaultCipherSuiteNames = DtlsConfiguration.CIPHER_SUITES.split(",");
        Assert.assertEquals(defaultCipherSuiteNames.length, dtls.getCipherSuites().length);
        for (int i = 0; i < dtls.getCipherSuites().length; i++) {
            Assert.assertEquals(dtls.getCipherSuites()[i].name(), defaultCipherSuiteNames[i].trim());
        }
    }

}
