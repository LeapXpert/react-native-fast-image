package com.dylanvann.fastimage;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.dylanvann.fastimage.bb.BBModel;
import com.dylanvann.fastimage.bb.BBModelLoaderFactory;

import java.io.InputStream;

// We need an AppGlideModule to be present for progress events to work.
@GlideModule
public final class FastImageGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.prepend(BBModel.class, InputStream.class, new BBModelLoaderFactory());
    }

}
