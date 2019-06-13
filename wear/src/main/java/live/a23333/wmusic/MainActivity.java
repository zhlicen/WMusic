package live.a23333.wmusic;


import android.media.session.PlaybackState;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.Toast;

import com.google.devrel.wcl.WearManager;




public class MainActivity extends FragmentActivity implements
       WatchViewStub.OnLayoutInflatedListener {
    static public WMediaPlayer mPlayer;
    static public MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(this);
    }

    @Override
    public void onLayoutInflated(WatchViewStub stub) {
        ViewPager viewPager = (ViewPager)findViewById(R.id.view_page);
        viewPager.setOffscreenPageLimit(3);
        WearFragmentAdapter adapter = new WearFragmentAdapter( getSupportFragmentManager(),
                this);
        viewPager.setAdapter(adapter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            if(mPlayer.mPlaybackState.getState() != PlaybackState.STATE_PLAYING) {
                quitApp();
                finishAndRemoveTask();
            }
        }
        catch (Exception ex) {
            quitApp();
            finishAndRemoveTask();
        }

    }

    public void quitApp() {
        if(mPlayer != null) {
            mPlayer.setPlayStateChangeCb(null);
            mPlayer.stop();
            mPlayer = null;
        }
        // WearManager.deinitialize();
        Intent wmIntent = new Intent(this, WMediaPlayer.WMediaService.class);
        stopService(wmIntent);
        Intent playerIntent = new Intent(this, WMediaService.class);
        stopService(playerIntent);
        Intent wwIntent = new Intent(this, WWListener.class);
        stopService(wwIntent);
    }


}
