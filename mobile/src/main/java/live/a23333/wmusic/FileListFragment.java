package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */
import android.content.Context;
import android.media.midi.MidiDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Toast;

public class FileListFragment extends Fragment implements WearHelper.OnFileListUpateListener {
    private Context mContext;
    private RecyclerView recyclerView;
    FileListRecycleAdapter fileListAdapter;
    SwipeRefreshLayout swipeContainer;
    static View v;
    public static FileListFragment instance;
    public static FileListFragment newInstance(Context context) {
        Bundle args = new Bundle();
        FileListFragment fragment = new FileListFragment();
        fragment.setArguments(args);
        fragment.mContext = context;
        instance = fragment;
        return fragment;
    }

    @Override
    public void onFileListUpdate(final WearHelper.MsgListFileNtf ntf) {
        swipeContainer.post(new Runnable() {
            @Override
            public void run() {
                if(swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }
                fileListAdapter.setFileList(ntf.fileList);
            }
        });

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        instance = this;
        mContext = inflater.getContext();
        WearHelper.fileListUpateListener = this;
        View view = inflater.inflate(R.layout.tab_file_list, container, false);
        recyclerView = (RecyclerView)view.findViewById(R.id.file_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        if(fileListAdapter == null) {
            fileListAdapter = new FileListRecycleAdapter(mContext);
        }
        recyclerView.setAdapter(fileListAdapter);
        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    try {
                        if(FtpClientHelper.ftpMode) {
                            MainActivity.ftpLsFiles();
                        }
                        else {
                            WearHelper.sayHello(WearHelper.getNearByNode());
                        }

                    }
                    catch (Exception ex) {

                    }
                    WearHelper.selectedMap.clear();
                    WearHelper.fileSelectedChange.OnFileSelectedChange(WearHelper.selectedMap);
                    WearHelper.getWearFileListReq(WearHelper.getNearByNode(),
                            getString(R.string.music_path));
                }
                catch(Exception ex) {

                }
                swipeContainer.setRefreshing(false);

            }
        });
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                if (dy > 0)
                    MainActivity.fab.hide();
                else if (dy < 0)
                    MainActivity.fab.show();
            }
        });
        v = view;
        return view;
    }
}