package org.aem.aws_sdkv1_connector.core;

import com.amazonaws.auth.BasicAWSCredentials;

public interface AwsCredentialsService {

	BasicAWSCredentials getCredentials();
}
