package com.vegangastro.email

import io.camassia.mjml.MJMLClient
import io.camassia.mjml.model.request.RenderRequest
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class TemplateRenderer : KoinComponent {

  private val mjmlClient by inject<MJMLClient>()
  private val cache: MutableMap<Template, String> = mutableMapOf()

  fun render(template: Template): String = cache.computeIfAbsent(template) {
    mjmlClient.render(RenderRequest(template.getString())).html
  }
}
