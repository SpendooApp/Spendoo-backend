package org.spendoo.identity

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/identity/health")
class IdentityHealthController {
    @GetMapping("/")
    fun home(): String = "identity OK"
}
