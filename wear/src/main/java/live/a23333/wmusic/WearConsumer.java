package live.a23333.wmusic;

/**
 * Created by zhlic on 5/17/2017.
 */
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;


import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

public class WearConsumer implements com.google.devrel.wcl.callbacks.WearConsumer {
    @Override
    public void onWearableInitialConnectedCapabilitiesReceived() {
        //no-op
    }

    @Override
    public void onWearableInitialConnectedNodesReceived() {
        //no-op
    }

    @Override
    public void onWearableAddCapabilityResult(int statusCode) {
        //no-op
    }

    @Override
    public void onWearableRemoveCapabilityResult(int statusCode) {
        //no-op
    }

    @Override
    public void onWearableCapabilityChanged(String capability, Set<Node> nodes) {
        //no-op
    }

    @Override
    public void onWearableSendMessageResult(int statusCode) {
        //no-op
    }

    @Override
    public void onWearableMessageReceived(MessageEvent messageEvent) {
        Log.d("WearConsumer", "onWearableMessageReceived");
        WearHelper.handleMessage(messageEvent);
    }

    @Override
    public void onWearableApiConnected() {
        Log.d("WearConsumer", "onWearableApiConnected");
    }

    @Override
    public void onWearableApiConnectionSuspended() {
        //no-op
    }

    @Override
    public void onWearableApiConnectionFailed() {
        //no-op
    }

    @Override
    public void onWearableApplicationLaunchRequestReceived(Bundle bundle,
                                                           boolean relaunchIfRunning) {
        //no-op
    }

    @Override
    public void onWearableHttpRequestReceived(String url, String method, String query,
                                              String charset, String nodeId, String requestId) {
        //no-op
    }

    @Override
    public void onWearableChannelOpened(Channel channel) {
        //no-op
    }

    @Override
    public void onWearableFileReceivedResult(int statusCode, String requestId, File savedFile,
                                             String originalName) {
        WearHelper.handleFileReceived(statusCode, requestId, savedFile, originalName);
    }

    @Override
    public void onWearableChannelClosed(Channel channel, int closeReason,
                                        int appSpecificErrorCode) {
        //no-op
    }

    @Override
    public void onWearableInputClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        //no-op
    }

    @Override
    public void onWearableOutputClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        //no-op
    }

    @Override
    public void onWearableDataChanged(DataEventBuffer dataEvents) {
        //no-op
    }

    @Override
    public void onWearableConnectedNodes(List<Node> connectedNodes) {
        //no-op
    }

    @Override
    public void onWearablePeerDisconnected(Node peer) {
        //no-op
    }

    @Override
    public void onWearablePeerConnected(Node peer) {
        //no-op
    }

    @Override
    public void onWearableSendDataResult(int statusCode) {
        //no-op
    }

    @Override
    public void onWearableGetDataItems(int status, DataItemBuffer dataItemBuffer) {
        //no-op
    }

    @Override
    public void onWearableGetDataItem(int statusCode, DataApi.DataItemResult dataItemResult) {
        //no-op
    }

    @Override
    public void onWearableDeleteDataItemsResult(int statusCode) {
        //no-op
    }

    @Override
    public void onWearableInputStreamForChannelOpened(int statusCode, String requestId,
                                                      Channel channel, InputStream inputStream) {
        //no-op
    }

    @Override
    public void onWearableSendFileResult(int statusCode, String requestId) {
        //no-op
    }

    @Override
    public void onOutputStreamForChannelReady(int statusCode, Channel channel,
                                              OutputStream outputStream) {
        //no-op
    }
}
