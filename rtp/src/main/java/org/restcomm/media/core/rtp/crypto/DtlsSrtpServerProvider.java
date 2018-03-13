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

package org.restcomm.media.core.rtp.crypto;

import org.bouncycastle.crypto.tls.ProtocolVersion;

/**
 * @author guilherme.jansen@telestax.com
 */
public class DtlsSrtpServerProvider {

    private ProtocolVersion minVersion;
    private ProtocolVersion maxVersion;
    private CipherSuite[] cipherSuites;
    private String[] certificatePaths;
    private String keyPath;
    private AlgorithmCertificate algorithmCertificate;

    public DtlsSrtpServerProvider(ProtocolVersion minVersion, ProtocolVersion maxVersion, CipherSuite[] cipherSuites,
            String certificatePath, String keyPath, AlgorithmCertificate algorithmCertificate) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.cipherSuites = cipherSuites;
        this.certificatePaths = new String[] { certificatePath };
        this.keyPath = keyPath;
        this.algorithmCertificate = algorithmCertificate;
    }

    public DtlsSrtpServer provide() {
        DtlsSrtpServer server = new DtlsSrtpServer(minVersion, maxVersion, cipherSuites, certificatePaths, keyPath,
                algorithmCertificate);
        return server;
    }

}
