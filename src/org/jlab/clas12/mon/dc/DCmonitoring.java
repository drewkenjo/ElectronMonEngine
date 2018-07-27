/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon.dc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.clas12.mon.MonitoringEngine;
import org.jlab.clas12.mon.Monitoring;

/**
 *
 * @author kenjo
 */
public class DCmonitoring extends MonitoringEngine {

    private Map<String, DoubleAdder> residual = new ConcurrentHashMap<>();
    private Map<String, DoubleAdder> trkdoca = new ConcurrentHashMap<>();
    private Map<String, DoubleAdder> nentries = new ConcurrentHashMap<>();
    private final AtomicInteger nprocessed = new AtomicInteger(0);
    private final int nintegration = 10000;
    private String variation;

    /**
     * Constructor.
     */
    public DCmonitoring() {
    }

    @Override
    public boolean init() {
        System.out.println("##################################################");
        System.out.println(this.getEngineConfigString("variation"));
        variation = this.getEngineConfigString("variation");
        if (variation == null){
            variation = "default";
        }
        return true;
    }

    @Override
    public boolean processDataEvent(DataEvent event) {
        if (event.hasBank("TimeBasedTrkg::TBHits") && event.hasBank("RUN::config")) {
            DataBank runbank = event.getBank("RUN::config");
            DataBank tbbank = event.getBank("TimeBasedTrkg::TBHits");

            String key0 = runbank.getInt("run", 0) + ", 0, ";

            int nrows = tbbank.rows();
            for (int ipart = 0; ipart < nrows; ipart++) {
                int sec = tbbank.getByte("sector", ipart);
                int superlayer = tbbank.getByte("superlayer", ipart);
                String keystr = key0 + sec + ", " + superlayer;

                nentries.computeIfAbsent(keystr, k -> new DoubleAdder()).add(1);
                residual.computeIfAbsent(keystr, k -> new DoubleAdder()).add(tbbank.getFloat("timeResidual", ipart));
                trkdoca.computeIfAbsent(keystr, k -> new DoubleAdder()).add(tbbank.getFloat("trkDoca", ipart));
            }
        }

        //we lose the last chunk of events. Need to ask Vardan how to check if it's the last event (problematic in parallel mode)

        if (nprocessed.incrementAndGet() % nintegration == 0) {

            nentries.keySet().stream()
                    .forEach(key -> {
                        if (nentries.containsKey(key) && nentries.get(key).doubleValue() > 100) {
                            String[] keys = key.split(", ");
                            int run = Integer.parseInt(keys[0]);
                            double denom = (double) nentries.get(key).doubleValue();
                            String sector = keys[2];
                            String superlayer = keys[3];
                            if (residual.containsKey(key)) {
                                Monitoring.upload("timeResidual" + sector + superlayer, variation, run, residual.get(key).doubleValue() / denom);
                            }
                            if (trkdoca.containsKey(key)) {
                                Monitoring.upload("trkDoca" + sector + superlayer, variation, run, trkdoca.get(key).doubleValue() / denom);
                            }
                        }
                    });

//            nrates.stream().forEach(x -> x.values().forEach(System.out::println));
        }

        return true;
    }

}
