package org.spendoo.app.controller

import org.spendoo.app.dto.categoryDtos.CategoryCreateRequest
import org.spendoo.app.dto.categoryDtos.CategoryDto
import org.spendoo.app.dto.categoryDtos.CategoryUpdateRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController {

    private val mock = mutableListOf(
        CategoryDto(1, "Food", "expense"),
        CategoryDto(2, "Travel", "expense"),
        CategoryDto(3, "Shopping", "expense"),
        CategoryDto(4, "Loan", "expense"),
        CategoryDto(5, "Car", "expense"),
        CategoryDto(6, "Health", "expense")
    )

    @GetMapping
    fun list(): List<CategoryDto> = mock

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): CategoryDto =
        mock.find {it.id == id} ?: CategoryDto(id, "Unknown", "Unknown")


// there was no post method in the current screen to add a new category

//    @PostMapping
//    fun create(@RequestBody req : CategoryCreateRequest): CategoryDto {
//        val created = CategoryDto(
//            id = (mock.maxOfOrNull { it.id }?:0 ) + 1,
//            name = req.name,
//            type = req.type
//        )
//        mock.add(created)
//        return created
//    }



    @PutMapping("/{id}")
    fun update(@PathVariable id : Long, @RequestBody req : CategoryUpdateRequest): CategoryDto {
        val old = mock.find { it.id == id } ?: return CategoryDto(id,"Unknown", "Unknown")
        val new = old.copy(
            name = req.name?: old.name,
            type = req.type?: old.type
        )
        mock.removeIf { it.id == id }
        mock.add(new)
        return new

    }

}