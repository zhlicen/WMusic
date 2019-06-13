package live.a23333.wmusic;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.net.InetAddress;

/**
 * Created by zhlic on 2017/8/3.
 */

public class FtpServerHelper {
    static Context mContext;
    static FtpServer server;
    static int bluetoothState = -1;

    static void startFtpServer() throws Exception {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(12358);
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());

        UserManager um = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        user.setName("wmusic");
        user.setPassword("*#06#");
        java.util.List<Authority> authorities = new java.util.ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        user.setHomeDirectory("/sdcard/");
        um.save(user);
        serverFactory.setUserManager(um);
        serverFactory.addListener("default", factory.createListener());
        server = serverFactory.createServer();

        server.start();
    }

    static void stopFtpServer() {
        if(server != null) {
            server.stop();
            server = null;
        }
    }

    public static String getWifiIP() {
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null) {
            Toast.makeText(mContext, "Can not get wifi manager", Toast.LENGTH_SHORT).show();
            return "";
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo == null) {
            Toast.makeText(mContext, "Can not get wifi info", Toast.LENGTH_SHORT).show();
            return "";
        }
        int addr = wifiInfo.getIpAddress();
        if(addr == 0) {
            return "";
        }
        byte[] addrAsBytes = { (byte) (addr), (byte) (addr >> 8),
                (byte) (addr >> 16), (byte) (addr >> 24) };

        try {
            InetAddress address = InetAddress.getByAddress(addrAsBytes);
            return address.getHostAddress();
        }
        catch (Exception ex) {
            Toast.makeText(mContext, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return "";
    }

    public static void closeBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            bluetoothState = 1;
            bluetoothAdapter.disable();
            Toast.makeText(mContext, mContext.getString(R.string.bluetooth_closed), Toast.LENGTH_SHORT).show();
        }
    }

    public static void restoreBluetooth() {
        if(bluetoothState == 1) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.enable();
            bluetoothState = -1;
            Toast.makeText(mContext, mContext.getString(R.string.bluetooth_restored), Toast.LENGTH_SHORT).show();
        }
    }

    public static void startWifiActivity() {
        mContext.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }
}
