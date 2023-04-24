package com.dylanvann.fastimage.bb;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class BBModelLoaderFactory  implements ModelLoaderFactory<BBModel, InputStream> {

    @Override
    public ModelLoader<BBModel, InputStream> build(MultiModelLoaderFactory unused) {
        return new BBImageModelLoader();
    }

    @Override
    public void teardown() {
        // Do nothing.
    }
}