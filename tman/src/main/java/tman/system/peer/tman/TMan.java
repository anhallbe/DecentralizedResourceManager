package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import common.peer.PeerDescriptor;
import java.util.ArrayList;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.DescriptorBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.hamcrest.SelfDescribing;
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
            PeerDescriptorTMan p = selectPeer(rank(myDescriptor, viewTMan));    //TODO: softMaxFunction?
            ArrayList<PeerDescriptorTMan> temp = new ArrayList<PeerDescriptorTMan>();
            temp.add(myDescriptor);
            List<PeerDescriptorTMan> buffer = merge(viewTMan, temp);
            buffer.remove(p);
            buffer = rank(p, buffer);
            buffer = buffer.subList(0, m);
            ExchangeMsg.Request req = new ExchangeMsg.Request(new DescriptorBufferTMan(myDescriptor, buffer), self, p.getAddress());
            trigger(req, networkPort);
            
        }
    };
    
    public List<PeerDescriptorTMan> rank(PeerDescriptorTMan myDescriptor, List<PeerDescriptorTMan> view) {
//        
//        PeerDescriptor myDescriptor = new PeerDescriptor(self, availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs());
//        Collections.sort(view, new ComparatorByCPU(myDescriptor));
//        
//        return view;
        throw new NotImplementedException();
    }
    
    public PeerDescriptorTMan selectPeer(List<PeerDescriptorTMan> view) {
        throw new NotImplementedException();
    }

    // Merge two lists and remove redundancy.
    public List<PeerDescriptorTMan> merge(List<PeerDescriptorTMan> list1, List<PeerDescriptorTMan> list2) {
        
        Set<PeerDescriptorTMan> mergeSet = new HashSet<PeerDescriptorTMan>();
        mergeSet.addAll(list1);
        mergeSet.addAll(list2);
        ArrayList<PeerDescriptorTMan> list = new ArrayList<PeerDescriptorTMan>();
        list.addAll(mergeSet);
        
        return list;        
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
            buffer = buffer.subList(0, m);
            ExchangeMsg.Response resp = new ExchangeMsg.Response(new DescriptorBufferTMan(myDescriptor, buffer), self, event.getSource());
            trigger(resp, networkPort);
            
            List<PeerDescriptorTMan> bufferQ = event.getRequestBuffer().getDescriptors();
            viewTMan = merge(bufferQ, viewTMan);
        }
    };

    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
        @Override
        public void handle(ExchangeMsg.Response event) {
            List<PeerDescriptorTMan> buffer = event.getResponseBuffer().getDescriptors();
            buffer.remove(myDescriptor);
            viewTMan = merge(buffer, viewTMan);
            
            // Publish sample to connected components
            List<PeerDescriptorTMan> rankedView = rank(myDescriptor, viewTMan);
            tmanPartners.clear();
            for(int i=0; i<sampleSize; i++)
                if(i < rankedView.size())
                    tmanPartners.add(rankedView.get(i).getAddress());
                
            Snapshot.updateTManPartners(self, tmanPartners);
            trigger(new TManSample(tmanPartners), tmanPort);
        }
    };

    // TODO - if you call this method with a list of entries, it will
    // return a single node, weighted towards the 'best' node (as defined by
    // ComparatorById) with the temperature controlling the weighting.
    // A temperature of '1.0' will be greedy and always return the best node.
    // A temperature of '0.000001' will return a random node.
    // A temperature of '0.0' will throw a divide by zero exception :)
    // Reference:
    // http://webdocs.cs.ualberta.ca/~sutton/book/2/node4.html
    public Address getSoftMaxAddress(List<Address> entries) {
        Collections.sort(entries, new ComparatorById(self));

        double rnd = r.nextDouble();
        double total = 0.0d;
        double[] values = new double[entries.size()];
        int j = entries.size() + 1;
        for (int i = 0; i < entries.size(); i++) {
            // get inverse of values - lowest have highest value.
            double val = j;
            j--;
            values[i] = Math.exp(val / tmanConfiguration.getTemperature());
            total += values[i];
        }

        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                values[i] += values[i - 1];
            }
            // normalise the probability for this entry
            double normalisedUtility = values[i] / total;
            if (normalisedUtility >= rnd) {
                return entries.get(i);
            }
        }
        return entries.get(entries.size() - 1);
    }

}
