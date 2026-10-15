package com.example.powerxchange2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class AutoTradeWorker extends Worker{

    private static final String CHANNEL_ID = "powerxchange_autotrade_channel";

    public AutoTradeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork(){
        Context ctx = getApplicationContext();
        SQLiteRepository repo = new SQLiteRepository(ctx);
        try {
            repo.runAutoTradeForAllUsers();
            repo.expireStalePools();
        }
        catch(Exception e){
            return Result.retry();
        }
        sendNotification(ctx,"PowerXchange Auto-Trade","Auto-trade engine checked new listings for your price rules.");
        return Result.success();
    }
    private void sendNotification(Context context, String title, String message) {
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mgr == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Auto-Trade Alerts", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for automated clean energy trades");
            mgr.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_rules)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        try { mgr.notify(1001, b.build()); } catch (Exception ignored) {}
    }
}