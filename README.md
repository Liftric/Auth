![CI](https://github.com/Liftric/auth/workflows/CI/badge.svg) 
![maven-central](https://img.shields.io/maven-central/v/com.liftric/auth?label=Maven%20Central) 
![OSS Sonatype (Releases)](https://img.shields.io/nexus/r/com.liftric/auth?label=Sonatype%20OSSRH%20%28Releases%29&server=https%3A%2F%2Fs01.oss.sonatype.org)
![npm (scoped)](https://img.shields.io/npm/v/@liftric/auth)

# Cognito-idp

Lightweight AWS Cognito Identity Provider client for Kotlin Multiplatform and Typescript projects.

> Not all requests, errors, and auth flows are implemented.  
> Feel free to [contribute](Contributing.md) if there is something missing for you.

## Import

### Kotlin

#### Gradle 

```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            implementation("com.liftric:cognito-idp:<version>")
        }
    }
}
```

### Typescript

#### Yarn
```bash
yarn add @liftric/cognito-idp@<version>
```
#### npm
```sh
npm i @liftric/cognito-idp@<version>
```

## How-to

### Init

#### Kotlin

```kotlin
val provider = IdentityProviderClient("<region>", "<clientId>") 
```

#### Typescript

```typescript
import {IdentityProviderClientJS} from '@liftric/cognito-idp';

const provider = new IdentityProviderClientJS('<region>', '<clientId>');
```

### Usage

#### Kotlin

All methods are suspending and return a [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)<T> object.

```kotlin
provider.signUp("user", "password").fold(
    onSuccess = {
        // Do something
    },
    onFailure = {
        // Handle exceptions
    }
)
```

#### Typescript

All methods return a [Promise](https://developer.mozilla.org/de/docs/Web/JavaScript/Reference/Global_Objects/Promise)<T> object.

### Errors

Request related exceptions are of type `IdentityProviderException`. They contain the http `status` code and the `message`. Common AWS exceptions are implemented as subclasses. In case that we don't have implemented the exception type it will default to ´IdentityProviderException.Unknown´, which will contain the AWS exception `type`.

Network related exceptions (e.g. no internet) are of type `IOException`.

### Requests 
#### Sign Up

Signs up the user.

Attributes are optional.

```kotlin
val attribute = UserAttribute("email", "name@url.tld")
signUp("<username>", "<password>", listOf(attribute)): Result<SignUpResponse>
```

#### Confirm Sign Up

Confirms the sign up (also the delivery medium).

```kotlin
confirmSignUp("<username>", "<confirmationCode>"): Result<Unit>
```

#### Sign In

Signs in the users.

```kotlin
signIn("<username>", "<password>"): Result<SignInResponse>
```

#### Refresh access token

Refreshes access token based on refresh token that's retrieved from an earlier sign in.

```kotlin
val signInResponse: SignInResponse = ... // from earlier login or refresh
val refreshToken = signInResponse.AuthenticationResult.RefreshToken
refresh(refreshToken): Result<SignInResponse>
```

#### Get Claims

You can retrieve the claims of both the IdTokens' and AccessTokens' payload by converting them to either a `CognitoIdToken` or `CognitoAccessToken`

```kotlin
val idToken = CognitoIdToken(idTokenString)
val phoneNumber = idToken.claims.phoneNumber
val sub = idToken.claims.sub
```

Custom attributes of the IdToken get mapped into `customAttributes`.

You have to drop the `custom:` prefix.

```kotlin
val twitter = idToken.claims.customAttributes["twitter"]
```

#### Get User

Returns the users attributes and metadata on success.

More info about this in the [official documentation](https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_GetUser.html).

```kotlin
getUser("<accessToken>"): Result<GetUserResponse>
```

#### Update User Attributes

Updates the users attributes (e.g. email, phone number, ...).

```kotlin
val attributes: List<UserAttribute> = ...
updateUserAttributes("<accessToken>", attributes): Result<UpdateUserAttributesResponse>
```

#### Change Password

Updates the users password 

```kotlin
changePassword("<accessToken>", "<currentPassword>", "<newPassword>"): Result<Unit>
```

#### Forgot Password

Invokes password forgot and sends a confirmation code the the users' delivery medium.

More info about the ForgotPasswordResponse in the [official documentation](https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_CodeDeliveryDetailsType.html).

```kotlin
forgotPassword("<username>"): Result<ForgotPasswordResponse>
```

#### Confirm Forgot Password

Confirms forgot password.

```kotlin
confirmForgotPassword("<confirmationCode>", "<username>", "<newPassword>"): Result<Unit>
```

#### Get user Attribute Verification Code

Gets the user attribute verification code for the specified attribute name

```kotlin
getUserAttributeVerificationCode("<accessToken>", "<attributeName>", "<clientMetadata>"): Result<GetAttributeVerificationCodeResponse>
```

#### Verify User Attribute

Verifies the specified user attribute.

```kotlin
verifyUserAttribute("<accessToken>", "<attributeName>", "<confirmationCode>"): Result<Unit>
```

#### Sign Out

Signs out the user globally.

```kotlin
signOut("<accessToken>"): Result<SignOutResponse>
```

#### Revoke Token

Revokes all access tokens generated by the refresh token.

```kotlin
revokeToken("<refreshToken>"): Result<Unit>
```

#### Delete User

Deletes the user from the user pool.

```kotlin
deleteUser("<accessToken>"): Result<Unit>
```

## License

Cognito-idp is available under the MIT license. See the LICENSE file for more info.
