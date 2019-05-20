package com.springboot.springbootimageeditor.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sun.javafx.iio.ImageStorage;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.ServletInputStream;

public class ImageProcessorController {

    private Map<String, BufferedImage> imageMap = new HashMap<>();

    public JSONObject setImage(ServletInputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);

        String uniqueID = UUID.randomUUID().toString();
        imageMap.put(uniqueID, image);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ID", uniqueID);
        jsonObject.put("Height", image.getHeight());
        jsonObject.put("Width", image.getWidth());

        return jsonObject;
    }

    public void deleteImage(String id) {
        imageMap.remove(id);
    }

    public boolean imageMapContains(String id) {
        if (imageMap.containsKey(id)) {
            return true;
        } else {
            return false;
        }
    }

    public JSONObject getSize(String id) {
        JSONObject jsonObject = new JSONObject();
        BufferedImage image = imageMap.get(id);
        jsonObject.put("Height", image.getHeight());
        jsonObject.put("Width", image.getWidth());
        return jsonObject;
    }

    private static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private BufferedImage bicubicScaling(BufferedImage image, double percentage) {
        BufferedImage clone = deepCopy(image);
        BufferedImage scaled = new BufferedImage((int) (image.getWidth() * percentage / 100), (int) (image.getHeight() * percentage / 100), BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics2D = scaled.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.scale(percentage / 100, percentage / 100);
        graphics2D.drawImage(clone, 0, 0, null);
        graphics2D.dispose();

        return scaled;
    }

    public byte[] getScaledImage(String id, double percentage) throws IOException {
        BufferedImage scaledImage = bicubicScaling(imageMap.get(id), percentage);
        return bufferedImageToByteArray(scaledImage);
    }

    private byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
    }

    public JSONObject getHistogram(String id) {
        BufferedImage image = imageMap.get(id);
        double R[] = new double[256];
        double G[] = new double[256];
        double B[] = new double[256];
        int rgb, r, g, b;

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                rgb = image.getRGB(i, j);
                r = (rgb >> 16) & 0xff;
                g = (rgb >> 8) & 0xff;
                b = (rgb) & 0xff;
                R[r]++;
                G[g]++;
                B[b]++;
            }
        }

        double maxR = R[0];
        double maxG = G[0];
        double maxB = B[0];

        for(int i=1;i<255;i++){
            if(maxR < R[i])
                maxR = R[i];
            if(maxG < G[i])
                maxG = G[i];
            if(maxB < B[i])
                maxB = B[i];
        }


        JSONObject jsonObject = new JSONObject();
        JSONObject rObject = new JSONObject();
        JSONObject gObject = new JSONObject();
        JSONObject bObject = new JSONObject();

        for (int i = 0; i < 256; i++) {
            R[i] /= maxR;
            G[i] /= maxG;
            B[i] /= maxB;

            rObject.put(i, R[i]);
            gObject.put(i, G[i]);
            bObject.put(i, B[i]);
        }
        jsonObject.put("R", rObject);
        jsonObject.put("G", gObject);
        jsonObject.put("B", bObject);

        return jsonObject;
    }

    public byte[] getCroppedImage(String id, int start, int stop, int width, int height) throws IOException, ResponseStatusException {
        if ((start >= 0 && start <= imageMap.get(id).getWidth()) && stop >= 0 && stop <= imageMap.get(id).getHeight() && (imageMap.get(id).getWidth() - start) >= width && (imageMap.get(id).getHeight() - stop >= height)) {

            BufferedImage croppedImage = imageMap.get(id).getSubimage(start, stop, width, height);

            return bufferedImageToByteArray(croppedImage);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "One of the arguments is not correct!"
            );
        }
    }

    public byte[] getGreyScale(String id) throws IOException {
        BufferedImage clone = deepCopy(imageMap.get(id));

        int rgb, r, g, b, R, G, B;
        for (int i = 0; i < clone.getWidth(); i++) {
            for (int j = 0; j < clone.getHeight(); j++) {
                rgb = clone.getRGB(i, j);
                r = (rgb >> 16) & 0xff;
                g = (rgb >> 8) & 0xff;
                b = (rgb) & 0xff;
                R = (int) (r * 0.29);
                G = (int) (g * 0.587);
                B = (int) (b * 0.114);
                Color grey = new Color(R + G + B, R + G + B, R + G + B);
                clone.setRGB(i, j, grey.getRGB());
            }
        }
        BufferedImage image = new BufferedImage(clone.getWidth(), clone.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D bg = image.createGraphics();

        bg.drawImage(clone, 0, 0, null);
        bg.dispose();

        return bufferedImageToByteArray(image);
    }

    public byte[] getBlurredImage(String id, float radius) throws IOException {
        BufferedImage image = imageMap.get(id);
        BufferedImage clone = deepCopy(image);

        GaussianFilter gaussianFilter = new GaussianFilter(radius, 100);
        clone = gaussianFilter.filter(image, clone, true);

        return bufferedImageToByteArray(clone);
    }

}
