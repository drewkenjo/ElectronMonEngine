/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon.ftof;

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
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import org.jlab.groot.data.H1F;
import org.jlab.detector.base.DetectorType;

/**
 *
 * @author kenjo
 */
public class FTOFmonitoring extends MonitoringEngine {

    private Map<String, H1F> hstart = new ConcurrentHashMap<>();
    private final AtomicInteger nprocessed = new AtomicInteger(0);
    private final int nintegration = 5000;

    /**
     * Constructor.
     */
    public FTOFmonitoring() {
    }

    @Override
    public boolean processDataEvent(DataEvent event) {
        if (event.hasBank("RUN::config") && event.hasBank("REC::Event") && event.hasBank("REC::Scintillator")) {
            DataBank runbank = event.getBank("RUN::config");
            DataBank evbank = event.getBank("REC::Event");
            DataBank scbank = event.getBank("REC::Scintillator");

            String keys = runbank.getInt("run", 0) + ",0,";
            //keys.add((runbank.getInt("unixtime", 0) / 60) * 60);

		  for(int isc=0;isc<scbank.rows();isc++){
			int idet = scbank.getByte("detector", isc);
			if(scbank.getShort("pindex", isc)==0 && idet==DetectorType.FTOF.getDetectorId()){
				int sec = scbank.getByte("sector", isc);
                    float sttime = evbank.getFloat("STTime",0);
				if(sec>0 && sttime>0){
					float time = scbank.getFloat("time", isc);
					float path = scbank.getFloat("path", isc);
					sttime = time - path/30;

                    	float rftime = evbank.getFloat("RFTime",0);
					//System.out.println(sttime-rftime);

					hstart.computeIfAbsent(keys+sec, k -> new H1F("h1",1000,-2,2)).fill((sttime - rftime + 1.002)%2.004-1.002);
				}
				break;
			}
		  }
	   }

        //we lose the last chunk of events. Need to ask Vardan how to check if it's the last event (problematic in parallel mode)a

        if (nprocessed.incrementAndGet() % nintegration == 0) {
/*
    			f1.setParameter(0, 100.0);
			f1.setParameter(1, 0.0);
		     f1.setParameter(2, 1.0);
			DataFitter.fit(f1, h1,"Q");

			try{
				BufferedWriter writer = new BufferedWriter(new FileWriter("/home/kenjo/h1.txt"));
				writer.write(h1.toString());
//				writer.write(" "+f1.getParameter(1)+" "+f1.getParameter(2));
    	 			writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
*/

            List<Map<String, String>> nmeans = hstart.keySet().stream()
                    .map(key -> {
                        Map<String, String> nftof = new HashMap<>();
                        if (hstart.containsKey(key) && hstart.get(key).getEntries() > 100) {
					   String[] keys = key.split(",");
                            nftof.put("run", keys[0]);
                            nftof.put("time", keys[1]);
                            nftof.put("sttime_m" + keys[2], Double.toString(hstart.get(key).getMean()));
                            nftof.put("sttime_s" + keys[2], Double.toString(hstart.get(key).getRMS()));
                        }
                        return nftof;
                    })
                    .filter(nftof -> nftof.keySet().size() > 2)
                    .collect(Collectors.toList());

//            nrates.stream().forEach(x->x.values().forEach(System.out::println));

            submit("monftof", nmeans);
        }

       return true;
    }

}
