package com.simplemobiletools.dialer.utilities;


import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import java.util.Locale;

public class countryDetector {

    public static countryDetector instance_;
    public final TelephonyManager telephonyManager;
    public final Context context;
    public Locale locale;
    public static final String DEFAULT_COUNTRY_ISO = "IN";

    public countryDetector(
        Context passedContext,
        TelephonyManager telephonyManagerInstance,
        Locale currentLocale
    ){
        this.context = passedContext;
        this.telephonyManager = telephonyManagerInstance;
        this.locale = currentLocale;
    }



    @RequiresApi(api = Build.VERSION_CODES.N)
    public static synchronized countryDetector getInstance(Context context){
       if(instance_==null){
           instance_ = new countryDetector(
               context,
               (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE),
               getCurrentLocale(context)
           );
       }
       return instance_;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static Locale getCurrentLocale(Context context){
            LocaleList localeList = null;
            localeList = context.getResources().getConfiguration().getLocales();
            if(!localeList.isEmpty()){
                return localeList.get(0);
            }
        return Locale.getDefault();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public  String getCountryIso() {
        String result = null;
        if (isNetworkCountryCodeAvailable()) {
            result = getNetworkBasedCountryIso();
        }
        if(result.isEmpty()){
            result = getSimBasedCountryIso();
        }
        if(result.isEmpty()){
            result = getLocaleBasedCountryIso();
        }
        if(result.isEmpty()){
            result = DEFAULT_COUNTRY_ISO;
        }
        return result.toUpperCase(Locale.US);
    }

    private boolean isNetworkCountryCodeAvailable() {
        // On CDMA TelephonyManager.getNetworkCountryIso() just returns the SIM's country code.
        // In this case, we want to ignore the value returned and fallback to location instead.
        return telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM;
    }

    private String getNetworkBasedCountryIso() {
        return telephonyManager.getNetworkCountryIso();
    }

    private String getSimBasedCountryIso() {
        return telephonyManager.getSimCountryIso();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String getLocaleBasedCountryIso() {
        Locale defaultLocale = getCurrentLocale(context);
        if (defaultLocale != null) {
            return defaultLocale.getCountry();
        }
        return null;
    }

}
