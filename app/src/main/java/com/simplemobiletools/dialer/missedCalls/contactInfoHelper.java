package com.simplemobiletools.dialer.missedCalls;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class contactInfoHelper {

    public Context context;

    public contactInfoHelper(Context context) {
        this.context = context;
    }

    public contactInfo getContactInfo(String number) {
       number = (number == null)? "": number;
       contactInfo contactInfo;
       contactInfo = lookupNumber(number);
       if(contactInfo!=null && contactInfo.name!=null) {
           if(!contactInfo.name.isEmpty()) {
               return contactInfo;
           }
       }
       if(!contactInfo.number.isEmpty()) {
           contactInfo.name = number;
       }else {
           contactInfo.name = "Unknown Caller";
       }
       return contactInfo;
    }


    private contactInfo lookupNumber(String number) {
        if(number.isEmpty()){
            return  null;
        }
        contactInfo contactInfo;
        contactInfo = queryContactInfoForNumber(number);

        final contactInfo updatedInfo;
        if (contactInfo == null) {
            // The lookup failed.
            Log.d("inYte", "lookup failed");
            updatedInfo = null;
        } else {
            // If we did not find a matching contact, generate an empty contact info for the number.
            if (contactInfo == contactInfo.EMPTY) {
                // Did not find a matching contact.
                updatedInfo = createEmptyContactInfoForNumber(number);
            } else {
                updatedInfo = contactInfo;
            }
        }
        return updatedInfo;
    }


    private contactInfo createEmptyContactInfoForNumber(String number) {
        contactInfo contactInfo = new contactInfo();
        contactInfo.number = number;
        return contactInfo;
    }



    private contactInfo queryContactInfoForNumber(String number) {
        contactInfo contactInfo = lookupContactFromUri(getContactInfoLookupUri(number));
        if(contactInfo == null) {
            Log.d("inYTE", this.getClass().toString()+"contactInfoisnull");
        }
        return  contactInfo;
    }

    private Uri getContactInfoLookupUri(String number) {
        Uri uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI;
        Uri.Builder builder =
            uri.buildUpon()
            .appendPath(number);
        return builder.build();
    }

    private contactInfo lookupContactFromUri(Uri uri) {
        if(uri == null) {
            Log.d("inYte", "uri in lookupcontactisnull");
            return null;
        }
        try(Cursor phoneLookupCursor =
            context
            .getContentResolver()
            .query(
                uri,
                PhoneQuery.getPhoneLookupProjection(),
                null,
                null,
                null)) {
            if(phoneLookupCursor == null) {
                return null;
            }
            if(!phoneLookupCursor.moveToFirst()) {
                return contactInfo.EMPTY;
            }
            boolean hasNumberMatch =
                updateCursorToMatchContactLookupUri(phoneLookupCursor, PhoneQuery.MATCHED_NUMBER, uri);
            if (!hasNumberMatch) {
                return contactInfo.EMPTY;
            }

            String lookupKey = phoneLookupCursor.getString(PhoneQuery.LOOKUP_KEY);
            contactInfo contactInfo = createPhoneLookupContactInfo(phoneLookupCursor, lookupKey);
            return contactInfo;
        }
    }

    private contactInfo createPhoneLookupContactInfo(Cursor phoneLookupCursor, String lookupKey) {
        contactInfo info = new contactInfo();
        info.lookupKey = lookupKey;
        info.lookupUri =
            ContactsContract.Contacts.getLookupUri(phoneLookupCursor.getLong(PhoneQuery.PERSON_ID), lookupKey);
        info.name = phoneLookupCursor.getString(PhoneQuery.NAME);
        info.label = phoneLookupCursor.getString(PhoneQuery.LABEL);
        info.number = phoneLookupCursor.getString(PhoneQuery.MATCHED_NUMBER);

        info.photoId = phoneLookupCursor.getLong(PhoneQuery.PHOTO_ID);
        info.photoUri = parseUriOrNull(phoneLookupCursor.getString(PhoneQuery.PHOTO_URI));
        info.formattedNumber = null;

        return info;
    }


    public static Uri parseUriOrNull(String uriString) {
        if (uriString == null) {
            return null;
        }
        return Uri.parse(uriString);
    }


    public static boolean updateCursorToMatchContactLookupUri(
       Cursor cursor, int columnIndexForNumber,  Uri contactLookupUri) {
        if (cursor == null || contactLookupUri == null) {
            return false;
        }

        if (!cursor.moveToFirst()) {
            return false;
        }

        Assert.checkArgument(
            0 <= columnIndexForNumber && columnIndexForNumber < cursor.getColumnCount());

        String lookupNumber = contactLookupUri.getLastPathSegment();
        if (lookupNumber.isEmpty()) {
            return false;
        }

        boolean lookupNumberHasSpecialChars = numberHasSpecialChars(lookupNumber);

        do {
            String existingContactNumber = cursor.getString(columnIndexForNumber);
            boolean existingContactNumberHasSpecialChars = numberHasSpecialChars(existingContactNumber);

            if ((!lookupNumberHasSpecialChars && !existingContactNumberHasSpecialChars)
                || sameRawNumbers(existingContactNumber, lookupNumber)) {
                return true;
            }

        } while (cursor.moveToNext());

        return false;
    }

    /** Returns true if the input phone number contains special characters. */
    public static boolean numberHasSpecialChars(String number) {
        return !number.isEmpty() && number.contains("#");
    }

    /** Returns true if the raw numbers of the two input phone numbers are the same. */
    public static boolean sameRawNumbers(String number1, String number2) {
        String rawNumber1 =
            PhoneNumberUtils.stripSeparators(PhoneNumberUtils.convertKeypadLettersToDigits(number1));
        String rawNumber2 =
            PhoneNumberUtils.stripSeparators(PhoneNumberUtils.convertKeypadLettersToDigits(number2));

        return rawNumber1.equals(rawNumber2);
    }
}
