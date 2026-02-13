package com.socoolheeya.bluebank.account.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class ExternalAccountController {
    @GetMapping("")
    fun test(): String {
        return "test"
    }
}