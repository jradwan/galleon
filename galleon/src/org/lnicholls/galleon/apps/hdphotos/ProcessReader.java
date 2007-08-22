/**
 * 
 */
package org.lnicholls.galleon.apps.hdphotos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessReader extends Thread {
    private static final Log log = LogFactory.getLog(ProcessReader.class);
    private String label;
    private boolean closed;
    private BufferedReader input;
    public ProcessReader(String label, InputStream input) {
        super("ProcessReader: " + label);
        setDaemon(true);
        this.label = label;
        this.input = new BufferedReader(new InputStreamReader(input));
    }
    
    public void close() throws IOException {
        closed = true;
        
        //this is bad practice to interrupt threads
        this.interrupt();
    }
    
    public void run() {
        try {
            for (String line=input.readLine(); line != null && !closed; line=input.readLine()) {
                if (label != null) {
                    line = label + line;
                }
                log.info(line);
            }
            input.close();
        } catch (IOException e) {
            log.warn("Cannot read from process stream.", e);
        }
    }
}