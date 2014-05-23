/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tman.system.peer.tman;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.address.Address;

/**
 *
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
