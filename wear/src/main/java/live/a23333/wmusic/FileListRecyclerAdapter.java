package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;
import java.util.MissingFormatArgumentException;


public class FileListRecyclerAdapter extends RecyclerView.Adapter<FileListRecyclerAdapter.RecVH> {
    private Context mContext;
    static FileListRecyclerAdapter instance;
    private int currentPlay = -1;
    public FileListRecyclerAdapter(Context context){
        mContext = context;
        instance = this;
    }

    static public void updateData() {
        if(instance != null) {
            try {
                if (instance.currentPlay != -1) {
                    instance.notifyItemChanged(instance.currentPlay + 1);
                    instance.currentPlay = MainActivity.mPlayer.playList.currentIndex;
                    instance.notifyItemChanged(instance.currentPlay + 1);
                }
                else {
                    instance.notifyDataSetChanged();
                }

            }
            catch (Exception ex) {

            }
        }
    }

    @Override
    public RecVH onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new RecVH(view);
    }

    @Override
    public void onBindViewHolder(final RecVH holder, final int position) {
        if(position >= getItemCount()){
            return;
        }
        if(position == 0) {
            holder.v.setBackgroundColor(0);
            holder.musicLayout.setVisibility(View.INVISIBLE);
            holder.ivCtrl.setVisibility(View.VISIBLE);
            int iconID = getPMIcon(MainActivity.mPlayer.playMode);
            holder.ivCtrl.setBackground(mContext.getDrawable(iconID));
            holder.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(MainActivity.mPlayer == null) {
                        return;
                    }
                    MainActivity.mPlayer.nextPM();
                    int iconID = getPMIcon(MainActivity.mPlayer.playMode);
                    holder.ivCtrl.setBackground(mContext.getDrawable(iconID));
                }
            });
            return;
        }
        try {
            final int musicIdx = position - 1;
            MusicFile file = MainActivity.mPlayer.playList.fileList.get(musicIdx);
            holder.v.setBackgroundColor(0x10000000);
            holder.musicLayout.setVisibility(View.VISIBLE);
            holder.ivCtrl.setVisibility(View.INVISIBLE);
            holder.tvTitle.setText(file.title);
            holder.index = musicIdx;
            if(musicIdx == MainActivity.mPlayer.playList.currentIndex) {
                holder.ivPic.setBackground(mContext.getDrawable(R.drawable.music_icon_playing));
            }
            else {
                holder.ivPic.setBackground(mContext.getDrawable(R.drawable.music_icon));
            }
            final int index = musicIdx;
            holder.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                        try {
                            Thread.sleep(100);
                            MainActivity.mPlayer.playFileByIndex(index);
                        }
                        catch (final Exception ex) {
                            holder.v.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, ex.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                        }}
                    }).start();

                }
            });
        }
        catch(Exception ex) {

        }
    }

    private int getPMIcon(WMediaPlayer.PlayMode mode) {
        if(mode == WMediaPlayer.PlayMode.PM_NORMAL) {
            return R.drawable.pm_normal;
        }
        else if(mode == WMediaPlayer.PlayMode.PM_SHUFFLE) {
            return R.drawable.pm_shuffle;
        }
        else {
            return R.drawable.pm_single;
        }
    }

    @Override
    public int getItemCount() {
        try {
            return MainActivity.mPlayer.playList.fileList.size() + 1;
        }
        catch (Exception ex) {
            return 0;
        }
    }


    public class RecVH extends RecyclerView.ViewHolder{
        ImageView ivPic;
        TextView tvTitle;
        int index;
        View v;
        View musicLayout;
        ImageView ivCtrl;

        public RecVH(View itemView) {
            super(itemView);
            ivPic = (ImageView)itemView.findViewById(R.id.item_image);
            tvTitle = (TextView)itemView.findViewById(R.id.item_title);
            v = itemView.findViewById(R.id.list_item);
            musicLayout = itemView.findViewById(R.id.music_layout);
            ivCtrl = (ImageView)itemView.findViewById(R.id.ctrl_image);
        }
    }
}