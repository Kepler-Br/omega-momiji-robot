package com.momiji.bot.config

import com.momiji.gateway.outbound.api.GatewayMessageSenderController
import feign.Contract
import feign.Feign
import feign.codec.Decoder
import feign.codec.Encoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.FeignClientsConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
// TODO: What FeignClientsConfiguration do?
@Import(FeignClientsConfiguration::class)
class GatewayConfig {

    @Bean
    fun gatewayMessageSenderClient(
        @Value("\${momiji.gateway.url}") url: String,
        contract: Contract,
        decoder: Decoder,
        encoder: Encoder,
    ): GatewayMessageSenderController {
        return Feign.builder()
            .encoder(encoder)
            .decoder(decoder)
            .contract(contract)
            .target(GatewayMessageSenderController::class.java, "$url/outbound")
    }
}
