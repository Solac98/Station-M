package com.example.ui.Nav.Live;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ui.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LiveResult extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private LiveData result;
    private TextView line, title, dir, data, time;
    private EditText cmtData;
    private String formatDate;

    // 리스트뷰
    private ListView listView;
    // 어댑터
    private LiveCmtAdapter adapter;
    //서버 주소
    private static String IP_ADDRESS = "www.solac.shop";
    private static String TAG = "phptest";
    private String mJsonString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_result);

        // 현재시간을 msec 으로 구한다.
        long now = System.currentTimeMillis();
        // 현재시간을 date 변수에 저장한다.
        Date date = new Date(now);
        // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
        SimpleDateFormat sdfNow = new SimpleDateFormat("MM/dd HH:mm");
        // nowDate 변수에 값을 저장한다.
        formatDate = sdfNow.format(date);

        line = (TextView) findViewById(R.id.live_line);
        title = (TextView) findViewById(R.id.live_title);
        dir = (TextView) findViewById(R.id.live_dir);
        data = (TextView) findViewById(R.id.live_data);
        cmtData = (EditText) findViewById(R.id.live_cmt_data);
        time = (TextView) findViewById((R.id.live_time));

        listView = (ListView) findViewById(R.id.live_cmt_list);
        listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        Intent intent = getIntent();
        result = (LiveData) intent.getSerializableExtra("result");

        //어댑터 생성
        adapter = new LiveCmtAdapter(result.getNum());
        // 어뎁터를 리스트뷰에 세팅한다.
        listView.setAdapter(adapter);
        // 리스트뷰에 아이템클릭리스너를 등록한다.
        listView.setOnItemClickListener(this);
        //초기 댓글 가져오기
        updateCmt();

        this.setData(result);

        //키보드 올라올 때 EditText 가림 해결
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }
    @Override
    protected void onResume() {
        super.onResume();
        listView.setAdapter(adapter);

    }
    public void OnClick(View v){
        if(v.getId() == R.id.live_add){

            InsertData task = new InsertData();
            task.execute("http://" + IP_ADDRESS + "/insertcmt.php", result.getNum(), cmtData.getText().toString(), formatDate);
            this.updateCmt();
        }
    }
    public void updateCmt(){
        GetData task = new GetData();
        task.execute("http://" + IP_ADDRESS + "/getCmt.php", result.getNum());

    }
    public void setData(LiveData result){

        line.setText(result.getLine() + "호선 게시글");
        title.setText(result.getTitle());
        dir.setText("열차정보 : " + result.getDir());
        data.setText(result.getData());
        time.setText(result.getTime());

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //////////////////
    }
    //PHP로 데이터 넣기
    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(LiveResult.this,
                    "Please Wait", null, true, true);
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "데이터 삽입 완료^^", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "POST response  - " + result);
        }
        @Override
        protected String doInBackground(String... params) {

            String num = (String)params[1];
            String cmtData = (String)params[2];
            String time = (String)params[3];

            String serverURL = (String)params[0];
            String postParameters = "num=" + num + "&cmtData=" + cmtData + "&time=" + time;
            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String lines = null;

                while((lines = bufferedReader.readLine()) != null){
                    sb.append(lines);
                }


                bufferedReader.close();


                return sb.toString();


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }

    private class GetData extends AsyncTask<String, Void, String> {
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        //데이터 다 가져오면 작동하는 메서드.
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result == null) {
            } else {
                adapter.clearItem();
                mJsonString = result;
                showResult();
                onResume();
            }
        }


        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String num = (String)params[1];
            String postParameters = "num=" + num;

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString().trim();
            } catch (Exception e) {
                Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();
                return null;
            }

        }
    }
    private void showResult() {

        String TAG_JSON = "실시간데이터";
        String TAG_cmtData = "cmtData";
        String TAG_time = "time";


        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject item = jsonArray.getJSONObject(i);

                String data = item.getString(TAG_cmtData);
                String time = item.getString(TAG_time);
                LiveCmtData cmtData = new LiveCmtData(data, time);
                adapter.addItem(cmtData);
            }
        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }
    }
}
