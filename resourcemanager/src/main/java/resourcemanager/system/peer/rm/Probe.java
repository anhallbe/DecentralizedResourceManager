/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package resourcemanager.system.peer.rm;

import java.io.Serializable;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author Andreas
 * TODO Use queue size instead of resource availability.
 */
public class Probe {
    public static class Request extends Message implements Serializable {
        private static final long serialVersionUID = 6778950069751417666L;
        //private static final long serialVersionUID = 123;
        private long id;
        
        public Request(Address source, Address destination, long id) {
            super(source, destination);
            this.id = id;
        }
        
        public long getId() {
            return id;
        }
    }
    
    public static class Response extends Message implements Serializable {
        private static final long serialVersionUID = 6566343625721530642L;
       // private static final long serialVersionUID = 321;
        private long id;
        private int queueLength;
        
        public Response(Address source, Address destination, long id, int queueLength) {
            super(source, destination);
            this.id = id;
            this.queueLength = queueLength;
        }
        
        public long getId() {
            return id;
        }
        
        public int getQueue() {
            return queueLength;
        }
    }
}
