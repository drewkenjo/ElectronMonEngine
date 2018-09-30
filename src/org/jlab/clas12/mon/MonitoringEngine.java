/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jlab.clas12.mon;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;
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
    public MonitoringEngine() {
        super("monitoring", "kenjo", "1.0");
        initialize();
    }

    public MonitoringEngine(String name, String author, String ver) {
        super(name, author, ver);
        initialize();
    }

    private void initialize() {
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

    @Override
    public boolean init() {
        return true;
    }
}
