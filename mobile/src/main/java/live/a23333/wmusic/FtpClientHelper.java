package live.a23333.wmusic;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import live.a23333.wmusic.WearHelper.MsgListFileNtf.FileInfo;

/**
 * Created by zhlic on 2017/8/4.
 */

public class FtpClientHelper {
    public static Context mContext;
    public static boolean ftpMode = false;
    public static String connectedName;
    public static class ScanNetworkItem {
        public String ip;
        public String hostName;
    }

    public interface ScanNetworkResultListener{
        void OnResult(ArrayList<ScanNetworkItem> result);
    }
    public static void scanLocalNetwork(final String ip, final int port, final ScanNetworkResultListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ExecutorService es = Executors.newFixedThreadPool(20);
                final int timeout = 400;
                String ipSeg = ip.substring(0, ip.lastIndexOf("."));
                String segsStr = ipSeg.substring(ipSeg.lastIndexOf(".") + 1, ipSeg.length());
                final int segs = Integer.parseInt(segsStr);
                ipSeg = ipSeg.substring(0, ipSeg.lastIndexOf("."));

                final List<Future<String>> futures = new ArrayList<>();
                int delta = 0;
                for(int ipFirst = (segs <= delta ? 0 : segs - delta); ipFirst <= segs + delta; ipFirst++) {
                    for (int ipLast = 1; ipLast <= 255; ipLast++) {
                        String scanIP = ipSeg + "." + ipFirst + "." + ipLast;
                        futures.add(portIsOpen(es, scanIP, port, timeout));
                    }
                }
                es.shutdown();
                ArrayList<ScanNetworkItem> result = new ArrayList<>();
                for (final Future<String> f : futures) {
                    try {
                        String avaIp = f.get();
                        if(!avaIp.equals("")) {

                            String host = "";
                            try {
                                InetAddress addr = InetAddress.getByName(avaIp);
                                host = addr.getHostName();
                            }
                            catch (Exception ex) {
                                Log.d("FtpClientHelper", ex.getMessage());
                            }

                            ScanNetworkItem item = new ScanNetworkItem();
                            if(!host.isEmpty()) {
                                item.hostName = host;
                            }
                            item.ip = avaIp;
                            result.add(item);
                        }
                    }
                    catch (Exception ex) {

                    }
                }
                if(listener != null){
                    listener.OnResult(result);
                }
            }
        }).start();

    }


    public static String getWifiIP() {
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null) {
            return "";
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo == null) {
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
        }
        return "";
    }

    private static Future<String> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
        return es.submit(new Callable<String>() {
            @Override public String call() {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    return ip;
                } catch (Exception ex) {
                    return "";
                }
            }
        });
    }

    public static class FtpClientW {
        static FTPClient client;
        public interface ConnectListener {
            void OnResult(final Exception ex);
        }
        public static void connect(final String addr, final int port, final String username, final String pwd,
                                   final ConnectListener listener) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(client != null) {
                        listener.OnResult(null);
                        return;
                    }
                    try {
                        client = new FTPClient();
                        client.connect(addr, port);
                        boolean login = client.login(username, pwd);
                        client.setControlEncoding("UTF-8");
                        connectedName = addr;
                        ftpMode = true;
                        client.enterLocalPassiveMode();
                        if(listener != null) {
                            listener.OnResult(null);
                        }
                    }
                    catch (Exception ex) {
                        if(listener != null) {
                            listener.OnResult(ex);
                        }
                    }
                }
            }).start();

        }

        public interface LsFileListener {
            void OnResult(final ArrayList<FileInfo> files,final Exception ex);
        }
        public static void lsFile(final String remotePath, final LsFileListener listener) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        checkConnected();
                        client.setDataTimeout(3000);
                        client.enterLocalPassiveMode();
                        client.changeWorkingDirectory(remotePath);
                        FTPFile files[] = client.listFiles();
                        ArrayList<FileInfo> infos = new ArrayList<>();
                        for (FTPFile file : files) {
                            if(!file.isDirectory()) {
                                FileInfo info = new FileInfo();
                                info.fileName = file.getName();
                                info.filePath = remotePath + "/" + file.getName();
                                info.fileSize = (float) file.getSize() / 1024 / 1024;
                                infos.add(info);
                            }
                        }
                        if(listener != null) {
                            listener.OnResult(infos, null);
                        }
                    }
                    catch (Exception ex) {
                        if(listener != null) {
                            listener.OnResult(null, ex);
                        }
                    }
                }
            }).start();
        }


        public interface SendFileListener {
            void OnResult(final String file, final Exception ex);
        }
        public static void sendFile(final String localFile, final String remotePath,
                final SendFileListener listener) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        checkConnected();
                        Thread.sleep(500);
                        String fileName = localFile.substring(localFile.lastIndexOf("/") + 1,
                                localFile.length());
                        client.enterLocalPassiveMode();
                        client.changeWorkingDirectory(remotePath);
                        client.setFileType(FTP.BINARY_FILE_TYPE);
                        BufferedInputStream buffIn;
                        buffIn = new BufferedInputStream(new FileInputStream(localFile));
                        fileName = new String(fileName.getBytes("utf-8"), "iso-8859-1");
                        if(!client.storeFile(fileName, buffIn)) {
                            throw new Exception("send failed");
                        }
                        buffIn.close();
                        if(listener != null) {
                            listener.OnResult(localFile, null);
                        }
                    } catch (Exception ex) {
                        if(listener != null) {
                            listener.OnResult(localFile, ex);
                        }
                    }
                }
            }).start();


        }

        private static void checkConnected() throws Exception {
            if(client == null) {
                ftpMode = false;
                connectedName = "";
                throw new Exception("not connected");
            }
            if(!client.isConnected()) {
                client = null;
                ftpMode = false;
                connectedName = "";
                throw new Exception("not connected");
            }
        }

        public interface DelFileListener {
            void OnResult(final String file, final Exception ex);
        }
        public static void delFile(final String remoteFile, final DelFileListener listener) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        checkConnected();
                        Thread.sleep(500);
                        String path = remoteFile.substring(0, remoteFile.lastIndexOf("/"));
                        String name = remoteFile.substring(remoteFile.lastIndexOf("/") + 1, remoteFile.length());
                        client.enterLocalPassiveMode();
                        client.changeWorkingDirectory(path);
                        client.setFileType(FTP.BINARY_FILE_TYPE);
                        name = new String(name.getBytes("utf-8"), "iso-8859-1");
                        boolean result = client.deleteFile(name);

                        Exception ex = null;
                        if(!result) {
                            ex = new Exception("delete failed");
                        }
                        if(listener != null) {
                            listener.OnResult(remoteFile, ex);
                        }
                    }
                    catch (Exception ex) {
                        if(listener != null) {
                            listener.OnResult(remoteFile, null);
                        }
                    }
                }
            }).start();
        }
        public interface DisconnectListener {
            void OnResult(final Exception ex);
        }
        public static void disconnect(final DisconnectListener listener) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        checkConnected();
                        client.logout();
                        client.disconnect();
                        ftpMode = false;
                        connectedName = "";
                        client = null;
                        if(listener != null) {
                            listener.OnResult(null);
                        }
                    }
                    catch (Exception ex) {
                        ftpMode = false;
                        connectedName = "";
                        client = null;
                        listener.OnResult(ex);
                    }
                }
            }).start();
        }
    }
}
