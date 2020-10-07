package aem.ai.aws.rekognition.workflow;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Instance;
import com.amazonaws.services.rekognition.model.Label;
import com.day.cq.dam.api.Asset;

import aem.ai.aws.services.AwsCredentialsService;
import aem.ai.aws.services.rekognition.Helper;

@Component(service = WorkflowProcess.class, property = { "process.label=AWS Rekognition - Detecting Labels in an Image" })
public class ImageRecognition implements WorkflowProcess {
	
	private static String TAG_NAME = "awstags";
	private static String RENDITION_NAME = "Bounding Boxes";

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
		log.info(args.toString());
		log.info("___> {}",args.get("PROCESS_ARGS", "string"));
		for(String keys: args.keySet()) {
			log.info("___> {}",keys, args.get(keys, "null"));
		}
		
		try {
			List<Label> labels = getLabels(asset);

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
			
			Helper.addRendition(boxedRenditionBuffer, asset, RENDITION_NAME);
			Helper.addMetadata(resource, TAG_NAME, formattedLabels);
			
			if (session.hasPendingChanges()) {
                session.save();
			}
		} catch (Exception e) {
			log.error("-->", e);
		}
	}
	
	private List<Label> getLabels(Asset asset) throws IOException {
		AmazonRekognition rekognitionClient = Helper.getClient(awsCredentialsService);

		DetectLabelsRequest request = new DetectLabelsRequest().withImage(Helper.getImage(asset))
				.withMaxLabels(10).withMinConfidence(77F);

		DetectLabelsResult result = rekognitionClient.detectLabels(request);
		return result.getLabels();
	}
}