package com.example.mylink_10.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mylink_10.R;
import com.example.mylink_10.gameRelated.Game;
import com.example.mylink_10.gameRelated.GameConf;
import com.example.mylink_10.gameRelated.GameView;
import com.example.mylink_10.pojo.AddressUrl;
import com.example.mylink_10.util.DateFormatUtil;
import com.example.mylink_10.util.ThemeUtil;
import com.example.mylink_10.util.ToastUtil;
import com.example.mylink_10.util.getValuesUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class XiuxianActivity extends AppCompatActivity {
    private AlertDialog.Builder win, lost, exitt;
    private EditText et;
    private final int finalTime = 0;
    private GameView gameView;
    private Game game;
    private int tim;
    private TextView num;
    private Timer timer;
    private Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0x666) {
                tim += 100;
                num.setText("用时: " + getTimeStr());
            }
        }
    };
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("receiver", "ok");
            stopTimer();
            win.setMessage("用时" + getTimeStr());
            win.show();
            String token = getValuesUtil.getStrValue(XiuxianActivity.this, "token");
            if ("".equals(token)) {
                ToastUtil.show(XiuxianActivity.this, "还没有登录，无法进行分数上传，请登录以获取更多体验");
            } else {
                new HttpRequestTask().execute();
            }
        }
    };

    private void setBroadcast() { // 注册广播接收器，它接收GameView里发出的游戏胜利信息
        IntentFilter intentFilter = new IntentFilter("action_win");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastReceiver, intentFilter);
    }

    private String getNow() {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm-yyyy/MM/dd");
        return dateTime.format(formatter);
    }

    @Override
    public void onDestroy() { // 注销广播接收器
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
    }

    private void start() {
        gameView.postInvalidate();
        game.startNewGame();
        tim = finalTime;
        num = findViewById(R.id.num_xiuxian);
        startTimer();
    }

    private void startTimer() {
        stopTimer();
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(0x666);
                }
            }, 0, 100);
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xiuxian);
        init();
    }

    /**
     * 游戏初始化
     */
    private void init() {
        GameConf.init(this, getApplicationContext());
        game = new Game();
        gameView = findViewById(R.id.gv_xiuxian);
        gameView.start(game, false);
        // et = new EditText(getApplicationContext());
        win = new AlertDialog.Builder(this).setTitle("完成!").setIcon(R.drawable.success)
                .setNeutralButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton("再来一次", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        start();
                    }
                }).setCancelable(false);
        start();
        setBroadcast();
    }

    private String getTimeStr() {
        String ret = Float.toString((float) (1.0 * tim / 1000));
        int dian = ret.indexOf(".");
        String s1 = ret.substring(0, dian);
        String s2 = ret.substring(dian, dian + 2);
        ret = s1 + s2 + "s";
        return ret;
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(AddressUrl.url + "/my/rank/createRankLow?date=" + DateFormatUtil.getTime() + "&time=" + getTimeStr());
                int selection = getValuesUtil.getIntValue(XiuxianActivity.this, "dif");
                Log.d("Time:",getTimeStr());
                switch (selection) {
                    case 0:
                        url = new URL(AddressUrl.url + "/rank/my/createRankLow?date=" + DateFormatUtil.getTime() + "&time=" + getTimeStr());
                        break;
                    case 1:
                        url = new URL(AddressUrl.url + "/rank/my/createRankMedium?date=" + DateFormatUtil.getTime() + "&time=" + getTimeStr());
                        break;
                    case 2:
                        url = new URL(AddressUrl.url + "/rank/my/createRankHigh?date=" + DateFormatUtil.getTime() + "&time=" + getTimeStr());
                        break;
                }
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                //允许输出并写入数据
//                connection.setDoOutput(true);
//                connection.setDoInput(true);
                connection.addRequestProperty("Authorization", getValuesUtil.getStrValue(XiuxianActivity.this, "token"));

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return "记录上传成功";
                } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    return "登录超时，请重新登录后再试";
                } else {
//                    return "GET request failed. Response Code: " + responseCode;
                    return "记录上传失败，请检查网络后重试";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // 处理网络请求的结果
            Toast.makeText(XiuxianActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

}