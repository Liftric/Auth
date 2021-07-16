package com.liftric.auth.jwt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.jvm.JvmInline

@Serializer(forClass = CognitoIdToken::class)
internal object CognitoIdTokenSerializer : KSerializer<CognitoIdTokenClaims> {
    private val stringToJsonElementSerializer = MapSerializer(String.serializer(), JsonElement.serializer())

    override val descriptor: SerialDescriptor = stringToJsonElementSerializer.descriptor

    @JvmInline
    private value class MapDecoder(val json: Json) {
        inline fun <reified T> Map<String, JsonElement>.decodeOrNull(key: String): T? =
            get(key)?.let { json.decodeFromJsonElement<T>(it) }

        inline fun <reified T> Map<String, JsonElement>.decode(key: String): T =
            get(key)?.let { json.decodeFromJsonElement<T>(it) } ?: throw SerializationException("Missing field $key")
    }

    override fun deserialize(decoder: Decoder): CognitoIdTokenClaims {
        require(decoder is JsonDecoder)
        val json = decoder.json
        val jsonMap = decoder.decodeSerializableValue(stringToJsonElementSerializer)

        return with(MapDecoder(json)) {
            val customAttributes = jsonMap.filter { (key, _) ->
                key.contains("custom")
            }.map { (key, value) ->
                key.removePrefix("custom:") to if (value.jsonPrimitive.isString) {
                    json.decodeFromJsonElement(String.serializer(), value)
                } else {
                    json.decodeFromJsonElement(Long.serializer(), value).toString()
                }
            }.toMap()

            CognitoIdTokenClaims(
                sub = jsonMap.decodeOrNull("sub"),
                name = jsonMap.decodeOrNull("name"),
                givenName = jsonMap.decodeOrNull("given_name"),
                familyName = jsonMap.decodeOrNull("family_name"),
                middleName = jsonMap.decodeOrNull("middle_name"),
                nickname = jsonMap.decodeOrNull("nickname"),
                preferredUsername = jsonMap.decodeOrNull("preferred_username"),
                profile = jsonMap.decodeOrNull("profile"),
                picture = jsonMap.decodeOrNull("picture"),
                website = jsonMap.decodeOrNull("website"),
                email = jsonMap.decodeOrNull("email"),
                emailVerified = jsonMap.decodeOrNull("email_verified"),
                gender = jsonMap.decodeOrNull("gender"),
                birthdate = jsonMap.decodeOrNull("birthdate"),
                zoneinfo = jsonMap.decodeOrNull("zoneinfo"),
                locale = jsonMap.decodeOrNull("locale"),
                phoneNumber = jsonMap.decodeOrNull("phone_number"),
                phoneNumberVerified = jsonMap.decodeOrNull("phone_number_verified"),
                address = jsonMap.decodeOrNull("address"),
                updatedAt = jsonMap.decodeOrNull("updated_at"),
                aud = jsonMap.decode("aud"),
                authTime = jsonMap.decode("auth_time"),
                cognitoGroups = jsonMap.decode("cognito:groups"),
                cognitoUsername = jsonMap.decode("cognito:username"),
                exp = jsonMap.decode("exp"),
                eventId = jsonMap.decode("event_id"),
                jti = jsonMap.decodeOrNull("jti"),
                originJti = jsonMap.decodeOrNull("origin_jti"),
                iss = jsonMap.decode("iss"),
                iat = jsonMap.decode("iat"),
                scope = jsonMap.decodeOrNull("scope"),
                tokenUse = jsonMap.decode("token_use"),
                customAttributes = customAttributes
            )
        }
    }

    override fun serialize(encoder: Encoder, value: CognitoIdTokenClaims) {
        require(encoder is JsonEncoder)
        val json = encoder.json

        val map: MutableMap<String, JsonElement> = mutableMapOf()
        value.sub?.let { map["name"] = json.encodeToJsonElement(it) }
        value.name?.let { map["name"] = json.encodeToJsonElement(it) }
        value.givenName?.let { map["given_name"] = json.encodeToJsonElement(it) }
        value.familyName?.let { map["family_name"] = json.encodeToJsonElement(it) }
        value.middleName?.let { map["middle_name"] = json.encodeToJsonElement(it) }
        value.nickname?.let { map["nickname"] = json.encodeToJsonElement(it) }
        value.preferredUsername?.let { map["preferred_username"] = json.encodeToJsonElement(it) }
        value.profile?.let { map["profile"] = json.encodeToJsonElement(it) }
        value.picture?.let { map["picture"] = json.encodeToJsonElement(it) }
        value.website?.let { map["website"] = json.encodeToJsonElement(it) }
        value.email?.let { map["email"] = json.encodeToJsonElement(it) }
        value.emailVerified?.let { map["email_verified"] = json.encodeToJsonElement(it) }
        value.gender?.let { map["gender"] = json.encodeToJsonElement(it) }
        value.birthdate?.let { map["birthdate"] = json.encodeToJsonElement(it) }
        value.zoneinfo?.let { map["zoneinfo"] = json.encodeToJsonElement(it) }
        value.locale?.let { map["locale"] = json.encodeToJsonElement(it) }
        value.phoneNumber?.let { map["phone_number"] = json.encodeToJsonElement(it) }
        value.phoneNumberVerified?.let { map["phone_number_verified"] = json.encodeToJsonElement(it) }
        value.address?.let { map["address"] = json.encodeToJsonElement(it) }
        value.updatedAt?.let { map["updated_at"] = json.encodeToJsonElement(it) }
        value.aud.let { map["aud"] = json.encodeToJsonElement(it) }
        value.authTime.let { map["auth_time"] = json.encodeToJsonElement(it) }
        value.cognitoGroups.let { map["cognito:groups"] = json.encodeToJsonElement(it) }
        value.cognitoUsername.let { map["cognito:username"] = json.encodeToJsonElement(it) }
        value.exp.let { map["exp"] = json.encodeToJsonElement(it) }
        value.eventId.let { map["event_id"] = json.encodeToJsonElement(it) }
        value.jti?.let { map["jti"] = json.encodeToJsonElement(it) }
        value.originJti?.let { map["origin_jti"] = json.encodeToJsonElement(it) }
        value.iss.let { map["iss"] = json.encodeToJsonElement(it) }
        value.iat.let { map["iat"] = json.encodeToJsonElement(it) }
        value.scope?.let { map["scope"] = json.encodeToJsonElement(it) }
        value.tokenUse.let { map["token_user"] = json.encodeToJsonElement(it) }
        value.customAttributes?.forEach { map["custom:${it.key}"] = json.encodeToJsonElement(it.value) }

        encoder.encodeSerializableValue(stringToJsonElementSerializer, map)
    }
}
