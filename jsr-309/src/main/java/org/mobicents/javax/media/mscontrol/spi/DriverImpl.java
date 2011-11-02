package org.mobicents.javax.media.mscontrol.spi;

import jain.protocol.ip.mgcp.DeleteProviderException;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.JainMgcpStack;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.NotifyResponse;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.media.mscontrol.spi.PropertyInfo;

import org.apache.log4j.Logger;
import org.mobicents.javax.media.mscontrol.MsControlFactoryImpl;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

/**
 * 
 * @author amit bhayani
 *
 */
public class DriverImpl implements Driver, JainMgcpListener {

    public static final String DRIVER_NAME = "org.mobicents.Driver_1.0";

    private JainMgcpStack mgcpStack;
    private JainMgcpProvider mgcpProvider;
    

    private NotifiedEntity callAgent;
    
    private String localHost;
    private String remoteHost;
    
    private int localPort;
    private int remotePort;
    
    private ScheduledExecutorService scheduler;
    
    private MsControlFactoryImpl factory;
    private Logger logger = Logger.getLogger(DriverImpl.class);
    
    private int txID;
    
    private ConcurrentHashMap<Integer, JainMgcpListener> txListeners = new ConcurrentHashMap();
    private ConcurrentHashMap<String, JainMgcpListener> requestListeners = new ConcurrentHashMap();
    
    static {
        DriverManager.registerDriver(new DriverImpl());
        //logger.info("Driver " + DRIVER_NAME + " registered with DriverManager");
    }

    public DriverImpl() {
    	
    }

    public MsControlFactory getFactory(Properties config) throws MsControlException {
        //if factory already created just return it
        if (factory != null) {
            return factory;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        if (config == null) {
            config = new Properties();
            config.setProperty("mgcp.bind.address", "127.0.0.1");
            config.setProperty("mgcp.server.address", "127.0.0.1");
            config.setProperty("mgcp.local.port", "2729");
            config.setProperty("mgcp.server.port", "2427");
        }
        
        //get the bind address and port
        localHost = config.getProperty("mgcp.bind.address");
        remoteHost = config.getProperty("mgcp.server.address");
        
        localPort = Integer.parseInt(config.getProperty("mgcp.local.port"));
        remotePort = Integer.parseInt(config.getProperty("mgcp.server.port"));
        
        InetAddress bindAddress = null;
        try {
            bindAddress = InetAddress.getByName(localHost);
        } catch (UnknownHostException e) {
        }
        
        boolean testMode = config.getProperty("driver.test.mode") != null &&
                Boolean.parseBoolean(config.getProperty("driver.test.mode"));
        
        if (!testMode) {
            logger.info("Initializing MGCP on " + bindAddress + ":" + localPort);

            //create and bind MGCP stack
            try {
                mgcpStack = new JainMgcpStackImpl(bindAddress, localPort);
                mgcpProvider = mgcpStack.createProvider();
                
                //assign itself as listener
                mgcpProvider.addJainMgcpListener(this);
            } catch (Exception e) {
                throw new MsControlException(e.getMessage());
            }
        }
        
        callAgent = new NotifiedEntity("mscontrol", localHost, localPort);

        factory = new MsControlFactoryImpl(this);
        return factory;
    }

    public synchronized int getNextTxID() {
        return ++txID;
    }
    
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
    
    public NotifiedEntity getCallAgent() {
        return callAgent;
    }
    
    public String getRemoteDomainName() {
        return remoteHost + ":" + remotePort;
    }
    
    public void send(JainMgcpEvent evt) {
        this.mgcpProvider.sendMgcpEvents(new JainMgcpEvent[]{evt});
    }
    
    /**
     * Attaches listener to the specific request.
     * 
     * @param reqID the request identifier.
     * @param listener listener to be attached.
     */
    public void attach(RequestIdentifier reqID, JainMgcpListener listener) {
        this.requestListeners.put(reqID.toString().trim(), listener);
    }
    
    /**
     * Deattaches listeners related to specific request.
     * 
     * @param reqID the request identifier.
     */
    public void deattach(RequestIdentifier reqID) {
        this.requestListeners.remove(reqID.toString().trim());
    }

    /**
     * Attaches listener to the specific transaction.
     * 
     * Listener is automaticaly deattaching when transaction is completed.
     * 
     * @param txID the identifier of the transaction.
     * @param listener the listener to attach.
     */
    public void attach(int txID, JainMgcpListener listener) {
        this.txListeners.put(txID, listener);
    }
    
    /**
     * Deattaches transaction listener upon user request.
     * 
     * @param listener the listener to deattach.
     */
    public void deattach(JainMgcpListener listener) {
        int identifier = -1;
        
        //search identifier of the specified listener 
        Set<Integer> IDs = txListeners.keySet();        
        for (Integer id : IDs) {
            if (txListeners.get(id) == listener) {
                identifier = id;
                break;
            }
        }
        
        //remove it from list if found
        if (identifier != -1) {
            txListeners.remove(identifier);
        }
    }
    
    
    public PropertyInfo[] getFactoryPropertyInfo() {
        return null;
    }

    public String getName() {
        return DRIVER_NAME;
    }
    
	public void shutdown() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}

		scheduler = null;
		if (mgcpProvider != null) {
			try {
				mgcpProvider.getJainMgcpStack().deleteProvider(mgcpProvider);
			} catch (DeleteProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mgcpProvider = null;
			mgcpStack = null;
		}
		factory = null;
	}

    public void processMgcpCommandEvent(JainMgcpCommandEvent evt) {
        //we are expecting two types of commands:
        //-delete connection notification (in case of failure connection on server side)
        //-notify
        switch (evt.getObjectIdentifier()) {
            case Constants.CMD_DELETE_CONNECTION :
                //TODO: handle delete connection request from server;
                break;
            case Constants.CMD_NOTIFY :  
                Notify event = (Notify) evt;
                
                //if there is attached handler deligate call to it
                if (this.requestListeners.containsKey(event.getRequestIdentifier().toString().trim())) {
                    requestListeners.get(event.getRequestIdentifier().toString().trim()).processMgcpCommandEvent(evt);
                }
                
                //send response to this transaction;
                NotifyResponse response = new NotifyResponse(this, ReturnCode.Transaction_Executed_Normally);
                response.setTransactionHandle(evt.getTransactionHandle());
                
                this.send(response);
                break;
            default :
        }
    }

    public void processMgcpResponseEvent(JainMgcpResponseEvent event) {
        JainMgcpListener handler = null;
        
        //we are considering transaction completed if any response received except provisional
        //listener related to completed transaction must be derigistered 
        if (event.getReturnCode() != ReturnCode.Transaction_Being_Executed) {
            handler = txListeners.remove(event.getTransactionHandle());
        } else {
            handler = txListeners.get(event.getTransactionHandle());                    
        }
        
        //unknown transaction?
        if (handler == null) {
            return;
        }
        
        //deliver event to tx handler
        handler.processMgcpResponseEvent(event);
    }
    
    public void info(String s) {
        logger.info(s);
    }
    
    public void debug(String s) {
        logger.debug(s);
    }
    
    public void warn(String s) {
        logger.warn(s);
    }
    
}
