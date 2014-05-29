package tman.system.peer.tman;

import common.configuration.GradientType;
import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import java.util.ArrayList;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import tman.simulator.snapshot.Snapshot;

public final class TMan extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TMan.class);

    Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    
    private long period;
    private Address self;
    private ArrayList<Address> tmanPartners;
    private TManConfiguration tmanConfiguration;
    private Random r;
    private AvailableResources availableResources;
    
    private int m = 10;         //m, message size, TODO: Add to configuration
    private int sampleSize = 10;
    
    private List<PeerDescriptorTMan> viewTMan;
    
    private PeerDescriptorTMan myDescriptor;
    
    private int GRADIENT_TYPE;
    
    public class TManSchedule extends Timeout {

        public TManSchedule(SchedulePeriodicTimeout request) {
            super(request);
        }

        public TManSchedule(ScheduleTimeout request) {
            super(request);
        }
    }

    public TMan() {
        tmanPartners = new ArrayList<Address>();

        subscribe(handleInit, control);
        subscribe(handleRound, timerPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleTManPartnersResponse, networkPort);
        subscribe(handleTManPartnersRequest, networkPort);
    }

    Handler<TManInit> handleInit = new Handler<TManInit>() {
        @Override
        public void handle(TManInit init) {
            self = init.getSelf();
            tmanConfiguration = init.getConfiguration();
            GRADIENT_TYPE = tmanConfiguration.getGradientType();
            period = tmanConfiguration.getPeriod();
            r = new Random(tmanConfiguration.getSeed());
            availableResources = init.getAvailableResources();
            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);
            
            viewTMan = new ArrayList<PeerDescriptorTMan>();            
            myDescriptor = new PeerDescriptorTMan(self, availableResources);
        }
    };

    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
        @Override
        public void handle(TManSchedule event) {
            
            //TODO: Active part of TMan. Initiate shuffle
            if (viewTMan.size() > 0) {
                PeerDescriptorTMan p = selectPeer(rank(myDescriptor, viewTMan));    //TODO: softMaxFunction?
                ArrayList<PeerDescriptorTMan> temp = new ArrayList<PeerDescriptorTMan>();
                temp.add(myDescriptor);
                List<PeerDescriptorTMan> buffer = merge(viewTMan, temp);
                
                // Remove the peers in sending buffer whose address is the same as p.
                List<PeerDescriptorTMan> tempList = new ArrayList<PeerDescriptorTMan>(buffer);
                for (PeerDescriptorTMan b : tempList) {
                    if (b.getAddress().getId() == p.getAddress().getId()) {
                        buffer.remove(b);
                    }
                }
                
                buffer = rank(p, buffer);
                // If buffer.size() < m, return buffer.size() peers.
                buffer = buffer.subList(0, Math.min(m, buffer.size()));
                ExchangeMsg.Request req = new ExchangeMsg.Request(new DescriptorBufferTMan(myDescriptor, buffer), self, p.getAddress());
                trigger(req, networkPort);
            }
        }
    };
    
    public List<PeerDescriptorTMan> rank(PeerDescriptorTMan myDescriptor, List<PeerDescriptorTMan> view) {
        switch(GRADIENT_TYPE) {
            case GradientType.TYPE_CPU:
                Collections.sort(view, new ComparatorByCPU(myDescriptor));
                break;
            case GradientType.TYPE_MEMORY:
                Collections.sort(view, new ComparatorByMemory(myDescriptor));
                break;
            default:
                Collections.sort(view, new ComparatorByCpuAndMem(myDescriptor));
        }
        
//        if(myDescriptor.getAvailableResources() != null)
//            System.out.println("Ordered view (Base CPU: " + myDescriptor.getAvailableResources().getNumFreeCpus() + " Mem: " + myDescriptor.getAvailableResources().getFreeMemInMbs() + "):");
//        for(PeerDescriptorTMan p : view)
//            if(p.getAvailableResources() != null)
//                System.out.println("CPU: " + p.getAvailableResources().getNumFreeCpus()+ " MEM: " + p.getAvailableResources().getFreeMemInMbs());
        return view;
    }
    
    public PeerDescriptorTMan selectPeer(List<PeerDescriptorTMan> view) {
//        throw new NotImplementedException();
        List<PeerDescriptorTMan> list = new ArrayList<PeerDescriptorTMan>();
        if (view.size() < 3) {
            return view.get(r.nextInt(view.size()));
        }
        else if (view.size() >= 3) {
            list = view.subList(0, view.size() / 2 + 1);
        }
        return list.get(r.nextInt(list.size()));
    }

    // Merge two lists and remove redundancy.
    // The local list is set as oldList, the incoming list is set as newList.
    public List<PeerDescriptorTMan> merge(List<PeerDescriptorTMan> oldList, List<PeerDescriptorTMan> newList) {
        
        List<PeerDescriptorTMan> list = new ArrayList<PeerDescriptorTMan>();
        List<PeerDescriptorTMan> ol = new ArrayList<PeerDescriptorTMan>(oldList);
        List<PeerDescriptorTMan> nl = new ArrayList<PeerDescriptorTMan>(newList);
        
        // TODO
        for (PeerDescriptorTMan dNew : newList) {
            for(PeerDescriptorTMan dOld : oldList) {
                // If dOld and dNew have the same address
                // if dNew.availableResources != null, remove dOld and keep dNew
                // if dNew.availableResources = null, remove dNew and keep dOld
                if (dOld.getAddress().getId() == dNew.getAddress().getId()) {
                    if (dNew.getAvailableResources() != null) {
                        ol.remove(dOld);
                    }
                    else {
                        nl.remove(dNew);
                        // Finish the loop of oldList, and start from next dNew in newList
                        break;
                    }
                }
            }
        }
        list.addAll(ol);
        list.addAll(nl);
        // Remove redundency of list
        List<PeerDescriptorTMan> tempList = new LinkedList<PeerDescriptorTMan>(list);
        for (int i = 0; i < list.size()-1; i++) {
            for (int j=i+1; j < list.size(); j++) {
                if (list.get(j).getAddress().getId() == list.get(i).getAddress().getId()) {
                    if (list.get(i).getAvailableResources() != null) {
                        tempList.remove(list.get(j));
                    }
                    else {
                        tempList.remove(list.get(i));
                        // Finish the loop of j, and start from
                        break;
                    }
                }
            }
        }
        return tempList;        
    }
    
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            List<Address> cyclonAddresses = event.getSample();
            List<PeerDescriptorTMan> cyclonDescriptors = new ArrayList<PeerDescriptorTMan>();
            for(Address a : cyclonAddresses)
                cyclonDescriptors.add(new PeerDescriptorTMan(a, null));
            
            viewTMan = merge(viewTMan, cyclonDescriptors);
            // merge cyclonPartners into TManPartners
            // TODO change to PeerDescriptor
            //tmanPartners = merge(cyclonPartners, tmanPartners);
        }
    };
    

    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
        @Override
        public void handle(ExchangeMsg.Request event) {
            List<PeerDescriptorTMan> temp = new ArrayList<PeerDescriptorTMan>();
            temp.add(myDescriptor);
            List<PeerDescriptorTMan> buffer = merge(viewTMan, temp);
            buffer = rank(event.getRequestBuffer().getFrom(), buffer);
            // If buffer.size() < m, return buffer.size() peers.
            buffer = buffer.subList(0, Math.min(m, buffer.size()));
            ExchangeMsg.Response resp = new ExchangeMsg.Response(new DescriptorBufferTMan(myDescriptor, buffer), self, event.getSource());
            trigger(resp, networkPort);
            
            List<PeerDescriptorTMan> bufferQ = event.getRequestBuffer().getDescriptors();
            viewTMan = merge(viewTMan, bufferQ);
        }
    };

    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
        @Override
        public void handle(ExchangeMsg.Response event) {
            List<PeerDescriptorTMan> buffer = event.getResponseBuffer().getDescriptors();
            
            // Delete the descriptors in buffer whose address is same as myDescriptor.
            List<PeerDescriptorTMan> tempList = new ArrayList<PeerDescriptorTMan>(buffer);
            for (PeerDescriptorTMan b : tempList) {
                if (b.getAddress().getId() == myDescriptor.getAddress().getId()) {
                    buffer.remove(b);
                }
            }
            
            viewTMan = merge(viewTMan, buffer);
            
            // Publish sample to connected components
            List<PeerDescriptorTMan> rankedView = rank(myDescriptor, viewTMan);
            
//            // Test: check the order of ranked sublist
//            System.out.println(self.getId() + " free cup: " + myDescriptor.getAvailableResources().getNumFreeCpus() + " After ranking: ");
//            for(int i = 0; i < viewTMan.size(); i++) {
//                if (viewTMan.get(i).getAvailableResources() != null) {
//                    System.out.print(viewTMan.get(i).getAvailableResources().getNumFreeCpus() + " ");
//                }
//                else {
//                    System.out.print(-1 + " ");
//                }
//            }
//            System.out.println();
            
            tmanPartners.clear();
            for(int i=0; i<sampleSize; i++)
                if(i < rankedView.size())
                    tmanPartners.add(rankedView.get(i).getAddress());
                
            Snapshot.updateTManPartners(self, tmanPartners);
            trigger(new TManSample(tmanPartners), tmanPort);
        }
    };
}
