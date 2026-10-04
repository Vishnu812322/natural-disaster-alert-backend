package com.disasteralert.mobile;

import android.Manifest;
import android.app.*;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MainActivity extends ComponentActivity {
    private static final String CHANNEL="emergency";
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        createChannel();
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(30,50,30,30);
        TextView title=new TextView(this); title.setText("🚨 Disaster Alert"); title.setTextSize(28); title.setTextColor(Color.rgb(7,89,133));
        TextView info=new TextView(this); info.setText("\\nMobile client foundation\\n\\nThe app is designed to consume the Spring Boot alert APIs and Firebase Cloud Messaging in production.");
        Button test=new Button(this); test.setText("Preview TEST notification"); test.setOnClickListener(v->showTest());
        root.addView(title);root.addView(info);root.addView(test);setContentView(root);
        if(android.os.Build.VERSION.SDK_INT>=33 && ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);
    }
    private void createChannel(){
        if(android.os.Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel(CHANNEL,"Emergency Alerts",NotificationManager.IMPORTANCE_HIGH);
            c.setDescription("High-priority disaster notifications");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
    private void showTest(){
        Notification n=new NotificationCompat.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("TEST ALERT — Disaster Alert")
            .setContentText("Development test only. Do not treat this as a real emergency.")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build();
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(1001,n);
    }
}
