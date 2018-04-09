package org.restcomm.media.core.resource.dtmf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.component.audio.AudioOutput;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) created on 09/04/2018
 */
public class InbandDtmfSink extends AbstractDtmfSink {

    private static final Logger log = LogManager.getLogger(InbandDtmfSink.class);

    private final AudioOutput output;

    public InbandDtmfSink(String name, DtmfDetector detector, AudioOutput output) {
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

    public AudioOutput getOutput() {
        return this.output;
    }

}
