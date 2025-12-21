package org.spendoo.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["org.spendoo"])
class SpendooApplication

fun main(args: Array<String>) {
	runApplication<SpendooApplication>(*args)
}
