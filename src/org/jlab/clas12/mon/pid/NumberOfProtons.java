/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon.pid;

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
import org.jlab.detector.base.DetectorType;


/**
 *
 * @author kenjo
 */
public class NumberOfProtons extends MonitoringEngine {

    private Map<String, AtomicInteger> nprotons = new ConcurrentHashMap<>();
    private Map<String, AtomicInteger> ntriggers = new ConcurrentHashMap<>();
    AtomicInteger nprocessed = new AtomicInteger(0);
    private final int nintegration = 10000;

    /**
     * Constructor.
     */
    public NumberOfProtons() {
    }

    @Override
    public boolean processDataEvent(DataEvent event) {
        if (event.hasBank("REC::Particle") && event.hasBank("REC::Scintillator") && event.hasBank("RUN::config")) {
            DataBank runbank = event.getBank("RUN::config");
            DataBank pbank = event.getBank("REC::Particle");
            DataBank scbank = event.getBank("REC::Scintillator");

            int nrows = pbank.rows();
            int[] sector = new int[nrows];
            for (int isc = 0; isc < scbank.rows(); isc++) {
			 int idet = scbank.getByte("detector", isc);
			 if(idet == DetectorType.FTOF.getDetectorId()){
	                int pindex = scbank.getShort("pindex", isc);
     	           sector[pindex] = scbank.getByte("sector", isc);
			 }
            }

            String keys = runbank.getInt("run", 0) + ",0,";

            ntriggers.computeIfAbsent(keys, k -> new AtomicInteger(0)).incrementAndGet();

            for (int ipart = 0; ipart < nrows; ipart++) {
                int pid = pbank.getInt("pid", ipart);
                if (pid == 2212 && sector[ipart] > 0) {
                    nprotons.computeIfAbsent(keys+sector[ipart], k->new AtomicInteger(0)).incrementAndGet();
                }
            }
        }

        //we lose the last chunk of events. Need to ask Vardan how to check if it's the last event (problematic in parallel mode)
        if (nprocessed.getAndIncrement() % nintegration == 0) {

            List<Map<String, String>> nrates = ntriggers.keySet().stream()
                    .map(key -> {
                        Map<String, String> npro = new HashMap<>();
                        if (ntriggers.containsKey(key) && ntriggers.get(key).get() > 100) {
					   String[] keys = key.split(",");
                            npro.put("run", keys[0]);
                            npro.put("time", keys[1]);
                            float denom = ntriggers.get(key).get();
                            for (int isec = 1; isec <= 6; isec++) {
                                if (nprotons.containsKey(key+isec)) {
                                    npro.put("npro" + isec, Float.toString(nprotons.get(key+isec).get() / denom));
                                }
                            }
                        }
                        return npro;
                    })
                    .filter(npro -> npro.keySet().size() > 2)
                    .collect(Collectors.toList());

//            nrates.stream().forEach(x->x.values().forEach(System.out::println));

            submit("monpro", nrates);
        }
        return true;
    }

}
