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
import se.sics.kompics.timer.SchedulePeriodicTimeout;
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
    // When you partition the index you need to find new nodes
    // This is a routing table maintaining a list of pairs in each partition.
    private Map<Integer, List<PeerDescriptor>> routingTable;
    
    private final int NPROBES = 2;
    private List<Probe.Response> probeResponses;
    private Queue<Task> pendingTasks;      //This queue contains tasks that need to be performed on this node.
    private int MAX_CPU;
    private int MAX_MEM;
//    private Queue<Task> pendingJobs;    //This queue is for tasks that have not been scheduled yet.
    private Map<Long, RequestResource> pendingRequests;
    
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
            pendingTasks = new LinkedList<Task>();
            MAX_CPU = availableResources.getNumFreeCpus();
            MAX_MEM = availableResources.getFreeMemInMbs();
//            pendingJobs = new LinkedList<Task>();
            
            pendingRequests = new LinkedHashMap<Long, RequestResource>();
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
     * Request sent from higher layer (client, not another resource manager)
     */
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource event) {
            System.out.println("Request with id " + event.getId() + " received.");
            logger.info("start\t" + event.getId());
//            logger.info("--------------------My node ID: " + self.getId());
//            logger.info("--------------------Request ID: " + event.getId());
            
            // Task start time
            FileIO.append(System.currentTimeMillis() + "\tstart\t" + event.getId() + "\n", "asd.log");

            int rCpu = event.getNumCpus();
            int rMem = event.getMemoryInMbs();
            int rTime = event.getTimeToHoldResource();
            long jobID = event.getId();
            int machines = event.getMachines();  //event.getMachines()
//            int myCpu = availableResources.getNumFreeCpus();
//            int myMem = availableResources.getFreeMemInMbs();
            
            //If the requested resources are available on this machine, there's no need to ask other peers. Just allocate them here!
            //Rev: On second thought, maybe it will be better for load balance to distribute work anyway...
//            if(rCpu <= myCpu && rMem <= myMem)S {
//                System.out.println("The requested resources are available on this node, don't bother with asking other peers.");
//                trigger(new RequestResources.Request(self, self, rCpu, rMem, rTime), networkPort);
//            } else {
                //System.out.println("Allocate resources: " + event.getNumCpus() + " + " + event.getMemoryInMbs() + " + " + event.getTimeToHoldResource());
            pendingRequests.put(jobID, event);
            if(neighbours.size() > 0) {
                for(int j=0; j<NPROBES*machines; j++) {
                    Address n1 = neighbours.get(random.nextInt(neighbours.size()));
                    Probe.Request r1 = new Probe.Request(self, n1, jobID);
                    trigger(r1, networkPort);
                }
                //System.out.println("------Probes sent");
            }
//            }
            
        }
    };
    
    
    Handler<Probe.Request> handleProbeRequest = new Handler<Probe.Request>() {
        @Override
        public void handle(Probe.Request e) {
            Probe.Response response = new Probe.Response(self, e.getSource(), e.getId(), pendingTasks.size());
            trigger(response, networkPort);
        }
    };
    
    Handler<Probe.Response> handleProbeResponse = new Handler<Probe.Response>() {
        @Override
        public void handle(Probe.Response e) {
            //System.out.println("Probe Response");
            //UUID id = e.getId();
            long id = e.getId();
            probeResponses.add(e);
            int machines = pendingRequests.get(id).getMachines();
            if(nProbeResponses(id) == NPROBES*machines) {
                // Send requests.
                //System.out.println("Received " + NPROBES + " Probe responses");
                //System.out.println("Best response queue length: " + bestResponse(probeResponses, id).getQueue());
//                Probe.Response best = bestResponses(probeResponses, id, machines).get(0);
//                Task job = pendingJobs.poll();
//                trigger(new RequestResources.Request(self, best.getSource(), job.getCpus(), job.getMem(), job.getMilliseconds(), id), networkPort);     //TODO Add real cpu, mem, time
                RequestResource r = pendingRequests.get(id);    //TODO, remove later, otherwise memory leak
                for(Probe.Response res : bestResponses(probeResponses, id, machines)) {
                    trigger(new RequestResources.Request(self, res.getSource(), r.getNumCpus(), r.getMemoryInMbs(), r.getTimeToHoldResource(), id), networkPort);
                }
            }
            //Else not all probes have returned.
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
                System.out.println(self.getId() + ": RESOURCES NOT AVAILABLE");
                trigger(new RequestResources.Response(self, event.getSource(), false, event.getId()), networkPort);
                return;
            }
            
            if(availableResources.isAvailable(cpu, mem)) {
                System.out.println("Task id " + event.getId() + " allocated.");
                logger.info("end\t" + event.getId());
                
//                FileIO.append(System.currentTimeMillis() + "\tend\t" + event.getId() + "\n", "asd.log");
                trigger(new RequestResources.Response(self, event.getSource(), true, event.getId()), networkPort);
                
                availableResources.allocate(cpu, mem);
                //TODO Add a timer event to notify us when the time has run out, and resources should be released.
                //Fixed
                //System.out.println("Allocated " + cpu + " CPUs and " + mem + " MB memory.");
                ScheduleTimeout t = new ScheduleTimeout(timeMS);
                t.setTimeoutEvent(new ResourceAllocationTimeout(t, cpu, mem));
                trigger(t, timerPort);
            } else {
                //System.out.println("Not enough resources, adding to queue.");
                pendingTasks.add(new Task(cpu, mem, timeMS, event.getId(), event.getSource()));
            }
            
            //TODO Should not be responding here, wait for the job to be finished instead?
            //Or wait for the resources to be allocated... To measure performance
//            trigger(new RequestResources.Response(self, event.getSource(), true, event.getId()), networkPort);
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
            
            //System.out.println("Released " + cpu + " CPU and " + mem + " MB memory.");
            
            //Take a new task from queue, try to allocate resources for it.
            Task nextTask = pendingTasks.peek();
            if(nextTask != null && availableResources.isAvailable(nextTask.getCpus(), nextTask.getMem())) {
                pendingTasks.remove();
                availableResources.allocate(nextTask.getCpus(), nextTask.getMem());
                System.out.println("Task id " + nextTask.getId() + " allocated.");
                logger.info("end\t" + nextTask.getId());
                
//                FileIO.append(System.currentTimeMillis() + "\tend\t" + nextTask.getId() + "\n", "asd.log");
                trigger(new RequestResources.Response(self, nextTask.getAddress(), true, nextTask.getId()), networkPort);
                
                ScheduleTimeout t = new ScheduleTimeout(nextTask.getMilliseconds());
                t.setTimeoutEvent(new ResourceAllocationTimeout(t, nextTask.getCpus(), nextTask.getMem()));
                trigger(t, timerPort);
                //System.out.println("Polled a task from queue and allocating resources.");
            } //else
                //System.out.println("NOTHING IN QUEUE");
        }
    };

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
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            //System.out.println("Received samples: " + event.getSample().size());
            
            // receive a new list of neighbours 
//            neighbours.clear();
//            neighbours.addAll(event.getSample());
            
//            System.out.print(self.getId() + " CyclonSample samples: ");
//            for (Address a : neighbours) {
//                System.out.print(a.getId() + " ");
//            }
//            System.out.println();
            
//            logger.info(self.getId() + ": Neighbors:");
//            for(Address n : neighbours)
//                logger.info("\t" + n.getId());
            
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
	
    
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {
            // TODO:
            
            neighbours.clear();
            neighbours.addAll(event.getSample());
            
//            System.out.print(self.getId() + " TManSample samples: ");
//            for (Address a : neighbours) {
//                System.out.print(a.getId() + " ");
//            }
//            System.out.println();
            
        }
    };

    
    /**
     * Count the number of probe responses with a given ID
     * @param id
     * @return number of responses with id == id
     */
    private int nProbeResponses(long id) {
        int n = 0;
        //System.out.println("Probe Request");
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
//        Probe.Response best = null;
//        for(Probe.Response res : responses) {
//            if(res.getId() == id && (best == null || res.getQueue() < best.getQueue()))
//                best = res;
//        }
//        return best;
        List<Probe.Response> relevantResponses = new ArrayList<Probe.Response>();
        for(Probe.Response res : responses)
            if(res.getId() == id)
                relevantResponses.add(res);
        Collections.sort(relevantResponses);
        
//        logger.info("The sorted List: ");
//        for(Probe.Response res : relevantResponses)
//            logger.info("" + res.getQueue());
        return relevantResponses.subList(0, n);
    }
}