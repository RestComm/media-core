package org.restcomm.media.core.resource.dtmf.detector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.component.oob.OOBOutput;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) created on 09/04/2018
 */
public class Rfc2833DtmfSink extends AbstractDtmfSink {

    private static final Logger log = LogManager.getLogger(Rfc2833DtmfSink.class);

    private final OOBOutput output;

    public Rfc2833DtmfSink(String name, DtmfDetector detector, OOBOutput output) {
        super(name, detector);
        this.output = output;
        this.output.join(this);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public void activate() {
        this.output.start();
    }

    @Override
    public void deactivate() {
        this.output.stop();
    }

    public OOBOutput getOutput() {
        return this.output;
    }

}
