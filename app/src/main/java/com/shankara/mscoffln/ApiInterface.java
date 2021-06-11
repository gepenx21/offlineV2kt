package com.shankara.mscoffln;

import com.shankara.mscoffln.Model.Song;

import java.util.ArrayList;

public interface ApiInterface {
    void onSuccess(ArrayList<Song> songs);
    void onError(String message);
}
