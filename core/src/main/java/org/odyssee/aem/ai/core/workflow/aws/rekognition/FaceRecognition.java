package org.odyssee.aem.ai.core.workflow.aws.rekognition;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.odyssee.aem.ai.core.services.AwsCredentialsService;
import org.odyssee.aem.ai.core.services.aws.rekognition.Helper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.day.cq.dam.api.Asset;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(service = WorkflowProcess.class, property = { "process.label=AWS Rekognition - Detecting Faces" })
public class FaceRecognition implements WorkflowProcess {
	
	private static String TAG_NAME = "awsfaces";
	private static String RENDITION_NAME = "Face Bounding Boxes";

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private AwsCredentialsService awsCredentialsService;

	@Override
	public void execute(WorkItem item, WorkflowSession wkfSsession, MetaDataMap args) throws WorkflowException {
		
		ResourceResolver resourceResolver = wkfSsession.adaptTo(ResourceResolver.class);
		Session session = resourceResolver.adaptTo(Session.class);

		String path = item.getWorkflowData().getPayload().toString();
		
		Resource resource = resourceResolver.getResource(path);
		Asset asset = resource.adaptTo(Asset.class);
		

		log.info("Getting the Asset: "+asset.getPath());
		
		try {
			List<FaceDetail> faceDetails = getFaceDetail(asset);

			BufferedImage originalImagebuffer = ImageIO.read(asset.getOriginal().getStream());
			int height = originalImagebuffer.getHeight();
	        int width = originalImagebuffer.getWidth();
	        
			BufferedImage boxedRenditionBuffer = new BufferedImage(width, height, originalImagebuffer.getType());
			
			Graphics2D boxedGraphic = boxedRenditionBuffer.createGraphics();
			boxedGraphic.drawImage(originalImagebuffer, 0, 0, null);
			boxedGraphic.setPaint(Color.red);
			
			String[] formattedLabels = new String[faceDetails.size()];
			
			for (int i=0; i < faceDetails.size();i++) {
				FaceDetail faceDetail = faceDetails.get(i);
				ObjectMapper objectMapper = new ObjectMapper();
				formattedLabels[i] = "face_"+i+": "+objectMapper.writeValueAsString(faceDetail);
			}

			Helper.addMetadata(resource, TAG_NAME, formattedLabels);
			Helper.addRendition(boxedRenditionBuffer, asset, RENDITION_NAME);
			
			if (session.hasPendingChanges()) {
                session.save();
			}
		} catch (Exception e) {
			log.error("-->", e);
		}
	}
	
	private List<FaceDetail> getFaceDetail(Asset asset) throws IOException {
		AmazonRekognition rekognitionClient = Helper.getClient(awsCredentialsService);

		DetectFacesRequest request = new DetectFacesRequest().withImage(Helper.getImage(asset))
				.withAttributes(Attribute.ALL);

		DetectFacesResult result = rekognitionClient.detectFaces(request);
		return result.getFaceDetails();
	}
}