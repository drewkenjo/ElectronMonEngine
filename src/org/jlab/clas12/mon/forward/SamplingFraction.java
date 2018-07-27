/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon.forward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import org.jlab.clas12.mon.MonitoringEngine;
import org.jlab.clas12.mon.Monitoring;
import org.jlab.detector.base.DetectorType;
import org.jlab.groot.data.H1F;

/**
 *
 * @author kenjo
 */
public class SamplingFraction extends MonitoringEngine {

    private Map<String, H1F> helesf = new ConcurrentHashMap<>();
    AtomicInteger nprocessed = new AtomicInteger(0);
    private final int nintegration = 10000;

    /**
     * Constructor.
     */
    public SamplingFraction() {
    }

    @Override
    public boolean processDataEvent(DataEvent event) {
        if (event.hasBank("REC::Particle") && event.hasBank("REC::Calorimeter") && event.hasBank("RUN::config")) {
            DataBank runbank = event.getBank("RUN::config");
            DataBank pbank = event.getBank("REC::Particle");
            DataBank calbank = event.getBank("REC::Calorimeter");

            int nrows = pbank.rows();
            int[] sector = new int[nrows];
            float[] edep = new float[nrows];
            for (int ical = 0; ical < calbank.rows(); ical++ ) {
                int idet = calbank.getByte("detector", ical);
                if (idet == DetectorType.ECAL.getDetectorId()){
                	 int ilay = calbank.getByte("layer", ical);
                    if (ilay == 1 || ilay == 4 || ilay == 7) {
                    	int pindex = calbank.getShort("pindex", ical);
     	           	sector[pindex] = calbank.getByte("sector", ical);
                        edep[pindex] += calbank.getFloat("energy", ical);
                    }
                }
            }

            String keystr = runbank.getInt("run",0) + ",0,";

            for (int ipart = 0; ipart < nrows; ipart++ ) {
                int pid = pbank.getInt("pid", ipart);
                if (pid == 11 && sector[ipart] > 0) {
                    float px = pbank.getFloat("px", ipart);
                    float py = pbank.getFloat("py", ipart);
                    float pz = pbank.getFloat("pz", ipart);
                    float pp = px*px + py*py + pz*pz;
                    helesf.computeIfAbsent(keystr + sector[ipart], k -> new H1F("sf", 300, 0.1, 0.4)).fill(edep[ipart]/Math.sqrt(pp));
                }
            }
        }

        //we lose the last chunk of events. Need to ask Vardan how to check if it's the last event (problematic in parallel mode)
        if (nprocessed.getAndIncrement() % nintegration == 0) {

            helesf.keySet().stream()
                    .forEach(key -> {
                        Map<String, String> elesf = new HashMap<>();
                        if (helesf.containsKey(key) && helesf.get(key).getEntries() > 100) {
                            String[] keys = key.split(",");
                            int run = Integer.parseInt(keys[0]);
                            Monitoring.upload("elesf" + keys[2], "default", run, helesf.get(key).getMean());
                        }
                    });

//            nrates.stream().forEach(x->x.values().forEach(System.out::println));
        }
        return true;
    }

}
