package com.piixdart.mscoffln;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.InterstitialCallbacks;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.piixdart.mscoffln.Adapter.SongAdapter;
import com.piixdart.mscoffln.Model.Song;
import com.piixdart.mscoffln.Utility.LoadJson;
import com.piixdart.mscoffln.Utility.ScrollTextView;
import com.piixdart.mscoffln.Utility.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static com.piixdart.mscoffln.Config.JSON_ID;


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
    int seekForward = 5000;
    int seekBackward = 5000;

    Looper looper = Looper.getMainLooper();
    final Handler mHandler = new Handler(looper);
    private ScrollTextView tb_title;
    Animation anim = new AlphaAnimation(0.0f, 1.0f);
    boolean mBlinking = false;
    FragmentManager fm = getSupportFragmentManager();

    private static final int MY_REQUEST_CODE = 999 ;
    AppUpdateManager appUpdateManager;
    Task<AppUpdateInfo> appUpdateInfoTask;
    InstallStateUpdatedListener listener;
    ReviewInfo reviewInfo;
    ReviewManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Appodeal.initialize(this,Config.appId, Appodeal.BANNER | Appodeal.INTERSTITIAL);
        Appodeal.disableLocationPermissionCheck();
        Appodeal.setTesting(true);
        Appodeal.cache(this, Appodeal.INTERSTITIAL);
        appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);
        initializeViews();
        versionCheck();

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
        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.show(this, Appodeal.BANNER_VIEW);
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
        btnBackward();
        btnForward();
        pushShare();
        pushInfo();
        //initDrawer();
        getSongListMain();
        Review();
    }
    //Version check
    private void versionCheck(){
        appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    listener = state -> {
                        if (state.installStatus() == InstallStatus.DOWNLOADED) {
                            popupSnackbarForCompleteUpdate();
                        }

                        if (state.installStatus() == InstallStatus.INSTALLED){
                            appUpdateManager.unregisterListener(listener);
                        }
                    };

                    appUpdateManager.registerListener(listener);
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.IMMEDIATE,
                            this,
                            MY_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Update success! Result Code: " + resultCode, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Update Failed! Result Code: " + resultCode, Toast.LENGTH_LONG).show();
                versionCheck();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.main_layout),
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.setActionTextColor(
                getResources().getColor(R.color.black));
        snackbar.show();
    }

    //Review methode
    private void Review(){
        manager = ReviewManagerFactory.create(this);
        manager.requestReviewFlow().addOnCompleteListener( task -> {
            if(task.isSuccessful()){
                reviewInfo = task.getResult();
                manager.launchReviewFlow(MainActivity.this, reviewInfo)
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Rating Failed", Toast.LENGTH_SHORT).show())
                        .addOnCompleteListener( task1 -> Toast.makeText(MainActivity.this, "Review Completed, Thank You!",
                        Toast.LENGTH_SHORT).show());
            }

        }).addOnFailureListener(e -> Toast.makeText(MainActivity.this, "In-App Request Failed", Toast.LENGTH_SHORT).show());
    }

    public static Intent getIntent(Context context, boolean consent) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(CONSENT, consent);
        return intent;
    }

    //music seekbar
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

                    }

                    @Override
                    public void onInterstitialShowFailed() {
                        intShow = false;
                    }

                    @Override
                    public void onInterstitialClicked() {

                    }

                    @Override
                    public void onInterstitialClosed() {
                        mp.start();
                        playProgress();
                        intShow = false;
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
            JSONArray jsonArray = new JSONArray(LoadJson.loadJSONFromAsset(this, JSON_ID));
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

    private void btnBackward(){
        iv_previous.setOnClickListener(v -> {
            int currentPos = mediaPlayer.getCurrentPosition();
            mediaPlayer.seekTo(Math.max(currentPos - seekBackward, 0));
        });
    }

    private void btnForward(){
        iv_next.setOnClickListener(v -> {
            int currentPos = mediaPlayer.getCurrentPosition();
            mediaPlayer.seekTo(Math.min(currentPos + seekForward, mediaPlayer.getDuration()));
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
