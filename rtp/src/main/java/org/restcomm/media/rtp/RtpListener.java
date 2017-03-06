/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.rtp;

/**
 * Listens for failures on RTP and RTCP flows.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface RtpListener {

	/**
	 * Event triggered when an RTP-related failure occurs.
	 * 
	 * @param e
	 *            The exception that originated the failure.
	 */
	public void onRtpFailure(Throwable e);

	/**
	 * Event triggered when an RTP-related failure occurs.
	 * 
	 * @param message
	 *            The reason why the failure occurred
	 */
	public void onRtpFailure(String message);

	/**
	 * Event triggered when an RTCP-related failure occurs.
	 * 
	 * @param e
	 *            The exception that originated the failure.
	 */
	public void onRtcpFailure(Throwable e);

	/**
	 * Event triggered when an RTCP-related failure occurs.
	 * 
	 * @param message
	 *            The reason why the failure occurred
	 */
	public void onRtcpFailure(String e);

}
