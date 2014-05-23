package tman.system.peer.tman;

import java.util.UUID;

//import cyclon.system.peer.cyclon.DescriptorBuffer;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class ExchangeMsg {

    public static class Request extends Message {

        private static final long serialVersionUID = 8493601671018888143L;
//        private final UUID requestId;
        private final DescriptorBufferTMan requestBuffer;


        public Request(DescriptorBufferTMan requestBuffer, Address source, Address destination) {
            super(source, destination);
//            this.requestId = requestId;
            this.requestBuffer = requestBuffer;
        }


//        public UUID getRequestId() {
//            return requestId;
//        }

        
        public DescriptorBufferTMan getRequestBuffer() {
            return requestBuffer;
        }
    }

    public static class Response extends Message {

        private static final long serialVersionUID = -5022051054665787770L;
//        private final UUID requestId;
        private final DescriptorBufferTMan responseBuffer;


        public Response(DescriptorBufferTMan responseBuffer, Address source, Address destination) {
            super(source, destination);
//            this.requestId = requestId;
            this.responseBuffer = responseBuffer;
        }


//        public UUID getRequestId() {
//            return requestId;
//        }


        public DescriptorBufferTMan getResponseBuffer() {
            return responseBuffer;
        }


        public int getSize() {
            return 0;
        }
    }

    public static class RequestTimeout extends Timeout {

        private final Address peer;


        public RequestTimeout(ScheduleTimeout request, Address peer) {
            super(request);
            this.peer = peer;
        }


        public Address getPeer() {
            return peer;
        }
    }
}