package org.spendoo.spendoo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpendooApplication

fun main(args: Array<String>) {
	runApplication<SpendooApplication>(*args)
}
