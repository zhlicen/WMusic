package live.a23333.wmusic;

import android.util.Log;

import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.devrel.wcl.WclWearableListenerService;


import java.util.List;

/**
 * Created by Samuel Zhou on 2017/5/23.
 */

public class WWListener extends WclWearableListenerService {

    @Override
    public void onCreate() {
        WearHelper.initialize(getApplicationContext());
        super.onCreate();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }
}
