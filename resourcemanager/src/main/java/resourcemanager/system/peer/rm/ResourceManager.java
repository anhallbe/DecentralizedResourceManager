package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * Should have some comments here.
 *
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    Positive<RmPort> indexPort = positive(RmPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<Web> webPort = negative(Web.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);
    ArrayList<Address> neighbours = new ArrayList<Address>();
    private Address self;
    private RmConfiguration configuration;
    Random random;
    private AvailableResources availableResources;
    // When you partition the index you need to find new nodes
    // This is a routing table maintaining a list of pairs in each partition.
    private Map<Integer, List<PeerDescriptor>> routingTable;
    
    private final int NPROBES = 2;
    private List<Probe.Response> probeResponses;
    private Queue<Task> workQueue;      //This queue contains tasks that need to be performed on this node.
    private int MAX_CPU;
    private int MAX_MEM;
    private Queue<Task> pendingJobs;    //This queue is for tasks that have not been scheduled yet.
    
    Comparator<PeerDescriptor> peerAgeComparator = new Comparator<PeerDescriptor>() {
        @Override
        public int compare(PeerDescriptor t, PeerDescriptor t1) {
            if (t.getAge() > t1.getAge()) {
                return 1;
            } else {
                return -1;
            }
        }
    };

	
    public ResourceManager() {

        subscribe(handleInit, control);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleRequestResource, indexPort);
        subscribe(handleUpdateTimeout, timerPort);
        subscribe(handleResourceAllocationRequest, networkPort);
        subscribe(handleResourceAllocationResponse, networkPort);
        subscribe(handleTManSample, tmanPort);
        
        subscribe(handleProbeRequest, networkPort);
        subscribe(handleProbeResponse, networkPort);
        subscribe(handleResourceAllocationTimeout, timerPort);
    }
	
    Handler<RmInit> handleInit = new Handler<RmInit>() {
        @Override
        public void handle(RmInit init) {
            self = init.getSelf();
            configuration = init.getConfiguration();
            routingTable = new HashMap<Integer, List<PeerDescriptor>>(configuration.getNumPartitions());
            random = new Random(init.getConfiguration().getSeed());
            availableResources = init.getAvailableResources();
            long period = configuration.getPeriod();
            availableResources = init.getAvailableResources();
            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new UpdateTimeout(rst));
            trigger(rst, timerPort);

            probeResponses = new ArrayList<Probe.Response>();
            workQueue = new LinkedList<Task>();
            MAX_CPU = availableResources.getNumFreeCpus();
            MAX_MEM = availableResources.getFreeMemInMbs();
            pendingJobs = new LinkedList<Task>();
        }
    };


    Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            // pick a random neighbour to ask for index updates from. 
            // You can change this policy if you want to.
            // Maybe a gradient neighbour who is closer to the leader?
            if (neighbours.isEmpty()) {
                return;
            }
            Address dest = neighbours.get(random.nextInt(neighbours.size()));


        }
    };

    /**
     * This event is received when a task is done and resources need to be released.
     */
    Handler<ResourceAllocationTimeout> handleResourceAllocationTimeout = new Handler<ResourceAllocationTimeout>() {
        @Override
        public void handle(ResourceAllocationTimeout e) {
            int cpu = e.getAllocatedCPU();
            int mem = e.getAllocatedMem();
            availableResources.release(cpu, mem);
            System.out.println("Released " + cpu + " CPU and " + mem + " MB memory.");
            
            //Take a new task from queue, try to allocate resources for it.
            Task nextTask = workQueue.peek();
            if(nextTask != null && availableResources.isAvailable(nextTask.getCpus(), nextTask.getMem())) {
                workQueue.remove();
                availableResources.allocate(nextTask.getCpus(), nextTask.getMem());
                ScheduleTimeout t = new ScheduleTimeout(nextTask.getMilliseconds());
                t.setTimeoutEvent(new ResourceAllocationTimeout(t, nextTask.getCpus(), nextTask.getMem()));
                trigger(t, timerPort);
                System.out.println("Polled a task from queue and allocating resources.");
            } else
                System.out.println("NOTHING IN QUEUE");
        }
    };

    Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {
            // TODO 
            //This is where resources should be allocated, also add some ID, send response (success/fail)
            int cpu = event.getNumCpus();
            int mem = event.getAmountMemInMb();
            int timeMS = event.getTimeMS();
            
            if(cpu > MAX_CPU || mem > MAX_MEM) {
                //TODO Abort and respond FAILURE    //Fixed
                System.out.println("RESOURCES NOT AVAILABLE");
                trigger(new RequestResources.Response(self, event.getSource(), false), networkPort);
                return;
            }
            
            if(availableResources.isAvailable(cpu, mem)) {
                availableResources.allocate(cpu, mem);
                //TODO Add a timer event to notify us when the time has run out, and resources should be released.
                //Fixed
                System.out.println("Allocated " + cpu + " CPUs and " + mem + " MB memory.");
                ScheduleTimeout t = new ScheduleTimeout(timeMS);
                t.setTimeoutEvent(new ResourceAllocationTimeout(t, cpu, mem));
                trigger(t, timerPort);
            } else {
                System.out.println("Not enough resources, adding to queue.");
                workQueue.add(new Task(cpu, mem, timeMS));
            }
            
            //TODO Should not be responding here, wait for the job to be finished instead?
            //Or wait for the resources to be allocated... To measure performance
            trigger(new RequestResources.Response(self, event.getSource(), true), networkPort);
        }
    };
    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {
            // TODO 
            // Received response from some peer. How to handle fail/success?
            boolean success = event.getSuccess();
            if(success)
                System.out.println("Response: SUCCESS");
            else
                System.out.println("Response: FAILURE.....Now what?");
        }
    };
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            System.out.println("Received samples: " + event.getSample().size());
            
            // receive a new list of neighbours
            neighbours.clear();
            neighbours.addAll(event.getSample());

            // update routing tables
            for (Address p : neighbours) {
                int partition = p.getId() % configuration.getNumPartitions();
                List<PeerDescriptor> nodes = routingTable.get(partition);
                if (nodes == null) {
                    nodes = new ArrayList<PeerDescriptor>();
                    routingTable.put(partition, nodes);
                }
                // Note - this might replace an existing entry in Lucene
                nodes.add(new PeerDescriptor(p));
                // keep the freshest descriptors in this partition
                Collections.sort(nodes, peerAgeComparator);
                List<PeerDescriptor> nodesToRemove = new ArrayList<PeerDescriptor>();
                for (int i = nodes.size(); i > configuration.getMaxNumRoutingEntries(); i--) {
                    nodesToRemove.add(nodes.get(i - 1));
                }
                nodes.removeAll(nodesToRemove);
            }
        }
    };
	
    /**
     * Request sent from higher layer (client, not another resource manager)
     */
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {
            int rCpu = event.getNumCpus();
            int rMem = event.getMemoryInMbs();
            int rTime = event.getTimeToHoldResource();
//            int myCpu = availableResources.getNumFreeCpus();
//            int myMem = availableResources.getFreeMemInMbs();
            
            //If the requested resources are available on this machine, there's no need to ask other peers. Just allocate them here!
            //Rev: On second thought, maybe it will be better for load balance to distribute work anyway...
//            if(rCpu <= myCpu && rMem <= myMem) {
//                System.out.println("The requested resources are available on this node, don't bother with asking other peers.");
//                trigger(new RequestResources.Request(self, self, rCpu, rMem, rTime), networkPort);
//            } else {
                System.out.println("Allocate resources: " + event.getNumCpus() + " + " + event.getMemoryInMbs() + " + " + event.getTimeToHoldResource());
                if(neighbours.size() > 0) {
                    pendingJobs.add(new Task(rCpu, rMem, rTime));
                    //Send probes to NPROBES neighbours
                    UUID pid = new UUID(random.nextLong(), random.nextLong());
                    for(int i=0; i<NPROBES; i++) {
                        Address n1 = neighbours.get(random.nextInt(neighbours.size()));
                        Probe.Request r1 = new Probe.Request(self, n1, pid);
                        trigger(r1, networkPort);
                    }
                    System.out.println("------Probes sent");
                }
//            }
            
        }
    };
    
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            // TODO: 
        }
    };

    Handler<Probe.Request> handleProbeRequest = new Handler<Probe.Request>() {
        @Override
        public void handle(Probe.Request e) {
            Probe.Response response = new Probe.Response(self, e.getSource(), e.getId(), workQueue.size());
            trigger(response, networkPort);
        }
    };
    
    Handler<Probe.Response> handleProbeResponse = new Handler<Probe.Response>() {
        @Override
        public void handle(Probe.Response e) {
            System.out.println("Probe Response");
            UUID id = e.getId();
            probeResponses.add(e);
            if(nProbeResponses(id) == NPROBES) {
                // Send requests.
                System.out.println("Received " + NPROBES + " Probe responses");
                System.out.println("Best response queue length: " + bestResponse(probeResponses, id).getQueue());
                Task job = pendingJobs.poll();
                trigger(new RequestResources.Request(self, e.getSource(), job.getCpus(), job.getMem(), job.getMilliseconds()), networkPort);     //TODO Add real cpu, mem, time
            }
            //Else not all probes have returned.
        }
    };
    
    /**
     * Count the number of probe responses with a given ID
     * @param id
     * @return number of responses with id == id
     */
    private int nProbeResponses(UUID id) {
        int n = 0;
        System.out.println("Probe Request");
        for(Probe.Response res : probeResponses) {
            if(res.getId().equals(id))
                n++;
        }
        return n;
    }
    
    /**
     * Go through the list of responses, and the probe with lowest queue length with ID = id
     */
    private Probe.Response bestResponse(List<Probe.Response> responses, UUID id) {
        Probe.Response best = null;
        for(Probe.Response res : responses) {
            if(res.getId().equals(id) && (best == null || res.getQueue() < best.getQueue()))
                best = res;
        }
        return best;
    }
}