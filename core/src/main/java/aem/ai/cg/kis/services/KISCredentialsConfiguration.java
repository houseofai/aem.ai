package aem.ai.cg.kis.services;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Cap Gemini KIS Web Service Configuration")
public @interface KISCredentialsConfiguration {
	
	@AttributeDefinition(
	        name = "Access Key",
	        type = AttributeType.STRING, required = true)
	String accessKey();
	
	@AttributeDefinition(
	        name = "Private Key",
	        type = AttributeType.PASSWORD, required = true)
	String privateKey();
	
	@AttributeDefinition(
	        name = "Server URL and port",
	        type = AttributeType.STRING, required = true, defaultValue = "3.130.53.190:4000")
	String serverUrl();
	
	
}
