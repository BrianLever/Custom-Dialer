package com.simplemobiletools.dialer.missedCalls;

import android.net.Uri;

public class contactInfo {

    public static final contactInfo EMPTY = new contactInfo();
    public Uri lookupUri;
    public String lookupKey;
    public String name;
    public String label;
    public String number;
    public String formattedNumber;

    /** The photo for the contact, if available. */
    public long photoId;
    /** The high-res photo for the contact, if available. */
    public Uri photoUri;

}

