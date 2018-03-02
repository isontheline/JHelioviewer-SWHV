package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class LoadJSON {

    public static JSONObject of(URI uri) throws IOException, JSONException {
        try (NetClient nc = NetClient.of(uri)) {
            return JSONUtils.readJSON(nc.getReader());
        }
    }

    public static JSONObject of(String uri) throws IOException, JSONException {
        try (NetClient nc = NetClient.of(uri)) {
            return JSONUtils.readJSON(nc.getReader());
        }
    }

}
