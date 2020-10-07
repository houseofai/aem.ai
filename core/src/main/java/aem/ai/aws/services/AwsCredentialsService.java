package aem.ai.aws.services;

import com.amazonaws.auth.BasicAWSCredentials;

public interface AwsCredentialsService {

	BasicAWSCredentials getCredentials();
}
