package live.a23333.wmusic;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.devrel.wcl.WearManager;
import com.google.devrel.wcl.filters.NearbyFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Created by zhlic on 5/17/2017.
 */

public class WearHelper {
    private static Context sContext;



    public static class Serializer {

        public static byte[] serialize(Object obj) throws IOException {
            try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
                try(ObjectOutputStream o = new ObjectOutputStream(b)){
                    o.writeObject(obj);
                }
                return b.toByteArray();
            }
        }

        public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
            try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
                try(ObjectInputStream o = new ObjectInputStream(b)){
                    return o.readObject();
                }
            }
        }

    }

    public static void initialize(Context context) {
        try {
            if (WearManager.getInstance() != null)
                return;
        }
        catch (Exception ex){

        }
        sContext = context;
        WearManager wm = WearManager.initialize(sContext);
        File dir = new File("/sdcard/Music");
        if(!dir.exists()) {
            dir.mkdirs();
        }
        wm.fileSavePath = dir.getAbsolutePath();
        wm.addWearConsumer(new WearConsumer());
    }

    @Nullable
    public static Node getNearByNode() {
        WearManager wearManager = WearManager.getInstance();
        Set<Node> nodes = wearManager.getConnectedNodes();
        if (nodes.isEmpty()) {
            return null;
        }
        Set<Node> nearbyNodes = new NearbyFilter().filterNodes(nodes);
        if (!nearbyNodes.isEmpty()) {
            return nearbyNodes.iterator().next();
        }
        return null;
    }


    // MessageDefs
    public static String SAY_HELLO = "hello_wear";
    public static String MSG_LIST_FILE_REQ = "list_file_req";
    public static class MsgListFileReq implements Serializable {
        public String path;
    }

    public static String MSG_LIST_FILE_NTF = "list_file_ntf";
    public static class MsgListFileNtf implements Serializable {
        public static class FileInfo implements Serializable {
            public String fileName;
            public String filePath;
            public float fileSize;
        }
        public List<FileInfo> fileList;
    }

    public static String MSG_DEL_FILE_REQ = "del_file_req";
    public static class MsgDelFile  implements Serializable {
        public String filePath;
    }

    public static String MSG_DEL_FILE_RESULT = "del_file_ret";
    public static class MsgDelFileRet implements Serializable {
        public boolean isSuccess;
        public String filePath;
        public String msg;
    }
    public static String MSG_FILE_RECEIVED_NTF = "file_received_ntf";
    public static class MsgFileReceived implements Serializable {
        public String filePath;
    }


    protected static class FileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return (name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".wav")
                    || name.endsWith(".ogg") || name.endsWith(".aac") || name.endsWith(".flac"));
        }
    }

    public static void sendFileList(String nodeId, MsgListFileNtf list) throws IOException {
        WearManager wm = WearManager.getInstance();
        byte[] body = Serializer.serialize(list);
        wm.sendMessage(nodeId, MSG_LIST_FILE_NTF, body);
    }

    public static void sendDelFileRet(String nodeId, MsgDelFileRet ret) throws IOException {
        WearManager wm = WearManager.getInstance();
        byte[] body = Serializer.serialize(ret);
        wm.sendMessage(nodeId, MSG_DEL_FILE_RESULT, body);
    }

    public static void sendFileReceived(String nodeId, MsgFileReceived ret) throws IOException {
        try {
            MainActivity.mPlayer.stop();
        }
        catch (Exception ex) {

        }
        WearManager wm = WearManager.getInstance();
        byte[] body = Serializer.serialize(ret);
        wm.sendMessage(nodeId, MSG_FILE_RECEIVED_NTF, body);
    }
    public static String userPath;
    public static void notifyFileList(String path, String nodeId){
        userPath = path;
        File f = new File(path);
        File files[] = f.listFiles(new FileExtensionFilter());
        Arrays.sort(files);
        MsgListFileNtf list = new MsgListFileNtf();
        if (files.length == 0) {
            try {
                sendFileList(nodeId, list);
            }
            catch (Exception ex) {
                Log.e("WearHelper", ex.getMessage());
            }
            return;
        }
        list.fileList = new ArrayList<>();
        for (File file : files) {
            if(file.isDirectory()){
                continue;
            }
            if(!file.exists()) {
                Log.d("WearHelper", file.getName() + " ignored");
                continue;
            }
            MsgListFileNtf.FileInfo info = new MsgListFileNtf.FileInfo();
            info.fileSize = (float)file.length() / 1024 / 1024;
            info.fileName = file.getName();
            // StringUtils.toUtf8(file.getName());
            info.filePath = file.getAbsolutePath();
            list.fileList.add(info);
        }
        try {
            sendFileList(nodeId, list);
        }
        catch (Exception ex) {
            Log.e("WearHelper", ex.getMessage());
        }
    }

    public static void handleFileReceived(int statusCode, String requestId, File savedFile,
                                             String originalName) {
        Log.d("WearHelper", "File Received:" + savedFile.getAbsolutePath());
        MsgFileReceived ret = new MsgFileReceived();
        ret.filePath = savedFile.getAbsolutePath();
        try {
            sendFileReceived(getNearByNode().getId(), ret);
        }
        catch (Exception ex){
            Log.e("WearHelper", ex.getMessage());
        }
    }

    public static void handleMessage(MessageEvent messageEvent){
        String msg = messageEvent.getPath();
        Log.d("WearHelper", msg);
        if(msg.equals(SAY_HELLO)){
            // Node node = getNearByNode();
            WearManager wm = WearManager.getInstance();
            wm.sendMessage(messageEvent.getSourceNodeId(), SAY_HELLO, null);
        }
        else if(msg.equals(MSG_LIST_FILE_REQ)){
            try {
                MsgListFileReq req = (MsgListFileReq) Serializer.deserialize(messageEvent.getData());
                Log.d("WearHelper", "path:" + req.path);
                notifyFileList(req.path, messageEvent.getSourceNodeId());
            }
            catch (Exception ex){
                Log.e("WearHelper", ex.getMessage());
            }
        }
        else if(msg.equals(MSG_DEL_FILE_REQ)){
            try {
                MainActivity.mPlayer.stop();
            }
            catch (Exception ex) {

            }
            MsgDelFileRet ret = new MsgDelFileRet();
            ret.isSuccess = false;
            try {
                MsgDelFile req = (MsgDelFile) Serializer.deserialize(messageEvent.getData());
                Log.d("WearHelper", "delFile:" + req.filePath);
                ret.filePath = req.filePath;

                File f = new File(req.filePath);
                if(f.exists()) {
                    f.delete();
                    ret.isSuccess = true;
                    sendDelFileRet(messageEvent.getSourceNodeId(), ret);
                    notifyFileList(userPath, messageEvent.getSourceNodeId());
                    return;
                }
                else {
                    ret.msg = "File not exist";
                }
            }
            catch (Exception ex){
                ret.msg = ex.getMessage();
                // Log.e("WearHelper", ex.getMessage());
            }
            try {
                sendDelFileRet(messageEvent.getSourceNodeId(), ret);
            }
            catch (Exception ex){
                Log.e("WearHelper", ex.getMessage());
            }
        }
    }

}
