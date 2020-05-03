package org.odyssee.aem.ai.core.services.aws.rekognition;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.sling.api.resource.Resource;
import org.odyssee.aem.ai.core.services.AwsCredentialsService;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;

public class Helper {

	
	public static AmazonRekognition getClient(AwsCredentialsService awsCredentialsService) throws IOException {

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentialsService.getCredentials()))
                .build();
		
		return rekognitionClient;
	}
	
	public static Image getImage(Asset asset) throws IOException {

		ByteBuffer imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(asset.getOriginal().getStream()));
		return new Image().withBytes(imageBytes);
	}
	
	static final String METADATA_PATH = JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER;
	
	public static void addMetadata(Resource resource, String tagName, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
		Resource metadataResource = resource.getChild(METADATA_PATH);
		Node assetNode = metadataResource.adaptTo(Node.class);
		assetNode.setProperty(tagName, values);
	}
	
	public static void addRendition(BufferedImage bi, Asset asset, String name) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(bi, "jpeg", os);
		InputStream is = new ByteArrayInputStream(os.toByteArray());
		asset.addRendition(name, is, asset.getMimeType());
		
	}
}
