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
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.GraphErrors;

/**
 *
 * @author kenjo
 */
public class Monitoring {
    static class RunSeriesElement {
        private final String name;
        private final String variation;
        private final int run;
        private final double value;
        private final double error;

        RunSeriesElement(String name, String variation, int run, double value, double error) {
            this.name = name;
            this.variation = variation;
            this.run = run;
            this.value = value;
            this.error = error;
        }
    }

    public static void upload(String name, String variation, int run, double value) {
        upload(name, variation, run, value, 0);
    }


    public static void upload(String name, String variation, int run, double value, double error) {
        Gson gson = new Gson();
        RunSeriesElement entry = new RunSeriesElement(name, variation, run, value, error);

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
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("name", h1.getName());
        entryMap.put("variation", variation);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("title", h1.getTitle());
        dataMap.put("xtitle", h1.getTitleX());
        dataMap.put("ytitle", h1.getTitleY());

        List<Map<String, Object>> seriesList = new ArrayList<>();
        Map<String, Object> serieMap = new HashMap<>();
        List<List<Double>> data = new ArrayList<>();
        for (int ibin = 0; ibin < h1.getDataSize(0); ibin++) {
            data.add(Arrays.asList(h1.getXaxis().getBinCenter(ibin), h1.getBinContent(ibin)));
        }
        serieMap.put("name", h1.getName()+"/"+variation);
        serieMap.put("type", "column");
        serieMap.put("data", data);
        seriesList.add(serieMap);
        dataMap.put("series", seriesList);
        entryMap.put("data", dataMap);

        upload(entryMap);
    }


    public static void upload(GraphErrors gr) {
        upload(gr, "default");
    }


    public static void upload(GraphErrors gr, String variation) {
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("name", gr.getName());
        entryMap.put("variation", variation);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("title", gr.getTitle());
        dataMap.put("xtitle", gr.getTitleX());
        dataMap.put("ytitle", gr.getTitleY());

        List<Map<String, Object>> seriesList = new ArrayList<>();
        Map<String, Object> serieMap = new HashMap<>();
        List<List<Double>> data = new ArrayList<>();
        for (int ibin = 0; ibin < gr.getDataSize(0); ibin++) {
            data.add(Arrays.asList(gr.getDataX(ibin), gr.getDataY(ibin)));
        }
        serieMap.put("name", gr.getName()+"/"+variation);
        serieMap.put("type", "scatter");
        serieMap.put("data", data);
        seriesList.add(serieMap);
         Map<String, Object> errMap = new HashMap<>();
        List<List<Double>> dataerr = new ArrayList<>();
        for (int ibin = 0; ibin < gr.getDataSize(0); ibin++) {
            dataerr.add(Arrays.asList(gr.getDataEX(ibin), gr.getDataEY(ibin)));
        }
        errMap.put("name", gr.getName()+"/"+variation);
        errMap.put("type", "errorbar");
        errMap.put("data", dataerr);
        seriesList.add(errMap);

        dataMap.put("series", seriesList);
        entryMap.put("data", dataMap);
        upload(entryMap);
    }


    public static void upload(Map mm) {
        try {
            Gson gson = new Gson();
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
                gson.toJson(mm, fwriter);
                fwriter.flush();
                fwriter.close();

//System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(entryMap));
                System.out.println(mm.get("name") + " " + mm.get("variation"));
                System.out.println(con.getResponseCode());
                System.out.println(con.getResponseMessage());
            }
        } catch (IOException ex) {
            Logger.getLogger(Monitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
