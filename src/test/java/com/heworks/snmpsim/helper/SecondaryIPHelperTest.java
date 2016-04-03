package com.heworks.snmpsim.helper;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by m2c2 on 3/5/16.
 */
public class SecondaryIPHelperTest {


    public static void main(String[] args) throws IOException {
        File file = new File("device.walk");

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line = null;
        String fullLine = "";

        fullLine = reader.readLine();
        while ((line = reader.readLine()) != null) {
            if (beginningOfNewOidLine(line)) {
                //full line is complete, process full line
                processOidString(fullLine);
                fullLine = line;
            } else {
                fullLine = fullLine + line;
            }
        }
        processOidString(fullLine);
    }

    public static boolean beginningOfNewOidLine(String string) {
        if (string.startsWith(".1.3.6.1")) {
            return true;
        }
        return false;
    }

    public static void processOidString(String fullLine) {
        String oid = null;
        String type = null;
        String value = null;
        String[] strings = fullLine.split("=");
        oid = strings[0].trim();
        //not a valid oid, skip
        if(!strings[1].contains(": ")) {
            return;
        }
        String[] strings2 = strings[1].split(": ");
        type = strings2[0].trim();
        value = strings[1].substring(strings2[0].length() + 2);
        System.out.println("oid: " + oid + ", type: " + type + ", value: " + value);
    }


}
