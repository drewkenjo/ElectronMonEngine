/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;

/**
 *
 * @author kenjo
 */
public class NumberOfElectrons extends MonitoringEngine {

    private Map<List<Integer>, AtomicInteger> nelectrons = new ConcurrentHashMap<>();
    private Map<List<Integer>, AtomicInteger> ntriggers = new ConcurrentHashMap<>();
    AtomicInteger nprocessed = new AtomicInteger(0);
    private final int nintegration = 3000;

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
                int pindex = calbank.getShort("pindex", ical);
                sector[pindex] = calbank.getByte("sector", ical);
            }

            List<Integer> keys = new ArrayList<>(3);
            keys.add(runbank.getInt("run", 0));
            keys.add((runbank.getInt("unixtime", 0) / 60) * 60);
            keys.add(0);

            if (!ntriggers.containsKey(keys)) {
                ntriggers.put(keys, new AtomicInteger(0));
            }
            ntriggers.get(keys).incrementAndGet();

            for (int ipart = 0; ipart < nrows; ipart++) {
                int pid = pbank.getInt("pid", ipart);
                if (pid == 11 && sector[ipart] > 0) {
                    keys.set(2, sector[ipart]);
                    if (!nelectrons.containsKey(keys)) {
                        nelectrons.put(keys, new AtomicInteger(0));
                    }
                    nelectrons.get(keys).incrementAndGet();
                }
            }
        }

        //we lose the last chunk of events. Need to ask Vardan how to check if it's the last event (problematic in parallel mode)
        if (nprocessed.getAndIncrement() % nintegration == 0) {

            List<Map<String, String>> nrates = ntriggers.keySet().stream()
                    .map(key -> {
                        Map<String, String> nele = new HashMap<>();
                        if (ntriggers.containsKey(key) && ntriggers.get(key).get() > 100) {
                            nele.put("run", Integer.toString(key.get(0)));
                            nele.put("time", Integer.toString(key.get(1)));
                            List<Integer> elekey = new ArrayList<>(key);
                            float denom = ntriggers.get(key).get();
                            for (int isec = 1; isec <= 6; isec++) {
                                elekey.set(2, isec);
                                if (nelectrons.containsKey(elekey)) {
                                    nele.put("nele" + isec, Float.toString(nelectrons.get(elekey).get() / denom));
                                }
                            }
                        }
                        return nele;
                    })
                    .filter(nele -> nele.keySet().size() > 2)
                    .collect(Collectors.toList());

//            nrates.stream().forEach(x->x.values().forEach(System.out::println));

            submit(nrates);
        }
        return true;
    }

}
