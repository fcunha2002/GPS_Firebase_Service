package br.com.fcunha.gpsfirebaseservice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TrackingService extends Service {
    private static final String TAG = TrackingService.class.getSimpleName();

    public TrackingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return  null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
        loginToFirebase();
    }

    //Cria a notificação persistente//
    private void buildNotification() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);

        // Cria a notificação persistente//
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
        {
            String NOTIFICATION_CHANNEL_ID = "br.com.fcunha.gpsfirebaseservice";
            String channelName = "GPS Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.tracking_enabled)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.tracking_enabled_notif))
                    .setOngoing(true)
                    .setContentIntent(broadcastIntent)
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        } else {
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.tracking_enabled_notif))

                    //Make this notification ongoing so it can’t be dismissed by the user//

                    .setOngoing(true)
                    .setContentIntent(broadcastIntent)
                    .setSmallIcon(R.drawable.tracking_enabled);
            startForeground(1, builder.build());
        }
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        //Desregistra o BroadcastReceiver quando a notificação é fechada//
            unregisterReceiver(stopReceiver);
            //Para o serviço//
            stopSelf();
        }
    };

    private void loginToFirebase() {
        //Autentica no Firebase, usando email e senha//
        String email = getString(R.string.test_email);
        String password = getString(R.string.test_password);

        //Chama OnCompleteListener quando o usuário é autenticado corretamente//
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                //Se o usuário foi autenticado...//
                if (task.isSuccessful()) {
                    //...então chama requestLocationUpdates//
                    requestLocationUpdates();
                } else {
                    //Se autenticação falhou, loga o erro//
                    Log.d(TAG, "Firebase authentication failed");
                }
            }
        });
    }

    //Inicia e requisição de localização//
    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();

        //Parâmetros recomendados para localização em tempo real.
        //Porém pode ser alto o consumo de bateria
        //Configura o intervalo em 10s//
        request.setInterval(10000);
        //Pega a localização mais fina//
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        final String path = getString(R.string.firebase_path);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        //If the app currently has access to the location permission...//

        if (permission == PackageManager.PERMISSION_GRANTED) {
            //...then request location updates//

            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {

                    //Get a reference to the database, so your app can perform read and write operations//

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        //Salva a localização no Banco de Dados//
                        ref.child(path).setValue(location);
                    }
                }
            }, null);
        }
    }
}
