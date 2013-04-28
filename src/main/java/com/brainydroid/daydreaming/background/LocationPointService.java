package com.brainydroid.daydreaming.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import com.brainydroid.daydreaming.db.LocationPoint;
import com.brainydroid.daydreaming.network.SntpClient;
import com.brainydroid.daydreaming.network.SntpClientCallback;
import com.brainydroid.daydreaming.ui.Config;
import com.google.inject.Inject;
import roboguice.service.RoboService;

/**
 * Start and manage {@link LocationService} to obtain a {@link
 * LocationPoint}.
 * <p/>
 * The service starts {@code LocationService} and lets it listen for {@code
 * LISTENING_TIME} milliseconds. That is, it starts the {@code
 * LocationService} and schedules itself ({@code LocationPointService}) to
 * start again later to stop that same {@code LocationService} after the
 * listening period. When stopping the listener, it also schedules itself
 * to start again after {@code SAMPLE_INTERVAL} milliseconds for the next
 * listening period.
 * <p/>
 * {@code LocationService} is only ever started if data and location
 * accesses are allowed.
 *
 * @author Sébastien Lerique
 * @author Vincent Adam
 */
public class LocationPointService extends RoboService {

    private static String TAG = "LocationPointService";

    /** Duration to listen for location updates. */
    public static long LISTENING_TIME = 2 * 60 * 1000;    // 2 min (in ms)
    /** Time to wait before starting to listen again. */
    public static long SAMPLE_INTERVAL = 18 * 60 * 1000;  // 18 min (in ms)
    /** Extra to set to {@code true} to stop the listening. */
    public static String STOP_LOCATION_LISTENING = "stopLocationListening";

    private boolean sntpRequestDone = false;
    private boolean serviceConnectionDone = false;

    @Inject SntpClient sntpClient;
    @Inject LocationPoint locationPoint;
    @Inject AlarmManager alarmManager;
    @Inject StatusManager statusManager;
    @Inject LocationServiceConnection locationServiceConnection;

    @Override
    public void onCreate() {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] onCreate");
        }

        super.onCreate();
        // Do nothing else here, the logic is i onStartCommand
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] onStartCommand");
        }

        super.onStartCommand(intent, flags, startId);

        // Were we started to start or to stop the listening?
        if (intent.getBooleanExtra(STOP_LOCATION_LISTENING, false)) {
            // If so, schedule ourselves for the next listening
            stopLocationListening();
            scheduleNextService();
        } else {
            // Only start listening if there a chance to obtain something
            if (statusManager.isDataAndLocationEnabled()) {
                startLocationListening();
            }

            // Don't forget to stop listening after a few minutes
            scheduleStopLocationListening();
        }

        // The service stops itself through callbacks set in
        // stopLocationListening or startLocationListening,
        // so no need to stop ourselves here.
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] onDestroy");
        }

        // Don't forget to unbind from the LocationService
        locationServiceConnection.unbindLocationService();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] onBind");
        }

        // Don't allow binding to ourselves
        return null;
    }

    /**
     * Stop the {@link LocationService} service and stop ourselves when
     * that's done.
     */
    private void stopLocationListening() {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] stopLocationListening");
        }

        // No need for an NTP request since we're shutting down
        setSntpRequestDone();

        // locationServiceConnection will clear our listener (registered on
        // the LocationService) when it binds to LocationService. We do
        // this even if the LocationService is not running (see test below),
        // since it will flush any other callback that could have been
        // waiting to be transferred to the LocationService on bind.
        locationServiceConnection.clearLocationPointCallback();

        // If LocationService is not running there's no need to stop it
        if (statusManager.isLocationServiceRunning()) {
            // We're not doing a service connection, so we're all done
            setServiceConnectionDone();
            return;
        }

        // Once it's connected to LocationService, locationServiceConnection
        // will also stop us through the following callback.
        ServiceConnectionCallback serviceConnectionCallback =
                new ServiceConnectionCallback() {

            private final String TAG = "ServiceConnectionCallback";

            @Override
            public void onServiceConnected() {

                // Debug
                if (Config.LOGD) {
                    Log.d(TAG, "[fn] onServiceConnected");
                }

                // Tell LocationPointService that we're done
                LocationPointService.this.setServiceConnectionDone();
            }

        };

        locationServiceConnection.setOnServiceConnectedCallback(
                serviceConnectionCallback);

        // Making the bind transfers the "clear listener" message to the
        // LocationService. Upon connection to the LocationService,
        // the locationServiceConnection also stops us through the above
        // callback. That, in turn, will make locationServiceConnection
        // unbind from the LocationService (see onDestroy). At that point the
        // LocationService will stop itself if it has no other listeners
        // registered.
        //
        // This sounds terribly complicated but it's the only way to deal
        // with the callbacks on a single thread (calling
        // locationServiceConnection.unbindLocationService() right here
        // would unbind before the connection to LocationService has taken
        // place: indeed, the connection is made after this function
        // returns).
        locationServiceConnection.bindLocationService();

    }

    /**
     * Start the {@link LocationService} service and register a listener on
     * it.
     */
    private void startLocationListening() {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] startLocationListening");
        }

        // Same mechanism as in stopLocationListening: when we ask the
        // locationServiceConnection to bind,
        // it registers the locationCallback (below) on the LocationService.
        // When the binding is done, locationServiceConnection tells us so
        // through this serviceConnectionCallback.
        ServiceConnectionCallback serviceConnectionCallback =
                new ServiceConnectionCallback() {

            private final String TAG = "ServiceConnectionCallback";

            @Override
            public void onServiceConnected() {

                // Debug
                if (Config.LOGD) {
                    Log.d(TAG, "[fn] onServiceConnected");
                }

                // Tell LocationPointService that we're done
                LocationPointService.this.setServiceConnectionDone();
            }

        };

        // This will be called by LocationService when it receives location
        // data. It gets registered on the LocationService when the
        // locationServiceConnection binds.
        LocationCallback locationCallback = new LocationCallback() {

            private final String TAG = "LocationCallback";

            @Override
            public void onLocationReceived(Location location) {

                // Debug
                if (Config.LOGD) {
                    Log.d(TAG, "[fn] (locationCallback) onLocationReceived");
                }

                locationPoint.setLocation(location);
                // save() is called from saveAndStopSelfIfAllDone
            }

        };

        // We also want an accurate timestamp for this location data (i.e.
        // not dependent on the user's settings), so we'll get it with NTP.
        // This callback is called by the sntpClient when the NTP request
        // completes.
        SntpClientCallback sntpCallback = new SntpClientCallback() {

            private final String TAG = "SntpClientCallback";

            @Override
            public void onTimeReceived(SntpClient sntpClient) {

                // Debug
                if (Config.LOGD) {
                    Log.d(TAG, "[fn] (sntpCallback) onTimeReceived");
                }

                if (sntpClient != null) {
                    locationPoint.setTimestamp(sntpClient.getNow());
                    // save() is called from saveAndStopSelfIfAllDone
                }

                // Tell LocationPointService that we're done
                LocationPointService.this.setSntpRequestDone();
            }

        };

        locationServiceConnection.setOnServiceConnectedCallback(
                serviceConnectionCallback);
        locationServiceConnection.setLocationPointCallback(locationCallback);

        sntpClient.asyncRequestTime(sntpCallback);

        // If the service isn't already running, it needs to be started as
        // well as bound, to be sure it stays alive after we unbind. If it
        // is already running, someone else took care of starting it.
        if (!statusManager.isLocationServiceRunning()) {
            locationServiceConnection.bindLocationService();
            locationServiceConnection.startLocationService();
        } else {
            locationServiceConnection.bindLocationService();
        }
    }

    /**
     * Schedule the next run of {@code LocationPointService},
     * after SAMPLE_INTERVAL milliseconds.
     */
    private void scheduleNextService() {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] scheduleNextService");
        }

        // Build the scheduled time
        long scheduledTime = SystemClock.elapsedRealtime() + SAMPLE_INTERVAL;

        // Create the PendingIntent. Any previous one is cancelled.
        Intent intent = new Intent(this, LocationPointService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // And set the alarm
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                scheduledTime, pendingIntent);
    }

    /**
     * Schedule the end of the listening to location updates,
     * when we stop the LocationService after LISTENING_TIME.
     */
    private void scheduleStopLocationListening() {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] scheduleStopLocationListening");
        }

        // Build the scheduled time
        long scheduledTime = SystemClock.elapsedRealtime() + LISTENING_TIME;

        // Create the PendingIntent with a flag telling
        // LocationPointService to stop the listening. Any previous
        // PendingIntent is cancelled.
        Intent intent = new Intent(this, LocationPointService.class);
        intent.putExtra(STOP_LOCATION_LISTENING, true);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // And set the alarm
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                scheduledTime, pendingIntent);
    }

    /**
     * Record that {@link SntpClient}'s NTP request has completed,
     * and stop ourselves if everything else has completed also.
     */
    private void setSntpRequestDone() {

        // Verbose
        if (Config.LOGV) {
            Log.v(TAG, "[fn] setSntpRequestDone");
        }

        sntpRequestDone = true;

        // If we're all done, we can stop ourselves
        saveAndStopSelfIfAllDone();
    }

    /**
     * Record that {@link LocationServiceConnection}'s service connection has
     * completed and stop ourselves if everything else has completed also.
     */
    private void setServiceConnectionDone() {

        // Verbose
        if (Config.LOGV) {
            Log.v(TAG, "[fn] setServiceConnectionDone");
        }

        serviceConnectionDone = true;

        // If we're all done, we can stop ourselves
        saveAndStopSelfIfAllDone();
    }

    /**
     * Save {@link LocationPoint} and stop ourselves if everything is done.
     */
    private void saveAndStopSelfIfAllDone() {

        // Debug
        if (Config.LOGD) {
            Log.d(TAG, "[fn] saveAndStopSelfIfAllDone");
        }

        if (sntpRequestDone && serviceConnectionDone) {
            if (locationPoint != null) {
                locationPoint.save();
            }
            stopSelf();
        }
    }

}
