package cn.garymb.ygomobile.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import java.net.URLEncoder;

import cn.garymb.ygomobile.GameUriManager;
import cn.garymb.ygomobile.YGOMobileActivity;
import cn.garymb.ygomobile.YGOStarter;
import cn.garymb.ygomobile.core.IrrlichtBridge;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.ui.plus.DialogPlus;
import cn.garymb.ygomobile.ui.plus.VUiKit;
import cn.garymb.ygomobile.utils.ComponentUtils;
import cn.garymb.ygomobile.utils.NetUtils;

import static cn.garymb.ygomobile.Constants.ACTION_RELOAD;
import static cn.garymb.ygomobile.Constants.ALIPAY_URL;

public class MainActivity extends HomeActivity {
    private GameUriManager mGameUriManager;
    private ImageUpdater mImageUpdater;
    private boolean enableStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YGOStarter.onCreated(this);
        mImageUpdater = new ImageUpdater(this);
        //资源复制
        checkResourceDownload((error, isNew) -> {
            if (error < 0) {
                enableStart = false;
            } else {
                enableStart = true;
            }
            if (isNew) {
                new DialogPlus(this)
                        .setTitle(getString(R.string.settings_about_change_log))
                        .setCancelable(false)
                        .loadUrl("file:///android_asset/changelog.html", Color.TRANSPARENT)
                        .hideButton()
                        .setCloseLinster((dlg, rs) -> {
                            dlg.dismiss();
                            //mImageUpdater
                            if (NetUtils.isConnected(getContext())) {
                                if (!mImageUpdater.isRunning()) {
                                    mImageUpdater.start();
                                }
                            }
                        })
                        .show();
            } else {
                getGameUriManager().doIntent(getIntent());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        YGOStarter.onResumed(this);
        //如果游戏Activity已经不存在了，则
        if (!ComponentUtils.isActivityRunning(this, new ComponentName(this, YGOMobileActivity.class))) {
            sendBroadcast(new Intent(IrrlichtBridge.ACTION_STOP).setPackage(getPackageName()));
        }
    }

    @Override
    protected void onDestroy() {
        YGOStarter.onDestroy(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_RELOAD.equals(intent.getAction())) {
            checkResourceDownload((error, isNew) -> {
                if (error < 0) {
                    enableStart = false;
                } else {
                    enableStart = true;
                }
                getGameUriManager().doIntent(getIntent());
            });
        } else {
            getGameUriManager().doIntent(intent);
        }
    }

    private GameUriManager getGameUriManager() {
        if (mGameUriManager == null) {
            mGameUriManager = new GameUriManager(this);
        }
        return mGameUriManager;
    }

    private void checkResourceDownload(ResCheckTask.ResCheckListener listener) {
        ResCheckTask task = new ResCheckTask(this, listener);
        if (Build.VERSION.SDK_INT >= 11) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }
    }

    @Override
    protected void openGame() {
        if (enableStart) {
            YGOStarter.startGame(this, null);
        } else {
            VUiKit.show(this, R.string.dont_start_game);
        }
    }

    @Override
    protected void updateImages() {
        if (!mImageUpdater.isRunning()) {
            mImageUpdater.start();
        } else {
            Toast.makeText(this, R.string.downloading_images, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 支付
     *
     * @param qrCode
     */
    @Override
    protected void openAliPay2Pay(String qrCode) {
        if (openAlipayPayPage(this, qrCode)) {
            Toast.makeText(this, "感谢您的支持", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "呀，没装支付宝", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean openAlipayPayPage(Context context, String qrcode) {
        try {
            qrcode = URLEncoder.encode(ALIPAY_URL, "utf-8");
        } catch (Exception e) {
        }
        try {
            final String alipayqr = "alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + qrcode;
            openUri(context, alipayqr + "%3F_s%3Dweb-other&_t=" + System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 发送一个intent
     *
     * @param context
     * @param s
     */
    private static void openUri(Context context, String s) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        context.startActivity(intent);
    }
}