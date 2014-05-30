package tman.system.peer.tman;

import java.util.Comparator;

/**
 * This Comparator is used to sort nodes in order to build a gradient network.
 * In this case, we only consider the number of free cpus.
 * @author Andreas
 */
public class ComparatorByCPU implements Comparator<PeerDescriptorTMan>{
    
    PeerDescriptorTMan myDescriptor;
    
    public ComparatorByCPU(PeerDescriptorTMan myDescriptor) {
        this.myDescriptor = myDescriptor;
    }
    
    @Override
    // In returned list after ordering, preferable peer should be placed to the head,
    // If base node prefers o1 rather than o2, o1 should be placed before o2, i.e. o1 < o2.
    public int compare(PeerDescriptorTMan o1, PeerDescriptorTMan o2) {
        int utility1, utility2, myUtility;
        
        if (o1.getAvailableResources() != null) {
            utility1 = o1.getAvailableResources().getNumFreeCpus();
        } else {
            utility1 = -1;
        }
        if (o2.getAvailableResources() != null) {
            utility2 = o2.getAvailableResources().getNumFreeCpus();
        } else {
            utility2 = -1;
        }
        if (myDescriptor.getAvailableResources() != null) {
            myUtility = myDescriptor.getAvailableResources().getNumFreeCpus();
        } else {
            myUtility = -1;
        }
        
        // Use gradient to order subset.
        // pick o1 return -1, pick o2 return 1;
        if (utility1 == utility2) {
            return 0;
        }
        else if (utility1 > myUtility && utility2 > myUtility) {
            if (utility1 > utility2) {
                return 1;
            }
            else {
                return -1;
            }
        }
        else {
            if (utility1 > utility2) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }
    
    
}
