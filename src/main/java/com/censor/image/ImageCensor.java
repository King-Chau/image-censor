package com.censor.image;

import org.tensorflow.Tensor;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.types.TFloat32;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageCensor {
    private static final String INPUT_OP_NAME = "input_1";
    private static final String OUTPUT_OP_NAME = "dense_3/Softmax";
    private static final String[] CLASSES = {"Drawing", "Hentai", "Neutral", "Porn", "Sexy"};
    private static final int IMAGE_SIZE = 299;

    public static void main(String[] args) throws Exception {
        String imagePath = "images/porn.png";

        float[][][][] inputData = preprocessImage(imagePath);

        try (TFloat32 inputTensor = createInputTensor(inputData, IMAGE_SIZE)) {
            try (Tensor outputTensor = ModelHolder.getSession()
                    .runner()
                    .feed(INPUT_OP_NAME, inputTensor)
                    .fetch(OUTPUT_OP_NAME)
                    .run()
                    .get(0)) {

                float[] probabilities = processOutput(outputTensor);
                printResults(probabilities);
            }
        }
    }

    public static TFloat32 createInputTensor(float[][][][] inputData, int imageSize) {
        TFloat32 tensor = TFloat32.tensorOf(Shape.of(1, imageSize, imageSize, 3));
        var data = tensor;

        for (int b = 0; b < 1; b++) {
            for (int y = 0; y < imageSize; y++) {
                for (int x = 0; x < imageSize; x++) {
                    for (int c = 0; c < 3; c++) {
                        data.setFloat(inputData[b][y][x][c], b, y, x, c);
                    }
                }
            }
        }
        return tensor;
    }

    private static float[] processOutput(Tensor outputTensor) {
        if (!(outputTensor instanceof TFloat32)) {
            throw new IllegalStateException("Unexpected output type: " + outputTensor.getClass());
        }
        TFloat32 floatTensor = (TFloat32) outputTensor;
        Shape shape = floatTensor.shape();
        if (shape.numDimensions() != 2 || shape.size(0) != 1 || shape.size(1) != CLASSES.length) {
            throw new IllegalStateException("Unexpected output shape: " + shape);
        }

        float[] results = new float[CLASSES.length];
        var data = floatTensor;

        for (int i = 0; i < CLASSES.length; i++) {
            results[i] = data.getFloat(0, i);
        }
        return results;
    }

    private static void printResults(float[] probabilities) {
        System.out.println("NSFW Prediction:");
        for (int i = 0; i < CLASSES.length; i++) {
            System.out.printf(" - %-8s: %.4f%n", CLASSES[i], probabilities[i]);
        }
    }

    private static float[][][][] preprocessImage(String path) throws Exception {
        BufferedImage img = ImageIO.read(new File(path));
        BufferedImage resized = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, IMAGE_SIZE, IMAGE_SIZE, null);
        g.dispose();

        float[][][][] result = new float[1][IMAGE_SIZE][IMAGE_SIZE][3];
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                result[0][y][x][0] = ((rgb >> 16) & 0xFF) / 255.0f; // R
                result[0][y][x][1] = ((rgb >> 8) & 0xFF) / 255.0f;  // G
                result[0][y][x][2] = (rgb & 0xFF) / 255.0f;         // B
            }
        }
        return result;
    }
}