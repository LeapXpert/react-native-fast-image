package com.dylanvann.fastimage.bb;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

public final class BBImageModelLoader implements ModelLoader<BBModel, InputStream> {

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(BBModel model, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(model.url), new BBDataFetcher(model));
    }

    @Override
    public boolean handles(BBModel model) {
        return model.url.startsWith("http");
    }
}