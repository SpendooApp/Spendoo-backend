package org.spendoo.app.controller

import org.spendoo.app.dto.notificationDtos.NotificationDto
import org.spendoo.app.dto.notificationDtos.NotificationReadResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/notifications")
class NotificationController {


    private val mock = mutableListOf(
        NotificationDto(1, "Budget Exceeded!", "You’ve exceeded your shopping budget by 20% this month.", "2026-1-21T09:00:00Z", false ),
        NotificationDto(2, "Saving Goal", "You’ve completed 80% of your car goal—keep it up!", "2026-1-21T08:00:00Z", true)
    )

    @GetMapping
    fun list(): List<NotificationDto> = mock

    @PutMapping("/{id}/read")
    fun read(@PathVariable id: Long): NotificationReadResponse {
        val notification = mock.find {it.id == id}
        notification?.let { mock[mock.indexOf(it) ] = it.copy(read = true) }
        return NotificationReadResponse(id = id , read = true )
    }

    // Another endpoint for deleting notification?
    // Are notifications deletable?


}