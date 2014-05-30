package tman.system.peer.tman;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class ExchangeMsg {

    /**
     * Shuffle request.
     */
    public static class Request extends Message {

        private static final long serialVersionUID = 8493601671018888143L;
        private final DescriptorBufferTMan requestBuffer;


        public Request(DescriptorBufferTMan requestBuffer, Address source, Address destination) {
            super(source, destination);
            this.requestBuffer = requestBuffer;
        }

        public DescriptorBufferTMan getRequestBuffer() {
            return requestBuffer;
        }
    }

    /**
     * Shuffle response
     */
    public static class Response extends Message {

        private static final long serialVersionUID = -5022051054665787770L;
        private final DescriptorBufferTMan responseBuffer;


        public Response(DescriptorBufferTMan responseBuffer, Address source, Address destination) {
            super(source, destination);
            this.responseBuffer = responseBuffer;
        }

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