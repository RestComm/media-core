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

package org.mobicents.media.core.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mobicents.media.server.impl.rtp.crypto.CipherSuite;

/**
 * @author guilherme.jansen@telestax.com
 */
public class DtlsConfiguration {

    public static final List<CipherSuite> CIPHER_SUITES = Arrays.asList(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256);

    private List<CipherSuite> cipherSuite;

    public DtlsConfiguration() {
        this.cipherSuite = CIPHER_SUITES;
    }

    public List<CipherSuite> getCipherSuites() {
        return cipherSuite;
    }

    public void setCipherSuites(List<CipherSuite> cipherSuites) {
        this.cipherSuite = cipherSuites;
    }

    public void setCipherSuite(String values[]) {
        ArrayList<CipherSuite> cipherSuiteTemp = new ArrayList<CipherSuite>();
        for (int i = 0; i < values.length; i++) {
            cipherSuiteTemp.add(CipherSuite.valueOf(values[i].trim()));
        }
        if (cipherSuiteTemp.size() > 0) {
            this.cipherSuite = cipherSuiteTemp;
        }
    }

}
