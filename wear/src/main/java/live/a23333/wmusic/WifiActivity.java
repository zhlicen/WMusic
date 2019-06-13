package live.a23333.wmusic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class WifiActivity extends Activity {

    Button mBtnPrepare;
    boolean mPrepared;
    PowerManager.WakeLock mWakeLock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        FtpServerHelper.mContext = this;
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                try {
                    MainActivity.instance.quitApp();
                }
                catch (Exception ex) {

                }
                if(android.os.Build.VERSION.SDK_INT > 23) {
                    try {
                        FtpServerHelper.closeBluetooth();
                    }
                    catch (Exception ex) {

                    }
                }
                mBtnPrepare = (Button)findViewById(R.id.btn_prepare);
                mBtnPrepare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mPrepared) {
                            FtpServerHelper.stopFtpServer();
                            updateStatus(false,"");
                        }
                        else {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final String ip = FtpServerHelper.getWifiIP();
                                    if(ip.isEmpty()) {

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(WifiActivity.this, getString(R.string.wifi_not_connected), Toast.LENGTH_SHORT).show();
                                                FtpServerHelper.startWifiActivity();
                                            }
                                        });
                                        return;
                                    }
                                    try {
                                        FtpServerHelper.startFtpServer();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                updateStatus(true, ip);
                                            }
                                        });
                                    }
                                    catch (final Exception ex) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(WifiActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    }
                                }
                            }).start();
                        }
                    }
                });
                updateStatus(false, "");
            }
        });

    }

    void updateStatus(boolean ready, String ip) {
        mPrepared = ready;
        if(ready) {
            mBtnPrepare.setText(
                    getString(R.string.wifi_prepared_ip) + ip + "\n"
                            + getString(R.string.wifi_prepared));
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Wifi Transfer");
            mWakeLock.acquire();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            mBtnPrepare.setText(
                    getString(R.string.wifi_not_prepared));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(mWakeLock != null) {
                mWakeLock.release();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FtpServerHelper.stopFtpServer();
        if(mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if(android.os.Build.VERSION.SDK_INT > 23) {
            try {
                FtpServerHelper.restoreBluetooth();
            }
            catch (Exception ex) {

            }
        }
        System.exit(0);
    }
}
