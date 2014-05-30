package tman.system.peer.tman;

import common.peer.AvailableResources;
import java.io.Serializable;
import se.sics.kompics.address.Address;

/**
 *
 * @author Andreas
 */
public class PeerDescriptorTMan implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Address address;
    private final AvailableResources availableResources;

    public PeerDescriptorTMan(Address address, AvailableResources availableResources) {
        this.address = address;
        this.availableResources = availableResources;
    }

    public Address getAddress() {
        return address;
    }
    
    public AvailableResources getAvailableResources() {
        return availableResources;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (this.address != null ? this.address.hashCode() : 0);
        return hash;
    }
}
