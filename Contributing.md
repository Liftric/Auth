# Contributing

We're using a live Cognito user pool for integration tests and the values are provided using code generation at compile time.

The build needs both `region` and `clientId` environment variables defined. `region` expects the AWS region code for the target region, like "us-east-1".

If you only want to build the project, provide `region` and `clientId` environment variables with garbage values.

To execute the tests you have to use your own Cognito user pool client values.
