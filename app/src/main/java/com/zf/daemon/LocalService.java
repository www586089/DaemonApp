package com.zf.daemon;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by vince on 2018/1/3.
 */

public class LocalService extends Service {

    private String TAG = this.getClass().getSimpleName();
    private LocalServiceConnection localServiceConnection = null;
    private LocalServiceBinder localServiceBinder = null;

    private static int notifyID = 1;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        if (null == localServiceConnection) {
            localServiceConnection = new LocalServiceConnection();
        }

        if (null == localServiceBinder) {
            localServiceBinder = new LocalServiceBinder();
        }

        bindRemoteService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand, flags = " + flags + ", startId = " + startId);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            startForeground(notifyID, /*new Notification()*/AppUtils.getNotification("本地服务", "我正在运行", this));//特权提升
            startService(new Intent(this, PrivilegeUpService.class));
        }

        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return localServiceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(localServiceConnection);
    }
    private void bindRemoteService() {
        bindService(new Intent(LocalService.this, RemoteService.class), localServiceConnection, Service.BIND_IMPORTANT);
    }

    private void startRemoteService() {
        startService(new Intent(LocalService.this, RemoteService.class));
    }

    private class LocalServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(LocalService.this, "已链接远程服务", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceConnected, name = " + name.getClassName() + ", service = " + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(LocalService.this, "远程服务被杀死", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceDisconnected, name = " + name.getClassName());
            startRemoteService();
            bindRemoteService();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG, "onBindingDied, name = " + name.getClassName());
        }
    }

    private class LocalServiceBinder extends IKeepService.Stub {
        @Override
        public String getServiceName() throws RemoteException {
            return "本地服务";
        }
    }

    /**
     * 此服务为了取消因特权提升而产生的通知图标而存在
     */
    public static class PrivilegeUpService extends Service {
        private String TAG = this.getClass().getSimpleName();
        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "onStartCommand");
            //startForeground(notifyID, new Notification());//取消通知
            stopSelf();
            return START_REDELIVER_INTENT;
        }
    }
}
