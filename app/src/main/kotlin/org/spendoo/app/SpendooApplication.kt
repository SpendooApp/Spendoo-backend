package org.spendoo.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ComponentScan(basePackages = ["org.spendoo"])
@EnableJpaRepositories(basePackages = ["org.spendoo"])
@EntityScan(basePackages = ["org.spendoo"])
@EnableScheduling
class SpendooApplication

fun main(args: Array<String>) {
	runApplication<SpendooApplication>(*args)
}
