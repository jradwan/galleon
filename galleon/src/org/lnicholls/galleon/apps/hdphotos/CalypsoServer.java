/**
 * 
 */
package org.lnicholls.galleon.apps.hdphotos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class CalypsoServer extends Thread {
    private static final Log log = LogFactory.getLog(CalypsoServer.class);
    
    private boolean closed;
    private ServerSocket serverSocket;
    public CalypsoServer() throws IOException {
        super("Calypso Server");
        setDaemon(true);
        log.info("Starting up calypso...");
        serverSocket = new ServerSocket(0);
    }
    
    public void close() {
        closed = true;
        synchronized (serverSocket) {
            serverSocket.notifyAll();
        }
    }
    
    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }
    
    public void run() {
        log.info("Accepting calypso connections on port " + getLocalPort());
        while (!closed) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                log.warn("Error receiving calypso connection", e);
                try {
                    sleep(1000);
                } catch (InterruptedException e2) {
                }
                continue;
            }

            log.info("Received calypso connection :" + socket.getRemoteSocketAddress());
            try {
                synchronized (serverSocket) {
                    serverSocket.wait();
                }
            } catch (InterruptedException e) {
            }
        }
        log.info("Shutting down calypso...");
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.warn("Error shutting down calypso", e);
        }
        serverSocket = null;
    }
}