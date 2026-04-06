package com.example.eventmanager.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;

/**
 * Centralised admin-role checks used to gate access to admin-only screens.
 *
 * <p>Two entry points:
 * <ul>
 *   <li>{@link #checkAdminStatus(Context, AdminCheckCallback)} – async check,
 *       returns the result via callback.</li>
 *   <li>{@link #guardActivity(AppCompatActivity)} – convenience wrapper that
 *       finishes the activity when the user is not an admin.</li>
 * </ul>
 */
public final class AdminGuard {

    private AdminGuard() {}

    /** Callback that receives the result of an admin-status check. */
    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
    }

    /**
     * Asynchronously checks whether the current device user has admin privileges.
     *
     * @param context  Android context used to resolve the device ID
     * @param callback receives {@code true} when the user's Firestore profile has
     *                 {@code isAdmin == true}, {@code false} otherwise (including
     *                 on errors or missing profiles)
     */
    public static void checkAdminStatus(Context context, AdminCheckCallback callback) {
        String deviceId = new DeviceAuthManager().getDeviceId(context);
        FirebaseRepository.getInstance().getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                callback.onResult(result != null && result.isAdmin());
            }

            @Override
            public void onError(Exception e) {
                callback.onResult(false);
            }
        });
    }

    /**
     * Guards an admin-only activity. If the current user is not an admin the
     * activity is finished with a toast message. Call this in {@code onCreate}
     * immediately after {@code setContentView}.
     *
     * @param activity the admin activity to guard
     */
    public static void guardActivity(AppCompatActivity activity) {
        checkAdminStatus(activity, isAdmin -> {
            if (!isAdmin && !activity.isFinishing()) {
                Toast.makeText(activity,
                        R.string.admin_access_required,
                        Toast.LENGTH_SHORT).show();
                activity.finish();
            }
        });
    }
}
