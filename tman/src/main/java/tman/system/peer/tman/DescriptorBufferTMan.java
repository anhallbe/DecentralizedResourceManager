package tman.system.peer.tman;

import java.io.Serializable;
import java.util.List;

/**
 * This buffer will be sent between TMan-nodes during shuffling.
 * @author Andreas
 */
public class DescriptorBufferTMan implements Serializable {
    private final long serialVersionUID = 1L;
    private final PeerDescriptorTMan source;
    private final List<PeerDescriptorTMan> descriptors;

    public DescriptorBufferTMan(PeerDescriptorTMan from, List<PeerDescriptorTMan> descriptors) {
        this.source = from;
        this.descriptors = descriptors;
    }

    public List<PeerDescriptorTMan> getDescriptors() {
        return descriptors;
    }

    public PeerDescriptorTMan getFrom() {
        return source;
    }
}
