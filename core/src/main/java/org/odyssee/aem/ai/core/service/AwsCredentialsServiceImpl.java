package org.odyssee.aem.ai.core.service;

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

    private final Logger logger = LoggerFactory.getLogger(AwsCredentialsService.class);
	
    private BasicAWSCredentials credentials; 
    private Region region;
    
	@Activate
	public void activate(AwsCredentialsConfiguration conf) throws Exception {
		this.credentials = new BasicAWSCredentials(conf.accessKey(), conf.privateKey());
	}
	
	@Override
	public BasicAWSCredentials getCredentials() {
		return this.credentials;
	}

}
