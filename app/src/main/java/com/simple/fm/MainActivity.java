package com.simple.fm;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.eguan.drivermonitor.b.b;
import com.simple.fm.manager.NetRequestManager;
import com.simple.fm.model.SearchResult;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.com.iresearch.mapptracker.util.DataProvider;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends Activity {

    private TextView mTitle;

    private SearchResultAdapter mSearchResultAdapter;
    private RecyclerView mSearchResultView;
    private View.OnClickListener mOnSearchResultClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = mSearchResultView.getChildLayoutPosition(v);
            if (position >=0 && position < mSearchResultAdapter.getItemCount()){
                SearchResult result = mSearchResultAdapter.getData(position);
                if (result!=null){
                    play(result);
                    setTitle(result);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        setActionBar(toolbar);

        mTitle = (TextView) findViewById(R.id.play_title);

        Player.getInstance().start(this);

        MediaCenter.getInstance().fetchList();


//        String deviceId = DeviceId.getDeviceId(this);
//        Output.show("deviceId:" + deviceId);
//        String vvuid = DataProvider.getVVUid();
//        Output.show("vvuid:" + vvuid);
//        Output.show("pd:" + DataProvider.pd());
//
//        Output.show("brand:" + Build.BRAND + "board:" + Build.BOARD);
//        Output.show(b.e);
//        Output.show(b.f);
        initSearch();
//        String origin = "{\"event_list\":[],\"header\":{\"app_key\":\"833c6d6eb8031de1\",\"uid\":\" 4E98018775981802FAEA399F54C7796E\",\"uidtype\":\"Meizu M3s\",\"ip\":\"\",\"imei\":\"qingtingFM_android\",\"appid\":\"fm.qingting.qtradio\",\"appver\":\"6.2.0\",\"mac_hash\":\"\",\"network\":\"WIFI\",\"carrier\":\"01\",\"country\":\"460\",\"city\":\"1280*720\",\"timezone\":\"28800\",\"os_name\":\"Android\",\"os_ver\":\"5.1\",\"sdk_ver\":\"2.3.3\",\"channel\":\"unknown\",\"col1\":\"3d6e80e3259ccbd\",\"col2\":\"869966022742409\",\"col3\":\"40:c6:2a:87:96:fc\",\"ts\":\"1488181354\",\"dd\":\"30170227\",\"lac_cid\":\"2_2\"},\"open_count\":1,\"page_count\":1,\"run_time\":0,\"page_list\":[],\"lat\":\"\",\"lng\":\"1\"}";
//        mockEncode(DataProvider.pd(), origin);

    }

    private void mockEncode(String pd, String origin) {
        byte[] keyBytes = decodeBytes(pd, "Tvb!@#RS".getBytes());
        if (keyBytes != null) {
            Output.show("got keybytes");
//            byte[] result = encodeBytes(origin.getBytes(Charset.forName("UTF-8")), Arrays.toString(keyBytes));
//            if (result != null) {
//                Output.show(Arrays.toString(result));
//            }
        }

    }

//    private void mockEncode(String pd, String origin) {
//        String v5 = "Tvb!@#RS";
//        byte[] bytes = v5.getBytes();
//        byte[] keyBytes = {1, 3, 7, 15, 31, 63, 127, -1};
//        byte[] v0 = encodeBytes(pd, v5.getBytes());
//        pd.substring(0, 3) + v0[0] + pd.substring(3);
//
//        int length = origin.length();
//        int v7 = length/400;
//        int v8 = length%400;
//        if (0<v7) {
//            String rest = origin.substring(length - v8);
//            byte[] temp = encodeBytes(rest.getBytes(), pd);
//
//        }
//
//        StringBuilder v6 = new StringBuilder(pd.substring(0, 3));
//        v6.append(output[0]);
//        v6.append(pd.substring(3));
//        int v7 = origin.length()/400;
//        int v8 = origin.length()%400;
//        if (0 < v7) {
//            int v0 = origin.length() - v8;
//            String rest = origin.substring(v0);
//        }
//    }
//
    private byte[] encodeBytes(byte[] p0, String p1) {
        byte[] v0 = p1.getBytes();
        byte[] keyBytes = {1, 2, 3, 4, 5, 6, 7, 8};
        IvParameterSpec spec = new IvParameterSpec(keyBytes);
        SecretKeySpec secretKeySpec = new SecretKeySpec(v0, "DES");
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(1, secretKeySpec, spec);
            return cipher.doFinal(p0);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] decodeBytes(String p0, byte[] p1) {
        byte[] v0 = new byte[]{-54,-124,-10,17,115,-78,-82,-5,-113,113,-71,20,-92,7,-104,-90,69,113,-84,18,1,42,17,-55,70,13,-20,-2,16,26,-55,-29,-45,14,-46,66,-49,-29,3,-77,83,69,-5,-98,63,-116,-76,29,-77,-113,-29,-68,31,52,-10,-34,53,-33,74,14,-37,38,72,120,-32,-126,-56,52,-38,40,-19,-32,-117,70,113,-114,-8,-94,90,-40,7,-49,-13,-114,-22,73,-21,-19,-19,-67,36,-40,-69,-4,-122,25,27,-62,51,-37,85,1,-56,-57,100,-120,-9,-122,83,69,-34,75,-107,-64,-86,10,-20,14,-49,-14,99,-59,16,-66,-2,-80,-65,63,-43,96,42,-71,-45,58,72,-66,83,14,-106,-25,42,-128,-68,48,121,-103,91,-81,32,51,-16,101,-47,-22,-32,51,-79,26,48,10,63,-52,-77,109,-6,49,-68,-48};
        byte[] keyBytes = {1, 2, 3, 4, 5, 6, 7, 8};
        IvParameterSpec spec = new IvParameterSpec(keyBytes);
        SecretKeySpec secretKeySpec = new SecretKeySpec(p1, "DES");
        try {
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);
            return cipher.doFinal(v0);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void initSearch(){
        SearchView searchView = (SearchView) findViewById(R.id.search);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query);
                findViewById(R.id.info).setVisibility(View.GONE);
                mSearchResultView.setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)){
                    findViewById(R.id.info).setVisibility(View.VISIBLE);
                    mSearchResultView.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });

        mSearchResultView = (RecyclerView) findViewById(R.id.search_result);
        mSearchResultView.setLayoutManager(new LinearLayoutManager(this));
        mSearchResultAdapter = new SearchResultAdapter(this,mOnSearchResultClickListener);
        mSearchResultView.setAdapter(mSearchResultAdapter);
    }

    @SuppressWarnings("unchecked")
    private void search(String keyword){
        NetRequestManager.getInstance().get(
                NetRequestManager.Config.SEARCH.getUrl(keyword, DeviceId.getDeviceId(this)),
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()){
                            List<SearchResult> result = (List<SearchResult>) Parser.parse(
                                    Parser.CLASSTYPE.SEARCH,response.body().string());
                            setSearchResult(result);

                        }
                    }
                });
    }

    private void setSearchResult(final List<SearchResult> result){
        if (result!=null){
            for (SearchResult sr:result){
                Output.show(sr.toString());
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSearchResultAdapter.setData(result);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
//        searchView.setQueryHint("SimpleFM");
//        searchView.setIconifiedByDefault(true);
//        searchView.setIconified(true);
//        searchView.onActionViewExpanded();
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
//            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void play(SearchResult result){
        Player.getInstance().stop();
        Player.getInstance().setSource(MediaCenter.getInstance().getUrl(result.getChannelId(),24,
                DeviceId.getDeviceId(this)));
        Player.getInstance().play();
    }

    private void setTitle(SearchResult result){
        mTitle.setText(result.getName());
    }

    public void playOrStop(View view){
        if (Player.getInstance().isPlaying()) {
//            Player.getInstance().pause();
            Player.getInstance().stop();
        } else if (Player.getInstance().isPaused()){
            Player.getInstance().resume();
        } else {
//            Player.getInstance().setSource("http://wsod.qingting.fm/m4a/5787c8767cb8910243186acb_5468278_24.m4a");
//            Player.getInstance().setSource(MediaCenter.getInstance().getUrl(386,24,DeviceId.getDeviceId(this)));
            Player.getInstance().play();
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        moveTaskToBack(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


}

class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.InnerViewHolder>{
    private List<SearchResult> mDatas;
    private Context context;
    private View.OnClickListener mOnClickListener;


    protected SearchResultAdapter(Context context, View.OnClickListener listener){
        this.context = context;

        mOnClickListener = listener;
    }

    protected void setData(List<SearchResult> data){
        mDatas = data;
        notifyDataSetChanged();
    }

    protected SearchResult getData(int position){
        if (mDatas!=null && position >=0 && position < mDatas.size()){
            return mDatas.get(position);
        }
        return null;
    }


    @Override
    public SearchResultAdapter.InnerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1,parent,false);
        InnerViewHolder viewHolder = new InnerViewHolder(itemView);
        itemView.setOnClickListener(mOnClickListener);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(SearchResultAdapter.InnerViewHolder holder, int position) {
        SearchResult result = mDatas.get(position);
        holder.setText(result.getName());
    }

    @Override
    public int getItemCount() {
        return mDatas == null?0:mDatas.size();
    }

    static class InnerViewHolder extends RecyclerView.ViewHolder{
        private TextView mTextView;
        private SearchResult mResult;

        public InnerViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(android.R.id.text1);
        }

        void setText(String text){
            mTextView.setText(text);
        }
    }
}
