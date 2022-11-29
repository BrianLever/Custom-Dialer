package com.simplemobiletools.dialer.utilities;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.prefixmapper.PrefixFileReader;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import java.util.Locale;


public class phoneNumberHelper {

    public phoneNumberHelper(){

    }

    public static int getCountryPrefix(String number){
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try{
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(number, "IN");
            int countryCode = phoneNumber.getCountryCode();
            return countryCode;
        }catch (NumberParseException e){
            Log.d("inYte", "numberParseException");
            Log.d("inYte", e.toString());

        }
        return 0;
    }


    public static String getCountryName (String number) {
        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

        try{
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(number, "IN");
            String countryCode  = geocoder.getDescriptionForNumber(phoneNumber, Locale.forLanguageTag("US"));
            if(countryCode!=null) return countryCode;
        }catch (NumberParseException e){
            Log.d("inYte", "getCountryNameException");
            Log.d("inYte", e.toString());
        }
        return null;
    }

    public static String getCorrectFormat(String number) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try{
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(number, "IN");
            String formattedNumber = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            if(formattedNumber!=null) return formattedNumber;
        }catch (NumberParseException e){
            Log.d("inYte", "numberFormattingexception");
            Log.d("inYte", e.toString());
        }
        return null;
    }
}
