package com.dylanvann.fastimage.bb;

import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.good.gd.apache.http.HttpResponse;
import com.good.gd.apache.http.client.methods.HttpGet;
import com.good.gd.file.FileInputStream;
import com.good.gd.file.GDFileSystem;
import com.good.gd.net.GDHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.good.gd.file.GDFileSystem.MODE_PRIVATE;

public class BBDataFetcher implements DataFetcher<InputStream> {

    BBModel model;

    public BBDataFetcher(BBModel model) {
        this.model = model;
    }

    InputStream stream;

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        try {
            GDHttpClient client = new GDHttpClient();
            HttpGet get = new HttpGet(model.url);
            Log.e("FAST_IMAGE", model.url);
            for (String key : model.headers.keySet()) {
                get.addHeader(key, model.headers.get(key));
            }
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                Log.e("FastImageViewManager", "Image error: " + statusCode);
                callback.onLoadFailed(new Exception("Server error: " + statusCode));
                return;
            }
            InputStream is = response.getEntity().getContent();
            callback.onDataReady(is);
        } catch (IOException e) {
            callback.onLoadFailed(e);
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup() {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}
