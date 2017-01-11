package es.schooleando.xkcdcomic;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public class ComicActivity extends AppCompatActivity implements BgResultReceiver.Receiver {
    private BgResultReceiver mResultReceiver;

    //Vistas del layout
    private ImageView imagen;
    private ProgressBar progressBar;

    private int topComic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);

        //Enganchar vistas del layout
        imagen = (ImageView)findViewById(R.id.imageView);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        //Instanciar el BgResultReceiver
        mResultReceiver = new BgResultReceiver(new Handler());
        mResultReceiver.setReceiver(this);

        // Esto es gratis: al arrancar debemos cargar el cómic actual
        Intent intent = new Intent(this, DownloadIntentService.class);
        intent.putExtra("url", "http://xkcd.com/info.0.json");
        intent.putExtra("receiver", mResultReceiver);
        startService(intent);

        // TODO: Falta un callback de ImageView para hacer click en la imagen y que se descargue otro comic aleatorio.
        //Listener al pinchar en la imagen
        imagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int aleatorio = ThreadLocalRandom.current().nextInt(1, topComic+1);
                    Intent intent = new Intent(ComicActivity.this, DownloadIntentService.class);
                    intent.putExtra("url", "http://xkcd.com/" + aleatorio + "/info.0.json");
                    intent.putExtra("receiver", mResultReceiver);
                    startService(intent);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(ComicActivity.this, "Excepción IllegalArgument al cambiar de imagen", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // TODO: podemos recibir diferentes resultCodes del IntentService
        //      ERROR -> ha habido un problema de la conexión (Toast)
        //      PROGRESS -> nos estamos descargando la imagen (ProgressBar)
        //      OK -> nos hemos descargado la imagen correctamente. (ImageView)
        // Debeis controlar cada caso

        //Si el Comic descargado es el último, se descargará el siguiente de forma aleatoria
        if (resultData.getBoolean("esUltimoComic")) {
            topComic = resultData.getInt("numComic");
        }

        //Según los resultados recibidos del IntentService
        switch (resultCode) {
            //      ERROR -> ha habido un problema de la conexión (Toast)
            case DownloadIntentService.ERROR:
                String mensaje = resultData.getString("mensaje");
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(ComicActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                break;

            //      PROGRESS -> nos estamos descargando la imagen (ProgressBar)
            case DownloadIntentService.PROGRESS:
                int progreso = resultData.getInt("progreso");
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(progreso < 0);
                progressBar.setProgress(progreso);
                break;

            //      OK -> nos hemos descargado la imagen correctamente. (ImageView)
            case DownloadIntentService.OK:
                String urlString = resultData.getString("urlString");
                progressBar.setVisibility(View.INVISIBLE);
                File fichero = new File(urlString);
                if (fichero.exists()) {
                    imagen.setImageBitmap(BitmapFactory.decodeFile(fichero.getAbsolutePath()));
                } else {
                    Toast.makeText(ComicActivity.this, "El fichero no existe", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

}
