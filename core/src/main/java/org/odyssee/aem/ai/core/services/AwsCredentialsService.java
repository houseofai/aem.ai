package org.odyssee.aem.ai.core.services;

import com.amazonaws.auth.BasicAWSCredentials;

public interface AwsCredentialsService {

	BasicAWSCredentials getCredentials();
}
