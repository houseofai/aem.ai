package org.odyssee.aem.ai.core.workflow.aws.rekognition;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Node;
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
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.Emotion;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;

@Component(service = WorkflowProcess.class, property = { "process.label=AWS Rekognition - Detecting Faces" })
public class FaceRecognition implements WorkflowProcess {
	
	private static String TAG_NAME = "awsfaces";
	private static String RENDITION_NAME = "Face Bounding Boxes";
	static final String METADATA_PATH = JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER;

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

		Resource metadataResource = resource.getChild(METADATA_PATH);
		Node metadataNode = metadataResource.adaptTo(Node.class);

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
			
			for (int i=0; i < faceDetails.size();i++) {
				FaceDetail faceDetail = faceDetails.get(i);

				metadataNode.setProperty("face_agerange", faceDetail.getAgeRange().getLow() +" - "+ faceDetail.getAgeRange().getHigh());
				
				if(faceDetail.getBeard() != null)
					metadataNode.setProperty("face_beard", faceDetail.getBeard().getValue()+"("+faceDetail.getBeard().getConfidence()+")");
				
				if(faceDetail.getEmotions() != null) {
					List<Emotion> emotions = faceDetail.getEmotions();
					String[] emotionString = new String[emotions.size()];
					for (int j=0; j < emotions.size();j++) {
						Emotion emotion = emotions.get(i);
						emotionString[i] = emotion.getType() + " ("+ emotion.getConfidence()+")";
					}
					metadataNode.setProperty("face_emotion", emotionString);
				}

				if(faceDetail.getEyeglasses() != null)
					metadataNode.setProperty("face_eyeglasses", faceDetail.getEyeglasses().getValue()+" ("+ faceDetail.getEyeglasses().getConfidence()+")");
				if(faceDetail.getEyesOpen() != null)
					metadataNode.setProperty("face_eyeopen", faceDetail.getEyesOpen().getValue()+" ("+ faceDetail.getEyesOpen().getConfidence()+")");
				if(faceDetail.getGender() != null)
					metadataNode.setProperty("face_gender", faceDetail.getGender().getValue()+" ("+ faceDetail.getGender().getConfidence()+")");
				if(faceDetail.getMouthOpen() != null)
					metadataNode.setProperty("face_mouthopen", faceDetail.getMouthOpen().getValue()+" ("+ faceDetail.getMouthOpen().getConfidence()+")");
				if(faceDetail.getMustache() != null)
					metadataNode.setProperty("face_mustache", faceDetail.getMustache().getValue()+" ("+ faceDetail.getMustache().getConfidence()+")");

				if(faceDetail.getPose() != null) {
					metadataNode.setProperty("face_pose", new String[] {"Pitch:"+faceDetail.getPose().getPitch(), "Roll:"+faceDetail.getPose().getRoll(), "Yaw:"+faceDetail.getPose().getYaw()});
				}

				if(faceDetail.getSmile() != null)
					metadataNode.setProperty("face_smile", faceDetail.getSmile().getValue()+" ("+ faceDetail.getSmile().getConfidence()+")");
				if(faceDetail.getSunglasses() != null)
					metadataNode.setProperty("face_sunglasses", faceDetail.getSunglasses().getValue()+" ("+ faceDetail.getSunglasses().getConfidence()+")");
				

                BoundingBox box = faceDetail.getBoundingBox();
                int left = Math.round(width * box.getLeft());
                int top = Math.round(height * box.getTop());
                
                int boxWidth = Math.round(width * box.getWidth());
                int boxHeight = Math.round(height * box.getHeight());
                boxedGraphic.drawRect(left, top, boxWidth, boxHeight);
        		
				boxedGraphic.setFont(new Font("Serif", Font.PLAIN, 20));
				boxedGraphic.drawString("Face", left, top-10);
			}

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