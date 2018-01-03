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

public class RemoteService extends Service {

    private String TAG = this.getClass().getSimpleName();
    private RemoteServiceConnection remoteServiceConnection = null;
    private RemoteServiceBinder remoteServiceBinder = null;
    private static int notifyID = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        if (null == remoteServiceConnection) {
            remoteServiceConnection = new RemoteServiceConnection();
        }

        if (null == remoteServiceBinder) {
            remoteServiceBinder = new RemoteServiceBinder();
        }
        bindLocalService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand, flags = " + flags + ", startId = " + startId);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            startForeground(notifyID, /*new Notification()*/AppUtils.getNotification("远程服务服务", "我正在运行", this));//特权提升
            startService(new Intent(this, PrivilegeUpService.class));
        }

        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return remoteServiceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(remoteServiceConnection);
    }

    private void bindLocalService() {
        bindService(new Intent(RemoteService.this, LocalService.class), remoteServiceConnection, Service.BIND_IMPORTANT);
    }

    private void startLocalService() {
        startService(new Intent(RemoteService.this, LocalService.class));
    }
    private class RemoteServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(RemoteService.this, "已链接本地服务", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceConnected, name = " + name.getClassName() + ", service = " + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(RemoteService.this, "本地服务被杀死", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceDisconnected, name = " + name.getClassName());
            startLocalService();
            bindLocalService();
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG, "onBindingDied, name = " + name.getClassName());
        }
    }

    private class RemoteServiceBinder extends IKeepService.Stub {
        @Override
        public String getServiceName() throws RemoteException {
            return "远程服务";
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
