/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tman.system.peer.tman;

import java.util.Comparator;

/**
 *  TODO: Change to PeerDesctiptorTMan
 * @author Zhao Zhengyang
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
        
//        // Use gradient to order subset.
//        // order o1 before o2, set o1 < o2.
//        if (utility1 > myUtility && utility2 < myUtility) {
//            return -1;
//        }
//        // order o2 before o1, set o1 > o2.
//        else if (utility1 < myUtility && utility2 > myUtility) {
//            return 1;
//        }
//        // order o1 before o2, set o1 < o2.
//        else if (Math.abs(utility1 - myUtility) < Math.abs(utility2 - myUtility)) {
//            return -1;
//        }
//        else if (utility1 == utility2) {
//            return 0;
//        }
        
        // Simply order subset according to utility decreasing. 
        if (utility1 < utility2) {
            return 1;
        }
        else if (utility1 > utility2) {
            return -1;
        }
        else if (utility1 == utility2) {
            return 0;
        }
        
        return 1;
    }
    
    
}
