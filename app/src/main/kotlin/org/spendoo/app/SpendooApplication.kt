package org.spendoo.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.persistence.autoconfigure.EntityScan

@SpringBootApplication
@ComponentScan(basePackages = ["org.spendoo"])
@EnableJpaRepositories(basePackages = ["org.spendoo"])
@EntityScan(basePackages = ["org.spendoo"])
class SpendooApplication

fun main(args: Array<String>) {
	runApplication<SpendooApplication>(*args)
}
