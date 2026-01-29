package org.spendoo.app.controller

import org.spendoo.app.dto.dashboardDtos.DashboardDto
import org.spendoo.app.dto.dashboardDtos.OfferDto
import org.spendoo.app.dto.dashboardDtos.SavingsGoalDto
import org.spendoo.app.dto.dashboardDtos.TopSpendingDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
class DashboardController {

    @GetMapping
    fun getDashboard(): DashboardDto =
        DashboardDto(userName = "Israa",totalBalance = 50000.0, expenses = 22000.0, income = 22000.0,
                offersPreview = listOf(
                OfferDto(1, "Food 20% off", "Discount on selected restaurants", 20),
            OfferDto(2, "Groceries 10% off", "Weekly grocery deal", 10)
        ),
            goalsPreview = listOf(
                SavingsGoalDto(1,"Laptop",66000.0,35000.0),
                SavingsGoalDto(2, "Mobile", 21000.0, 3000.0),
                SavingsGoalDto(3, "Car",500000.0, 25000.0),
                SavingsGoalDto(4,"Xbox",70000.0, 15000.0)
            ),
            topSpendingPreview = listOf(
                TopSpendingDto("Food",10000.0),
                TopSpendingDto("Shopping", 7000.0),
                TopSpendingDto("Movie", 5000.0)
            )

        )

    @GetMapping("/offers")
    fun getOffers(): List<OfferDto> = listOf(
        OfferDto(1, "Food 20% off", "Discount on selected restaurants", 20),
        OfferDto(2, "Groceries 10% off", "Weekly grocery deal", 10),
    )

    @GetMapping("/goals")
    fun getMyGoals(): List<SavingsGoalDto> = listOf(
        SavingsGoalDto(1,"Laptop",66000.0,35000.0),
        SavingsGoalDto(2, "Mobile", 21000.0, 3000.0),
        SavingsGoalDto(3, "Car",500000.0, 25000.0),
        SavingsGoalDto(4,"Xbox",70000.0, 15000.0)
    )

    @GetMapping("/top-spending")
    fun getTopSpending(): List<TopSpendingDto> = listOf(
        TopSpendingDto("Food",10000.0),
        TopSpendingDto("Shopping", 7000.0),
        TopSpendingDto("Movie", 5000.0)
    )

}