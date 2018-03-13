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

import org.bouncycastle.crypto.tls.ClientCertificateType;
import org.bouncycastle.crypto.tls.SignatureAlgorithm;

/**
 * @author guilherme.jansen@telestax.com
 */
public enum AlgorithmCertificate {

    RSA(SignatureAlgorithm.rsa, ClientCertificateType.rsa_sign),
    RSA_FIXED_DH(SignatureAlgorithm.rsa, ClientCertificateType.rsa_fixed_dh),
    RSA_EPHEMERAL_DH_RESERVED(SignatureAlgorithm.rsa, ClientCertificateType.rsa_ephemeral_dh_RESERVED),
    RSA_FIXED_ECDH(SignatureAlgorithm.rsa, ClientCertificateType.rsa_fixed_ecdh),
    DSA(SignatureAlgorithm.dsa, ClientCertificateType.dss_sign),
    DSA_FIXED_DH(SignatureAlgorithm.dsa, ClientCertificateType.dss_fixed_dh),
    DSA_EPHEMERAL_DH_RESERVED(SignatureAlgorithm.dsa, ClientCertificateType.dss_ephemeral_dh_RESERVED),
    ECDSA(SignatureAlgorithm.ecdsa, ClientCertificateType.ecdsa_sign),
    ECDSA_FIXED_ECDH(SignatureAlgorithm.ecdsa, ClientCertificateType.ecdsa_fixed_ecdh);

    private short signatureAlgorithm;
    private short clientCertificate;

    AlgorithmCertificate(short signatureAlgorithm, short clientCertificate) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.clientCertificate = clientCertificate;
    }

    public short getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public short getClientCertificate() {
        return clientCertificate;
    }

}
