package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;


public class FileListRecycleAdapter extends RecyclerView.Adapter<FileListRecycleAdapter.RecVH> {

    static public List<WearHelper.MsgListFileNtf.FileInfo> mfileList;
    public Context mContext;
    public void setFileList(final List<WearHelper.MsgListFileNtf.FileInfo> fileList){

        new Handler(mContext.getMainLooper()).post(new Runnable() {
            public void run() {
                mfileList.clear();
                if(fileList != null)
                    mfileList.addAll(fileList);
                notifyDataSetChanged();
            }
        });


        Log.d("FileListRecycleAdapter", mfileList.toString());
    }

    public FileListRecycleAdapter(Context context){
        mContext = context;
        mfileList = new ArrayList<>();
    }

    @Override
    public RecVH onCreateViewHolder(final ViewGroup parent, int viewType) {
        final View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new RecVH(view);
    }

    @Override
    public void onBindViewHolder(final RecVH holder, int position) {
        if(mfileList == null){
            return;
        }
        try {
            final WearHelper.MsgListFileNtf.FileInfo fileInfo = mfileList.get(position);
            holder.tvTitle.setText(fileInfo.fileName);
            holder.tvDetail.setText(String.format("%.2f MB", fileInfo.fileSize));
            boolean selected = WearHelper.isFileSelected(fileInfo.filePath);
            if(!selected) {
                holder.ivPic.setBackground(mContext.getDrawable(R.drawable.music_icon));
            }
            else {
                holder.ivPic.setBackground(mContext.getDrawable(R.drawable.music_icon_selected));
            }

            holder.data = fileInfo.filePath;
            holder.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean selected = WearHelper.isFileSelected(fileInfo.filePath);
                    selected = !selected;
                    if (selected) {
                        holder.ivPic.setBackground(
                                mContext.getDrawable(R.drawable.music_icon_selected));
                    } else {
                        holder.ivPic.setBackground(
                                mContext.getDrawable(R.drawable.music_icon));
                    }
                    WearHelper.selectFile(fileInfo.filePath, selected);
                }
            });
        }
        catch(Exception ex) {

        }
    }

    @Override
    public int getItemCount() {
        if(mfileList == null){
            return 0;
        }
        return mfileList.size();
    }


    public void selectAll(boolean select) {
        for (WearHelper.MsgListFileNtf.FileInfo file : mfileList) {
            WearHelper.selectFile(file.filePath, select);
        }
        notifyDataSetChanged();
    }

    public class RecVH extends RecyclerView.ViewHolder{
        ImageView ivPic;
        TextView tvTitle;
        TextView tvDetail;
        boolean selected;
        String data;
        View v;

        public RecVH(View itemView) {
            super(itemView);
            ivPic = (ImageView)itemView.findViewById(R.id.item_image);
            tvTitle = (TextView)itemView.findViewById(R.id.item_title);
            tvDetail = (TextView)itemView.findViewById(R.id.item_detail);
            v = itemView.findViewById(R.id.list_item);
            selected = false;
        }
    }
}