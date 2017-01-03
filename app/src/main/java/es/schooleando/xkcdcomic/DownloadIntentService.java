package es.schooleando.xkcdcomic;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

public class DownloadIntentService extends IntentService  {
    private static final String TAG = DownloadIntentService.class.getSimpleName();
    private ResultReceiver mReceiver;

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra("receiver");
        Log.d(TAG, "onHandleIntent");

        // TODO Aquí hacemos la conexión y accedemos a la imagen.
        //
        // TODO: Habrá que hacer 2 conexiones:
        //  1. Para descargar el resultado JSON para leer la URL.
        //  2. Una vez tenemos la URL descargar la imagen en la carpeta temporal.


        // TODO: Devolver la URI de la imagen si todo ha ido bien.

        // TODO: Controlar los casos en los que no ha ido bien: excepciones en las conexiones, etc...

        mReceiver.send(0, Bundle.EMPTY);  // cambiar
    }
}
