package org.odyssee.aem.ai.core.workflow;

import java.nio.ByteBuffer;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.odyssee.aem.ai.core.services.AwsCredentialsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.util.IOUtils;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;

@Component(service = WorkflowProcess.class, property = { "process.label=Image Recognition" })
public class ImageRecognition implements WorkflowProcess {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private AwsCredentialsService awsCredentialsService;

	@Override
	public void execute(WorkItem item, WorkflowSession wkfSsession, MetaDataMap args) throws WorkflowException {

		log.info("Getting the AWS credentials service: "+awsCredentialsService);
		log.info("Getting the AWS credentials: "+awsCredentialsService.getCredentials());
		
		ResourceResolver resourceResolver = wkfSsession.adaptTo(ResourceResolver.class);
		Session session = resourceResolver.adaptTo(Session.class);

		String path = item.getWorkflowData().getPayload().toString();
		Resource resource = resourceResolver.getResource(path);
		Asset asset = resource.adaptTo(Asset.class);
		
		Resource metadataResource = resource.getChild(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER);
		Node assetNode = metadataResource.adaptTo(Node.class);

		log.info("Getting the Asset: "+asset.getPath());
		
		try {
			
			ByteBuffer imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(asset.getOriginal().getStream()));
			AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentialsService.getCredentials()))
                    .build();

			DetectLabelsRequest request = new DetectLabelsRequest().withImage(new Image().withBytes(imageBytes))
					.withMaxLabels(10).withMinConfidence(77F);

			DetectLabelsResult result = rekognitionClient.detectLabels(request);
			List<Label> labels = result.getLabels();

			log.info("Detected labels for " + asset.getPath());
			String[] formattedLabels = new String[labels.size()];
			for (int i=0; i < labels.size();i++) {
				Label label = labels.get(i);
				log.info(label.getName() + ": " + label.getConfidence().toString());
				formattedLabels[i] = label.getName() + "(" + label.getConfidence().toString()+")";
			}

			assetNode.setProperty("awstags", formattedLabels);
			
			if (session.hasPendingChanges()) {
                session.save();
			}
		} catch (Exception e) {
			log.error("-->", e);
		}
	}
}