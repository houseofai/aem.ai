package org.odyssee.aem.ai.core.services;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;


@Component(service = AwsCredentialsService.class)
@Designate(ocd = AwsCredentialsConfiguration.class)
public class AwsCredentialsServiceImpl implements AwsCredentialsService {

    private final Logger log = LoggerFactory.getLogger(AwsCredentialsService.class);
	
    private BasicAWSCredentials credentials; 
    private Region region;
    
	@Activate
	public void activate(AwsCredentialsConfiguration conf) throws Exception {
		log.info("AwsCredentialsService loaded");
		String accessKey = conf.accessKey();
		String privateKey = conf.privateKey();
		

//		log.info("AwsCredentialsService Key: "+ accessKey);
		
		if(!StringUtils.isEmpty(accessKey) && !StringUtils.isEmpty(privateKey)) {
			this.credentials = new BasicAWSCredentials(accessKey, privateKey);
		}
	}
	
	@Override
	public BasicAWSCredentials getCredentials() {
		return this.credentials;
	}

}
