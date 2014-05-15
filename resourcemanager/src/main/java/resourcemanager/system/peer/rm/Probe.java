/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package resourcemanager.system.peer.rm;

import common.peer.AvailableResources;
import java.io.Serializable;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author Andreas
 */
public class Probe {
    public static class Request extends Message implements Serializable {
        private static final long serialVersionUID = 123;
        private int id;
        
        public Request(Address source, Address destination, int id) {
            super(source, destination);
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
    }
    
    public static class Response extends Message implements Serializable {
        private static final long serialVersionUID = 321;
        private int id, cpu, mem;   //mem in MB
        
        public Response(Address source, Address destination, int id, int cpu, int mem) {
            super(source, destination);
            this.id = id;
            this.cpu = cpu;
            this.mem = mem;
        }
        
        public int getId() {
            return id;
        }
        
        public int getCPU() {
            return cpu;
        }
        
        public int getMem() {
            return mem;
        }
    }
}
