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

package org.restcomm.media.core.configuration;

import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.restcomm.media.rtp.crypto.AlgorithmCertificate;
import org.restcomm.media.rtp.crypto.CipherSuite;

/**
 * @author guilherme.jansen@telestax.com
 */
public class DtlsConfiguration {

    public static final String MIN_VERSION = "1.0";
    public static final String MAX_VERSION = "1.2";
    public static final String CIPHER_SUITES = "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA";
    public static final String CERTIFICATE_PATH = "../conf/dtls/x509-server-ecdsa.pem";
    public static final String KEY_PATH = "../conf/dtls/x509-server-key-ecdsa.pem";
    public static final String ALGORITHM_CERTIFICATE = AlgorithmCertificate.ECDSA.name();

    private ProtocolVersion minVersion;
    private ProtocolVersion maxVersion;
    private CipherSuite[] cipherSuites;
    private String certificatePath;
    private String keyPath;
    private AlgorithmCertificate algorithmCertificate;

    public DtlsConfiguration() {
        setMinVersion(MIN_VERSION);
        setMaxVersion(MAX_VERSION);
        setCipherSuites(CIPHER_SUITES);
        this.certificatePath = CERTIFICATE_PATH;
        this.keyPath = KEY_PATH;
        setAlgorithmCertificate(ALGORITHM_CERTIFICATE);
    }

    public ProtocolVersion getMinVersion() {
        return minVersion;
    }

    public ProtocolVersion getMaxVersion() {
        return maxVersion;
    }

    public CipherSuite[] getCipherSuites() {
        return cipherSuites;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public AlgorithmCertificate getAlgorithmCertificate() {
        return algorithmCertificate;
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = getVersionFromString(minVersion);
    }

    public void setMaxVersion(String maxVersion) {
        this.maxVersion = getVersionFromString(maxVersion);
    }

    private ProtocolVersion getVersionFromString(String version) {
        if ("1.0".equals(version)) {
            return ProtocolVersion.DTLSv10;
        } else if ("1.2".equals(version)) {
            return ProtocolVersion.DTLSv12;
        } else {
            throw new IllegalArgumentException("Invalid DTLS version");
        }
    }

    public void setCipherSuites(String cipherSuites) {
        String[] values = cipherSuites.split(",");
        CipherSuite[] cipherSuiteTemp = new CipherSuite[values.length];
        for (int i = 0; i < values.length; i++) {
            cipherSuiteTemp[i] = CipherSuite.valueOf(values[i].trim());
        }
        if (cipherSuiteTemp.length > 0) {
            this.cipherSuites = cipherSuiteTemp;
        }
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public void setAlgorithmCertificate(String algorithmCertificate) {
        this.algorithmCertificate = AlgorithmCertificate.valueOf(algorithmCertificate.toUpperCase());
    }
}
