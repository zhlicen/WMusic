package live.a23333.wmusic;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.devrel.wcl.WearManager;
import com.google.devrel.wcl.connectivity.WearFileTransfer;
import com.google.devrel.wcl.filters.NearbyFilter;

import java.io.File;
import java.util.List;
import java.util.Set;


/**
 * Created by zhlic on 5/17/2017.
 */



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class WearHelper {
    private static Context sContext;

    public static Map<String, String> selectedMap;

    public static interface OnFileSendStatusListener {
        void onFileSendStatus(String filePath, int statusCode);
    }

    public static interface OnNodeConnectedListener {
        void onNodeConnected();
    }

    public static interface OnHelloListener {
        void onHello();
    }

    public static interface OnFileSendResultListener {
        void onFileSendResult(String filePath, boolean success);
    }

    public static interface OnFileListUpateListener {
        void onFileListUpdate(MsgListFileNtf ntf);
    }

    public static interface OnDelFileResultListener {
        void onDelFileResult(MsgDelFileRet ret);
    }

    public static interface OnFileSelectedChangeListener {
        void OnFileSelectedChange(Map<String, String> selectedMap);
    }




    public static OnFileSendStatusListener fileSendStatusListener = null;
    public static OnFileSendResultListener fileSendResultListener = null;
    public static OnFileListUpateListener fileListUpateListener = null;
    public static OnDelFileResultListener delFileResultListener = null;
    public static OnFileSelectedChangeListener fileSelectedChange = null;
    public static OnNodeConnectedListener nodeConnectedListener = null;
    public static OnHelloListener helloListener = null;
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

    public static void sayHello(Node node) throws IOException {
        if(node == null) {
            throw new IOException("Invalid Node");
        }
        WearManager.getInstance().sendMessage(node.getId(), SAY_HELLO, null);
    }

    public static void selectFile(String file, boolean seleted){
        if(seleted) {
            selectedMap.put(file, "");
            Log.d("WearHelper", "Selected:" + file);
        } else {
            try {
                selectedMap.remove(file);
                Log.d("WearHelper", "Unselected:" + file);
            }
            catch(Exception ex) {
                Log.d("WearHelper", ex.getMessage());
            }
        }
        if(fileSelectedChange != null){
            fileSelectedChange.OnFileSelectedChange(selectedMap);
        }
    }

    public static boolean isFileSelected(String file) {
        return selectedMap.containsKey(file);
    }

    public static void initialize(Context context) {
        sContext = context;
        selectedMap = new HashMap<>();
        WearManager wm = WearManager.initialize(sContext);
        wm.addWearConsumer(new WearConsumer());
     }

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
    public static class MsgListFileReq implements Serializable{
        public String path;
    }

    public static String MSG_LIST_FILE_NTF = "list_file_ntf";
    public static class MsgListFileNtf implements Serializable {
        public static class FileInfo implements Serializable  {
            public String fileName;
            public String filePath;
            public float fileSize;
        }
        public List<FileInfo> fileList;
    }

    public static String MSG_DEL_FILE_REQ = "del_file_req";
    public static class MsgDelFile implements Serializable {
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


    public static void getWearFileListReq(Node node, String path) throws IOException {
        if(node == null) {
            throw new IOException("Invalid Node");
        }
        MsgListFileReq req = new MsgListFileReq();
        req.path = path;
        byte[] body = Serializer.serialize(req);
        Log.d("WearHelper", node.getDisplayName());
        WearManager.getInstance().sendMessage(node.getId(), MSG_LIST_FILE_REQ, body);
    }

    public static void sendFile(Node node, String filePath, String fileName) {
        final File file = new File(filePath, fileName);
        WearFileTransfer fileTransfer = new WearFileTransfer.Builder(node)
                .setFile(file)
                .setRequestId(file.getAbsolutePath())
                .setTargetName(fileName)
                .setOnFileTransferResultListener(new WearFileTransfer.OnFileTransferRequestListener(){
                    @Override
                    public void onFileTransferStatusResult(int statusCode) {
                        if(fileSendStatusListener != null) {
                            fileSendStatusListener.onFileSendStatus(file.getAbsolutePath(), statusCode);
                        }
                        Log.d("WearHelper", file.getAbsolutePath() + " send:" + statusCode);
                    }
                }).build();
        fileTransfer.startTransfer();
    }


    public static void delWearFile(Node node, String path) throws IOException {
        MsgDelFile req = new MsgDelFile();
        req.filePath = path;
        byte[] body = Serializer.serialize(req);
        Log.d("WearHelper", node.getDisplayName());
        WearManager.getInstance().sendMessage(node.getId(), MSG_DEL_FILE_REQ, body);
    }

    public static void handleMessage(MessageEvent messageEvent){
        String msg = messageEvent.getPath();
        Log.d("WearHelper", msg);
        if(msg.equals(MSG_LIST_FILE_NTF)){
            try {
                MsgListFileNtf list =
                        (MsgListFileNtf)Serializer.deserialize(messageEvent.getData());
                if(fileListUpateListener != null) {
                    fileListUpateListener.onFileListUpdate(list);
                }
            }
            catch (Exception ex){
            }
        }
        else if(msg.equals(MSG_DEL_FILE_RESULT)) {
            try {
                MsgDelFileRet ret =
                        (MsgDelFileRet)Serializer.deserialize(messageEvent.getData());
                Log.d("WearHelper", ret.filePath + "Del result:" + ret.isSuccess + " " + ret.msg);
                if(delFileResultListener != null) {
                    delFileResultListener.onDelFileResult(ret);
                }
            }
            catch (Exception ex){
                Log.e("WearHelper", ex.getMessage());
            }
        }
        else if(msg.equals(MSG_FILE_RECEIVED_NTF)){
            try {
                MsgFileReceived ret =
                        (MsgFileReceived)Serializer.deserialize(messageEvent.getData());
                Log.e("WearHelper", ret.filePath + " received");
                if(fileSendResultListener != null) {
                    fileSendResultListener.onFileSendResult(ret.filePath, true);
                }
            }
            catch (Exception ex){
                Log.e("WearHelper", ex.getMessage());
            }
        }
        else if(msg.equals(SAY_HELLO)) {
            if(WearHelper.helloListener != null) {
                WearHelper.helloListener.onHello();
            }
        }
    }

}
