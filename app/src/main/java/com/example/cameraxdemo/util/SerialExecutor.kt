package com.example.cameraxdemo.util

import android.os.Handler
import java.util.concurrent.Executor

class SerialExecutor(handler: Handler): Executor {

    private var mHandler: Handler? = null

    init {
        mHandler = handler
    }

    override fun execute(command: Runnable?) {
        mHandler?.post(command)
    }
}