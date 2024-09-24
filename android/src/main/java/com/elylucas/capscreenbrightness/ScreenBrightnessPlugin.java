package com.elylucas.capscreenbrightness;

import android.Manifest;
import android.app.Activity;
import android.util.Log;
import android.view.WindowManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CapacitorPlugin(name = "ScreenBrightness",
permissions = { @Permission(strings = {Manifest.permission.READ_SMS}, alias = "sms")}
)
public class ScreenBrightnessPlugin extends Plugin {

    private SMSes implementation;
    private static final String P_Tag   = "permissions";
    @PluginMethod
    public void setBrightness(PluginCall call) {
        Float brightness = call.getFloat("brightness");
        Activity activity = getActivity();
        WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();

        activity.runOnUiThread(
            () -> {
                layoutParams.screenBrightness = brightness;
                activity.getWindow().setAttributes(layoutParams);
                call.resolve();
            }
        );
    }
    @Override
    public void load() {
        implementation = new SMSes(getActivity());
    }
    private void requestSMSPermission(PluginCall call){
        requestPermissionForAlias("sms",call,"permissionCallback");
    }
    private boolean SMSPermissionNotGranted(){
        return getPermissionState("sms") != PermissionState.GRANTED;
    }
    @PluginMethod
    public void getBrightness(PluginCall call) {
        WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
        JSObject ret = new JSObject();
        call.resolve(
            new JSObject() {
                {
                    put("brightness", layoutParams.screenBrightness);
                }
            }
        );
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call){
        if(SMSPermissionNotGranted()){
            call.reject("Permission is required to access SMS");
        }

        switch(call.getMethodName()){
            case "getInboxSMS":
                getInboxSMS(call);
                break;
        }
    }

    @PluginMethod
    public void getInboxSMS(PluginCall call){
        if(SMSPermissionNotGranted()){
            requestSMSPermission(call);
        }
        Log.d(P_Tag,"Permission granted!!!");
        ExecutorService executor = Executors.new();

        executor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        bridge.getActivity().runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        call.resolve(result);
                                    }
                                }
                        );
                    }
                }
        );
        executor.shutdown();
    }
}
