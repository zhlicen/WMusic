package live.a23333.wmusic;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.TimedText;
import android.os.Bundle;
import android.os.PowerManager;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Intent;
import android.app.PendingIntent;
import android.media.session.*;
import android.app.Notification;
import android.media.session.MediaSessionManager;
import android.media.MediaMetadata;
import android.support.v4.app.NotificationManagerCompat;

import static android.content.Context.MODE_PRIVATE;


/**
 * Created by zhlic on 5/5/2017.
 */

public class WMediaPlayer  implements MediaPlayer.OnErrorListener{
    private SharedPreferences prefs = null;
    AtomicBoolean isPlayerBusy = new AtomicBoolean(false);
    boolean checkIsBusy() {
        if(isPlayerBusy.get()) {
            return isPlayerBusy.get();
        }
        isPlayerBusy.set(true);
        // timeout
        new Thread(new Runnable() {
            @Override
            public void run() {
                // timeout
                Log.d("WMediaPlayer", "Busy waiting..");
                try {
                    Thread.sleep(1200);
                }
                catch (Exception ex) {
                    Log.d("WMediaPlayer", "Busy wait ex:" + ex.getMessage());
                }
                Log.d("WMediaPlayer", "Auto unlock busy");
                isPlayerBusy.set(false);
            }

        }).start();
        return false;
    }

    public enum PlayMode {
        PM_NORMAL(0),  PM_SHUFFLE(1),  PM_SINGLE(2);
        private final int value;
        PlayMode(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }


    public PlayMode playMode = PlayMode.PM_NORMAL;
    public PlayMode nextPM() {
        if(playMode == PlayMode.PM_NORMAL) {
            playMode = PlayMode.PM_SHUFFLE;
            if(mMediaPlayer != null) {
                mMediaPlayer.setLooping(false);
            }
            if(playList != null) {
                playList.setShuffle(true);
            }
        } else if(playMode == PlayMode.PM_SHUFFLE ) {
            playMode = PlayMode.PM_SINGLE;
            if(mMediaPlayer != null) {
                mMediaPlayer.setLooping(true);
            }
            if(playList != null) {
                playList.setShuffle(false);
            }
        } else {
            playMode = PlayMode.PM_NORMAL;
            if(mMediaPlayer != null) {
                mMediaPlayer.setLooping(false);
            }
            if(playList != null) {
                playList.setShuffle(false);
            }
        }
        int pm = playMode.getValue();
        prefs.edit().putInt("PlayMode", pm).commit();
        return playMode;

    }
    public static WMediaPlayer instance;
    public WMediaPlayer() {
        instance = this;
    }

    public interface OnPlayStateChange {
        void onPlayStateChange(WMediaPlayer mp, int state);
    }


    public interface OnPlayPositionListener {
        void onPlayPosition(int position);
    }

    void setOnPlayPositionListener(OnPlayPositionListener listener){
        mPlayPositionListener = listener;
    }

    public interface OnPlayFileChangeListener {
        void onPlayFileChange(MusicFile music);
    }

    protected OnPlayFileChangeListener mOnPlayFileChangeListener;

    public void setOnPlayFileChangeListener(OnPlayFileChangeListener listener){
        mOnPlayFileChangeListener = listener;
    }


    protected void notifyPlayFileChange(MusicFile music){
        if(mOnPlayFileChangeListener != null){
            mOnPlayFileChangeListener.onPlayFileChange(music);
        }
    }


    public WPlayList playList;
    protected OnPlayStateChange mPlayStateChangeCb;
    protected OnPlayPositionListener mPlayPositionListener;
    protected static final String MUSIC_PATH = "/sdcard/music";
    protected static final String INIT = "init";
    protected static final String PLAY = "play";
    protected static final String PASUE = "pause";
    protected static final String NEXT = "next";
    protected static final String PRE = "pre";
    protected static final String STOP = "stop";
    protected static final String FF = "ff";
    protected static final String FB = "fb";
    protected static final int musicBarNtfId = 1;

    protected class WMediaCallback extends MediaSession.Callback {
        private WMediaPlayer mwPlayer;
        WMediaCallback(WMediaPlayer player) {
            mwPlayer = player;
        }

        @Override
        public void onCommand(String command, Bundle args, ResultReceiver cb) {
            switch (command){
                case PLAY:
                    onPlay();
                    break;
                case PASUE:
                    onPause();
                    break;
                case NEXT:
                    onSkipToNext();
                    break;
                case PRE:
                    onSkipToPrevious();
                    break;
                case STOP:
                    onStop();
                    break;
                case FF:
                    doFF();
                    break;
                case FB:
                    doFB();
                    break;
                default:
                    break;
            }
        }

        void doFF() {
            if(mMediaPlayer != null && mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
                int curPosition = mMediaPlayer.getCurrentPosition();
                Log.d("FF", "FF:" + curPosition);
                mMediaPlayer.seekTo(curPosition + 5000);
            }
        }

        void doFB() {
            if(mMediaPlayer != null && mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
                int curPosition = mMediaPlayer.getCurrentPosition();
                Log.d("FB", "FB:" + curPosition);
                mMediaPlayer.seekTo(curPosition - 6000);
            }
        }


        @Override
        public void onPlay() {
            Log.d("WMediaPlayer", "onPlay()");
            try {
                mwPlayer.doPlay();
            }
           catch (Exception ex){
               Log.d("WMediaPlayer", "onPlay:" + ex.getMessage());
               onSkipToNext();
           }
        }

        @Override
        public void onPause() {
            Log.d("WMediaPlayer", "onPause()");
            try {
                mwPlayer.doPause();
            }
            catch (Exception ex){
                Log.d("WMediaPlayer", "onPause:" + ex.getMessage());
            }
        }

        public void savePlayPos() {
            try {
                MusicFile file = playList.getCurrentFile();
                if (file == null) {
                    return;
                }
                int pos = lastPos;
                if (mMediaPlayer != null) {
                    pos = mMediaPlayer.getCurrentPosition();
                }
                prefs.edit().putString("lastPlayFile", file.title).commit();
                prefs.edit().putInt("lastPlayPos", pos).commit();
            }
            catch (Exception ex) {

            }
        }



        @Override
        public void onStop() {
            Log.d("WMediaPlayer", "onStop()");
            updateNotifyBar(null);
            savePlayPos();
            playList = null;
            updatePlayState(PlaybackState.STATE_STOPPED);
            if(mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
                mPlaybackState = null;
                mSession.setActive(false);
                lastFile = null;
                lastPos = -1;
            }
            live.a23333.wmusic.WMediaService.mPlayer = null;
        }
        boolean skipped = false;
        @Override
        public void onSkipToNext() {
            try {
                mwPlayer.doNext();
            }
            catch (Exception ex){
                if(skipped == false) {
                    skipped = true;
                    Log.d("onSkipToNext:doNext", ex.getMessage());
                    onSkipToNext();
                    skipped = false;
                }
            }
        }

        @Override
        public void onSkipToPrevious() {
            try {
                mwPlayer.doPre();
            }
            catch (Exception ex){
                Log.d("onSkipToPrevious:doPre", ex.getMessage());
                onSkipToNext();
            }
        }

    }
    private MediaPlayer mMediaPlayer;
    private MediaSessionManager mManager;
    static private MediaSession mSession;
    static private MediaController mController;
    private Context mContext;
    private WMediaCallback mCallback;
    static public PlaybackState mPlaybackState;


    public void play() {
        //mController.getTransportControls().play();
        mController.sendCommand(PLAY, null, null);
    }

    public void pause() {
        mController.sendCommand(PASUE, null, null);
    }

    public void stop() {
        if(mController != null)
            mCallback.onStop();
            // mController.sendCommand(STOP, null, null);
    }

    public void next(){
        mController.sendCommand(NEXT, null, null);
    }

    public void ff(){
        mController.sendCommand(FF, null, null);
    }

    public void fb(){
        mController.sendCommand(FB, null, null);
    }

    public void pre(){
        mController.sendCommand(PRE, null, null);
    }

    private void runPlayPositionTask(){
//        final Timer timer = new Timer();
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                if(mMediaPlayer != null && mPlayPositionListener != null){
//                    try {
//                        mPlayPositionListener.onPlayPosition(mMediaPlayer.getCurrentPosition());
//                    }
//                    catch(Exception ex){
//
//                    }
//                }
//            }
//        }, 0, 1000);
    }


    private void prepareNewMusic(MusicFile music) throws IOException, IllegalStateException{
        if(mMediaPlayer != null){
            mMediaPlayer.reset();
        } else {
            mMediaPlayer = new android.media.MediaPlayer();
        }
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setDataSource(music.filePath);
        mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setLooping(playMode == PlayMode.PM_SINGLE);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(playMode != PlayMode.PM_SINGLE) {
                    next();
                }
            }
        });

        mMediaPlayer.prepare();
        music.loadCover();
        notifyPlayFileChange(music);
    }

    public void resumeActivity(){
        if(playList != null) {
            notifyPlayFileChange(playList.getCurrentFile());
        }
    }
    private void updatePlayState(int state){
        Log.d("WMediaPlayer", "updatePlayState:" + state);
        if(mSession != null && mMediaPlayer != null && mPlaybackState != null) {
            mPlaybackState = new PlaybackState.Builder(mPlaybackState)
                    .setState(state, mMediaPlayer.getCurrentPosition(), 1)
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                    .build();
            mSession.setPlaybackState(mPlaybackState);
        }
        if(mPlayStateChangeCb != null){
            mPlayStateChangeCb.onPlayStateChange(this, state);
        }
        if(playList != null) {
            updateNotifyBar(playList.getCurrentFile());
        }
    }

    public void setPlayStateChangeCb(OnPlayStateChange callback){
        mPlayStateChangeCb = callback;
    }

    public void playFileByIndex(final int index) throws IOException, IllegalStateException {
        Log.d("WMediaPlayer", "playFileByIndex:" + index);
        if(checkIsBusy()) {
            Log.d("WMediaPlayer", "Player is busy, retry later");
            return;
        }

        MusicFile music = playList.getFileByIndex(index);
        if(music != null) {
            prepareNewMusic(music);
        } else {
            Toast.makeText(mContext, "No file found in /sdcard/Music.", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaPlayer.start();
        updatePlayState(PlaybackState.STATE_PLAYING);
        if(playMode == PlayMode.PM_SHUFFLE) {
            playList.generateShuffleList();
        }
    }


    protected void doPlay() throws IOException, IllegalStateException{
        if(checkIsBusy()) {
            Log.d("WMediaPlayer", "Player is busy, retry later");
            return;
        }
        if(mContext == null){
            throw new IllegalStateException("Not initialized:1");
        }
        if(mPlaybackState != null &&
                mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            throw new IllegalStateException("Not initialized:2");
        }
        if(mMediaPlayer == null) {
            MusicFile music = lastFile;
            if(music == null) {
                music = playList.getNextFile();
                lastPos = -1;
            }
            if(music != null) {
                prepareNewMusic(music);
            } else {
                Toast.makeText(mContext, "No file found in /sdcard/Music.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        mMediaPlayer.start();
        if(lastFile != null && lastPos > 0) {
            mMediaPlayer.seekTo(lastPos);
            lastPos = -1;
            lastFile = null;
        }
        updatePlayState(PlaybackState.STATE_PLAYING);
        isPlayerBusy.set(false);
    }

    protected void doPause() throws IOException, IllegalStateException{
        if(checkIsBusy()) {
            Log.d("WMediaPlayer", "Player is busy, retry later");
            return;
        }
        if(mContext == null || mMediaPlayer == null){
            throw new IllegalStateException("Not initialized:3");
        }
        if(mPlaybackState.getState() == PlaybackState.STATE_PAUSED) {
            throw new IllegalStateException("Not initialized:4");
        }
        mMediaPlayer.pause();
        updatePlayState(PlaybackState.STATE_PAUSED);
        isPlayerBusy.set(false);
    }

    protected void doNext() throws IOException, IllegalStateException{
        if(checkIsBusy()) {
            Log.d("WMediaPlayer", "Player is busy, retry later");
            return;
        }
        if(mContext == null || mMediaPlayer == null){
            doPlay();
        }
        MusicFile music = playList.getNextFile();
        if(music != null) {
            prepareNewMusic(music);
        } else {
            Toast.makeText(mContext, "No file found in /sdcard/Music.", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaPlayer.start();
        updatePlayState(PlaybackState.STATE_PLAYING);
    }

    protected void doPre() throws IOException, IllegalStateException{
        if(checkIsBusy()) {
            Log.d("WMediaPlayer", "Player is busy, retry later");
            return;
        }
        if(mContext == null || mMediaPlayer == null){
            throw new IllegalStateException("Not initialized:5");
        }
        MusicFile music = playList.getPreFile();
        if(music != null) {
            prepareNewMusic(music);
        } else {
            return;
        }
        mMediaPlayer.start();
        updatePlayState(PlaybackState.STATE_PLAYING);
    }

    public void initMediaPlayer(Context context){
        Log.d("WMediaPlayer", "init player");
        mContext = context;
        mPlaybackState = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_PAUSED, 0, 1)
                .build();
        mManager = (MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        playList = new WPlayList();
        playList.doLoad(MUSIC_PATH);
        playList.setShuffle(false);
        mSession = new MediaSession(context, "WMusic");
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setActive(true);
        mSession.setPlaybackState(mPlaybackState);
        mController = mSession.getController();
        mCallback = new WMediaCallback(this);
        mSession.setCallback(mCallback);
        Intent intent = new Intent(mContext, WMediaService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        PendingIntent pIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setMediaButtonReceiver(pIntent);
        AudioManager audioManager = (AudioManager)
                mContext.getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                                                        @Override
                                                        public void onAudioFocusChange(int focusChange) {
                                                            if(instance != null && focusChange ==  AudioManager.AUDIOFOCUS_LOSS){
                                                                try {
                                                                    instance.doPause();
                                                                }
                                                                catch (Exception ex){
                                                                    Log.d("WMediaPlayer", "onAudioFocusChange:" + ex.getMessage());
                                                                    // Toast.makeText(mContext, ex.getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        }
                                                    },
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_GAIN) {
            Toast.makeText(mContext, "Failed to gain media focus.", Toast.LENGTH_SHORT).show();
        }
        runPlayPositionTask();
        prefs = mContext.getSharedPreferences("live.a23333.wmusic", MODE_PRIVATE);
        loadLastFile();
        int pm = prefs.getInt("PlayMode", 0);
        playMode = PlayMode.values()[pm];
        if(playList != null && playMode == PlayMode.PM_SHUFFLE) {
            playList.setShuffle(true);
        }
    }
    private MusicFile lastFile;
    private int lastPos = -1;
    private void loadLastFile() {
        lastPos = prefs.getInt("lastPlayPos", -1);
        String fileName = prefs.getString("lastPlayFile", "");
        prefs.edit().putString("lastPlayFile", "").commit();
        prefs.edit().putInt("lastPlayPos", -1).commit();
        lastFile = playList.getFileByName(fileName, true);
        if(lastFile != null) {
            if(mOnPlayFileChangeListener != null) {
                mOnPlayFileChangeListener.onPlayFileChange(lastFile);
            }
        }
    }

    private void updateSession(final MusicFile music) {
        if(music == null) {
            return;
        }
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, music.title);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                music.album + "-" + music.artist);
        Bitmap cover = music.cover;
        if (cover != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover);
        } else {
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.default_music_cover);
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
        }
        mSession.setMetadata(metadataBuilder.build());
    }

    public void updateNotifyBar(final MusicFile music){
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(mContext);
        if(music == null){
            notificationManager.cancel(musicBarNtfId);
            return;
        }
        updateSession(music);
        Notification.MediaStyle style = new Notification.MediaStyle()
                .setMediaSession(mController.getSessionToken());
        PendingIntent contentIntent;
        if(MainActivity.instance != null) {
            contentIntent =
                    PendingIntent.getActivity(mContext, 0, MainActivity.instance.getIntent()
                            , PendingIntent.FLAG_UPDATE_CURRENT);
        }
        else {
            Intent i = new Intent(mContext.getApplicationContext(), MainActivity.class);
            contentIntent = PendingIntent.getActivity(mContext, 0, i, 0);
        }
        Intent intent = new Intent( mContext, WMediaService.class );
        intent.setAction(STOP);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 1, intent, 0);
        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setSmallIcon(android.R.drawable.btn_radio)
                .setContentTitle(music.title)
                .setContentText( music.album + " - " + music.artist)
                .setLargeIcon(music.cover)
                .setColor(mContext.getResources().getColor(android.R.color.holo_blue_dark))
                .setStyle(style)
                .setDeleteIntent(pendingIntent)
                .setContentIntent(contentIntent);
        if(mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            builder.setOngoing(true);
        }
        try {
            notificationManager.notify(musicBarNtfId, builder.build());
        }
        catch (Exception ex) {

        }
    }

    static public class WMediaNoisyReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(instance == null || instance.mCallback == null) {
                return;
            }
            String action = intent.getAction();
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                Toast.makeText(instance.mContext, "Headset disconnected", Toast.LENGTH_SHORT).show();
                try {
                    instance.doPause();
                }
                catch (Exception ex){
                    // Toast.makeText(instance.mContext, ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    static public class WMediaService extends IntentService {
        public WMediaService(){
            super("WMediaService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            if( intent == null || intent.getAction() == null )
                return;
            String action = intent.getAction();
            if( action.equalsIgnoreCase( STOP ) && instance != null ) {
                instance.stop();
            } else if( action.equalsIgnoreCase( PASUE ) && instance != null) {
                instance.pause();
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return super.onStartCommand(intent,flags,startId);
        }
    }



    @Override
    public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_IO:
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Toast.makeText(mContext, "MP Error:" + what, Toast.LENGTH_SHORT).show();
                mCallback.onSkipToNext();
                return false;
        }

        return true;
    }


}
