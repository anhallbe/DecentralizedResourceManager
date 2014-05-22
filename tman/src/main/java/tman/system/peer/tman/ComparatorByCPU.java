/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tman.system.peer.tman;

import common.peer.PeerDescriptor;
import java.util.Comparator;

/**
 *
 * @author Zhao Zhengyang
 */
public class ComparatorByCPU implements Comparator<PeerDescriptor>{
    
    PeerDescriptor myDescriptor;
    
    public ComparatorByCPU(PeerDescriptor myDescriptor) {
        this.myDescriptor = myDescriptor;
    }
    
     @Override
    public int compare(PeerDescriptor o1, PeerDescriptor o2) {
        assert (o1.getNumFreeCpus() == o2.getNumFreeCpus());
        if (o1.getNumFreeCpus() < myDescriptor.getNumFreeCpus() && o2.getNumFreeCpus() > myDescriptor.getNumFreeCpus()) {
            return 1;
        } else if (o2.getNumFreeCpus() < myDescriptor.getNumFreeCpus() && o1.getNumFreeCpus() > myDescriptor.getNumFreeCpus()) {
            return -1;
        } else if (Math.abs(o1.getNumFreeCpus() - myDescriptor.getNumFreeCpus()) < Math.abs(o2.getNumFreeCpus() - myDescriptor.getNumFreeCpus())) {
            return -1;
        }
        return 1;
    }
    
}
