package Utility;

import RoboCam.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraHelper {
    public static String[] getPresets(String cameraUrl) {
        URL urlPresets;
        //System.out.println("getPresets1 : "+ cameraUrl);
        try {
            urlPresets = new URL(cameraUrl);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println("getPresets 1a : "+ cameraUrl);
            return new String[0];
        }
        //System.out.println("getPresets 2 : "+ cameraUrl);
        String str;
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader bin;
        try {
            bin = new BufferedReader(
                    new InputStreamReader(urlPresets.openStream()));
            while ((str = bin.readLine()) != null) {
                lines.add(str);
                //System.out.println("getPresets 3 : "+ str);
            }
        } catch (IOException ex) {
            System.err.println("ip cam not selected? - IOException: " + urlPresets);
            //Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            System.err.println("ip cam not selected? - NullPointerException: " + urlPresets);
        }

        ArrayList<String> filteredList = new ArrayList<>();
        for (int linesPrint = 0; linesPrint < lines.size(); linesPrint++) {
            if (!lines.get(linesPrint).equals("0")) {
                String s = lines.get(linesPrint);
                Pattern p = Pattern.compile(">.*?<");
                Matcher m = p.matcher(s);
                if (m.find()) {
                    String preset_detail = String.valueOf(m.group().subSequence(1, m.group().length() - 1));
                    filteredList.add(preset_detail);
                    //System.out.println("getPresets 4 : "+ preset_detail);
                }
            }
        }
        return filteredList.stream().toArray(String[]::new);
    }
}
