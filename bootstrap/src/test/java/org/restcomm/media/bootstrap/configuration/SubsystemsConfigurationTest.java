/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.media.core.configuration.DriverConfiguration;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.core.configuration.SubsystemsConfiguration;

/**
 * @author anikiforov
 */
public class SubsystemsConfigurationTest {

    /**
     * Test all information from a valid and complete configuration file is loaded properly into memory.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadConfiguration() throws Exception {
        // given
        XmlConfigurationLoader loader = new XmlConfigurationLoader();

        // when
        MediaServerConfiguration config = loader.load("mediaserver.xml");

        final SubsystemsConfiguration subsystemsConf = config.getSubsystemsConfiguration();
        Assert.assertNotNull(subsystemsConf);
        final Collection<DriverConfiguration> drivers = subsystemsConf.getDrivers("asr");
        Assert.assertNotNull(drivers);
        Assert.assertEquals(2, drivers.size());
        {
            final DriverConfiguration driver = subsystemsConf.getDriver("asr", "stub");
            Assert.assertEquals("stub", driver.getDriverName());
            Assert.assertEquals("org.mobicents.media.resource.asr.StubAsrDriver", driver.getClassName());
            final Map<String, String> parameters = driver.getParameters();
            Assert.assertNotNull(parameters);
            Assert.assertEquals(1, parameters.size());
            Assert.assertTrue(parameters.containsKey("stubName"));
            Assert.assertEquals("Stub Driver", parameters.get("stubName"));
        }
        {
            final DriverConfiguration driver = subsystemsConf.getDriver("asr", "stub2");
            Assert.assertEquals("stub2", driver.getDriverName());
            Assert.assertEquals("org.mobicents.media.resource.asr.StubAsrDriver2", driver.getClassName());
            final Map<String, String> parameters = driver.getParameters();
            Assert.assertNotNull(parameters);
            Assert.assertEquals(1, parameters.size());
            Assert.assertTrue(parameters.containsKey("stubName2"));
            Assert.assertEquals("Stub Driver 2", parameters.get("stubName2"));
        }
    }

    @Test
    public void testNoSubsystems() throws Exception {
        // given
        XmlConfigurationLoader loader = new XmlConfigurationLoader();

        // when
        MediaServerConfiguration config = loader.load("mediaserver-no-subsystems.xml");

        final SubsystemsConfiguration subsystemsConf = config.getSubsystemsConfiguration();
        Assert.assertNotNull(subsystemsConf);
        Assert.assertEquals(0, subsystemsConf.getDrivers("asr").size());
    }

    @Test
    public void testNoSubsystem() throws Exception {
        // given
        XmlConfigurationLoader loader = new XmlConfigurationLoader();

        // when
        MediaServerConfiguration config = loader.load("mediaserver-no-subsystem.xml");

        final SubsystemsConfiguration subsystemsConf = config.getSubsystemsConfiguration();
        Assert.assertNotNull(subsystemsConf);
        Assert.assertEquals(0, subsystemsConf.getDrivers("asr").size());
    }

    @Test
    public void testNoAsrSubsystem() throws Exception {
        // given
        XmlConfigurationLoader loader = new XmlConfigurationLoader();

        // when
        MediaServerConfiguration config = loader.load("mediaserver-no-subsystem.xml");

        final SubsystemsConfiguration subsystemsConf = config.getSubsystemsConfiguration();
        Assert.assertNotNull(subsystemsConf);
        Assert.assertEquals(0, subsystemsConf.getDrivers("asr").size());
        Assert.assertEquals(0, subsystemsConf.getDrivers("any").size());
    }

    @Test
    public void testNoAsrDrivers() throws Exception {
        // given
        XmlConfigurationLoader loader = new XmlConfigurationLoader();

        // when
        MediaServerConfiguration config = loader.load("mediaserver-no-drivers.xml");

        final SubsystemsConfiguration subsystemsConf = config.getSubsystemsConfiguration();
        Assert.assertNotNull(subsystemsConf);
        Assert.assertEquals(0, subsystemsConf.getDrivers("any").size());
    }

}
