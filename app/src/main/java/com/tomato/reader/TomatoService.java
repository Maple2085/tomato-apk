package com.tomato.reader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TomatoService extends Service {

    private static final String TAG = "TomatoService";
    private static final String CHANNEL_ID = "tnd";
    private static final int NOTIF_ID = 1001;
    static final String ACTION_STOP = "com.tomato.reader.action.STOP";
    private static final int PORT = 18423;

    private volatile Process proc;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            shutdown();
            return START_NOT_STICKY;
        }
        startForeground(NOTIF_ID, buildNotification("正在启动…"));
        if (proc == null) {
            new Thread(this::launchBinary, "tnd-launch").start();
        }
        return START_STICKY;
    }

    private void launchBinary() {
        try {
            File bin = new File(getFilesDir(), "tnd");
            if (!bin.exists() || bin.length() < 1024) {
                copyAsset("tnd", bin);
            }
            bin.setExecutable(true, false);
            if (!bin.canExecute()) {
                Log.w(TAG, "file not executable: " + bin);
            }

            File data = new File(getFilesDir(), "data");
            if (!data.exists()) {
                data.mkdirs();
            }
            File cfg = new File(data, "config.yml");
            if (!cfg.exists()) {
                copyAsset("config.yml", cfg);
            }
            normalizeSavePath(cfg);

            ProcessBuilder pb = new ProcessBuilder(
                    bin.getAbsolutePath(), "--server", "--data-dir", data.getAbsolutePath());
            pb.directory(getFilesDir());
            pb.redirectErrorStream(true);
            pb.environment().put("TOMATO_WEB_ADDR", "127.0.0.1:" + PORT);
            pb.environment().put("HOME", getFilesDir().getAbsolutePath());
            pb.environment().put("TMPDIR", getCacheDir().getAbsolutePath());

            Process p = pb.start();
            proc = p;
            updateNotification("运行中 · http://127.0.0.1:" + PORT + "/");

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                Log.i(TAG, line);
            }
            int code = p.waitFor();
            Log.i(TAG, "server exited with " + code);
            proc = null;
            stopSelf();
        } catch (Throwable t) {
            Log.e(TAG, "launch failed", t);
            updateNotification("启动失败：" + t.getMessage());
        }
    }

    /**
     * 老版本生成的 config.yml 里 save_path 是空的（会存到应用私有目录，阅读器看不到）。
     * 启动前统一改成一个外部可访问的目录。
     */
    private void normalizeSavePath(File cfg) {
        try {
            String txt = readAll(cfg);
            String want = pickSaveDir();
            String patched = txt
                    .replaceAll("(?m)^save_path:\\s*''\\s*$", "save_path: " + want)
                    .replaceAll("(?m)^save_path:\\s*\"\"\\s*$", "save_path: " + want);
            if (!patched.equals(txt)) {
                writeAll(cfg, patched);
                Log.i(TAG, "save_path set to " + want);
            }
        } catch (Exception e) {
            Log.w(TAG, "normalize config failed", e);
        }
    }

    private String pickSaveDir() {
        File dir = new File("/sdcard/Download/TomatoNovel");
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (dir.isDirectory() && dir.canWrite()) {
                return dir.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }
        File fb = new File(getFilesDir(), "books");
        fb.mkdirs();
        return fb.getAbsolutePath();
    }

    private String readAll(File f) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        in.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

    private void writeAll(File f, String content) throws Exception {
        java.io.FileOutputStream out = new java.io.FileOutputStream(f);
        out.write(content.getBytes("UTF-8"));
        out.getFD().sync();
        out.close();
    }

    private void copyAsset(String name, File dst) throws Exception {
        InputStream in = getAssets().open(name);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        out.getFD().sync();
        out.close();
        in.close();
    }

    private void shutdown() {
        Process p = proc;
        proc = null;
        if (p != null) {
            p.destroy();
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        shutdown();
        super.onDestroy();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "番茄下载器", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private Notification buildNotification(String text) {
        ensureChannel();
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open, flags);

        Intent stop = new Intent(this, TomatoService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 1, stop, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        b.setContentTitle("番茄小说下载器")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setContentIntent(openPi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPi);
        return b.build();
    }

    private void updateNotification(String text) {
        NotificationManager nm =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, buildNotification(text));
        }
    }
}
