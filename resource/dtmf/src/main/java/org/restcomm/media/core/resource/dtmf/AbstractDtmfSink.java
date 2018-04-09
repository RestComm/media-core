package org.restcomm.media.core.resource.dtmf;

import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.component.AbstractSink;
import org.restcomm.media.core.spi.memory.Frame;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract DTMF Sink that feeds audio frames to a DTMF Detector.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com) created on 09/04/2018
 */
public abstract class AbstractDtmfSink extends AbstractSink implements DtmfEventObserver, DtmfEventSubject {

    private final DtmfDetector detector;

    private final Set<DtmfEventObserver> observers;

    public AbstractDtmfSink(String name, DtmfDetector detector) {
        super(name);
        this.detector = detector;
        this.observers = ConcurrentHashMap.newKeySet(3);

        this.detector.observe(this);
    }

    @Override
    public void onMediaTransfer(Frame frame) {
        this.detector.detect(frame.getData(), frame.getDuration() / 1000000);
    }

    @Override
    public void onDtmfEvent(DtmfEvent event) {
        // Propagate event to registered observers
        notify(event);
    }

    @Override
    public void observe(DtmfEventObserver observer) {
        final boolean added = this.observers.add(observer);
        if (added && getLogger().isDebugEnabled()) {
            getLogger().debug("Registered observer DtmfEventObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void forget(DtmfEventObserver observer) {
        final boolean removed = this.observers.remove(observer);
        if (removed && getLogger().isDebugEnabled()) {
            getLogger().debug("Unregistered observer DtmfEventObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void notify(DtmfEvent event) {
        for (DtmfEventObserver observer : this.observers) {
            if (observer != this) {
                observer.onDtmfEvent(event);
            }
        }
    }

    protected abstract Logger getLogger();

}
