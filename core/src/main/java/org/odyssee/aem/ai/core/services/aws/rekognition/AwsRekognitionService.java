package org.odyssee.aem.ai.core.services.aws.rekognition;

import java.util.List;

import com.amazonaws.services.rekognition.model.Label;
import com.day.cq.dam.api.Asset;

public interface AwsRekognitionService {

	List<Label> getLabels(Asset asset);
}
