package com.dylanvann.fastimage.bb;

import java.util.Map;

public class BBModel {

    String url;

    Map<String, String> headers;

    public BBModel(String url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }
}
