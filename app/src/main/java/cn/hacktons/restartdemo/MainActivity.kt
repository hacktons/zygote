package cn.hacktons.restartdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.TextView
import cn.hacktons.zygote.App

class MainActivity : Activity() {
    private val tag = MainActivity::class.java.simpleName

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "pid=" + Process.myPid() + ", ")
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv_restart).setOnClickListener {
            App.restart(it.context) {
                Log.i(tag, "do some clean task, such as flush pending log request")
                // TODO xxx
                Process.killProcess(Process.myPid())
            }
        }
        val source = intent?.let {
            if (it.getBooleanExtra(App.KEY_RESTART, false)) {
                it.getStringExtra(App.KEY_SOURCE)
            } else {
                "launcher"
            }
        }
        findViewById<TextView>(R.id.tv_source).text = "from: $source"
    }
}

