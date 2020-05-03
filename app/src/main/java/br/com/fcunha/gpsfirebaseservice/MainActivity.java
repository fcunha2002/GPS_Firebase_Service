package br.com.fcunha.gpsfirebaseservice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Verifica se o GPS está habilitado
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            finish();
        }

        //Verifica se o app tem acesso à localização do usuário
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        //Se a permissão de acessar a localização foi dada, então inicia o Serviço de Localização//
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startTrackerService();
        } else {
        //Se o app atualmente não tem acesso à localização, então requisita o acesso
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //Se a permissão foi concedida...//
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        //...inicia o serviço de rastreamento//
            startTrackerService();
        } else {
        //Se o usuário negou a permissão de acesso, então mostra uma mensagem com mais informações//
            Toast.makeText(this, "Favor habilitar o serviço de localização por GPS", Toast.LENGTH_SHORT).show();
        }
    }

    //Inicia o serviço de rastreamento//
    private void startTrackerService() {
        startService(new Intent(getApplicationContext(), TrackingService.class));
        //Notifica o usuário que ele está sendo rastreado//
        Toast.makeText(this, "Rastreamento por GPS iniciado", Toast.LENGTH_SHORT).show();
        //Fecha a MainActivity//
        finish();
    }

}

