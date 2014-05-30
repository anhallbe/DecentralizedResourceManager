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
 */
public class Probe {
    /**
     * When a probe request is sent from A to B, A wants to know the current workload of B (i.e length of the work queue).
     */
    public static class Request extends Message implements Serializable {
        private static final long serialVersionUID = 6778950069751417666L;
        private long id;
        
        public Request(Address source, Address destination, long id) {
            super(source, destination);
            this.id = id;
        }
        
        public long getId() {
            return id;
        }
    }
    
    /**
     * A response should contain the current length of the responder's work queue.
     */
    public static class Response extends Message implements Serializable, Comparable<Response> {
        private static final long serialVersionUID = 6566343625721530642L;
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

        @Override
        public int compareTo(Response o) {
            int myQueue = getQueue();
            int otherQueue = o.getQueue();
            
            if(myQueue < otherQueue)
                return -1;
            else if(myQueue > otherQueue)
                return 1;
            else
                return 0;
        }
    }
}
