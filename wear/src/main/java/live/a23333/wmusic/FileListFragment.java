package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.wearable.view.WearableRecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

public class FileListFragment extends Fragment {

    public static FileListFragment instance;
    private Context mContext;
    private View v;
    private WearableRecyclerView recyclerView;
    public static FileListFragment newInstance(Context context) {
        FileListFragment fragment = new FileListFragment();
        fragment.mContext = context;
        instance = fragment;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = inflater.getContext();
        instance = this;
        View view;
        boolean isRound = true;
        try {
            isRound = mContext.getResources().getConfiguration().isScreenRound();
        }
        catch (Exception ex) {

        }

        if(isRound) {
           view = inflater.inflate(R.layout.file_list_view, container,false);
        }
        else {
            view = inflater.inflate(R.layout.file_list_view_rect, container,false);
        }
        v = view;
        recyclerView = (WearableRecyclerView) view.findViewById(R.id.file_list_recycler);
        recyclerView.setAdapter(new FileListRecyclerAdapter(mContext));
        if(mContext.getResources().getConfiguration().isScreenRound()) {
            recyclerView.setCenterEdgeItems(true);
        }
        return view;
    }

    public static void updateSelect() {
        if(instance != null && instance.recyclerView != null &&
                !instance.getUserVisibleHint()) {
            if(MainActivity.mPlayer != null && MainActivity.mPlayer.playList != null &&
                    MainActivity.mPlayer.playList.currentIndex != -1)
                instance.recyclerView.scrollToPosition(MainActivity.mPlayer.playList.currentIndex + 1);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(!isVisibleToUser) {
            updateSelect();
        }
    }
}