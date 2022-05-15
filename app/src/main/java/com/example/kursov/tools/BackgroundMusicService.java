package com.example.kursov.tools;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.example.kursov.R;

public class BackgroundMusicService extends Service {
    private static MediaPlayer player;

    @Nullable
    @Override
    public IBinder onBind(Intent arg0) {

        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        player = MediaPlayer.create(this, R.raw.background_music);
        player.setLooping(true);
        player.setVolume(1,1);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        player.start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        player.stop();
        player.release();
    }

    public static void mutePlayer() {
        player.setVolume(0, 0);
    }

    public static void unmutePlayer() {
        player.setVolume(1, 1);
    }

    public static void pausePlayer() {
        player.pause();
    }

    public static void resumePlayer() {
        player.start();
    }
}
