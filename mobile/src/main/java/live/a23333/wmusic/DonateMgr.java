package live.a23333.wmusic;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import static android.net.Uri.parse;

/**
 * Created by zhlic on 5/21/2017.
 */

public class DonateMgr {
    public static void startChineseDonate(Context context){
        String url = "https://zhlicen.github.com/app/donate/donate.htm";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(parse(url));
        context.startActivity(i);
    }

    public static void startGooglePlayDonate(){
        if(mActivity == null) {
            return;
        }
        try {
            String sku = "donate";
            Bundle buyIntentBundle = mService.getBuyIntent(3, mActivity.getPackageName(),
                    sku, "inapp", "default");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

            mActivity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        }
        catch (Exception ex){
            Toast.makeText(mActivity, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    public static void initPlayDonate(Activity activity) {
        mActivity = activity;
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        activity.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    public static void uninitPlayDonate() {
        mActivity.unbindService(mServiceConn);
    }

    static IInAppBillingService mService;
    static Activity mActivity;

    static ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };
}
