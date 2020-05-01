package org.aem.aws_sdkv1_connector.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Amazon Web Service Configuration")
public @interface AwsCredentialsConfiguration {
	
	@AttributeDefinition(
	        name = "Access Key",
	        description = "IAM Access Key",
	        type = AttributeType.STRING, required = true)
	String accessKey();
	
	@AttributeDefinition(
	        name = "Private Key",
	        description = "IAM Private Key",
	        type = AttributeType.PASSWORD, required = true)
	String privateKey();
	
	@AttributeDefinition(
	        name = "Region",
	        description = "Amazon Web Service cloud region (e.g., us-east-2). See 'Regions and Availability Zones' page from AWS",
	        type = AttributeType.STRING, required = true, defaultValue = "us-east-1")
	String region();
	
	
}
