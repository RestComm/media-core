/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.restcomm.media.ice;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceAuthenticatorImpl implements IceAuthenticator {

    // Control message integrity
    private final SecureRandom random;
    protected String ufrag;
    protected String password;
    protected String remoteUfrag;
    protected String remotePassword;

    private final StringBuilder builder;

    public IceAuthenticatorImpl() {
        this.random = new SecureRandom();
        this.ufrag = "";
        this.password = "";
        this.remoteUfrag = "";
        this.remotePassword = "";

        this.builder = new StringBuilder();
    }

    public String getUfrag() {
        return ufrag;
    }

    public String getPassword() {
        return password;
    }

    public String getRemoteUfrag() {
        return remoteUfrag;
    }

    public void setRemoteUfrag(String remoteUfrag) {
        this.remoteUfrag = remoteUfrag;
    }

    public String getRemotePassword() {
        return remotePassword;
    }

    public void setRemotePassword(String remotePassword) {
        this.remotePassword = remotePassword;
    }

    /**
     * The ice-ufrag and ice-pwd attributes MUST be chosen randomly at the beginning of a session.
     * 
     * <p>
     * The ice-ufrag attribute MUST contain at least 24 bits of randomness, and the ice-pwd attribute MUST contain at least 128
     * bits of randomness.<br>
     * This means that the ice-ufrag attribute will be at least 4 characters long, and the ice-pwd at least 22 characters long,
     * since the grammar for these attributes allows for 6 bits of randomness per character. <br>
     * The attributes MAY be longer than 4 and 22 characters, respectively, of course, up to 256 characters. The upper limit
     * allows for buffer sizing in implementations. Its large upper limit allows for increased amounts of randomness to be added
     * over time.
     * </p>
     * 
     * @see <a href="https://tools.ietf.org/html/rfc5245#section-15.4">RFC 5245</a>
     */
    public void generateIceCredentials() {
        this.ufrag = generateIceCredential(24, 4, 256);
        this.password = generateIceCredential(128, 22, 256);
    }

    private String generateIceCredential(int numBits, int min, int max) {
        // Clean string builder
        this.builder.setLength(0);

        // Generate random strings until minimum size is satisfied
        do {
            this.builder.append(new BigInteger(numBits, this.random).toString(32));
        } while (this.builder.length() < min);

        // Trim string if it surpasses maximum size
        if (builder.length() > max) {
            builder.setLength(max);
        }
        return this.builder.toString();
    }

    @Override
    public byte[] getLocalKey(String ufrag) {
        if (isUserRegistered(ufrag)) {
            if (this.password != null) {
                return this.password.getBytes();
            }
        }
        return null;
    }

    @Override
    public byte[] getRemoteKey(String ufrag, String media) {
        // Check whether full username is provided or just the fragment
        int colon = ufrag.indexOf(":");
        if (colon < 0) {
            if (ufrag.equals(this.remoteUfrag)) {
                return this.remotePassword.getBytes();
            }
        } else {
            if (ufrag.equals(this.ufrag)) {
                if (this.remotePassword != null) {
                    return this.remotePassword.getBytes();
                }
            }
        }
        return null;
    }

    @Override
    public boolean isUserRegistered(String ufrag) {
        int colon = ufrag.indexOf(":");
        String result = colon < 0 ? ufrag : ufrag.substring(0, colon);
        return result.equals(this.ufrag);
    }

    @Override
    public boolean validateUsername(String username) {
        // Username must separate local and remote ufrags with a colon
        int colon = username.indexOf(":");
        if (colon == -1) {
            return false;
        }
        // Local ufrag must match the one generated by this agent
        return username.substring(0, colon).equals(this.ufrag);
    }

    public void reset() {
        this.ufrag = "";
        this.password = "";
        this.remoteUfrag = "";
        this.remotePassword = "";
    }

}
