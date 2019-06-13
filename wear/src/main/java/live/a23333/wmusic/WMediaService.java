package live.a23333.wmusic;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Samuel Zhou on 2017/6/13.
 */

public class WMediaService extends Service {
    public static final String ACTION_INIT = "live.a23333.wmusic.INIT";
    public static final String ACTION_REINIT = "live.a23333.wmusic.REINIT";
    public static WMediaPlayer mPlayer = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // mPlayer = null;
    }

    private final IBinder iBinder = new LocalBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(mPlayer == null) {
            try {
                mPlayer = new WMediaPlayer();
                mPlayer.initMediaPlayer(this);
            }
            catch (Exception ex) {
                Log.d("WMediaService", ex.getMessage());
            }
        }
        return iBinder;
    }



    public class LocalBinder extends Binder {
        public WMediaService getService() {
            return WMediaService.this;
        }
    }
}