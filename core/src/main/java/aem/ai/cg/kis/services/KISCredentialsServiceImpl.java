package aem.ai.cg.kis.services;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(service = KISCredentialsService.class)
@Designate(ocd = KISCredentialsConfiguration.class)
public class KISCredentialsServiceImpl implements KISCredentialsService {

    private final Logger log = LoggerFactory.getLogger(KISCredentialsService.class);
	
    private String serverUrl;
    
	@Activate
	public void activate(KISCredentialsConfiguration conf) throws Exception {
		log.info("KISCredentialsService loaded");
		String accessKey = conf.accessKey();
		String privateKey = conf.privateKey();
		
		this.serverUrl = conf.serverUrl();
		
		// TODO Authenticate with keys when the authentication will be set
	}
	
	@Override
	public String getServerURL() {
		return serverUrl;
	}

}
