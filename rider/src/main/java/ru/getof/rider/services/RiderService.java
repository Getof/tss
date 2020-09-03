package ru.getof.rider.services;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import ru.getof.rider.R;
import ru.getof.rider.events.LoginResultEvent;
import ru.getof.taxispb.events.BackgroundServiceStartedEvent;
import ru.getof.taxispb.events.ConnectEvent;
import ru.getof.taxispb.events.ConnectResultEvent;
import ru.getof.taxispb.events.DisconnectedEvent;
import ru.getof.taxispb.events.LoginEvent;
import ru.getof.taxispb.models.Rider;
import ru.getof.taxispb.utils.CommonUtils;
import ru.getof.taxispb.utils.MyPreferenceManager;
import ru.getof.taxispb.utils.ServerResponse;


public class RiderService extends Service {

    Socket socket;
    Vibrator vibe;
    EventBus eventBus = EventBus.getDefault();

    @Subscribe
    public void connectSocket(ConnectEvent connectEvent) {
        try {
            IO.Options options = new IO.Options();
            options.query = "token=" + connectEvent.token;
            socket = IO.socket(getString(R.string.server_address), options);
            socket.on(Socket.EVENT_CONNECT, args -> eventBus.post(new ConnectResultEvent(ServerResponse.OK.getValue())))
                    .on(Socket.EVENT_DISCONNECT, args -> eventBus.post(new DisconnectedEvent()))
                    .on("error", args -> {
                try {
                    JSONObject obj = new JSONObject(args[0].toString());
                    eventBus.post(new ConnectResultEvent(ServerResponse.UNKNOWN_ERROR.getValue(), obj.getString("message")));
                } catch (JSONException c) {
                    eventBus.post(new ConnectResultEvent(ServerResponse.UNKNOWN_ERROR.getValue(), args[0].toString()));
                }
            });
                    /*.on("driverInLocation", args -> {
                try {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(RiderService.this)
                                    .setSmallIcon(R.drawable.fab_requests)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                                    .setContentText(getString(R.string.notification_driver_in_location));
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.notify(0, mBuilder.build());
                    }
                } catch (Exception c) {
                    c.printStackTrace();
                }
            }).on("startTravel", args -> eventBus.post(new ServiceStartedEvent())).on("cancelTravel", args -> eventBus.post(new ServiceCancelResultEvent(200)))
                    .on("driverAccepted", args -> eventBus.post(new NewDriverAcceptedEvent(args[0].toString(), (Integer) args[1], (Integer) args[2], (Double.valueOf(args[3].toString())))))
                    .on("finishedTaxi", args -> eventBus.post(new ServiceFinishedEvent((Integer) args[0], (Boolean) args[1], Float.parseFloat(args[2].toString()))))
                    .on("riderInfoChanged", args -> {
                        MyPreferenceManager SP = new MyPreferenceManager(getApplicationContext());
                        SP.putString("rider_user", args[0].toString());
                        CommonUtils.rider = Rider.fromJson(args[0].toString());
                        eventBus.postSticky(new ProfileInfoChangedEvent());
                    })
                    .on("travelInfoReceived", args -> {
                        eventBus.post(new GetTravelInfoResultEvent((int) args[0], (int) args[1], Float.parseFloat(args[2].toString()), Float.valueOf(args[3].toString()), Float.valueOf(args[4].toString())));
                    });*/
            socket.connect();

        } catch (Exception ignored) {
        }
    }

    @Subscribe
    public void login(LoginEvent event){
        new LoginRequest().execute(String.valueOf(event.userName), String.valueOf(event.versionNumber));
    }

    @SuppressLint("StaticFieldLeak")
    private class LoginRequest extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... uri) {
            try {
                URL url = new URL(getString(R.string.server_address) + "rider_login");
                HttpURLConnection client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("POST");
                client.setDoOutput(true);
                client.setDoInput(true);
                DataOutputStream wr = new DataOutputStream(client.getOutputStream());

                HashMap<String, String> postDataParams = new HashMap<>();
                postDataParams.put("user_name", uri[0]);
                postDataParams.put("version", uri[1]);
                //postDataParams.put("password", uri[1]);
                StringBuilder result = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String, String> entry : postDataParams.entrySet()) {
                    if (first)
                        first = false;
                    else
                        result.append("&");
                    result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    result.append("=");
                    result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
                wr.write(result.toString().getBytes());
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                return sb.toString();
            } catch (Exception c) {
                c.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject obj = new JSONObject(result);
                int status = obj.getInt("status");
                if (status == 200)
                    eventBus.post(new LoginResultEvent(obj.getInt("status"), obj.getString("user"), obj.getString("token")));
                else
                    eventBus.post(new LoginResultEvent(status, obj.getString("error")));
            } catch (Exception ex) {
                Log.e("JSON Parse Failed", "Parse in Login Request Failed");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vibe = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        EventBus.getDefault().register(this);
        eventBus.post(new BackgroundServiceStartedEvent());
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        socket.disconnect();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
