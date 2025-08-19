package com.example.facedetector;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
public class SoundPoolHelper {
    private static final String TAG = "SoundPoolHelper";
    private static final long DEFAULT_THROTTLE_MS = 2000; // giống UIIntervalTime()

    private final SoundPool pool;
    private final Map<String, Integer> externalMap = new HashMap<>();
    private final Context appCtx;

    private long lastPlayAt = 0L;
    private long throttleMs = DEFAULT_THROTTLE_MS;

    // public để gọi giống app gốc
    public int soundSuccess = 0;
    public int soundFailed  = 0;

    public SoundPoolHelper(Context ctx) {
        this.appCtx = ctx.getApplicationContext();

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        pool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();

        // Thử load 2 âm mặc định trong res/raw (nếu bạn đặt đúng tên file)
        int successId = getResIdByName("sound_success");
        if (successId != 0) soundSuccess = pool.load(appCtx, successId, 1);

        int failedId = getResIdByName("sound_failed");
        if (failedId != 0)  soundFailed  = pool.load(appCtx, failedId, 1);
    }

    /** Tuỳ chọn: đổi thời gian chặn spam (ms) */
    public void setThrottleMs(long ms) { this.throttleMs = Math.max(0, ms); }

    /** Phát theo ID (success/failed hoặc ID tự load) */
    public void playSoundBeep(int soundId) {
        if (soundId == 0) return;
        if (isThrottled()) return;
        pool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    /** Phát theo tên file đã load từ thư mục ngoài (vd "ok.wav") */
    public boolean playSoundBeepByFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        Integer id = externalMap.get(fileName);
        if (id == null || id == 0) return false;
        if (isThrottled()) return false;
        pool.play(id, 1f, 1f, 1, 0, 1f);
        return true;
    }

    /** Quét & load tất cả .wav trong thư mục (khuyên: /sdcard/pakkaGoat/face8sound) */
    public void loadExternalWavDir(String absoluteDirPath) {
        try {
            File dir = new File(absoluteDirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                Log.w(TAG, "External dir not found: " + absoluteDirPath);
                return;
            }
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
                    int id = pool.load(f.getAbsolutePath(), 1);
                    externalMap.put(f.getName(), id);
                }
            }
            Log.i(TAG, "Loaded external wav: " + externalMap.keySet());
        } catch (Exception e) {
            Log.e(TAG, "loadExternalWavDir error", e);
        }
    }

    /** Giải phóng tài nguyên */
    public void release() {
        try { pool.release(); } catch (Exception ignore) {}
        externalMap.clear();
    }

    // ---------- helpers ----------

    private boolean isThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastPlayAt < throttleMs) return true;
        lastPlayAt = now;
        return false;
    }

    private int getResIdByName(String rawName) {
        try {
            return appCtx.getResources().getIdentifier(rawName, "raw", appCtx.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }
}
