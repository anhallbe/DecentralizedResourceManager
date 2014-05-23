package resourcemanager.system.peer.rm;

import java.util.List;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * User: jdowling
 */
public class RequestResources  {

    public static class Request extends Message {

        private final int numCpus;
        private final int amountMemInMb;
        private final int ms;
        private final long id;
    
        public Request(Address source, Address destination, int numCpus, int amountMemInMb, int ms, long id) {
            super(source, destination);
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.ms = ms;
            this.id = id;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
        }

        public int getTimeMS() {
            return ms;
        }

        public long getId() {
            return id;
        }
    }
    
    public static class Response extends Message {

        private final boolean success;
        private final long id;
        
        public Response(Address source, Address destination, boolean success, long id) {
            super(source, destination);
            this.success = success;
            this.id = id;
        }
        
        public boolean getSuccess() {
            return success;
        }

        public long getId() {
            return id;
        }
    }
    
    public static class RequestTimeout extends Timeout {
        private final Address destination;
        RequestTimeout(ScheduleTimeout st, Address destination) {
            super(st);
            this.destination = destination;
        }

        public Address getDestination() {
            return destination;
        }
    }
}
