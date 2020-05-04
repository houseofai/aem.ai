package org.odyssee.aem.ai.core.servlet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.odyssee.aem.ai.core.services.AwsCredentialsService;
import org.odyssee.aem.ai.core.services.aws.rekognition.Helper;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.Image;
import com.day.cq.dam.api.Asset;

@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=Face ID Servlet for Authenticator",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.paths=" + "/bin/faceid" })
public class FaceIDAuth extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	private final Logger log = LoggerFactory.getLogger(FaceIDAuth.class);
	
	@Reference
	private AwsCredentialsService awsCredentialsService;

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

		String strSnapshot = request.getParameter("snapShot");
		String blobString = strSnapshot.replace("data:image/jpeg;base64,", "");
		byte[] byteArray = Base64.getDecoder().decode(blobString);
		
		// Posted Image 
		Image postedImage = getImage(byteArray);
				
		// Image from user
		ResourceResolver rr = request.getResourceResolver();
		Resource r = rr.getResource("/content/dam/we-retail/en/people/me.png");
		Image userImage = Helper.getImage(r.adaptTo(Asset.class));
		
		
		if (isSimilar(postedImage,userImage)) {
			request.setAttribute("message", "Hello world");
			response.sendRedirect("/content/aemai/login/success.html");
		} else {
			response.sendRedirect("/content/aemai/login/fail.html");
		}
	}

	private boolean isSimilar(Image unsafeImage, Image originalImage) throws IOException {

		Float similarityThreshold = 70F;
		AmazonRekognition rekognitionClient = Helper.getClient(awsCredentialsService);

		CompareFacesRequest request = null;
		try {
			// #AWS Bug: if a picture doesn't contain a face, an error is thrown
			request = new CompareFacesRequest().withSourceImage(unsafeImage).withTargetImage(originalImage).withSimilarityThreshold(similarityThreshold);

			// Call operation
			CompareFacesResult compareFacesResult = rekognitionClient.compareFaces(request);

			return compareFacesResult.getFaceMatches().size()>0;
		} catch (Exception e) {
			log.error("-->", e);
		}
		return false;
	}
	
	private Image getImage(byte[] byteArray) throws IOException {

		ByteBuffer imageBytes = ByteBuffer.wrap(byteArray);
		return new Image().withBytes(imageBytes);
	}
}