package live.a23333.wmusic;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Locale;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;
import java.util.Map;
import java.io.File;
import android.support.design.widget.TabLayout;
import android.content.res.ColorStateList;
import android.app.ProgressDialog;
import java.util.List;

import com.google.devrel.wcl.WearManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.google.android.gms.wearable.Node;
import com.tencent.stat.StatService;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity
        implements WearHelper.OnFileSelectedChangeListener, WearHelper.OnFileSendResultListener,
        WearHelper.OnDelFileResultListener,
        WearHelper.OnFileSendStatusListener,
        WearHelper.OnNodeConnectedListener,
    WearHelper.OnHelloListener{
    public static MainActivity instance;
    public static boolean isChineseVersion = false;
    public static FloatingActionButton fab;
    private boolean mSelectMode = false;
    private boolean mConnected = false;
    public static Snackbar snack;
    WFragmentAdapter adapter;
    private ImageButton btnWifi;
    static int DEFAULT_FTP_PORT = 12358;
    static String FTP_USER_NAME = "wmusic";
    static String FTP_PWD = "*#06#";
    static public FirebaseAnalytics mFirebaseAnalytics;
    private CheckBox cbSelectAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

//        Locale locale = getCurrentLocale();
//        if(locale.getCountry() == Locale.CHINA.getCountry()) {
//            isChineseVersion = true;
//        }
//        else {
//            isChineseVersion = false;
//        }

        if(!isChineseVersion) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "MainActivity");
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "MainActivity");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
        }
        else {
            StatService.setContext(this.getApplication());
            Properties prop = new Properties();
            StatService.trackCustomBeginKVEvent(this, "activity_created", prop);
        }

        if(!isChineseVersion) {
            if(!checkPlayStore()) {
                return;
            }
        }

        cbSelectAll = (CheckBox) findViewById(R.id.cb_select_all);
        cbSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileListFragment.instance.fileListAdapter.selectAll(cbSelectAll.isChecked());
            }
        });
        initialWifiLogic();
        WearHelper.fileSelectedChange = this;
        WearHelper.delFileResultListener = this;
        WearHelper.fileSendStatusListener = this;
        WearHelper.fileSendResultListener = this;
        WearHelper.nodeConnectedListener = this;
        WearHelper.helloListener = this;
        WearHelper.initialize(this);
        ViewPager viewPager = (ViewPager)findViewById(R.id.view_page);
        if(adapter == null) {
            adapter = new WFragmentAdapter(getSupportFragmentManager(),
                    this);
        }
        viewPager.setAdapter(adapter);
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Bundle bundle = new Bundle();
                Properties prop = new Properties();
                if(tab.getPosition() == 0) {
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "TabFilelist");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "TabFilelist");
                    prop.setProperty("value", "TabFilelist");
                    fab.show();
                    if(mSelectMode) {
                        cbSelectAll.setVisibility(View.VISIBLE);
                    }
                    else {
                        btnWifi.setVisibility(View.VISIBLE);
                    }
                    checkConnected();
                }
                else {
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "TabAbout");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "TabAbout");
                    prop.setProperty("value", "TabAbout");
                    fab.hide();
                    if(snack != null) {
                        snack.dismiss();
                    }
                    if(mSelectMode) {
                        cbSelectAll.setVisibility(View.INVISIBLE);
                    }
                    else {
                        btnWifi.setVisibility(View.INVISIBLE);
                    }
                }
                if(!isChineseVersion) {
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                }
                else {
                    StatService.trackCustomBeginKVEvent(MainActivity.this, "tab_selected", prop);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkConnected()) {
                    return;
                }
                if(mSelectMode){
                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Light_Dialog_Alert);

                    builder.setMessage(getString(R.string.ensure_del_files))
                           .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                   delOneSelected();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                else {
                    OnAddAction();
                }
            }
        });
        updateFab();
        reqPermissions();
        fab.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkConnected();
                    }
                });
            }
        }, 1000);
    }
    private ProgressDialog pDlg;
    private void showLoadingDlg(String title, String msg) {
        if(pDlg != null) {
            pDlg.cancel();
            pDlg = null;
        }
        pDlg = ProgressDialog.show(this, title,
                msg, true);
        showLoadingNotif(title, msg);
        pDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(mSelectMode) {
                    delOneSelected();
                }
                else {
                    sendOneFile();
                }
            }
        });
        Log.d("MainActivity", "Loading dialog, exist");
    }

    private void initialWifiLogic() {
        FtpClientHelper.mContext = this;
        btnWifi = (ImageButton) findViewById(R.id.btn_wifi);
        btnWifi.setVisibility(View.VISIBLE);
        btnWifi.setImageResource(R.drawable.wifi_disabled);
        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(FtpClientHelper.ftpMode) {
                    AlertDialog.Builder alert =
                            new AlertDialog.Builder(MainActivity.this);
                    alert.setTitle("WMusic");
                    alert.setMessage(getString(R.string.disconnect_wifi));
                    alert.setPositiveButton(getString(android.R.string.yes),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FtpClientHelper.FtpClientW.disconnect(new FtpClientHelper.FtpClientW.DisconnectListener() {
                                        @Override
                                        public void OnResult(final Exception ex) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mConnected = false;
                                                    checkConnected();
                                                    fadeAnimation();

                                                }
                                            });

                                        }
                                    });
                                }
                            });
                    alert.setNegativeButton(getString(android.R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    alert.show();

                    return;
                }
                AlertDialog.Builder normalDialog =
                        new AlertDialog.Builder(MainActivity.this);
                normalDialog.setTitle(getString(R.string.wifi_transfer));
                normalDialog.setMessage(getString(R.string.wifi_notice));
                normalDialog.setPositiveButton(getString(R.string.prepared),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String localIP = FtpClientHelper.getWifiIP();
                                if(localIP.isEmpty()){
                                    AlertDialog.Builder alert =
                                            new AlertDialog.Builder(MainActivity.this);
                                    alert.setTitle("WMusic");
                                    alert.setMessage(getString(R.string.enable_wifi));
                                    alert.setPositiveButton(getString(R.string.set_wifi),
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    MainActivity.this.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                                                }
                                            });
                                    alert.show();
                                }
                                else {
                                    showLoadingDlg("", getString(R.string.scanning));
                                    FtpClientHelper.scanLocalNetwork(localIP, DEFAULT_FTP_PORT, new FtpClientHelper.ScanNetworkResultListener() {
                                        @Override
                                        public void OnResult(ArrayList<FtpClientHelper.ScanNetworkItem> result) {
                                            closeLoadingDlg();
                                            if(result.isEmpty()) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        userInputIP();
                                                    }
                                                });

                                            } else {
                                                final String[] ipList = new String[result.size()];
                                                for (int idx = 0; idx < result.size(); idx++) {
                                                    ipList[idx] = result.get(idx).ip;
                                                }
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                        builder.setTitle(getString(R.string.choose_ip))
                                                                .setItems(ipList, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        connectFtp(ipList[which], DEFAULT_FTP_PORT);
                                                                    }
                                                                });
                                                        builder.create().show();
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }
                        });
                normalDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                    }
                });
                normalDialog.show();
                Node node = WearHelper.getNearByNode();
                if(node != null) {
                    WearManager.getInstance().launchAppOnNode("live.a23333.wmusic.WifiActivity", null, false, node);
                }
            }
        });

    }

    private void userInputIP() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.user_input_ip));
        // builder.setMessage("");
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.ip_input_view, null);
        final EditText ipInput = (EditText) view.findViewById(R.id.ip_input);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };
        ipInput.setFilters(filters);
        builder.setView(view);
        builder.setPositiveButton(getString(android.R.string.yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        String ip = ipInput.getText().toString();
                        Pattern IP_ADDRESS
                                = Pattern.compile(
                                "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                                        + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                                        + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                                        + "|[1-9][0-9]|[0-9]))");
                        Matcher matcher = IP_ADDRESS.matcher(ip);
                        if (matcher.matches()) {
                            connectFtp(ip, DEFAULT_FTP_PORT);
                        }
                        else {
                            Toast.makeText(MainActivity.this, getString(R.string.invalid_input_ip),Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void connectFtp(String ip, int port) {
        FtpClientHelper.FtpClientW.connect(ip, port, FTP_USER_NAME, FTP_PWD, new FtpClientHelper.FtpClientW.ConnectListener() {
            @Override
            public void OnResult(final Exception ex) {
                if(ex != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(ex.getMessage() != null && !ex.getMessage().isEmpty())
                                Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }
                else {
                    onNodeConnected();
                }
            }

        });
    }
    private void closeLoadingDlg(){
        closeLoadingNotif();
        if(pDlg != null)
            pDlg.cancel();
    }
    public boolean checkPlayStore() {
        String installer = getPackageManager()
            .getInstallerPackageName(getPackageName());
        if(installer != null && installer.equals("com.android.vending")) {
            return true;
        }
        else {
            final AlertDialog.Builder normalDialog =
                    new AlertDialog.Builder(MainActivity.this);
            normalDialog.setTitle("WMusic");
            normalDialog.setMessage(getString(R.string.play_store_warning));
            normalDialog.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String appPackageName = getPackageName();
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                            System.exit(0);
                        }
                    });
            normalDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    System.exit(0);
                }
            });
            normalDialog.show();
            return false;
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        if(isChineseVersion) {
            StatService.onPause(this);
        }
    }

    final int PERMISSION_REQ_CODE_READ_STORAGE = 0;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_CODE_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    final AlertDialog.Builder normalDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    normalDialog.setTitle("WMusic");
                    normalDialog.setMessage(getString(R.string.permission_required));
                    normalDialog.setPositiveButton(getString(android.R.string.yes),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    normalDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {

                        }
                    });
                    normalDialog.show();


                }
                return;
            }
        }
    }

    protected void reqPermissions(){
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQ_CODE_READ_STORAGE);
        }
    }

    @Override
    public void onFileSendStatus(String filePath, int statusCode) {
        if(statusCode == 0) {
            return;
        }
        if(mSendList != null && mSendList.size() > 0) {
            mSendList.remove(0);
        }
        final String fileName = filePath.substring(
                filePath.lastIndexOf("/") + 1);
        final int code = statusCode;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,
                        fileName + " " + getString(R.string.file_send_error) + ":" + code,
                        Toast.LENGTH_SHORT).show();
                showLoadingDlg("", getString(R.string.sending_file));
            }
        });

    }

    public static void ftpLsFiles() {
        FtpClientHelper.FtpClientW.lsFile("Music", new FtpClientHelper.FtpClientW.LsFileListener() {
            @Override
            public void OnResult(final ArrayList<WearHelper.MsgListFileNtf.FileInfo> files, final Exception ex) {
                if(ex != null) {
                    MainActivity.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(ex.getMessage() != null && !ex.getMessage().isEmpty())
                                Toast.makeText(MainActivity.instance, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                WearHelper.MsgListFileNtf ntf = new WearHelper.MsgListFileNtf();
                ntf.fileList = files;
                FileListFragment.instance.onFileListUpdate(ntf);
            }
        });
        MainActivity.instance.checkConnected();
    }

    @Override
    public void onNodeConnected() {
        if(FtpClientHelper.ftpMode) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bundle bundle = new Bundle();
                    bundle.putString("node", FtpClientHelper.connectedName);
                    mFirebaseAnalytics.logEvent("ftp_node_connected", bundle);
                    checkConnected();
                    fadeAnimation();
                }
            });
            ftpLsFiles();
        }
        else {
            Node node = WearHelper.getNearByNode();
            if (node == null) {
                return;
            }
            if (!isChineseVersion) {
                Bundle bundle = new Bundle();
                bundle.putString("node", WearHelper.getNearByNode().getDisplayName());
                mFirebaseAnalytics.logEvent("node_connected", bundle);
            } else {
                Properties prop = new Properties();
                prop.setProperty("node", WearHelper.getNearByNode().getDisplayName());
                StatService.trackCustomBeginKVEvent(this, "node_connected", prop);
            }
            try {
                WearHelper.sayHello(WearHelper.getNearByNode());
            } catch (Exception ex) {

            }
            reqFileList();
        }
    }

    @Override
    public void onHello() {
        mConnected = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkConnected();
            }
        });
    }

    private void fadeAnimation(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Animator animator = AnimatorInflater.loadAnimator(MainActivity.this, R.animator.fade_in);
                animator.setTarget(btnWifi);
                animator.setDuration(500);
                animator.start();
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if(FtpClientHelper.ftpMode) {
                            btnWifi.setImageResource(R.drawable.wifi_enabled);
                        }
                        else {
                            btnWifi.setImageResource(R.drawable.wifi_disabled);
                        }
                        btnWifi.invalidate();
                        Animator animator = AnimatorInflater.loadAnimator(MainActivity.instance, R.animator.fade_out);
                        animator.setTarget(btnWifi);
                        animator.setDuration(500);
                        animator.start();
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
    }
    public boolean checkConnected() {
        String connectedName = "";
        if(FtpClientHelper.ftpMode) {
            if(mConnected == false) {
                fadeAnimation();
            }
            mConnected = true;
            connectedName = FtpClientHelper.connectedName;
        }
        else {
            if(btnWifi != null) {
                btnWifi.setImageResource(R.drawable.wifi_disabled);
            }
            Node node = WearHelper.getNearByNode();
            if(node == null) {
                mConnected = false;
            }
            else {
                connectedName = node.getDisplayName();
            }
        }
        if(mConnected){
            snack = Snackbar.make(fab,
                    getString(R.string.status_connected) + ": " + connectedName, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null);
            snack.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.snackbarColor));
            snack.show();
            return true;
        }
        WearHelper.MsgListFileNtf ntf = new WearHelper.MsgListFileNtf();
        if(ntf == null) {
            return false;
        }
        ntf.fileList = new ArrayList<>();
        try {
            FileListFragment.instance.onFileListUpdate(ntf);
        }
        catch (Exception ex) {

        }
        snack = Snackbar.make(fab,
                getString(R.string.status_not_connected), Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null);
        snack.getView().setBackgroundColor(
                ContextCompat.getColor(this, R.color.colorPrimary));
        snack.show();
        if(!FtpClientHelper.ftpMode) {
            try {
                WearHelper.sayHello(WearHelper.getNearByNode());
            } catch (Exception ex) {

            }
        }
        return false;
    }

    @Override
    public void onDelFileResult(final WearHelper.MsgDelFileRet ret) {
        WearHelper.selectedMap.remove(ret.filePath);
        this.OnFileSelectedChange(WearHelper.selectedMap);
        final String fileName = ret.filePath.substring(
                ret.filePath.lastIndexOf("/") + 1);
        if(!isChineseVersion) {
            Bundle bundle = new Bundle();
            bundle.putString("file", fileName);
            if(FtpClientHelper.ftpMode) {
                mFirebaseAnalytics.logEvent("ftp_del_file_ret", bundle);
            }
            else {
                mFirebaseAnalytics.logEvent("del_file_ret", bundle);
            }

        }
        else {
            Properties prop = new Properties();
            prop.setProperty("file", fileName);
            StatService.trackCustomBeginKVEvent(this, "del_file_ret", prop);
        }
        cbSelectAll.post(new Runnable() {
            @Override
            public void run() {
                String deleteResult;
                if(ret.isSuccess) {
                    deleteResult = getString(R.string.file_deleted);
                }
                else {
                    deleteResult = getString(R.string.file_deleted_failed);
                }
                if(!ret.isSuccess) {
                    Toast.makeText(MainActivity.this,
                            fileName + deleteResult, Toast.LENGTH_SHORT)
                            .show();
                }
                closeLoadingDlg();
                reqFileList();
            }
        });
    }

    public void reqFileList() {
        if(FtpClientHelper.ftpMode) {
            ftpLsFiles();
        }
        else {
            try {
                WearHelper.getWearFileListReq(
                        WearHelper.getNearByNode(), getString(R.string.music_path));
            } catch (Exception ex) {
                Log.e("WearHelper", ex.getMessage());
            }
        }
    }

    @Override
    public void onFileSendResult(String filePath, boolean success) {

        final String fileName = filePath.substring(
                filePath.lastIndexOf("/") + 1);

        if(!isChineseVersion) {
            Bundle bundle = new Bundle();
            bundle.putString("file", fileName);
            if(FtpClientHelper.ftpMode) {
                mFirebaseAnalytics.logEvent("ftp_send_file_ret", bundle);
            }
            else {
                mFirebaseAnalytics.logEvent("send_file_ret", bundle);
            }
        }
        else {
            Properties prop = new Properties();
            prop.setProperty("file", fileName);
            StatService.trackCustomBeginKVEvent(this, "send_file_ret", prop);
        }
        if(mSendList != null && mSendList.size() > 0) {
            mSendList.remove(0);
        }
        String resultText;
        if(!success) {
            resultText = getString(R.string.file_sent_failed);
        }
        else {
            resultText = getString(R.string.file_sent);
        }
        final String sendResult = resultText;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, fileName + sendResult,
                        Toast.LENGTH_SHORT)
                        .show();
                closeLoadingDlg();
            }
        });
        reqFileList();
    }

    @Override
    public void OnFileSelectedChange(Map<String, String> selectedMap) {
        if(selectedMap.size() == 0){
            mSelectMode = false;
        }
        else {
            mSelectMode = true;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mSelectMode) {
                    cbSelectAll.setVisibility(View.VISIBLE);
                    btnWifi.setVisibility(View.INVISIBLE);
                }
                else {
                    cbSelectAll.setVisibility(View.INVISIBLE);
                    btnWifi.setVisibility(View.VISIBLE);
                }
                updateFab();
            }
        });
    }

    public void delOneSelected(){
        if(WearHelper.selectedMap.size() == 0) {
            if(FtpClientHelper.ftpMode) {
                ftpLsFiles();
            }
            else {
                try {
                    WearHelper.getWearFileListReq(
                            WearHelper.getNearByNode(), getString(R.string.music_path));
                }
                catch (Exception ex) {
                    Log.e("WearHelper", ex.getMessage());
                }
                return;
            }

        }
//        WearManager.getInstance().launchAppOnNode(
//                "live.a23333.wmusic.SyncActivity", null, false, WearHelper.getNearByNode());
        for (Map.Entry<String, String> entry : WearHelper.selectedMap.entrySet()) {
            try {
                if(!isChineseVersion) {
                    Bundle bundle = new Bundle();
                    bundle.putString("file", entry.getKey());
                    mFirebaseAnalytics.logEvent("del_action", bundle);
                }
                else {
                    Properties prop = new Properties();
                    prop.setProperty("file", entry.getKey());
                    StatService.trackCustomBeginKVEvent(this, "del_action", prop);
                }
                if(FtpClientHelper.ftpMode) {
                    FtpClientHelper.FtpClientW.delFile(entry.getKey(), new FtpClientHelper.FtpClientW.DelFileListener() {
                        @Override
                        public void OnResult(String file, Exception ex) {
                            WearHelper.MsgDelFileRet ret = new WearHelper.MsgDelFileRet();
                            ret.filePath = file;
                            if(ex == null) {
                                ret.isSuccess = true;
                            }
                            else {
                                ret.isSuccess = false;
                                ret.msg = ex.getMessage();
                            }
                            onDelFileResult(ret);
                        }
                    });
                }
                 else {
                    WearHelper.delWearFile(WearHelper.getNearByNode(), entry.getKey());
                }

                String filePath = entry.getKey();
                final String fileName = filePath.substring(
                        filePath.lastIndexOf("/") + 1);
                showLoadingDlg(getString(R.string.deleting_file), fileName);
                return;
            }
            catch (Exception ex){
                Log.e("WearHelper", ex.getMessage());
            }
        }

    }

    private static int GET_FILE_ACTION_REQUEST = 0X01;
    public void OnAddAction(){

        String[] filters = {".wav",".mp3", ".m4a", ".ogg", ".aac", ".flac"};
        new LFilePicker()
                .withFileFilter(filters)
                .withBackgroundColor("#6c41b9")
                .withTitle(getString(R.string.select_files))
                .withActivity(MainActivity.this)
                .withRequestCode(GET_FILE_ACTION_REQUEST)
                .start();
//        Intent i = new Intent(this, FilePickerActivity.class);
//        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
//        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
//        i.putExtra(FilePickerActivity.EXTRA_START_PATH, "/sdcard/");
//        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//        startActivityForResult(i, GET_FILE_ACTION_REQUEST);
        overridePendingTransition(R.anim.right_bottom_up, R.anim.no_animation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_FILE_ACTION_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                List<String> files = data.getStringArrayListExtra("paths");
                // List<Uri> files = Utils.getSelectedFilesFromResult(data);
                for (String file: files) {
                    // File file = Utils.getFileForUri(uri);
                    addToSendList(file);
                }
                sendOneFile();
            }
            else {
                // Toast.makeText(this, getString(R.string.select_err), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static int PROGRESS_NTF_ID = 1;
    private void showLoadingNotif(String title, String text) {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.shuffle_icon)
                .setContentTitle(title)
                .setContentText(text);
        // builder.setOngoing(true);
        PendingIntent i =
                PendingIntent.getActivity(this, 0, getIntent()
                        , PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(i);
        notificationManager.notify(PROGRESS_NTF_ID, builder.build());
    }

    private void closeLoadingNotif() {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancel(PROGRESS_NTF_ID);
    }
    private void sendOneFile() {
        if(mSendList == null || mSendList.size() == 0) {
            return;
        }
        final String filePath = mSendList.get(0);
        String path = filePath.substring(0, filePath.lastIndexOf("/"));
        String name = filePath.substring(filePath.lastIndexOf("/") + 1);
        if(isExist(name)) {
            mSendList.remove(0);
            Toast.makeText(this, name + " " + getString(R.string.file_exist_skip), Toast.LENGTH_SHORT).show();
            sendOneFile();
            return;
        }
        if(FtpClientHelper.ftpMode) {
            FtpClientHelper.FtpClientW.sendFile(path + "/" + name, "Music", new FtpClientHelper.FtpClientW.SendFileListener() {
                @Override
                public void OnResult(String file, Exception ex) {
                    onFileSendResult(filePath, ex == null);
                }
            });
        }
        else {
            WearHelper.sendFile(WearHelper.getNearByNode(), path, name);
        }
        showLoadingDlg(getString(R.string.sending_file), name);
        if(!isChineseVersion) {
            Bundle bundle = new Bundle();
            bundle.putString("file", filePath);
            mFirebaseAnalytics.logEvent("send_action", bundle);
        }
        else {
            Properties prop = new Properties();
            prop.setProperty("file", filePath);
            StatService.trackCustomBeginKVEvent(this, "send_action", prop);
        }
        return;
    }

    protected boolean isExist(final String fileName) {
        if(FileListRecycleAdapter.mfileList == null) {
            return false;
        }
        List<WearHelper.MsgListFileNtf.FileInfo> fileList = FileListRecycleAdapter.mfileList;
        for (WearHelper.MsgListFileNtf.FileInfo file : fileList) {
            if(file.fileName.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isChineseVersion) {
            StatService.onResume(this);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        // finishAndRemoveTask();
    }

    private List<String> mSendList;
    private void addToSendList(String filePath) {
        File file = new File(filePath);
        String absPath = file.getAbsolutePath();
        if(file.exists()) {
            Log.d("SelectFile:", file.getAbsolutePath());
            String ext = filePath.substring(filePath.lastIndexOf(".") + 1);
            ext = ext.toLowerCase();
            if(ext.equals("wav") || ext.equals("mp3") || ext.equals("m4a")
                    || ext.equals("ogg") || ext.equals("aac") || ext.equals("flac")){
                if(mSendList == null) {
                    mSendList = new ArrayList<>();
                }
                mSendList.add(filePath);
                return;
            }
        }
        Log.d("UnknownFile:", file.getAbsolutePath());
        Toast.makeText(this,
                getString(R.string.unsupported_file) + absPath,
                Toast.LENGTH_SHORT).show();
    }

    private void updateFab(){
        if(mSelectMode) {
            fab.setImageResource(R.drawable.del_icon);
            ColorStateList csl = new ColorStateList(new int[][]
                    { new int[0] }, new int[]{ContextCompat.getColor(this, R.color.colorPrimary)});
            fab.setBackgroundTintList(csl);
            fab.show();
        }
        else {
            fab.setImageResource(R.drawable.plus_icon);
            ColorStateList csl = new ColorStateList(new int[][]
                    { new int[0] }, new int[]{ContextCompat.getColor(this, R.color.colorAccent)});
            fab.setBackgroundTintList(csl);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        closeLoadingDlg();
        // if(!isChineseVersion)
            // DonateMgr.uninitPlayDonate();
        super.onDestroy();
    }

}
