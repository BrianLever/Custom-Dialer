package com.simplemobiletools.dialer.helpers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import com.simplemobiletools.dialer.activities.CallActivity
import com.simplemobiletools.dialer.utilities.Constants

class bluetoothHelper: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(Constants.TAG, "received bluetooth intent")
        if (intent != null) {
            var action = intent.action
            when (action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    var bluetoothHeadsetState =
                        intent.getIntExtra(
                            BluetoothHeadset.EXTRA_STATE,
                            -1
                        )
                    var device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    var deviceName =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_NAME)
                    if (device == null) {
                        Log.d("inYte", "inreturnAUDIOSTATECHANGED1")
                        return
                    }
                    when (bluetoothHeadsetState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            if (CallManager.foregroundCall != null) {
                                Log.d("inYte", "inreturnAUDIOSTATECHANGED3")
                                if (deviceName != null) CallActivity.thisActivity!!.onStateChanged(
                                    true,
                                    deviceName.toString()
                                )
                                else CallActivity.thisActivity!!.onStateChanged(true, "inYteDummy")
                            } else return
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            if (CallManager.foregroundCall != null) {
                                if (deviceName != null) CallActivity.thisActivity!!.onStateChanged(
                                    false,
                                    deviceName.toString()
                                )
                                else CallActivity.thisActivity!!.onStateChanged(false, "inYteDummy")
                            } else return
                        }
                    }
                }

                BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                    var bluetoothHeadsetAudioState =
                        intent.getIntExtra(
                            BluetoothHeadset.EXTRA_STATE,
                            -1
                        )
                    var device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    var deviceName =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_NAME)
                    if (device == null) {
                        Log.d("inYte", "inreturnAUDIOSTATECHANGED2")
                        return
                    }
                    when (bluetoothHeadsetAudioState) {
                        BluetoothHeadset.STATE_AUDIO_CONNECTED -> {
                            if (CallManager.foregroundCall != null) {
                                Log.d("inYte", "inreturnAUDIOSTATECHANGED4")
                                if (deviceName != null) CallActivity.thisActivity!!.onStateChanged(
                                    true,
                                    deviceName.toString()
                                )
                                else CallActivity.thisActivity!!.onStateChanged(true, "inYteDummy")
                            } else return
                        }
                        BluetoothHeadset.STATE_AUDIO_DISCONNECTED -> {
                            if (CallManager.foregroundCall != null) {
                                if (deviceName != null) CallActivity.thisActivity!!.onStateChanged(
                                    false,
                                    deviceName.toString()
                                )
                                else CallActivity.thisActivity!!.onStateChanged(false, "inYteDummy")
                            } else return
                        }
                    }

                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    var state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    Log.d("inYte", "ACTIONSTATECHANGED")
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            Log.d("inYte", "STATEON")
                        //    if (CallManager.foregroundCall != null) {
                        //        Log.d("inYte", "inreturnAUDIOSTATECHANGED5")
                         //       CallActivity.thisActivity!!.onStateChanged(true, "inYteDummy")
                         //   } else return
                        }

                        BluetoothAdapter.STATE_OFF -> {
                            Log.d("inYte", "STATEOFF")
                            if (CallManager.foregroundCall != null) {
                                Log.d("inYte", "inreturnAUDIOSTATECHANGED6")
                                CallActivity.thisActivity!!.onStateChanged(false, "inYteDummy")
                            } else return
                        }
                    }
                }
            }
        }
    }

    interface onBlueToothStateChange {
        fun onStateChanged(state: Boolean, device: String)
    }
}
