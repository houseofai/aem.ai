package org.aem.rekognition.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.util.IOUtils;

public class DetectLabels {
	public static void main(String[] args) throws Exception {
		String photo = "steelhead-and-spines-in-alaska-1.jpg";

		ByteBuffer imageBytes;
		try (InputStream inputStream = new FileInputStream(new File(photo))) {
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
		}

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

		DetectLabelsRequest request = new DetectLabelsRequest().withImage(new Image().withBytes(imageBytes))
				.withMaxLabels(10).withMinConfidence(77F);

		try {
			DetectLabelsResult result = rekognitionClient.detectLabels(request);
			List<Label> labels = result.getLabels();

			System.out.println("Detected labels for " + photo);
			for (Label label : labels) {
				System.out.println(label.getName() + ": " + label.getConfidence().toString());
			}

		} catch (AmazonRekognitionException e) {
			e.printStackTrace();
		}
	}
}