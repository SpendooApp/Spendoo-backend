package org.spendoo.savingGoals

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.savingGoals.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.savingGoals.repository"])
@ComponentScan(basePackages = ["org.spendoo"])
class SavingGoalsTestApplication