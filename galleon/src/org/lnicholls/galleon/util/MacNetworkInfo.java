package org.lnicholls.galleon.util;

/*
 * Original source from: http://forum.java.sun.com/thread.jsp?forum=4&thread=245711
 */

import java.io.IOException;
import java.text.ParseException;

public class MacNetworkInfo extends NetworkInfo {
    public static final String IPCONFIG_COMMAND = "ifconfig";

    public String parseMacAddress() throws ParseException {
        String ipConfigResponse = null;
        try {
            ipConfigResponse = runConsoleCommand(IPCONFIG_COMMAND);
        } catch (IOException e) {
            //e.printStackTrace();
            throw new ParseException(e.getMessage(), 0);
        }
        String localHost = null;
        try {
            localHost = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException ex) {
            //ex.printStackTrace();
            throw new ParseException(ex.getMessage(), 0);
        }

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(ipConfigResponse, "\n");
        String lastMacAddress = null;

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken().trim();
            boolean containsLocalHost = line.indexOf(localHost) >= 0;

            // see if line contains IP address
            if (containsLocalHost && lastMacAddress != null) {
                return lastMacAddress;
            }

            // see if line contains MAC address
            int macAddressPosition = line.indexOf("ether");
            if (macAddressPosition <= 0) {
                continue;
            }

            String macAddressCandidate = line.substring(macAddressPosition + 6).trim();
            if (isMacAddress(macAddressCandidate)) {
                lastMacAddress = macAddressCandidate;
                continue;
            }
        }

        ParseException ex = new ParseException("cannot read MAC address for " + localHost + " from ["
                + ipConfigResponse + "]", 0);
        //ex.printStackTrace();
        throw ex;
    }

    public String parseDomain(String hostname) throws ParseException {
        return "";
    }

    private final boolean isMacAddress(String macAddressCandidate) {
        if (macAddressCandidate.length() != 17)
            return false;
        return true;
    }
}