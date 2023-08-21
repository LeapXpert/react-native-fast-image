package com.dylanvann.fastimage.bb;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

// We need an AppGlideModule to be present for progress events to work.
@GlideModule
public final class FastImageBBGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.prepend(BBModel.class, InputStream.class, new BBModelLoaderFactory());
    }
}
