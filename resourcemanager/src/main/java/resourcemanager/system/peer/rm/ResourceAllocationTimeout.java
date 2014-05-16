/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 *
 * @author Andreas
 */
public class ResourceAllocationTimeout extends Timeout {
    private final int allocatedCPU;
    private final int allocatedMem;
    
    public ResourceAllocationTimeout(ScheduleTimeout request, int allocatedCPU, int allocatedMem) {
        super(request);
        this.allocatedCPU = allocatedCPU;
        this.allocatedMem = allocatedMem;
    }

    public int getAllocatedCPU() {
        return allocatedCPU;
    }

    public int getAllocatedMem() {
        return allocatedMem;
    }
}
