package ru.getof.driver.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;

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
import ru.getof.driver.R;
import ru.getof.driver.events.LoginResultEvent;
import ru.getof.taxispb.events.BackgroundServiceStartedEvent;
import ru.getof.taxispb.events.ConnectEvent;
import ru.getof.taxispb.events.ConnectResultEvent;
import ru.getof.taxispb.events.DisconnectedEvent;
import ru.getof.taxispb.events.LoginEvent;
import ru.getof.taxispb.events.ProfileInfoChangedEvent;
import ru.getof.taxispb.models.Driver;
import ru.getof.taxispb.utils.CommonUtils;
import ru.getof.taxispb.utils.MyPreferenceManager;
import ru.getof.taxispb.utils.ServerResponse;

public class DriverService extends Service {
    final static String DRIVER_ACCEPTED = "driverAccepted";
    final static String EDIT_PROFILE = "editProfile";
    final static String ACTIVATE_DRIVER = "changeStatus";
    final static String BUZZ = "buzz";
    final static String START_TRAVEL = "startTravel";
    final static String CANCEL_TRAVEL = "cancelTravel";
    final static String GET_DRIVER_TRAVELS = "getTravels";
    final static String CALL_REQUEST = "callRequest";
    Socket socket;
    Vibrator vibe;
    EventBus eventBus = EventBus.getDefault();

    @Subscribe
    public void connectSocket(ConnectEvent event) {
        try {
            IO.Options options = new IO.Options();
            options.query = "token=" + event.token;
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
                    })
//                    .on("requestReceived", args -> {
//                        try {
//                            eventBus.post(new RequestReceivedEvent(args[0].toString(), (Integer) args[1], (Integer) args[2], Double.valueOf(args[3].toString())));
//                            NotificationCompat.Builder mBuilder =
//                                    new NotificationCompat.Builder(DriverService.this)
//                                            .setSmallIcon(R.drawable.fab_requests)
//                                            .setContentTitle(getString(R.string.app_name))
//                                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
//                                            .setContentText(getString(R.string.notification_requests_waiting, CommonUtils.requests.size()));
//                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//                            notificationManager.notify(0, mBuilder.build());
//                        } catch (Exception c) {
//                            c.printStackTrace();
//                        }
//                    })
//                    .on("riderAccepted", args -> {
//                        try {
//                            CommonUtils.currentTravel = Travel.fromJson(args[0].toString());
//                            CommonUtils.rider = Rider.fromJson(args[1].toString());
//                            eventBus.post(new RiderAcceptedEvent());
//                        } catch (Exception c) {
//                            c.printStackTrace();
//                        }
//                    })
                    .on("driverInfoChanged", args -> {
                        MyPreferenceManager SP = new MyPreferenceManager(getApplicationContext());
                        SP.putString("driver_user", args[0].toString());
                        CommonUtils.driver = new Gson().fromJson(args[0].toString(), Driver.class);
                        eventBus.postSticky(new ProfileInfoChangedEvent());
                    });
//                    .on("getTravelInfo", args -> {
//                        if (CommonUtils.currentTravel != null)
//                            socket.emit("travelInfo", CommonUtils.currentTravel.getDistanceReal(), CommonUtils.currentTravel.getDurationReal(), CommonUtils.currentTravel.getCost());
//                    })
//                    .on("cancelTravel", args -> eventBus.post(new ServiceCancelResultEvent(200)));
            socket.connect();
        } catch (Exception c) {
            Log.e("Connect Socket", c.getMessage());
        }

    }

    @Subscribe
    public void login(LoginEvent event) {
        new LoginRequest().execute(String.valueOf(event.userName), String.valueOf(event.versionNumber));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vibe = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        eventBus.post(new BackgroundServiceStartedEvent());
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        socket.disconnect();
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class LoginRequest extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {
            try {
                URL url = new URL(getString(R.string.server_address) + "driver_login");
                HttpURLConnection client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("POST");
                client.setDoOutput(true);
                client.setDoInput(true);
                DataOutputStream wr = new DataOutputStream(client.getOutputStream());

                HashMap<String, String> postDataParams = new HashMap<>();
                postDataParams.put("user_name", uri[0]);
                postDataParams.put("version", uri[1]);
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
}
