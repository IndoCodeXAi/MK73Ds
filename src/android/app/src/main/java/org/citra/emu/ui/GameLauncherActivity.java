package org.citra.emu.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

import org.citra.emu.utils.PermissionsHandler;

/**
 * Pintu masuk tunggal. Saat dibuka, aplikasi otomatis membuat folder game di
 * penyimpanan internal (folder milik aplikasi itu sendiri):
 *   /storage/emulated/0/Android/data/<paket>/files/MarioKart7/
 * File pertama berekstensi .3ds/.cci/.cxi/.cia/.3dsx/.app di folder itu
 * langsung dijalankan. Kalau belum ada, ditampilkan petunjuk letak folder.
 */
public final class GameLauncherActivity extends Activity {
    private static final String GAME_FOLDER = "MarioKart7";
    private static final String[] EXTENSIONS = {".3ds", ".cci", ".cxi", ".cia", ".3dsx", ".app"};

    @Override
    protected void onResume() {
        super.onResume();
        tryLaunch();
    }

    private File getGameDir() {
        File base = getExternalFilesDir(null);
        if (base == null) {
            base = getFilesDir();
        }
        File dir = new File(base, GAME_FOLDER);
        if (!dir.isDirectory()) {
            dir.mkdirs(); // dibuat otomatis
        }
        return dir;
    }

    private void tryLaunch() {
        // Android 9 ke bawah butuh izin tulis biasa (untuk inisialisasi emulator).
        // Android 10 ke atas: langsung true, tanpa izin apa pun.
        if (!PermissionsHandler.checkWritePermission(this)) {
            return; // menunggu hasil izin
        }

        File dir = getGameDir();
        File game = findGame(dir);
        if (game == null) {
            showMessage(dir);
            return;
        }

        Intent intent = new Intent(this, EmulationActivity.class);
        intent.putExtra(EmulationActivity.EXTRA_GAME_PATH, game.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private File findGame(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            String name = f.getName().toLowerCase(Locale.ROOT);
            for (String ext : EXTENSIONS) {
                if (name.endsWith(ext)) {
                    return f;
                }
            }
        }
        return null;
    }

    private void showMessage(File dir) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#111111"));
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER);
        text.setText("Folder game sudah dibuat otomatis di penyimpanan internal:\n\n" +
                     dir.getPath() +
                     "\n\nTaruh file game (.3ds / .cci / .cxi / .cia) ke folder itu, " +
                     "lalu tekan \"Coba lagi\".");
        root.addView(text);

        Button retry = new Button(this);
        retry.setText("Coba lagi");
        retry.setOnClickListener(v -> tryLaunch());
        root.addView(retry);

        setContentView(root);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PermissionsHandler.REQUEST_CODE_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tryLaunch();
            } else {
                showMessage(getGameDir());
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
