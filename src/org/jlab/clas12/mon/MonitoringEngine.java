/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas12.mon;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.clas.reco.ReconstructionEngine;

/**
 *
 * @author kenjo
 */
public abstract class MonitoringEngine extends ReconstructionEngine {

    protected Gson gson = new Gson();
    private String token = "";

    /**
     * Constructor.
     */
    public MonitoringEngine(){
        super("test", "kenjo", "1.0");
        initialize();
    }

    public MonitoringEngine(String name, String author, String ver){
        super(name, author, ver);
        initialize();
    }
    
    private void initialize(){
        addJLabCert();
        try {
            getToken();
        } catch (FileNotFoundException ex) {
            System.err.println("No security token is found");
            Logger.getLogger(MonitoringEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getToken() throws FileNotFoundException {
        String homedir = System.getenv("HOME");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(homedir + "/.clas12mon.token"));
        Map<String, String> tokenMap = gson.fromJson(bufferedReader, Map.class);
        if (tokenMap.containsKey("key")) {
            token = tokenMap.get("key");
        }
    }

    private void addJLabCert() {
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
            System.err.println("[Mon12Resources] keystore with JLab SSL certificate can't be added!");
        }
    }

    public void submit(List<Map<String, String>> entryList) {
//        System.out.println(gson.toJson(entryList, List.class));

        if (!entryList.isEmpty()) {
            try {
                URL url = new URL("https://clas12mon.jlab.org/mondb/data");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setRequestMethod("PUT");
                con.setRequestProperty("token", token);
                Writer fwriter = new OutputStreamWriter(con.getOutputStream());
                gson.toJson(entryList, fwriter);
                fwriter.flush();
                fwriter.close();

                System.out.println(con.getResponseCode());
                System.out.println(con.getResponseMessage());
            } catch (IOException ex) {
                Logger.getLogger(MonitoringEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public boolean init() {
        return true;
    }
}
