package com.vegangastro.email

import io.camassia.mjml.MJMLClient
import io.camassia.mjml.model.request.RenderRequest
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class TemplateRenderer : KoinComponent {

  private val mjmlClient by inject<MJMLClient>()

  fun render(template: Template, replacements: Map<String, String>): String =
    mjmlClient.render(RenderRequest(template.getString())).html.let {
      replacements.entries.forEach { entry ->
        it.replace("{{ ${entry.key} }}", entry.value)
      }
      it
    }
}
