package com.example.biometricdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        print("macy777 --onReceive--> $intent")
        val passcode = intent?.getStringExtra("passcode")
        val param = intent?.getStringExtra("param")
        print("macy777 --onReceive--> $passcode $param")
        Toast.makeText(context, "passcode received!!  $passcode $param", Toast.LENGTH_LONG).show()
    }
}