package com.sampleloyaltyapp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import co.poynt.api.model.Order;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.services.v1.IPoyntLoyaltyService;
import co.poynt.os.services.v1.IPoyntLoyaltyServiceListener;

public class SampleLoyaltyService extends Service {
    private static final String TAG = SampleLoyaltyService.class.getSimpleName();

    public SampleLoyaltyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IPoyntLoyaltyService.Stub mBinder = new IPoyntLoyaltyService.Stub() {

        @Override
        public void process(Payment payment,
                            String requestId,
                            IPoyntLoyaltyServiceListener iPoyntLoyaltyServiceListener)
                throws RemoteException {
            Log.d(TAG, "process(): " + payment);

            Intent intent = new Intent(Intents.ACTION_PROCESS_LOYALTY);
            intent.setComponent(new ComponentName(getPackageName(), MainActivity.class.getName()));
            intent.putExtra("payment", payment);
            iPoyntLoyaltyServiceListener.onLaunchActivity(intent, requestId);
        }
    };
}
