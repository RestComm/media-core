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

package org.restcomm.media.bootstrap.configuration;

import org.bouncycastle.crypto.tls.ClientCertificateType;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.SignatureAlgorithm;
import org.junit.Assert;
import org.junit.Test;
import org.restcomm.media.bootstrap.configuration.XmlConfigurationLoader;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.core.configuration.MediaConfiguration;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.core.configuration.MgcpControllerConfiguration;
import org.restcomm.media.core.configuration.MgcpEndpointConfiguration;
import org.restcomm.media.core.configuration.NetworkConfiguration;
import org.restcomm.media.core.configuration.ResourcesConfiguration;
import org.restcomm.media.spi.RelayType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class XmlConfigurationLoaderTest {

    /**
     * Test all information from a valid and complete configuration file is loaded properly into memory.
     * @throws Exception
     */
    @Test
    public void testLoadConfiguration() throws Exception {
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
        MgcpEndpointConfiguration bridgeEndpoint = controller.getEndpoint("mobicents/bridge/");
        Assert.assertNotNull(bridgeEndpoint);
        Assert.assertEquals(RelayType.SPLITTER, bridgeEndpoint.getRelayType());
        MgcpEndpointConfiguration ivrEndpoint = controller.getEndpoint("mobicents/ivr/");
        Assert.assertNotNull(ivrEndpoint);
        Assert.assertEquals(RelayType.MIXER, ivrEndpoint.getRelayType());
        MgcpEndpointConfiguration cnfEndpoint = controller.getEndpoint("mobicents/cnf/");
        Assert.assertNotNull(cnfEndpoint);
        Assert.assertEquals(RelayType.MIXER, cnfEndpoint.getRelayType());

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
        Assert.assertEquals(-25, resources.getDtmfDetectorDbi());
        Assert.assertEquals(100, resources.getDtmfGeneratorToneDuration());
        Assert.assertEquals(-25, resources.getDtmfGeneratorToneVolume());

        DtlsConfiguration dtls = config.getDtlsConfiguration();
        Assert.assertEquals(ProtocolVersion.DTLSv10, dtls.getMinVersion());
        Assert.assertEquals(ProtocolVersion.DTLSv12, dtls.getMaxVersion());
        String[] defaultCipherSuiteNames = DtlsConfiguration.CIPHER_SUITES.split(",");
        Assert.assertEquals(defaultCipherSuiteNames.length, dtls.getCipherSuites().length);
        for (int i = 0; i < dtls.getCipherSuites().length; i++) {
            Assert.assertEquals(dtls.getCipherSuites()[i].name(), defaultCipherSuiteNames[i].trim());
        }
        Assert.assertEquals(DtlsConfiguration.CERTIFICATE_PATH, dtls.getCertificatePath());
        Assert.assertEquals(DtlsConfiguration.KEY_PATH, dtls.getKeyPath());
        Assert.assertEquals(SignatureAlgorithm.ecdsa, dtls.getAlgorithmCertificate().getSignatureAlgorithm());
        Assert.assertEquals(ClientCertificateType.ecdsa_sign, dtls.getAlgorithmCertificate().getClientCertificate());

        Assert.assertEquals(100, resources.getPlayerCacheSize());
        Assert.assertEquals(true, resources.getPlayerCacheEnabled());
        Assert.assertEquals(".*", resources.getPlayerCacheUrlPattern());
    }

    /**
     * Test default configuration is loaded into memory after failing to locate a configuration file.
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFileNotFound() throws Exception {
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
        Assert.assertFalse(controller.getEndpoints().hasNext());

        MediaConfiguration media = config.getMediaConfiguration();
        Assert.assertEquals(MediaConfiguration.TIMEOUT, media.getTimeout());
        Assert.assertEquals(MediaConfiguration.LOW_PORT, media.getLowPort());
        Assert.assertEquals(MediaConfiguration.HIGH_PORT, media.getHighPort());
        Assert.assertEquals(MediaConfiguration.JITTER_BUFFER_SIZE, media.getJitterBufferSize());
        Assert.assertEquals(0, media.countCodecs());

        ResourcesConfiguration resources = config.getResourcesConfiguration();
        Assert.assertEquals(ResourcesConfiguration.DTMF_DETECTOR_DBI, resources.getDtmfDetectorDbi());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_DURATION, resources.getDtmfGeneratorToneDuration());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_VOLUME, resources.getDtmfGeneratorToneVolume());

        DtlsConfiguration dtls = config.getDtlsConfiguration();
        Assert.assertEquals(ProtocolVersion.DTLSv10, dtls.getMinVersion());
        Assert.assertEquals(ProtocolVersion.DTLSv12, dtls.getMaxVersion());
        String[] defaultCipherSuiteNames = DtlsConfiguration.CIPHER_SUITES.split(",");
        Assert.assertEquals(defaultCipherSuiteNames.length, dtls.getCipherSuites().length);
        for (int i = 0; i < dtls.getCipherSuites().length; i++) {
            Assert.assertEquals(dtls.getCipherSuites()[i].name(), defaultCipherSuiteNames[i].trim());
        }
        Assert.assertEquals(DtlsConfiguration.CERTIFICATE_PATH, dtls.getCertificatePath());
        Assert.assertEquals(DtlsConfiguration.KEY_PATH, dtls.getKeyPath());
        Assert.assertEquals(SignatureAlgorithm.ecdsa, dtls.getAlgorithmCertificate().getSignatureAlgorithm());
        Assert.assertEquals(ClientCertificateType.ecdsa_sign, dtls.getAlgorithmCertificate().getClientCertificate());
    }

    @Test(expected = Exception.class)
    public void testFileNetworkMisconfigured() throws Exception {
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
        Assert.assertFalse(controller.getEndpoints().hasNext());
        
        MediaConfiguration media = config.getMediaConfiguration();
        Assert.assertEquals(MediaConfiguration.TIMEOUT, media.getTimeout());
        Assert.assertEquals(MediaConfiguration.LOW_PORT, media.getLowPort());
        Assert.assertEquals(MediaConfiguration.HIGH_PORT, media.getHighPort());
        Assert.assertEquals(MediaConfiguration.JITTER_BUFFER_SIZE, media.getJitterBufferSize());
        Assert.assertEquals(0, media.countCodecs());
        
        ResourcesConfiguration resources = config.getResourcesConfiguration();
        Assert.assertEquals(ResourcesConfiguration.DTMF_DETECTOR_DBI, resources.getDtmfDetectorDbi());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_DURATION, resources.getDtmfGeneratorToneDuration());
        Assert.assertEquals(ResourcesConfiguration.DTMF_GENERATOR_TONE_VOLUME, resources.getDtmfGeneratorToneVolume());
        
        DtlsConfiguration dtls = config.getDtlsConfiguration();
        Assert.assertEquals(ProtocolVersion.DTLSv10, dtls.getMinVersion());
        Assert.assertEquals(ProtocolVersion.DTLSv12, dtls.getMaxVersion());
        String[] defaultCipherSuiteNames = DtlsConfiguration.CIPHER_SUITES.split(",");
        Assert.assertEquals(defaultCipherSuiteNames.length, dtls.getCipherSuites().length);
        for (int i = 0; i < dtls.getCipherSuites().length; i++) {
            Assert.assertEquals(dtls.getCipherSuites()[i].name(), defaultCipherSuiteNames[i].trim());
        }
        Assert.assertEquals(DtlsConfiguration.CERTIFICATE_PATH, dtls.getCertificatePath());
        Assert.assertEquals(DtlsConfiguration.KEY_PATH, dtls.getKeyPath());
        Assert.assertEquals(SignatureAlgorithm.ecdsa, dtls.getAlgorithmCertificate().getSignatureAlgorithm());
        Assert.assertEquals(ClientCertificateType.ecdsa_sign, dtls.getAlgorithmCertificate().getClientCertificate());
    }

}
