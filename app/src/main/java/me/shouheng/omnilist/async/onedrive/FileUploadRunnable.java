package me.shouheng.omnilist.async.onedrive;

import com.onedrive.sdk.extensions.Item;

import java.io.File;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import me.shouheng.omnilist.manager.onedrive.OneDriveManager;
import me.shouheng.omnilist.model.Attachment;
import me.shouheng.omnilist.provider.AttachmentsStore;
import me.shouheng.omnilist.utils.LogUtils;

/**
 * Created by shouh on 2018/4/2.*/
public class FileUploadRunnable implements Runnable {
    private Attachment attachment;
    private String toItemId;
    private CountDownLatch downLatch;
    private OnUploadListener onUploadListener;

    FileUploadRunnable(Attachment attachment, String toItemId, CountDownLatch downLatch, OnUploadListener onUploadListener) {
        this.attachment = attachment;
        this.downLatch = downLatch;
        this.toItemId = toItemId;
        this.onUploadListener = onUploadListener;
    }

    @Override
    public void run() {
        try {
            LogUtils.d(Thread.currentThread() + " ran ");
            OneDriveManager.getInstance().upload(toItemId,
                    new File(attachment.getPath()),
                    ConflictBehavior.REPLACE,
                    new OneDriveManager.UploadProgressCallback<Item>() {

                        @Override
                        public void success(Item item) {
                            // Update attachment fields in database.
                            attachment.setOneDriveItemId(item.id);
                            attachment.setOneDriveSyncTime(new Date());
                            AttachmentsStore.getInstance().update(attachment);
                            // Return.
                            onFinish();
                        }

                        @Override
                        public void failure(Exception e) {
                            onFailed(e.getMessage());
                        }
                    });
        } catch (Exception e) {
            onFailed(e.getMessage());
        }
    }

    private void onFinish() {
        LogUtils.d(Thread.currentThread() + " finished ");
        this.downLatch.countDown();
        if (onUploadListener != null) {
            onUploadListener.onSuccess();
        }
    }

    private void onFailed(String msg) {
        LogUtils.d(Thread.currentThread() + " failed ");
        this.downLatch.countDown();
        if (onUploadListener != null) {
            onUploadListener.onFail(msg);
        }
    }

    public interface OnUploadListener {
        void onSuccess();
        void onFail(String msg);
    }
}
