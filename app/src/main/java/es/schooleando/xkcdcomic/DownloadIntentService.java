package es.schooleando.xkcdcomic;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class DownloadIntentService extends IntentService  {
    private static final String TAG = DownloadIntentService.class.getSimpleName();
    private ResultReceiver mReceiver;

    //CONSTANTES para devolver resultados (resultCodes)
    public static final int ERROR = 0;
    public static final int PROGRESS = 1;
    public static final int OK = 2;

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra("receiver");
        Log.d(TAG, "onHandleIntent");

        String urlString = intent.getStringExtra("url");
        Bundle bundle = new Bundle();

        //Informar de que el Comic es el actual (último publicado)
        bundle.putBoolean("esUltimoComic", urlString.equals("http://xkcd.com/info.0.json"));

        // TODO Aquí hacemos la conexión y accedemos a la imagen.
        //
        // TODO: Habrá que hacer 2 conexiones:
        //  1. Para descargar el resultado JSON para leer la URL.
        //  2. Una vez tenemos la URL descargar la imagen en la carpeta temporal.

        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        //Si está conectado...
        if (networkInfo != null && networkInfo.isConnected()) {
            HttpURLConnection conexion = null;

            try {
                //Conexión
                URL url = new URL(urlString);
                conexion = (HttpURLConnection)url.openConnection();

                StringBuilder resultado = new StringBuilder();

                //Flujos entrada
                InputStream is = new BufferedInputStream(conexion.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                //Se va leyendo y añadiendo
                String linea;
                while ((linea = br.readLine()) != null) {
                    resultado.append(linea);
                }

                //Desconexión
                conexion.disconnect();

                //Descargar el resultado JSON para leer la URL y descargar la imagen en carpeta temporal
                JSONObject jsonObject = new JSONObject(resultado.toString());
                bundle.putInt("numComic", jsonObject.getInt("num"));
                String urlComic = jsonObject.getString("img");

                url = new URL(urlComic);
                conexion = (HttpURLConnection)url.openConnection();
                conexion.setConnectTimeout(7000);
                conexion.setReadTimeout(7000);
                conexion.setRequestMethod("HEAD");
                conexion.connect();

                //Comprobar respuesta del servidor
                int codigoRespuesta = conexion.getResponseCode();
                if (codigoRespuesta == 200) {
                    int tamanyo = conexion.getContentLength();
                    is = url.openStream();

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];

                    //Escritura y progreso
                    for (int i; (i = is.read(buffer)) != -1; ) {
                        bos.write(buffer, 0, i);
                        if (tamanyo > 0) {
                            bundle.putInt("progreso", bos.size() * 100 / tamanyo);
                        } else {
                            bundle.putInt("progreso", i*-1);
                        }
                        mReceiver.send(PROGRESS, bundle);
                    }

                    //Temporal
                    File outputDir = getExternalCacheDir();
                    String[] datos = urlComic.split("/");
                    String[] trozo = datos[datos.length-1].split("\\.");
                    File outputFile = File.createTempFile(trozo[0], "." + trozo[1], outputDir);
                    outputFile.deleteOnExit();      //Eliminar temporal al SALIR

                    //Flujos salida
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    fos.write(bos.toByteArray());

                    // TODO: Devolver la URI de la imagen si todo ha ido bien.
                    bundle.putString("urlString", outputFile.getPath());
                    mReceiver.send(OK, bundle);

                    //Cerrar flujos I/O
                    bos.close();
                    is.close();
                    fos.close();

                } else {
                    bundle.putString("mensaje", "ERROR del código de respuesta del servidor: " + codigoRespuesta);
                    mReceiver.send(ERROR, bundle);
                }

                // TODO: Controlar los casos en los que no ha ido bien: excepciones en las conexiones, etc...
            } catch (MalformedURLException e) {
                bundle.putString("mensaje", "URL incorrecta");
                mReceiver.send(ERROR, bundle);
            } catch (SocketTimeoutException e) {
                bundle.putString("mensaje", "Excepción TimeOut");
                mReceiver.send(ERROR, bundle);
            } catch (IOException e) {
                bundle.putString("mensaje", "Error de I/O");
                mReceiver.send(ERROR, bundle);
            } catch (JSONException e) {
                bundle.putString("mensaje", "Error en el JSON");
                mReceiver.send(ERROR, bundle);
            } catch (Exception e) {
                bundle.putString("mensaje", "Excepción: " + e.getMessage());
                mReceiver.send(ERROR, bundle);
            } finally {
                //Se comprueba siempre si la conexión sigue para desconectarla
                if (conexion != null) {
                    conexion.disconnect();
                }
            }

        } else {
            bundle.putString("mensaje", "No está conectado");
            mReceiver.send(ERROR, bundle);
        }

        //mReceiver.send(0, Bundle.EMPTY);  // cambiar
    }
}
