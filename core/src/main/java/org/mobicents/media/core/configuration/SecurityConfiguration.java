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

/**
 * @author guilherme.jansen@telestax.com
 */
public class SecurityConfiguration {

    public static final String CIPHER_SUITE = "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256";
    public static final String CERTIFICATE_PATH = "x509-server-ecdsa.pem";
    public static final String KEY_PATH = "x509-server-key-ecdsa.pem";

    private String cipherSuite;
    private String certificatePath;
    private String keyPath;

    public SecurityConfiguration() {
        this.cipherSuite = CIPHER_SUITE;
        this.certificatePath = CERTIFICATE_PATH;
        this.keyPath = KEY_PATH;
    }

    public String getCipherSuite() {
        return this.cipherSuite;
    }

    public String getCertificatePath() {
        return this.certificatePath;
    }

    public String getKeyPath() {
        return this.keyPath;
    }

    public void setCipherSuite(String cipherSuite) {
        if (cipherSuite == null || cipherSuite.isEmpty()) {
            throw new IllegalArgumentException("CipherSuite cannot be empty.");
        }
        this.cipherSuite = cipherSuite;
    }

    public void setCertificatePath(String certificatePath) {
        if (certificatePath == null || certificatePath.isEmpty()) {
            throw new IllegalArgumentException("CertificatePath cannot be empty.");
        }
        this.certificatePath = certificatePath;
    }

    public void setKeyPath(String keyPath) {
        if (keyPath == null || keyPath.isEmpty()) {
            throw new IllegalArgumentException("KeyPath cannot be empty.");
        }
        this.keyPath = keyPath;
    }

}
