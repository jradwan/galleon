package org.lnicholls.galleon.apps.photos;

import com.tivo.hme.bananas.BText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.Effects;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultOptionsScreen;
import org.lnicholls.galleon.widget.MusicOptionsScreen;
import org.lnicholls.galleon.widget.OptionsButton;


public class OptionsScreen extends DefaultOptionsScreen {
    public OptionsScreen(DefaultApplication app) {
        super(app);
        getBelow().setResource(getApp().getInfoBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
        getBelow().flush();
        PhotosConfiguration imagesConfiguration = getConfiguration();
        int start = TOP;
        int width = 270;
        int increment = 37;
        int height = 25;
        BText text = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH,
                30);
        text.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP
                | RSRC_VALIGN_CENTER);
        text.setFont("default-24-bold.font");
        text.setShadow(true);
        text.setValue("Use safe viewing area");
        NameValue[] nameValues = new NameValue[] {
                new NameValue("Yes", "true"), new NameValue("No", "false") };
        mUseSafeButton = new OptionsButton(getNormal(), BORDER_LEFT
                + BODY_WIDTH - width, start, width, height,
        true, nameValues, String.valueOf(imagesConfiguration.isUseSafe()));
        setFocusDefault(mUseSafeButton);
        start = start + increment;
        text = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
        text.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP
                | RSRC_VALIGN_CENTER);
        text.setFont("default-24-bold.font");
        text.setShadow(true);
        text.setValue("Slideshow Effects");
        String names[] = new String[0];
        names = (String[]) Effects.getEffectNames().toArray(names);
        Arrays.sort(names);
        List nameValuesList = new ArrayList();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            nameValuesList.add(new NameValue(name, name));
        }
        nameValuesList.add(new NameValue(Effects.RANDOM, Effects.RANDOM));
        nameValuesList.add(new NameValue(Effects.SEQUENTIAL,
                Effects.SEQUENTIAL));
        nameValues = (NameValue[]) nameValuesList.toArray(new NameValue[0]);
        mEffectButton = new OptionsButton(getNormal(), BORDER_LEFT
                + BODY_WIDTH - width, start, width, height,
        true, nameValues, imagesConfiguration.getEffect());
        start = start + increment;
        text = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
        text.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP
                | RSRC_VALIGN_CENTER);
        text.setFont("default-24-bold.font");
        text.setShadow(true);
        text.setValue("Display Time");
        nameValues = new NameValue[] { new NameValue("2 seconds", "2"),
                new NameValue("3 seconds", "3"),
                new NameValue("4 seconds", "4"),
                new NameValue("5 seconds", "5"),
                new NameValue("6 seconds", "6"),
                new NameValue("7 seconds", "7"),
                new NameValue("8 seconds", "8"),
                new NameValue("9 seconds", "9"),
                new NameValue("10 seconds", "10"),
                new NameValue("11 seconds", "11"),
                new NameValue("12 seconds", "12"),
                new NameValue("13 seconds", "13"),
                new NameValue("14 seconds", "14"),
                new NameValue("15 seconds", "15"),
                new NameValue("16 seconds", "16"),
                new NameValue("17 seconds", "17"),
                new NameValue("18 seconds", "18"),
                new NameValue("19 seconds", "19"),
                new NameValue("20 seconds", "20") };
        mDisplayTimeButton = new OptionsButton(getNormal(), BORDER_LEFT
                + BODY_WIDTH - width, start, width, height,
        true, nameValues, String.valueOf(imagesConfiguration
                .getDisplayTime()));
        start = start + increment;
        text = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
        text.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP
                | RSRC_VALIGN_CENTER);
        text.setFont("default-24-bold.font");
        text.setShadow(true);
        text.setValue("Transition Time");
        mTransitionTimeButton = new OptionsButton(getNormal(), BORDER_LEFT
                + BODY_WIDTH - width, start, width,
        height, true, nameValues, String.valueOf(imagesConfiguration
                .getTransitionTime()));
    }
    
    public Photos getApp() {
        return (Photos)super.getApp();
    }
    
    public PhotosConfiguration getConfiguration() {
        return getApp().getConfiguration();
    }
    
    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        getBelow().setResource(getApp().getInfoBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
        getBelow().flush();
        return super.handleEnter(arg, isReturn);
    }
    public boolean handleExit() {
        try {
            DefaultApplication application = (DefaultApplication) getApp();
            if (!application.isDemoMode())
            {
                PhotosConfiguration imagesConfiguration = getConfiguration();
                imagesConfiguration.setUseSafe(Boolean.valueOf(
                        mUseSafeButton.getValue()).booleanValue());
                imagesConfiguration.setEffect(mEffectButton.getValue());
                imagesConfiguration.setDisplayTime(Integer
                        .parseInt(mDisplayTimeButton.getValue()));
                imagesConfiguration.setTransitionTime(Integer
                        .parseInt(mTransitionTimeButton.getValue()));
                Server.getServer().updateApp(getApp().getFactory().getAppContext());
            }
        } catch (Exception ex) {
            Tools.logException(MusicOptionsScreen.class, ex,
                    "Could not configure music player");
        }
        return super.handleExit();
    }
    private OptionsButton mUseSafeButton;
    private OptionsButton mEffectButton;
    private OptionsButton mDisplayTimeButton;
    private OptionsButton mTransitionTimeButton;
}