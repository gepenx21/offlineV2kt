package com.shankara.mscoffln;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.InterstitialCallbacks;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.shankara.mscoffln.Adapter.SongAdapter;
import com.shankara.mscoffln.Model.Song;
import com.shankara.mscoffln.Utility.ScrollTextView;
import com.shankara.mscoffln.Utility.Utility;
import com.shankara.mscoffln.Utility.loadJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import static com.shankara.mscoffln.Config.JSON_ID;


public class MainActivity extends AppCompatActivity {


    private static final String CONSENT = "consent";
    private RecyclerView recycler;
    private SongAdapter mAdapter;
    public static ArrayList<Song> songList;
    private int currentIndex;
    private TextView tv_time, total_duration;
    private ImageView iv_play, iv_next, iv_previous, iv_share, iv_info;
    private ProgressBar pb_main_loader;
    private MediaPlayer mediaPlayer;
    private SeekBar mSeekBar;
    boolean firstLaunch = true;
    private static int mCount = 0;
    private static int counter = 1;
    boolean intShow = false;
    Toolbar toolbar;
    Drawer result;

    final Handler mHandler = new Handler();
    private ScrollTextView tb_title;
    Animation anim = new AlphaAnimation(0.0f, 1.0f);
    boolean mBlinking = false;
    FragmentManager fm = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Appodeal.initialize(this,Config.appId, Appodeal.BANNER | Appodeal.INTERSTITIAL);
        Appodeal.disableLocationPermissionCheck();
        Appodeal.setTesting(true);
        Appodeal.cache(this, Appodeal.INTERSTITIAL);
        initializeViews();

        songList = new ArrayList<>();
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mAdapter = new SongAdapter(getApplicationContext(), songList, (song, position) -> {
            changeSelectedSong(position);
            if (mBlinking) {
                mBlinking = false;
                tv_time.clearAnimation();
                tv_time.setAlpha(1.0f);
            }
            prepareSong(song);

        });
        LinearLayout linearlayout = findViewById(R.id.adView);
        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.show(this, Appodeal.BANNER_VIEW, String.valueOf(linearlayout));
        recycler.setAdapter(mAdapter);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(this::togglePlay);

        anim.setDuration(500); //manage the blinking time
        anim.setStartOffset(50);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        mediaPlayer.setOnCompletionListener(mp -> {
            if (Config.isPlaying){
                Config.isPlaying=false;
            }
            if(currentIndex + 1 < songList.size()){
                Song next = songList.get(currentIndex + 1);
                changeSelectedSong(currentIndex+1);
                prepareSong(next);
            }else{
                Song next = songList.get(0);
                changeSelectedSong(0);
                prepareSong(next);
            }
        });
        handleSeekbar();
        pushPlay();
        btnPrevious();
        btnNext();
        pushShare();
        pushInfo();
        initDrawer();
        getSongListMain();
    }

    public static Intent getIntent(Context context, boolean consent) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(CONSENT, consent);
        return intent;
    }

    public void initDrawer(){
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        if (toolbar != null)
            setSupportActionBar(toolbar);
        @SuppressLint("UseCompatLoadingForDrawables") AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withCompactStyle(false)
                .addProfiles(new ProfileDrawerItem().
                        withName(getString(R.string.AUTHOR))
                        .withEmail(getString(R.string.PUBLISHER_EMAIL))
                        .withIcon(getResources().getDrawable(R.drawable.menu_profile)))
                .withOnAccountHeaderListener((view, profile, currentProfile) -> false)
                .build();

        result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .inflateMenu(R.menu.main_menu)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    switch (position) {
                        case 2:
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, "Download " + getString(R.string.app_name) + " in : " + "\n" + "https://play.google.com/store/apps/details?id=" + getPackageName());
                            sendIntent.setType("text/plain");
                            startActivity(sendIntent);
                            break;
                        case 3:
                            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName());
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                            break;
                        case 4:
                            Intent intents = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.MORE_APP)));
                            startActivity(intents);
                            break;
                        default:
                            break;
                    }
                    return false;
                })
                .build();
        result.setSelection(-1);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
    }

    private void handleSeekbar(){
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int seekProgress;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekProgress = progress;
                seekProgress = seekProgress * 1000;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekProgress);
            }
        });
    }

    private void prepareSong(Song song){
        if (firstLaunch) {
            firstLaunch = false;
        }
        showIntersititial(true);
        Config.isPlaying = true;
        pb_main_loader.setVisibility(View.VISIBLE);
        iv_play.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.selector_play));
        tb_title.setText(song.getTitle());
        tb_title.startScroll();

        AssetFileDescriptor afd = null;
        try {
            afd = getAssets().openFd("data/"+song.getStreamUrl());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.reset();
        try {
            if (afd != null) {
                mediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                mmr.release();

                long currentSongLength = Long.parseLong(duration);
                total_duration.setText(Utility.milliSecondsToTimer(currentSongLength));
            }
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playProgress() {
        iv_play.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.selector_pause));
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                mSeekBar.setProgress(mCurrentPosition);
                tv_time.setText(Utility.milliSecondsToTimer(mediaPlayer.getCurrentPosition()));
                mHandler.postDelayed(this, 1000);
            }
        });
    }

    static void randomNum() {
        int number = 5;
        counter = new Random().nextInt(number);
    }

    public void showIntersititial(boolean count) {
        if(count){
            mCount++;
            Log.d("mcount",Integer.toString(mCount));
            Log.d("counter",Integer.toString(counter));
            if(counter <= mCount) {
                if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                    Appodeal.show(this, Appodeal.INTERSTITIAL);
                    mCount=0;
                    intShow = true;
                }else mCount--;
            }
        } else if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
            Appodeal.show(this, Appodeal.INTERSTITIAL);
        }
    }

    public void togglePlay(MediaPlayer mp){
        if(mp.isPlaying()){
            mp.stop();
            mp.reset();

        }else{
            pb_main_loader.setVisibility(View.GONE);
            tb_title.setVisibility(View.VISIBLE);
            mSeekBar.setMax(mp.getDuration() / 1000);
            if (intShow) {
                Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
                    @Override
                    public void onInterstitialLoaded(boolean b) {

                    }

                    @Override
                    public void onInterstitialFailedToLoad() {

                    }

                    @Override
                    public void onInterstitialShown() {
                        intShow = false;
                    }

                    @Override
                    public void onInterstitialShowFailed() {

                    }

                    @Override
                    public void onInterstitialClicked() {

                    }

                    @Override
                    public void onInterstitialClosed() {
                        mp.start();
                        playProgress();
                        randomNum();
                    }

                    @Override
                    public void onInterstitialExpired() {

                    }
                });
            } else {
                mp.start();
                playProgress();
            }
        }
    }

    private void initializeViews(){

        tb_title = findViewById(R.id.tb_title);
        iv_play = findViewById(R.id.iv_play);
        iv_next = findViewById(R.id.iv_next);
        iv_previous = findViewById(R.id.iv_previous);
        total_duration = findViewById(R.id.total_duration);
        pb_main_loader = findViewById(R.id.pb_main_loader);
        recycler = findViewById(R.id.recylerView);
        mSeekBar = findViewById(R.id.seekbar);
        tv_time = findViewById(R.id.tv_time);
        iv_share = findViewById(R.id.share);
        iv_info = findViewById(R.id.about);
        Appodeal.isLoaded(Appodeal.BANNER);
    }


    public void getSongListMain(){
        getSongList(new ApiInterface() {
            @Override
            public void onSuccess(ArrayList<Song> songs) {
                currentIndex = 0;
                songList.clear();
                songList.addAll(songs);
                mAdapter.notifyDataSetChanged();
                mAdapter.setSelectedPosition(0);
            }
            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void getSongList(final ApiInterface callback){
        try {
            JSONArray jsonArray = new JSONArray(loadJson.loadJSONFromAsset(this, JSON_ID));
            ArrayList<Song> songs = new ArrayList<>();
            if (jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject songObject = jsonArray.getJSONObject(i);
                        String artist = songObject.getString("id");
                        String title = songObject.getString("lagu");
                        String streamUrl = songObject.getString("source");

                        Song song = new Song(artist, title, streamUrl);
                        songs.add(song);

                    } catch (JSONException e) {
                        String TAG = "App";
                        Log.d(TAG, "onResponse: " + e.getMessage());
                        callback.onError("An error has occurred");
                        e.printStackTrace();
                    }
                }
                callback.onSuccess(songs);

            } else {
                callback.onError("No songs found");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void changeSelectedSong(int index){
        mAdapter.notifyItemChanged(mAdapter.getSelectedPosition());
        currentIndex = index;
        mAdapter.setSelectedPosition(currentIndex);
        mAdapter.notifyItemChanged(currentIndex);
    }

    private void pushPlay(){
        iv_play.setOnClickListener(v -> {

            if(mediaPlayer.isPlaying() && mediaPlayer != null){
                iv_play.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.selector_play));
                mediaPlayer.pause();
                tv_time.startAnimation(anim);
                tb_title.pauseScroll();
                mBlinking = true;
            }else{
                if(firstLaunch){
                    Song song = songList.get(0);
                    changeSelectedSong(0);
                    prepareSong(song);
                }else{
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
                    firstLaunch = false;
                }
                if (tb_title.isPaused()) {
                    tb_title.resumeScroll();
                }
                iv_play.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.selector_pause));
                tv_time.clearAnimation();
                tv_time.setAlpha(1.0f);
                mBlinking = false;

            }

        });
    }

    private void pushShare() {
        iv_share.setOnClickListener(v -> {
            Utility.animateButton(iv_share);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = getString(R.string.share)+" \n"+"https://play.google.com/store/apps/details?id="+getPackageName();
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name)+" Application");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        });
    }

    private void pushInfo() {
        iv_info.setOnClickListener(v -> {
            Utility.animateButton(iv_info);
            AboutFragment aboutFragment = new AboutFragment();
            aboutFragment.show(fm, "about fragment");
        });
    }

    private void btnPrevious(){
        iv_previous.setOnClickListener(v -> {
            firstLaunch = false;
            if(mediaPlayer != null){
                if(currentIndex - 1 >= 0){
                    Song previous = songList.get(currentIndex - 1);
                    changeSelectedSong(currentIndex - 1);
                    prepareSong(previous);
                }else{
                    changeSelectedSong(songList.size() - 1);
                    prepareSong(songList.get(songList.size() - 1));
                }
            }
        });
    }

    private void btnNext(){
        iv_next.setOnClickListener(v -> {
            firstLaunch = false;
            if(mediaPlayer != null){
                if(currentIndex + 1 < songList.size()){
                    Song next = songList.get(currentIndex + 1);
                    changeSelectedSong(currentIndex + 1);
                    prepareSong(next);
                }else{
                    changeSelectedSong(0);
                    prepareSong(songList.get(0));
                }

            }
        });

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Confirm quit");
        builder.setMessage("Are you sure?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            //if user pressed "yes", then he is allowed to exit from application
            if (mediaPlayer != null) {
                if(mediaPlayer.isPlaying()){
                    try {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mHandler.removeCallbacksAndMessages(null);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
            finishAffinity();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            //if user select "No", just cancel this dialog and continue with app
            dialog.cancel();
        });
        builder.show();
    }
}
