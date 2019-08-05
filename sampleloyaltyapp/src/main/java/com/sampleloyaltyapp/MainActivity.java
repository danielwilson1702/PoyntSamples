package com.sampleloyaltyapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.poynt.api.model.Discount;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderAmounts;
import co.poynt.api.model.OrderStatus;
import co.poynt.api.model.OrderStatuses;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    Payment payment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button addDiscount = (Button) findViewById(R.id.addDiscount);
        addDiscount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Order paymentOrder = payment.getOrder();
                if(paymentOrder == null){
                    //Handle Terminal only payments, which do not possess an Order
                    paymentOrder = new Order();
                    paymentOrder.setId(UUID.randomUUID());
                    payment.setOrder(paymentOrder);
                }

                long discountAmount = 100l;
                List<Discount> discounts = payment.getOrder().getDiscounts();
                if (discounts == null) {
                    discounts = new ArrayList<>();
                }
                Discount discount = new Discount();
                discount.setAmount(-1l * discountAmount);
                discount.setCustomName("Loyalty Discount");
                discount.setProcessor(getPackageName());
                discounts.add(discount);
                payment.getOrder().setDiscounts(discounts);

                //long discountTotal = payment.getOrder().getAmounts().getDiscountTotal() + discount.getAmount();
                //payment.getOrder().getAmounts().setDiscountTotal(discountTotal);

                OrderAmounts amounts = new OrderAmounts();
                amounts.setDiscountTotal(-1l * discountAmount);
                amounts.setSubTotal(payment.getAmount());
                payment.getOrder().setAmounts(amounts);

                long orderTotal = payment.getOrder().getAmounts().getSubTotal();
                if (orderTotal >= discountAmount) {
                    payment.getOrder().getAmounts().setNetTotal(orderTotal - discountAmount);
                    payment.setAmount(orderTotal - discountAmount);

                    if(payment.getOrder().getStatuses() == null){
                        payment.getOrder().setStatuses(new OrderStatuses());
                    }
                    payment.getOrder().getStatuses().setStatus(OrderStatus.COMPLETED);

                    try {
                        poyntOrderService.createOrder(payment.getOrder(), UUID.randomUUID().toString(), new IPoyntOrderServiceListener() {
                            @Override
                            public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {

                                Log.d(TAG, "Discount added to order: " + payment);
                                Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
                                result.putExtra("payment", payment);
                                setResult(Activity.RESULT_OK, result);
                                finish();
                            }

                            @Override
                            public IBinder asBinder() {
                                Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
                                result.putExtra("payment", payment);
                                setResult(Activity.RESULT_OK, result);
                                finish();
                                return null;
                            }
                        });
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(MainActivity.this, "Discount amount is larger than total", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            }
        });

        // Get the intent that started this activity
        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        } else {
            Log.e(TAG, "Loyalty activity launched with no intent!");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        // If the customer is not checked in the activity can call IPoyntSecondScreenService
        // to display collect phone number, email or scan QR code screen to allow customer
        // to check in.
    }

    private IPoyntOrderService poyntOrderService = null;
    private ServiceConnection orderConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            poyntOrderService = IPoyntOrderService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            poyntOrderService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        bindService(Intents.getComponentIntent(Intents.COMPONENT_POYNT_ORDER_SERVICE), orderConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(orderConnection);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if (Intents.ACTION_PROCESS_LOYALTY.equals(action)) {
            payment = intent.getParcelableExtra("payment");
            if (payment == null) {
                Log.e(TAG, "launched with no payment object");
                Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
                setResult(Activity.RESULT_CANCELED, result);
                finish();
            } else {
                // add a discount to the order
                if (payment.getOrder() == null) {
                    /*Log.e(TAG, "launched with no order object");
                    Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
                    setResult(Activity.RESULT_CANCELED, result);
                    finish();*/
                } else {
                    // wait till the Add discount is clicked
                }
            }
        } else {
            Log.e(TAG, "launched with unknown intent action");
            Intent result = new Intent(Intents.ACTION_PROCESS_LOYALTY_RESULT);
            setResult(Activity.RESULT_CANCELED, result);
            finish();
        }
    }
}
