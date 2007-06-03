package com.almilli.tivo.hme.hd;

import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.io.HmeInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResolutionInfo extends HmeEvent {
    public static final int EVT_RES_INFO = 8;
    
    private Resolution currentResolution;
    private List<Resolution> supportedResolutions;

    public ResolutionInfo(Resolution currentResolution, List<Resolution> supportedResolutions) {
        super(EVT_RES_INFO, 1);
        this.currentResolution = currentResolution;
        if (supportedResolutions != null) {
            this.supportedResolutions = new ArrayList<Resolution>(supportedResolutions);
        }
    }

    public ResolutionInfo(HmeInputStream input) throws IOException {
        super(EVT_RES_INFO, (int)input.readVInt());

        supportedResolutions = new ArrayList<Resolution>();
        int fieldCount = (int)input.readVInt();
        currentResolution = parseResolution(input, fieldCount);
        int count = (int)input.readVInt();
        for(int i = 0; i < count; i++)
        {
            Resolution resolution = parseResolution(input, fieldCount);
            supportedResolutions.add(resolution);
        }
    }
    
    protected Resolution parseResolution(HmeInputStream input, int fieldCount) throws IOException {
        int width = (int)input.readVInt();
        int height = (int)input.readVInt();
        int pixelAspectNumerator = (int)input.readVInt();
        int pixelAspectDenominator = (int)input.readVInt();
        
        //throw away the remaining fields
        for(int i = 4; i < fieldCount; i++) {
            input.readVInt();
        }

        return new Resolution(width, height, pixelAspectNumerator, pixelAspectDenominator);
    }

    public Resolution getCurrentResolution() {
        return currentResolution;
    }

    public List<Resolution> getSupportedResolutions() {
        return Collections.unmodifiableList(supportedResolutions);
    }

    public Resolution getPreferredResolution() {
        return supportedResolutions.get(0);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResolutionInfo[current=").append(currentResolution);
        sb.append(",supported=");
        boolean first = true;
        for (Resolution res : supportedResolutions) {
            if (!first) {
                sb.append(',');
            }
            sb.append(res);
        }
        sb.append("]");
        return sb.toString();
    }

}
