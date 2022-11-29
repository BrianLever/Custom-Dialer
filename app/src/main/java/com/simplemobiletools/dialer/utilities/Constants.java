package com.simplemobiletools.dialer.utilities;

import java.util.HashMap;

public class Constants {

    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_FIRST_NAME = "first_name";
    public static final String KEY_LAST_NAME = "last_name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_MOBILE = "mobile_number";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_PREFERENCE_NAME = "videoMeetingPreference";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String KEY_PROFILE_PICTURE_SELECTED = "selected";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER = "user";
    public static final String KEY_FCM_TOKEN = "fcm_token";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";
    public static final String REMOTE_MSG_TYPE = "type";
    public static final String REMOTE_MSG_INVITE = "invite";
    public static final String REMOTE_MSG_RCD = "rcd";
    public static final String REMOTE_MSG_MEETING_TYPE = "meetingType";
    public static final String REMOTE_MSG_INITIATOR_TOKEN = "initiatorToken";
    public static final String REMOTE_MSG_INITIATOR_NUMBER = "initiatorNumber";
    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";
    public static final String REMOTE_MSG_MSG_DATA = "data";
    public static final String REMOTE_MSG_INVITE_RESPONSE = "inviteResponse";
    public static final String REMOTE_MSG_PROVISIONAL_RESPONSE = "provisionalResponse";
    public static final String REMOTE_MSG_INVITE_ACCEPTED = "accepted";
    public static final String REMOTE_MSG_INVITE_DELCINE = "declined";
    public static final String REMOTE_MSG_INVITE_CANCELLED = "cancelled";
    public static final String REMOTE_MSG_INVITE_ACK = "ack";
    public static final String REMOTE_MSG_RECONNECT = "reconnect";
    public static final String REMOTE_MSG_RECONNECT_RESPONSE = "reconnectresponse";
    public static final String REMOTE_MSG_RECONNECT_ACCEPT = "reconnectaccept";
    public static final String REMOTE_MSG_RECONNECT_DECLINED = "reconnectdeclined";
    public static final String REMOTE_MSG_BUSY = "busy";
    public static final String REMOTE_MSG_MEETING_IDENTIFIER = "meetingIdentifier";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_IS_OUTGOING = "outgoing";
    public static final String REMOTE_MSG_END_CALL = "endCall";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_LAST_SEEN_CELLULAR ="lastSeenCellular";
    public static final String KEY_IS_DEFAULT_DIALER ="defaultDialer";
    public static final String KEY_DB_INIT = "dbInit";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String KEY_LAST_SEEN = "lastSeen";
    public static final String KEY_PEER_RECEIVED = "peerReceived";
    public static final String KEY_PEER_READ = "peerRead";
    public static final String RCD_NAME = "profileName";
    public static final String RCD_EMAIL = "profileEmail";
    public static final String RCD_ADDRESS = "profileAddress";
    public static final String RCD_PHONE_ONE = "profilePhoneOne";
    public static final String RCD_PHONE_TWO = "profilePhoneTwo";
    public static final String KEY_REQUEST_TIME = "requestTime";
    public static final String RCD_ALTERNATE_PHONE = "alternatePhone";
    public static final String RCD_FACEBOOK_URL="facebookURL";
    public static final String RCD_FACEBOOK_URL_FLAG="facebookURLFlag";
    public static final String RCD_INSTA_URL = "instaURL";
    public static final String RCD_INSTA_URL_FLAG = "instaURLFlag";
    public static final String RCD_TWITTER_URL = "twitterURL";
    public static final String RCD_TWITTER_URL_FLAG = "twitterURLFlag";
    public static final String RCD_LINKEDIN_URL = "linkedinURL";
    public static final String RCD_LINKEDIN_URL_FLAG = "linkedinURLFlag";
    public static final String RCD_WEB_URL = "webURL";
    public static final String RCD_WEB_URL_FLAG = "webURLFlag";
    public static final String RCD_CALL_TYPE="rcdCallType";
    public static final String RCD_CALL_MESSAGE="rcdCallMessage";
    public static final String RCD_CALL_BITMAP_DATA="rcdCallBitmapData";
    public static final String RCD_CALL_EMOTICON="rcdCallEmoticon";
    public static final String RCD_CALL_REASON = "callReason";
    public static final String RCD_IMAGE = "rcdImage";
    public static final String KEY_COLLECTION_RCD = "rcd";
    public static final String TAG = "inYte";
    public static final String KEY_VIDEO_ACTIVE = "isVideoActive";
    



    public static HashMap<String, String> getRemoteMessageHeaders(){
        HashMap<String, String> headers = new HashMap<>();
        headers.put(
            Constants.REMOTE_MSG_AUTHORIZATION,
            "key=AAAAGR2QSnQ:APA91bEl4KgcTVQ61zwHnqcGDw6r2stUjE3Ir4pmqwDNmgkDpKICr7G4sMD0_RBb_em4fwsfdljVdsQ6XPWAYyI_P12qbfll914sUGMkU6pNFLSk6fXcI1wstOjMRH7qnQK7FyxZH6FQ"
        );
        headers.put(
            Constants.REMOTE_MSG_CONTENT_TYPE, "application/json");
        return headers;
    }

}
