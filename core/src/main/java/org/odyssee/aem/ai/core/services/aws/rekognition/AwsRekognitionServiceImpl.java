package org.odyssee.aem.ai.core.services.aws.rekognition;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.odyssee.aem.ai.core.services.AwsCredentialsConfiguration;
import org.odyssee.aem.ai.core.services.AwsCredentialsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.util.IOUtils;
import com.day.cq.dam.api.Asset;

@Component(service = AwsRekognitionService.class)
public class AwsRekognitionServiceImpl implements AwsRekognitionService {

	private final Logger log = LoggerFactory.getLogger(AwsRekognitionService.class);

	@Reference
	private AwsCredentialsService awsCredentialsService;
	
	@Activate
	public void activate(AwsCredentialsConfiguration conf) throws Exception {
		log.info("AwsRekognition service loaded");
	}

	@Override
	public List<Label> getLabels(Asset asset) {
		

		ByteBuffer imageBytes;
		try (InputStream inputStream = new FileInputStream(new File(asset.getPath()))) 
		{
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
			AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentialsService.getCredentials()))
                    .build();

			DetectLabelsRequest request = new DetectLabelsRequest().withImage(new Image().withBytes(imageBytes))
					.withMaxLabels(10).withMinConfidence(77F);

			DetectLabelsResult result = rekognitionClient.detectLabels(request);
			List<Label> labels = result.getLabels();

			log.info("Detected labels for " + asset.getPath());
			for (Label label : labels) {
				log.info(label.getName() + ": " + label.getConfidence().toString());
			}

			return labels;
		} catch (Exception e) {
			log.error("", e);
			e.printStackTrace();
		}
		return null;
	}

}
