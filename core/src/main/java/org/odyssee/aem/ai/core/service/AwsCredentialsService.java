package org.odyssee.aem.ai.core.service;

import com.amazonaws.auth.BasicAWSCredentials;

public interface AwsCredentialsService {

	BasicAWSCredentials getCredentials();
}
