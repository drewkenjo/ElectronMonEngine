/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon.pid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.clas12.mon.MonitoringEngine;
import org.jlab.clas12.mon.Monitoring;
import org.jlab.detector.base.DetectorType;


/**
 *
 * @author kenjo
 */
public class NumberOfElectrons extends MonitoringEngine {

    private Map<String, AtomicInteger> nelectrons = new ConcurrentHashMap<>();
    private Map<String, AtomicInteger> ntriggers = new ConcurrentHashMap<>();
    AtomicInteger nprocessed = new AtomicInteger(0);
    private final int nintegration = 10000;

    /**
     * Constructor.
     */
    public NumberOfElectrons() {
    }

    @Override
    public boolean processDataEvent(DataEvent event) {
        if (event.hasBank("REC::Particle") && event.hasBank("REC::Calorimeter") && event.hasBank("RUN::config")) {
            DataBank runbank = event.getBank("RUN::config");
            DataBank pbank = event.getBank("REC::Particle");
            DataBank calbank = event.getBank("REC::Calorimeter");

            int nrows = pbank.rows();
            int[] sector = new int[nrows];
            for (int ical = 0; ical < calbank.rows(); ical++) {
                int idet = calbank.getByte("detector", ical);
                if (idet == DetectorType.ECAL.getDetectorId()) {
                    int ilay = calbank.getByte("layer", ical);
                    if (ilay == 1 || ilay == 4 || ilay == 7) {
                        int pindex = calbank.getShort("pindex", ical);
                        sector[pindex] = calbank.getByte("sector", ical);
                    }
                }
            }

            String keystr = runbank.getInt("run", 0) + ", 0, ";
            ntriggers.computeIfAbsent(keystr, k -> new AtomicInteger(0)).incrementAndGet();

            for (int ipart = 0; ipart < nrows; ipart++) {
                int pid = pbank.getInt("pid", ipart);
                if (pid == 11 && sector[ipart] > 0) {
                    nelectrons.computeIfAbsent(keystr + sector[ipart], k -> new AtomicInteger(0)).incrementAndGet();
                }
            }
        }

        //we lose the last chunk of events. Need to ask Vardan how to check if it's the last event (problematic in parallel mode)
        if (nprocessed.getAndIncrement() % nintegration == 0) {

            ntriggers.keySet().stream()
                    .forEach(key -> {
                        if (ntriggers.containsKey(key) && ntriggers.get(key).get() > 100) {
                            String[] keys = key.split(", ");
                            int run = Integer.parseInt(keys[0]);
                            float denom = ntriggers.get(key).get();
                            for (int isec = 1; isec <= 6; isec++) {
                                if (nelectrons.containsKey(key + isec)) {
                                    Monitoring.upload("nele" + isec, "default", run, nelectrons.get(key + isec).get() / denom);
                                }
                            }
                        }
                    });

//            nrates.stream().forEach(x -> x.values().forEach(System.out::println));
        }
        return true;
    }

}
