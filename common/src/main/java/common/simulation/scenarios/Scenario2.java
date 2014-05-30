package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario2 extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {{
                
		SimulationScenario.StochasticProcess process00 = new SimulationScenario.StochasticProcess() {{
			eventInterArrivalTime(constant(1000));
			raise(20, Operations.peerJoin(), 
                                uniform(0, Integer.MAX_VALUE), 
                                constant(12), constant(2)
                             );
		}};
                
                SimulationScenario.StochasticProcess process01 = new SimulationScenario.StochasticProcess() {{
			eventInterArrivalTime(constant(1000));
			raise(20, Operations.peerJoin(), 
                                uniform(0, Integer.MAX_VALUE), 
                                constant(2), constant(12)
                             );
		}};
                
                SimulationScenario.StochasticProcess process02 = new SimulationScenario.StochasticProcess() {{
			eventInterArrivalTime(constant(1000));
			raise(20, Operations.peerJoin(), 
                                uniform(0, Integer.MAX_VALUE), 
                                constant(6), constant(6)
                             );
		}};
                
		SimulationScenario.StochasticProcess process1 = new SimulationScenario.StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(3600, Operations.requestResources(), 
                                uniform(0, Integer.MAX_VALUE),
                                constant(2), constant(2),
                                constant(1000*3), // task execution time
                                constant(2)
                                );
		}};
                
                // TODO - not used yet
		SimulationScenario.StochasticProcess failPeersProcess = new SimulationScenario.StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.peerFail, 
                                uniform(0, Integer.MAX_VALUE));
		}};
                
		SimulationScenario.StochasticProcess terminateProcess = new SimulationScenario.StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(1, Operations.terminate);
		}};
		process00.start();
                process01.startAfterTerminationOf(10, process00);
                process02.startAfterTerminationOf(10, process01);
		process1.startAfterTerminationOf(2000, process02);
                terminateProcess.startAfterTerminationOf(100*1000, process1);
	}};

	// -------------------------------------------------------------------
	public Scenario2() {
		super(scenario);
	}
}
