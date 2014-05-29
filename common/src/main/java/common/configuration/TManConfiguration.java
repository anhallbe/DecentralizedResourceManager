package common.configuration;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

public final class TManConfiguration {

    private final long period;
    private final long seed;
    private final int gradientType;


    public TManConfiguration(long seed, long period, int gradientType) {
        super();
        this.seed = seed;
        this.period = period;
        this.gradientType = gradientType;
    }

    public long getSeed() {
        return seed;
    }


    public long getPeriod() {
        return this.period;
    }

    public void store(String file) throws IOException {
        Properties p = new Properties();
        p.setProperty("seed", "" + seed);
        p.setProperty("period", "" + period);
        p.setProperty("gradient_type", "" + gradientType);

        Writer writer = new FileWriter(file);
        p.store(writer, "se.sics.kompics.p2p.overlay.application");
    }


    public static TManConfiguration load(String file) throws IOException {
        Properties p = new Properties();
        Reader reader = new FileReader(file);
        p.load(reader);

        long seed = Long.parseLong(p.getProperty("seed"));
        long period = Long.parseLong(p.getProperty("period"));
//        double temp = Double.parseDouble(p.getProperty("temperature"));
        int gtype = Integer.parseInt(p.getProperty("gradient_type"));
        
        return new TManConfiguration(seed, period, gtype);
    }

    public int getGradientType() {
        return this.gradientType;
    }
}
