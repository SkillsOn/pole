package com.pushpole.sdk.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.pushpole.sdk.internal.log.LogData;
import com.pushpole.sdk.internal.log.Logger;

/***
 * A class for downloading notification image
 */

public class ImageDownloader {
    //private Context mContext;

    /***
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both
     * height and width larger than the requested height and width.
     *
     * @param options   the options
     * @param reqWidth  the desired width
     * @param reqHeight the desired height
     * @return size
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /***
     * First decode with inJustDecodeBounds=true to check dimensions
     * Calculate inSampleSize
     * Decode bitmap with inSampleSize set
     *
     * @param stream
     * @param reqWidth
     * @param reqHeight
     * @return bitmap
     */
    public static Bitmap decodeSampledBitmapFromStream(InputStream stream,
                                                       int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(stream, null, options);
    }

    /**
     * Attempts at downloading an image synchronously.
     *
     * @param url The image url
     * @return The downloaded Bitmap image or null if downloading fails
     */
    public Bitmap downloadImage(String url) {
        try {
            URL imgUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) imgUrl.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();

            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Logger.warning("Downloading image failed", new LogData(
                    "Url", url
            ));
            return null;
        }
    }

    /**
     * Attempts at downloading an image synchronously.
     *
     * @param url The image url
     * @return The downloaded Bitmap image or null if downloading fails
     */
    public Bitmap downloadImage(String url, int reqWidth, int reqHeight) {
        try {
            URL imgUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) imgUrl.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return decodeSampledBitmapFromStream(input, reqWidth, reqHeight);
        } catch (IOException e) {
            Logger.warning("Downloading image failed", new LogData(
                    "Url", url
            ));
            return null;
        }
    }
}
