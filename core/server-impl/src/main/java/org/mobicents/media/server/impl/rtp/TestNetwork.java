/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author kulikov
 */
public class TestNetwork {

    private DatagramSocket socket1,  socket2;
    private DatagramChannel channel1,  channel2;
    private Selector selector;
    private ByteBuffer txBuffer1 = ByteBuffer.allocateDirect(160);
    private ByteBuffer rxBuffer1 = ByteBuffer.allocateDirect(160);
    private ByteBuffer txBuffer2 = ByteBuffer.allocateDirect(160);
    private ByteBuffer rxBuffer2 = ByteBuffer.allocateDirect(160);
    private long total;
    private InetSocketAddress dest;
    private InetSocketAddress address1,  address2;
    private ExecutorService pool = Executors.newFixedThreadPool(2);

    private long t = 0;
    
    public TestNetwork() throws IOException {
        selector = Selector.open();
    }

    private void join() throws IOException {
        dest = new InetSocketAddress("80.69.146.12", 8999);
        channel1 = DatagramChannel.open();
        channel1.configureBlocking(false);
        socket1 = channel1.socket();

        channel2 = DatagramChannel.open();
        channel2.configureBlocking(false);
        socket2 = channel2.socket();

        address1 = new InetSocketAddress(InetAddress.getLocalHost(), 9201);
        address2 = new InetSocketAddress(InetAddress.getLocalHost(), 9202);

        socket1.bind(address1);
        socket2.bind(address2);

//        channel1.connect(address2);
//        channel2.connect(address1);

        SelectionKey k1 = channel1.register(selector, SelectionKey.OP_READ);
        k1.attach(rxBuffer1);

        SelectionKey k2 = channel2.register(selector, SelectionKey.OP_READ);
        k2.attach(rxBuffer2);
    }

    public void run() throws IOException {
        //do send first
        txBuffer1.rewind();
        txBuffer2.rewind();

        channel1.send(txBuffer1, address2);
        channel2.send(txBuffer2, address1);

        //do read
        selector.selectNow();
        Set<SelectionKey> keys = selector.selectedKeys();
        for (SelectionKey key : keys) {
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            ((DatagramChannel) key.channel()).receive(buffer);
            total += buffer.position();
            buffer.flip();
            buffer.clear();
        }

    }

    public void perform() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            run();
        }
        long finish = System.currentTimeMillis();
        System.out.println("Transmitted " + total + " bytes within " + (finish - start) + " ms");
    }

    public void perform1() throws Exception {
        Connection[] connections = new Connection[500];
        for (int i = 0; i < connections.length; i++) {
            connections[i] = new Connection(1024 + i, 65530 - i);
            connections[i].join();
            connections[i].start();
        }

        long s = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            selector.selectNow();
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                Connection con = (Connection) key.attachment();
                con.run1((DatagramChannel) key.channel());
            }
        }
        
        Thread.currentThread().sleep(10000);
        
        //long t = System.currentTimeMillis();
        System.out.println("Time = " + (t - s));

        for (int i = 0; i < connections.length; i++) {
//            connections[i].print();
            connections[i].close();
        }
        
        pool.shutdownNow();
    }

    private void close() throws IOException {
        channel1.close();
        channel2.close();
        socket1.disconnect();
        socket2.disconnect();
        socket1.close();
        socket2.close();
        selector.close();
    }

    public static void main(String[] args) throws Exception {
        TestNetwork test = new TestNetwork();
        test.perform1();
    }

    private class Connection implements Runnable {

        private DatagramSocket socket1,  socket2;
        private DatagramChannel channel1,  channel2;
        private ByteBuffer txBuffer = ByteBuffer.allocateDirect(160);
        private ByteBuffer rxBuffer = ByteBuffer.allocateDirect(160);
        private InetSocketAddress address1,  address2;
        private int port1,  port2;
        private long total;
        private DatagramChannel channel;

        public Connection(int port1, int port2) {
            this.port1 = port1;
            this.port2 = port2;
        }

        private void join() throws IOException {
            channel1 = DatagramChannel.open();
            channel1.configureBlocking(false);
            socket1 = channel1.socket();

            channel2 = DatagramChannel.open();
            channel2.configureBlocking(false);
            socket2 = channel2.socket();

            address1 = new InetSocketAddress(InetAddress.getLocalHost(), port1);
            address2 = new InetSocketAddress(InetAddress.getLocalHost(), port2);

            socket1.bind(address1);
            socket2.bind(address2);

            channel1.connect(address2);
            channel2.connect(address1);

            SelectionKey k1 = channel1.register(selector, SelectionKey.OP_READ);
            k1.attach(this);

            SelectionKey k2 = channel2.register(selector, SelectionKey.OP_READ);
            k2.attach(this);
        }

        public void start() throws IOException {
            txBuffer.rewind();
            channel1.write(txBuffer);
        }

        public void run(DatagramChannel channel) throws IOException {
            this.channel = channel;
            txBuffer.rewind();
            channel.write(txBuffer);

            channel.read(rxBuffer);
            total += rxBuffer.position();
            rxBuffer.flip();
            rxBuffer.clear();
            
            t= System.currentTimeMillis();
        }

        public void run1(DatagramChannel channel) throws IOException {
            this.channel = channel;
            pool.execute(this);
        }
        
        public void run() {
            try {
                txBuffer.rewind();
                channel.write(txBuffer);

                channel.read(rxBuffer);
                total += rxBuffer.position();
                rxBuffer.flip();
                rxBuffer.clear();
            } catch (IOException e) {
            }
            t = System.currentTimeMillis();
        }

        public void print() {
            System.out.println("Total =" + total);
        }

        private void close() throws IOException {
            channel1.close();
            channel2.close();
            socket1.disconnect();
            socket2.disconnect();
            socket1.close();
            socket2.close();
            selector.close();
        }
    }
}
