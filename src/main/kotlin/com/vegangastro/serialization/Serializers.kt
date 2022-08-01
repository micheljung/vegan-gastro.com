package com.vegangastro.serialization

import com.vegangastro.country.Country
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URL
import java.time.Instant
import java.util.*

@Serializer(forClass = Instant::class)
@OptIn(ExperimentalSerializationApi::class)
object InstantSerializer : KSerializer<Instant?> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
  override fun serialize(encoder: Encoder, value: Instant?) =
    value?.let { encoder.encodeLong(it.toEpochMilli()) } ?: encoder.encodeNull()

  override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}

@Serializer(forClass = Locale::class)
@OptIn(ExperimentalSerializationApi::class)
object LocaleSerializer : KSerializer<Locale?> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Locale?) =
    value?.let { encoder.encodeString(it.toLanguageTag()) } ?: encoder.encodeNull()

  override fun deserialize(decoder: Decoder): Locale = Locale.Builder().setLanguageTag(decoder.decodeString()).build()
}

@Serializer(forClass = URL::class)
@OptIn(ExperimentalSerializationApi::class)
object UrlSerializer : KSerializer<URL?> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: URL?) =
    value?.let { encoder.encodeString(it.toExternalForm()) } ?: encoder.encodeNull()

  override fun deserialize(decoder: Decoder): URL = URL(decoder.decodeString())
}


@Serializer(forClass = URL::class)
@OptIn(ExperimentalSerializationApi::class)
object CountrySerializer : KSerializer<Country> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Country", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Country) = value.let { encoder.encodeString(it.code) }

  override fun deserialize(decoder: Decoder) = error("Deserialization not supported")
}
