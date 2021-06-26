package com.piixdart.mscoffln;

import com.piixdart.mscoffln.Model.Song;

import java.util.ArrayList;

public interface ApiInterface {
    void onSuccess(ArrayList<Song> songs);
    void onError(String message);
}
