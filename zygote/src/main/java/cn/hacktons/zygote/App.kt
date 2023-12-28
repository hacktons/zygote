package cn.hacktons.zygote

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log

/**
 * A helper used to restart application.
 *
 * <h3>Usage:
 * <pre>
 * [App].[App.restart]
</pre> *
</h3> */
object App {
    private val TAG = Zygote::class.java.simpleName
    private const val DEFAULT_DELAY_KILL_SECONDS = 3
    private const val KEY_DELAY = "__zygote_delay__"
    const val KEY_RESTART = "__zygote_restart__"
    const val KEY_SOURCE = "__zygote_source__"
    /**
     * Start a sub-process to kill main process and handle the `kill` yourself
     *
     * @param ctx      application context
     * @param listener kill listener
     * @param target   target activity, subclass of [Zygote]
     * @param delay    delayed seconds to kill sub-process
     */
    /**
     * Start a sub-process to kill main process
     */
    @JvmOverloads
    @JvmStatic
    fun restart(
        ctx: Context,
        target: Class<out Zygote> = Zygote::class.java,
        delay: Int = DEFAULT_DELAY_KILL_SECONDS,
        listener: OnKillListener = {
            Process.killProcess(Process.myPid());
        }
    ) {
        val context = ctx.applicationContext
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "kill main process:" + Process.myPid())
                context.unregisterReceiver(this)
                setResult(Activity.RESULT_OK, "", null)
                listener.invoke()
            }
        }
        val action = action(context)
        val permission = permission(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(action),
                permission,
                null,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(receiver, IntentFilter(action), permission, null)
        }
        val intent = Intent(context, target)
        intent.putExtra(KEY_DELAY, delay)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun permission(ctx: Context): String {
        return String.format("%s.permission.RESTART_APP", ctx.packageName)
    }

    private fun action(ctx: Context): String {
        return String.format("%s.action.RESTART_ACTION", ctx.packageName)
    }

    /**
     * Activity in sub-process which will kill main-process
     */
    open class Zygote : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "pid=" + Process.myPid() + ", ")
            setContent()
            startLauncherIntentAndSelfExit(this)
        }

        /**
         * default Zygote is transparent with empty contentView
         */
        protected open fun setContent() {}

        /**
         * Callback before the sub-process is going to be killed
         *
         * @return true allow the sub-process to be killed, otherwise sub-process is kept
         */
        protected open fun onKillSubProcessSelf() {
            Log.d(TAG, "kill sub-process: " + Process.myPid())
            Process.killProcess(Process.myPid())
        }

        /**
         * Notify the main process to exit then launch main activity in a new main process.
         */
        private fun startLauncherIntentAndSelfExit(context: Context) {
            val ctx = context.applicationContext
            val permission = String.format(permission(ctx), ctx.packageName)
            ctx.sendOrderedBroadcast(Intent(action(ctx)), permission, object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d(TAG, "restart new process")
                    startLauncher(context)
                    val delay = getIntent().getIntExtra(KEY_DELAY, DEFAULT_DELAY_KILL_SECONDS)
                    if (delay > 0) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            onKillSubProcessSelf()
                        }, delay * 1000L)
                    } else {
                        onKillSubProcessSelf()
                    }
                }
            }, null, RESULT_OK, "", null)
        }

        /**
         * Start default main activity
         */
        private fun startLauncher(context: Context) {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)!!
            val componentName = intent.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            mainIntent.setPackage(context.packageName)
            mainIntent.putExtra(KEY_RESTART, true)
            mainIntent.putExtra(KEY_SOURCE, javaClass.simpleName + "#pid(" + Process.myPid() + ")")
            context.startActivity(mainIntent)
        }
    }
}
/**
 * Kill listener.
 * Clear resource, submit any pending requests before kill process
 */
typealias OnKillListener = () -> Unit