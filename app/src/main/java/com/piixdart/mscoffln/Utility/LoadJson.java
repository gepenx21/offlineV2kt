package com.piixdart.mscoffln.Utility;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LoadJson {
    public static String loadJSONFromAsset(Context context, String jsonFile) {
        //Turn json data into string
        StringBuilder stringBuilder = new StringBuilder();
        try {
            //Get assets resource manager
            AssetManager assetManager = context.getAssets();
            //Open the file and read through the manager
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(jsonFile)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
//        String json;
//        try {
//            InputStream is = context.getAssets().open(jsonFile);
//            int size = is.available();
//            byte[] buffer = new byte[size];
//            is.read(buffer);
//            is.close();
//            json = new String(buffer, StandardCharsets.UTF_8);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return null;
//        }
//        Log.d("json",json);
//        return json;
    }
}
