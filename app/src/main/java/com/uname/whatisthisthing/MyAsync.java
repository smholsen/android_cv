package com.uname.whatisthisthing;

import com.google.api.services.vision.v1.VisionRequest;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

// Do the real work in an async task, because we need to use the network anyway
class MyAsync extends AsyncTask<Object, Void, String> {

    private Main main;
    private String CLOUD_VISION_API_KEY = "";
    private Bitmap bitmap;
    private String mode;

    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";


    MyAsync(Main main, Bitmap bitmap, String mode){
        this.main = main;
        this.bitmap = bitmap;
        this.mode = mode;
    }

    @Override
    protected String doInBackground(Object... params) {
        try {
            Log.i("cloud", "started async");
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);

            VisionRequestInitializer requestInitializer =
                    new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                        /**
                         * We override this so we can inject important identifying fields into the HTTP
                         * headers. This enables use of a restricted cloud platform API key.
                         */
                        @Override
                        protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                throws IOException {
                            super.initializeVisionRequest(visionRequest);

                            String packageName = Main.PACKAGE_NAME;
                            visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                            String sig = PackageManagerUtils.getSignature(Main.PACKAGE_MANAGER, packageName);

                            visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                        }
                    };


            builder.setVisionRequestInitializer(requestInitializer);
            Vision vision = builder.build();

            Log.i("cloud", "Vision built");
            BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                    new BatchAnnotateImagesRequest();
            batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                // Add the image
                com.google.api.services.vision.v1.model.Image base64EncodedImage = new com.google.api.services.vision.v1.model.Image();
                // Convert the bitmap to a JPEG
                // Just in case it's a format that Android understands but Cloud Vision
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Log.i("cloud", "be4 compression");
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                Log.i("cloud", "after compression");
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                Log.i("cloud", "be4 Base64 encode");

                // Base64 encode the JPEG
                base64EncodedImage.encodeContent(imageBytes);
                annotateImageRequest.setImage(base64EncodedImage);

                // add the features we want
                final Feature feat = new Feature();
                feat.setType(mode);
                feat.setMaxResults(2);
                annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                    add(feat);
                }});

                Log.i("cloud", "mode: " + mode);

                // Add the list of one thing to the request
                add(annotateImageRequest);
            }});

            Log.i("cloud", "first try completed");

            try {
                Vision.Images.Annotate annotateRequest =
                        vision.images().annotate(batchAnnotateImagesRequest);

                // Due to a bug: requests to Vision API containing large images fail when GZipped.
                annotateRequest.setDisableGZipContent(true);
                Log.d("CloudVision", "created Cloud Vision request object, sending request");

                BatchAnnotateImagesResponse response = annotateRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d("CloudVision", "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d("CloudVision", "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        } catch (Exception e){
            Log.i("cloud", e.toString());
        }
        return null;
    }

    protected void onPostExecute(String result) {
        if (result != null && !result.equals(Constants.NORESPONSE)){
            main.sendRestRequest(result);
        } else {
            main.restResponseReceived(null);
        }
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        // TODO: whooh this is bad. rework everything to not be hardcoded.
        if(response.getResponses().get(0).isEmpty()){
            Log.i("cloud", "empty Response");
            return Constants.NORESPONSE;
        }

        Log.i("cloud", "fullResponse: " + response.toString());
        //TODO If result is "product" then ignore the shit.

        // Todo Have to include more alternatives, top one is often bad. I think photo quality
        // has alot to do with it. (Not sure)

        // TODO: If lower than [treshold] (e.g. 0.75) notify user that this is incorrect and tell him to focus and hold camera steady.
        if (mode.equals(Constants.LABEL)) {
            if (response.getResponses().get(0).getLabelAnnotations().get(0).getDescription().equals("Product") || response.getResponses().get(0).getLabelAnnotations().get(0).getDescription().equals("product")) {
                return response.getResponses().get(0).getLabelAnnotations().get(1).getDescription();
            }
        } else if (mode.equals(Constants.LOGO)){
            Log.i("cloud", "Logo response: " + response.getResponses().get(0).getLogoAnnotations().get(0).getDescription());
            return response.getResponses().get(0).getLogoAnnotations().get(0).getDescription();
        } else if (mode.equals(Constants.LANDMARK)){
            Log.i("cloud", "Landmark response: " + response.getResponses().get(0).getLandmarkAnnotations().get(0).getDescription());
            return response.getResponses().get(0).getLandmarkAnnotations().get(0).getDescription();
        }
        return response.getResponses().get(0).getLabelAnnotations().get(0).getDescription();
    }
}