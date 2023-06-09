package com.example.mylink_10.activity;

import static com.example.mylink_10.util.getValuesUtil.getStrValue;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mylink_10.R;
import com.example.mylink_10.util.ThemeUtil;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class ModeSelectionActivity extends AppCompatActivity implements View.OnClickListener {
    public String ip = "1.15.76.132:8080";
    private String token;
    private Competition competition;
    private boolean flag = false;
    AlertDialog dialog;

    public int qiPan;

    public static class Competition {
        public int sign;
        public String start;
        public boolean end;
        public String grade;
        public int checkerboard;
        public Player[] players;
    }

    public static class Player {
        public String username;
        public boolean connStatus;
        public int score;
        public float sumTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_selection);
        findViewById(R.id.btn_standalone).setOnClickListener(this);
        findViewById(R.id.btn_online).setOnClickListener(this);
        findViewById(R.id.btn_challenge).setOnClickListener(this);
        token = getStrValue(this, "token");
        if ("".equals(token)) {
            findViewById(R.id.btn_online).setEnabled(false);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.btn_standalone:
                intent = new Intent(this, XiuxianActivity.class);
                startActivity(intent);
//                finish();
                break;
            case R.id.btn_online:
                new JoinCompetition().execute();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 弹出取消匹配按钮，按下发出删除比赛请求
                        // 然后
                        showCancelButton();
                    }
                });
                break;
            case R.id.btn_challenge:
                intent = new Intent(this, NewGameActivity.class);
                startActivity(intent);
                break;
        }
    }

    private class JoinCompetition extends AsyncTask<String, String, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            try {
                Random random = new Random();
                qiPan = random.nextInt(100);
                //这里的username和棋盘号写死了，
                //后续需要登陆后获取username，并且随机一个棋盘号
                String joinUrl = "http://" + ip + "/competition/my/joinCompetition?checkerboard="+qiPan;
                URL url = new URL(joinUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", token);
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                InputStream inputStream = connection.getInputStream();
                competition = getCompetition(inputStream);
                //flag表示是否一场比赛有两个对象？
                while (!flag) {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        if (!competition.players[0].username.isEmpty() && !competition.players[1].username.isEmpty()) {
                            flag = true;
                            runOnUiThread(() -> {
                                Toast.makeText(ModeSelectionActivity.this, "匹配成功", Toast.LENGTH_SHORT).show();
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            dialog.cancel();
                            Intent intent = new Intent(ModeSelectionActivity.this, DuizhanActivity.class);
                            Gson gson = new Gson();
                            String competitionJson = gson.toJson(competition);
                            intent.putExtra("competition", competitionJson);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            joinUrl = "http://" + ip + "/competition/my/OpponentFound?sign=" + competition.sign;
                            url = new URL(joinUrl);
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");
                            responseCode = connection.getResponseCode();
                            inputStream = connection.getInputStream();
                            competition = getCompetition(inputStream);
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(ModeSelectionActivity.this, "网络出现了一些问题", Toast.LENGTH_SHORT).show();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return false;
        }

        private Competition getCompetition(InputStream inputStream) throws IOException, JSONException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();
            String serverResponse = response.toString();
            JSONObject responseJson = new JSONObject(serverResponse);
            String data = responseJson.optString("data");

            Gson gson = new Gson();
            //获取到比赛信息
            return gson.fromJson(data, Competition.class);
        }
    }

    private void showCancelButton() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("正在匹配中");
        builder.setMessage("请等待一会儿");
        builder.setPositiveButton("取消匹配", (dialog, which) -> {
            //发送网络请求，告知服务端，该比赛取消
            Thread thread = new Thread(() -> {
                try {
                    URL url = new URL("http://" + ip + "/competition/my/finishGame?room=" + competition.sign);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("取消", "取消成功 ");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            runOnUiThread(() -> {
                Toast.makeText(ModeSelectionActivity.this, "取消匹配成功", Toast.LENGTH_SHORT).show();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }
}
