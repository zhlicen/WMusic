package live.a23333.wmusic;

/**
 * Created by zhlic on 5/19/2017.
 */
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.wearable.view.WearableRecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;

public class StartWifiFragment extends Fragment {

    private Context mContext;
    public static StartWifiFragment newInstance(Context context) {
        StartWifiFragment fragment = new StartWifiFragment();
        fragment.mContext = context;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = inflater.getContext();
        View view = inflater.inflate(R.layout.start_wifi_view, container,false);

        Button btnStartView = (Button) view.findViewById(R.id.btn_start_wifi);
        btnStartView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WifiActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }


}