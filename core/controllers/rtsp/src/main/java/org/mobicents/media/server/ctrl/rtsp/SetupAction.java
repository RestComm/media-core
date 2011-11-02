/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.ctrl.rtsp;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.rtsp.RtspHeaders;
import org.jboss.netty.handler.codec.rtsp.RtspResponseStatuses;
import org.jboss.netty.handler.codec.rtsp.RtspVersions;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.resource.Player;

/**
 * 
 * @author amit bhayani
 * 
 */
public class SetupAction implements Callable<HttpResponse> {

    // TODO : Multicast not taken care
    // TODO : Only UDP is supported, check that no TCP in Transport
    private static final Logger logger = Logger.getLogger(SetupAction.class);
//    private final String ENDPOINT_NAME = "/mobicents/media/aap/$";
    private final String ENDPOINT_NAME = "/mobicents/media/mms/$";
    private static final ConnectionMode mode = ConnectionMode.SEND_ONLY;
    private final RtspController rtspController;
    private final HttpRequest request;
    private final String remoteHost;
    private String clientPort = null;
    
	private static final String DATE_PATTERN = "EEE, d MMM yyyy HH:mm:ss z";
	private static final SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN);

    public SetupAction(RtspController rtspController, HttpRequest request, String remoteIp) {
        this.rtspController = rtspController;
        this.request = request;
        this.remoteHost = remoteIp;
    }

    private Session getSession(String sessionID) {
        Session session = null;
        
        if (sessionID != null) {
            session = rtspController.getSession(sessionID);
        }
        
        if (session != null) {
            return session;
        }
        
        session = new Session();
        rtspController.addSession(session);
        
        return session;
    }
    
    public HttpResponse call() throws Exception {
    	HttpResponse response = null;

        //determine session
        Session session = getSession(this.request.getHeader(RtspHeaders.Names.SESSION));
        if (session == null) {
            response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.SESSION_NOT_FOUND);
            response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
            response.setHeader(RtspHeaders.Names.CSEQ, this.request.getHeader(RtspHeaders.Names.CSEQ));
            return response;
        }

        String filePath = null;
        String trackID = null;
        
        URI uri = new URI(this.request.getUri());
        String path = uri.getPath();

        int pos = path.indexOf("/trackID");
        filePath = rtspController.getMediaDir();
        if (pos > 0) {
            filePath += path.substring(0, pos);
            trackID = path.substring(pos + 1);
        } else {
            filePath += path;
        }
        
        File f = new File(filePath);
        if (f.isDirectory() || !f.exists()) {
                response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.NOT_FOUND);
                response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
                response.setHeader(RtspHeaders.Names.CSEQ, this.request.getHeader(RtspHeaders.Names.CSEQ));
                return response;
        }

        int remotePort = this.getRemotePort();
        InetSocketAddress remoteAddress = new InetSocketAddress(remoteHost, remotePort);
                
        if (session.getState() == SessionState.PLAYING || 
                session.getState() == SessionState.RECORDING) {
            // We don't support changing the Transport while state is PLAYING or RECORDING
            response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.METHOD_NOT_VALID);
            response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
            response.setHeader(RtspHeaders.Names.CSEQ, this.request.getHeader(RtspHeaders.Names.CSEQ));
            return response;
        }
        
        Endpoint endpoint = (Endpoint) session.getAttribute("endpoint");
        if (endpoint == null) {
            try {
                endpoint = rtspController.getNamingService().lookup(ENDPOINT_NAME, false);
                session.addAttribute("endpoint", endpoint);
            } catch (ResourceUnavailableException e) {
                logger.warn("There is no free endpoint: " + ENDPOINT_NAME);
                response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.SERVICE_UNAVAILABLE);
                response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
                response.setHeader(RtspHeaders.Names.CSEQ, this.request.getHeader(RtspHeaders.Names.CSEQ));
                return response;
            }
        }
        
        Connection connection = (Connection) session.getAttribute("connection");
        if (connection == null) {
            try {
                connection = endpoint.createConnection();
                connection.setMode(mode);
                session.addAttribute("connection", connection);
            } catch (Exception e) {
                logger.error(e);
                response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.SERVICE_UNAVAILABLE);
                response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
                response.setHeader(RtspHeaders.Names.CSEQ, this.request.getHeader(RtspHeaders.Names.CSEQ));
                return response;
            }
        }

        int ssrc =  268435456 + (int) ( Math.random()*(Integer.MAX_VALUE - 268435456) );
        
        Player player = null;//(Player) endpoint.getComponent("player");
        player.setURL(f.getAbsolutePath());
//        player.setSSRC(trackID, ssrc);
        
        List<String> trackIds = (List<String>)session.getAttribute("trackIds");
        if(trackIds == null){
        	trackIds = new ArrayList<String>();
        	session.addAttribute("trackIds", trackIds);
        }
        trackIds.add(trackID);
        
        connection.setOtherParty(trackID, remoteAddress);
        
        int port = 0;//endpoint.getLocalPort(trackID);
        String source = null;//endpoint.getLocalAddress(trackID);
		String lastModified = formatter.format(new Date(f.lastModified()));
		String date = formatter.format(new Date());
        
        String transport = "RTP/AVP/UDP;unicast;source="+source + ";"+this.clientPort + ";server_port=" + port + "-" + port +
                ";ssrc=" + Integer.toHexString(ssrc);
        response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
        response.setHeader(RtspHeaders.Names.CSEQ, this.request.getHeader(RtspHeaders.Names.CSEQ));
        response.setHeader(RtspHeaders.Names.SESSION, session.getId());
        response.setHeader(RtspHeaders.Names.TRANSPORT, transport);
		response.setHeader(HttpHeaders.Names.LAST_MODIFIED, lastModified);
		//TODO CACHE_CONTROL must come from settings. Let user decide how they want CACHE_CONTROL
		response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "must-revalidate");
		response.setHeader(HttpHeaders.Names.DATE, date);
		//TODO EXPIRES must come from settings. Also depends on CACHE_CONTRL
		response.setHeader(HttpHeaders.Names.EXPIRES, date);
        session.setState(SessionState.READY);
        //ConnectionActivity connectionActivity = session.addConnection(connection);

        return response;
    }

    private int getRemotePort() {
        String transport = this.request.getHeader(RtspHeaders.Names.TRANSPORT);
        String[] transParameters = transport.split(";");
        for (String s : transParameters) {
            if (s.contains("client_port")) {
                this.clientPort = s;
                String[] values = s.split("=");
                values = values[1].split("-");
                return Integer.parseInt(values[0]);
            }
        }
        return 0;
    }
}
