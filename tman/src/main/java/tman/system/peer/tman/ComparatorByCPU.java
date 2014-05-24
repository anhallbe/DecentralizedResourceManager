/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tman.system.peer.tman;

import common.peer.PeerDescriptor;
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
    public int compare(PeerDescriptorTMan o1, PeerDescriptorTMan o2) {
        int utility1 = o1.getAvailableResources().getNumFreeCpus();
        int utility2 = o2.getAvailableResources().getNumFreeCpus();
        int myUtility = myDescriptor.getAvailableResources().getNumFreeCpus();
        
        if (utility1 > myUtility && utility2 < myUtility) {
            return 1;
        }
        else if (utility1 < myUtility && utility2 > myUtility) {
            return -1;
        }
        else if (Math.abs(utility1 - myUtility) < Math.abs(utility2 - myUtility)) {
            return 1;
        }
        else if (utility1 == utility2) {
            return 0;
        }
        
        return 1;
    }
    
    
}
