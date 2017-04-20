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
        
package org.restcomm.media.network.common.filter;

import java.net.InetSocketAddress;

import org.restcomm.media.network.api.NetworkChannel;
import org.restcomm.media.network.api.NetworkFilter;

import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SbcNetworkFilter implements NetworkFilter, IpFilterRule {

    @Override
    public boolean isSecure(NetworkChannel<?> channel, InetSocketAddress source) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean matches(InetSocketAddress remoteAddress) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IpFilterRuleType ruleType() {
        // TODO Auto-generated method stub
        return null;
    }

}
