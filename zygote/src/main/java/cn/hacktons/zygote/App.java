package cn.hacktons.zygote;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

/**
 * A helper used to restart application.
 *
 * <h3>Usage:</h1>
 * <pre>
 *     {@link App}.{@link App#restart(Context)}
 * </pre>
 */
public class App {
    private final static String TAG = Zygote.class.getSimpleName();

    /**
     * Start a sub-process to kill main process
     */
    public static void restart(Context ctx) {
        restart(ctx, () -> Process.killProcess(Process.myPid()), Zygote.class);
    }

    /**
     * Start a sub-process to kill main process and handle the `kill` yourself
     *
     * @param ctx      application context
     * @param listener kill listener
     * @param clz      target activity, subclass of {@link Zygote}
     */
    public static void restart(Context ctx, final OnKillListener listener, Class<? extends Zygote> clz) {
        Context context = ctx.getApplicationContext();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "kill self");
                setResult(Activity.RESULT_OK, "", null);
                listener.onKill();
                context.unregisterReceiver(this);
            }
        };
        String action = action(ctx);
        String permission = permission(ctx);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, new IntentFilter(action), permission, null, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, new IntentFilter(action), permission, null);
        }
        Intent intent = new Intent(context, clz);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Activity in sub-process which will kill main-process
     */
    public static class Zygote extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "pid=" + Process.myPid() + ", ");
            setContent();
            startMainProcessThenSelfExit(this);
        }

        /**
         * default Zygote is transparent with empty contentView
         */
        protected void setContent() {

        }

        /**
         * Callback before the sub-process is going to be killed
         *
         * @return true allow the sub-process to be killed, otherwise sub-process is kept
         */
        protected void onKillSubProcessSelf() {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        /**
         * Notify the main process to exit then launch main activity in a new main process.
         */
        public void startMainProcessThenSelfExit(Context context) {
            Context ctx = context.getApplicationContext();
            String permission = String.format(permission(ctx), ctx.getPackageName());
            ctx.sendOrderedBroadcast(new Intent(action(ctx)), permission, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "restart new process");
                    startLauncher(context);
                    onKillSubProcessSelf();
                }
            }, null, Activity.RESULT_OK, "", null);
        }

        /**
         * Start default main activity
         */
        private void startLauncher(Context context) {
            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            assert intent != null;
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            mainIntent.setPackage(context.getPackageName());
            mainIntent.putExtra("__zygote_restart__", true);
            context.startActivity(mainIntent);
        }
    }

    private static String permission(Context ctx) {
        return String.format("%s.permission.RESTART_APP", ctx.getPackageName());
    }

    private static String action(Context ctx) {
        return String.format("%s.action.RESTART_ACTION", ctx.getPackageName());
    }

    /**
     * Kill listener
     */
    public interface OnKillListener {
        /**
         * Do clear resource, submit any pending requests then kill process
         */
        void onKill();
    }

}