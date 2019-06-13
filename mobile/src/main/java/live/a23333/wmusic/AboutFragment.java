package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

public class AboutFragment extends Fragment {
    static View v;
    private RecyclerView recyclerView;
    private Context mContext;
    static AboutFragment instance;
    public static AboutFragment newInstance(Context context) {
//        if(instance != null) {
//            return instance;
//        }
        AboutFragment fragment = new AboutFragment();
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
//        if(v != null) {
//            return v;
//        }
        mContext = inflater.getContext();
        View view = inflater.inflate(R.layout.tab_about, container,false);
        recyclerView = (RecyclerView)view.findViewById(R.id.about_page);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        AboutRecycleAdapter adapter = new AboutRecycleAdapter(mContext);
        recyclerView.setAdapter(adapter);
        v = view;
        return view;
    }
}