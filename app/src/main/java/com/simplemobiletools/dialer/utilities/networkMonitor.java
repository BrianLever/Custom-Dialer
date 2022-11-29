package com.simplemobiletools.dialer.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;

public class networkMonitor  {
    public static enum connectionType {
        CONNECTION_UNKNOWN,
        CONNECTION_ETHERNET,
        CONNECTION_WIFI,
        CONNECTION_CELLULAR_USABLE,
        CONNECTION_CELLULAR_UNUSABLE,
        CONNECTION_3G,
        CONNECTION_BLUETOOTH,
        CONNECTION_VPN
    }

    private connectionType type;
    private Boolean isConnected;
    private networkCallBack networkCallBack = null;
    private ConnectivityManager connectivityManager = null;
    private NetworkInfo networkInfo = null;
    private NetworkCapabilities networkCapabilities = null;
    private int networkType;
    private int networkSubType;
    private Observer observer;

    public networkMonitor(Observer observer, Context context){
        if(context!=null){
            if(networkCallBack==null){
                networkCallBack = new networkCallBack();
            }
            if(connectivityManager==null){
                connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            }
            registerNetworkCallback(networkCallBack);
        }
        this.observer = observer;
    }

    private void registerNetworkCallback(ConnectivityManager.NetworkCallback networkCallBack){
        connectivityManager.registerNetworkCallback(
            new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build(),networkCallBack);
    }


    private class networkCallBack extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.d("TETHERAPPLICATION", "onAvailable");
            if(network!=null) {
               networkInfo = connectivityManager.getNetworkInfo(network);
               checkNetworkViability(networkInfo,networkInfo.getType(),networkInfo.getSubtype());
            }
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.d("TETHERAPPLICATION", "onCapsChanged");
          /*  if(network!=null) {
                networkInfo = connectivityManager.getNetworkInfo(network);
                checkNetworkViability(networkInfo, networkInfo.getType(), networkInfo.getSubtype());
            } */
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            Log.d("TETHERAPPLICATION", "onLinkPropsChanged");
       /*     if(network!=null) {
                networkInfo = connectivityManager.getNetworkInfo(network);
                checkNetworkViability(networkInfo, networkInfo.getType(), networkInfo.getSubtype());
            } */
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            Log.d("TETHERAPPLICATION", "onLost");
            if(network!=null) {
                isConnected = false;
                observer.onDisconnected();
            }
        }

        private void checkNetworkViability(NetworkInfo networkInfo, int networkType, int subtype){
            Log.d("TETHERAPPLICATION", "innetworkviability");
            switch (networkType) {
                case ConnectivityManager.TYPE_ETHERNET:
                    type = connectionType.CONNECTION_ETHERNET;
                    isConnected=true;
                    observer.onConnected(type);
                    return;
                case ConnectivityManager.TYPE_WIFI:
                    type = connectionType.CONNECTION_WIFI;
                    isConnected = true;
                    observer.onConnected(type);
                    return;
                case ConnectivityManager.TYPE_WIMAX:
                    type = connectionType.CONNECTION_CELLULAR_USABLE;
                    isConnected = true;
                    observer.onConnected(type);
                    return;
                case ConnectivityManager.TYPE_BLUETOOTH:
                    type = connectionType.CONNECTION_BLUETOOTH;
                    isConnected = true;
                    observer.onConnected(type);
                    return;
                case ConnectivityManager.TYPE_MOBILE:
                     switch (subtype) {
                         case TelephonyManager.NETWORK_TYPE_GPRS:
                         case TelephonyManager.NETWORK_TYPE_EDGE:
                         case TelephonyManager.NETWORK_TYPE_CDMA:
                         case TelephonyManager.NETWORK_TYPE_1xRTT:
                         case TelephonyManager.NETWORK_TYPE_IDEN:
                             type = connectionType.CONNECTION_CELLULAR_UNUSABLE;
                             isConnected=true;
                             observer.onConnected(type);
                             return;
                         case TelephonyManager.NETWORK_TYPE_UMTS:
                         case TelephonyManager.NETWORK_TYPE_EVDO_0:
                         case TelephonyManager.NETWORK_TYPE_EVDO_A:
                         case TelephonyManager.NETWORK_TYPE_HSDPA:
                         case TelephonyManager.NETWORK_TYPE_HSUPA:
                         case TelephonyManager.NETWORK_TYPE_HSPA:
                         case TelephonyManager.NETWORK_TYPE_EVDO_B:
                         case TelephonyManager.NETWORK_TYPE_EHRPD:
                         case TelephonyManager.NETWORK_TYPE_HSPAP:
                             type = connectionType.CONNECTION_3G;
                             isConnected=true;
                             observer.onConnected(type);
                             return;
                         case TelephonyManager.NETWORK_TYPE_LTE:
                             type = connectionType.CONNECTION_CELLULAR_USABLE;
                             isConnected=true;
                             observer.onConnected(type);
                             return;
                         default:
                             type = connectionType.CONNECTION_UNKNOWN;
                             isConnected=true;
                             observer.onConnected(type);
                             return;
                     }
                case ConnectivityManager.TYPE_VPN:
                    type = connectionType.CONNECTION_VPN;
                    isConnected = true;
                    observer.onConnected(type);
                    return;
                default:
                    type = connectionType.CONNECTION_UNKNOWN;
                    isConnected=true;
                    observer.onConnected(type);
                    return;
            }
        }

    }

    public interface Observer {
        public void onConnected(connectionType connectionType);
        public void onDisconnected();
    }

    public void destructor(){
        connectivityManager.unregisterNetworkCallback(networkCallBack);
        networkCallBack = null;
        connectivityManager = null;
    }

}
