package com.vegangastro.serialization

import com.vegangastro.country.SupportedCountries
import com.vegangastro.job.*
import com.vegangastro.locale.SupportedLocales
import com.vegangastro.place.PlacesSummary
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.*

@Serializable(with = MsgSerializer::class)
abstract class Msg {
  abstract val type_: String
}

object MsgSerializer : KSerializer<Msg> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Msg") {
    element("type", serialDescriptor<String>())
    element("payload", buildClassSerialDescriptor("Any"))
  }

  @Suppress("UNCHECKED_CAST")
  private val typeSerializers: Map<String, KSerializer<Msg>> =
    mapOf(
      Job.MSG_TYPE to serializer<Job>(),
      PlacesSummary.MSG_TYPE to serializer<PlacesSummary>(),
      PlacesSearch.MSG_TYPE to serializer<PlacesSearch>(),
      PlaceStatus.MSG_TYPE to serializer<PlaceStatus>(),
      SearchDone.MSG_TYPE to serializer<SearchDone>(),
      SupportedLocales.MSG_TYPE to serializer<SupportedLocales>(),
      SupportedCountries.MSG_TYPE to serializer<SupportedCountries>(),
      ContactPlace.MSG_TYPE to serializer<ContactPlace>(),
    ).mapValues { (_, v) -> v as KSerializer<Msg> }

  private fun getPayloadSerializer(type: String): KSerializer<Msg> = typeSerializers[type]
    ?: throw SerializationException("Serializer for class $type is not registered in $javaClass")

  override fun serialize(encoder: Encoder, value: Msg) {
    encoder.encodeStructure(descriptor) {
      encodeStringElement(descriptor, 0, value.type_)
      encodeSerializableElement(descriptor, 1, getPayloadSerializer(value.type_), value)
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  override fun deserialize(decoder: Decoder): Msg = decoder.decodeStructure(descriptor) {
    if (decodeSequentially()) {
      val type = decodeStringElement(descriptor, 0)
      val payload = decodeSerializableElement<Msg>(descriptor, 1, getPayloadSerializer(type))
      payload
    } else {
      require(decodeElementIndex(descriptor) == 0) { "type field should precede payload field" }
      val type = decodeStringElement(descriptor, 0)
      val payload = when (val index = decodeElementIndex(descriptor)) {
        1 -> decodeSerializableElement<Msg>(descriptor, 1, getPayloadSerializer(type))
        CompositeDecoder.DECODE_DONE -> throw SerializationException("payload field is missing")
        else -> error("Unexpected index: $index")
      }
      payload
    }
  }
}
