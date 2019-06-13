package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */
import android.*;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.icu.text.DateFormat;
import android.media.AudioManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import rm.com.youtubeplayicon.PlayIconDrawable;

public class PlayerFragment extends Fragment implements View.OnClickListener {

    private Context mContext;
    private ImageView mBtnPlayPause;
    private Button mBtnPlayNext;
    private Button mBtnPlayPre;
    private Button mBtnVolUp;
    private Button mBtnVolDown;
    private TextView mTxtTitle;
    private TextView mTxtInfo;
    private View mPlayerBk;
    private View mPlayerBkFront;
    static private WMediaService mService;
    private boolean waitPlay = false;
    static public boolean firstInited;
    static View v;
    PlayIconDrawable playIcon;


    final int PERMISSION_REQ_CODE_READ_STORAGE = 0;
    AudioManager audioMgr;
    public static PlayerFragment newInstance(Context context) {
        PlayerFragment fragment = new PlayerFragment();
        fragment.mContext = context;
        return fragment;
    }
    boolean holding = false;
    protected void initComponents() {
        mBtnPlayPause = (ImageView) v.findViewById(R.id.id_btn_player_play_pause);
        mBtnPlayNext= (Button) v.findViewById(R.id.id_btn_player_next);
        mBtnPlayPre = (Button) v.findViewById(R.id.id_btn_player_pre);
        mBtnVolUp = (Button) v.findViewById(R.id.id_btn_vol_up);
        mBtnVolDown = (Button) v.findViewById(R.id.id_btn_vol_down);
        mBtnPlayPause.setOnClickListener(this);
        mBtnPlayNext.setOnClickListener(this);
        mBtnPlayPre.setOnClickListener(this);
        mBtnVolUp.setOnClickListener(this);
        mBtnVolDown.setOnClickListener(this);
        mTxtTitle = (TextView)v.findViewById(R.id.id_txt_media_title);
        mTxtInfo = (TextView)v.findViewById(R.id.id_txt_media_info);
        mPlayerBk = v.findViewById(R.id.id_player_bk);
        mPlayerBkFront = v.findViewById(R.id.id_player_bk_front);
        audioMgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mBtnPlayNext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                holding = true;
                runFF();
                return true;
            }
        });

        mBtnPlayNext.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    holding = false;
                }
                return false;
            }
        });

        mBtnPlayPre.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                holding = true;
                runFB();
                return true;
            }
        });
        mBtnPlayPre.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_OUTSIDE ) {
                    holding = false;
                }
                return false;
            }
        });
    }

    public void runFF() {
        mBtnPlayNext.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(holding) {
                    if(MainActivity.mPlayer != null) {
                        MainActivity.mPlayer.ff();
                    }
                    runFF();
                }
            }
        }, 1000);
    }

    public void runFB() {
        mBtnPlayPre.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(holding) {
                    if(MainActivity.mPlayer != null) {
                        MainActivity.mPlayer.fb();
                    }
                    runFB();
                }
            }
        }, 1000);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = inflater.getContext();
        if(v != null) {
            FileListRecyclerAdapter.updateData();
        }
        View view;
        if(mContext.getResources().getConfiguration().isScreenRound()) {
            view = inflater.inflate(R.layout.player_view, container, false);
        }
        else {
            view = inflater.inflate(R.layout.player_view_rect, container, false);
        }
        v = view;
        initComponents();
        playerRequestPermissions();
        WearHelper.initialize(mContext);
        return view;
    }
    static BitmapDrawable bmd = null;
    protected void UpdatePlayFile(final MusicFile music) {
        if(music == null){
            mPlayerBk.setBackground(mContext.getDrawable(R.drawable.default_music_cover));
            mPlayerBkFront.setBackgroundColor(0);
            return;
        }

        final String title = music.title;
        final String info = music.album + " - " + music.artist;

        if(mTxtInfo.getText().equals(title)) {
            return;
        }
        mTxtTitle.post(new Runnable() {
            @Override
            public void run() {
                mTxtTitle.setText(title);
            }
        });

        mTxtInfo.post(new Runnable() {
            @Override
            public void run() {
                mTxtInfo.setText(info);
            }
        });

        mPlayerBk.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        // music.loadCover();
                        final Bitmap cover = music.cover;
                        if(cover != null){
                            bmd = new BitmapDrawable(getResources(), cover);
                        }
                        if(MainActivity.instance == null) {
                            return;
                        }
                        MainActivity.instance.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Animator animator = AnimatorInflater.loadAnimator(mContext, R.animator.fade_in);
                                animator.setTarget(mPlayerBk);
                                animator.setDuration(400);
                                animator.start();
                                animator.addListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        try {
                                            if (cover == null || cover.isRecycled()) {
                                                mPlayerBk.setBackground(mContext.getDrawable(R.drawable.default_music_cover));
                                                mPlayerBkFront.setBackgroundColor(0);
                                            } else {
                                                mPlayerBk.setBackground(bmd);
                                                mPlayerBkFront.setBackgroundColor(mContext.getColor(R.color.color_player_bk));
                                            }
                                            Animator animator = AnimatorInflater.loadAnimator(mContext, R.animator.fade_out);
                                            animator.setTarget(mPlayerBk);
                                            animator.setDuration(400);
                                            animator.start();
                                        }
                                        catch(Exception ex) {

                                        }
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {

                                    }
                                });

                            }
                        });

                        MainActivity.mPlayer.updateNotifyBar(music);
                    }
                }).start();
            }
        }, 200);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.id_btn_player_play_pause:
                onBtnPlayPause();
                break;
            case R.id.id_btn_player_next:
                onBtnNext();
                break;
            case R.id.id_btn_player_pre:
                onBtnPre();
                break;
            case R.id.id_btn_vol_up:
                volUp();
                break;
            case R.id.id_btn_vol_down:
                volDown();
                break;
            default:
                break;
        }
    }
    Toast volToast;
    private void showVol(final int vol) {
        if(volToast == null) {
            volToast = Toast.makeText(mContext, Integer.toString(vol), Toast.LENGTH_SHORT);
        }

        volToast.setText(Integer.toString(vol));
        volToast.show();
    }
    private void volUp() {
        int volNow = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(volNow == audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC)){
            showVol(volNow);
            return;
        }
        audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, volNow + 1, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        showVol(volNow + 1);
    }

    private void volDown() {
        int volNow = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(volNow == 0){
            showVol(volNow);
            return;
        }
        audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, volNow - 1, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        showVol(volNow - 1);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((WMediaService.LocalBinder)service).getService();
            initMusicPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MainActivity.mPlayer = null;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mService != null) {
            mContext.unbindService(serviceConnection);
        }
    }

    protected void initMusicService() {
        if(mService != null) {
            Intent playerIntent = new Intent(mContext, WMediaService.class);
            playerIntent.setAction(WMediaService.ACTION_REINIT);
            mContext.bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            return;
//            if(MainActivity.mPlayer != null && MainActivity.mPlayer.mPlaybackState != null) {
//                int state = MainActivity.mPlayer.mPlaybackState.getState();
//                if(state != PlaybackState.STATE_STOPPED) {
//                    mBtnPlayPause.setForeground(null);
//                    playIcon = PlayIconDrawable.builder()
//                            .withColor(Color.WHITE)
//                            .withInterpolator(new FastOutSlowInInterpolator())
//                            .withDuration(250)
//                            .withInitialState(PlayIconDrawable.IconState.PLAY)
//                            .into(mBtnPlayPause);
//                    if(state == PlaybackState.STATE_PAUSED) {
//                        playIcon.setIconState(PlayIconDrawable.IconState.PLAY);
//                    }
//                    else {
//                        playIcon.setIconState(PlayIconDrawable.IconState.PAUSE);
//                    }
//                    updateBtnStatus(state);
//                    UpdatePlayFile(MainActivity.mPlayer.playList.getCurrentFile());
//                    return;
//                }
//            }
//            Log.d("Activity", "stopMusicService");
//            mContext.unbindService(serviceConnection);
//            mContext.stopService(new Intent(mContext, WMediaService.class));
        }
        Log.d("Activity", "initMusicService");
        Intent playerIntent = new Intent(mContext, WMediaService.class);
        playerIntent.setAction(WMediaService.ACTION_INIT);
        mContext.startService(playerIntent);
        mContext.bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void initMusicPlayer() {
        if(WMediaService.mPlayer == null) {
            initMusicService();
            return;
        }
        MainActivity.mPlayer = WMediaService.mPlayer;
        MainActivity.mPlayer.setOnPlayFileChangeListener(new WMediaPlayer.OnPlayFileChangeListener() {
            @Override
            public void onPlayFileChange(MusicFile music) {
                UpdatePlayFile(music);
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        FileListRecyclerAdapter.updateData();
                        FileListFragment.updateSelect();
                    }
                });

            }
        });
        MainActivity.mPlayer.setPlayStateChangeCb(new WMediaPlayer.OnPlayStateChange() {
            @Override
            public void onPlayStateChange(WMediaPlayer mp, int state) {
                final int fstate = state;
                try {
                    if (fstate == PlaybackState.STATE_STOPPED) {
                        MainActivity.mPlayer = null;
                        MainActivity.instance.finishAndRemoveTask();
                        return;
                    }
                }
                catch (Exception ex) {

                }
                if(MainActivity.instance == null) {
                    return;
                }
                MainActivity.instance.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateBtnStatus(fstate);
                    }
                });
            }
        });
        v.post(new Runnable() {
            @Override
            public void run() {
                FileListRecyclerAdapter.instance.notifyDataSetChanged();
            }
        });

        MainActivity.mPlayer.setOnPlayPositionListener(new WMediaPlayer.OnPlayPositionListener() {
            @Override
            public void onPlayPosition(int position) {
            }
        });
        mPlayerBk.setBackground(mContext.getDrawable(R.drawable.default_music_cover));

        playIcon = PlayIconDrawable.builder()
                .withColor(Color.WHITE)
                .withInterpolator(new FastOutSlowInInterpolator())
                .withDuration(250)
                .withInitialState(PlayIconDrawable.IconState.PLAY)
                .into(mBtnPlayPause);
        UpdatePlayFile(MainActivity.mPlayer.playList.getCurrentFile());
        if(MainActivity.mPlayer != null && MainActivity.mPlayer.mPlaybackState != null) {
            int state = MainActivity.mPlayer.mPlaybackState.getState();
            updateBtnStatus(state);
        }
        else {
            updateBtnStatus(PlaybackState.STATE_PAUSED);
        }
        FileListFragment.updateSelect();
//        if(waitPlay) {
//            onBtnPlayPause();
//        }
        firstInited = true;
        WearFragmentAdapter.COUNT = 3;
    }

    protected void playerRequestPermissions(){
        if (mContext.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQ_CODE_READ_STORAGE);
        }
        else{
            initMusicService();
        }
    }

    protected void onBtnPlayPause(){
        if(!firstInited) {
            return;
        }
        if(MainActivity.mPlayer == null) {
            initMusicService();
            waitPlay = true;
            return;
        }
        waitPlay = false;
        try {
            if(MainActivity.mPlayer.mPlaybackState.getState() != PlaybackState.STATE_PLAYING) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.mPlayer.play();
                    }
                }).start();
            }
            else{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.mPlayer.pause();
                    }
                }).start();
            }
        }
        catch(Exception ex){
            // Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
        updateBtnStatus(MainActivity.mPlayer.mPlaybackState.getState());
    }

    protected void onBtnNext(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.mPlayer.next();
            }
        }).start();
    }


    protected void onBtnPre(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.mPlayer.pre();
            }
        }).start();
    }

    protected void updateBtnStatus(int state){
        if(MainActivity.mPlayer == null) {
            return;
        }

        if(state == PlaybackState.STATE_PLAYING){
            mBtnPlayPause.setForeground(null);
            if(mBtnPlayPre.getVisibility() == View.VISIBLE) {
                return;
            }
            if(playIcon.getIconState() == PlayIconDrawable.IconState.PLAY) {
                playIcon.animateToState(PlayIconDrawable.IconState.PAUSE);
            }
            // mBtnPlayPause.setForeground(mContext.getDrawable(R.drawable.icon_pause));
            mBtnPlayPre.setVisibility(View.VISIBLE);
            mBtnPlayNext.setVisibility(View.VISIBLE);
            mBtnVolUp.setVisibility(View.VISIBLE);
            mBtnVolDown.setVisibility(View.VISIBLE);
        }
        else if(state == PlaybackState.STATE_STOPPED){
            mBtnPlayPause.setForeground(null);
            if(playIcon.getIconState() == PlayIconDrawable.IconState.PAUSE) {
                playIcon.animateToState(PlayIconDrawable.IconState.PLAY);
            }
            // mBtnPlayPause.setForeground(mContext.getDrawable(R.drawable.icon_play));
            mBtnPlayPre.setVisibility(View.INVISIBLE);
            mBtnPlayNext.setVisibility(View.INVISIBLE);
            mBtnVolUp.setVisibility(View.INVISIBLE);
            mBtnVolDown.setVisibility(View.INVISIBLE);
            mPlayerBk.setBackground(new BitmapDrawable(getResources(),
                    BitmapFactory.decodeResource(getResources(),
                            R.drawable.default_music_cover)));
            mPlayerBkFront.setBackgroundColor(0);
            mTxtTitle.setText("");
            mTxtInfo.setText("");
            MainActivity.mPlayer = null;
            Intent playerIntent = new Intent(mContext, WMediaService.class);
            mContext.stopService(playerIntent);
        }
        else {
            mBtnPlayPause.setForeground(null);
            if(playIcon.getIconState() == PlayIconDrawable.IconState.PAUSE) {
                playIcon.animateToState(PlayIconDrawable.IconState.PLAY);
            }
            // mBtnPlayPause.setForeground(mContext.getDrawable(R.drawable.icon_play));
            mBtnPlayPre.setVisibility(View.INVISIBLE);
            mBtnPlayNext.setVisibility(View.INVISIBLE);
            mBtnVolUp.setVisibility(View.INVISIBLE);
            mBtnVolDown.setVisibility(View.INVISIBLE);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_CODE_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initMusicService();
                } else {

                }
                return;
            }
        }
    }

}