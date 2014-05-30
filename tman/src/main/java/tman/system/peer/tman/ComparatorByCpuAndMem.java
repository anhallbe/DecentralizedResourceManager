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
public class ComparatorByCpuAndMem implements Comparator<PeerDescriptorTMan>{
    
    PeerDescriptorTMan myDescriptor;
    
    public ComparatorByCpuAndMem(PeerDescriptorTMan myDescriptor) {
        this.myDescriptor = myDescriptor;
    }
    
    @Override
    // In returned list after ordering, preferable peer should be placed to the head,
    // If base node prefers o1 rather than o2, o1 should be placed before o2, i.e. o1 < o2.
    public int compare(PeerDescriptorTMan o1, PeerDescriptorTMan o2) {
        double utility1, utility2, myUtility, myCpu, myMem, cpu1, mem1, cpu2, mem2;
        
        if (o1.getAvailableResources() != null) {
            cpu1 = (double)o1.getAvailableResources().getNumFreeCpus()/(double)o1.getAvailableResources().getMaxCPU();
            mem1 = (double)o1.getAvailableResources().getFreeMemInMbs()/(double)o1.getAvailableResources().getMaxMem();
            utility1 = mem1 * cpu1;
        } else {
            utility1 = -1;
        }
        if (o2.getAvailableResources() != null) {
            cpu2 = (double)o2.getAvailableResources().getNumFreeCpus()/(double)o2.getAvailableResources().getMaxCPU();
            mem2 = (double)o2.getAvailableResources().getFreeMemInMbs()/(double)o2.getAvailableResources().getMaxMem();
            utility2 = mem2 * cpu2;
        } else {
            utility2 = -1;
        }
        if (myDescriptor.getAvailableResources() != null) {
            myCpu = (double)myDescriptor.getAvailableResources().getNumFreeCpus()/(double)myDescriptor.getAvailableResources().getMaxCPU();
            myMem = (double)myDescriptor.getAvailableResources().getFreeMemInMbs()/(double)myDescriptor.getAvailableResources().getMaxMem();
            myUtility = myMem * myCpu;
        } else {
            myUtility = -1;
        }
        
        // Use gradient to order subset.
        // pick o1 return -1, pick o2 return 1;
//        System.out.println("u1: " + utility1 + ", u2: " + utility2);
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
