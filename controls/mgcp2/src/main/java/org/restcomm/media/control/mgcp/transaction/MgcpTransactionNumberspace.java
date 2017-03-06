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

package org.restcomm.media.control.mgcp.transaction;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

/**
 * Transaction identifiers are integer numbers in the range from 1 to 999,999,999 (both included).
 * 
 * <p>
 * Call-agents may decide to use a specific number space for each of the gateways that they manage, or to use the same number
 * space for all gateways that belong to some arbitrary group.
 * </p>
 * 
 * <p>
 * Call agents may decide to share the load of managing a large gateway between several independent processes. These processes
 * MUST then share the transaction number space.
 * <p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionNumberspace {

    private static final Logger log = Logger.getLogger(MgcpTransactionNumberspace.class);

    private static final int MINIMUM_ID = 1;
    private static final int MAXIMUM_ID = 999999999;

    private final int minimum;
    private final int maximum;
    private AtomicInteger current;

    /**
     * Creates a new MGCP transaction number space.
     * 
     * @param minimum The minimum transaction identifier.
     * @param maximum The maximum transaction identifier.
     * @throws IllegalArgumentException When minimum or maximum are outside the legal range [1, 999.999.999] or when maximum is
     *         lesser than minimum.
     */
    public MgcpTransactionNumberspace(int minimum, int maximum) throws IllegalArgumentException {
        if (minimum < MINIMUM_ID || minimum > MAXIMUM_ID) {
            throw new IllegalArgumentException(
                    "Minimum transaction identifier " + minimum + " must be " + MINIMUM_ID + " <= id <= " + MAXIMUM_ID);
        }

        if (maximum < MINIMUM_ID || maximum > MAXIMUM_ID) {
            throw new IllegalArgumentException(
                    "Maximum transaction identifier " + maximum + " must be " + MINIMUM_ID + " <= id <= " + MAXIMUM_ID);
        }

        if (maximum < minimum) {
            throw new IllegalArgumentException(
                    "Maximum transaction identifier " + maximum + " must be greater than minimum identifier " + minimum);
        }

        this.minimum = minimum;
        this.maximum = maximum;
        this.current = new AtomicInteger(this.minimum);
    }

    /**
     * Creates a new MGCP transaction number space with range [1;999.999.999]
     */
    public MgcpTransactionNumberspace() {
        this(MINIMUM_ID, MAXIMUM_ID);
    }

    public int getMinimum() {
        return minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    public int getCurrent() {
        return current.get();
    }

    public int generateId() {
        final int next = this.current.getAndIncrement();
        boolean reset = this.current.compareAndSet(this.maximum + 1, this.minimum);

        if(reset) {
            if (log.isInfoEnabled()) {
                log.info("Reached maximum transaction identifier " + this.maximum + ". Reset number space to " + this.minimum);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Current MGCP transaction identifier is " + next);
            }
        }
        return next;
    }

}
