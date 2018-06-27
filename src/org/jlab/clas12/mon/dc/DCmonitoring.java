/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon.dc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataBank;
import java.util.Collections;
import org.jlab.clas12.mon.MonitoringEngine;

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

    /**
     * Constructor.
     */
    public DCmonitoring() {
    }

    @Override
    public boolean processDataEvent(DataEvent event) {
        if (event.hasBank("TimeBasedTrkg::TBHits") && event.hasBank("RUN::config")) {
            DataBank runbank = event.getBank("RUN::config");
            DataBank tbbank = event.getBank("TimeBasedTrkg::TBHits");

            String key0 = runbank.getInt("run", 0)+",0,";

		  int nrows = tbbank.rows();
            for (int ipart = 0; ipart < nrows; ipart++) {
                int sec = tbbank.getByte("sector", ipart);
                int superlayer = tbbank.getByte("superlayer", ipart);
			 String keystr = key0 + sec + "," + superlayer;

			 nentries.computeIfAbsent(keystr, k -> new DoubleAdder()).add(1);
			 residual.computeIfAbsent(keystr, k -> new DoubleAdder()).add(tbbank.getFloat("timeResidual", ipart));
			 trkdoca.computeIfAbsent(keystr, k -> new DoubleAdder()).add(tbbank.getFloat("trkDoca", ipart));
            }
        }

        //we lose the last chunk of events. Need to ask Vardan how to check if it's the last event (problematic in parallel mode)a

        if (nprocessed.incrementAndGet() % nintegration == 0) {

            List<Map<String, String>> nmeans = nentries.keySet().stream()
                    .map(key -> {
                        Map<String, String> ndc = new HashMap<>();
                        if (nentries.containsKey(key) && nentries.get(key).doubleValue() > 100) {
 			             String[] keys = key.split(",");
                            ndc.put("run", keys[0]);
                            ndc.put("time", keys[1]);
                            double denom = (double) nentries.get(key).doubleValue();
                            String sector = keys[2];
                            String superlayer = keys[3];
                            if (residual.containsKey(key)) {
                               ndc.put("timeResidual" + sector+superlayer, Double.toString(residual.get(key).doubleValue() / denom));
					   }
                            if (trkdoca.containsKey(key)) {
                               ndc.put("trkDoca" + sector+superlayer, Double.toString(trkdoca.get(key).doubleValue() / denom));
                            }
                        }
                        return ndc;
                    })
                    .filter(ndc -> ndc.keySet().size() > 2)
                    .collect(Collectors.toList());

//            nrates.stream().forEach(x->x.values().forEach(System.out::println));

            submit("mondc", nmeans);

        }

        return true;
    }

}
