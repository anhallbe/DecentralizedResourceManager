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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import simulator.snapshot.FileIO;
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
    
    private final int NPROBES = 2;
    
    private List<Probe.Response> probeResponses;    //Keeps all probe responses
    private Queue<Task> pendingTasks;               //This queue contains tasks that need to be performed on this node.
    private int MAX_CPU;                            //Maximal CPU capacity
    private int MAX_MEM;                            //Maximal Mem capacity
    private Map<Long, RequestResource> pendingRequests; //Contains requests that haven't been sent yet.
	
    public ResourceManager() {

        subscribe(handleInit, control);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleRequestResource, indexPort);
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
            random = new Random(init.getConfiguration().getSeed());
            availableResources = init.getAvailableResources();
            availableResources = init.getAvailableResources();
            probeResponses = new ArrayList<Probe.Response>();
            pendingTasks = new LinkedList<Task>();
            MAX_CPU = availableResources.getNumFreeCpus();
            MAX_MEM = availableResources.getFreeMemInMbs();
            
            pendingRequests = new LinkedHashMap<Long, RequestResource>();
        }
    };
    
    /**
     * Request sent from higher layer (client, not another resource manager).
     * Save the request to "pending" list, and start to send probes to neighbors.
     */
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {
            System.out.println("Request with id " + event.getId() + " received.");
            logger.info("start\t" + event.getId());
            
            // Task start time
            FileIO.append(System.currentTimeMillis() + "\tstart\t" + event.getId() + "\n", "asd.log");

            int rCpu = event.getNumCpus();
            int rMem = event.getMemoryInMbs();
            int rTime = event.getTimeToHoldResource();
            long jobID = event.getId();
            int machines = event.getMachines();  //event.getMachines()

            if(neighbours.size() > 0) {
                pendingRequests.put(jobID, event);
                for(int j=0; j<NPROBES*machines; j++) {
                    Address n1 = neighbours.get(random.nextInt(neighbours.size()));
                    Probe.Request r1 = new Probe.Request(self, n1, jobID);
                    trigger(r1, networkPort);
                }
            } else {
                System.out.println("No neighbours, sending request to self...");
                trigger(new RequestResources.Request(self, self, rCpu, rMem, rTime, jobID), networkPort);
            }
        }
    };
    
    /**
     * When a probe request is received, simply send a response with information
     * about the load of this node (size of pendingTasks).
     */
    Handler<Probe.Request> handleProbeRequest = new Handler<Probe.Request>() {
        @Override
        public void handle(Probe.Request e) {
            Probe.Response response = new Probe.Response(self, e.getSource(), e.getId(), pendingTasks.size());
            trigger(response, networkPort);
        }
    };
    
    /**
     * Wait for NPROBES responses
     * When all responses are received, compare them and send a request
     * to the node with the lowest load.
     */
    Handler<Probe.Response> handleProbeResponse = new Handler<Probe.Response>() {
        @Override
        public void handle(Probe.Response e) {
            long id = e.getId();
            probeResponses.add(e);
            int machines = pendingRequests.get(id).getMachines();
            if(nProbeResponses(id) == NPROBES*machines) {
                RequestResource r = pendingRequests.get(id);    //TODO, remove later, otherwise memory leak
                for(Probe.Response res : bestResponses(probeResponses, id, machines)) {
                    trigger(new RequestResources.Request(self, res.getSource(), r.getNumCpus(), r.getMemoryInMbs(), r.getTimeToHoldResource(), id), networkPort);
                }
            }
            //Else not all probes have returned.
        }
    };
    
    /**
     * If this node will never be able to handle the request, respond FAILURE.
     * Else, if the requested resources are available, allocate them.
     *       else add the request to the pendingTasks
     */
     Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {
            //This is where resources should be allocated, also add some ID, send response (success/fail)
            int cpu = event.getNumCpus();
            int mem = event.getAmountMemInMb();
            int timeMS = event.getTimeMS();
            
            if(cpu > MAX_CPU || mem > MAX_MEM) {
                //TODO Abort and respond FAILURE    //Fixed
                System.out.println(self.getId() + ": RESOURCES NOT AVAILABLE");
                trigger(new RequestResources.Response(self, event.getSource(), false, event.getId()), networkPort);
                return;
            }
            
            if(availableResources.isAvailable(cpu, mem)) {
                System.out.println("Task id " + event.getId() + " allocated.");
                logger.info("end\t" + event.getId());
              
                trigger(new RequestResources.Response(self, event.getSource(), true, event.getId()), networkPort);
                
                availableResources.allocate(cpu, mem);
                ScheduleTimeout t = new ScheduleTimeout(timeMS);
                t.setTimeoutEvent(new ResourceAllocationTimeout(t, cpu, mem));
                trigger(t, timerPort);
            } else {
                pendingTasks.add(new Task(cpu, mem, timeMS, event.getId(), event.getSource()));
            }
        }
    };

    /**
     * This event is received when a task is done and resources need to be released.
     * Since some resources have been released, this is a good place to try
     * to allocate the resources in a pending task.
     */
    Handler<ResourceAllocationTimeout> handleResourceAllocationTimeout = new Handler<ResourceAllocationTimeout>() {
        @Override
        public void handle(ResourceAllocationTimeout e) {
            int cpu = e.getAllocatedCPU();
            int mem = e.getAllocatedMem();
            availableResources.release(cpu, mem);
            
            //Take a new task from queue, try to allocate resources for it.
            Task nextTask = pendingTasks.peek();
            if(nextTask != null && availableResources.isAvailable(nextTask.getCpus(), nextTask.getMem())) {
                pendingTasks.remove();
                availableResources.allocate(nextTask.getCpus(), nextTask.getMem());
                System.out.println("Task id " + nextTask.getId() + " allocated.");
                logger.info("end\t" + nextTask.getId());
                
                trigger(new RequestResources.Response(self, nextTask.getAddress(), true, nextTask.getId()), networkPort);
                
                ScheduleTimeout t = new ScheduleTimeout(nextTask.getMilliseconds());
                t.setTimeoutEvent(new ResourceAllocationTimeout(t, nextTask.getCpus(), nextTask.getMem()));
                trigger(t, timerPort);
            }
        }
    };

    /**
     * If the resources were successfully allocated, put it to the logger.
     * else, something went wrong.. At the moment failures are not handled.
     */
    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {
            // TODO 
            // Received response from some peer. How to handle fail/success?
            boolean success = event.getSuccess();
            if(success) {
                System.out.println("Response: SUCCESS");
                
                // Task end time
                FileIO.append(System.currentTimeMillis() + "\tend\t" + event.getId() + "\n", "asd.log");
            }            
            
            else
                System.out.println("Response: FAILURE.....Now what?");
        }
    };
    
    /**
     * Replace our old Cyclon sample with this new one.
     */
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            // receive a new list of neighbours 
//            neighbours.clear();
//            neighbours.addAll(event.getSample());
        }
    };
	
    /**
     * Replace our known neighbors with this new updated set.
     */
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            neighbours.clear();
            neighbours.addAll(event.getSample());
        }
    };

    
    /**
     * Count the number of probe responses with a given ID
     * @param id
     * @return number of responses with id == id
     */
    private int nProbeResponses(long id) {
        int n = 0;
        for(Probe.Response res : probeResponses) {
            if(res.getId() == id)
                n++;
        }
        return n;
    }
    
    /**
     * Go through the list of responses, and the probe with lowest queue length with ID = id
     */
    private List<Probe.Response> bestResponses(List<Probe.Response> responses, long id, int n) {
        List<Probe.Response> relevantResponses = new ArrayList<Probe.Response>();
        
        for(Probe.Response res : responses)
            if(res.getId() == id)
                relevantResponses.add(res);
        Collections.sort(relevantResponses);
        
        return relevantResponses.subList(0, n);
    }
}