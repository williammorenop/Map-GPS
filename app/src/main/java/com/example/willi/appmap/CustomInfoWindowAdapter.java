package com.example.willi.appmap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.willi.appmap.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;
    private Context mContext;

    public CustomInfoWindowAdapter(Context context) {
        mContext = context;
        mWindow = LayoutInflater.from(context).inflate(R.layout.customwindow, null);
    }

    private void rendowWindowText(Marker marker, View view ){

        String title = marker.getTitle();
        TextView tvTitle = (TextView) view.findViewById(R.id.Nombre);

        if(!title.equals("") && title!=null){
            tvTitle.setText(title);
        }

        String snippet = marker.getSnippet();
        TextView tvSnippet = (TextView) view.findViewById(R.id.Ubicacion);

        if(!snippet.equals("")&& snippet!=null){
            tvSnippet.setText(snippet.substring(0,10));
        }

      TextView tvCalif = (TextView) view.findViewById(R.id.Calificacion);

       if(!snippet.equals("") && snippet!=null){
            tvCalif.setText(snippet.substring(10,15));
        }

    }

    @Override
    public View getInfoWindow(Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }
}
