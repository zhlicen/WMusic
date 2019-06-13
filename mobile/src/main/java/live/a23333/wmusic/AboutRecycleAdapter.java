package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.stat.StatService;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static android.net.Uri.parse;


public class AboutRecycleAdapter extends RecyclerView.Adapter<AboutRecycleAdapter.RecVH> {


    private class AboutInfo {
        AboutInfo(String title, String detail, Drawable bk) {
            mTitle = title;
            mDetail = detail;
            mBk = bk;
        }
        public String mTitle;
        public String mDetail;
        public Drawable mBk;
    }

    private List<AboutInfo> mAboutList;
    private Context mContext;
    private static int versionClickCount = 0;
    private static long versionLastClick = 0;

    public AboutRecycleAdapter(Context context){
        if(context == null) {
            return;
        }
        mContext = context;
        mAboutList = new ArrayList<>();
        String appVersion = "Unknown";
        try {
            appVersion = mContext.getPackageManager().getPackageInfo(
                    mContext.getPackageName(), 0).versionName;
        }
        catch (Exception ex) {

        }
        mAboutList.add(new AboutInfo(mContext.getString(R.string.app_version),
                appVersion,
                context.getDrawable(R.drawable.info_icon)));
        mAboutList.add(new AboutInfo(mContext.getString(R.string.app_license),
                mContext.getString(R.string.app_license_detail),
                context.getDrawable(R.drawable.license_icon)));
        mAboutList.add(new AboutInfo(mContext.getString(R.string.app_rate),
                mContext.getString(R.string.app_rate_detail),
                context.getDrawable(R.drawable.star_icon)));
        mAboutList.add(new AboutInfo(mContext.getString(R.string.app_feedback),
                mContext.getString(R.string.app_feedback_detail),
                context.getDrawable(R.drawable.mail_icon)));
//        mAboutList.add(new AboutInfo(mContext.getString(R.string.app_donate),
//                mContext.getString(R.string.app_donate_detail),
//                context.getDrawable(R.drawable.donate_icon)));

    }

    @Override
    public RecVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent,false);
        return new RecVH(view);
    }

    @Override
    public void onBindViewHolder(final RecVH holder, int position) {
        if(mAboutList == null){
            return;
        }
        final AboutInfo aboutInfo = mAboutList.get(position);
        holder.tvTitle.setText(aboutInfo.mTitle);
        holder.tvDetail.setText(aboutInfo.mDetail);
        holder.ivPic.setBackground(aboutInfo.mBk);
        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                String action = "";
                if(!aboutInfo.mTitle.equals(mContext.getString(R.string.app_version))) {
                    versionClickCount = 0;
                }
                if(aboutInfo.mTitle.equals(mContext.getString(R.string.app_feedback))){
                    action = "Feedback";
                     String appVersion = "Unknown";
                    try {
                        appVersion = mContext.getPackageManager().getPackageInfo(
                                mContext.getPackageName(), 0).versionName;
                    }
                    catch (Exception ex) {

                    }
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"zhlicen@gmail.com"});
                    String appName = mContext.getResources().getString(R.string.app_name);
                    String feedback = mContext.getResources().getString(R.string.feedback);
                    i.putExtra(Intent.EXTRA_SUBJECT, "[" + appName + "-" + appVersion + "] " + feedback);
                    try {
                        mContext.startActivity(Intent.createChooser(i,
                                mContext.getResources().getString(R.string.send_feedback)));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Snackbar.make(holder.v, ex.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
                else if(aboutInfo.mTitle.equals(mContext.getString(R.string.app_version))) {
                    action = "Version";
                    if(System.currentTimeMillis() - versionLastClick > 2000) {
                        versionClickCount = 0;
                    }
                    versionLastClick = System.currentTimeMillis();
                    versionClickCount++;
                    if(versionClickCount == 7){
                        action = action + "-VersionEgg";
                        Toast.makeText(mContext, "😘😘😘😘😘~", Toast.LENGTH_SHORT).show();
                        versionClickCount = 0;
                    }
                }
                else if(aboutInfo.mTitle.equals(mContext.getString(R.string.app_rate))){
                    action = "Rate";
                    final String appPackageName = mContext.getPackageName(); // getPackageName() from Context or Activity object
                    try {
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                }
                else if(aboutInfo.mTitle.equals(mContext.getString(R.string.app_donate))){
                    // bundle.putString("option", "Donate");
                    action = "Donate";
                    if(MainActivity.isChineseVersion)
                        DonateMgr.startChineseDonate(mContext);
                    else
                        DonateMgr.startGooglePlayDonate();
                }
                else if(aboutInfo.mTitle.equals(mContext.getString(R.string.app_license))){
                    action = "License";
                    // bundle.putString("option", "License");
                    String url = "https://zhlicen.github.io/app/wmusic/license.htm";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(parse(url));
                    mContext.startActivity(i);
                }
                bundle.putString("option", action);
                if(!MainActivity.isChineseVersion) {
                    MainActivity.mFirebaseAnalytics.logEvent("about_click", bundle);
                }
                else {
                    Properties prop = new Properties();
                    prop.setProperty("option", action);
                    StatService.trackCustomBeginKVEvent(mContext, "about_click", prop);
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        if(mAboutList == null){
            return 0;
        }
        return mAboutList.size();
    }


    public class RecVH extends RecyclerView.ViewHolder{
        ImageView ivPic;
        TextView tvTitle;
        TextView tvDetail;
        View v;

        public RecVH(View itemView) {
            super(itemView);
            ivPic = (ImageView)itemView.findViewById(R.id.item_image);
            tvTitle = (TextView)itemView.findViewById(R.id.item_title);
            tvDetail = (TextView)itemView.findViewById(R.id.item_detail);
            v = itemView.findViewById(R.id.list_item);
        }
    }
}