/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jlab.clas12.mon;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.groot.data.H1F;

/**
 *
 * @author kenjo
 */
public class Monitoring {
    public static void addJLabCert() {
        String coatdir = System.getenv("COATJAVA");
        if (coatdir != null) {
            String storename = coatdir + "/etc/data/keystore/jlab.keystore";
            File ff = new File(storename);

            if (ff.isFile()) {
                Properties p = new Properties(System.getProperties());
                p.setProperty("javax.net.ssl.trustStore", storename);
                System.setProperties(p);
            }
        } else {
            System.err.println("[Mon12Resources] can't find COATJAVA environment variable");
            System.err.println("[Mon12Resources] keystore with JLab SSL certif icate can't be added!");
        }
    }

    static class RunSeriesElement {
        private final String name;
        private final String variation;
        private final int run;
        private final double value;

        RunSeriesElement(String name, String variation, int run, double value) {
            this.name = name;
            this.variation = variation;
            this.run = run;
            this.value = value;
        }
    }

    public static void upload(String name, String variation, int run, double value) {
        Gson gson = new Gson();
        RunSeriesElement entry = new RunSeriesElement(name, variation, run, value);

        try {
            String homedir = System.getenv("HOME");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(homedir + "/.clas12mon.token"));
            Map<String, String> tokenMap = gson.fromJson(bufferedReader, Map.class);
            if (tokenMap.containsKey("key")) {
                String token = tokenMap.get("key");

                URL url = new URL("https://clas12mon.jlab.org/mondb/runseries/");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setRequestMethod("PUT");
                con.setRequestProperty("token", token);
                Writer fwriter = new OutputStreamWriter(con.getOutputStream());
                gson.toJson(entry, fwriter);
                fwriter.flush();
                fwriter.close();

                System.out.println(gson.toJson(entry));
                System.out.println(con.getResponseCode());
                System.out.println(con.getResponseMessage());
            }
        } catch (IOException ex) {
            Logger.getLogger(Monitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void upload(H1F h1) {
        upload(h1, "default");
    }


    public static void upload(H1F h1, String variation) {
        Gson gson = new Gson();
        Map<String, String> entryMap = new HashMap<>();
        entryMap.put("name", h1.getName());
        entryMap.put("title", h1.getTitle());
        entryMap.put("xtitle", h1.getTitleX());
        entryMap.put("ytitle", h1.getTitleY());
        entryMap.put("variation", variation);
        StringBuilder data = new StringBuilder("[");
        for(int ibin=0;ibin< h1.getDataSize(0);ibin++) {
            data.append("[" + h1.getXaxis().getBinCenter(ibin) + ", " + h1.getBinContent(ibin) + "], ");
        }
        data.setCharAt(data.length()-1, ']');
        entryMap.put("data", data.toString());
        try {
            String homedir = System.getenv("HOME");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(homedir + "/.clas12mon.token"));
            Map<String, String> tokenMap = gson.fromJson(bufferedReader, Map.class);
            if (tokenMap.containsKey("key")) {
                String token = tokenMap.get("key");

                URL url = new URL("https://clas12mon.jlab.org/mondb/h1d/");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setRequestMethod("PUT");
                con.setRequestProperty("token", token);
                Writer fwriter = new OutputStreamWriter(con.getOutputStream());
                gson.toJson(entryMap, fwriter);
                fwriter.flush();
                fwriter.close();

//				System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(entryMap));
                System.out.println(h1.getName() + " " + variation);
                System.out.println(con.getResponseCode());
                System.out.println(con.getResponseMessage());
            }
        } catch (IOException ex) {
            Logger.getLogger(Monitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
