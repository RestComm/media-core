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
package org.mobicents.media.server.bootstrap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.Extension;
import org.mobicents.media.server.impl.resource.mediaplayer.mpeg.AudioTrack;
import org.mobicents.media.server.impl.resource.mediaplayer.mpeg.MpegPresentation;
import org.mobicents.media.server.impl.resource.mediaplayer.mpeg.VideoTrack;

/**
 * 
 * @author amit bhayani
 *
 */
public class SDPDeployer {

	private static Logger logger = Logger.getLogger(SDPDeployer.class);

	private static final String EXTENSION_SDP = ".sdp";

	public SDPDeployer() {

	}

	public void undeploy(File file) {
		try {
			File sdpFile = getSDPFile(file, false);
			if (sdpFile.exists()) {
				sdpFile.delete();
			}
		} catch (Exception e) {
			logger.info("Could not un-deploy " + file, e);
		}
	}

	public void deploy(File file) {
		String filePath = file.getPath();

		if (filePath.endsWith(Extension.WAV) || filePath.endsWith(Extension.GSM)) {
			try {
				File sdpFile = getSDPFile(file, true);
				String mediaSdp = createAudioMediaDescription(file);
				FileWriter writer = new FileWriter(sdpFile);
				writer.write(this.getSessionDesc());
				writer.write(mediaSdp);
				writer.close();
			} catch (Exception e) {
				logger.info("Could not deploy " + file, e);
			}
		} else if (filePath.endsWith(Extension.MP4) || filePath.endsWith(Extension.THREE_GP)
				|| filePath.endsWith(Extension.MOV)) {
			MpegPresentation presentation = null;
			try {
				presentation = new MpegPresentation(file.toURI().toURL());

				AudioTrack audioTrack = presentation.getAudioTrack();
				String audioSdp = audioTrack != null ? audioTrack.getSdpText() : null;

				VideoTrack videoTrack = presentation.getVideoTrack();
				String videoSdp = videoTrack != null ? videoTrack.getSdpText() : null;

				File sdpFile = getSDPFile(file, true);
				FileWriter writer = new FileWriter(sdpFile);

				writer.write(this.getSessionDesc());

				boolean write = true;
				if (audioSdp != null) {
					writer.write(audioSdp);
					write = false;
				}

				if (videoSdp != null) {
					writer.write(videoSdp);
					write = false;
				}

				writer.close();

				if (write) {
					logger.error("Writing to file " + sdpFile.getPath() + " failed!");
					// TODO : Should we delete file if writing fails?
				}

			} catch (Exception e) {
				logger.info("Could not deploy " + file, e);
			} finally {
				if (presentation != null) {
					presentation.close();
					presentation = null;
				}
			}
		}
	}

	private File getSDPFile(File origFile, boolean create) throws IOException {
		File sdpFile = null;
		String orgFileName = origFile.getName();
		String parentPath = origFile.getParent();
		String sdpFilePath = parentPath + File.separator + orgFileName + EXTENSION_SDP;

		sdpFile = new File(sdpFilePath);
		if (!sdpFile.exists() && create) {
			boolean success = sdpFile.createNewFile();
			if (!success) {
				logger.warn("Creation of SDP file " + sdpFilePath + " failed");
			} else if (logger.isInfoEnabled()) {
				logger.info("Created new empty SDP File " + sdpFilePath);
			}
		}
		return sdpFile;
	}

	private String getSessionDesc() {
		StringBuffer s = new StringBuffer();
		s.append("v=0\n");

		String sessionID = Long.toString(System.currentTimeMillis() & 0xffffff);
		String ipAddress = System.getProperty(Main.MMS_BIND_ADDRESS);
		s.append("o=MobicentsMediaServer ").append(sessionID).append(" ").append(sessionID).append(" ").append("IN")
				.append(" ").append("IP4").append(" ").append(ipAddress).append("\n");
		s.append("s=session\n");
		s.append("c=IN IP4 ").append(ipAddress).append("\n");
		s.append("t=0 0\n");
		return s.toString();
	}

	private String createAudioMediaDescription(File file) throws UnsupportedAudioFileException, IOException {

		int payload = -1;
		String rtpMap = null;

		StringBuffer sb = new StringBuffer();

		String filePath = file.getPath();

		if (filePath.endsWith(Extension.WAV)) {
			AudioFileFormat auFileFmt = AudioSystem.getAudioFileFormat(file);
			AudioFormat auFmt = auFileFmt.getFormat();

			Encoding encoding = auFmt.getEncoding();
			if (encoding == Encoding.ALAW) {
				payload = 8;
				rtpMap = "8 pcma/8000";
			} else if (encoding == Encoding.ULAW) {
				payload = 0;
				rtpMap = "0 pcmu/8000";
			} else if (encoding == Encoding.PCM_SIGNED) {
				int sampleSize = auFmt.getSampleSizeInBits();
				if (sampleSize != 16) {
					throw new UnsupportedAudioFileException("Found unsupported Format " + auFileFmt);
				}
				int sampleRate = (int) auFmt.getSampleRate();
				if (sampleRate == 44100) {
					int channels = auFmt.getChannels();
					if (channels == 1) {
						payload = 11;
						rtpMap = "11 l16/44100/1";
					} else {
						payload = 10;
						rtpMap = "10 l16/44100/2";
					}
				}
			}
		} else if (filePath.endsWith(Extension.GSM)) {
			payload = 3;
			rtpMap = "3 gsm/8000";
		}

		sb.append("m=audio 0 RTP/AVP ").append(payload).append("\n");
		sb.append("a=rtpmap:").append(rtpMap).append("\n");

		return sb.toString();

	}

}
