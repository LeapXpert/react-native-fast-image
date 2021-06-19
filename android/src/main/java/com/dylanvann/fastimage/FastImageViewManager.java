package com.dylanvann.fastimage;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.dylanvann.fastimage.bb.BBDataFetcher;
import com.dylanvann.fastimage.bb.BBModel;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.good.gd.apache.http.Header;
import com.good.gd.apache.http.HttpResponse;
import com.good.gd.apache.http.client.methods.HttpGet;
import com.good.gd.file.File;
import com.good.gd.file.FileInputStream;
import com.good.gd.file.FileOutputStream;
import com.good.gd.file.GDFileSystem;
import com.good.gd.net.GDHttpClient;
import com.leap.blackberry_utils.BackgroundService;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import static com.dylanvann.fastimage.FastImageRequestListener.REACT_ON_ERROR_EVENT;
import static com.dylanvann.fastimage.FastImageRequestListener.REACT_ON_LOAD_END_EVENT;
import static com.dylanvann.fastimage.FastImageRequestListener.REACT_ON_LOAD_EVENT;
import static com.good.gd.file.GDFileSystem.MODE_PRIVATE;

class FastImageViewManager extends SimpleViewManager<FastImageViewWithUrl> implements FastImageProgressListener {

    private static final String REACT_CLASS = "FastImageView";
    private static final String REACT_ON_LOAD_START_EVENT = "onFastImageLoadStart";
    private static final String REACT_ON_PROGRESS_EVENT = "onFastImageProgress";
    private static final Map<String, List<FastImageViewWithUrl>> VIEWS_FOR_URLS = new WeakHashMap<>();
    static String TAG = "FAST_IMAGE";
    Context context;

    @Nullable
    private RequestManager requestManager = null;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected FastImageViewWithUrl createViewInstance(ThemedReactContext reactContext) {
        this.context = reactContext;
        if (isValidContextForGlide(reactContext)) {
            requestManager = Glide.with(reactContext);
        }

        return new FastImageViewWithUrl(reactContext);
    }

    public String md5(@Nullable String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & b));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return s;
    }

    @ReactProp(name = "source")
    public void setSrc(final FastImageViewWithUrl view, @Nullable final ReadableMap source) {
        if (source == null || !source.hasKey("uri") || isNullOrEmpty(source.getString("uri"))) {
            // Cancel existing requests.
            if (requestManager != null) {
                requestManager.clear(view);
            }

            if (view.glideUrl != null) {
                FastImageOkHttpProgressGlideModule.forget(view.glideUrl.toStringUrl());
            }
            // Clear the image.
            view.setImageDrawable(null);
            return;
        }
        final String uri = source.getString("uri");
//        if (downloadingUrls.contains(uri)) {
//            Log.e(TAG, "add to listener: " + uri);
//            listeners.add(new DownloadListener(uri) {
//                @Override
//                void onDownloaded() {
//                    Log.e(TAG, "Something has downloaded");
//                    final String fileName = md5(uri) + ".png";
//                    final File file = new File(fileName);
//                    if (file.exists()) {
//                        displayImage(requestManager, file, view, uri);
//                    }
//                }
//            });
//            Log.e(TAG, "listener size: " + listeners.size());
//            return;
//        }
//        downloadingUrls.add(uri);
        final FastImageSource imageSource = FastImageViewConverter.getImageSource(view.getContext(), source);
        final GlideUrl glideUrl = imageSource.getGlideUrl();

        // Cancel existing request.
        view.glideUrl = glideUrl;
        if (requestManager != null) {
            requestManager.clear(view);
        }

        String key = glideUrl.toStringUrl();
        FastImageOkHttpProgressGlideModule.expect(key, this);
        List<FastImageViewWithUrl> viewsForKey = VIEWS_FOR_URLS.get(key);
        if (viewsForKey != null && !viewsForKey.contains(view)) {
            viewsForKey.add(view);
        } else if (viewsForKey == null) {
            List<FastImageViewWithUrl> newViewsForKeys = new ArrayList<>(Collections.singletonList(view));
            VIEWS_FOR_URLS.put(key, newViewsForKeys);
        }

        Map<String, String> headers = imageSource.getHeaders().getHeaders();

        final ThemedReactContext context = (ThemedReactContext) view.getContext();
        RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
        int viewId = view.getId();
        eventEmitter.receiveEvent(viewId, REACT_ON_LOAD_START_EVENT, new WritableNativeMap());


        if (uri.startsWith("http")) {
            requestManager
                    .load(new BBModel(uri, headers))
                    .listener(new FastImageRequestListener(uri))
                    .into(view);
            return;
        }

        requestManager
                .load(uri)
                .listener(new FastImageRequestListener(uri))
                .into(view);
//
//        if (requestManager != null) {
//            if (uri != null && ((uri.startsWith("/") || uri.startsWith("file://")))) {
//                requestManager
//                        .load(new java.io.File(uri.replace("file://", "")))
//                        .listener(new FastImageRequestListener(uri))
//                        .into(view);
//                downloadingUrls.remove(uri);
//                return;
//            }
//            if (uri.startsWith("data:")) {
//                requestManager
//                        .load(uri)
//                        .listener(new FastImageRequestListener(uri))
//                        .into(view);
//                downloadingUrls.remove(uri);
//                return;
//            }
//            final String fileName = md5(uri) + ".png";
//            final File file = new File(fileName);
//            if (file.exists() && file.length() > 200) {
//                long length = file.length();
//                Log.e("File exist length", "" + length);
//                displayImage(requestManager, file, view, uri);
//                downloadingUrls.remove(uri);
//                Iterator<DownloadListener> i = listeners.iterator();
//                while (i.hasNext()) {
//                    DownloadListener s = i.next(); // must be called before you can call i.remove()
//                    // Do something
//                    s.onDownloaded();
//                    i.remove();
//                }
//                return;
//            }
//            BackgroundService.execute(new Runnable() {
//                @Override
//                public void run() {
//                    Log.e(TAG, "start download image" + "==" + uri);
//                    int viewId = view.getId();
//                    RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
//                    eventEmitter.receiveEvent(viewId, REACT_ON_ERROR_EVENT, new WritableNativeMap());
//                    eventEmitter.receiveEvent(viewId, REACT_ON_LOAD_END_EVENT, new WritableNativeMap());
//                    boolean success = downloadFile(source.getString("uri"), imageSource.getHeaders(), fileName);
//                    Log.e(TAG, "image downloaded" + "==" + uri);
//                    Log.e(TAG, "listener size" + "==" + listeners.size());
//
//                    downloadingUrls.remove(uri);
//                    if (success) {
//                        displayImage(requestManager, file, view, uri);
//                        Iterator<DownloadListener> i = listeners.iterator();
//                        while (i.hasNext()) {
//                            DownloadListener s = i.next(); // must be called before you can call i.remove()
//                            // Do something
//                            s.onDownloaded();
//                            i.remove();
//                        }
//                    }
//                }
//            });


//        }
    }

    static synchronized void displayImage(final RequestManager requestManager, File file, final ImageView view, final String url) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            final byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            fileInputStream.close();
            BackgroundService.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    requestManager
                            .load(bytes)
                            .listener(new FastImageRequestListener(url))
                            .into(view);
                }
            });

        } catch (IOException e) {
            file.delete();
            Log.e("FAST_IMAGE", "image error" + "==" + e.getMessage());
            Log.e("FAST_IMAGE", "image error url" + "==" + url);
            e.printStackTrace();
        }
    }


    private static final List<DownloadListener> listeners = new ArrayList<>();
    private static final List<String> downloadingUrls = new ArrayList<>();

    public boolean downloadFile(String url, Headers headers, String fileName) {
        try {
            GDHttpClient client = new GDHttpClient();
            HttpGet get = new HttpGet(url);
            for (String key : headers.getHeaders().keySet()) {
                String value = headers.getHeaders().get(key);
                get.addHeader(key, value);
            }
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                Log.e("FastImageViewManager", "Image error: " + statusCode);
                return false;
            }
            InputStream is = response.getEntity().getContent();


            FileOutputStream fos = GDFileSystem.openFileOutput(fileName, MODE_PRIVATE);
            int read = 0;
            byte[] buffer = new byte[32768];
            while ((read = is.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            is.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    @ReactProp(name = "tintColor", customType = "Color")
    public void setTintColor(FastImageViewWithUrl view, @Nullable Integer color) {
        if (color == null) {
            view.clearColorFilter();
        } else {
            view.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    @ReactProp(name = "resizeMode")
    public void setResizeMode(FastImageViewWithUrl view, String resizeMode) {
        final FastImageViewWithUrl.ScaleType scaleType = FastImageViewConverter.getScaleType(resizeMode);
        view.setScaleType(scaleType);
    }

    @Override
    public void onDropViewInstance(FastImageViewWithUrl view) {
        // This will cancel existing requests.
        if (requestManager != null) {
            requestManager.clear(view);
        }

        if (view.glideUrl != null) {
            final String key = view.glideUrl.toString();
            FastImageOkHttpProgressGlideModule.forget(key);
            List<FastImageViewWithUrl> viewsForKey = VIEWS_FOR_URLS.get(key);
            if (viewsForKey != null) {
                viewsForKey.remove(view);
                if (viewsForKey.size() == 0) VIEWS_FOR_URLS.remove(key);
            }
        }

        super.onDropViewInstance(view);
    }

    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.<String, Object>builder()
                .put(REACT_ON_LOAD_START_EVENT, MapBuilder.of("registrationName", REACT_ON_LOAD_START_EVENT))
                .put(REACT_ON_PROGRESS_EVENT, MapBuilder.of("registrationName", REACT_ON_PROGRESS_EVENT))
                .put(REACT_ON_LOAD_EVENT, MapBuilder.of("registrationName", REACT_ON_LOAD_EVENT))
                .put(REACT_ON_ERROR_EVENT, MapBuilder.of("registrationName", REACT_ON_ERROR_EVENT))
                .put(REACT_ON_LOAD_END_EVENT, MapBuilder.of("registrationName", REACT_ON_LOAD_END_EVENT))
                .build();
    }

    @Override
    public void onProgress(String key, long bytesRead, long expectedLength) {
        List<FastImageViewWithUrl> viewsForKey = VIEWS_FOR_URLS.get(key);
        if (viewsForKey != null) {
            for (FastImageViewWithUrl view : viewsForKey) {
                WritableMap event = new WritableNativeMap();
                event.putInt("loaded", (int) bytesRead);
                event.putInt("total", (int) expectedLength);
                ThemedReactContext context = (ThemedReactContext) view.getContext();
                RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
                int viewId = view.getId();
                eventEmitter.receiveEvent(viewId, REACT_ON_PROGRESS_EVENT, event);
            }
        }
    }

    @Override
    public float getGranularityPercentage() {
        return 0.5f;
    }

    private boolean isNullOrEmpty(final String url) {
        return url == null || url.trim().isEmpty();
    }


    private static boolean isValidContextForGlide(final Context context) {
        Activity activity = getActivityFromContext(context);

        if (activity == null) {
            return false;
        }

        return !isActivityDestroyed(activity);
    }

    private static Activity getActivityFromContext(final Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }

        if (context instanceof ThemedReactContext) {
            final Context baseContext = ((ThemedReactContext) context).getBaseContext();
            if (baseContext instanceof Activity) {
                return (Activity) baseContext;
            }

            if (baseContext instanceof ContextWrapper) {
                final ContextWrapper contextWrapper = (ContextWrapper) baseContext;
                final Context wrapperBaseContext = contextWrapper.getBaseContext();
                if (wrapperBaseContext instanceof Activity) {
                    return (Activity) wrapperBaseContext;
                }
            }
        }

        return null;
    }

    private static boolean isActivityDestroyed(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return activity.isDestroyed() || activity.isFinishing();
        } else {
            return activity.isDestroyed() || activity.isFinishing() || activity.isChangingConfigurations();
        }

    }

    static class DownloadListener {
        String url;

        public DownloadListener(String url) {
            this.url = url;
        }

        void onDownloaded() {
        }

    }

}
