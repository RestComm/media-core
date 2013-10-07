package org.mobicents.media.server.impl.rtp.crypto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.bouncycastle.crypto.tls.DatagramTransport;


/**
 * 
 * An adaptor class intended to be used as a disposable, thread-unsafe object
 * that bridges a raw RTP packet to a DTLSTransport handler.  
 * 
 * 
 * @author ivelin.ivanov@telestax.com
 *
 */
public class DatagramTransportAdaptor implements DatagramTransport
    {

        private final static int MIN_IP_OVERHEAD = 20;
        private final static int MAX_IP_OVERHEAD = MIN_IP_OVERHEAD + 64;
        private final static int UDP_OVERHEAD = 8;

        private int receiveLimit, sendLimit;
        
        // a buffer intended for one time read.
        private ByteBuffer inputBuffer;

        // a buffer intended for one time write.
        private ByteBuffer outputBuffer;

        /**
         * 
         * @param newBuffer this is a single use, disposable buffer for reading data from an external source
         */
        public void setInputBuffer (ByteBuffer newBuffer) {
        	inputBuffer = newBuffer;
            this.receiveLimit = inputBuffer.capacity() - MIN_IP_OVERHEAD - UDP_OVERHEAD;
            // prepare to read buffer from the beginning
            inputBuffer.flip();
        }
        
        /**
         * 
         * @param newBuffer this is a single use, disposable buffer for writing data to an external source
         */
        public void setOutputBuffer (ByteBuffer newBuffer) {
        	outputBuffer = newBuffer;
            this.sendLimit = outputBuffer.capacity() - MAX_IP_OVERHEAD - UDP_OVERHEAD;
            // prepare to read buffer from the beginning
            outputBuffer.clear();
        }
        
        public int getReceiveLimit()
        {
            return receiveLimit;
        }

        public int getSendLimit()
        {
            // TODO[DTLS] Implement Path-MTU discovery?
            return sendLimit;
        }

        public int receive(byte[] buf, int off, int len, int waitMillis)
            throws IOException
        {
        	int remaining = inputBuffer.remaining();
        	if (len < remaining) { throw new BufferOverflowException(); };
        	inputBuffer.get(buf, off, remaining);
            return remaining;
        }

        public void send(byte[] buf, int off, int len)
            throws IOException
        {
            if (len > getSendLimit())
            {
                /*
                 * RFC 4347 4.1.1. "If the application attempts to send a record larger than the MTU,
                 * the DTLS implementation SHOULD generate an error, thus avoiding sending a packet
                 * which will be fragmented."
                 */
                throw new BufferOverflowException();
            }

            outputBuffer.put(buf, off, len);
        }

        public void close()
            throws IOException
        {
        	// Nothing to do here. This method is more useful for external socket or channel read/writes
        }
}
