package org.odyssee.aem.ai.core.workflow;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import javax.imageio.ImageIO;
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
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Instance;
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

			BufferedImage originalImagebuffer = ImageIO.read(asset.getOriginal().getStream());
			int height = originalImagebuffer.getHeight();
	        int width = originalImagebuffer.getWidth();
	        
			BufferedImage boxedRenditionBuffer = new BufferedImage(width, height, originalImagebuffer.getType());
			
			Graphics2D boxedGraphic = boxedRenditionBuffer.createGraphics();
			boxedGraphic.drawImage(originalImagebuffer, 0, 0, null);
			boxedGraphic.setPaint(Color.red);
			
			String[] formattedLabels = new String[labels.size()];
			for (int i=0; i < labels.size();i++) {
				Label label = labels.get(i);
				log.info(label.getName() + ": " + label.getConfidence().toString());
				formattedLabels[i] = label.getName() + "(" + label.getConfidence().toString()+")";
				
				List<Instance> instances = label.getInstances();
				log.info("Instances of " + label.getName());
	            if (instances.isEmpty()) {
	            	log.info("  " + "None");
	            } else {
	                for (Instance instance : instances) {
	                	log.info("Confidence: " + instance.getConfidence().toString());
	                	log.info("Bounding box: " + instance.getBoundingBox().toString());
	                    
	                    BoundingBox box = instance.getBoundingBox();
	                    int left = Math.round(width * box.getLeft());
	                    int top = Math.round(height * box.getTop());
	                    
	                    int boxWidth = Math.round(width * box.getWidth());
	                    int boxHeight = Math.round(height * box.getHeight());
	                    boxedGraphic.drawRect(left, top, boxWidth, boxHeight);
	            		
						boxedGraphic.setFont(new Font("Serif", Font.PLAIN, 20));
						boxedGraphic.drawString(label.getName(), left, top-10);
	                }
	            }
			}
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(boxedRenditionBuffer, "jpeg", os);
			InputStream is = new ByteArrayInputStream(os.toByteArray());
			asset.addRendition("Bounding Boxes", is, asset.getMimeType());

			assetNode.setProperty("awstags", formattedLabels);
			
			if (session.hasPendingChanges()) {
                session.save();
			}
		} catch (Exception e) {
			log.error("-->", e);
		}
	}
}